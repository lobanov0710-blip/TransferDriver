# Предложение: чтение данных ручного назначения в Transfer Admin

Версия2 (исправлена проверка legacy roles). Статус: НЕ УТВЕРЖДЕНО. Это предложение технической детализации D3, не изменение
ТЗ и не разрешение на deploy. Реализация зависящих новых API ожидает решения.

## Подтверждённый пробел

POST /v1/admin/orders/{id}/assign|reassign уже поддерживает OWNER/уполномоченного
DISPATCHER, reason, expectedRevision, Idempotency-Key и server eligibility.
GET /orders доступен OWNER/DISPATCHER: их staff authentication category=admin,
а operational role/permissions сервер определяет отдельно. Первоначальное
утверждение о недоступности этого endpoint было ошибкой анализа и отозвано.
Проверены core/adminAuthorization.js и AdminSession: базовая роль admin сохраняется.
Однако legacy выдаёт также passenger name/phone/comment и читает лишь последние
1000 orders до фильтра; cursor нет. Это ограничение полноты списка, а не отказ ролям.
GET /staff/accounts не даёт совместного snapshot автомобилей/доступности;
registration review требует verification permissions, которых у обычного
DISPATCHER нет. Не использовать legacy роль, угаданные revisions, ввод внутренних
ID вручную или moderation permissions как замену операционному чтению.

## Предлагаемый согласованный вариант

1. GET /v1/admin/orders — операционный список для ручного выбора заказа.
   Параметры status=new|taken|in_progress, limit=1..50 (default20), cursor.
   Ответ: ok, orders, nextCursor|null, requestId. Поля заказа: id, from, to,
   date, tariff, status, revision, текущая assignment (id/driverId/vehicleId/
   revision/phase) либо null. Новое назначение только new без активной assignment;
   reassign — только согласно существующему mutation service.
2. GET /v1/admin/orders/{id}/assignment-options — paginated snapshot порядка
   выбора связки driver+vehicle для этого заказа, с orderId/orderRevision,
   options и nextCursor|null. В каждой строке driverId/fullName, vehicleId/
   make/model/plate/status, availability state/activeVehicleId, eligibility
   и stable blocker codes по действующим D3 проверкам mutation/repository.
   Упорядочение стабильное по opaque IDs, без score/rank/автовыбора. Никаких
   рекомендаций D6, расстояний/GPS, новых правил приоритета либо новых критериев
   допуска. Offline отображается явно, не вводится новый запрет назначения
   offline, которого сейчас нет в mutation service. Пустой список — явный UI state.

Оба endpoint: server-resolved OWNER с действующим MFA либо DISPATCHER с явными
orders.read; options также drivers.read + vehicles.read и нужное orders.assign
либо orders.reassign. Потеря права проверяется на каждом запросе; роли/допуск
не доверяются клиенту. Телефон/имя пассажира/comment, DOB/phone водителя,
документы, biometric material и private URLs не возвращаются. Route from/to
нужны оператору и защищены orders.read; никакой публичной выдачи.

Pagination: opaque cursor по стабильному ключу, строгая валидация, отсутствие
скрытого усечения; неизвестные параметры отвергаются. Read security audit без
PII/токенов. Новых таблиц/миграций не предлагается. Ошибки401/403/404/400,
stable code +requestId, raw provider/SQL output клиенту не выдаётся.

Client после выбора отправляет только действующий assign/reassign body:
driverId, vehicleId, reason, expectedRevision и Idempotency-Key. Причина
обязательна; подтверждает человек. Сервер повторно атомарно проверяет состояние,
права/допуск/занятость и audit. Read snapshot не даёт права назначить сам по себе.
При409/неопределённом ответе — обновление данных, без автоматического другого
кандидата/новой операции; повтор того же намерения только явно с тем же key/body.

## Границы решения

Вариант A (рекомендуется): два описанных GET с минимальной server projection
и полной cursor pagination; legacy endpoints остаются без изменений.
Вариант B (минимальный): только новый assignment-options, список через действующий
GET /orders. В таком варианте legacy cutoff1000 и extra PII response сохраняются;
клиент покажет ограничение списка и не будет отображать/сохранять ненужные поля.
Это не полная история заказов. Варианты пока не выбраны; можно обсудить отдельно.
До решения готовится независимый Driver availability UI по уже существующим API.
G1 остаётся на паузе. ADMIN_PHONE_TEST_ONLY не меняется, новые operational routes
на текущем OWNER-only STAGING не открываются. Проверки синтетические. Merge и
production deploy требуют отдельного решения владельца.
