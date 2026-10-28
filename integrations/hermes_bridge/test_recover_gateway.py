import importlib.util
import json
from pathlib import Path
import stat
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

spec = importlib.util.spec_from_file_location("recover_gateway", Path(__file__).with_name("recover_gateway.py"))
recover = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recover)


class RecoveryTests(unittest.TestCase):
    def test_only_existing_approved_and_allowlisted_users(self):
        self.assertEqual(recover.allowed_ids("20,10", ["30", "10", "bad", "-50"]), ["10", "20", "30"])

    def test_never_grants_wildcard(self):
        with self.assertRaises(recover.RecoveryError):
            recover.allowed_ids("*", ["10"])

    def test_missing_owner_does_not_bypass_pairing(self):
        with self.assertRaises(recover.RecoveryError):
            recover.allowed_ids("", [])

    def test_other_credentials_and_comments_unchanged(self):
        old = '# existing\nTELEGRAM_BOT_TOKEN="keep:exact"\nAPI_KEY=keepthis\nHASS_TOKEN=old\n'
        new = recover.patch_env(old, {"HASS_TOKEN": "", "TELEGRAM_ALLOWED_USERS": "10"})
        self.assertIn('TELEGRAM_BOT_TOKEN="keep:exact"\nAPI_KEY=keepthis\n', new)
        self.assertTrue(new.startswith('# existing\n'))
        self.assertIn("HASS_TOKEN=''\n", new)

    def test_duplicate_homeassistant_keys_all_disabled(self):
        new = recover.patch_env("HASS_TOKEN=one\nexport HASS_TOKEN=two\n", {"HASS_TOKEN": ""})
        self.assertEqual(new, "HASS_TOKEN=''\nHASS_TOKEN=''\n")

    def test_existing_allowlist_merged_not_replaced(self):
        self.assertEqual(recover.allowed_ids("11,22", ["33"]), ["11", "22", "33"])

    def test_backup_private_and_content_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            old = b"TELEGRAM_BOT_TOKEN=keep\nHASS_TOKEN=old\n"
            (home/".env").write_bytes(old)
            (home/"config.yaml").write_text("model: keep\n")
            backup = recover.backup_and_patch(home, ["123"])
            self.assertEqual((backup/".env").read_bytes(), old)
            self.assertEqual((backup/"config.yaml").read_text(), "model: keep\n")
            self.assertEqual(stat.S_IMODE((backup/".env").stat().st_mode), 0o600)
            self.assertEqual(stat.S_IMODE((home/".env").stat().st_mode), 0o600)
            self.assertEqual((home/"config.yaml").read_text(), "model: keep\n")

    def test_no_backup_when_already_correct(self):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            (home/".env").write_text("HASS_TOKEN=''\nTELEGRAM_ALLOWED_USERS='123'\n")
            self.assertIsNone(recover.backup_and_patch(home, ["123"]))

    def test_symlink_env_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            (home/"target").write_text("HASS_TOKEN=keep")
            (home/".env").symlink_to(home/"target")
            with self.assertRaises(recover.RecoveryError):
                recover.backup_and_patch(home, ["123"])
            self.assertEqual((home/"target").read_text(), "HASS_TOKEN=keep")

    def test_preserves_crlf_on_unrelated_lines(self):
        new = recover.patch_env("KEY=keep\r\nHASS_TOKEN=old\r\n", {"HASS_TOKEN": ""})
        self.assertEqual(new, "KEY=keep\r\nHASS_TOKEN=''\r\n")

    def test_missing_final_newline_stays_separate(self):
        self.assertEqual(recover.patch_env("KEY=keep", {"HASS_TOKEN": ""}), "KEY=keep\nHASS_TOKEN=''\n")

    def test_probe_success_and_auth_failure_sanitized(self):
        class API(BaseHTTPRequestHandler):
            status = 200
            def log_message(self, *args): pass
            def do_GET(self):
                self.send_response(self.status)
                self.end_headers()
                self.wfile.write(json.dumps({"ok":True,"result":{"is_bot":True}}).encode())
        server = ThreadingHTTPServer(("127.0.0.1", 0), API)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            url = "http://127.0.0.1:" + str(server.server_port)
            self.assertTrue(recover.telegram_probe("fake-token", base_url=url))
            API.status = 401
            with self.assertRaises(recover.RecoveryError) as caught:
                recover.telegram_probe("fake-secret-token", base_url=url)
            self.assertIn("BOT_TOKEN_REJECTED", str(caught.exception))
            self.assertNotIn("fake-secret-token", str(caught.exception))
        finally:
            server.shutdown()
            server.server_close()

    def test_socks_proxy_not_silently_ignored(self):
        with self.assertRaisesRegex(recover.RecoveryError, "PROXY_CHECK_NEEDED"):
            recover.telegram_probe("secret", "socks5://localhost:9999")


if __name__ == "__main__":
    unittest.main()
