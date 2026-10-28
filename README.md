# Hermes Android Bridge

Kotlin/Jetpack Compose companion for the existing Hermes Agent in Android Termux
Ubuntu PRoot. Conversation stays in the existing paired Telegram bot; Android
operation uses a user-started authenticated127.0.0.1 bridge and19 MCP tools.

## Current development release0.4.0

[Download APK](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-12/Hermes-Bridge-0.4.0.apk) · [Phone upgrade guide](docs/UPGRADE_0_4.md) · [Current handoff](docs/AI_HANDOFF.md)

- Explicit screen-control opt-in, bounded UI tree and password-control omission.
- Fresh-target click, long press, scroll and editable text replacement; reobserve
  to verify. Snapshot expiry and state checks reject stale targets.
- Discover/open launchable apps, Settings, Home/Back/Recents, notification shade,
  Quick Settings, numbered dialer, basic device/battery state.
- Speak using saved engine/voice/language/speed; Stop speech and bridge session.
- Safe test activity for real-phone verification.
- Optional preserved companion preferences: natural warm Bengali, feminine persona,
  welcome affectionate tone, conversation, planning/coding and task verification.

CI compile/lint,46 fake-handler socket checks and59 Python tests on3.12/3.14
passed. **Actual0.4 phone actions and per-app workflows remain unverified.**
No live AI call audio, incoming AI telephone calls, standalone mic/chat UI,
hardware-repair guarantee, root/ADB/EMI bypass or unrestricted provider promise.
Installed TTS voices must be previewed to choose a clear feminine voice.
See [scope and tests](docs/COMPANION_0_4.md).

## Phone-only development

Push code → GitHub Actions → compile/lint/tests → APK prerelease. No Android Studio
or Developer Options needed. Preserve existing Hermes/provider/Telegram/memory.
Debug signing uses a cached development key; cache eviction may require a
Bridge-only reinstall. Stable release signing remains future work.

JDK17, Android SDK35/build-tools35.0.0, Gradle8.11.1:

```sh
gradle --no-daemon :app:assembleDebug :app:lintDebug
python3 -m unittest discover -s integrations/hermes_bridge -p 'test_*.py' -v
```

The bridge/installed offline TTS add no subscription. Existing inference-provider
limits/costs and online-voice terms remain. Android may stop background processes.
