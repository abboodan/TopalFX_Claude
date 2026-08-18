# TopalFX Pro

Professional FX & Remittance margin calculator — Kotlin + Jetpack Compose (Material 3).

حاسبة هوامش صرف وتحويلات احترافية — عربي (افتراضي) + إنجليزي، مع أسعار حية وتحديث تلقائي داخل التطبيق.

## Features / المزايا

- **3 calculation modes**: Send Exact (إرسال محدد), Receive Exact (استلام محدد), Custom Deal (صفقة خاصة)
- **4 directions**: EUR➔USD, USD➔EUR, EUR➔EUR, USD➔USD (same-currency locked at 1.0000)
- **Deduction base toggle** (أساس التنزيل): office % cost on received amount or on delivered target, always rounded to a whole integer
- **Live rates ticker** (Frankfurter API) — auto-refresh every 30s, offline-safe (shows `--`), editable pairs
- **In-app auto-updater** from GitHub Releases with APK install via FileProvider
- **Arabic (default) / English**, RTL, runtime toggle

## Release setup (one-time) / إعداد النشر (مرة واحدة)

The in-app updater installs new APKs over the existing app. **Android requires every
release to be signed with the SAME keystore forever** — if the signature changes,
updates fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

المحدّث الداخلي يثبّت النسخ الجديدة فوق التطبيق الحالي، لذلك **يجب توقيع كل الإصدارات
بنفس الـ keystore دائماً** وإلا يفشل التحديث.

A release keystore was generated locally at `app/keystore/release.keystore`
(gitignored — **back it up somewhere safe!**). Its credentials are in
`app/keystore/keystore.properties` (also gitignored).

### GitHub Secrets

Create these 4 repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_B64` | contents of `app/keystore/release.keystore.b64` |
| `RELEASE_KEYSTORE_PASSWORD` | from `app/keystore/keystore.properties` |
| `RELEASE_KEY_ALIAS` | `topalfx` |
| `RELEASE_KEY_PASSWORD` | from `app/keystore/keystore.properties` |

### Release checklist / خطوات إصدار نسخة

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
   — `versionName` **must match the tag** (tag `v1.2.0` ⇒ versionName `"1.2.0"`).
2. Commit and push.
3. Tag and push the tag:
   ```
   git tag v1.2.0
   git push origin v1.2.0
   ```
4. GitHub Actions builds the signed APK and publishes the release automatically.
5. Installed apps detect the new version via the update button (or on next launch).

## Development

```
./gradlew testDebugUnitTest   # margin engine + semver unit tests
./gradlew assembleDebug       # debug build
```

Local release builds fall back to debug signing unless the `RELEASE_KEYSTORE_*`
environment variables are set.
