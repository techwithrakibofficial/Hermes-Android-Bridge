# CURRENT HANDOFF — Hermes Android Bridge
Updated 2026-10-28, Asia/Dhaka.

## 1. Goal and constraints
Companion app for the user's existing Hermes Agent, enabling voice/text discussion and Android actions through Accessibility and an authenticated loopback bridge. Kotlin/Jetpack Compose; free and phone-only; GitHub Actions APK builds. No root, ADB, Developer Options unlock or EMI bypass. Preserve existing Hermes files, memory, Telegram bot/pairing and configuration.

## 2. Finalized decisions
- Keep existing Hermes v0.15.2 in Termux Ubuntu PRoot.
- Separate companion app and optional adapter.
- First milestone validates actual device Accessibility and Android TTS capability.
- Do not assume that EMI-locked Developer Options mean Accessibility is blocked or available.
- Implement secure loopback and Hermes adapter only after device capability results and inspection of supported Hermes interfaces.
- No arbitrary shell endpoint, public listener, hardcoded tokens or invented Hermes API.

## 3. Completed and verified
User-reported (not independently retested): Hermes installed; Telegram paired/working; native Termux:API TTS succeeded.
Independently verified:
- Repository write access now works.
- Compose companion source, protected AccessibilityService, Home/Back manual tests and Android TTS test committed.
- Static XML/YAML checks passed. No app network/microphone/storage permission; no screen retrieval/gesture capability.
- CI commit: 3826ff409062c592bbc59a131fc39dd756301d73
- CI run: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40604989249
- Run conclusion: SUCCESS, including assembleDebug, lintDebug, artifact upload and prerelease publication.
- Development APK: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-2/Hermes-Bridge-0.1.0.apk
- Release: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-2
- APK asset size: 9,287,944 bytes.

## 4. Exact stopping point
First development APK is ready. Device installation, screen appearance, audible TTS and actual Accessibility actions have NOT been tested. Waiting for the user's phone results. Hermes connectivity remains explicitly unavailable in the app.

## 5. Outstanding tasks and blockers
- User device tests in docs/DEVICE_TEST.md.
- EMI policy may block Accessibility; availability unknown.
- Secure loopback bridge, pairing/authentication, protocol/security tests.
- Supported Hermes v0.15.2 integration inspection and optional adapter.
- Chat, microphone input and voice session controls.
- Stable signing/versioning before wider distribution. Current APK uses default debug signing and future runner keys may differ.
- CI reports action runtime deprecation warnings; successful build is verified, future action maintenance remains.

## 6. Single next action
Install dev-2 APK and report Accessibility enablement, Home/Back visible behavior, audible TTS, and whether existing Telegram still works.

## 7. Continuation commands and files
- Device procedure: docs/DEVICE_TEST.md.
- Build command: gradle --no-daemon :app:assembleDebug :app:lintDebug (Gradle 8.11.1/JDK 17/SDK 35).
- Read docs/BUILD_AND_TEST_STATUS.md, TECHNICAL_DECISIONS.md and PROJECT_PLAN.md before implementation.
- Do not reinstall Hermes or repeat Telegram pairing.
- First CI run 40604912403 failed before compilation because setup-android requested removed SDK package 'tools'; fixed with explicit platform-tools. No need to revisit this resolved issue.
