# 0.3.0 — Settings and supported app launches

The user indicated all previous Telegram status/Home tests were done. Treat that
as user-reported end-to-end evidence, not independent screen observation.

This milestone adds two MCP tools and two narrow HTTP routes:

- android_open_settings → POST /v1/actions/settings (no arguments)
- android_open_app(app) → POST /v1/actions/open-app/<alias>

Aliases: whatsapp, facebook, messenger, telegram, chrome, termux. Exact packages
are declared through manifest queries; QUERY_ALL_PACKAGES is not requested.
An unavailable/blocked app returns rejection; no app is installed, no login is
performed and no message/call is sent. No arbitrary package, URL or deep-link
input is accepted. Screen retrieval/gestures remain disabled. An accepted launch
request must still be verified by looking at the phone.

## Upgrade procedure

1. Temporarily stop the foreground gateway with Ctrl+C.
2. Install APK0.3.0. If Android rejects a development signing-key mismatch,
   reinstall only Bridge and normally enable its Accessibility service again.
   Preserve Termux/Telegram/Hermes. Voice preferences may need reselection after
   Bridge-only reinstall. A development-key Actions cache starts with this build;
   cache eviction still means signing continuity is not guaranteed.
3. Start local bridge and copy its fresh pairing export into native Termux.
4. Run the updated SHA256-pinned setup helper supplied in chat. It reuses the
   private SDK runtime, updates the adapter, checks the new app capabilities,
   discovers5 tools and restarts the gateway. No Hermes upgrade/recovery loop.
5. In Telegram send /new, then ask for android_bridge_status. Expect allowed
   actions home/back/settings/open_app. Ask to open Settings, then WhatsApp.
   Report whether the correct screens actually appeared.

The existing private restart launcher remains
/root/.local/share/hermes-android-bridge/hermes-with-android gateway run.
Do not stop/restart the app bridge after pairing without renewing the token.
