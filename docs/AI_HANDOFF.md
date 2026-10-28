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
User explicitly confirms remote Back works and the post-Stop status request fails to connect as expected. User previously replied Done after status/Home instructions; no raw output or screen evidence for those two was returned. These are user-reported device results, not independently observed Android tests.
Native Termux-to-Android bridge validation has progressed; next inspect existing Ubuntu PRoot/Hermes environment read-only before writing an adapter. Keep current bridge stopped until needed. No Hermes adapter/chat/microphone exists yet.

## 5. Outstanding and blockers
1. Confirm exact installed PRoot distro alias/launch mechanism and current Hermes entrypoints using read-only inspection. Never expose keys or Telegram configuration.
2. Read-only inspection of installed Hermes v0.15.2 supported tool/adapter interfaces, then separate adapter that preserves existing Telegram runtime.
3. Chat/task status and Android voice input with visible active state and cancellation.
4. Stable signing/monotonic release workflow; default debug signing may differ by CI runner.
5. Obtain exact Telegram gateway status if needed; do not infer health from old screenshot.

## 6. Single next action
In native Termux run proot-distro list and report the installed distro alias. No recorded successful native-Termux-to-PRoot launch command is available, so do not guess ubuntu as an alias. If proot-distro is unavailable, inspect the user's actual launcher next rather than reinstalling.

## 7. Continue commands/files
docs/LOCAL_BRIDGE.md contains exact device commands, protocol and limits.
CI protocol tests: javac -d /tmp/hermes-protocol-test app/src/main/java/io/github/techwithrakibofficial/hermesbridge/LocalBridgeServer.java tests/LocalBridgeServerTest.java; java -cp /tmp/hermes-protocol-test LocalBridgeServerTest
Android: gradle --no-daemon :app:assembleDebug :app:lintDebug (Gradle 8.11.1/JDK17/SDK35).
Local scratch lacks javac; tests were run and verified in CI.
Do not repeat Hermes installation or Telegram pairing. Resolved first-run SDK issue: removed package tools, fixed by requesting platform-tools.

## Recorded Hermes environment context (prior conversation, not freshly verified)
Hermes executable /usr/local/bin/hermes, Python 3.14.4, package location /usr/local/lib/python3.14/dist-packages, reported venv /usr/local/lib/python3.14/dist-packages/venv. Previously recorded gateway command hermes gateway run produced Telegram replies. Current gateway process/health unknown. No exact distro alias or launch script found. Latest upstream docs may describe a different Hermes version; v0.15.2 tag lookup returned 404, so inspect installed code/help before selecting a plugin/MCP mechanism.

## Latest installed-runtime evidence (supersedes prior distro discovery task)
User ran proot-distro list: installed alias ubuntu.
User ran proot-distro login ubuntu -- bash -lc 'command -v hermes; hermes --version; hermes --help'. Output directly reports /usr/local/bin/hermes, Hermes Agent v0.15.2 (2026.5.29.2), project /usr/local/lib/python3.14/dist-packages, Python 3.14.4, OpenAI SDK 2.24.0. Commands include plugins, mcp, acp, chat and gateway. The CLI advertises one-shot mode as auto-bypassing approvals; do not choose that as the default app integration without explicit permission/confirmation design.
Exact current stopping point: installed runtime and distro entry confirmed by user command output; plugin/MCP subcommand capabilities not yet inspected. No adapter installed, no Hermes config changes.
Single next action: run proot-distro login ubuntu -- bash -lc 'hermes plugins --help; hermes mcp --help' and review output before choosing integration. The native shell command only requests help; do not upgrade/reinstall or restart/reconfigure Telegram.

## Current integration work (supersedes prior stopping point)
User supplied help proving installed Hermes plugins install/list/enable and MCP add/test/configure/serve commands. Selected direction: add Android as an MCP tool server to Hermes, not expose Hermes conversations with mcp serve.
Implemented a separate stdlib-only legacy-MCP stdio adapter at integrations/hermes_bridge/bridge_mcp.py with android_bridge_status/android_home/android_back. Explicit pairing validates app status and saves the session token in an adapter-owned private Ubuntu config file (700 directory/600 file), separate from ~/.hermes. Rereads token per call; no dependency on new shell exports reaching an existing Telegram process. Fixed loopback URL, no proxy/redirect, allowlist, no automatic retries, sanitized errors.
17 tests passed locally on Python3.12 (fake HTTP and legacy stdio handshake/discovery, not real Hermes or Android actions). Python3.12/3.14 CI prepared. Exact installed mcp add syntax not yet provided; no phone adapter installation or Hermes config change has occurred. Android app remains 0.2.0 and need not be rebuilt for this adapter.
Single next action on phone: proot-distro login ubuntu -- bash -lc 'hermes mcp add --help'. Then install the pinned adapter and append only its MCP entry using confirmed CLI options; inspect/discuss reload requirements before touching the current gateway.

Adapter source committed at 3c8ccba88514fba4ddaefdad1c547ac57db444e2. Local 17-test suite passed. CI workflow file is present but no Actions run for this commit was visible during verification; Python3.14 CI is NOT confirmed. Real Hermes registration and tool discovery remain pending. The next required user command remains hermes mcp add --help within Ubuntu, not a repeat of top-level mcp --help.

## Current blocker — gateway recovery after Termux session closed
User's original terminal session closed. User-service status hit missing D-Bus/XDG runtime; this does not establish foreground process state. Foreground hermes gateway run attempts Home Assistant at homeassistant.local:8123 and fails. User diagnostics confirm /root/.hermes/.env has Telegram token; effective loaded config reports Telegram enabled/token_loaded true and Home Assistant enabled/token_loaded true.
User backed up config.yaml and successfully set platforms.homeassistant.enabled=false, but Home Assistant still starts. Prior flag-only fix is ineffective on this installed version.
Exact upstream source for v2026.5.29.2 at gateway/config.py has unconditional Home Assistant enablement whenever HASS_TOKEN is nonempty. hermes_cli/main.py and gateway/run.py reload user dotenv overriding stale shell exports. Therefore process-only empty env cannot be assumed sufficient. Issue https://github.com/NousResearch/hermes-agent/issues/25065 corroborates this behavior.
Planned next repair, NOT YET EXECUTED: stop the newly launched foreground gateway; privately back up /root/.hermes/.env; use python-dotenv set_key to empty only HASS_TOKEN, preserving Telegram/provider credentials and pairing. Keep the already saved Home Assistant enabled=false. Then restart normal foreground gateway and test Telegram reply. Do not allow all users or reinstall Hermes. Native Android bridge tests and staged MCP adapter remain intact. Current priority: Telegram recovery; mcp add --help still needed afterward.
