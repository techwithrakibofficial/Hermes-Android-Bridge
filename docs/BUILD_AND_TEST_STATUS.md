# Build and test status —0.4.1

- Android run40648224661: compile/lint,46 loopback protocol checks and22
  app-catalog worker/cache/timeout/Stop checks PASS; release dev-15 published.
  Android handlers/package providers in these tests are fakes.
- Python run40648006478:61 tests each on3.12 and3.14 PASS; local3.12 also61 PASS.
  Final app-only TTS patch changes no adapter Python source.
- User0.4 phone output confirms actual Telegram bridge status and device-state
  tools, screen permission true, remote voice_ready false. App listing failed503;
  safe UI test did not run. These are the problems addressed by0.4.1.
- Actual0.4.1 app listing, screen/click/type, TTS and per-app workflows: pending.
  Follow docs/FIX_0_4_1.md. Android acceptance is not visible task completion.
- Older direct Back/Stop explicitly confirmed; status/Home user-reported. Preserve
  distinctions between prior phone evidence and newly passing CI tests.

https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40648224661
https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40648006478
https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-15

## Latest0.4.1 phone result
User-supplied actual Telegram tool output confirms bridge/device reads and one
app-list call:110 apps, truncated false, label_fallbacks0; catalog ready,
voice_ready true (en-US), no dispatch error, screen permission true. No screen,
click/type or audible TTS result yet. Next: safe control-test activity, then a
listening test. Do not repeat installation, pairing or catalog recovery.
