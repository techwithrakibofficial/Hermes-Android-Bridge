# CURRENT HANDOFF — Hermes Android Bridge
Updated2026-10-28. This file supersedes older appended handoff notes.

## Goal and invariants
Personal phone-operating companion: Bengali conversation, warm feminine persona,
planning/coding, opt-in proactive contact, app/settings workflows. Free phone-only
development/GitHub Actions builds. No root/ADB/Developer Options/EMI bypass.
Preserve existing Hermes/provider/Telegram/pairing/memory; no reinstall/reset.
User asks for maximum possible features and accepts risk. This does not establish
any untested phone capability or override Android/provider policies.

## Confirmed existing phone runtime
- Native Termux PRoot alias ubuntu. Hermes /usr/local/bin/hermes v0.15.2
  (2026.5.29.2); Python3.14.4, OpenAI SDK2.24.0; home/root/.hermes.
- Private MCP SDK1.26.0 installed successfully, actual Hermes discovery of3 tools
  passed. Ubuntu authenticated bridge pairing passed. Telegram reply recovered.
- User confirmed All done after Telegram status/Home test; direct Back and bridge
  Stop were explicitly confirmed. Voice speed and selection were user-accepted.
- User now says the0.3 functionality works; no raw Settings/app execution output.
- HASS_TOKEN previously force-enabled HomeAssistant despite config.enabled=false;
  recovery has resolved it. Do not repeat recovery or change Telegram credentials.
- D-Bus service-status error is expected in PRoot; use foreground gateway.

## Required future startup
proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run
Do not replace this with the global hermes shebang: it cannot see the private SDK.
Private token: ~/.config/hermes-android-bridge/connection.json mode600.
Fresh app bridge Start rotates token; copy export into native Termux alone, then
forward HERMES_BRIDGE_TOKEN to the pinned Ubuntu setup. Keep token/.env private.

## Last released and verified baseline
0.3.0 APK dev-11, code b86c29673939df243f113832efe42af92c71c64b.
Android run40637536913: compile/lint,30 fake-handler socket checks, publication PASS.
Python run40637536937:51 tests Python3.12/3.14 PASS.
https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-11

## Current0.4 checkpoint
Source implemented: explicit per-session screen-control switch; bounded UI tree,
password-control omission; stale snapshot/node checking; click/long-click/scroll/
replace text; installed launchable-app discovery/open package; dialer intent;
remote speech with saved voice, stop speech, recents/notification shade/Quick
Settings/basic device state.19 MCP tools. Safe control-test activity added.
Optional --companion setup flag adds a backed-up marked block to existing SOUL.md;
other personality content preserved. No cron job is created during install.
Read docs/COMPANION_0_4.md for full behavior, limits and phone acceptance tests.

Local59 Python tests passed (stdlib/fake HTTP/temp files). Python3.12/3.14 CI
run40642075403 passed59 each. Android run40642075456 passed compile/lint,46
fake-handler real-socket checks and publication as dev-12. APK9,354,144 bytes;
SHA2562671a19802bd77954172444eaef79c099e7655841cbab6c7315ca9321ac59fed.
Actual0.4 phone actions remain pending. Do not
claim broad messaging/call automation is phone-tested or a final universal agent.

## Single next action
Follow docs/UPGRADE_0_4.md: install dev-12 APK, start bridge and enable screen
control, copy fresh pairing export into native Termux, run the pinned updater
with --install-sdk --companion, then test the safe control activity via Telegram.
No new Termux session required: Ctrl+C the current foreground gateway first.
Code head d616cfb14878b1a7652212e2de49207118f9490f; adapter pin
b08f55fd44255222d6a787472d944fd93fb64656,
SHA2562b5c1b1df0b5daa781749941950c271d99750093bfb6547a754b944a00b23902.
Setup SHA2568bb988ef62eda3b4c2828fb3f8d1cc707f130d635ccae946a4624b6b8e841908.

## Still unimplemented or unverified
- No live carrier/WhatsApp call audio, Telecom dialer role, incoming-call answer/
  end integration, real incoming AI telephone calls, always-listening microphone
  or standalone two-way companion chat/voice UI. Current chat remains Telegram.
- Generic nodes can operate app controls, but per-app recipient/send/call and
  repair workflows need individual real-phone tests. Custom/protected UI may fail.
- Proactive Telegram check-ins use existing Hermes cron tools and need verified
  timezone/private-chat delivery; no jobs configured or delivery tested here.
- Feminine voice preference is profile-level; installed TTS voices must be
  previewed/selected on phone. Standard Voice API has no portable gender field.
- No guarantees of unlimited free inference or Android background uptime.
- Debug signing key cache began in0.3; eviction can require Bridge-only reinstall.

## Verification commands
python -m unittest discover -s integrations/hermes_bridge -p 'test_*.py' -v
CI javac socket protocol test and gradle :app:assembleDebug :app:lintDebug.
Screen control off on every bridge restart; Stop revokes access/token and stops
remote TTS, but cannot undo an already-dispatched Android action. No blind retries.
