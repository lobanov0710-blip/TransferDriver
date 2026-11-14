# Transfer Admin: предложение TOTP MFA для OWNER

Дата: 2026-11-14. **СТАТУС: ПАРАМЕТРЫ И API УТВЕРЖДЕНЫ ВЛАДЕЛЬЦЕМ 2026-11-14.**
Основание: D1 требует усиления MFA OWNER в первом этапе Admin. Владелец выбрал
вариант 1 (TOTP + резервные коды) на вопрос, какой вариант подготовить для
отдельного технического утверждения. Затем владелец явно подтвердил: «Предложение параметров и API TOTP утверждаю».
G1 приостановлен владельцем; эта запись не возобновляет его и не разрешает deploy.

## Утверждённое поведение

Пароль OWNER сам по себе не выдаёт рабочую staff session. После проверки пароля
backend выдаёт только краткоживущий одноразовый MFA challenge. TOTP либо резервный
код завершают вход; только после этого выдаются обычные access JWT/rotating refresh.
Dispatcher сохраняет текущий staff-auth; операционные permissions остаются серверными.
Существующий auth role admin не считается OWNER: OWNER берётся из staff_owner_binding.

Исторический первый срез Admin (до данного утверждения) использовал API лишь для входа/собственного профиля и
чтения очереди dispatcher с drivers.read. Рабочие разделы OWNER в ней закрыты.
Это клиентское ограничение незавершённой функции, а не внедрённая серверная MFA:
backend до реализации этого предложения использовал прежнюю staff-auth.
Утверждённая реализация теперь подготовлена в arch/g2-transfer-admin и
arch/g2-owner-totp-mfa; точный статус проверок и отсутствие deployment — в журнале.

## Утверждённые параметры

| Параметр | Утверждено |
|---|---|
| Алгоритм | RFC 6238, HMAC-SHA-1, 6 цифр, период 30 секунд; совместимость со стандартными TOTP authenticator apps |
| Seed | 20 случайных bytes CSPRNG; на сервере только AES-256-GCM ciphertext с account-bound AAD и версией ключа |
| Ключ шифрования | Отдельный ADMIN_MFA_ENCRYPTION_KEY в защищённом серверном secret storage; не в Git/D1/Android |
| Допустимое смещение | Предыдущий/текущий/следующий 30-секундный интервал; успешный counter одноразовый и фиксируется атомарно |
| Challenge | 5 минут, одноразовый, максимум 5 неверных попыток; привязан к аккаунту и login challenge, без рабочей access session |
| Лимиты | Дополнительно 10 неуспешных проверок / 15 минут на аккаунт и 20 / минуту на IP; общий фиксированный ответ без раскрытия аккаунта |
| Подтверждение MFA | Максимум 12 часов и не дольше исходного срока session family; refresh не продлевает этот срок |
| Резервные коды | 10 кодов по 128 случайных bits, показ один раз; на сервере только SHA-256 digest с контекстом accountId, атомарное одноразовое использование |

Значения утверждены для реализации и локальных тестов. Изменение сроков
OWNER MFA явно ограничит его рабочий доступ раньше общей 30-дневной staff family;
сроки Driver/Dispatcher при этом не меняются.

## Утверждённый контракт

1. POST /staff/login: для OWNER после правильного пароля ответ содержит только
   ok, mfaRequired, challengeId, expiresAt; accessToken/refreshToken отсутствуют.
   Без подключённой MFA OWNER получает требование подключения, без рабочей сессии.
2. POST /v1/admin/mfa/totp/verify: challengeId + code; успешный ответ — существующий
   staff login response. Код и challenge не попадают в URL/логи; повтор запрещён.
3. POST /v1/admin/mfa/recovery/verify: challengeId + recoveryCode; те же ограничения.
4. Первичное подключение — отдельный одноразовый bootstrap-код, созданный владельцем
   через его защищённый серверный control plane. Один пароль не разрешает привязать
   новый seed. Bootstrap: только текущий OWNER, 15 минут, до 3 попыток, digest-only
   storage, одноразовая атомарная активация после первого правильного TOTP.
5. Enrollment start/confirm endpoints и локальный owner bootstrap CLI подготавливаются
   после утверждения. Seed показывается только в защищённом экране; без передачи
   внешнему QR-сервису, записи в clipboard, аналитики или сохранения в View state.
6. Потеря устройства: резервный код. Сброс MFA без него — отдельное подтверждённое
   владельцем control-plane действие с отзывом всех OWNER sessions и audit;
   fallback на SMS/один пароль/самостоятельный сброс dispatcher не допускается.

## Серверная защита

Добавочные локально проверенные миграции для enrollment/challenges/recovery и
MFA evidence в staff sessions. Каждая OWNER операция, включая legacy admin paths,
проверяет evidence/expiry из D1; client header или локальный role flag его не заменяют.
Существующие password-only OWNER sessions до MFA не дают operational access.
Refresh переносит evidence только из той же подтверждённой family и не продлевает MFA.
Изменение MFA отзывает все OWNER session families. Сбор биометрии не добавляется.

Audit фиксирует enrollment, успешный/неуспешный challenge, replay, использование
recovery и reset, без seed/кодов/паролей/raw tokens. Отдельный rate limit защищает
MFA; существующая защита password login сохраняется.

## Критерии локальной приёмки

- Правильный пароль без MFA не даёт OWNER staff session/доступ через legacy paths.
- Driver/Dispatcher не становятся OWNER и не меняют его MFA.
- Неверный/просроченный код, чужой challenge и replay отвергаются.
- Из двух одновременных использований одного counter/recovery кода успешно одно.
- Лимиты, TTL, refresh/logout/reset и отзыв прав проверены на реальном SQLite/workerd.
- Seed зашифрован; в D1/logs/FCM/Android state нет seed, raw recovery или passwords.
- Android Keystore, origin locks, backup/secure windows и account/session races проверены.

Явное утверждение параметров и API получено 2026-11-14. Разрешены реализация
и локальные проверки. Утверждение реализации/локальных тестов не разрешает remote migration apply,
merge, deployment или production release. D4–D7 остаются вне этого решения.
