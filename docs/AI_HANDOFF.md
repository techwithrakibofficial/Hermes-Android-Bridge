# CURRENT HANDOFF — Hermes Android Bridge
Updated 2026-10-28, Asia/Dhaka.

## 1. Goal and constraints
Kotlin/Jetpack Compose Android companion for existing Hermes Agent v0.15.2 in Termux Ubuntu PRoot: voice/text discussion and Android actions via Accessibility and authenticated loopback. Free and phone-only; GitHub Actions APK workflow. Preserve Hermes config/state/memory and Telegram pairing. No root, ADB, Developer Options unlock or EMI bypass.

## 2. Finalized decisions
Existing Hermes remains the agent. Separate companion and optional adapter. No assumed Hermes API. First validate native Termux-to-companion Android actions, then inspect/connect supported Hermes interfaces.
Voice uses installed TTS engines, Bengali/English matching previews, saved voice/speed, neutral pitch and offline default; online voice only by explicit opt-in.
Bridge is explicitly started by user, hosted in enabled AccessibilityService and bound only to IPv4 127.0.0.1:18765. Fresh 256-bit token per session, memory only. Status GET and Home/Back POSTs only; no screen retrieval, gestures, arbitrary shell or sensitive actions.
Request IDs reserved before execution, no replay eviction, maximum 256 actions/session. Stop/unbind/destroy/interruption ends session. Restart changes token. Android acceptance does not prove visible completion; ambiguous timeouts must not be automatically retried.

## 3. Completed and verified
User-reported: Hermes/Telegram setup and Termux:API TTS; initial companion functions work; speed slider helped; latest voice controls now all okay. Individual Home/Back/disconnect tests were not itemized and gateway health was not independently checked.
Directly verified:
- Repository write access.
- 0.1.0/0.1.1/0.1.2 build/lint/release success.
- 0.2.0 code commit: 35595abb33466daa7d2584c78d2eac06848c2054
- 0.2.0 successful run: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40608250521
- Real loopback JVM test harness: 20 checks passed with FAKE Android action handler.
- Tests cover loopback binding, token format/authentication/rotation, browser-origin rejection, methods/allowlist, missing IDs, body rejection, duplicate headers, accepted actions, replay denial, header bounds, session capacity and rejected actions.
- assembleDebug/lintDebug, artifact upload and prerelease publication passed.
- Release: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-5
- APK: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-5/Hermes-Bridge-0.2.0.apk
- Version 0.2.0, versionCode 4, APK size 9337200 bytes.

## 4. Exact stopping point
0.2.0 APK released. Native Termux-to-phone status/Home/Back/Stop tests NOT performed yet. Actual Android background lifetime and stop/restart token rotation require device verification. No Hermes chat/adapter/microphone integration yet.

## 5. Outstanding and blockers
1. User test via docs/LOCAL_BRIDGE.md, without exposing tokens.
2. Read-only inspection of installed Hermes v0.15.2 supported tool/adapter interfaces, then separate adapter that preserves existing Telegram runtime.
3. Chat/task status and Android voice input with visible active state and cancellation.
4. Stable signing/monotonic release workflow; default debug signing may differ by CI runner.
5. Obtain exact Telegram gateway status if needed; do not infer health from old screenshot.

## 6. Single next action
Install 0.2.0, explicitly start bridge, copy pairing command to native Termux, run status then Home test, and report output/visible result without token.

## 7. Continue commands/files
docs/LOCAL_BRIDGE.md contains exact device commands, protocol and limits.
CI protocol tests: javac -d /tmp/hermes-protocol-test app/src/main/java/io/github/techwithrakibofficial/hermesbridge/LocalBridgeServer.java tests/LocalBridgeServerTest.java; java -cp /tmp/hermes-protocol-test LocalBridgeServerTest
Android: gradle --no-daemon :app:assembleDebug :app:lintDebug (Gradle 8.11.1/JDK17/SDK35).
Local scratch lacks javac; tests were run and verified in CI.
Do not repeat Hermes installation or Telegram pairing. Resolved first-run SDK issue: removed package tools, fixed by requesting platform-tools.
