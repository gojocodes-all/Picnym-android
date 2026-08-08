package ng.name.gojodev.picnym.data

import ng.name.gojodev.picnym.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val JSON = "application/json; charset=utf-8".toMediaType()

data class AuthOutcome(val signedIn: Boolean, val message: String)

class AuthRepository(private val store: SessionStore) {
    private fun request(path: String, payload: JSONObject, accessToken: String? = null): Request {
        val builder = Request.Builder()
            .url(BuildConfig.SUPABASE_URL + path)
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
        if (!accessToken.isNullOrBlank()) builder.header("Authorization", "Bearer $accessToken")
        return builder.build()
    }

    private suspend fun saveTokens(json: JSONObject): StoredSession? {
        val access = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        if (access.isBlank() || refresh.isBlank()) return null
        val user = json.optJSONObject("user")
        val session = StoredSession(
            accessToken = access,
            refreshToken = refresh,
            userId = user?.optString("id").orEmpty(),
            email = user?.optString("email").orEmpty()
        )
        store.saveSession(session)
        return session
    }

    suspend fun signIn(email: String, password: String): AuthOutcome {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val response = NetClient.execute(request("/auth/v1/token?grant_type=password", body))
        val json = response.requireSuccess()
        val session = saveTokens(json) ?: throw ApiException(401, "Sign in did not return a session.")
        return AuthOutcome(session.signedIn, "Signed in.")
    }

    suspend fun signUp(displayName: String, email: String, password: String): AuthOutcome {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("data", JSONObject().put("display_name", displayName.trim()))
        val response = NetClient.execute(request("/auth/v1/signup", body))
        val json = response.requireSuccess()
        val session = saveTokens(json)
        return if (session != null) AuthOutcome(true, "Account created.")
        else AuthOutcome(false, "Account created. Check your email to confirm it, then sign in.")
    }

    suspend fun signInWithGoogleIdToken(idToken: String): AuthOutcome {
        if (idToken.isBlank()) throw ApiException(400, "Google did not return a valid ID token.")
        val body = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)
        val response = NetClient.execute(request("/auth/v1/token?grant_type=id_token", body))
        val json = response.requireSuccess()
        val session = saveTokens(json) ?: throw ApiException(401, "Google sign-in did not return a session.")
        return AuthOutcome(session.signedIn, "Signed in with Google.")
    }

    suspend fun refresh(): StoredSession? {
        val current = store.current()
        if (current.refreshToken.isBlank()) return null
        val body = JSONObject().put("refresh_token", current.refreshToken)
        return try {
            val response = NetClient.execute(request("/auth/v1/token?grant_type=refresh_token", body))
            if (response.status !in 200..299) {
                store.clearSession()
                null
            } else saveTokens(response.json)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun token(): String? = store.current().accessToken.takeIf { it.isNotBlank() }

    suspend fun hasSession(): Boolean = store.current().signedIn

    suspend fun signOut() {
        val current = store.current()
        if (current.accessToken.isNotBlank()) {
            runCatching {
                val req = Request.Builder()
                    .url(BuildConfig.SUPABASE_URL + "/auth/v1/logout")
                    .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                    .header("Authorization", "Bearer ${current.accessToken}")
                    .post(ByteArray(0).toRequestBody(null))
                    .build()
                NetClient.execute(req)
            }
        }
        store.clearSession()
    }
}
