# PICNYM Android

Native Android client for PICNYM.

This repository intentionally contains **no WebView wrapper and no copied website bundle**. The UI is written in Kotlin with Jetpack Compose and talks directly to the existing PICNYM v4 API and Supabase Auth.

The web application remains in its separate repository and is not modified by this Android project.

## Architecture decision

- Kotlin and Jetpack Compose remain the smallest stable native stack for the existing Android product and deep-link contract.
- OkHttp talks directly to the established PICNYM v4 Edge Function; Credential Manager handles Google sign-in without a WebView.
- DataStore persists the minimum local session, theme and onboarding state. Android backups are disabled so session data is not copied into cloud backups.

Significant dependencies: Compose Material 3 for accessible native controls, Navigation Compose for routes and verified deep links, Credential Manager plus Google ID for authentication, Coil for remote profile/media images, OkHttp for API and upload calls, Coroutines for structured asynchronous work, and DataStore for local preferences. No dependency is duplicated by another library with the same job.

## Current product surface

- three-step first-run message-desk introduction
- 18+ create-account and sign-in flow
- Google sign-in through Android Credential Manager and Supabase ID-token exchange
- email/password authentication
- text, photo, native voice-note and poll messages
- conversation prompt deck
- multiple inboxes, profiles and friends
- hidden words, link pausing, account-only and friend-only controls
- reporting, blocking, favorites, archive and public answers
- light/dark theme and the shared PICNYM paper-and-redaction visual system

## Build and verify

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Version 3.0.0 uses the same API and account data as the web client; no database migration is required for this visual release.

See `BUILDING.md` for the one-time Google Cloud and Supabase provider setup.
