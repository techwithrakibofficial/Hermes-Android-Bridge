# Local bridge protocol —0.4.0

For current install/pair/device steps use [UPGRADE_0_4.md](UPGRADE_0_4.md).
The former0.2 native Home/Back test is retained by the same action routes.

## Transport

User-started IPv4 loopback127.0.0.1:18765 only. Each session has a fresh random
256-bit Bearer token. Exact Host127.0.0.1:18765 required; Origin and duplicate
headers rejected. No public/LAN listener, CORS, redirects, proxy or tunnel.
Each response is JSON/no-store/connection-close. Bounded ASCII headers/read
request deadline2s; main-thread dispatch wait1.2s; client timeout5s. Token is
private memory on Android and private mode600 pairing file in Ubuntu.

Authenticated GET routes: /v1/status, /v1/screen, /v1/apps, /v1/device.
Screen requires separate per-session screen-control opt-in. Other observations
require an unlocked phone. Status is bridge readiness, not Hermes health.

POST bodyless actions: /v1/actions/home, back, settings, recents, notifications,
quick_settings, stop_speech; /v1/actions/open-app/<one of six known aliases>.

POST JSON commands: /v1/commands/click, long-click, scroll, set-text,
open-package, dial, speak. Exact Content-Type application/json and decimal
Content-Length1..8192, valid UTF-8 required. No transfer encoding. Strict
per-command keys/types checked on Android and in MCP input schemas.

Mutations require X-Request-Id8..80 ASCII letters/digits/underscore/hyphen.
Duplicate409; all attempted IDs remain reserved even on failure/timeout, with
256 IDs/session maximum then429. No eviction or automatic retry. Malformed or
unknown actions never dispatch. New session invalidates the old token.

## UI actions

Screen snapshot returns up to120 visible nodes, depth24/child80 limits,
120-character text/description fields, class/view ID, bounds, interaction flags,
parent IDs and a random snapshot ID. Password-control text/descriptions/children
omitted. App mislabeling may still reveal sensitive data. Tree is not stored to
disk; screen text sent through Hermes may reach its configured model provider.

One precise action consumes the snapshot. Current window/package, event revision,
60s age, refreshed-node signature, visible/enabled/non-password state and supported
action are checked. Long-click/click/scroll use Accessibility node actions;
set-text replaces/clears an editable field. No coordinate gesture fallback.
Reobserve to verify the actual result; accepted is not task completion.

App discovery is limited to launchable activities visible through a launcher
intent query, not QUERY_ALL_PACKAGES. Open-package must be currently launchable.
Dial opens ACTION_DIAL with digits/optional plus only, not call placement or AI
call audio. Speech queues selected TTS voice/speed, not proof sound was heard.

## Stop and evidence

Stop closes listener/active socket, rotates/revokes session, clears snapshots and
screen permission, stops remote TTS. Disable Accessibility/process death also
ends the session. A started Android action cannot be undone by closing a socket.
No background-uptime guarantee or protection against compromised OS/root/token
file theft. No root/ADB/EMI bypass is implemented.

Run40642075456 passed compile/lint and46 real-socket checks with a fake Android
handler. These do not test real Accessibility node correctness or app outcomes.
See BUILD_AND_TEST_STATUS.md and AI_HANDOFF.md for actual phone evidence.
