#!/usr/bin/env python3
"""Back up and recover the existing Hermes Telegram gateway; never re-pair/reset.

Run inside the existing Ubuntu PRoot after stopping the foreground gateway.
Checks are staged and sanitized. A successful Bot API check is NOT an end-to-end
AI reply test; the user must send one Telegram message after startup.
"""
import argparse
import json
import logging
import os
from pathlib import Path
import re
import shutil
import socket
import ssl
import sys
import tempfile
import time
import urllib.error
import urllib.request

logging.disable(logging.CRITICAL)  # Never let HTTP libraries print bot-token URLs.


class RecoveryError(Exception):
    pass


def allowed_ids(existing, approved):
    ids = {str(value).strip() for value in approved if re.fullmatch(r"[1-9][0-9]*", str(value).strip())}
    prior = {value.strip() for value in (existing or "").split(",") if value.strip()}
    if any(not re.fullmatch(r"[1-9][0-9]*", value) for value in prior):
        raise RecoveryError("AUTH_REVIEW_NEEDED: existing Telegram allowlist has nonnumeric entries; nothing changed.")
    ids |= prior
    if not ids:
        raise RecoveryError("NO_APPROVED_OWNER: no saved Telegram pairing or numeric allowlist found. Nothing changed; do not enable allow-all.")
    return sorted(ids, key=int)


def patch_env(original, changes):
    """Change only target assignment lines. Preserve every other byte as text."""
    lines = original.splitlines(keepends=True)
    seen = set()
    out = []
    for line in lines:
        match = re.match(r"^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=", line)
        key = match.group(1) if match else None
        if key in changes:
            # Replace every duplicate too; dotenv otherwise may choose a later value.
            ending = "\r\n" if line.endswith("\r\n") else "\n"
            out.append(key + "='" + changes[key] + "'" + ending)
            seen.add(key)
        else:
            out.append(line)
    for key in changes:
        if key not in seen:
            if out and not out[-1].endswith(("\n", "\r")):
                out[-1] += "\n"
            out.append(key + "='" + changes[key] + "'\n")
    return "".join(out)


