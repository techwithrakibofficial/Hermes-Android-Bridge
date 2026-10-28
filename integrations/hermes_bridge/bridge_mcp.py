#!/usr/bin/env python3
"""Small legacy MCP stdio adapter for Hermes v0.15.x; Python stdlib only.

Does not read or edit Hermes/Telegram configuration. Pair explicitly after every
Android bridge restart. stdout is reserved for MCP JSON-RPC while serving.
"""
import argparse
import json
import os
from pathlib import Path
import re
import stat
import sys
import tempfile
import urllib.error
import urllib.request
import uuid

BASE_URL = "http://127.0.0.1:18765"
SUPPORTED_PROTOCOLS = {"2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25"}
MAX_MESSAGE = 65536
SUPPORTED_APPS = ("whatsapp", "facebook", "messenger", "telegram", "chrome", "termux")
TOOL_PATHS = {
    "android_bridge_status": ("GET", "/v1/status"),
    "android_home": ("POST", "/v1/actions/home"),
    "android_back": ("POST", "/v1/actions/back"),
    "android_open_settings": ("POST", "/v1/actions/settings"),
    "android_open_app": ("POST", None),
}
DESCRIPTIONS = {
    "android_open_settings": "Open normal Android Settings when the user asks. Does not change settings or bypass protected restrictions. Verify the phone screen; never automatically retry.",
    "android_open_app": "Open one supported installed app by its exact alias when the user asks. Does not type, click inside apps, send messages or make calls. Verify the phone screen; never automatically retry.",
    "android_bridge_status": "Check the local Android companion bridge only, not Hermes or Telegram health. No Android action.",
    "android_home": "Go to the Android launcher only when the user asks. accepted:true is Android acceptance, not proof of visible completion. Never automatically retry.",
    "android_back": "Perform Android Back only when the user asks. May close the keyboard or leave the current screen. Never automatically retry or claim visual completion.",
}


def valid_arguments(name, arguments):
    if not isinstance(arguments, dict):
        return False
    if name == "android_open_app":
        return set(arguments) == {"app"} and arguments["app"] in SUPPORTED_APPS
    return not arguments


def input_schema(name):
    if name == "android_open_app":
        return {"type": "object", "properties": {"app": {"type": "string", "enum": list(SUPPORTED_APPS)}},
                "required": ["app"], "additionalProperties": False}
    return {"type": "object", "properties": {}, "additionalProperties": False}


class BridgeError(Exception):
    """Safe user-facing error; never contains token or raw HTTP response."""


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class BridgeClient:
    def __init__(self, config_path=None):
        self.config_path = Path(config_path) if config_path else Path.home() / ".config/hermes-android-bridge/connection.json"
        # Never send localhost tokens through a user's HTTP(S) proxy or redirects.
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())

    def read_token(self):
        try:
            info = self.config_path.lstat()
            if not stat.S_ISREG(info.st_mode) or info.st_mode & 0o077 or info.st_size > 2048:
                raise BridgeError("Pairing file must be a private regular file (mode 600). Pair again.")
            data = json.loads(self.config_path.read_text())
            token = data.get("token") if isinstance(data, dict) else None
            if not isinstance(token, str) or not re.fullmatch(r"[A-Za-z0-9_-]{43}", token):
                raise BridgeError("Pairing file is invalid. Pair again with the current Android bridge session.")
            return token
        except (OSError, ValueError):
            raise BridgeError("Not paired. Start the Android bridge and run the adapter's --pair command.") from None

    def request(self, name, token=None, arguments=None):
        if name not in TOOL_PATHS:
            raise BridgeError("Unsupported Android action.")
        arguments = {} if arguments is None else arguments
        if not valid_arguments(name, arguments):
            raise BridgeError("Unsupported app or unexpected tool arguments.")
        token = token if token is not None else self.read_token()
        method, path = TOOL_PATHS[name]
        if name == "android_open_app":
            path = "/v1/actions/open-app/" + arguments["app"]
        headers = {"Authorization": "Bearer " + token}
        if method == "POST":
            headers["X-Request-Id"] = uuid.uuid4().hex
        request = urllib.request.Request(BASE_URL + path, method=method, headers=headers,
                                         data=b"" if method == "POST" else None)
        try:
            with self.opener.open(request, timeout=5) as response:
                raw = response.read(4097)
                if len(raw) > 4096:
                    raise BridgeError("Invalid bridge response. Do not retry an action automatically.")
                data = json.loads(raw)
                if name == "android_bridge_status" and isinstance(data, dict) and data.get("bridge") == "ready":
                    actions = data.get("allowed_actions", [])
                    apps = data.get("supported_apps", [])
                    return {"bridge": "ready",
                            "allowed_actions": [item for item in actions if item in ("home", "back", "settings", "open_app")] if isinstance(actions, list) else [],
                            "supported_apps": [item for item in apps if item in SUPPORTED_APPS] if isinstance(apps, list) else []}
                if method == "POST" and isinstance(data, dict) and data.get("accepted") is True:
                    return {"accepted": True, "verification": "check_phone_screen", "automatic_retry": False}
                raise BridgeError("Bridge did not confirm the request. Check the phone; do not automatically retry.")
        except urllib.error.HTTPError as error:
            messages = {
                401: "Pairing token expired or incorrect. Start/copy/pair the current bridge session again.",
                409: "Request already used. Check the phone; do not automatically retry.",
                429: "Bridge session limit reached. Stop/start the bridge and pair again.",
                503: "Android action was rejected, app unavailable/blocked, or request timed out. Check the phone and do not automatically retry.",
            }
            raise BridgeError(messages.get(error.code, "Bridge HTTP request rejected. No automatic retry.")) from None
        except (OSError, urllib.error.URLError, TimeoutError, ValueError):
            raise BridgeError("Bridge unreachable or response interrupted. Check Start/Stop on the phone. An action may have run; do not automatically retry.") from None

    def pair(self, token):
        if not isinstance(token, str) or not re.fullmatch(r"[A-Za-z0-9_-]{43}", token):
            raise BridgeError("Current HERMES_BRIDGE_TOKEN is missing/invalid. Copy the app pairing command first.")
        self.request("android_bridge_status", token=token)  # Verify before persisting anything.
        directory = self.config_path.parent
        directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        if directory.is_symlink():
            raise BridgeError("Pairing directory must not be a symbolic link.")
        directory.chmod(0o700)
        temp_path = None
        try:
            fd, temp_path = tempfile.mkstemp(dir=directory, prefix=".pair-")
            with os.fdopen(fd, "w") as stream:
                json.dump({"version": 1, "token": token}, stream)
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temp_path, self.config_path)
        finally:
            if temp_path and os.path.exists(temp_path):
                os.unlink(temp_path)


