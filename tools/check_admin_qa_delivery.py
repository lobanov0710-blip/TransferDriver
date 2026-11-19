"""Verify real QA APKs and update behaviour on a disposable CI emulator only."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

PACKAGE = 'ru.transferservis.admin.qa'
ADMIN = 'ru.transferservis.admin'


def run(*args, success=True, **kwargs):
    result = subprocess.run([str(a) for a in args], capture_output=True, text=True, **kwargs)
    if success and result.returncode:
        raise RuntimeError('Command failed: ' + Path(str(args[0])).name)
    return result


def manifest(aapt, apk, version):
    value = run(aapt, 'dump', 'xmltree', apk, 'AndroidManifest.xml').stdout
    assert 'A: package="' + PACKAGE + '"' in value, 'Wrong QA package'
    assert re.search(r'android:versionCode\([^\n]+\)=\(type 0x10\)0x' + format(version, 'x') + r'\b', value), 'Wrong QA version'
    assert re.search(r'android:minSdkVersion\([^\n]+\)=\(type 0x10\)0x1a\b', value), 'Expected Android 8+'
    for name in ('debuggable', 'testOnly', 'allowBackup', 'usesCleartextTraffic'):
        assert not re.search(r'android:' + name + r'\([^\n]+\)=\(type 0x12\)0xffffffff', value), name
    assert 'sharedUserId' not in value, 'Shared UID is forbidden'
    return value


def snapshot(adb, package):
    text = run(adb, 'shell', 'dumpsys', 'package', package).stdout
    def field(pattern):
        match = re.search(pattern, text)
        assert match, 'Missing package metadata'
        return match.group(1)
    return dict(uid=field(r'(?:userId|appId)=(\d+)'),
                dataDir=field(r'dataDir=(\S+)'), version=int(field(r'versionCode=(\d+)')),
                firstInstall=field(r'firstInstallTime=([^\n]+)'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', type=Path, required=True)
    parser.add_argument('--upgrade', type=Path)
    parser.add_argument('--receipt', type=Path, required=True)
    args = parser.parse_args()
    sdk = Path(os.environ['ANDROID_HOME'])
    aapt = sdk / 'build-tools/37.0.0/aapt'
    signer = sdk / 'build-tools/37.0.0/apksigner'
    adb = sdk / 'platform-tools/adb'
    manifest(aapt, args.base, 2)
    unsigned = run(signer, 'verify', args.base, success=False)
    assert unsigned.returncode != 0 and 'DOES NOT VERIFY' in unsigned.stdout + unsigned.stderr, 'Expected unsigned QA APK'
    receipt = dict(package=PACKAGE, versionCode=2, unsignedSha256=hashlib.sha256(args.base.read_bytes()).hexdigest(),
                   unsignedBytes=args.base.stat().st_size, debuggable=False, endpoint='STAGING',
                   sourceCommit=os.environ.get('GITHUB_SHA'), checks=['binary_manifest', 'unsigned'])
    if args.upgrade:
        assert os.environ.get('GITHUB_ACTIONS') == 'true', 'Installation checks are CI-only'
        assert run(adb, 'get-serialno').stdout.strip().startswith('emulator-'), 'Physical devices forbidden'
        assert run(adb, 'shell', 'getprop', 'ro.kernel.qemu').stdout.strip() == '1', 'Disposable emulator required'
        assert 'package:' + PACKAGE not in run(adb, 'shell', 'pm', 'list', 'packages', PACKAGE).stdout, 'QA must start absent'
        manifest(aapt, args.upgrade, 3)
        original = snapshot(adb, ADMIN)
        driver = snapshot(adb, 'ru.transferservis.driver')
        marker = 'files/qa-installation-marker'
        run(adb, 'shell', 'run-as', ADMIN, 'touch', marker)
        try:
            with tempfile.TemporaryDirectory(prefix='admin-qa-ci-') as directory:
                temp = Path(directory)
                env = dict(os.environ, ADMIN_QA_TEST_PASSWORD='ci-disposable-only')
                for name in ('same', 'other'):
                    run('keytool', '-genkeypair', '-keystore', temp / (name + '.p12'), '-storetype', 'PKCS12',
                        '-storepass:env', 'ADMIN_QA_TEST_PASSWORD', '-keypass:env', 'ADMIN_QA_TEST_PASSWORD',
                        '-alias', 'qa-ci', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '2',
                        '-dname', 'CN=Disposable QA CI Only', env=env)
                for name, source, key in [('base', args.base, 'same'), ('upgrade', args.upgrade, 'same'), ('wrong', args.upgrade, 'other')]:
                    run(signer, 'sign', '--ks', temp / (key + '.p12'), '--ks-key-alias', 'qa-ci',
                        '--ks-pass', 'env:ADMIN_QA_TEST_PASSWORD', '--out', temp / (name + '.apk'), source, env=env)
                    run(signer, 'verify', temp / (name + '.apk'))
                run(adb, 'install', temp / 'base.apk')
                base = snapshot(adb, PACKAGE)
                assert base['uid'] not in (original['uid'], driver['uid']), 'Separate UID required'
                assert base['dataDir'] not in (original['dataDir'], driver['dataDir']), 'Separate app data required'
                assert run(adb, 'shell', 'run-as', PACKAGE, 'id', success=False).returncode != 0, 'QA must not allow run-as'
                launch = run(adb, 'shell', 'am', 'start', '-W', '-n', PACKAGE + '/ru.transferservis.admin.ui.AdminActivity')
                assert 'Status: ok' in launch.stdout and 'Error' not in launch.stdout, 'QA launcher failed'
                run(adb, 'shell', 'am', 'force-stop', PACKAGE)
                run(adb, 'install', '-r', temp / 'upgrade.apk')
                upgraded = snapshot(adb, PACKAGE)
                assert upgraded == dict(base, version=3), 'Upgrade must preserve UID/dataDir/first install time'
                wrong = run(adb, 'install', '-r', temp / 'wrong.apk', success=False)
                assert wrong.returncode != 0 and 'INSTALL_FAILED_UPDATE_INCOMPATIBLE' in wrong.stdout + wrong.stderr, 'Foreign signing key must fail'
                assert snapshot(adb, PACKAGE) == upgraded, 'Rejected update changed QA identity'
                assert snapshot(adb, ADMIN) == original, 'Original Admin changed'
                run(adb, 'shell', 'run-as', ADMIN, 'ls', marker)
                receipt['checks'] += ['side_by_side', 'distinct_uid_dataDir', 'not_debuggable', 'launch', 'same_key_upgrade', 'foreign_key_rejected', 'original_admin_preserved']
        finally:
            run(adb, 'uninstall', PACKAGE, success=False)
            run(adb, 'shell', 'run-as', ADMIN, 'rm', '-f', marker, success=False)
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(receipt, indent=2) + '\n')
    print('ADMIN_QA_DELIVERY=PASS ' + ','.join(receipt['checks']))


if __name__ == '__main__':
    main()
