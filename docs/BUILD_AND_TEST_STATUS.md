# Build and test status — 0.2.0
Updated 2026-10-28, Asia/Dhaka.

| Check | Result |
| --- | --- |
| 20 real-loopback JVM protocol checks, fake Android handler | PASS |
| assembleDebug / lintDebug | PASS |
| Artifact upload / prerelease publication | PASS |
| Existing voice controls | User says all okay |
| Native Termux authenticated status | Awaiting phone test |
| Remote Home/Back visible result | Awaiting phone test |
| Stop/restart/old-token invalidation on Android | Awaiting phone test |
| Accessibility process background lifetime | Awaiting phone test |
| Existing Telegram health | Not independently verified |
| Hermes adapter/chat/microphone | Not implemented |

Code commit: 35595abb33466daa7d2584c78d2eac06848c2054
Successful CI: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40608250521
Release: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-5
APK: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-5/Hermes-Bridge-0.2.0.apk
Size: 9337200 bytes.

Protocol test coverage: bind address, token format/auth, status, browser-origin rejection, GET cannot act, unknown action/missing ID/body/duplicate-header rejection, Home/Back fake dispatch, replay protection, oversized headers, bounded session without ID eviction, token rotation, Android rejection response and reserved failed IDs.
Local javac execution was unavailable; CI performed the tests with JDK17. No emulator/device action tests ran. Accepted means Android accepted a request; verify the phone screen.
