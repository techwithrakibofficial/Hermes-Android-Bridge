# Hermes MCP adapter (staged integration)

This separate stdlib-only Python adapter exposes three legacy MCP stdio tools to Hermes: android_bridge_status, android_home and android_back. It never edits Hermes or Telegram files. Android app 0.2.0 already provides the needed endpoints.

## Current status

The user verified native Termux → Android Back and bridge Stop behavior and indicated completion after status/Home instructions. Installed PRoot alias is ubuntu; Hermes v0.15.2 (2026.5.29.2) runs at /usr/local/bin/hermes with Python 3.14.4.

17 local adapter tests pass, including fake loopback HTTP and stdio handshake/tool discovery. Real Hermes discovery, PRoot network access and real Android tool execution via this adapter are **not tested yet**. The transport implements the legacy initialize handshake used by the listed 2024/2025 MCP protocol versions, not the newer 2026 discovery protocol.

## Installation and registration

Exact v2026.5.29.2 upstream parser and mcp_config source confirm: hermes mcp add android-bridge --command python3 --args /private/path/bridge_mcp.py. It discovers tools, asks Enable all 3 tools [Y/n/select], then saves the entry. No further top-level help is required.

setup_android_bridge.py installs the pinned SHA256-verified adapter, validates and pairs the current Android session, privately backs up config.yaml, invokes the installed Hermes CLI for registration, verifies all 3 tools using the installed client's discovery function, and starts the foreground gateway. Run inside Ubuntu with HERMES_BRIDGE_TOKEN explicitly supplied from native Termux. Stop the old foreground gateway first; keep the Android app bridge running. A conflicting/disabled existing entry is not overwritten. No token is placed in Hermes config.

Local tests total37 (17 adapter,13 recovery,7 setup). Actual installed Hermes SDK discovery and Android tool execution still require the user's phone run.

## Pairing design

The app generates a new token each time its bridge starts. Copy the app pairing export into native Termux. Explicitly pass that variable into Ubuntu when running the script's --pair command; never assume an already-running Telegram gateway inherited a newly exported token.

`python3 bridge_mcp.py --pair` reads HERMES_BRIDGE_TOKEN, checks authenticated bridge status, then atomically saves only that token in `$HOME/.config/hermes-android-bridge/connection.json` inside Ubuntu, directory mode 700/file mode 600. This is adapter-owned data, separate from ~/.hermes. The MCP process rereads it per tool call, so token renewal does not require MCP restarts. Never send the token/config file to chat or commit it. Pair again after every Android bridge restart. `--status` checks pairing without starting stdio.

## Protocol and security limits

- Fixed 127.0.0.1:18765 address and allowlisted paths. No URL, shell or arbitrary Android command arguments.
- Proxy use and HTTP redirects disabled; bounded response reads and five-second timeout.
- New unique request ID per intentional action, no automatic action retry.
- Failed/ambiguous requests return tool errors; successful requests report Android acceptance, never visual completion.
- Tool discovery works before pairing. Actual status/action tools require a valid private pairing file.
- stdout contains only newline-delimited JSON-RPC while serving. No tokens printed or raw response errors returned.
- This local session design does not defend against root, compromised OS or stolen private files/clipboard.
- No chat UI, microphone, settings automation, text input or messaging feature is added here.

## Test

```sh
python3 -m unittest discover -s integrations/hermes_bridge -p 'test_*.py' -v
```
