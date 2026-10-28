# Hermes Companion master plan —0.5.0

## Product and delivery scope
A native personal companion for the existing Hermes in Termux Ubuntu: typed chat,
spoken turns, warm Bengali conversation, coding/planning, observed Android actions,
visible progress, explicit approvals and immediate local Stop. Keep applicationId,
Accessibility bridge, Telegram pairing, provider configuration, private SDK,
cron jobs and saved voices. No root/ADB/EMI bypass or paid service dependency.
The release is a complete integration build for phone acceptance, not proof all
OEM/app behaviors or all future features work. Unbounded feature completeness,
unlimited free cloud inference and carrier-call AI audio cannot be promised.

## User experience
- New companion home: connection/model status, conversation history, copyable
  replies, input, Send/Mic, conversation toggle, live task/tool status, Stop.
- Existing Bridge/voice setup and harmless control-test activity remain reachable.
- New conversation and local history clearing; no silent replay of a pending task.
- Native app talks to the user's existing Hermes via authenticated loopback;
  API credentials stay in Ubuntu. Telegram continues independently.
- Consequential tool approvals appear with Allow once/Deny; no blanket yolo.
- Calls/messages use the existing generic UI tools and require observed recipient
  and result checks. No new claim of reliable every-app workflows, live telephone
  audio, incoming AI calls or hardware repair.

## Voice design
Foreground, user-started turn-taking conversation, not a fake full-duplex call.
One recognizer instance/session at a time, main-thread APIs, generation guards,
final-result deduplication, bounded capture/finalization timers. Stop/cancel,
permission denial, audio-focus loss, phone-call audio mode and recognizer errors
pause rather than start retry loops. Recognition is off while TTS plays. Only a
successful final TTS completion can rearm one next listening turn in opted-in
conversation mode. Silence/error pauses and requires user resume. No own beeps,
no global volume muting. System recognizer sounds are engine/OEM controlled.
On-device recognition preferred; network recognizer requires opt-in. No recorded
audio files. Microphone foreground notification with Stop, limited session timer.
Text input remains usable if mic/language support is unavailable. User-selected
TTS voice/rate is reused; voice gender/quality requires listening tests.

## Native/server boundary
Android server127.0.0.1:18765 remains the existing action bridge. A Python companion
server127.0.0.1:18766 uses the current paired session token. App never stores an
inference API key. Cleartext allowed only for localhost. Strict Host, no Origin/
CORS/redirect/proxy, bounded JSON, private files, fixed routes. Renew pairing after
bridge restart. New jobs require a live authenticated Android bridge; Stop/token
rotation cancels active companion work best-effort. Already dispatched Android
side effects cannot be undone.

Hermes ACP stdio is used for sessions, text/tool updates, approvals and cancellation.
Pin its optional agent-client-protocol==0.9.0 in the existing private Python only;
no Hermes upgrade/global pip override. The backend owns only its ACP child, not
Telegram's gateway. Jobs have persistent request IDs, payload fingerprints and
single-active-job admission; retrying the same POST returns the old job, never
re-executes it. Backend restart marks incomplete jobs interrupted. Read-only polls
may reconnect; inference/mutations do not auto-retry. Conversations use Hermes's
own session persistence and separate companion IDs.

## Failure handling
-401/pairing lost: stop listening, show pairing steps, retain transcript/job ID.
-Provider402/429: clear safe explanation/cooldown, pause auto voice; no retry storm.
-Transport interruption: retain job ID and reconcile via GET, do not resend prompt.
-Stop: stop recognizer/TTS immediately, request session cancellation; clearly
  distinguish requested cancellation from confirmed completion.
-Callback after Stop/background/new session: ignored by generation/turn guard.
-Long reply: bounded TTS chunks with one completion chain; no overlapping speech.
-Task opens another app: foreground task continues; mic is never silently started
  from the background. First microphone foreground service starts from visible UI.
-Android process/battery restrictions: visible health, no uptime guarantee.

## Verification and release gates
1. Preserve90 existing JVM bridge/catalog/snapshot checks and61 Python tests.
2. Add pure state-machine tests for overlapping start, late/duplicate callbacks,
   silence/error/busy, Stop, turn rearm and speech chunks.
3. Backend real-loopback tests for auth/Host/Origin/body/ID conflict/restart,
   single job, approval/cancel, slow provider and safe errors. Actual ACP SDK wire
   integration; matching Hermes-source smoke/integration where feasible in CI.
4. Android compile/lint and APK publication must pass before sharing download.
5. Real TECNO acceptance after installation: typing, Bengali/English recognition,
   TTS, silence, repeated tap/Stop, interruption, background action/return,
   permissions denied/revoked, network loss,402/429, process kill/re-pair. No phone
   result is claimed by JVM/HTTP fixtures or compile alone.

## Installation and checkpoints
Keep current Telegram/cron launch running. Install0.5 APK, renew existing pairing,
install the pinned companion backend/ACP optional package in private Python and
start it separately (phone-only). Provide one pinned setup command and a simple
launcher. Update AI_HANDOFF and build status with exact source/run/release hashes.
Stable external release signing, full-duplex local audio, offline local inference,
attachments and device-specific telecom roles are separate future work; no hidden
placeholder buttons that claim these are active.