def backup_and_patch(home, ids):
    path = home / ".env"
    if path.is_symlink() or not path.is_file():
        raise RecoveryError("ENV_FILE_UNAVAILABLE: expected a regular .env file; nothing changed.")
    with path.open("r", encoding="utf-8", newline="") as stream:
        original = stream.read()
    updated = patch_env(original, {"HASS_TOKEN": "", "TELEGRAM_ALLOWED_USERS": ",".join(ids)})
    if updated == original:
        return None
    directory = home / "android-bridge-recovery" / str(time.time_ns())
    directory.mkdir(mode=0o700, parents=True)
    directory.parent.chmod(0o700)
    for name in [".env", "config.yaml"]:
        source = home / name
        if source.is_file() and not source.is_symlink():
            target = directory / name
            shutil.copy2(source, target)
            target.chmod(0o600)
    temp_name = None
    try:
        fd, temp_name = tempfile.mkstemp(dir=home, prefix=".env-recovery-")
        with os.fdopen(fd, "w", encoding="utf-8", newline="") as stream:
            stream.write(updated)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temp_name, path)
    finally:
        if temp_name and os.path.exists(temp_name):
            os.unlink(temp_name)
    return directory


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def telegram_probe(token, proxy=None, base_url="https://api.telegram.org"):
    """Read-only Bot API getMe. Never return/log the URL or raw exceptions."""
    handlers = [NoRedirect()]
    if proxy:
        if not proxy.startswith(("http://", "https://")):
            raise RecoveryError("PROXY_CHECK_NEEDED: Telegram has a non-HTTP proxy. No proxy or credentials were changed.")
        handlers.append(urllib.request.ProxyHandler({"https": proxy}))
    opener = urllib.request.build_opener(*handlers)
    try:
        with opener.open(base_url + "/bot" + token + "/getMe", timeout=20) as response:
            raw = response.read(65537)
            if len(raw) > 65536:
                raise RecoveryError("TELEGRAM_RESPONSE_INVALID: response too large.")
            data = json.loads(raw)
        if not isinstance(data, dict) or data.get("ok") is not True:
            raise RecoveryError("TELEGRAM_API_FAILED: bot identity could not be verified.")
        bot = data.get("result")
        if not isinstance(bot, dict) or bot.get("is_bot") is not True:
            raise RecoveryError("TELEGRAM_API_FAILED: unexpected identity response.")
        return True
    except urllib.error.HTTPError as error:
        if error.code in (401, 404):
            raise RecoveryError("BOT_TOKEN_REJECTED: Telegram rejected the saved token. No token replacement was attempted.") from None
        raise RecoveryError("TELEGRAM_HTTP_ERROR: status " + str(error.code) + ". No token or raw URL is printed.") from None
    except (ssl.SSLError, socket.timeout, urllib.error.URLError, OSError):
        raise RecoveryError("TELEGRAM_NETWORK_FAILED: HTTPS/DNS/TLS or proxy connection failed. This is not proof of lost pairing. No gateway was started.") from None
    except (ValueError, UnicodeError):
        raise RecoveryError("TELEGRAM_RESPONSE_INVALID: could not read Telegram's response.") from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", action="store_true", help="Start foreground gateway only if checks pass")
    args = parser.parse_args()
    try:
        # All imports are from the existing installation; never pip-install/upgrade.
        from dotenv import dotenv_values, load_dotenv
        from hermes_cli.config import get_hermes_home
        from gateway.pairing import PairingStore
        home = Path(get_hermes_home())
        path = home / ".env"
        env = dotenv_values(path)
        token = env.get("TELEGRAM_BOT_TOKEN")
        if not isinstance(token, str) or not token.strip():
            raise RecoveryError("NO_SAVED_BOT_TOKEN: .env has no usable Telegram bot token. Nothing changed.")
        if any(ord(char) > 127 or char.isspace() for char in token):
            raise RecoveryError("BOT_TOKEN_FORMAT_INVALID: saved token contains whitespace/non-ASCII. Nothing changed.")
        approved = PairingStore().list_approved("telegram")
        ids = allowed_ids(env.get("TELEGRAM_ALLOWED_USERS"), [row.get("user_id", "") for row in approved])
        print("CHECK: saved Telegram authorization found (" + str(len(ids)) + " user(s)).", flush=True)
        # Network/token failure must stop before any settings are edited.
        load_dotenv(path, override=True)  # Honor saved HTTPS/proxy settings for this probe process too.
        print("CHECK: testing Telegram HTTPS and bot identity (up to 20 seconds)…", flush=True)
        telegram_probe(token, env.get("TELEGRAM_PROXY") or os.getenv("TELEGRAM_PROXY"))
        print("PASS: Telegram bot token and HTTPS getMe request.", flush=True)
        backup = backup_and_patch(home, ids)
        print("BACKUP: " + str(backup) if backup else "CHECK: targeted settings already correct.", flush=True)
        load_dotenv(path, override=True)
        from gateway.config import load_gateway_config
        config = load_gateway_config()
        platforms = {getattr(key, "value", str(key)): value for key, value in config.platforms.items()}
        ha = platforms.get("homeassistant")
        tg = platforms.get("telegram")
        if ha and ha.enabled:
            raise RecoveryError("HA_STILL_ENABLED: another config/credential source enables Home Assistant. Backup exists; gateway not started.")
        if not tg or not tg.enabled or not tg.token:
            raise RecoveryError("TELEGRAM_CONFIG_NOT_READY: effective Telegram config is not enabled/configured. Backup exists; gateway not started.")
        print("PASS: Home Assistant disabled; Telegram enabled; existing paired users explicitly allowed.", flush=True)
        print("CHECK: provider settings, Telegram token, pairing files and memory were not changed.", flush=True)
        if args.run:
            print("STARTING: foreground gateway. Keep Termux open. Send one Telegram message to verify the AI reply.", flush=True)
            os.execvp("hermes", ["hermes", "gateway", "run"])
        return 0
    except RecoveryError as error:
        print("STOP: " + str(error), flush=True)
        return 1
    except ImportError:
        print("STOP: installed Hermes dependency/entrypoint unavailable. No automatic installation attempted.", flush=True)
        return 1
    except Exception:
        print("STOP: unexpected local check/write failure. No raw exception or credential is printed. Report the CHECK/PASS/STOP lines only.", flush=True)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
