#!/usr/bin/env python3
"""Control-flow checks: a failed native suite never masks failure or blocks independent diagnostics."""
from pathlib import Path
import os
import subprocess
import tempfile
import unittest

class NativeRunnerTest(unittest.TestCase):
    def check_run(self, fail):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);(root/'bin').mkdir();(root/'tools').mkdir()
            (root/'registration-evidence').mkdir()
            for name in ('.validated','.driver-validated'):(root/'registration-evidence'/name).touch()
            (root/'tools/run_android_native_tests.sh').write_text(Path('tools/run_android_native_tests.sh').read_text())
            (root/'gradlew').write_text('''#!/bin/bash
printf '%s\\n' "$*" >> calls
case "$1:$FAIL" in
 :app:connectedDebugAndroidTest:driver) exit 9;;
 :admin:connectedDebugAndroidTest:admin) exit 8;;
esac
''')
            (root/'bin/adb').write_text('#!/bin/bash\nif [[ "$FAIL" == adb && "$*" == *files/registration-evidence/* ]]; then exit 7; fi\n')
            (root/'bin/python3').write_text('#!/bin/bash\nif [[ "$FAIL" == readiness && "$1" == tools/check_android_emulator_ready.py ]]; then exit 5; fi\nif [[ "$FAIL" == verifier && "$1" == *check_registration_evidence.py ]]; then exit 6; fi\n')
            for p in (root/'gradlew',root/'bin/adb',root/'bin/python3'):p.chmod(0o700)
            env=dict(os.environ,PATH=str(root/'bin')+os.pathsep+os.environ['PATH'],FAIL=fail)
            result=subprocess.run(['bash','tools/run_android_native_tests.sh'],cwd=root,env=env,capture_output=True,text=True)
            if fail == 'readiness':
                self.assertEqual(5,result.returncode)
                self.assertFalse((root/'calls').exists())
                for name in ('.validated','.driver-validated'):
                    self.assertFalse((root/'registration-evidence'/name).exists())
                return
            self.assertIn(':admin:connectedDebugAndroidTest',(root/'calls').read_text())
            self.assertEqual(0 if not fail else 1,result.returncode,result.stderr)
            self.assertEqual(not fail,(root/'registration-evidence/.validated').exists())
            self.assertEqual(fail not in ('driver','adb','verifier'),(root/'registration-evidence/.driver-validated').exists())
    def test_both_suites_and_evidence_are_required(self):self.check_run('')
    def test_unready_emulator_prevents_suites_and_stale_validation_markers(self):self.check_run('readiness')
    def test_driver_failure_does_not_mask_status_or_block_admin(self):self.check_run('driver')
    def test_admin_failure_keeps_overall_failure(self):self.check_run('admin')
    def test_adb_failure_cannot_create_validation_marker(self):self.check_run('adb')
    def test_png_verifier_failure_cannot_create_validation_marker(self):self.check_run('verifier')
if __name__=='__main__':unittest.main(verbosity=2)
