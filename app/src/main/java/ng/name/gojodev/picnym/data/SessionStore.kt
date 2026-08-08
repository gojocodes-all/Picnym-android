package ng.name.gojodev.picnym.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.picnymStore by preferencesDataStore(name = "picnym_native")

data class StoredSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val email: String = ""
) {
    val signedIn: Boolean get() = accessToken.isNotBlank() && refreshToken.isNotBlank()
}

class SessionStore(private val context: Context) {
    private object Keys {
        val access = stringPreferencesKey("access_token")
        val refresh = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val email = stringPreferencesKey("email")
        val theme = stringPreferencesKey("theme")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
    }

    val sessionFlow: Flow<StoredSession> = context.picnymStore.data.map { prefs ->
        StoredSession(
            accessToken = prefs[Keys.access].orEmpty(),
            refreshToken = prefs[Keys.refresh].orEmpty(),
            userId = prefs[Keys.userId].orEmpty(),
            email = prefs[Keys.email].orEmpty()
        )
    }

    val themeFlow: Flow<String> = context.picnymStore.data.map { it[Keys.theme] ?: "system" }
    val onboardingFlow: Flow<Boolean> = context.picnymStore.data.map { it[Keys.onboardingComplete] ?: false }

    suspend fun current(): StoredSession = sessionFlow.first()

    suspend fun saveSession(session: StoredSession) {
        context.picnymStore.edit { prefs ->
            prefs[Keys.access] = session.accessToken
            prefs[Keys.refresh] = session.refreshToken
            prefs[Keys.userId] = session.userId
            prefs[Keys.email] = session.email
        }
    }

    suspend fun clearSession() {
        context.picnymStore.edit { prefs ->
            prefs.remove(Keys.access)
            prefs.remove(Keys.refresh)
            prefs.remove(Keys.userId)
            prefs.remove(Keys.email)
        }
    }

    suspend fun saveTheme(theme: String) {
        context.picnymStore.edit { it[Keys.theme] = theme }
    }

    suspend fun hasCompletedOnboarding(): Boolean = onboardingFlow.first()

    suspend fun completeOnboarding() {
        context.picnymStore.edit { it[Keys.onboardingComplete] = true }
    }
}
