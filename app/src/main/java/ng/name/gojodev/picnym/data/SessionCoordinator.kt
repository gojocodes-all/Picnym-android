package ng.name.gojodev.picnym.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed interface RefreshAttempt {
    data class Success(val session: StoredSession) : RefreshAttempt
    data class Failure(val errorCode: String) : RefreshAttempt
}

private val TERMINAL_REFRESH_ERRORS = setOf(
    "refresh_token_not_found",
    "refresh_token_already_used",
    "session_not_found",
    "session_expired",
    "user_not_found",
    "user_banned"
)

internal fun RefreshAttempt.Failure.invalidatesSession(): Boolean =
    errorCode in TERMINAL_REFRESH_ERRORS

internal class SessionCoordinator {
    private val mutex = Mutex()

    suspend fun <T> mutate(block: suspend () -> T): T = mutex.withLock { block() }

    suspend fun refresh(
        failedAccessToken: String,
        currentSession: suspend () -> StoredSession,
        attemptRefresh: suspend (StoredSession) -> RefreshAttempt,
        saveSession: suspend (StoredSession) -> Unit,
        clearSession: suspend () -> Unit
    ): StoredSession? = mutex.withLock {
        val current = currentSession()
        if (current.refreshToken.isBlank()) return@withLock null

        // Another request may have refreshed this session while this caller waited.
        if (current.accessToken.isNotBlank() && current.accessToken != failedAccessToken) {
            return@withLock current
        }

        when (val attempt = attemptRefresh(current)) {
            is RefreshAttempt.Success -> {
                saveSession(attempt.session)
                attempt.session
            }
            is RefreshAttempt.Failure -> {
                if (attempt.invalidatesSession()) clearSession()
                null
            }
        }
    }
}
