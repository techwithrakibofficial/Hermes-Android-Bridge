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
User confirmed All done after Telegram status/Home test instructions; record user-reported 0.2 round-trip completion. User expanded the goal to general phone operation/diagnosis; docs/PHONE_AGENT_GOAL.md preserves it.
0.3.0 Settings and allowlisted app-opening code built successfully. Android run40637536913 passed compile/lint and30 real-socket checks with a fake Android handler; publication succeeded as dev-11. Adapter run40637536937 passed51 tests on Python3.12 and3.14. No new Settings/app-launch phone results yet.
APK https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-11/Hermes-Bridge-0.3.0.apk (9,337,448 bytes). Code commit b86c29673939df243f113832efe42af92c71c64b. Updated adapter source pinned to d0d742277c6b1850edf82305c13ef41a51afcbfd. Updated private-runtime setup hash 381b4a1aea4ba8768b30e052dc3dd5dc7353f14657b657f0e81cfe2b1d22efaa.

## 5. Outstanding/blockers
1. Stop current foreground gateway, start Android app local bridge, copy pairing export into native Termux, then run the pinned setup command with explicit token forwarding to Ubuntu.
2. User has confirmed Telegram AI reply. Next verify actual MCP discovery, then an Android status tool call from Telegram.
3. PRoot→Android action tests via actual Hermes tool; then Settings/open app/screen/click/type capabilities and chat/voice input.
4. Stable APK signing/versioning; current debug runner keys may differ.
5. Android may kill Termux processes; no background-uptime guarantee.

## 6. Single next action
Follow docs/UPGRADE_0_3.md: install APK0.3.0, copy a fresh pairing export, run the pinned updater and verify Telegram Settings/WhatsApp launches. Existing private SDK is reused. Then implement screen observation/click/type loop, not another limited Home/Back-only design. Do not claim general autonomous phone repair or screen control is implemented yet.

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

## SDK-fix source/evidence
Exact upstream v2026.5.29.2/pyproject.toml pins optional mcp=[mcp==1.26.0]. No Hermes upgrade or all-extras installation selected. New setup helper SHA256 7e73aa218cb75e47cd5778334fe7acf0d0e8a0004392c0022327759841829239. The installed SDK was missing, not merely disabled in config; the phone had never run real MCP discovery successfully.41 local tests cover install opt-in/exact interpreter+pin and owned-disabled recovery safeguards; they do not execute pip/network against the phone environment. Pairing and Ubuntu local bridge connectivity have direct phone PASS output.

## Private runtime fix
Updated setup SHA256 c7c0900286c845896f3c6ff46ce619ffe673bc3fbdb129c4f87ad1e9b24412ca. SDK venv: /root/.local/share/hermes-android-bridge/mcp-env. Future restart launcher after successful setup: proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run. No new APK needed. Use private interpreter for both MCP client/gateway and adapter entry; global Hermes shebang alone cannot see private SDK. See integrations/hermes_bridge/PRIVATE_RUNTIME.md.47 local tests pass; no actual phone SDK install or MCP discovery success claimed.

## Latest confirmed phone evidence
User log: Successfully installed mcp-1.26.0 in private runtime; SDK imports PASS; adapter integrity PASS; pairing PASS; disabled entry enabled PASS; Hermes MCP discovery PASS for all3 tools; gateway launch attempted. This supersedes prior phone-SDK/discovery-pending notes. Future restart: proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run. Telegram→tool→Android round-trip remains unverified until the next test.

## 0.3 staged source and expanded goal
Adapter source pinned to commit d0d742277c6b1850edf82305c13ef41a51afcbfd with SHA2566ee58693062578f057b67aad4b720f2a664a0d13ef992643404686f386d749b0. Updated setup helper SHA256 381b4a1aea4ba8768b30e052dc3dd5dc7353f14657b657f0e81cfe2b1d22efaa; it requires APK0.3 capabilities and expects5 tools. Existing gateway/private SDK1.26 and token file remain in place. New APK session token requires pairing renewal. Actual new app-opening results are unverified.51 local Python tests passed; platform build pending. Development debug key caching starts with this build; it does not recover the prior runner's signing key or guarantee cache retention.
