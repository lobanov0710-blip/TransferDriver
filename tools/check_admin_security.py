"""Offline Admin isolation/privacy guards; runtime coverage is in androidTest."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'admin/src/main'
JAVA = MAIN / 'java/ru/transferservis/admin'
A = '{http://schemas.android.com/apk/res/android}'


class AdminSecurityConfigurationTests(unittest.TestCase):
    def test_admin_has_separate_identity_and_no_driver_or_firebase_dependency(self):
        build = (ROOT / 'admin/build.gradle.kts').read_text()
        self.assertIn('applicationId = "ru.transferservis.admin"', build)
        self.assertIn('.local-signing/admin-debug.keystore', build)
        self.assertNotIn('firebase', build)
        self.assertNotIn('project(":app")', build)
        for p in JAVA.rglob('*.java'):
            self.assertNotIn('import ru.transferservis.driver.', p.read_text(), str(p))

    def test_only_launcher_exported_and_no_shared_uid(self):
        manifest = ET.parse(MAIN / 'AndroidManifest.xml').getroot()
        self.assertIsNone(manifest.get(A + 'sharedUserId'))
        self.assertEqual({'android.permission.INTERNET'}, {e.get(A + 'name') for e in manifest.findall('uses-permission')})
        app = manifest.find('application')
        for tag in ('activity', 'service', 'receiver', 'provider'):
            for e in app.findall(tag):
                self.assertEqual('true' if e.get(A + 'name') == '.ui.AdminActivity' else 'false', e.get(A + 'exported'))
        for name in ('allowBackup', 'usesCleartextTraffic'):
            self.assertEqual('false', app.get(A + name))

    def test_backup_and_transfer_exclude_all_domains(self):
        domains = {'root','file','database','sharedpref','external','device_root','device_file','device_database','device_sharedpref'}
        backup = ET.parse(MAIN / 'res/xml/backup_rules.xml').getroot()
        rules = ET.parse(MAIN / 'res/xml/data_extraction_rules.xml').getroot()
        for section in (backup, rules.find('cloud-backup'), rules.find('device-transfer')):
            self.assertEqual(domains, {e.get('domain') for e in section.findall('exclude') if e.get('path') == '.'})

    def test_sensitive_views_and_system_trust_only(self):
        views = ET.parse(MAIN / 'res/layout/activity_admin.xml').getroot()
        for name in ('login','password','role','registrations','totpCode','recoveryCode','bootstrapCode','enrollmentCode','enrollmentSecret','backupCodes'):
            view = next(e for e in views.iter() if e.get(A+'id') == '@+id/'+name)
            self.assertEqual('false', view.get(A+'saveEnabled'))
        self.assertIn('FLAG_SECURE', (JAVA / 'ui/AdminActivity.java').read_text())
        net = ET.parse(MAIN / 'res/xml/network_security_config.xml').getroot()
        self.assertEqual('false', net.find('base-config').get('cleartextTrafficPermitted'))
        self.assertEqual(['system'], [e.get('src') for e in net.iter('certificates')])
        self.assertIsNone(net.find('debug-overrides'))

    def test_no_raw_secrets_application_logs_or_logging_interceptors(self):
        patterns = [r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',r'AIza[0-9A-Za-z_-]{35}',
                    r'gh[pousr]_[A-Za-z0-9]{30,}',r'\bLog\.[vdiew]\s*\(',r'\.printStackTrace\s*\(',r'HttpLoggingInterceptor']
        for p in JAVA.rglob('*.java'):
            for pattern in patterns:
                self.assertIsNone(re.search(pattern,p.read_text()), str(p.relative_to(ROOT)))


if __name__ == '__main__':
    unittest.main(verbosity=2)
