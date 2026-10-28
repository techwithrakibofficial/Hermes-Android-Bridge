# Technical decisions

- Existing Hermes Agent v0.15.2 remains in Termux Ubuntu PRoot. Its configuration and Telegram pairing are out of scope for replacement.
- First companion milestone deliberately isolates device capability testing from chat integration. EMI-locked Developer Options do not prove whether Accessibility is available.
- Kotlin 2.1.20 with matching Compose compiler plugin, AGP 8.9.2, JDK 17, Gradle 8.11.1, Compose BOM 2025.04.01. Versions are pinned; update them together after CI validation.
- AccessibilityService has no window-content retrieval or gesture capability in milestone 1. Android alone binds it through BIND_ACCESSIBILITY_SERVICE; there is no exported custom command receiver.
- The app exposes only explicit Home/Back button tests. A true global-action return value is reported as Android acceptance, never as verified completion.
- No network endpoint or Hermes API is assumed or shipped yet. Next milestone must authenticate loopback requests and implement a small command allowlist.
- App UI uses Bangla explanatory copy and English test names so phone test reports can identify controls precisely.
- APK is development-only with default debug signing. Long-term stable signing remains open.
- Official reference: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Compose compiler setup: https://developer.android.com/develop/ui/compose/compiler

## Speech-rate patch (0.1.1)
Default speech rate 0.70, selectable 0.25–1.25 in 0.05 increments. App-private SharedPreferences stores the chosen value. Apply rate during initialization and before each utterance; handle the engine error return. Stop current speech when the slider changes, then require an explicit Test tap. Engine-specific audible results require device confirmation. No system TTS or Termux setting is modified.

## Local bridge milestone (0.2.0)
Host the explicitly activated listener inside the enabled AccessibilityService; stop on unbind/destroy/interruption or user Stop. No auto-start or public endpoint. Ephemeral 256-bit token per session, no persisted secret, 127.0.0.1:18765 only, authenticated status and allowlisted Home/Back POSTs. Reserve request IDs before dispatch and never evict within a 256-action session. Main-thread FutureTask dispatch cancels a queued task on timeout; timeout responses do not prove non-execution if action was already running. No automatic retries. INTERNET permission is added solely because socket access requires it; no outbound network code is added by the bridge. Network TTS remains the user's existing engine opt-in.
