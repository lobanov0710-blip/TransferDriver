"""Offline source/configuration guards. These do not replace Android runtime tests."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main'
JAVA = MAIN / 'java/ru/transferservis/driver'
A = '{http://schemas.android.com/apk/res/android}'


class SecurityConfigurationTests(unittest.TestCase):
    def test_backup_and_device_transfer_exclude_all_storage(self):
        required = {'root', 'file', 'database', 'sharedpref', 'external',
                    'device_root', 'device_file', 'device_database', 'device_sharedpref'}
        legacy = ET.parse(MAIN / 'res/xml/backup_rules.xml').getroot()
        extraction = ET.parse(MAIN / 'res/xml/data_extraction_rules.xml').getroot()
        for section in (legacy, extraction.find('cloud-backup'), extraction.find('device-transfer')):
            self.assertIsNotNone(section)
            self.assertEqual(required, {x.attrib['domain'] for x in section.findall('exclude')
                                        if x.attrib.get('path') == '.'})

    def test_only_launcher_is_exported_and_sensitive_screens_are_secure(self):
        app = ET.parse(MAIN / 'AndroidManifest.xml').getroot().find('application')
        self.assertEqual('false', app.get(A + 'allowBackup'))
        self.assertEqual('false', app.get(A + 'usesCleartextTraffic'))
        self.assertEqual('@xml/backup_rules', app.get(A + 'fullBackupContent'))
        self.assertEqual('@xml/data_extraction_rules', app.get(A + 'dataExtractionRules'))
        self.assertEqual('@xml/network_security_config', app.get(A + 'networkSecurityConfig'))
        for component in list(app):
            if component.tag not in {'activity', 'service', 'receiver', 'provider'}:
                continue
            name = component.get(A + 'name')
            self.assertEqual('true' if name == '.ui.login.LoginActivity' else 'false',
                             component.get(A + 'exported'), name)
            if component.tag == 'activity':
                source = JAVA / (name.lstrip('.').replace('.', '/') + '.java')
                self.assertRegex(source.read_text(), r'extends\s+SecureActivity\b')
        self.assertIn('FLAG_SECURE', (JAVA / 'security/SecureActivity.java').read_text())

    def test_transport_does_not_trust_user_cas_or_cleartext(self):
        config = ET.parse(MAIN / 'res/xml/network_security_config.xml').getroot()
        self.assertEqual('false', config.find('base-config').get('cleartextTrafficPermitted'))
        self.assertEqual(['system'], [x.get('src') for x in config.iter('certificates')])
        self.assertIsNone(config.find('debug-overrides'))
        client = (JAVA / 'data/api/SecureHttpClient.java').read_text()
        for guard in ('.followRedirects(false)', '.followSslRedirects(false)',
                      '.retryOnConnectionFailure(false)', 'ApiEndpointPolicy.isAllowed'):
            self.assertIn(guard, client)
        for path in JAVA.rglob('*.java'):
            if path.name != 'SecureHttpClient.java':
                self.assertNotIn('new OkHttpClient.Builder()', path.read_text(), str(path))

    def test_notification_code_cannot_read_passenger_fields(self):
        code = (JAVA / 'notifications/DriverNotificationManager.java').read_text()
        self.assertIsNone(re.search(r'\.get(?:Route|From|To|Name|Phone|Comment)\s*\(', code))
        self.assertIn('PendingIntent.FLAG_IMMUTABLE', code)
        receiver = (JAVA / 'notifications/TransferFirebaseMessagingService.java').read_text()
        self.assertIn('hasRefreshableDriverSession()', receiver)
        self.assertIn('AssignmentPushEvent.parse(', receiver)

    def test_phone_and_sms_are_not_saved_in_view_state(self):
        view = ET.parse(MAIN / 'res/layout/activity_login.xml').getroot()
        for name in ('editPhone', 'editSmsCode'):
            field = next(x for x in view.iter() if x.get(A + 'id') == '@+id/' + name)
            self.assertEqual('false', field.get(A + 'saveEnabled'), name)

    def test_no_secret_literals_or_application_logging_in_sources(self):
        patterns = [r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
                    r'AIza[0-9A-Za-z_-]{35}', r'gh[pousr]_[A-Za-z0-9]{30,}',
                    r'\bLog\.[vdiew]\s*\(', r'\.printStackTrace\s*\(',
                    r'HttpLoggingInterceptor']
        for path in JAVA.rglob('*.java'):
            code = path.read_text()
            for pattern in patterns:
                self.assertIsNone(re.search(pattern, code), str(path.relative_to(ROOT)))


if __name__ == '__main__':
    unittest.main(verbosity=2)
