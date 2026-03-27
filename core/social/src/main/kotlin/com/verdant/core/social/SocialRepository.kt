package com.verdant.core.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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
    private val auth: FirebaseAuth,
) {
    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    /** Generates a 6-digit invite code for a habit. */
    suspend fun createInvite(habitId: String, habitName: String): String? {
        val uid = auth.currentUser?.uid ?: return null
        val displayName = auth.currentUser?.displayName ?: "Verdant User"
        val code = (100_000..999_999).random().toString()

        val ref = db.getReference("invites").child(code)
        val data = mapOf(
            "habit_id" to habitId,
            "habit_name" to habitName,
            "owner_uid" to uid,
            "owner_display_name" to displayName,
            "created_at" to ServerValue.TIMESTAMP,
        )
        suspendCancellableCoroutine { cont ->
            ref.setValue(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }
        return code
    }

    /** Accepts a buddy invite and creates a two-way connection. */
    suspend fun acceptInvite(code: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val displayName = auth.currentUser?.displayName ?: "Verdant User"

        val invite = getInvite(code) ?: return false
        if (invite.ownerUid == uid) return false // Can't buddy yourself

        // Create connection for the inviter
        val ownerRef = db.getReference("buddies")
            .child(invite.ownerUid)
            .child(invite.habitId)
            .child(uid)
        ownerRef.setValue(
            mapOf(
                "display_name" to displayName,
                "streak" to 0,
                "last_updated" to ServerValue.TIMESTAMP,
            ),
        )

        // Create connection for the acceptor
        val acceptorRef = db.getReference("buddies")
            .child(uid)
            .child(invite.habitId)
            .child(invite.ownerUid)
        acceptorRef.setValue(
            mapOf(
                "display_name" to invite.ownerDisplayName,
                "streak" to 0,
                "last_updated" to ServerValue.TIMESTAMP,
            ),
        )

        // Clean up the invite
        db.getReference("invites").child(code).removeValue()
        return true
    }

    /** Gets invite details from a code. */
    private suspend fun getInvite(code: String): HabitInvite? {
        return suspendCancellableCoroutine { cont ->
            db.getReference("invites").child(code).get()
                .addOnSuccessListener { snap ->
                    if (!snap.exists()) { cont.resume(null); return@addOnSuccessListener }
                    val invite = HabitInvite(
                        code = code,
                        habitId = snap.child("habit_id").getValue(String::class.java) ?: "",
                        habitName = snap.child("habit_name").getValue(String::class.java) ?: "",
                        ownerUid = snap.child("owner_uid").getValue(String::class.java) ?: "",
                        ownerDisplayName = snap.child("owner_display_name").getValue(String::class.java) ?: "",
                        createdAt = snap.child("created_at").getValue(Long::class.java) ?: 0L,
                    )
                    cont.resume(invite)
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    /** Observes buddy connections for a specific habit. */
    fun observeBuddies(habitId: String): Flow<List<BuddyConnection>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        val ref = db.getReference("buddies").child(uid).child(habitId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val buddies = snapshot.children.mapNotNull { child ->
                    BuddyConnection(
                        habitId = habitId,
                        buddyUid = child.key ?: return@mapNotNull null,
                        buddyDisplayName = child.child("display_name").getValue(String::class.java) ?: "Unknown",
                        buddyStreak = child.child("streak").getValue(Int::class.java) ?: 0,
                        lastUpdated = child.child("last_updated").getValue(Long::class.java) ?: 0L,
                    )
                }
                trySend(buddies)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Updates this user's streak for a habit so buddies can see it. */
    suspend fun updateMyStreak(habitId: String, streak: Int) {
        val uid = auth.currentUser?.uid ?: return
        // Update streak in all buddy connections that point to this user
        val buddiesRef = db.getReference("buddies")
        // We need to find all users who have us as a buddy for this habit
        // This is denormalized -- we update our side, the other user sees it
        val myRef = buddiesRef.child(uid).child(habitId)
        myRef.get().addOnSuccessListener { snap ->
            snap.children.forEach { child ->
                val buddyUid = child.key ?: return@forEach
                buddiesRef.child(buddyUid).child(habitId).child(uid)
                    .child("streak").setValue(streak)
            }
        }
    }
}
