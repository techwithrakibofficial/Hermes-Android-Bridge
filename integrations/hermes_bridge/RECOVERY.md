# Existing gateway recovery

The latest user log has no Home Assistant or Telegram network error, only the
startup allowlist warning. That warning alone does not prove gateway failure:
v2026.5.29.2's startup check omits the pairing-approved list although its runtime
authorization accepts paired users. An actual Telegram reply remains unverified.

`recover_gateway.py` is a scoped helper for the existing Ubuntu PRoot. Stop the
current foreground gateway with Ctrl+C before running it. No root, upgrade,
reinstallation, new pairing, provider/model edits or allow-all setting is used.

It checks the saved bot token and approved Telegram owners. It will not invent
an owner ID if none is saved. It tests Telegram's read-only getMe endpoint over
HTTPS (20 second timeout). Network/auth failures stop before settings edits and
never print a token-containing URL or raw exception. Telegram SOCKS proxies need
their existing supported client path; this helper stops instead of ignoring them.

If preflight passes, it privately backs up .env and config.yaml in
~/.hermes/android-bridge-recovery/<timestamp> (directory700/files600). It changes
only HASS_TOKEN and TELEGRAM_ALLOWED_USERS assignment lines in .env. Existing
numeric allowlisted users are preserved, and existing pairing-approved users are
added. All other lines/credentials remain unchanged. config.yaml, pairing files,
memory and provider settings are not edited. Duplicate HASS_TOKEN assignments are
all disabled. Repeating an already-correct repair does not make another backup.

It reloads effective config and refuses to start if Home Assistant is still
enabled or Telegram is unconfigured. With --run, it execs `hermes gateway run`
in the foreground. Android can still terminate Termux; background uptime is not
guaranteed. A successful getMe check does not verify long polling, provider
inference or a complete chat round trip. The final acceptance test is the user's
Telegram message receiving an AI reply.

Expected output stages: CHECK, PASS, BACKUP, PASS, STARTING. If STOP appears,
report that sanitized line. Do not send .env, backup files or pairing codes.

Tests use fake Telegram HTTP and temporary files, not the user's account.
