# Следующий срез G2: внеплановый фотоконтроль Admin

Статус: **ПРЕДЛОЖЕНИЕ, НЕ УТВЕРЖДЕНО**. Дата: 2026-11-16.
Рекомендуется вариант «Каталог и список проверок», но выбор за владельцем.
Прежнее утверждение варианта A относилось к чтению назначений, а не к этому документу.

## Основание и текущая граница

[ТЗ §§14–15, 28–30](TRANSFER_DRIVER_TECHNICAL_SPEC_V1_0.md) предусматривает
фотоконтроль и два режима внепланового ограничения. D3 уже включает создание
проверки. Завершённый предыдущий срез — Admin manual assignment,
source `86ae9b46fe6b7918eb28cf00b822a34f26f3b52c`, CI `43877768185`.
Это component/synthetic checkpoint, не завершение всего G2.

Сводный план не задаёт отдельного утверждённого контракта каталога автомобилей
и очереди фотоконтроля после назначений. Подготовлен конкретный следующий срез
в рамках существующей функции ТЗ. Он не выбран и не реализован этим документом.
[AGENTS.md, п.3](../AGENTS.md) запрещает самостоятельно выбирать неутверждённое решение.

Проверенные источники backend: head
`506d7523c61e86cfb703a9a0eaffae5d36787294`,
tree `f57c9bed945281a44297a0f63eb0cc752a1fb328`,
`src/router.js`, `core/vehiclePhotoControlService.js`,
`core/vehiclePhotoControlRepository.js`, `core/staffAuthorization.js`,
`core/adminPhoneTestScope.js`, migrations 0016/0017 и две SQLite suites.
Android: docs head `ff628f71a561841a5d0ae80fef3ea2b6ba2de549`,
`RegistrationReview.java`, `ReviewPanel.java`, `AdminAccess.java`, `AdminApiClient.java`.

## Что уже есть в коде

| Операция | Подтверждённый контракт |
| --- | --- |
| Создать проверку | `POST /v1/admin/vehicles/{vehicleId}/photo-checks` |
| Прочитать известную проверку | `GET /v1/admin/vehicles/{vehicleId}/photo-checks/{photoCheckId}` |
| Решение по известной проверке | `POST /v1/admin/vehicles/{vehicleId}/photo-checks/{photoCheckId}/review` |

Создание принимает только `kind`, `reason`, `restrictionMode`, `expectedRevision`
и заголовок Idempotency-Key. Причина после trim — 1–500 символов.
Для внеплановой проверки уже существуют `kind=complaint|manual`, режимы
`after_current_trip|immediate`. `expectedRevision` относится к **автомобилю**.
ACK содержит check ID/revision и vehicleRevision; это разные версии.
Права проверяются сервером (`vehicles.verify`), staff authentication проверяет
действующее OWNER MFA. Чтение JavaScript-кода этого guard не заменяет HTTP/MFA E2E.

Коллекция `/v1/admin/vehicles/{vehicleId}/photo-checks` сейчас принимает только
POST: GET получает405 вне дополнительной phone-test блокировки. Общего
`GET /v1/admin/vehicles` в router нет. Список assignment-options привязан к
заказу и его permissions; он не заменяет каталог фотоконтроля. Модерационная
карточка даёт server vehicle ID/revision, но требует своего доступа к регистрациям.
Это пробел операционного чтения, а не доказанная уязвимость существующего API.

## Вариант «Каталог и список проверок» — рекомендуется

Небольшой законченный вход в управление фотоконтролем: выбрать автомобиль
в каталоге → увидеть проверки → явно выбрать внеплановую проверку/режим →
указать причину → подтвердить → получить ACK и перечитать состояние.
Срез включает метаданные и создание, не просмотр/одобрение реальных фотоматериалов.

Для утверждения предлагаются два API чтения, без новых таблиц и без новых
бизнес-правил. Параметры/поля ниже тоже являются предложением.

1. `GET /v1/admin/vehicles?status=...&limit=20&cursor=...`.
   `status` опционален и использует существующие статусы автомобиля;
   `limit`1–50, default20. Ответ `ok,vehicles,nextCursor,requestId`.
   Поля строки: `id,driverId,make,model,plate,status,revision,
   lastPhotoCheckPassedAt,nextPhotoCheckDueAt`. Даты могут быть null.
   Только `vehicles.read`; право чтения не даёт права назначать проверку.
   Имена/телефоны водителей, VIN, документы, фотографии, private keys/URLs
   и произвольные комментарии не входят в эту проекцию.
2. `GET /v1/admin/vehicles/{vehicleId}/photo-checks?limit=20&cursor=...`.
   Ответ `ok,vehicleId,vehicleRevision,checks,nextCursor,requestId`.
   Поля строки проверки: `id,vehicleId,kind,restrictionMode,result,revision,
   dueAt,graceUntil,createdAt,updatedAt`. Значения — из действующей модели;
   новых статусов/таймаутов нет. Только `vehicles.read` + `vehicles.verify`.
   Без фото, review notes, reason/free text и storage metadata в списке.

