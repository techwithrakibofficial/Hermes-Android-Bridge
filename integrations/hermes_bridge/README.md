# Hermes MCP adapter (staged integration)

This separate stdlib-only Python adapter exposes three legacy MCP stdio tools to Hermes: android_bridge_status, android_home and android_back. It never edits Hermes or Telegram files. Android app 0.2.0 already provides the needed endpoints.

## Current status

The user verified native Termux → Android Back and bridge Stop behavior and indicated completion after status/Home instructions. Installed PRoot alias is ubuntu; Hermes v0.15.2 (2026.5.29.2) runs at /usr/local/bin/hermes with Python 3.14.4.

17 local adapter tests pass, including fake loopback HTTP and stdio handshake/tool discovery. Real Hermes discovery, PRoot network access and real Android tool execution via this adapter are **not tested yet**. The transport implements the legacy initialize handshake used by the listed 2024/2025 MCP protocol versions, not the newer 2026 discovery protocol.

## Installation and registration gate

Next inspect the actual installed command before providing a registration invocation:

```sh
proot-distro login ubuntu -- bash -lc 'hermes mcp add --help'
```

Do not blindly rewrite ~/.hermes/config.yaml or choose hermes mcp serve: serve exposes Hermes conversations to another agent, while this integration adds Android tools to Hermes. The adapter runs with `python3 bridge_mcp.py` over stdio. Install the script at a private, separately managed path after confirming the installed CLI options. No pip dependency or Hermes upgrade is required by the script.

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
