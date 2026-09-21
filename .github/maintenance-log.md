# Maintenance log

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
