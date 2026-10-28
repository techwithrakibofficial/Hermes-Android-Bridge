# Build and test status
Updated 2026-10-28, Asia/Dhaka.

| Check | Evidence / result |
| --- | --- |
| Repository write access | Verified |
| 0.1.2 assembleDebug / lintDebug | Passed in run 40607155921 |
| 0.1.2 artifact and prerelease publication | Passed, dev-4 |
| Initial companion functions | User reports working; individual actions not itemized |
| 0.1.1 speech-speed slider | User reports improvement |
| Old voice quality | User reports strange/unintelligible |
| 0.1.2 voice naturalness / clarity | Awaiting phone listening |
| Engine/language/voice switching and persistence on phone | Not verified |
| Current Telegram gateway health | Not established by screenshot |
| Hermes chat/secure local bridge/microphone | Not implemented |

Latest code: 406db52e335792b32af1d067c68cb1ab96bc6b7f
Run: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40607155921
APK: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-4/Hermes-Bridge-0.1.2.apk

No emulator/device audio test ran. Engine voice-quality metadata is used for ordering only and does not prove humanlike output. Missing Bengali voices produce an actionable message; no silent English fallback.
Previous successful builds: 0.1.0 run 40604989249; 0.1.1 run 40606075816. First run 40604912403 failed in SDK setup (removed tools package), fixed in workflow.

## 0.2.0 implementation pending CI
User reports all voice controls now okay. Implemented an opt-in authenticated loopback server and Home/Back dispatcher, session token rotation, bounded parser, request-ID replay protection and protocol test harness. Local test execution was unavailable because javac is not installed in this workspace; CI will compile and run the real-socket tests using a fake action handler. APK compile/lint and native Termux/Android tests pending. No Hermes agent API integration yet.
