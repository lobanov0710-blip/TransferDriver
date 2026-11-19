# Transfer Admin — первый этап G2

Отдельное Android Java + XML приложение ru.transferservis.admin. Firebase Driver
конфиг не используется; shared UID/session storage/Keystore aliases отсутствуют.
Текущий срез: staff login → server /v1/admin/me, безопасная сессия/refresh/logout,
read-only очередь регистраций dispatcher с drivers.read. Изменение статусов,
просмотр документов и другие разделы ещё не реализованы.

OWNER operational UI закрыт до отдельного утверждения/реализации MFA. TOTP выбран
как направление предложения: ../docs/ADMIN_MFA_TOTP_PROPOSAL.md. Это не серверная
MFA и не готовность к production. G1 приостановлен владельцем.

## Независимая debug-подпись

До первой debug-сборки, из корня проекта (JDK keytool):

    mkdir -p .local-signing
    keytool -genkeypair -keystore .local-signing/admin-debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Transfer Admin Debug"

Это общедоступные стандартные DEBUG passwords, не release credentials. Файл ключа
локальный/gitignored; production/upload keys не создаются здесь. Admin debug key
отличается от Driver debug key; Android CI проверяет разные UID/signatures.
На Windows папку можно создать New-Item -ItemType Directory .local-signing;
команда keytool та же. Не перезаписывать существующий ключ без необходимости.

## Варианты и проверки

- debug → STAGING backend, независимая debug-подпись;
- staging → STAGING, non-debug, unsigned;
- release → PRODUCTION endpoint, non-debug, unsigned, не разрешение на выпуск.

    bash ./gradlew :admin:testDebugUnitTest :admin:testStagingUnitTest :admin:testReleaseUnitTest :admin:assembleDebug :admin:assembleStaging :admin:assembleRelease :admin:bundleStaging :admin:lintDebug :admin:lintStaging :admin:lintRelease

Runtime tests: :admin:connectedDebugAndroidTest. Для проверки изоляции signatures/UID
перед запуском должны быть установлены debug Driver и Admin (Android CI делает это).
Не используйте реальные документы/production OWNER credentials для тестирования.
