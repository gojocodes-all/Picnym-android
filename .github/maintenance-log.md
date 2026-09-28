# Maintenance log

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
