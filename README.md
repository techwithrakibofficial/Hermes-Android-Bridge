# Hermes Android Bridge / Companion 0.5.0

Native Kotlin/Jetpack Compose chat and voice companion for the existing Hermes
Agent in Android Termux Ubuntu PRoot. Existing Telegram, cron, saved TTS and 19
Android MCP tools remain available. No root, ADB or Developer Options required.

[Download APK — 0.5.0 dev-20](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-20/Hermes-Bridge-0.5.0.apk) · [Phone installation guide / one setup command](docs/UPGRADE_0_5.md) · [Master plan](docs/MASTER_PLAN.md) · [Current handoff](docs/AI_HANDOFF.md)

## Included

- Native chat, New chat/History, copyable replies, model/connection/task status.
- User-started microphone and foreground task service, notification Stop,
  turn-taking voice, interruption and saved TTS voice/rate. On-device speech by
  default; explicit network recognition opt-in when needed.
- No own beeps, overlapping recognizers, silence/error restart loop or automatic
  prompt replay. OEM recognizer sounds and language availability vary by device.
- Private localhost ACP backend using the existing Hermes and credentials;
  persistent job IDs, single active job, session restore, Allow once/Deny, cancel,
  safe provider/quota errors. Combined Termux launcher runs Telegram alongside it.
- Existing observed screen control, app catalog, Settings/Home/Back/Recents,
  notifications/Quick Settings, text/click/scroll, dialer and speech tools.

## Verified and pending

Release source `c1bdca0215d5386ff096cc6cc5591d900e0fb5a3` passed Android compile/lint,
124 Java checks and 80 Python tests on each of Python3.12/3.14. Exact Hermes
v2026.5.29.2 ACP/MCP integration and persisted session restore passed against local
LLM/Android fixtures. **These do not establish real-phone microphone/TTS success.**
0.4.x phone evidence includes real screen reads, package opening, app catalog,
Bengali draft/counter test, audible TTS and a user-confirmed Facebook read-only
workflow. New native0.5.0 phone acceptance is pending. See [build evidence](docs/BUILD_AND_TEST_STATUS.md).

This is a development acceptance APK. APK alone needs the matching private
backend; follow the guide. No promise of live AI carrier-call audio, every-app
workflow, hardware repair, unlimited free inference or full-duplex Gemini parity.

## Phone-only development

Push code → GitHub Actions → tests/compile/lint → APK prerelease. JDK17,
SDK35/build-tools35.0.0, Gradle8.11.1. Debug signing uses a cached development key;
cache eviction can require a Bridge-only reinstall. Stable signing remains future work.

```sh
gradle --no-daemon :app:assembleDebug :app:lintDebug
python3 -m unittest discover -s integrations/hermes_bridge -p 'test_*.py' -v
```

The bridge and installed offline TTS add no subscription. Provider/network speech
limits and terms apply. No hidden auto-start microphone; Android can stop
background processes. Keep credentials and pairing tokens out of issues/logs.
