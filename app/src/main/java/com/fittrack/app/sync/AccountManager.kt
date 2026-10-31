package com.fittrack.app.sync

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Sign-in state for cloud sync. Phase 1 supports email + password, which
 * needs no provider setup; Google / Apple sign-in can be layered on later.
 * Sessions persist across launches via the Supabase client's own storage.
 */
class AccountManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _email = MutableStateFlow<String?>(null)
    /** Signed-in user's email, or null when signed out / unconfigured. */
    val email: StateFlow<String?> = _email

    val isSignedIn: Boolean get() = _email.value != null

    init {
        SupabaseProvider.client?.let { client ->
            scope.launch {
                client.auth.sessionStatus.collect { status ->
                    _email.value = when (status) {
                        is SessionStatus.Authenticated -> status.session.user?.email
                        else -> null
                    }
                }
            }
        }
    }

    /** Returns null on success, otherwise a user-facing error message. */
    suspend fun signIn(email: String, password: String): String? {
        val client = SupabaseProvider.client ?: return "Sync isn't configured in this build."
        return runCatching {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.exceptionOrNull()?.let { friendly(it) }
    }

    /**
     * Create an account. Returns null on success. If the project requires
     * email confirmation, the user is told to check their inbox.
     */
    suspend fun signUp(email: String, password: String): String? {
        val client = SupabaseProvider.client ?: return "Sync isn't configured in this build."
        return runCatching {
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.exceptionOrNull()?.let { friendly(it) }
    }

    suspend fun signOut() {
        SupabaseProvider.client?.let { runCatching { it.auth.signOut() } }
        _email.value = null
    }

    private fun friendly(t: Throwable): String {
        val m = t.message.orEmpty()
        return when {
            m.contains("Invalid login", true) -> "Wrong email or password."
            m.contains("already registered", true) -> "That email already has an account. Sign in instead."
            m.contains("Password should be", true) -> "Password must be at least 6 characters."
            m.contains("Unable to resolve host", true) || m.contains("timeout", true) -> "No connection. Try again when you're online."
            m.isBlank() -> "Something went wrong. Try again."
            else -> m.take(140)
        }
    }
}
