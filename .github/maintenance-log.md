# Maintenance log

## 2026-10-09 — Expand the Android project guide

### Rationale

The root README named the native stack and product features but did not provide a complete onboarding path. Supported Android versions, first-time setup, runtime configuration boundaries, deep-link contracts, repository structure, CI artifacts, local-data behavior and contribution checks were difficult to discover without reading Gradle and Kotlin source.

### Files changed

- `README.md` — document verified requirements, setup, architecture, runtime configuration, navigation links, repository structure, validation, build outputs, local data, security and contribution guidance.
- `.github/maintenance-log.md` — record this documentation work.

### Validation

- Compared Android and JDK requirements with `app/build.gradle.kts` and the checked-in wrapper.
- Compared permissions, backup/cleartext policy and link hosts with `AndroidManifest.xml`.
- Compared routes and prompt handling with `PicnymApp.kt`.
- Compared storage and network claims with `SessionStore.kt` and `ApiSupport.kt`.
- Compared validation and artifact paths with `.github/workflows/android.yml` and `BUILDING.md`.
- Ran the hosted Android workflow: wrapper validation, JVM tests, Android lint, and debug/release APK builds.
- Reviewed the complete diff for accuracy, secrets, security guidance, compatibility and repository conventions.

### Risk

Low. This change updates documentation only. Application code, dependencies, configuration values, permissions, APIs, signing and runtime behavior are unchanged.

### Rollback

Revert this pull request to restore the shorter project overview.

## 2026-10-05 — Validate anonymous messages before submission

### Rationale

The public inbox composer allowed blank text, incomplete polls and missing media
to enter the send coroutine. These invalid submissions either reached the API
or failed only after the UI entered its sending state. The composer now reports
a specific local validation error before any network work begins.

### Files changed

- `app/src/main/java/ng/name/gojodev/picnym/util/InputRules.kt` — centralize
  validation for text, image, voice and poll submissions.
- `app/src/main/java/ng/name/gojodev/picnym/ui/screens/PublicInboxScreen.kt` —
  validate the current composer state before starting a send.
- `app/src/test/java/ng/name/gojodev/picnym/util/InputRulesTest.kt` — cover valid
  and incomplete states for every supported message type.
- `.github/maintenance-log.md` — record this maintenance work.

### Validation

- Ran the JVM unit tests.
- Ran Android lint for the debug variant.
- Built the debug and release APKs.
- Reviewed the complete diff for accessibility, security, compatibility and
  repository conventions.

### Risk

Low. Valid submissions use the existing API paths unchanged. Only submissions
that cannot produce a usable message are stopped locally with clearer feedback.

### Rollback

Revert the pull request's squash commit to restore server-side-only validation.

## 2026-09-28 — Pin the local and CI Gradle toolchain

### Rationale

The Android plugin was versioned, but the repository had no Gradle Wrapper.
Developers had to install a compatible global Gradle version themselves, while
CI installed Gradle 8.13 through separate workflow configuration. A checked-in
wrapper now makes the build entrypoint reproducible across local development
and CI.

### Files changed

- `gradlew`, `gradlew.bat` and `gradle/wrapper/*` — add the Gradle 8.13
  wrapper, pin the official distribution checksum and allow slower connections
  up to 60 seconds.
- `.gitattributes` — preserve the generated Unix and Windows launcher line
  endings and treat the wrapper JAR as binary.
- `.github/workflows/android.yml` — validate the wrapper and run the wrapper
  instead of a separately installed Gradle executable.
- `README.md` and `BUILDING.md` — document the shared Unix and Windows build
  commands and remaining JDK/SDK requirements.
- `.github/maintenance-log.md` — record this maintenance work.

### Validation

- Verified the downloaded Gradle 8.13 distribution against Gradle's published
  SHA-256 checksum.
- Generated the wrapper with Gradle 8.13 and verified the wrapper JAR checksum.
- Ran `./gradlew --version`.
- Ran JVM tests, Android lint, and debug/release APK builds through the wrapper.
- Reviewed the complete diff for security, compatibility and repository
  conventions.

### Risk

Low. Application source, dependencies, SDK targets and APK behavior are
unchanged. The existing Gradle 8.13 CI version is now repository-controlled and
checksum-verified.

### Rollback

Revert the pull request's squash commit to remove the wrapper and restore the
workflow's direct Gradle installation.

## 2026-09-21 — Preserve existing-account sign-in

### Rationale

The authentication screen applied the eight-character new-account password rule to sign-in attempts. Existing accounts with a shorter password could therefore be rejected by the Android client before their credentials reached Supabase.

### Files changed

- `app/src/main/java/ng/name/gojodev/picnym/ui/screens/AuthScreen.kt` — apply the password-length rule only while creating an account and show mode-appropriate validation feedback.
- `app/src/main/java/ng/name/gojodev/picnym/util/InputRules.kt` — centralize the mode-aware password validation rule.
- `app/src/test/java/ng/name/gojodev/picnym/util/InputRulesTest.kt` — cover both account creation and existing-account sign-in behavior.
- `.github/maintenance-log.md` — record this maintenance work.

### Validation

- Ran the JVM unit tests.
- Ran Android lint for the debug variant.
- Built the debug APK.
- Reviewed the complete diff for accessibility, security, and compatibility.

### Risk

Low. Account creation still requires at least eight password characters. Sign-in now rejects only blank passwords and otherwise lets Supabase validate the existing account credentials.

### Rollback

Revert the pull request's squash commit to restore the previous client-side sign-in validation.
