# Building PICNYM Android

PICNYM Android is a native Kotlin + Jetpack Compose application. It does not use a WebView.

## Android Studio

1. Clone `gojocodes-all/Picnym-android`.
2. Open the repository root in a current Android Studio release.
3. Use JDK 17.
4. Let Gradle sync and install Android SDK 36 if Android Studio asks.
5. Build `app`.

## Google sign-in setup

The app already uses Android Credential Manager and exchanges the Google ID token with Supabase Auth. To activate it:

1. In Google Cloud, configure the OAuth consent screen.
2. Create a **Web application** OAuth client. Add `https://ahvusnmuyfvdzjmdkgzj.supabase.co/auth/v1/callback` as an authorized redirect URI.
3. Create an **Android** OAuth client for package `ng.name.gojodev.picnym` and add the SHA-1 fingerprint for the certificate used to sign that build.
4. In Supabase Dashboard → Authentication → Providers → Google, enable Google and enter the Web client ID and its client secret.
5. Put the Web client ID—not the secret—in your user Gradle properties:

```properties
PICNYM_GOOGLE_WEB_CLIENT_ID=123456789-example.apps.googleusercontent.com
```

You can also pass it to a build with `-PPICNYM_GOOGLE_WEB_CLIENT_ID=...`. Never put the Google client secret in this repository, `BuildConfig`, or an APK.

For release builds, add the release/App Signing SHA-1 to the Android OAuth client as well as the debug SHA-1 used during local testing.

## APK outputs

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release (unsigned until you add your signing config): `app/build/outputs/apk/release/app-release-unsigned.apk`

GitHub Actions also builds both variants and uploads them as workflow artifacts on pushes to `main`.

## Signing

Signing keys are intentionally not committed. Keep your keystore private. You can sign the release through Android Studio's **Build > Generate Signed App Bundle or APK** flow, or add a private local signing configuration later.

## Backend

The Android client uses the same live PICNYM infrastructure as the web client:

- PICNYM API v4 on Supabase Edge Functions
- Supabase Auth
- Existing Postgres + Storage backend

Only the public Supabase publishable key is present in the app. No service-role key or server secret belongs in an APK.