class MCPAdapter:
    def __init__(self, client=None):
        self.client = client or BridgeClient()
        self.initialized = False

    def handle(self, message):
        if not isinstance(message, dict) or message.get("jsonrpc") != "2.0" or not isinstance(message.get("method"), str):
            return self.error(None, -32600, "Invalid request")
        if "id" not in message:  # No replies to MCP notifications.
            return None
        request_id = message["id"]
        method = message["method"]
        params = message.get("params", {})
        if not isinstance(params, dict):
            return self.error(request_id, -32602, "Invalid params")
        if method == "initialize":
            requested = params.get("protocolVersion")
            protocol = requested if requested in SUPPORTED_PROTOCOLS else "2025-06-18"
            self.initialized = True
            result = {"protocolVersion": protocol, "capabilities": {"tools": {}},
                      "serverInfo": {"name": "hermes-android-bridge", "version": "0.1.0"}}
        elif method == "ping":
            result = {}
        elif not self.initialized:
            return self.error(request_id, -32000, "Initialize first")
        elif method == "tools/list":
            result = {"tools": [{"name": name, "description": DESCRIPTIONS[name],
                                  "inputSchema": input_schema(name)}
                                 for name in TOOL_PATHS]}
        elif method == "tools/call":
            name = params.get("name")
            arguments = params.get("arguments", {})
            if not isinstance(name, str) or name not in TOOL_PATHS or not valid_arguments(name, arguments):
                return self.error(request_id, -32602, "Unknown tool or unexpected arguments")
            try:
                data = self.client.request(name, arguments=arguments)
                result = {"content": [{"type": "text", "text": json.dumps(data)}], "isError": False}
            except BridgeError as error:
                result = {"content": [{"type": "text", "text": str(error)}], "isError": True}
        else:
            return self.error(request_id, -32601, "Method not found")
        return {"jsonrpc": "2.0", "id": request_id, "result": result}

    @staticmethod
    def error(request_id, code, text):
        return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": text}}

    def serve(self):
        while True:
            line = sys.stdin.buffer.readline(MAX_MESSAGE + 1)
            if not line:
                return
            if len(line) > MAX_MESSAGE:
                print("MCP input size limit exceeded", file=sys.stderr)
                return
            try:
                message = json.loads(line)
                response = self.handle(message)
            except (ValueError, TypeError):
                response = self.error(None, -32700, "Parse error")
            if response is not None:
                print(json.dumps(response, ensure_ascii=False), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pair", action="store_true", help="Verify/save current token from HERMES_BRIDGE_TOKEN")
    parser.add_argument("--status", action="store_true", help="Print authenticated bridge status without starting MCP")
    args = parser.parse_args()
    try:
        client = BridgeClient()
        if args.pair:
            client.pair(os.environ.get("HERMES_BRIDGE_TOKEN"))
            print("Android bridge paired. No Hermes or Telegram settings were changed.")
        elif args.status:
            print(json.dumps(client.request("android_bridge_status")))
        else:
            MCPAdapter(client).serve()
        return 0
    except BridgeError as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
