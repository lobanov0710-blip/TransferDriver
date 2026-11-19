"""Require a responsive normal HOME before synthetic native tests; never dismiss ANRs."""
from pathlib import Path
import re
import subprocess
import sys
import time


def adb(*args):
    return subprocess.run(['adb', 'shell', *args], check=True, capture_output=True,
                          text=True, timeout=15).stdout


def diagnostic(run=adb, report=None):
    # Only fixed system-state fields, never raw app/provider/SQL output.
    lines = ['Synthetic emulator readiness diagnostic']
    if report:
        for key in ['stage', 'bootCompleted', 'resolvedHomePackage', 'homeStartOk', 'samples', 'matchedSamples', 'stableSamples', 'bootstrapHomeObserved', 'homeTargetChanges']:
            if key in report:
                lines.append(key + '=' + str(report[key]))
    for command in [('dumpsys', 'window', 'displays'), ('dumpsys', 'activity', 'lastanr')]:
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
    if len(lines) == 1:
        lines.append('NO_SUPPORTED_SYSTEM_FIELDS')
    path = Path('registration-evidence/native-diagnostics/emulator-readiness.txt')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text('\n'.join(lines) + '\n')


def ready(run=adb, clock=time.monotonic, pause=time.sleep, report=None):
    report = {} if report is None else report
    report.update(stage='boot', bootCompleted=False)
    deadline = clock() + 60
    while run('getprop', 'sys.boot_completed').strip() != '1':
        if clock() >= deadline:
            return False
        pause(1)
    report.update(stage='start_home', bootCompleted=True)
    # A normal launch warms the system launcher before app/configuration switches.
    # It neither kills processes nor accepts/closes an ANR or security dialog.
    result = run('am', 'start', '-W', '-a', 'android.intent.action.MAIN', '-c',
                 'android.intent.category.HOME')
    report['homeStartOk'] = bool(re.search(r'^Status: ok\s*$', result, re.M))
    if not report['homeStartOk']:
        return False
    stable = 0
    previous_package = None
    report.update(stage='focus', samples=0, matchedSamples=0, stableSamples=0,
                  bootstrapHomeObserved=False, homeTargetChanges=0)
    while clock() < deadline:
        # During cold boot Settings.FallbackHome yields to the real HOME.
        # Resolve on each observation; never pin that temporary boot activity.
        report['stage'] = 'resolve_home'
        component = run('cmd', 'package', 'resolve-activity', '--brief', '-a',
                        'android.intent.action.MAIN', '-c', 'android.intent.category.HOME').strip().splitlines()[-1]
        if not re.fullmatch(r'[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+', component):
            return False
        package = component.split('/')[0]
        bootstrap = package == 'com.android.settings' and component.split('/')[1].endswith('.FallbackHome')
        report['bootstrapHomeObserved'] |= bootstrap
        report['resolvedHomePackage'] = package
        if previous_package != package:
            stable = 0
            report['homeTargetChanges'] += int(previous_package is not None)
            previous_package = package
        report['stage'] = 'focus'
        windows = run('dumpsys', 'window', 'displays')
        focus = next((line for line in windows.splitlines() if 'mCurrentFocus=' in line), '')
        if 'Application Not Responding:' in windows:
            report['stage'] = 'anr'
            return False
        matched = not bootstrap and bool(re.search(r'\b' + re.escape(package) + r'/', focus))
        stable = stable + 1 if matched else 0
        report['samples'] += 1
        report['matchedSamples'] += int(matched)
        report['stableSamples'] = stable
        if stable >= 3:
            report['stage'] = 'ready'
            return True
        pause(1)
    return False


if __name__ == '__main__':
    report = {}
    try:
        success = ready(report=report)
    except (OSError, subprocess.SubprocessError, IndexError):
        success = False
    if not success:
        diagnostic(report=report)
        print('ANDROID_EMULATOR_READY=FAIL; safe system diagnostic recorded')
        sys.exit(1)
    print('ANDROID_EMULATOR_READY=PASS')
    print('ANDROID_EMULATOR_HOME_TRANSITIONS=' + str(report['homeTargetChanges']))
    print('ANDROID_EMULATOR_BOOTSTRAP_HOME_OBSERVED=' + str(report['bootstrapHomeObserved']).lower())
