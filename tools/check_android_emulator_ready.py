"""Require a responsive normal HOME before synthetic native tests; never dismiss ANRs."""
from pathlib import Path
import re
import subprocess
import sys
import time


def adb(*args):
    return subprocess.run(['adb', 'shell', *args], check=True, capture_output=True,
                          text=True, timeout=15).stdout


def diagnostic(run=adb):
    # Only fixed system-state fields, never raw app/provider/SQL output.
    lines = []
    for command in [('dumpsys', 'window', 'windows'), ('dumpsys', 'activity', 'lastanr')]:
        try:
            output = run(*command)
            for line in output.splitlines():
                if re.match(r'\s*(mCurrentFocus=|mFocusedApp=|Process:)', line):
                    lines.append(line.strip())
                elif line.strip().startswith('Reason:'):
                    cause = line.lower()
                    lines.append('ANR_REASON=' + ('input_dispatch_timeout' if 'input dispatching timed out' in cause
                                 else 'broadcast_timeout' if 'broadcast' in cause else 'other'))
        except (OSError, subprocess.SubprocessError):
            lines.append('SYSTEM_DIAGNOSTIC_UNAVAILABLE')
    path = Path('registration-evidence/native-diagnostics/emulator-readiness.txt')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text('\n'.join(lines) + '\n')


def ready(run=adb, clock=time.monotonic, pause=time.sleep):
    deadline = clock() + 60
    while run('getprop', 'sys.boot_completed').strip() != '1':
        if clock() >= deadline:
            return False
        pause(1)
    component = run('cmd', 'package', 'resolve-activity', '--brief', '-a',
                    'android.intent.action.MAIN', '-c', 'android.intent.category.HOME').strip().splitlines()[-1]
    if not re.fullmatch(r'[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+', component):
        return False
    package = component.split('/')[0]
    # A normal launch warms the system launcher before app/configuration switches.
    # It neither kills processes nor accepts/closes an ANR or security dialog.
    result = run('am', 'start', '-W', '-a', 'android.intent.action.MAIN', '-c',
                 'android.intent.category.HOME')
    if not re.search(r'^Status: ok\s*$', result, re.M):
        return False
    stable = 0
    while clock() < deadline:
        windows = run('dumpsys', 'window', 'windows')
        focus = next((line for line in windows.splitlines() if 'mCurrentFocus=' in line), '')
        if 'Application Not Responding:' in windows:
            return False
        stable = stable + 1 if re.search(r'\b' + re.escape(package) + r'/', focus) else 0
        if stable >= 3:
            return True
        pause(1)
    return False


if __name__ == '__main__':
    try:
        success = ready()
    except (OSError, subprocess.SubprocessError, IndexError):
        success = False
    if not success:
        diagnostic()
        print('ANDROID_EMULATOR_READY=FAIL; safe system diagnostic recorded')
        sys.exit(1)
    print('ANDROID_EMULATOR_READY=PASS')
