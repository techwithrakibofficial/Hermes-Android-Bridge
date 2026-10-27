# Implementation milestones

1. Buildable Compose companion and actual device Accessibility/TTS capability test.
2. Authenticated loopback bridge: lifecycle/foreground requirements, local pairing token, bounded protocol, action allowlist, cancellation, disconnect/error behavior and security tests. Start with Home/Back only. Bind explicitly to 127.0.0.1; no public port forwarding or arbitrary shell endpoint.
3. Read the installed Hermes v0.15.2 supported integration points using read-only inspection. Implement a separate optional adapter without editing Telegram configuration, tokens, memory or the existing gateway.
4. Text conversation and task status, then Android speech recognition/TTS with clear active/idle state, duration and stop/cancel feedback.
5. Expand actions only after individual phone tests and consent design. Sensitive actions require explicit confirmation. Never equate an agent's spoken response with completed Android execution.
6. Stable APK signing, monotonic versioning and phone-only update workflow before broader distribution.

After every milestone update AI_HANDOFF, TECHNICAL_DECISIONS and BUILD_AND_TEST_STATUS. Device evidence is required to close milestone 1.
