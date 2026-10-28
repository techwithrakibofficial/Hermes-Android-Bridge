# Local bridge test — 0.2.0

This milestone connects native Termux to the Android companion. It does NOT connect chat to Hermes yet.

## Phone steps
1. Install the 0.2.0 APK and enable the existing Hermes Bridge Accessibility service if necessary.
2. In the app, tap **Start local bridge**, read the Home/Back disclosure and confirm. The app should show Listening on 127.0.0.1:18765.
3. Tap **Copy Termux pairing command**, open your native Termux shell and paste it there. The command sets HERMES_BRIDGE_TOKEN and HERMES_BRIDGE_URL. Keep the real token out of chat, screenshots and GitHub.
4. In that same Termux session run:
```sh
curl --max-time 5 --noproxy '*' -sS -H "Authorization: Bearer $HERMES_BRIDGE_TOKEN" "$HERMES_BRIDGE_URL/v1/status"
```
Expected authenticated result:
```json
{"bridge":"ready","allowed_actions":["home","back"]}
```
5. Test Home:
```sh
curl --max-time 5 --noproxy '*' -sS -X POST -H "Authorization: Bearer $HERMES_BRIDGE_TOKEN" -H "X-Request-Id: home-$(date +%s)-$RANDOM-$RANDOM" "$HERMES_BRIDGE_URL/v1/actions/home"
```
The phone should display its launcher. The response accepted:true means Android accepted the request; verify the actual screen.
6. Reopen Termux. Test Back only when it is safe to navigate away from its current screen:
```sh
curl --max-time 5 --noproxy '*' -sS -X POST -H "Authorization: Bearer $HERMES_BRIDGE_TOKEN" -H "X-Request-Id: back-$(date +%s)-$RANDOM-$RANDOM" "$HERMES_BRIDGE_URL/v1/actions/back"
```
7. Reopen Bridge and press **Stop local bridge**. The status request should no longer connect. Restarting creates a new token, so copy the new pairing command. Disabling Accessibility or process termination also ends the session.
8. Confirm the old Telegram bot still responds without changing configuration.

If curl is missing, install only curl using Termux's package manager; do not reinstall Hermes or Termux:API. A connection-refused result means the listener is stopped, could not bind, or the Android process ended. Reopen Bridge and check its state. An unauthorized response means the token is missing/wrong/old; copy the current pairing command again.
Default debug keys can differ between builds. If Android rejects the update certificate, reinstall only the Bridge app and manually enable its service again, preserving Termux/Telegram/Hermes.

## Protocol and limits
- IPv4 loopback only: 127.0.0.1:18765, no public/LAN listener, tunnel or port forwarding.
- HTTP/1.1, exact Host header and bearer token for every endpoint. No CORS/browser origin.
- GET /v1/status is bridge readiness, not Hermes health.
- POST /v1/actions/home and /v1/actions/back only. No body. X-Request-Id must be 8–80 ASCII letters, digits, underscore or hyphen; use a fresh value for each intentional command.
- Duplicate IDs return 409 and never dispatch again within the session. Failed/timed-out actions reserve the ID too. Never automatically retry an action after an ambiguous timeout.
- Up to 256 action IDs per session; then 429 requires a new explicitly started session and token. No replay-cache eviction within a session.
- Bounded headers, line length and read deadline; one request per connection; close after response.
- Dispatch happens on Android's main thread with a bounded wait. Background lifetime depends on Android's Accessibility service/process; no uptime guarantee.
- The token is in memory only, excluded from logs and hidden from UI. It is explicitly copied to the user's local shell. Anyone with the current token on the device can invoke the allowed actions.
- Plain HTTP is confined to local loopback. This is not protection against root, debugger access, compromised apps/OS or stolen clipboard/shell contents.

## Verification status
Protocol tests run on a JVM with a fake Android handler. They cannot verify actual Home/Back effects, Android background behavior, or PRoot/Hermes integration. Report status response, Home/Back screen behavior and Stop behavior without the token.
