#!/usr/bin/env python3
"""Install/pair/register this adapter with existing Hermes, then run gateway.

Run in the existing Ubuntu PRoot after stopping the old foreground gateway and
passing the Android app's current HERMES_BRIDGE_TOKEN into this process.
"""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request

SOURCE = "https://raw.githubusercontent.com/techwithrakibofficial/Hermes-Android-Bridge/0fa73edc21dcf6597b04026ac8416270432517b1/integrations/hermes_bridge/bridge_mcp.py"
SOURCE_SHA256 = "49fe8a9b8c645fcd68b5faf0ba8a5dd16021acc708e5db21f75ae8a9786149a0"
SERVER_NAME = "android-bridge"
EXPECTED_TOOLS = {"android_bridge_status", "android_home", "android_back"}


class SetupError(Exception):
    pass


def verified_source(data):
    if len(data) > 131072 or hashlib.sha256(data).hexdigest() != SOURCE_SHA256:
        raise SetupError("Adapter download verification failed. No installation performed.")
    return data


def install_file(path, data):
    if path.is_symlink() or path.parent.is_symlink():
        raise SetupError("Adapter path must not be a symbolic link.")
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    path.parent.chmod(0o700)
    if path.exists() and path.read_bytes() != data:
        backup = path.with_name(path.name + ".before-setup-" + str(time.time_ns()))
        shutil.copy2(path, backup)
        backup.chmod(0o600)
    temp_name = None
    try:
        fd, temp_name = tempfile.mkstemp(dir=path.parent, prefix=".install-")
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temp_name, path)
    finally:
        if temp_name and os.path.exists(temp_name):
            os.unlink(temp_name)


def config_is_ours(entry, path):
    if not isinstance(entry, dict) or entry.get("enabled") is False:
        return False
    return entry.get("command") in {sys.executable, "python3"} and entry.get("args") == [str(path)]


def load_adapter(path):
    spec = importlib.util.spec_from_file_location("phone_android_bridge", path)
    if spec is None or spec.loader is None:
        raise SetupError("Could not load the installed adapter.")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    try:
        token = os.environ.get("HERMES_BRIDGE_TOKEN", "")
        if not token:
            raise SetupError("Start the app bridge and paste its Copy Termux pairing command into native Termux first.")
        import yaml
        from hermes_cli.config import get_hermes_home
        home = Path(get_hermes_home())
        config_path = home / "config.yaml"
        original = yaml.safe_load(config_path.read_text()) or {}
        path = Path.home() / ".local/share/hermes-android-bridge/bridge_mcp.py"
        existing = (original.get("mcp_servers") or {}).get(SERVER_NAME)
        if existing is not None and not config_is_ours(existing, path):
            raise SetupError("A different/disabled android-bridge MCP entry already exists. It was not overwritten.")
        print("CHECK: downloading pinned adapter…", flush=True)
        with urllib.request.urlopen(SOURCE, timeout=30) as response:
            data = verified_source(response.read(131073))
        install_file(path, data)
        print("PASS: adapter SHA256 verified and installed privately.", flush=True)
        adapter = load_adapter(path)
        try:
            adapter.BridgeClient().pair(token)
        except adapter.BridgeError as error:
            raise SetupError(str(error)) from None
        print("PASS: Ubuntu can reach Android; current bridge session paired.", flush=True)
        backup = config_path.with_name("config.yaml.before-android-bridge-" + str(time.time_ns()))
        shutil.copy2(config_path, backup)
        backup.chmod(0o600)
        if existing is None:
            print("NEXT: Hermes will discover 3 tools. Choose Y to enable them.", flush=True)
            # Let the installed CLI own registration and tool-selection behavior.
            subprocess.run(["hermes", "mcp", "add", SERVER_NAME, "--command", sys.executable,
                            "--args", str(path)], check=True)
        saved = yaml.safe_load(config_path.read_text()) or {}
        entry = (saved.get("mcp_servers") or {}).get(SERVER_NAME)
        if not config_is_ours(entry, path):
            raise SetupError("Adapter was not saved/enabled. Gateway not started. Config backup is available.")
        from hermes_cli.mcp_config import _probe_single_server
        tools = _probe_single_server(SERVER_NAME, entry)
        if not EXPECTED_TOOLS.issubset({name for name, _ in tools}):
            raise SetupError("Hermes did not discover all 3 Android tools. Gateway not started.")
        include = (entry.get("tools") or {}).get("include")
        exclude = (entry.get("tools") or {}).get("exclude", [])
        if (include is not None and not EXPECTED_TOOLS.issubset(set(include))) or EXPECTED_TOOLS.intersection(exclude):
            raise SetupError("Some Android tools were filtered out. Enable the 3 tools before continuing.")
        print("PASS: Hermes MCP discovery verified: android_bridge_status, android_home, android_back.", flush=True)
        print("STARTING: gateway. Keep Termux and the app bridge running. Test bridge status in Telegram first.", flush=True)
        os.execvp("hermes", ["hermes", "gateway", "run"])
        return 0
    except SetupError as error:
        print("STOP: " + str(error), flush=True)
        return 1
    except Exception:
        print("STOP: setup failed. Report the visible CHECK/PASS/STOP lines. No automatic reinstall or credential reset was attempted.", flush=True)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
