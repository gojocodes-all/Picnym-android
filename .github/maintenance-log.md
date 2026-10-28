# Maintenance log

## 2026-10-28 — Make Android CI immutable and bounded

### Rationale

The Android workflow used movable major-version tags for every external action.
Those tags can change the code executed by a previously reviewed workflow
without a repository diff. The job also retained checkout credentials, allowed
duplicate runs for superseded commits and inherited GitHub's six-hour default
timeout. CI now executes verified action commits with an explicitly bounded,
least-privilege lifecycle.

### Files changed

- `.github/workflows/android.yml` — pin all actions to immutable upstream
  commits, disable checkout credential persistence, cancel superseded runs and
  cap the build job at 30 minutes.
- `BUILDING.md` — document the CI safeguards and the required action-update
  procedure.
- `.github/maintenance-log.md` — record the change, validation, risk and
  rollback.

### Validation

- Resolved each configured upstream release tag through GitHub's Git data API;
  the Gradle Actions `v6.4.1` tag has a valid signature.
- Parsed the workflow as YAML and verified that every `uses:` reference is a
  full 40-character commit SHA.
- Attempted the repository's full Gradle validation command locally; the
  sandbox could not reach `services.gradle.org`.
- The pull request's hosted Android workflow validated the wrapper, ran the JVM
  unit tests and Android lint, built both APK variants and uploaded both
  artifacts successfully.
- Reviewed the complete diff for credential exposure, permissions, build
  behavior, backward compatibility and repository conventions.

### Risk

Low. Application source, dependencies, API behavior, SDK targets and artifact
paths are unchanged. The workflow still runs the same Gradle tasks with the
same action major versions; only their exact revisions and execution bounds are
now explicit.

### Rollback

Revert this pull request to restore movable major-version action tags and the
previous unbounded workflow lifecycle; no application or data rollback is
required.

## 2026-10-21 — Normalize account identity fields before authentication

### Rationale

The email and display-name fields accepted accidental leading or trailing whitespace, then sent those raw values to Supabase. Mobile keyboards and pasted credentials can introduce invisible padding, causing valid sign-in details to fail or creating a visibly padded profile name. Authentication now trims identity fields before validation and submission while leaving passwords unchanged.

### Files changed

- `app/src/main/java/ng/name/gojodev/picnym/util/InputRules.kt` — add focused normalization helpers for authentication email and display name.
- `app/src/main/java/ng/name/gojodev/picnym/ui/screens/AuthScreen.kt` — validate, display and submit normalized identity fields.
- `app/src/test/java/ng/name/gojodev/picnym/util/InputRulesTest.kt` — cover padded email and display-name input.
- `.github/maintenance-log.md` — record the change, validation, risk and rollback.

### Validation

- Ran the JVM unit tests.
- Ran Android lint for the debug variant.
- Built the debug and unsigned release APKs.
- Reviewed the complete diff for authentication behavior, password preservation, accessibility, security, backward compatibility and repository conventions.

### Risk

Low. Only surrounding whitespace is removed from email and display name at submission. Passwords, Google sign-in, API routes, dependencies, session storage and valid unpadded credentials are unchanged.

### Rollback

Revert this pull request to restore raw email and display-name submission; no data migration or credential change is required.

## 2026-10-16 — Make session refresh atomic and failure-aware

### Rationale

Authenticated screens can start overlapping API calls, including an initial load and a user-requested refresh. When an access token expired, each `401` previously exchanged the same rotating Supabase refresh token independently. The refresh path also cleared local credentials for every non-success response, including rate limits and temporary server failures, and an explicitly anonymous request could acquire credentials during its retry. Session mutations are now serialized, successful refreshes are shared by callers that failed with the same token, and only documented terminal Auth error codes invalidate the stored session.

### Files changed

- `app/src/main/java/ng/name/gojodev/picnym/data/SessionCoordinator.kt` — serialize session mutations, coalesce successful refreshes and classify terminal refresh failures.
- `app/src/main/java/ng/name/gojodev/picnym/data/AuthRepository.kt` — route sign-in, refresh, account-deletion cleanup and sign-out through the coordinator, and preserve sessions after transient Auth failures.
- `app/src/main/java/ng/name/gojodev/picnym/data/PicnymApi.kt` — identify the rejected access token and retry only requests that originally carried credentials.
- `app/src/test/java/ng/name/gojodev/picnym/data/SessionCoordinatorTest.kt` — cover concurrent refreshes, queued session mutation, stale-token reuse, terminal/transient failures and missing refresh tokens.
- `README.md` — document the session-refresh boundary and retry guarantees.
- `.github/maintenance-log.md` — record this maintenance work.

### Validation

- Ran the JVM unit tests.
- Ran Android lint for the debug variant.
- Built the debug and unsigned release APKs.
- Reviewed the complete diff for session security, anonymous-request isolation, backward compatibility and repository conventions.

### Risk

Low. API routes, request payloads, dependencies and successful sign-in behavior are unchanged. The change only serializes local session writes, avoids duplicate refresh exchanges and retains credentials after failures that do not prove the session is invalid.

### Rollback

Revert this pull request's squash commit to restore independent refresh attempts and the previous non-success handling.

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
