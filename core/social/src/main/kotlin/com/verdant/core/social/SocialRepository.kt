package com.verdant.core.social

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

data class BuddyConnection(
    val habitId: String,
    val buddyUid: String,
    val buddyDisplayName: String,
    val buddyStreak: Int,
    val lastUpdated: Long,
)

data class HabitInvite(
    val code: String,
    val habitId: String,
    val habitName: String,
    val ownerUid: String,
    val ownerDisplayName: String,
    val createdAt: Long,
)

@Singleton
class SocialRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private fun userId(): String? = supabase.auth.currentUserOrNull()?.id

    private fun displayName(): String {
        val user = supabase.auth.currentUserOrNull()
        val meta = user?.userMetadata
        return meta?.get("full_name")?.toString()?.trim('"') ?: "Verdant User"
    }

    /** Generates a 6-digit invite code for a habit. */
    suspend fun createInvite(habitId: String, habitName: String): String? {
        val uid = userId() ?: return null
        val code = (100_000..999_999).random().toString()

        supabase.postgrest.from("invites").insert(
            InviteDto(
                code = code,
                habitId = habitId,
                habitName = habitName,
                ownerUid = uid,
                ownerDisplayName = displayName(),
                createdAt = System.currentTimeMillis(),
            )
        )
        return code
    }

    /** Accepts a buddy invite and creates a two-way connection. */
    suspend fun acceptInvite(code: String): Boolean {
        val uid = userId() ?: return false
        val name = displayName()

        val invite = getInvite(code) ?: return false
        if (invite.ownerUid == uid) return false // Can't buddy yourself

        val now = System.currentTimeMillis()

        // Create connection for the inviter
        supabase.postgrest.from("buddy_connections").insert(
            BuddyDto(
                userId = invite.ownerUid,
                habitId = invite.habitId,
                buddyUid = uid,
                displayName = name,
                streak = 0,
                lastUpdated = now,
            )
        )

        // Create connection for the acceptor
        supabase.postgrest.from("buddy_connections").insert(
            BuddyDto(
                userId = uid,
                habitId = invite.habitId,
                buddyUid = invite.ownerUid,
                displayName = invite.ownerDisplayName,
                streak = 0,
                lastUpdated = now,
            )
        )

        // Clean up the invite
        supabase.postgrest.from("invites").delete {
            filter { eq("code", code) }
        }
        return true
    }

    /** Gets invite details from a code. */
    private suspend fun getInvite(code: String): HabitInvite? {
        return supabase.postgrest.from("invites")
            .select { filter { eq("code", code) } }
            .decodeList<InviteDto>()
            .firstOrNull()
            ?.let {
                HabitInvite(
                    code = it.code,
                    habitId = it.habitId,
                    habitName = it.habitName,
                    ownerUid = it.ownerUid,
                    ownerDisplayName = it.ownerDisplayName,
                    createdAt = it.createdAt,
                )
            }
    }

    /** Observes buddy connections for a specific habit via Supabase Realtime. */
    fun observeBuddies(habitId: String): Flow<List<BuddyConnection>> = callbackFlow {
        val uid = userId()
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        send(fetchBuddies(uid, habitId))

        try {
            val channel = supabase.realtime.channel("buddies_${habitId}_$uid")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = "buddy_connections"
                }.collect { send(fetchBuddies(uid, habitId)) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    private suspend fun fetchBuddies(uid: String, habitId: String): List<BuddyConnection> {
        return supabase.postgrest.from("buddy_connections")
            .select {
                filter {
                    eq("user_id", uid)
                    eq("habit_id", habitId)
                }
            }
            .decodeList<BuddyDto>()
            .map {
                BuddyConnection(
                    habitId = it.habitId,
                    buddyUid = it.buddyUid,
                    buddyDisplayName = it.displayName,
                    buddyStreak = it.streak,
                    lastUpdated = it.lastUpdated,
                )
            }
    }

    /** Updates this user's streak for a habit so buddies can see it. */
    suspend fun updateMyStreak(habitId: String, streak: Int) {
        val uid = userId() ?: return
        supabase.postgrest.from("buddy_connections").update(
            {
                set("streak", streak)
                set("last_updated", System.currentTimeMillis())
            }
        ) {
            filter {
                eq("buddy_uid", uid)
                eq("habit_id", habitId)
            }
        }
    }
}

@Serializable
internal data class InviteDto(
    val code: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("habit_name") val habitName: String,
    @SerialName("owner_uid") val ownerUid: String,
    @SerialName("owner_display_name") val ownerDisplayName: String,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
internal data class BuddyDto(
    @SerialName("user_id") val userId: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("buddy_uid") val buddyUid: String,
    @SerialName("display_name") val displayName: String,
    val streak: Int,
    @SerialName("last_updated") val lastUpdated: Long,
)
