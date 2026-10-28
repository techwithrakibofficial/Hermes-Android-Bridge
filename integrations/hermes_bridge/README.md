# Hermes Android MCP integration0.4

The stdlib-only legacy MCP stdio adapter exposes19 Android tools. The existing
Hermes client needs mcp==1.26.0 in a private Python environment; the pinned updater
reuses the already-working environment without upgrading/reinstalling Hermes.
See [full phone upgrade](../../docs/UPGRADE_0_4.md).

## Tools

Status, Home, Back, Settings, six app aliases; screen snapshot, installed launchable
apps, basic device state; click, long click, scroll, replace editable text; open
one discovered package; numbered dialer (not automatic call); speak, stop speech;
Recents, notification shade and Quick Settings. No raw shell/URL/arbitrary intent.

The adapter itself changes no Hermes settings. Setup verifies the pinned source,
backs up config, preserves unrelated MCP entries and existing filters, probes real
Hermes discovery before gateway launch, and optionally appends a backed-up marked
companion block in SOUL.md with --companion. It does not create check-in jobs.

## Runtime and pairing

App token rotates on every bridge Start. Copy its export into native Termux and
explicitly forward HERMES_BRIDGE_TOKEN into Ubuntu when updating/pairing. Pairing
checks authenticated status before privately writing
~/.config/hermes-android-bridge/connection.json (dir700/file600). Every tool call
rereads it; already-running gateway shell environment does not need token updates.
Never upload pairing files, .env, backups or copied exports.

Use the saved private launcher, not global Hermes's Python shebang:

```sh
proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run
```

## Protocol

Fixed IPv4 localhost, proxy/redirect rejection, current Bearer token, unique request
ID per intentional mutation, no automatic retries. Input schemas reject extra
fields, unknown aliases, arbitrary URL/package syntax and dialer service codes.
JSON UTF-8 command bodies <=8192 bytes; text <=2000 chars; response <=256KiB and
5-second client timeout. Android may accept an action without completing it;
observe again. Ambiguous requests must not be repeated blindly.

Screen observations need the app's separate per-session opt-in; personal visible
text may reach the configured model provider. Only one fresh snapshot can target
an action; screen changes, invalid window/target, age>60s or Stop revoke it.
Password controls omitted, but mislabeled sensitive text cannot be guaranteed.
Legacy initialize versions2024/2025 are supported; no2026-protocol claim.

## Evidence

59 tests passed on Python3.12/3.14 in run40642075403. They use fake HTTP/stdio and
private temporary files; they are not proof of0.4 Android UI outcomes. Earlier
phone MCP1.26 installation, pairing and3-tool discovery passed in actual user logs;
status/Home completion was user-reported. See current handoff for distinctions.
