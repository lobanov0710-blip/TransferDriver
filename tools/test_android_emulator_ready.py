"""Boot/focus/ANR gates use a synthetic adb fixture and virtual time."""
import subprocess
import unittest
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
            return next(frames, 'mCurrentFocus=null')
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


if __name__ == '__main__':
    unittest.main(verbosity=2)
