# CURRENT HANDOFF — Hermes Android Bridge / Companion

Updated2026-10-28. Read this before changes. Preserve working state; do not restart
setup or claim phone success from fixture results. No root/ADB/Developer Options/
EMI bypass. Free, phone-only development through GitHub Actions.

## Exact checkpoint / single next action

0.5.0 native companion, private backend and installer are implemented, committed
and CI-tested. Release **dev-20** source
`c1bdca0215d5386ff096cc6cc5591d900e0fb5a3` is published.
**Next: install the0.5.0 APK on the user's TECNO, then follow UPGRADE_0_5.md to
pair and run the combined native+Telegram launcher. Start acceptance with one
text-only reply before mic or actions.** New0.5 phone testing has not occurred.

[Download](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-20/Hermes-Bridge-0.5.0.apk)
[Phone guide + exact pinned command](UPGRADE_0_5.md)
[Master plan](MASTER_PLAN.md)
[Build evidence](BUILD_AND_TEST_STATUS.md)

APK SHA256 `ed4ec5c67b2698261cf57ab9437fdb51da249a669fe5e4467efb92859b31ee9a`.
Setup helper at release source SHA256
`b7f18b3e8c42a5dae33511078b8d924dc21f038021d7b4c49ed8ded0947e70cb`.
It pins backend commit `3fad2328dea77584be2337744623ae5d3fe57d56`, SHA256
`7471d22ebf140e2f88b5022c9f65f26a198a422692827dedb2f3b3a114b3c35f`.
Future doc-only commits do not alter this release or installer pin.

## Preserved environment (confirmed)

- Native Termux → proot-distro `ubuntu`. `/usr/local/bin/hermes` v0.15.2
  `(2026.5.29.2)`, Python3.14.4, OpenAI SDK2.24.0; Hermes home `/root/.hermes`.
- Exact upstream source tag `v2026.5.29.2`. Private Python
  `/root/.local/share/hermes-android-bridge/mcp-env/bin/python`, MCP SDK1.26.0.
  Avoid global pip/--break-system-packages, Hermes upgrade or credential reset.
- Telegram bot configured/paired and responding. Old gateway service status uses
  systemd user bus unavailable in PRoot; use foreground gateway, not service fix.
- Home Assistant was unintentionally enabled via HASS_TOKEN. Config-only enabled
  false did not stop it; later gateway output had no HA errors and Telegram
  responded. Do not reenable or reset .env; token values must never be printed.
- Old launcher: `proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run`.
- TECNO KM9 Android16 SDK36; prior installed Bridge0.4.2. Same applicationId
  `io.github.techwithrakibofficial.hermesbridge`;0.5 versionCode9, minAPI26.

## Pairing and Android tools

User-started Android bridge `127.0.0.1:18765`; 43-char256-bit session token.
App exports HERMES_BRIDGE_TOKEN/URL; paste export alone at native Termux prompt.
Never log/share/commit it. Stop/unbind/death revokes pairing. Private Ubuntu
connection file `~/.config/hermes-android-bridge/connection.json` file600/dir700.
MCP rereads it on requests. Bridge-only restart needs fresh export/pairing.

19 existing tools unchanged: status, home/back/settings/open_app, screen, click,
long_click, scroll, set_text, list_apps, open_package, dial, speak, device_state,
recents, notifications, quick_settings, stop_speech. Six aliases WhatsApp,
Facebook, Messenger, Telegram, Chrome, Termux. Generic UI automation is not proof
of every app workflow. Explicit screen-control opt-in; bounded120 nodes,
password omission; snapshot60s/single-use with actual context/exact node check.
No blind stale snapshot retry, no accepted==verified result assumption.

##0.5 architecture and implemented features

- New native CompanionActivity launcher: chat/copy/New chat/History; Voice and
  Connection tabs; saved TTS setup; progress; tool Allow once/Deny; Stop.
  Existing MainActivity bridge setup and ControlTestActivity remain.
- Android → private companion localhost18766 using current bridge pairing.
  Inference keys remain in Ubuntu. Cleartext limited to localhost; strict
  Host/Origin, bounded bodies, no CORS. No prompt/token logging.
- Backend uses actual Hermes ACP stdio, optional `agent-client-protocol==0.9.0`
  only in existing venv. No -z/--yolo blanket approval. Persistent SQLite request
  IDs/fingerprints/job journal plus Hermes session IDs; no prompt replay after
  restart/ambiguous network. Single task. Up to5 loaded conversations before
  restarting own ACP child and restoring sessions. Thought updates not displayed.
