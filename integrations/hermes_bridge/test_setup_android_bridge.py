import importlib.util
from pathlib import Path
import stat
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("setup_android_bridge", Path(__file__).with_name("setup_android_bridge.py"))
setup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(setup)


class SetupTests(unittest.TestCase):
    def test_pinned_source_matches_repository_file(self):
        data = Path(__file__).with_name("bridge_mcp.py").read_bytes()
        self.assertEqual(setup.verified_source(data), data)

    def test_tampered_download_rejected(self):
        with self.assertRaises(setup.SetupError):
            setup.verified_source(b"tampered")

    def test_private_atomic_install_and_replacement_backup(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/"private/bridge_mcp.py"
            setup.install_file(path, b"old")
            setup.install_file(path, b"new")
            self.assertEqual(path.read_bytes(), b"new")
            backups = list(path.parent.glob("*.before-setup-*"))
            self.assertEqual(len(backups), 1)
            self.assertEqual(backups[0].read_bytes(), b"old")
            self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
            self.assertEqual(stat.S_IMODE(path.parent.stat().st_mode), 0o700)

    def test_symlink_target_not_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)/"existing"
            target.write_bytes(b"keep")
            path = Path(directory)/"link"
            path.symlink_to(target)
            with self.assertRaises(setup.SetupError):
                setup.install_file(path, b"new")
            self.assertEqual(target.read_bytes(), b"keep")

    def test_foreign_or_disabled_entry_not_claimed(self):
        path = Path("/private/bridge_mcp.py")
        self.assertFalse(setup.config_is_ours({"command":"other", "args":[str(path)]}, path))
        self.assertFalse(setup.config_is_ours({"command":"python3", "args":[str(path)], "enabled":False}, path))
        self.assertTrue(setup.config_is_ours({"command":sys.executable, "args":[str(path)], "enabled":True}, path))

    def test_disabled_owned_entry_can_be_considered_for_probe_only(self):
        path = Path("/private/bridge_mcp.py")
        entry = {"command":"python3", "args":[str(path)], "enabled":False}
        self.assertTrue(setup.config_is_ours(entry, path, allow_disabled=True))
        self.assertFalse(setup.config_is_ours({"command":"other", "args":[str(path)], "enabled":False}, path, allow_disabled=True))

    def test_existing_filters_must_not_be_overridden(self):
        self.assertTrue(setup.tools_allowed({}))
        self.assertFalse(setup.tools_allowed({"tools":{"include":["android_home"]}}))
        self.assertFalse(setup.tools_allowed({"tools":{"exclude":["android_back"]}}))

    def test_missing_sdk_does_not_install_without_explicit_flag(self):
        with patch.object(setup.importlib.util, "find_spec", return_value=None), patch.object(setup.subprocess, "run") as run:
            with self.assertRaisesRegex(setup.SetupError, "SDK missing"):
                setup.ensure_sdk(False)
            run.assert_not_called()

    def test_failed_sdk_install_stops_and_uses_existing_python_exact_pin(self):
        with patch.object(setup.sys, "prefix", "/private/mcp-env"), patch.object(setup.sys, "base_prefix", "/system"), patch.object(setup.importlib.util, "find_spec", return_value=None), patch.object(setup.subprocess, "run", side_effect=RuntimeError("install failed")) as run:
            with self.assertRaises(RuntimeError):
                setup.ensure_sdk(True)
            run.assert_called_once_with([sys.executable, "-m", "pip", "install", "mcp==1.26.0"], check=True)

    def test_system_python_never_receives_pip_install(self):
        with patch.object(setup.sys, "prefix", "/system"), patch.object(setup.sys, "base_prefix", "/system"), patch.object(setup.importlib.util, "find_spec", return_value=None), patch.object(setup.subprocess, "run") as run:
            with self.assertRaisesRegex(setup.SetupError, "system Python"):
                setup.ensure_sdk(True)
            run.assert_not_called()

    def test_hermes_cli_uses_private_interpreter_not_old_shebang(self):
        with patch.object(setup.shutil, "which", return_value="/usr/local/bin/hermes"), patch.object(setup.sys, "executable", "/private/mcp-env/bin/python"):
            self.assertEqual(setup.hermes_command("gateway", "run"), ["/private/mcp-env/bin/python", "/usr/local/bin/hermes", "gateway", "run"])

    def test_base_python_owned_entry_migrates_only_for_expected_script(self):
        path = Path("/private/bridge_mcp.py")
        with patch.object(setup.sys, "_base_executable", "/usr/bin/python3.14"):
            entry = {"command":"/usr/bin/python3.14", "args":[str(path)], "enabled":False}
            self.assertTrue(setup.config_is_ours(entry, path, allow_disabled=True, allow_base_python=True))
            entry["args"] = ["/unrelated/server.py"]
            self.assertFalse(setup.config_is_ours(entry, path, allow_disabled=True, allow_base_python=True))

    def test_private_environment_requests_system_packages_without_pip_bootstrap(self):
        with tempfile.TemporaryDirectory() as directory:
            env = Path(directory)/"mcp-env"
            with patch.object(setup.venv, "EnvBuilder") as builder, patch.object(setup.subprocess, "run") as run:
                run.return_value.stdout = "1\n1\n"
                setup.create_private_environment(env)
                builder.assert_called_once_with(system_site_packages=True, with_pip=False, symlinks=True)
                builder.return_value.create.assert_called_once_with(env)

    def test_failed_private_environment_probe_prevents_install(self):
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(setup.venv, "EnvBuilder"), patch.object(setup.subprocess, "run") as run:
                run.return_value.stdout = "1\n0\n"
                with self.assertRaisesRegex(setup.SetupError, "unavailable"):
                    setup.create_private_environment(Path(directory)/"env")

    def test_real_venv_uses_its_own_prefix_and_sees_system_pip(self):
        with tempfile.TemporaryDirectory() as directory:
            env = Path(directory)/"mcp-env"
            # This exercises actual venv creation, not package installation.
            try:
                python = setup.create_private_environment(env)
            except setup.SetupError as error:
                if "unavailable" in str(error):
                    self.skipTest("Host base Python has no importable pip; phone's global pip is already confirmed")
                raise
            self.assertTrue(python.exists())
            self.assertIn("include-system-site-packages = true", (env/"pyvenv.cfg").read_text())

    def test_missing_pair_token_stops_before_runtime_import_or_write(self):
        with patch.dict("os.environ", {}, clear=True):
            self.assertEqual(setup.main(), 1)

    def test_installed_adapter_import_is_idle(self):
        module = setup.load_adapter(Path(__file__).with_name("bridge_mcp.py"))
        self.assertEqual(set(module.TOOL_PATHS), setup.EXPECTED_TOOLS)


if __name__ == "__main__":
    unittest.main()
