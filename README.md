# PICNYM Android

Native Android client for PICNYM.

This repository intentionally contains **no WebView wrapper and no copied website bundle**. The UI is written in Kotlin with Jetpack Compose and talks directly to the existing PICNYM v4 API and Supabase Auth.

The web application remains in its separate repository and is not modified by this Android project.

## Current product surface

- animated three-card first-run introduction
- redesigned 18+ create-account and sign-in flow
- Google sign-in through Android Credential Manager and Supabase ID-token exchange
- email/password authentication
- text, photo, native voice-note and poll messages
- conversation prompt deck
- multiple inboxes, profiles and friends
- hidden words, link pausing, account-only and friend-only controls
- reporting, blocking, favorites, archive and public answers
- light/dark theme and a redesigned native Compose visual system

See `BUILDING.md` for the one-time Google Cloud and Supabase provider setup.
