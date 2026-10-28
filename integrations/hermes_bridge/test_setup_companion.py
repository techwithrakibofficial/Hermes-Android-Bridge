from pathlib import Path
import tempfile
import unittest
import setup_companion as setup

class NativeSetupTests(unittest.TestCase):
    def test_repository_backend_matches_pin(self):
        data=Path(__file__).with_name("companion_server.py").read_bytes()
        self.assertEqual(setup.verify(data),data)
        self.assertNotIn("PIN_",setup.SOURCE_COMMIT)
    def test_bad_download_is_rejected(self):
        with self.assertRaises(setup.SetupStop):setup.verify(b"tampered")
    def test_private_file_and_backup_modes(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/"private/launcher"
            setup.install(path,b"original",executable=True)
            self.assertEqual(path.stat().st_mode & 0o777,0o700)
            setup.install(path,b"updated")
            self.assertEqual(path.read_bytes(),b"updated")
            self.assertEqual(path.stat().st_mode & 0o777,0o600)
            backups=list(path.parent.glob("*.before-native-*"))
            self.assertEqual(backups[0].read_bytes(),b"original")
            self.assertEqual(backups[0].stat().st_mode & 0o777,0o600)
    def test_symlinks_not_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            target=Path(directory)/"target";target.write_text("preserve")
            link=Path(directory)/"link";link.symlink_to(target)
            with self.assertRaises(setup.SetupStop):setup.install(link,b"new")
            self.assertEqual(target.read_text(),"preserve")

if __name__=="__main__":unittest.main()
