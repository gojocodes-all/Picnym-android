# Building PICNYM Android

PICNYM Android is a native Kotlin + Jetpack Compose application. It does not use a WebView.

## Android Studio

1. Clone `gojocodes-all/Picnym-android`.
2. Open the repository root in a current Android Studio release.
3. Use JDK 17.
4. Let Gradle sync and install Android SDK 36 if Android Studio asks.
5. Build `app`.

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