Оба чтения: активный staff session, текущие server permissions, OWNER с
действующим server MFA; no-store; audit чтения без чувствительного содержимого.
Неизвестные/повторные/пустые параметры отвергаются. Cursor непрозрачен для клиента,
ASCII base64url, до512 символов; scoped к коллекции/фильтру/vehicleId.
Стабильное keyset упорядочение по opaque ID; без скрытого cutoff и N+1.
Строгие IDs/types, safe integer revisions,401/403/404/400 и safe requestId/code.
SQL/provider output не отдаётся клиенту. Новых permissions не предлагается.

Существующие POST/GET конкретной проверки сохраняют действующие права;
каталог не должен расширять их. Материалы по существующему GET не загружаются
автоматически, raw response не сохраняется. Этот срез не добавляет preview
реальных фото или новые download URLs. Нельзя подменять проверку approve из
регистрации, автоматически выдавать passed или создавать monthly вручную в UI.

## Вариант «Из карточки регистрации» — ограниченный

Без новых API чтения: выбрать конкретный автомобиль в уже доступной карточке
регистрации и назначить внеплановую проверку через существующий POST.
Используются ID/revision выбранного server item, а не ввод внутренних ID вручную.
Нужны действующие права на карточку и `vehicles.verify`; права модерации не
выдаются ради доступа к фотоконтролю. Нельзя выбирать автомобиль автоматически.

После ACK можно перечитать известную проверку и карточку. После потери RAM
нет общего списка проверок для повторного открытия. Это ограниченное действие
в регистрации, а не самостоятельный раздел автомобилей/фотоконтроля и не
полная приёмка §15. Новые endpoints не нужны, но объём UX отличается от первого
варианта, поэтому он не выбран автоматически.

## Общие правила реализации после выбора владельца

Причина, kind и режим выбираются явно; защищённое подтверждение показывает
конкретный автомобиль и последствия режима словами из ТЗ. Без оптимистичного
успеха. Никакой отмены текущего заказа, фиктивной identity verification или
нового scheduling правила. Server enforcement обоих режимов сверяется с ТЗ
до подключения UI; обнаруженное несоответствие фиксируется и исправляется,
не маскируется новым клиентским правилом.

Перед GET/POST заново проверяются server me/permissions/MFA и исходная сессия.
Pending intent содержит исходные vehicle ID/revision/body/key. Неопределённый
результат — явное чтение либо явный повтор прежнего намерения с тем же ключом,
без automatic refresh/replay. При401/403/409 старый выбор закрывается.
onStop очищает причины, snapshots, pending intent и отменяет запросы;
новый вход не может использовать действие старой сессии или удалить её замену.
FLAG_SECURE, отсутствие raw logs/disk state/clipboard, Autofill и IME protections
сохраняются. Это требования к будущему срезу, не результат новых проверок UI.

## Проверки и порядок завершения

1. Backend: actual SQLite/HTTP и native Worker/D1 для выбранного контракта,
   current permissions/OWNER MFA, чужих IDs, revision races, same-key replay,
   обоих restriction modes, grace24h, no private projection, scoped pagination
   и OWNER-only404. Новые read audit не должны включать free text/cursor/PII.
2. Android: JVM/transport и actual Activity/Keystore synthetic tests для
   явного выбора/причины/режима/подтверждения, session replacement, revocation,
   ухода в фон, uncertain retry и404/409. Driver/Admin debug/staging/release
   build/lint, guards и фактически выполненные runtime reports.
3. Native кадры light/dark/font1.3, проверка PNG/provenance, независимые finish
   reviewer/documenter при изменении UI, фиксация всех замечаний/исправлений.
4. Отдельный verification receipt с точными source/run/counts и пределами.

Готовность существующего backend перепроверена только локально: Node24.22.1,
`photoControlSupport.sqlite.test.mjs`5 PASS (из них2 — support regression),
`photoControlLifecycle.sqlite.test.mjs`6 PASS; итого11, failures/skipped0.
Это существующие SQLite/service tests на синтетических fixtures, не новые GET,
не native Worker, не Android, не HTTP/MFA E2E и не проверка всех обоих режимов.
[Readiness receipt](G2_PHOTO_CONTROL_READINESS_2026-11-16.json).

G1 остаётся на паузе, D4–D7 не утверждены. Действующий OWNER-only STAGING
whitelist не расширяется; фотоконтроль там остаётся404. Используется синтетика;
реальные фото/документы, обработка биометрии и private R2 E2E не включаются.
Новых сборок APK, deploy, remote migrations или merge в ходе подготовки
предложения не выполнялось. Выбор среза/новых GET требует отдельного явного
решения владельца; ни один вариант этим документом не считается выбранным.

## Уточнение владельца: расширенный список марок

Владелец запросил Haval, «Cherry» и расширенный список для утверждения.
Подготовлен отдельный [справочник140 марок](VEHICLE_MAKES_CATALOG_PROPOSAL_2026-11-16.md)
и [JSON](VEHICLE_MAKES_CATALOG_PROPOSAL_2026-11-16.json). Для отображения предложено
Chery, для поиска — также Cherry/Черри/Чери. Текущие make/model остаются строками;
справочник помогает выбору названия и не определяет допуск/класс автомобиля.
Состав справочника и его внедрение ожидают решения; запрос списка не считается
утверждением новых GET или выбором объёма фотоконтроля из этого документа.
