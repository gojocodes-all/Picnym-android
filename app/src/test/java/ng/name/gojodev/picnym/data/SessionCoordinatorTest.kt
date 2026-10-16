package ng.name.gojodev.picnym.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCoordinatorTest {
    private val expired = StoredSession(
        accessToken = "expired-access",
        refreshToken = "refresh-1",
        userId = "user-1",
        email = "test@example.com"
    )
    private val refreshed = expired.copy(
        accessToken = "fresh-access",
        refreshToken = "refresh-2"
    )

    @Test
    fun concurrentFailuresShareOneSuccessfulRefresh() = runBlocking {
        val coordinator = SessionCoordinator()
        val refreshStarted = CompletableDeferred<Unit>()
        val finishRefresh = CompletableDeferred<Unit>()
        var stored = expired
        var refreshCalls = 0

        suspend fun refresh() = coordinator.refresh(
            failedAccessToken = expired.accessToken,
            currentSession = { stored },
            attemptRefresh = {
                refreshCalls += 1
                refreshStarted.complete(Unit)
                finishRefresh.await()
                RefreshAttempt.Success(refreshed)
            },
            saveSession = { stored = it },
            clearSession = { stored = StoredSession() }
        )

        val first = async { refresh() }
        refreshStarted.await()
        val second = async { refresh() }
        yield()
        finishRefresh.complete(Unit)

        assertEquals(refreshed, first.await())
        assertEquals(refreshed, second.await())
        assertEquals(1, refreshCalls)
        assertEquals(refreshed, stored)
    }

    @Test
    fun reusesSessionThatAnotherRequestAlreadyRefreshed() = runBlocking {
        var refreshCalled = false
        val result = SessionCoordinator().refresh(
            failedAccessToken = expired.accessToken,
            currentSession = { refreshed },
            attemptRefresh = {
                refreshCalled = true
                RefreshAttempt.Failure("")
            },
            saveSession = {},
            clearSession = {}
        )

        assertEquals(refreshed, result)
        assertFalse(refreshCalled)
    }

    @Test
    fun queuesSessionMutationBehindAnInFlightRefresh() = runBlocking {
        val coordinator = SessionCoordinator()
        val refreshStarted = CompletableDeferred<Unit>()
        val finishRefresh = CompletableDeferred<Unit>()
        var stored = expired

        val refresh = async {
            coordinator.refresh(
                failedAccessToken = expired.accessToken,
                currentSession = { stored },
                attemptRefresh = {
                    refreshStarted.complete(Unit)
                    finishRefresh.await()
                    RefreshAttempt.Success(refreshed)
                },
                saveSession = { stored = it },
                clearSession = { stored = StoredSession() }
            )
        }
        refreshStarted.await()
        val clear = async {
            coordinator.mutate { stored = StoredSession() }
        }
        yield()

        assertEquals(expired, stored)
        finishRefresh.complete(Unit)
        assertEquals(refreshed, refresh.await())
        clear.await()
        assertEquals(StoredSession(), stored)
    }

    @Test
    fun clearsOnlyForTerminalSupabaseAuthErrors() = runBlocking {
        val terminalCodes = listOf(
            "refresh_token_not_found",
            "refresh_token_already_used",
            "session_not_found",
            "session_expired",
            "user_not_found",
            "user_banned"
        )

        terminalCodes.forEach { code ->
            var cleared = false
            val result = SessionCoordinator().refresh(
                failedAccessToken = expired.accessToken,
                currentSession = { expired },
                attemptRefresh = { RefreshAttempt.Failure(code) },
                saveSession = {},
                clearSession = { cleared = true }
            )

            assertNull(result)
            assertTrue("Expected $code to clear the session", cleared)
        }
    }

    @Test
    fun preservesSessionForTransientOrUnknownAuthErrors() = runBlocking {
        listOf("", "over_request_rate_limit", "unexpected_failure").forEach { code ->
            var cleared = false
            val result = SessionCoordinator().refresh(
                failedAccessToken = expired.accessToken,
                currentSession = { expired },
                attemptRefresh = { RefreshAttempt.Failure(code) },
                saveSession = {},
                clearSession = { cleared = true }
            )

            assertNull(result)
            assertFalse("Did not expect $code to clear the session", cleared)
        }
    }

    @Test
    fun skipsRefreshWhenNoRefreshTokenIsStored() = runBlocking {
        var refreshCalled = false
        val result = SessionCoordinator().refresh(
            failedAccessToken = expired.accessToken,
            currentSession = { expired.copy(refreshToken = "") },
            attemptRefresh = {
                refreshCalled = true
                RefreshAttempt.Success(refreshed)
            },
            saveSession = {},
            clearSession = {}
        )

        assertNull(result)
        assertFalse(refreshCalled)
    }
}
