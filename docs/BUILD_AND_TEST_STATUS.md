# Build and test status
Updated 2026-10-28, Asia/Dhaka.

| Check | Result |
| --- | --- |
| GitHub write access | Verified |
| Static XML and workflow YAML validation | Passed |
| Protected Accessibility service; no screen/gesture capability | Verified from source |
| Android debug APK compilation | Passed in CI |
| Android lintDebug | Passed in CI |
| Artifact upload | Passed |
| Prerelease APK publication | Passed |
| Actual phone install/UI | Not tested |
| Audible Android app TTS | Not tested |
| EMI device Accessibility enablement | Unknown |
| Actual Home/Back visible effect | Not tested |
| Existing Telegram after companion install | Not retested |
| Hermes chat/voice/secure local bridge | Not implemented |

Code commit: 3826ff409062c592bbc59a131fc39dd756301d73
Successful run: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40604989249
Development release: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-2
APK size: 9,287,944 bytes.

First run 40604912403 failed in SDK setup, before compilation: removed SDK package 'tools'. Fixed by requesting platform-tools explicitly. Replacement run completed successfully.

No emulator/device tests ran. Compilation and lint do not prove Android execution or audible speech. CI action deprecation warnings did not fail this run. Debug signing may differ between future CI runners; stable update signing is still pending.

## User report and speech correction (0.1.1)
User says all initial companion functions work, but voice is too fast to understand. This confirms audible TTS by user report; individual Home/Back/disconnect tests were not separately listed. Screenshot shows native termux-api installed and a TTS command returned to prompt. Latest Telegram message has no visible reply in the screenshot; current gateway health cannot be inferred.

Implemented slower default 0.70, slider range 0.25–1.25, saved preference across restarts, stop playback when adjusting, reset control and setSpeechRate error handling. VersionCode 2. New compile/lint and phone speed/persistence tests pending.

0.1.1 verification: compile/lint, artifact and publication PASSED in run 40606075816 at commit 31bb9f4937754a14c0f23fdcf6105fac2fc9ea2e. Release dev-3 includes Hermes-Bridge-0.1.1.apk (9,287,940 bytes). Phone speed adjustment and preference persistence remain pending.

## 0.1.2 voice-quality correction
User confirms slider helped speed; reports unintelligible voice quality. Phone engine/voice not identified. Installed-engine and language-matched voice picker, preview, neutral pitch, voice-data handling and explicit online opt-in implemented. Compile/lint and actual phone listening/persistence tests pending. No claim of humanlike quality is made.