- User-started foreground task/mic service; local Stop immediately stops audio,
  requests ACP cancellation;3s grace then only own child group terminated.
  Already executed side effects cannot be undone. Pairing/live bridge monitored.
- One recognizer, generation/final dedup guards,25s capture+8s finalization bounds,
  mic off while TTS. Successful TTS completion may rearm once after500ms only if
  automatic conversation opted-in. Silence/error/busy pauses; no retry/beep loop.
- On-device recognition default; explicit network opt-in, no silent fallback.
  Missing engine/language clearly pauses. OEM sounds cannot universally be muted.
 15min voice session; audio-focus/call guards; timed16min wake lock; nonsticky
  foreground service with Stop; no boot/hidden microphone. TTS shares one speech
  owner, unique callback IDs,1800-char chunks; long responses capped for audio.
- Provider402/429 pauses safely; no provider inference retry storm by app.
  Backend task timeout10min. Read-only reconnect polls bounded; preserves job ID.
- Setup validates installed Hermes initialize without inference, installs pinned
  backend with backups, saves `hermes-companion` (backend only) and
  `hermes-companion-all` (backend + existing Telegram gateway). Default combined
  launcher eliminates need for a new Termux session. Stop old foreground gateway
  with Ctrl+C first; never launch duplicate Telegram pollers.

## Provider/cron continuity

Paid model earlier `deepseek/deepseek-v4.1-flash-1017` failed OpenRouter402.
Backed-up `model.max_tokens` reduced131072→4096, but remaining balance/prompt and
in-flight budget still failed. User applied
`/model openrouter/free --provider openrouter --global`, reported model saved
labels + Done; later bridge tool response succeeded. Actual post-switch model ID
not separately shown. No API access unlimited/expiry guarantee. Quota/routing
can change; do not evade limits or flood retries.

Telegram `/sethome` succeeded. One-shot `bangla-check-in-test` id`b32a97a7322d`
delivered (awkward generated Bengali); no longer listed after one run.
User supplied scheduled recurring jobs, no duplicates reported:
- Morning `01076cf41bca`, `0 5 * * *` UTC →11:00 Asia/Dhaka.
- Evening `740de51d6d08`, `0 14 * * *` UTC →20:00 Asia/Dhaka.
Both modelopenrouter/free/provideropenrouter, Telegram home, repeatforever.
**First recurring delivery remains unverified.** Do not create replacements.

## Verification evidence

Final release CI runs all success:
- Android40678159006: compile/lint, publication and124 Java checks
  (46protocol+22catalog+22snapshot+34voice/chunks).
- Python40678159057:80 tests each3.12/3.14 with real ACP SDK0.9.0 wire fixture.
- Exact Hermes40678159077: actual v2026.5.29.2 ACP→configured MCP→fake Android
  with local LLM fixture; same persisted session restored after process restart.
These use fixtures, not actual phone/provider. Native0.5 voice, permissions,
background behavior and task UI remain phone acceptance work.

Actual earlier phone evidence:110 launchable apps, package opening and screen
reads; safe test Bengali draft `পরীক্ষা সফল`; one counter click accepted while
immediate screen0, later user confirmed1 (do not click again blindly); English
and Bengali TTS audible; user-confirmed successful read-only Facebook workflow.
Settings navigation reached intended page; final battery report not supplied.
No personal account counts/app details need to be stored here.

## Scope and remaining separate work

Do not promise full-duplex Gemini parity, AI live carrier/WhatsApp call audio,
autonomous AI incoming calls, universal hardware repair, offline unlimited LLM,
unlimited free cloud inference, or lock/policy bypass. Installed feminine voice
must be auditioned; API gender cannot guarantee quality. Android UI workflow
success needs observed recipient/target/result; external side effects follow
user authorization. Ordinary coding/planning/conversation uses existing Hermes.

Next fixes should follow actual acceptance failures, not speculative rewrites.
Stable external signing, attachments, local inference feasibility, and tested
app-specific messaging recipes are future milestones. Current signing caches a
development key; eviction may require Bridge-only reinstall, never Termux reset.
Rollback: stop combined launcher; use existing Telegram-only launcher. Hermes
config/Telegram/cron remain independent of native companion backend.
