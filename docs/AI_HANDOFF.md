# CURRENT HANDOFF — Hermes Android Bridge
Updated 2026-10-28 (Asia/Dhaka).

## 1. Goal and constraints
Kotlin/Compose companion for existing Hermes Agent in Termux Ubuntu PRoot. Voice/text discussion and Android actions through Accessibility and authenticated loopback. Free, phone-only, GitHub Actions APK builds. No root/ADB/Developer Options unlock/EMI bypass. Preserve Telegram token, pairing, provider settings and memory. No reinstall or automatic upgrade.

## 2. Finalized
- PRoot alias ubuntu; /usr/local/bin/hermes; v0.15.2 (2026.5.29.2), Python3.14.4, OpenAI SDK2.24.0; package /usr/local/lib/python3.14/dist-packages. Hermes home /root/.hermes.
- Android0.2.0: user-started 127.0.0.1:18765 bridge with ephemeral256-bit token; authenticated status/Home/Back; request-ID replay prevention, bounded input/session. No screen read/typing/messaging yet.
- Voice engine/language/voice selection, speed and neutral pitch: user now says all okay.
- Add Android MCP tools TO Hermes; do not use mcp serve as the client integration. Stdio adapter staged; installation/registration awaits actual mcp add --help.
- Gateway recovery takes priority after original Termux session closed.

## 3. Completed and verified
Direct CI evidence:
- Android0.2.0 compile/lint,20 fake-handler socket protocol checks and dev-5 release passed in run40608250521. APK https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-5/Hermes-Bridge-0.2.0.apk.
User phone evidence:
- Done after native status/Home instructions; no raw screen/output captured.
- Explicitly confirmed Back works and post-Stop connection fails as expected.
- Foreground gateway starts; service-status command fails on missing D-Bus in PRoot.
- Telegram enabled/token_loaded true; HomeAssistant enabled/token_loaded true in actual loaded config.
- Saved config HomeAssistant enabled:false still re-enabled by HASS_TOKEN. Exact upstream v2026.5.29.2 code confirms unconditional env enablement.
- Earlier network log included Telegram httpx.ReadError; latest user log has ONLY allowlist warning, no HA/network error. A warning alone is not gateway failure; its startup check omits pairing while runtime accepts paired users.
Local coding/testing:
- Legacy-MCP stdlib adapter with private session-token pairing,17 local tests passed. Real Hermes handshake/discovery/actions remain untested; Python3.14 CI not confirmed.
- Recovery helper integrations/hermes_bridge/recover_gateway.py added.13 recovery tests plus17 adapter tests =30 tests passed locally on Python3.12 using temp files/fake Telegram HTTP. No real account or phone recovery test yet.

## 4. Exact stopping point
User confirms receiving a Telegram AI reply after gateway recovery. Record Telegram recovery SUCCESS by user report; do not repeat the Home Assistant/allowlist troubleshooting. Resume Android MCP installation.
Prepared setup_android_bridge.py: pinned/download-verified adapter, private install/pairing, config backup, installed CLI registration, exact-client tool discovery and automatic foreground gateway start. Local tests total37 passed on Python3.12; actual setup/registration/actions have NOT run on the phone yet.
Exact upstream v2026.5.29.2 parser/source now verifies mcp add android-bridge --command <python> --args <script>; no more help output is required. CLI asks Enable all 3 tools [Y/n/select]. Setup calls its discovery verifier after saving.

## 5. Outstanding/blockers
1. Stop current foreground gateway, start Android app local bridge, copy pairing export into native Termux, then run the pinned setup command with explicit token forwarding to Ubuntu.
2. User has confirmed Telegram AI reply. Next verify actual MCP discovery, then an Android status tool call from Telegram.
3. PRoot→Android action tests via actual Hermes tool; then Settings/open app/screen/click/type capabilities and chat/voice input.
4. Stable APK signing/versioning; current debug runner keys may differ.
5. Android may kill Termux processes; no background-uptime guarantee.

## 6. Single next action
Use the setup command supplied in chat after explicitly stopping old gateway and copying the app's fresh pairing command into native Termux. The setup forwards the token to Ubuntu, pairs the adapter, asks Y for three tools, verifies discovery and restarts gateway. Then in Telegram start /new and ask the agent to use android_bridge_status. Report PASS/STOP and actual tool result, not the secret token.

## 7. Files/verification limits
- integrations/hermes_bridge/RECOVERY.md: scope, checks, safe diagnostics and backups.
- integrations/hermes_bridge/recover_gateway.py: recovery helper; SHA256 d647989bf3999d19d1b082c91ddf1d2dd406fc62195d03d5014dc4dad126924f.
- integrations/hermes_bridge/bridge_mcp.py: staged status/Home/Back MCP adapter; no registration performed.
- docs/LOCAL_BRIDGE.md: existing native bridge protocol/tests.
- Local tests: python -m unittest discover -s integrations/hermes_bridge -p 'test_*.py' -v.
- Recovery backups live only on phone in ~/.hermes/android-bridge-recovery/<timestamp> (private); never upload them or .env.
- Do not call a build/device/integration working without appropriate evidence.

## Setup source and verification
integrations/hermes_bridge/setup_android_bridge.py SHA256 6331a216e81885fa9a0ca4d696c3df44b7ab47799946bdef42b39170c8d553c6.37 local tests passed; temporary-file/HTTP/stdin tests do not establish actual installed Hermes MCP compatibility. The user must run setup before any claim of agent-controlled Android success. This phase does not add Settings/click/type/chat-app actions, voice input or companion chat UI.
