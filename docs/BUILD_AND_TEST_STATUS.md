# Build and test status — 0.3.0
Updated 2026-10-28 (Asia/Dhaka).

| Check | Result |
| --- | --- |
| 0.2 Telegram status/Home | User said All done after test instructions |
| 0.2 direct Back/Stop | User explicitly confirmed |
| Telegram recovery and private SDK1.26.0 | Confirmed by user reply and pasted install/discovery output |
| 0.3 Java loopback protocol tests |30 passed in CI, fake Android handler |
| 0.3 Android assembleDebug/lintDebug | Passed |
| 0.3 artifact/release publication | Passed, dev-11 |
| Python adapter/recovery/setup suite |51 passed on both3.12 and3.14 |
| 0.3 actual Settings/app launches | Awaiting phone test |
| Screen reading/click/scroll/type | Not implemented |
| Autonomous phone diagnosis/repair | Goal recorded; not implemented |
| Companion chat/microphone UI | Not implemented |

Code: b86c29673939df243f113832efe42af92c71c64b
Android CI: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40637536913
Python CI: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40637536937
APK: https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-11/Hermes-Bridge-0.3.0.apk
APK size:9,337,448 bytes.
Instructions: docs/UPGRADE_0_3.md.
Android acceptance is not visual success; actual phone test is still required.
