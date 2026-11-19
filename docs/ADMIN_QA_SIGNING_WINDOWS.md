# Transfer Admin QA: постоянная подпись на Windows

Вариант A утверждён 2026-11-19. Это отдельное тестовое приложение
`ru.transferservis.admin.qa`, **Transfer Admin QA**, Android 8+, только STAGING.
Оно устанавливается рядом с прежним Admin. Старый Admin не удаляйте.
Этот ключ не позволит обновить прежний `ru.transferservis.admin` с иной подписью.

## Подготовка

Нужны JDK (например, Temurin 17+) и Android SDK Build-Tools 37.0.0.
SDK Manager в Android Studio: SDK Tools → Show Package Details →
Android SDK Build-Tools 37.0.0. Системные Android-проверки не отключайте.
Ключ храните вне Git/OneDrive/общих папок; пароль — в менеджере паролей,
зашифрованную резервную копию ключа — отдельно. Не включайте запись терминала.
Ключ/пароль в чат не передавайте. Ключ Driver здесь не используется.

## Создать ключ один раз

В обычном Windows PowerShell, на своём компьютере:

```powershell
$qaSigningDir = Join-Path $env:USERPROFILE 'TransferAdmin-QA-Signing'
New-Item -ItemType Directory -Path $qaSigningDir -Force | Out-Null
$qaKey = Join-Path $qaSigningDir 'transfer-admin-qa.p12'
if (Test-Path -LiteralPath $qaKey) { throw 'Ключ уже существует: не создавайте замену.' }
keytool -genkeypair -keystore $qaKey -storetype PKCS12 -alias transfer-admin-qa -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname 'CN=Transfer Admin QA'
if ($LASTEXITCODE -ne 0) { throw 'Создание ключа не завершено.' }
```

Пароль вводится только по приватному запросу keytool, символы не отображаются.
Выберите стойкий отдельный пароль. Создайте зашифрованную резервную копию.
При будущих обновлениях **не повторяйте создание** и не заменяйте этот файл.

Сохраните публичный сертификат и его отпечаток:

```powershell
$qaCert = Join-Path $qaSigningDir 'transfer-admin-qa-cert.der'
keytool -exportcert -keystore $qaKey -storetype PKCS12 -alias transfer-admin-qa -file $qaCert
if ($LASTEXITCODE -ne 0) { throw 'Экспорт публичного сертификата не завершён.' }
(Get-FileHash -LiteralPath $qaCert -Algorithm SHA256).Hash.ToLowerInvariant()
```

Полученные 64 символа — SHA256 **публичного сертификата**, не пароль/ключ.
Сохраните отпечаток независимо от keystore; его можно передать для сверки.
Потерянный приватный ключ невозможно восстановить из APK/сертификата.

## Подписать проверенный APK

Используйте выданный **unsigned QA APK**, receipt с его SHA256 и script
`tools/sign_admin_qa.ps1` **из того же проверенного commit**, не прежний debug APK.
Сначала сравните хеш скачанного файла с receipt. Пример вызова из папки проекта:

```powershell
.\tools\sign_admin_qa.ps1 -UnsignedApk 'C:\путь\admin-qa-unsigned.apk' -ExpectedSha256 'SHA256_ИЗ_BUILD_RECEIPT' -ExpectedCertificateSha256 'SHA256_ВАШЕГО_ПУБЛИЧНОГО_СЕРТИФИКАТА' -KeyStore "$env:USERPROFILE\TransferAdmin-QA-Signing\transfer-admin-qa.p12" -OutputApk 'C:\путь\TransferAdmin-QA-v2.apk'
```

Замените пути и два SHA256; пароль не добавляйте в команду. Если SDK расположен
в другом каталоге, передайте `-SdkRoot 'C:\путь\Android\Sdk'`.
При блокировке PowerShell policy остановитесь и сообщите только безопасный текст
ошибки: не меняйте системную политику безопасности для обхода проверки.
Скрипт проверяет входной хеш/пакет/security flags/alignment, запрашивает пароль
в терминале, проверяет подпись и точное совпадение с публичным отпечатком.
Успех — `QA_SIGNING=PASS` и SHA256 подписанного APK. Это новый хеш: подпись
меняет файл. Сохраните хеш и versionCode рядом с публичным receipt.

## Установка и последующие обновления

Передайте **подписанный** `TransferAdmin-QA-v2.apk` на телефон и откройте его.
Появится отдельный значок **Transfer Admin QA**. Старый Admin и его данные
сохраняются. Войдите существующим OWNER и действующим TOTP; повторно создавать
OWNER/сбрасывать фактор/вводить bootstrap для уже настроенного MFA не требуется.
Приложения хранят сессии отдельно, автоматического переноса между ними нет.

Каждый следующий выданный QA APK должен иметь больший versionCode и быть
подписан **тем же файлом ключа**. Тогда Android принимает обычное обновление QA.
Подпись новым ключом при прежнем имени пакета снова заблокирует обновление.
Если установка не прошла, сохраните приложение и сообщите текст ошибки/версию
Android; не удаляйте приложения/данные и не отключайте защиту телефона.

Физическая установка/вход остаются проверкой владельца. CI использует только
одноразовые синтетические ключи для теста поведения Android, они не выдаются для
телефона и не заменяют ваш постоянный ключ. Backend/OWNER-phone scope/G1 не меняются.
