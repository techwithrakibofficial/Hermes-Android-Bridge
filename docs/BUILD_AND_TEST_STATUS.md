# Build and test status

Updated 2026-10-28 (Asia/Dhaka).

## Verified
- GitHub repository write access: initial handoff commit succeeded after user updated access.

## Implemented, pending CI
- Compose companion and Accessibility service.
- Home/Back test controls and Android TTS test.
- Pinned GitHub Actions compile/lint/artifact/prerelease pipeline.

## Not verified
- Kotlin/Android compilation and lint: awaiting first CI run.
- APK installation and rendering on user's phone.
- Accessibility availability under EMI policy, Home/Back execution, audible app TTS.
- Existing Telegram continuity after companion installation.

## Not implemented
- Local bridge authentication/server, Hermes adapter, chat, speech input and remote Android actions.

Never report this milestone as working end-to-end without CI and device evidence.

## First CI result
Run 40604912403 failed before compilation: setup-android tried to install the removed SDK package 'tools'. Fixed by explicitly requesting platform-tools only. No app compile result yet.
