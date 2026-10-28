import importlib.util
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch

SOURCE = Path(__file__).with_name("bridge_mcp.py")
spec = importlib.util.spec_from_file_location("bridge_mcp", SOURCE)
bridge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bridge)
TOKEN = "A" * 43


class FakeAndroid(BaseHTTPRequestHandler):
    calls = []
    code = 200
    bodies = []
    rejection = None

    def log_message(self, *args):
        pass

    def do_GET(self):
        if self.path == "/v1/screen":
            self.answer({"snapshot": "fresh", "nodes": [{"id": "n0", "text": "বাংলা"}]}); return
        if self.path == "/v1/apps":
            self.answer({"apps": [{"package": "com.example.app", "label": "Example"}]}); return
        if self.path == "/v1/device":
            self.answer({"screen_control_enabled": True}); return
        self.answer({"bridge": "ready", "allowed_actions": ["home", "back", "settings", "open_app"], "supported_apps": list(bridge.SUPPORTED_APPS)})

    def do_POST(self):
        self.calls.append((self.path, self.headers.get("X-Request-Id"), self.headers.get("Authorization")))
        length = int(self.headers.get("Content-Length", "0"))
        self.bodies.append((self.path, self.headers.get("Content-Type"), self.rfile.read(length).decode("utf-8")))
        self.answer(self.rejection or {"accepted": True})

    def answer(self, data):
        if self.headers.get("Authorization") != "Bearer " + TOKEN:
            self.send_response(401)
            self.end_headers()
            return
        self.send_response(self.code)
        body = json.dumps(data).encode()
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


class AdapterTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), FakeAndroid)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "pair/connection.json"
        self.client = bridge.BridgeClient(self.path)
        self.url_patch = patch.object(bridge, "BASE_URL", "http://127.0.0.1:" + str(self.server.server_port))
        self.url_patch.start()
        self.addCleanup(self.url_patch.stop)
        FakeAndroid.calls = []
        FakeAndroid.bodies = []
        FakeAndroid.rejection = None
        FakeAndroid.code = 200

    def test_pair_verifies_and_uses_private_modes(self):
        self.client.pair(TOKEN)
        self.assertEqual(self.client.read_token(), TOKEN)
        self.assertEqual(stat.S_IMODE(self.path.stat().st_mode), 0o600)
        self.assertEqual(stat.S_IMODE(self.path.parent.stat().st_mode), 0o700)

    def test_invalid_token_not_written(self):
        with self.assertRaises(bridge.BridgeError):
            self.client.pair("bad")
        self.assertFalse(self.path.exists())

    def test_wrong_session_token_not_written(self):
        with self.assertRaises(bridge.BridgeError):
            self.client.pair("B" * 43)
        self.assertFalse(self.path.exists())

    def test_missing_pairing_is_clear(self):
        with self.assertRaisesRegex(bridge.BridgeError, "Not paired"):
            self.client.read_token()

    def test_world_readable_pairing_rejected(self):
        self.client.pair(TOKEN)
        self.path.chmod(0o644)
        with self.assertRaisesRegex(bridge.BridgeError, "private"):
            self.client.read_token()

    def test_symlink_pairing_rejected(self):
        self.client.pair(TOKEN)
        link = self.path.parent / "symlink.json"
        link.symlink_to(self.path)
        with self.assertRaises(bridge.BridgeError):
            bridge.BridgeClient(link).read_token()

    def test_status_makes_no_action(self):
        self.client.pair(TOKEN)
        self.assertEqual(self.client.request("android_bridge_status")["bridge"], "ready")
        self.assertEqual(FakeAndroid.calls, [])

    def test_actions_have_unique_ids_and_do_not_claim_completion(self):
        self.client.pair(TOKEN)
        for name in ["android_home", "android_back"]:
            self.assertEqual(self.client.request(name)["verification"], "check_phone_screen")
        self.assertEqual([c[0] for c in FakeAndroid.calls], ["/v1/actions/home", "/v1/actions/back"])
        self.assertNotEqual(FakeAndroid.calls[0][1], FakeAndroid.calls[1][1])

    def test_no_automatic_retries_on_rejection(self):
        self.client.pair(TOKEN)
        FakeAndroid.code = 503
        with self.assertRaisesRegex(bridge.BridgeError, "do not automatically retry"):
            self.client.request("android_home")
        self.assertEqual(len(FakeAndroid.calls), 1)

    def test_token_read_fresh_for_each_call(self):
        self.client.pair(TOKEN)
        self.path.write_text(json.dumps({"token": "B" * 43}))
        with self.assertRaisesRegex(bridge.BridgeError, "expired"):
            self.client.request("android_bridge_status")

    def test_redirects_disabled(self):
        self.assertIsNone(bridge.NoRedirect().redirect_request(None, None, 302, "", {}, "https://example.com"))

    def test_unknown_action_cannot_make_request(self):
        with self.assertRaises(bridge.BridgeError):
            self.client.request("shell")
        self.assertEqual(FakeAndroid.calls, [])

    def initialized_adapter(self):
        adapter = bridge.MCPAdapter(self.client)
        response = adapter.handle({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2025-06-18"}})
        self.assertEqual(response["result"]["protocolVersion"], "2025-06-18")
        return adapter

    def test_tool_discovery_does_not_need_pairing(self):
        adapter = self.initialized_adapter()
        response = adapter.handle({"jsonrpc": "2.0", "id": 2, "method": "tools/list"})
        self.assertEqual({t["name"] for t in response["result"]["tools"]}, set(bridge.TOOL_PATHS))

    def test_initialization_required(self):
        result = bridge.MCPAdapter().handle({"jsonrpc": "2.0", "id": 1, "method": "tools/list"})
        self.assertIn("error", result)

    def test_tool_arguments_cannot_add_url_or_action(self):
        adapter = self.initialized_adapter()
        result = adapter.handle({"jsonrpc": "2.0", "id": 2, "method": "tools/call", "params": {"name": "android_home", "arguments": {"url": "https://example.com"}}})
        self.assertIn("error", result)
        self.assertEqual(FakeAndroid.calls, [])

    def test_tool_failure_is_error_and_does_not_leak_token(self):
        adapter = self.initialized_adapter()
        result = adapter.handle({"jsonrpc": "2.0", "id": 2, "method": "tools/call", "params": {"name": "android_home", "arguments": {}}})
        self.assertTrue(result["result"]["isError"])
        self.assertNotIn(TOKEN, json.dumps(result))

    def test_stdio_initialize_list_notifications_ping(self):
        messages = [
            {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2025-06-18"}},
            {"jsonrpc": "2.0", "method": "notifications/initialized"},
            {"jsonrpc": "2.0", "id": 2, "method": "tools/list"},
            {"jsonrpc": "2.0", "id": 3, "method": "ping"},
        ]
        result = subprocess.run([sys.executable, str(SOURCE)], input="".join(json.dumps(m) + "\n" for m in messages), text=True, capture_output=True, timeout=5)
        self.assertEqual(result.returncode, 0)
        responses = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertEqual([r["id"] for r in responses], [1, 2, 3])
        self.assertEqual(len(responses[1]["result"]["tools"]), 19)

    def test_settings_uses_only_allowed_path(self):
        self.client.pair(TOKEN)
        self.client.request("android_open_settings")
        self.assertEqual(FakeAndroid.calls[0][0], "/v1/actions/settings")

    def test_supported_apps_dispatch_exact_alias_paths(self):
        self.client.pair(TOKEN)
        for app in bridge.SUPPORTED_APPS:
            self.client.request("android_open_app", arguments={"app": app})
        self.assertEqual([row[0] for row in FakeAndroid.calls], ["/v1/actions/open-app/" + app for app in bridge.SUPPORTED_APPS])

    def test_app_cannot_accept_arbitrary_package_url_or_path(self):
        self.client.pair(TOKEN)
        for arguments in [{}, {"app":"com.evil.package"}, {"app":"../settings"}, {"app":"https://example.com"}, {"app":"whatsapp", "url":"evil"}]:
            with self.assertRaises(bridge.BridgeError):
                self.client.request("android_open_app", arguments=arguments)
        self.assertEqual(FakeAndroid.calls, [])

    def test_app_tool_schema_requires_bounded_enum(self):
        schema = bridge.input_schema("android_open_app")
        self.assertEqual(schema["required"], ["app"])
        self.assertFalse(schema["additionalProperties"])
        self.assertEqual(set(schema["properties"]["app"]["enum"]), {"whatsapp","facebook","messenger","telegram","chrome","termux"})

    def test_visible_screen_is_returned_without_fabricated_completion(self):
        self.client.pair(TOKEN)
        result = self.client.request("android_screen")
        self.assertEqual(result["nodes"][0]["text"], "বাংলা")
        self.assertEqual(FakeAndroid.calls, [])

    def test_bengali_text_body_preserves_target_and_unicode(self):
        self.client.pair(TOKEN)
        args = {"snapshot": "fresh-123", "node": "n3", "text": "কেমন আছ?"}
        self.client.request("android_set_text", arguments=args)
        path, kind, raw = FakeAndroid.bodies[0]
        self.assertEqual(path, "/v1/commands/set-text")
        self.assertEqual(kind, "application/json")
        self.assertEqual(json.loads(raw), args)

    def test_stale_rejection_is_reported_once(self):
        self.client.pair(TOKEN)
        FakeAndroid.rejection = {"accepted": False, "error": "stale_snapshot_observe_again"}
        with self.assertRaisesRegex(bridge.BridgeError, "stale_snapshot"):
            self.client.request("android_click", arguments={"snapshot": "old", "node": "n0"})
        self.assertEqual(len(FakeAndroid.calls), 1)

    def test_dialer_disallows_service_codes_urls_and_extra_fields(self):
        for number in ["*123#", "tel:1234", "+880 123", "123;456", "12", "9"*21]:
            self.assertFalse(bridge.valid_arguments("android_dial", {"number": number}))
        self.assertTrue(bridge.valid_arguments("android_dial", {"number": "+8801712345678"}))

    def test_screen_actions_require_exact_fresh_target_shape(self):
        for args in [{}, {"snapshot":"x", "node":"n0", "x":20}, {"snapshot":"x", "node":0}, {"snapshot":"x", "node":"../"}]:
            self.assertFalse(bridge.valid_arguments("android_click", args))
        self.assertFalse(bridge.valid_arguments("android_scroll", {"snapshot":"x", "node":"n0", "direction":"diagonal"}))
        self.assertFalse(bridge.valid_arguments("android_set_text", {"snapshot":"x", "node":"n0", "text":"a"*2001}))

    def test_launch_package_is_data_not_a_url(self):
        self.assertTrue(bridge.valid_arguments("android_open_package", {"package":"com.example.app"}))
        for package in ["https://bad", "com.good/evil", "com.good;echo"]:
            self.assertFalse(bridge.valid_arguments("android_open_package", {"package":package}))


if __name__ == "__main__":
    unittest.main()
