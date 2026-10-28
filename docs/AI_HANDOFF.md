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

## 0.4 phone evidence and current repair
User installed0.4, enabled screen control and ran pinned updater; gateway banner
shown. Telegram actual tool output: bridge ready/version0.4.0 and device state
TECNO KM9, Android16/SDK36, screen_control_enabled true, voice_ready false. These
two read tools work. They do NOT prove screen/click/type or voice functionality.
The safe acceptance workflow stopped at android_list_apps with generic HTTP503;
subsequent bridge status remained ready. No app-open/screen/click/text ran.
No home channel set was also reported; /sethome may be used in the user's verified
private Telegram chat for later delivery. It is not this listing failure.

Confirmed code defect: full PackageManager query and label-loading comparator
ran on UI thread under1.2s dispatch wait; loadLabel repeated during sorting.
Likely cause of reported503, not proven without phone logs. No claim of missing
permission, locked screen or EMI cause.0.4.1 moves catalog to one background
loader, caps and loads labels once, shares pending work and caches30s. Read wait
2.8s returns apps_loading if unfinished. Exact-package open no longer queries
all installed apps. Specific main-dispatch codes and voice_status/catalog state
added to device diagnostics. Also confirmed remote VoiceOutput objects were
constructed without calling refresh/startEngine; service now starts engine on
creation/reload/fallback. MainActivity's manual voice had its own onResume refresh,
explaining why its earlier test could work while remote voice_ready stayed false.
Screen observation has a separate3.5s dispatch wait; mutation wait stays1.2s.
Existing19-tool adapter remains compatible. Actual updated TTS remains untested.

##0.4.1 published evidence
Code cbe205a130d1d713a2910b812238a265296cd505; release dev-15 (not dev-14,
which predates remote-TTS initialization fix). Android run40648224661 passed
compile/lint,46 protocol checks and22 catalog checks with fake providers/handlers.
Python run40648006478 passed61 tests each on3.12/3.14 (adapter unchanged by final
app-only TTS patch). APK9,354,144 bytes;
SHA2567ae075a2c17f5ab7ae65f8344a0199eeca843a5258df48cc18c31445f366cbd5.
Actual updated TECNO app-list/screen/click/type/speech results remain pending.

## Latest phone evidence —0.4.1
User pasted Telegram tool results after the0.4.1 upgrade: status ready/version0.4.1;
TECNO KM9 Android16/SDK36; screen_control_enabled true; voice_ready true;
voice_status "Voice ready. Test করে শুনুন।", voice_language en-US;
app_catalog_state ready; last_dispatch_error none. One android_list_apps call
returned110 launchable apps, truncated false, label_fallbacks0. No app opened or
setting changed. This confirms those read tools and catalog on the actual phone,
not independently observed screen actions or audible speech. This supersedes the
prior app-list and remote-engine readiness blockers. Individual app labels alone
are not evidence of an app's safety or behavior; no action on them is authorized.
No Telegram home channel is set; /sethome in the verified private chat is optional
for future scheduled delivery and unrelated to current bridge health.

## Latest blocker — OpenRouter output-credit preflight
User's next safe UI test invoked open_package, screen and scroll several times,
then failed at the model provider with HTTP402/openrouter_credits: requested output
ceiling131072 exceeds current allowance. Tool names alone do not prove accepted
or visible UI results. No Draft/Counter or click/type success was provided.
Do not replay the whole action sequence blindly or reinstall the APK.

Exact Hermes upstream v2026.5.29.2 agent/agent_init.py reads model.max_tokens from
config.yaml when no explicit constructor cap is supplied. OpenRouter official
limits docs confirm output maximum contributes to estimated/preflight cost.
Planned cap4096 with private config backup preserves current model/provider and
other configuration. Temporary-file validation passed for dict and legacy string
model formats; actual phone save and post-change API request remain untested.
This does not add credits, change inference pricing or guarantee unlimited free use.

## Single next action
User Ctrl+C foreground gateway. Run backed-up output-limit command from
 docs/PROVIDER_402.md; confirm PASS model.max_tokens=4096. Restart saved private
hermes-with-android gateway run, send /new in Telegram, request a brief text-only
reply first. If successful, make one read-only screen request before resuming safe
UI tests. No bridge restart/pairing, provider/key/model change needed for this step.
Current TTS language remains en-US; audible speech and safe-test Draft/Counter
are still pending. Stop repeating402 requests until config changes are loaded.

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
