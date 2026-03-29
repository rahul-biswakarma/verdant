package com.verdant.core.common.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val supabase: SupabaseClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentUser: StateFlow<AuthUser?> = supabase.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    user?.let {
                        AuthUser(
                            uid = it.id,
                            displayName = it.userMetadata?.get("full_name")?.toString()
                                ?.removeSurrounding("\""),
                            email = it.email,
                            photoUrl = it.userMetadata?.get("avatar_url")?.toString()
                                ?.removeSurrounding("\""),
                        )
                    }
                }
                else -> null
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val currentUser: StateFlow<AuthUser?> get() = _currentUser

    val isSignedIn: Flow<Boolean> = _currentUser.map { it != null }

    /**
     * Launches Google Sign-In via Credential Manager and authenticates with Supabase GoTrue.
     *
     * @param activityContext Must be an Activity context for Credential Manager UI.
     * @param webClientId The OAuth 2.0 web client ID.
     */
    suspend fun signInWithGoogle(activityContext: Context, webClientId: String): Result<AuthUser> =
        runCatching {
            val credentialManager = CredentialManager.create(activityContext)

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken

            supabase.auth.signInWith(IDToken) {
                provider = Google
                this.idToken = idToken
            }

            val user = supabase.auth.currentUserOrNull()
                ?: throw IllegalStateException("Supabase sign-in succeeded but user is null")

            AuthUser(
                uid = user.id,
                displayName = user.userMetadata?.get("full_name")?.toString()
                    ?.removeSurrounding("\""),
                email = user.email,
                photoUrl = user.userMetadata?.get("avatar_url")?.toString()
                    ?.removeSurrounding("\""),
            )
        }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    /**
     * Updates the current user's profile metadata (display name, avatar URL) in Supabase Auth.
     */
    suspend fun updateProfile(
        displayName: String? = null,
        avatarUrl: String? = null,
    ): Result<AuthUser> = runCatching {
        supabase.auth.updateUser {
            data {
                displayName?.let { put("full_name", kotlinx.serialization.json.JsonPrimitive(it)) }
                avatarUrl?.let { put("avatar_url", kotlinx.serialization.json.JsonPrimitive(it)) }
            }
        }

        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("User is null after profile update")

        AuthUser(
            uid = user.id,
            displayName = user.userMetadata?.get("full_name")?.toString()
                ?.removeSurrounding("\""),
            email = user.email,
            photoUrl = user.userMetadata?.get("avatar_url")?.toString()
                ?.removeSurrounding("\""),
        )
    }
}
