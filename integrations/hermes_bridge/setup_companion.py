#!/usr/bin/env python3
"""Install native companion backend into existing private Hermes Python. No provider/Telegram changes."""
import argparse
import hashlib
import importlib.metadata
import importlib.util
import json
import os
from pathlib import Path
import shlex
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

SOURCE_COMMIT = "f54c454ca2d1787bdeb2797ee2baa73f504c026c"
SERVER_SHA256 = "5cd4e6d865c2e35d70f9bc4a6981445097511ef691674b9abc9d8f26f697b53d"
ACP_REQUIREMENT = "agent-client-protocol==0.9.0"  # Exact Hermes v2026.5.29.2 optional dependency.


class SetupStop(Exception):
    pass


def install(path, data, executable=False):
    if path.is_symlink() or path.parent.is_symlink():
        raise SetupStop("private_path_required")
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    path.parent.chmod(0o700)
    if path.exists() and path.read_bytes() != data:
        backup = path.with_name(path.name + ".before-native-" + str(time.time_ns()))
        shutil.copy2(path, backup); backup.chmod(0o600)
    fd, name = tempfile.mkstemp(dir=path.parent, prefix=".native-")
    try:
        with os.fdopen(fd, "wb") as out:
            out.write(data); out.flush(); os.fsync(out.fileno())
        os.chmod(name, 0o700 if executable else 0o600)
        os.replace(name, path)
    finally:
        if os.path.exists(name): os.unlink(name)


def verify(data):
    if len(data) > 131072 or hashlib.sha256(data).hexdigest() != SERVER_SHA256:
        raise SetupStop("Backend integrity verification failed")
    return data


def main(install_acp=False, launch=True):
    directory = Path.home()/".local/share/hermes-android-bridge"
    python = directory/"mcp-env/bin/python"
    if not python.exists() or not (directory/"bridge_mcp.py").is_file():
        raise SetupStop("Existing private Android bridge setup is required; no Hermes reinstall attempted")
    if Path(sys.prefix).absolute() != (directory/"mcp-env").absolute():
        os.execv(str(python), [str(python), str(Path(__file__).absolute()), *(["--install-acp"] if install_acp else []), *([] if launch else ["--no-launch"])])
    if sys.prefix == sys.base_prefix:
        raise SetupStop("Refusing system-Python package installation")
    try: available = importlib.metadata.version("agent-client-protocol") == "0.9.0"
    except importlib.metadata.PackageNotFoundError: available = False
    if not available:
        if not install_acp: raise SetupStop("Run with --install-acp to install the exact optional ACP SDK privately")
        print("INSTALL: " + ACP_REQUIREMENT + " in existing private Python; Hermes itself is not upgraded.", flush=True)
        subprocess.run([sys.executable, "-m", "pip", "install", ACP_REQUIREMENT], check=True)
    import acp
    if importlib.util.find_spec("acp_adapter") is None:
        raise SetupStop("Existing Hermes distribution lacks acp_adapter; report this result without reinstalling")
    sys.path.insert(0, str(directory))
    from bridge_mcp import BridgeClient
    bridge = BridgeClient()
    token = os.environ.get("HERMES_BRIDGE_TOKEN", "")
    if token: bridge.pair(token)
    status = bridge.request("android_bridge_status")
    if status.get("bridge") != "ready": raise SetupStop("Start and pair the current Android bridge")
    print("PASS: Android bridge pairing; existing private ACP SDK0.9.0 imports.", flush=True)
    source = "https://raw.githubusercontent.com/techwithrakibofficial/Hermes-Android-Bridge/"+SOURCE_COMMIT+"/integrations/hermes_bridge/companion_server.py"
    with urllib.request.urlopen(source, timeout=30) as response: data = verify(response.read(131073))
    path = directory/"companion_server.py"
    install(path, data)
    print("PASS: pinned companion backend SHA256 verified.", flush=True)
    probe = subprocess.run([sys.executable, str(path), "--doctor"], capture_output=True, text=True, timeout=120)
    if probe.returncode:
        raise SetupStop("Existing Hermes ACP initialize failed; no provider or credential reset attempted")
    print("PASS: actual installed Hermes ACP initialize handshake; no inference or Android action executed.", flush=True)
    launchers=[]
    for name, args in [("hermes-companion", []), ("hermes-companion-all", ["--with-gateway"])]:
        command = [sys.executable, str(path), *args]
        target = directory/name
        install(target, ("#!/bin/sh\nexec " + " ".join(shlex.quote(part) for part in command) + "\n").encode(), executable=True)
        launchers.append(target)
    print("PASS: companion launchers saved. Provider, Telegram, cron and memory configuration preserved.", flush=True)
    print("LAUNCHER: " + str(launchers[1]), flush=True)
    if not launch: return 0
    # Never kill/rebind an existing server or replace an unrelated gateway.
    client = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    request = urllib.request.Request("http://127.0.0.1:18766/v1/health", headers={"Authorization":"Bearer "+bridge.read_token()})
    try:
        with client.open(request, timeout=2) as response:
            current = json.loads(response.read(4096))
        if current.get("backend") == "ready":
            print("PASS: companion already running. Reconnect in the app; no second gateway was started.", flush=True)
            return 0
    except (OSError, ValueError, urllib.error.URLError): pass
    print("STARTING: native companion + existing Telegram gateway. Keep this Termux session running.", flush=True)
    os.execv(sys.executable, [sys.executable, str(path), "--with-gateway"])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--install-acp", action="store_true")
    parser.add_argument("--no-launch", action="store_true")
    args = parser.parse_args()
    try: raise SystemExit(main(args.install_acp, not args.no_launch))
    except SetupStop as error:
        print("STOP: " + str(error), flush=True)
        raise SystemExit(1)
    except Exception:
        print("STOP: native companion setup failed. Share only visible PASS/STOP or missing-package lines, never tokens/config backups.", flush=True)
        raise SystemExit(1)
