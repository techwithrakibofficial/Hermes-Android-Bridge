# Build and test checkpoint — 2026-10-28

## Published native companion0.5.0

- Source: `c1bdca0215d5386ff096cc6cc5591d900e0fb5a3`.
- APK: [dev-20 / Hermes-Bridge-0.5.0.apk](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-20/Hermes-Bridge-0.5.0.apk),9,469,614 bytes.
- GitHub asset SHA256: `ed4ec5c67b2698261cf57ab9437fdb51da249a669fe5e4467efb92859b31ee9a`.
- [Android build40678159006](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40678159006): successful compile/lint and publication.124 Java checks:46 loopback protocol,22 catalog,22 snapshot,34 voice lifecycle/chunks.
- [Python40678159057](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40678159057):80 tests passed on each of3.12/3.14, with actual pinned ACP SDK0.9.0 installed. Local run without optional SDK skips its wire fixture; CI did not skip it.
- [Exact Hermes40678159077](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40678159077): pinned NousResearch/hermes-agent tag `v2026.5.29.2`, actual ACP child and configured MCP tool calling with local LLM and fake Android. Confirmed same persisted session after child restart and no job replay. No real inference account or phone actions involved.

## Coverage and limits

Protocol tests use real localhost sockets with bounded/authenticated HTTP. Python
fixtures cover pairing, Host/Origin/body limits, request idempotence/conflict,
single task, approval/cancel, persisted interrupted jobs, provider safe errors,
bridge revocation and actual ACP SDK wire flow. Java tests cover state transitions,
double start, duplicate/late callback, silence/error pause, Stop/rearm and speech
chunk boundaries. Compiler/lint validates Android source/API usage.

None of these fixtures proves OEM SpeechRecognizer callbacks, Bengali offline
model availability, audio focus behavior on TECNO, foreground/battery survival,
actual Play Store/TTS engine voice quality, live provider routing or arbitrary app
UI reliability. The phone acceptance matrix is in [UPGRADE_0_5.md](UPGRADE_0_5.md).

## Actual prior phone evidence

User-reported0.4.x: working Ubuntu private MCP connection; app catalog110 entries;
exact-package opening and screen reads; safe test activity Bengali draft and
counter1 after reobservation; audible English/Bengali speech; successful read-only
Facebook navigation. Settings navigation reached intended page, final battery
report was not supplied. Current phone installed version before upgrade0.4.2.
No new0.5 microphone/background/approval result has been reported yet.

Telegram home was set. One-shot check-in delivered. Two daily jobs were reported
scheduled at11:00/20:00 Asia/Dhaka using openrouter/free. First actual recurring
run is unverified. Model switch plus successful bridge tools was reported; free
quota is bounded and no unlimited service promise is made.

## Reproducibility and signing

GitHub Actions uses JDK17, Gradle8.11.1, Android SDK35/build-tools35.0.0.
Private installer is pinned with SHA256; backend pin is recorded in upgrade guide.
Existing19-tool adapter remains unchanged. SDK installation is confined to the
existing private venv; no system pip override/Hermes reinstall is part of0.5 setup.
Cached debug signing is development-only; key eviction/update mismatch remains a
known packaging risk. Stable release signing is a future separate milestone.
