# CURRENT HANDOFF — Hermes Android Bridge
Updated 2026-10-28, Asia/Dhaka.

## 1. Goal and constraints
Kotlin/Jetpack Compose companion for the existing Hermes Agent v0.15.2 in Termux Ubuntu PRoot: voice/text discussion and Android actions through Accessibility and a secure loopback bridge. Free, phone-only, GitHub Actions APK workflow. Preserve Hermes config/memory/state and Telegram pairing. No root, ADB, Developer Options unlock or EMI bypass.

## 2. Finalized decisions
Keep the existing Hermes runtime and separate companion/adapter. Start with device capability tests, then authenticated loopback commands, then a supported Hermes adapter. Never invent a Hermes endpoint. Do not equate spoken response with completed Android action.
Use installed Android TTS engines for the current voice preview. App-specific engine/language/voice preferences; no system/Termux changes. Google engine is preferred only if installed and there is no saved engine. Default language Bengali, neutral pitch, retained speed slider. Online voices require explicit opt-in; text may be sent to the engine provider.

## 3. Completed and verified
User-reported:
- Hermes and Telegram configured/paired; native Termux:API TTS succeeded.
- Initial companion functions work; no itemized Home/Back/disconnect report.
- 0.1.1 speed slider helped, but voice sounds strange/unintelligible.
Directly verified:
- GitHub write access and source commits.
- Protected AccessibilityService with explicit Home/Back controls, no screen retrieval/gesture capabilities.
- Previous 0.1.0 and 0.1.1 compile/lint and development releases succeeded.
- Latest 0.1.2 compile/lint, artifact upload and prerelease publication SUCCESS.
- Code commit: 406db52e335792b32af1d067c68cb1ab96bc6b7f
- Run: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40607155921
- Release: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-4
- APK: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-4/Hermes-Bridge-0.1.2.apk
- VersionCode 3; APK size 9,320,708 bytes.
Implemented 0.1.2: installed engine chooser; Bengali/English matching voices and previews; saved voice selection; engine quality/locale sorting; missing-data errors and install/refresh; offline default; online opt-in; neutral pitch 1.0 and persisted speed.

## 4. Exact stopping point
0.1.2 APK released, awaiting phone listening tests. Actual phone engine and voice are unknown. Humanlike quality, selected-voice persistence and engine switching have NOT been verified on a phone. CI verifies compilation/lint, not naturalness or sound.
Hermes chat, microphone input, remote actions and secure bridge are NOT implemented.

## 5. Outstanding and blockers
- Install/update a suitable TTS engine and Bengali voice data if missing; choose and preview voices. See docs/VOICE_SETUP.md.
- User must report engine label and which voice sounds clear; no naturalness guarantee from quality metadata.
- Obtain explicit individual device capability tests if needed before remote actions.
- Implement/test authenticated loopback bridge and supported Hermes v0.15.2 adapter without changing existing gateway.
- Add chat and voice input/active-state/cancel feedback.
- Stable APK signing/versioning before distribution. Debug runner keys may differ.
- Latest Telegram screenshot does not establish current gateway health.

## 6. Single next action
Install 0.1.2; choose Google engine if installed, Bengali language and an available voice; preview, then report clarity and engine/voice label.

## 7. Continue
Read docs/VOICE_SETUP.md, BUILD_AND_TEST_STATUS.md, TECHNICAL_DECISIONS.md and PROJECT_PLAN.md. Build with Gradle 8.11.1/JDK17/SDK35: gradle --no-daemon :app:assembleDebug :app:lintDebug.
Do not repeat Hermes installation or Telegram pairing.
Resolved CI issue: first run 40604912403 requested removed SDK package tools; explicitly requesting platform-tools fixed it.

## Current milestone — 0.2.0 (supersedes previous stopping point)
User reports voice now all okay. Proceeding to native Termux → Android secure local bridge. Implemented explicit Start/Stop in AccessibilityService, IPv4 loopback 18765, fresh 256-bit in-memory token per session, authenticated GET status and POST Home/Back, replay rejection with 256-action bounded session, header/read limits, main-thread bounded dispatch. No chat/agent adapter yet. Existing Hermes/Telegram untouched.
Exact stopping point: code prepared; JVM protocol tests and APK compile/lint pending CI. Local workspace lacks javac. Next: verify CI/release, then user follows docs/LOCAL_BRIDGE.md and reports status/Home/Back/Stop results without secrets. Voice data choices and speed remain as previously implemented.
