# PICNYM Android

PICNYM Android is the native Android client for PICNYM, an anonymous messaging and social-inbox product. The interface is written in Kotlin with Jetpack Compose and talks directly to the existing PICNYM v4 API and Supabase Auth.

This repository intentionally contains no WebView wrapper or copied website bundle. The web application remains in its own repository.

## Product surface

The current Android client provides:

- a three-step first-run introduction;
- email/password account creation and sign-in for users aged 18 or older;
- Google sign-in through Android Credential Manager and Supabase ID-token exchange;
- multiple anonymous inboxes, profiles, friends, favorites, archives, reports and blocking;
- text, photo, native voice-note and poll submissions;
- conversation prompts and public answers;
- hidden-word, link-pausing, account-only and friend-only controls;
- system, light and dark themes using PICNYM's paper-and-redaction visual system;
- verified web links and custom-scheme links for inboxes, profiles and polls.

Version 3.0.0 uses the existing PICNYM API and account data. It does not introduce a separate database or migration.

## Requirements

For development:

- a current Android Studio release;
- JDK 17;
- Android SDK 36;
- an internet connection for Gradle dependencies and the live PICNYM backend;
- a device or emulator running Android 8.0 (API 26) or newer.

Use the checked-in Gradle Wrapper. A separate global Gradle installation is not required.

## Get started

1. Clone and enter the repository:

   ```bash
   git clone https://github.com/gojocodes-all/Picnym-android.git
   cd Picnym-android
   ```

2. Open the repository root in Android Studio and allow Gradle to sync.

3. Install Android SDK 36 if Android Studio requests it. Confirm that Gradle uses JDK 17.

4. Run the app configuration on an Android 8.0+ emulator or device.

For a command-line debug build:

```bash
./gradlew :app:assembleDebug
```

On Windows Command Prompt, use `gradlew.bat` in place of `./gradlew`.

See [BUILDING.md](BUILDING.md) for Google Cloud and Supabase provider setup, APK paths and release-signing guidance.

## Architecture

| Layer | Responsibility |
| --- | --- |
| Jetpack Compose | Native screens, controls and the shared visual system |
| Navigation Compose | Onboarding, authentication, home, account, inbox, profile, poll and dashboard routes |
| `PicnymApi` / OkHttp | PICNYM v4 API requests, uploads and response handling |
| `AuthRepository` | Email/password auth, Google ID-token exchange and session refresh |
| DataStore / `SessionStore` | Access and refresh tokens, user identity, theme and onboarding state |
| Android Credential Manager | Native Google account selection |
| Coil | Remote profile and media images |
| Coroutines | Asynchronous API, storage and media work |

The app selects its initial route from local onboarding and session state. Authenticated users enter the home screen; returning signed-out users enter authentication; new installations see onboarding first.

Network calls use HTTPS. The shared OkHttp client uses a 20-second connection timeout and 90-second read/write timeouts. Non-success API responses are converted into `ApiException` messages at the data boundary.

## Runtime configuration

The app's existing service endpoints and public client configuration are defined as `BuildConfig` fields in `app/build.gradle.kts`:

- `API_BASE` — PICNYM v4 Supabase Edge Function;
- `SUPABASE_URL` — Supabase project URL;
- `SUPABASE_PUBLISHABLE_KEY` — public client key intended for app use;
- `SITE_URL` — canonical PICNYM website used by verified links;
- `GOOGLE_WEB_CLIENT_ID` — Google OAuth web client ID.

Override the Google web client ID without editing tracked source by adding this to your user-level Gradle properties:

```properties
PICNYM_GOOGLE_WEB_CLIENT_ID=123456789-example.apps.googleusercontent.com
```

You can also pass `-PPICNYM_GOOGLE_WEB_CLIENT_ID=...` to Gradle. Never place a Google client secret, Supabase service-role key, signing key or other server credential in Gradle properties committed to this repository, `BuildConfig`, or an APK.

## Links and navigation

The manifest and navigation graph accept these routes:

| Destination | Verified HTTPS link | Custom scheme |
| --- | --- | --- |
| Public inbox | `https://anonymous.gojodev.name.ng/u/{slug}` | `picnym://u/{slug}` |
| Public profile | `https://anonymous.gojodev.name.ng/profile/{username}` | `picnym://profile/{username}` |
| Poll | `https://anonymous.gojodev.name.ng/poll/{slug}` | `picnym://poll/{slug}` |

Inbox links may include a `prompt` query value. The app limits the initial prompt passed into the composer to 180 characters.

Keep the application ID `ng.name.gojodev.picnym`, website hosts and route patterns synchronized with the deployed website and Android asset-link configuration when changing this contract.

## Project structure

```text
app/
├── build.gradle.kts                         Android SDK, version and dependency configuration
└── src/
    ├── main/
    │   ├── AndroidManifest.xml              permissions, launcher activity and deep links
    │   ├── java/ng/name/gojodev/picnym/
    │   │   ├── data/                        API, auth, models and local session storage
    │   │   ├── ui/                          navigation, components, screens and theme
    │   │   ├── util/                        input rules, Google sign-in and native media helpers
    │   │   └── MainActivity.kt              Compose application entry point
    │   └── res/                             icons, theme resources and FileProvider paths
    └── test/                                deterministic JVM unit tests
gradle/wrapper/                              pinned Gradle 8.13 wrapper
.github/workflows/android.yml                pull-request and main-branch validation
BUILDING.md                                  provider setup, artifacts and signing
```

## Validation

Run the same test, lint and build tasks used by GitHub Actions:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

This validates deterministic input rules, Android lint, the debug APK and the unsigned release APK. CI also validates the checked-in Gradle Wrapper and uploads both APK variants as workflow artifacts.

Build outputs:

- debug: `app/build/outputs/apk/debug/app-debug.apk`;
- unsigned release: `app/build/outputs/apk/release/app-release-unsigned.apk`.

The repository does not contain release signing keys or a tracked signing configuration. Follow [BUILDING.md](BUILDING.md) before distributing a release build.

## Local data, permissions and security

- The app requests internet access and microphone access. Microphone access supports native voice notes.
- Session tokens, user identity, theme and onboarding state are stored with Android DataStore.
- Android application backups are disabled, so that local session data is not copied through the platform backup mechanism.
- Cleartext HTTP traffic is disabled.
- The FileProvider used for media sharing is not exported and grants access only through explicit URI permissions.
- The checked-in Supabase key is a publishable client key. Privileged server secrets do not belong in a mobile application.

Use test accounts and non-sensitive media while developing. Do not commit credentials, local Gradle properties, keystores, generated APKs, or user data.

## Contributing

Keep changes focused and preserve the native-client boundary: do not add a WebView or copy the web application into this repository.

Before opening a pull request:

1. run the complete validation command;
2. add or update JVM tests when changing deterministic validation or transformation logic;
3. confirm new permissions, deep links and exported Android components are necessary and least-privilege;
4. verify authenticated and signed-out navigation when changing session or route behavior;
5. document configuration, provider or build changes in this README or [BUILDING.md](BUILDING.md);
6. record maintenance work in `.github/maintenance-log.md`.

Changes requiring live Supabase, Google OAuth, media upload or verified-link behavior should also be exercised with a non-production account because the unit suite does not contact those services.
