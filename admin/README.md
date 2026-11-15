# Transfer Admin — первый этап G2

Отдельное Android Java + XML приложение ru.transferservis.admin. Firebase Driver
конфиг не используется; shared UID/session storage/Keystore aliases отсутствуют.
Текущий срез: staff login → MFA OWNER → server profile/session/refresh/logout;
пять фильтров очереди регистраций, карточка с историей, начало проверки,
поэлементные решения и завершение регистрации. Максимум 50 записей одного
запроса очереди; cursor pagination ещё не добавлена.

Просмотр/изменение определяется текущими серверными permissions. Для private
документа нужны documents.read_sensitive и соответствующее documents/identity
разрешение; фотографии требуют vehicles.verify. Клиент загружает только JPEG,
PNG или WebP с фиксированного backend endpoint, до 10 MiB, декодирует ограниченный
preview в памяти. Дискового cache, внешнего viewer, public URL или экспорта нет.
Перед подтверждением документа/фото UI требует просмотра материала. Identity
verified вручную не выставляется; окончательное одобрение дополнительно проверяет
backend readiness. Возврат/отклонение требует причины до 1000 символов.

Mutation использует исходную revision и уникальный Idempotency-Key; timeout/
неподтверждённый результат удерживает один action в volatile state. Только явный
повтор того же body/key либо новое чтение карточки. После 401 POST не переигрывается
через refresh или другую учётную запись. Данные карточки, image, причина и pending
action очищаются при уходе в фон и не входят в saved view state. Тесты — только
синтетические fixtures; реальные документы и настоящий E2E требуют D5/G1.

Параметры/API TOTP утверждены 2026-11-14: ../docs/ADMIN_MFA_TOTP_PROPOSAL.md.
Добавлены challenge/TOTP/recovery/enrollment UI. OWNER разделы требуют действующего
server MFA evidence из /v1/admin/me; роль сама по себе не открывает их.
Backend реализация находится в arch/g2-owner-totp-mfa; это не deployment или
готовность к production. G1 приостановлен владельцем.

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

MFA setup/key/recovery доступны только в защищённом foreground UI. Уход в фон очищает
временные данные и отменяет pending login; seed/коды не сериализуются в SavedState.
Ключ из authenticator нельзя получить повторно из приложения; при прерывании
неподтверждённого enrollment требуется новый protected bootstrap.
