# Hermes Android Bridge

Android companion for an existing Hermes Agent in Termux Ubuntu PRoot. Preserve the existing agent and Telegram gateway.

## Current milestone: device capability test

- Kotlin / Jetpack Compose app, Android 8+ (API 26), targeting API 35.
- Live Accessibility connection status, disclosure and manual Settings handoff.
- Explicit Home and Back buttons using Android global actions.
- Android TTS test with ready/speaking/completed/error states and stop control.
- No Internet, microphone, storage, screen-content or gesture capability requested in this milestone.
- Hermes chat, voice input and secure loopback bridge are **not implemented yet**.

## Phone-only workflow

Push code to main → GitHub Actions compiles and lints → successful build uploads APK artifact → publish job creates a development prerelease with a direct APK asset. No Android Studio or Developer Options required.

Open **Releases**, choose the newest development release, and download `Hermes-Bridge-0.1.0.apk`. Do not treat the presence of this README as evidence that a build passed. See `docs/BUILD_AND_TEST_STATUS.md`.

Development APK uses default Android debug signing. Different CI runners may use different keys, so in-place updates are not guaranteed. Stable signing/versioning is required before wider distribution. Never overwrite a release key with a debug key.

## Build locally or in CI

Use JDK 17, Android SDK platform 35/build-tools 35.0.0, and Gradle **8.11.1**:

```sh
gradle --no-daemon :app:assembleDebug :app:lintDebug
```

CI deliberately provisions the pinned Gradle distribution using `gradle/actions/setup-gradle`. It does not require an absent `gradlew` or an uploaded keystore. Gradle wrapper files can be generated and committed later from a verified distribution.

## Device test

See `docs/DEVICE_TEST.md`. If the EMI policy blocks Accessibility, record that result and stop that test. No root, ADB, restriction bypass, Telegram reconfiguration, or agent reinstallation.
