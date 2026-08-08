package ng.name.gojodev.picnym.util

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import ng.name.gojodev.picnym.BuildConfig

suspend fun requestGoogleIdToken(context: Context): String {
    val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    if (clientId.isBlank() || clientId.startsWith("YOUR_")) {
        error("Add PICNYM_GOOGLE_WEB_CLIENT_ID to your Gradle properties before using Google sign-in.")
    }

    val googleOption = GetSignInWithGoogleOption.Builder(clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleOption)
        .build()
    val credential = CredentialManager.create(context)
        .getCredential(context = context, request = request)
        .credential

    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        error("Google returned an unsupported credential.")
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}

suspend fun clearGoogleCredentialState(context: Context) {
    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
}
