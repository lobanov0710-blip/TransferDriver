# Подпись Transfer Driver: действия владельца

Владелец подтвердил: ключа пока нет; нужны Google Play и прямая установка APK.
Ниже подготовка, без автоматической регистрации аккаунтов, публикации или оплаты.

## 1. Upload-ключ создаётся на вашем компьютере

Установите [Temurin JDK 25 LTS для Windows](https://adoptium.net/temurin/releases/?version=25&os=windows),
включив Add to PATH. Откройте новый PowerShell. Проверка: `keytool -help`.

```powershell
New-Item -ItemType Directory -Path "$env:USERPROFILE\TransferDriver-Signing" -Force
Set-Location "$env:USERPROFILE\TransferDriver-Signing"
keytool -genkeypair -keystore "transfer-driver-upload.p12" -storetype PKCS12 -alias transfer-driver-upload -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname "CN=Transfer Driver Upload"
keytool -exportcert -rfc -keystore "transfer-driver-upload.p12" -storetype PKCS12 -alias transfer-driver-upload -file "transfer-driver-upload-cert.pem"
```

Пароль задаётся только в запросе keytool; символы при вводе не отображаются.
Сохраните пароль в менеджере паролей и резервную копию .p12 в защищённом месте.
Храните .p12 вне папки Git; резервную копию держите зашифрованной отдельно от пароля.
В чат можно передать только публичный `transfer-driver-upload-cert.pem`.
Файл `.p12` и пароль остаются у владельца.

## 2. Подписать тестовый AAB локально

После подготовки ключа скачайте staging AAB, проверьте SHA-256 по build receipt
и скопируйте его в ту же папку. Затем:

```powershell
Get-FileHash "TransferDriver-staging-unsigned.aab" -Algorithm SHA256
jarsigner -keystore "transfer-driver-upload.p12" -storetype PKCS12 -signedjar "TransferDriver-staging-upload-signed.aab" "TransferDriver-staging-unsigned.aab" transfer-driver-upload
jarsigner -verify "TransferDriver-staging-upload-signed.aab"
```

Staging AAB предназначен для тестового backend. Production release AAB — другой
артефакт. Google Play требует новый versionCode для каждой последующей загрузки.
Настройка upload key отличается от подписи APK, распространяемого пользователям:
[официальная схема Android](https://developer.android.com/studio/publish/app-signing).

## 3. Одинаковая подпись для двух каналов

Предлагаем Play App Signing с ключом приложения у Google и upload-ключом у владельца.
Для прямой раздачи берите **Signed, universal APK** из Play Console:
Test and release → Latest releases and app bundles → All app versions → версия →
Downloads → Assets. Этот APK подписан тем же app signing key, что и Play-версия.
Не подписывайте его upload-ключом повторно.
[Источник Google Play](https://support.google.com/googleplay/android-developer/answer/9844279?hl=en).
Публикацию, платный аккаунт и юридические сведения подтверждает владелец.

## 4. Firebase App Check после настройки Play

Владелец сообщил, что Phone уже включён, App Check ещё не настроен.
В Play Console → App integrity привяжите `transfer-driver-1590d`; затем в
[Firebase App Check](https://console.firebase.google.com/project/transfer-driver-1590d/appcheck/apps)
зарегистрируйте Driver с Play Integrity и SHA-256 **app signing certificate** из Play.
В Firebase → Project settings → Your apps → Transfer Driver добавьте SHA-1 и
SHA-256 того же app signing certificate для Phone Auth.
Upload certificate и debug certificate не заменяют этот сертификат.

Для обоих каналов официальная таблица Firebase рекомендует PLAY_RECOGNIZED required,
LICENSED not required; дополнительное требование Device integrity в этой строке
не устанавливается. Библиотека Play Integrity и backend App Check verification
сохраняются. Способ распространения и подпись нужно сверить до реального входа.
[Официальная документация Firebase](https://firebase.google.com/docs/app-check/android/play-integrity-provider).

## Текущее ограничение

Подготовленный debug APK — проверочная сборка; он не становится релизом после
добавления google-services.json. Реальный вход/E2E ожидает App Check, корректную
релизную подпись/канал и готовый private STAGING R2/backend deployment.
Технические решения D4–D7 этим документом не изменяются.
