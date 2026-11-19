"""Boot/focus/ANR gates use a synthetic adb fixture and virtual time."""
import subprocess
import unittest
from unittest.mock import patch
import check_android_emulator_ready as gate
from check_android_emulator_ready import ready


class EmulatorReadyTest(unittest.TestCase):
    def fixture(self, *, boot='1', component='synthetic.launcher/.Home', status='Status: ok', windows=None):
        self.now = 0
        self.calls = []
        frames = iter(windows or ['mCurrentFocus=Window{0 synthetic.launcher/.Home}'] * 3)
        def run(*command):
            self.calls.append(command)
            if command[0] == 'getprop':
                return boot
            if command[0] == 'cmd':
                return component
            if command[0] == 'am':
                return status
            if command == ('dumpsys', 'window', 'displays'):
                return next(frames, 'mCurrentFocus=null')
            raise AssertionError('Unsupported synthetic adb command')
        def pause(seconds):
            self.now += seconds
        return dict(run=run, clock=lambda: self.now, pause=pause)

    def test_normal_home_requires_three_focused_samples(self):
        self.assertTrue(ready(**self.fixture()))
        self.assertEqual(3, sum(c[0] == 'dumpsys' for c in self.calls))

    def test_boot_timeout_never_launches_home(self):
        self.assertFalse(ready(**self.fixture(boot='0')))
        self.assertFalse(any(c[0] == 'am' for c in self.calls))

    def test_anr_window_fails_without_dismissal(self):
        self.assertFalse(ready(**self.fixture(windows=['mCurrentFocus=Application Not Responding: synthetic.launcher'])))
        self.assertFalse(any(c[0] in ('input', 'kill', 'force-stop') for c in self.calls))

    def test_unfocused_home_never_counts_as_ready(self):
        self.assertFalse(ready(**self.fixture(windows=['mCurrentFocus=null'])))

    def test_another_package_cannot_satisfy_home_focus(self):
        self.assertFalse(ready(**self.fixture(windows=['mCurrentFocus=Window{0 other.app/.Home}'])))

    def test_lost_focus_resets_stability(self):
        frames=['mCurrentFocus=Window{0 synthetic.launcher/.Home}', 'mCurrentFocus=null'] + ['mCurrentFocus=Window{0 synthetic.launcher/.Home}'] * 3
        self.assertTrue(ready(**self.fixture(windows=frames)))
        self.assertEqual(5, sum(c[0] == 'dumpsys' for c in self.calls))

    def test_unresolved_or_malformed_home_is_rejected(self):
        for value in ['No activity found', 'synthetic.launcher/.Home; input keyevent 66']:
            self.assertFalse(ready(**self.fixture(component=value)))

    def test_failed_home_start_is_rejected(self):
        self.assertFalse(ready(**self.fixture(status='Error: launch failed')))

    def test_report_records_expected_package_and_stability(self):
        report = {}
        self.assertTrue(ready(**self.fixture(), report=report))
        self.assertEqual('synthetic.launcher', report['resolvedHomePackage'])
        self.assertEqual('ready', report['stage'])
        self.assertEqual(3, report['stableSamples'])

    def test_report_distinguishes_unmatched_focus_from_boot_failure(self):
        report = {}
        self.assertFalse(ready(**self.fixture(windows=['mCurrentFocus=Window{0 other.app/.Home}']), report=report))
        self.assertTrue(report['bootCompleted'])
        self.assertEqual('focus', report['stage'])
        self.assertEqual(0, report['matchedSamples'])

    def test_diagnostic_uses_display_focus_and_redacts_anr_reason(self):
        commands = []
        def run(*command):
            commands.append(command)
            if command == ('dumpsys', 'window', 'displays'):
                return '  mCurrentFocus=Window{0 synthetic.launcher/.Home}'
            return 'Reason: Input dispatching timed out (sensitive synthetic detail)'
        with patch.object(gate, 'Path') as path:
            gate.diagnostic(run)
        output = path.return_value.write_text.call_args.args[0]
        self.assertIn('mCurrentFocus=', output)
        self.assertIn('ANR_REASON=input_dispatch_timeout', output)
        self.assertNotIn('sensitive synthetic detail', output)

    def test_missing_diagnostic_fields_are_explicit(self):
        with patch.object(gate, 'Path') as path:
            gate.diagnostic(lambda *command: '')
        self.assertIn('NO_SUPPORTED_SYSTEM_FIELDS', path.return_value.write_text.call_args.args[0])


if __name__ == '__main__':
    unittest.main(verbosity=2)
