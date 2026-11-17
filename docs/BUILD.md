# Build, Test & Release

## Prerequisites
- JDK 17 or 21 (Temurin recommended)
- Android SDK: platform 36, build-tools 36 (set `ANDROID_HOME` or `local.properties` `sdk.dir`)
- Gradle 8.14.3 (wrapper provided)

## Debug
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
Debug uses `applicationIdSuffix .debug` and Google's **test ad units** — safe to install and review.

Install on a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Unit tests (JVM game logic)
```bash
./gradlew testDebugUnitTest
# report: app/build/reports/tests/testDebugUnitTest/index.html
```
Covers genetics (incl. negative controls), fusion matrix, battle determinism, economy, offline cap, save round-trip, content integrity.

## Release signing
Create an upload keystore (keep it secret, never commit):
```bash
keytool -genkeypair -v -keystore signing/slimeverse-upload.jks \
  -alias slimeverse -keyalg RSA -keysize 2048 -validity 10000
```
Create `keystore.properties` at the repo root (gitignored):
```
storeFile=signing/slimeverse-upload.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=slimeverse
keyPassword=YOUR_KEY_PASSWORD
```
A template is provided at `keystore.properties.example`.

## Release artifacts
```bash
./gradlew assembleRelease   # signed APK: app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease     # signed AAB: app/build/outputs/bundle/release/app-release.aab
```
Release enables R8 minification + resource shrinking with `app/proguard-rules.pro`.

If `keystore.properties` is absent, release builds are unsigned (CI-friendly) — add it before uploading to Play.

## Before publishing checklist
1. Set real AdMob App ID in `AndroidManifest.xml` and `RELEASE_*` unit IDs in `ads/AdConfig.kt`.
2. Create Play Console product `remove_ads_permanent` (non-consumable, $4.99).
3. Host a privacy policy (AdMob + UMP + Billing) and link it.
4. Complete the AdMob + Play Console data-safety forms.
5. Upload the AAB; use Play App Signing.
6. Provide store graphics from `aso/` and device screenshots.

## CI
`.github/workflows/android.yml` runs unit tests and builds a debug APK on push/PR.