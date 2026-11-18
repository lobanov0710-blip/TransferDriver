# Следующий шаг G2: проверка параметров и системный класс

**Статус: контракт утверждён 2026-11-17; новые права диспетчерам запрещены.**
Продолжение проверенных Driver PR14/backend PR35. Бизнес-решения владельца
не отменяются и не запрашиваются повторно. Новые API/права ещё не реализованы.

Полный [проект контракта](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-admin-classification-contract/docs/ADMIN_VEHICLE_CLASSIFICATION_CONTRACT_PROPOSAL_2026-11-17.md),
[OpenAPI](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-admin-classification-contract/docs/admin-vehicle-classification-v1.proposed.openapi.json)
и [аудит исходников](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-admin-classification-contract/docs/ADMIN_VEHICLE_CLASSIFICATION_AUDIT_2026-11-17.json)
подготовлены в отдельной зависимой backend-ветке.

## Конкретное предложение владельцу

1. Проверка конкретного автомобиля — существующее vehicles.verify. Для общих
   правил — отдельное vehicle_rules.manage; индивидуальное исключение требует
   vehicles.approve_exception вместе с vehicles.verify. Эти новые права
   диспетчерам не выдаются и не признаются, даже при ошибочной записи гранта. OWNER сохраняет действующее серверное MFA.
2. Закрепить правило при первом сохранении автомобиля. Пока документы/параметры
   не проверены, это не даёт класса/линии. Новое правило не меняет прежнюю
   привязку; цвет/фото/служебная revision также не выбирают его заново.
   При смене make/model/year — текущая версия по уже принятому решению владельца.
3. Admin подтверждает год, исполнение и условия по документам. Может исправить
   параметры с историей; исключение связывает реальную модель с соответствующей
   моделью той же марки из справочника. Класс берёт сервер из правила.
   BEV/запрещённый бизнес-кузов не обходятся исключением; Li Auto L7/L9 сохраняются.

Владелец утвердил гранулярные права/момент фиксации версии с поправкой:
новые полномочия правил и исключений — только OWNER. Повторное согласование
не требуется. Ниже сохранено основание исторического запроса решения.
Основание запроса решения — [AGENTS.md](../AGENTS.md), п.3:
«Не заменять отсутствующее решение владельца предположением» и
«дождаться его явного решения». В [решениях владельца](VEHICLE_RULES_OWNER_DECISIONS_2026-11-17.md)
прямо указано: «До реализации нужны ... согласованный контракт справочника/правил».

## Что выявила сверка

Admin bundle/DTO ещё не представляет год и trusted классификацию; null-класс
в backend review превращается в пустую строку. Одобрение, on_line и назначение
пока не требуют подтверждённого года/assessment. Snapshot0019 не представляет
отдельную модель-основание исключения или допустимый год ниже порога;
ослабление его существующих FK/triggers не предлагается. Общая vehicle revision
не подходит для выбора новой версии при каждом изменении цвета/фото/статуса.
Чистый evaluator candidate не проверяет кузов/примечания и не является допуском.
Эти открытые задачи входят в проект приёмки, не объявлены исправленными.

## Порядок после утверждения

Backend trusted read/assessment и добавочная миграция с populated rollback;
проверки прав/MFA/ревизий/idempotency/audit и immutable version binding.
Затем server admission guards (approval/line/assign/reassign/phase/done),
согласованные Admin DTO/readonly class/форма условий и явное подтверждение.
Параметры, evidence previews и причины только foreground memory; no replay,
проверка текущей сессии/прав/MFA, secure capture и отдельные sensitive grants.
Новые операции не расширяют allowlist OWNER-only STAGING автоматически.

Обязательны backend/SQLite/nativeD1 и Android JVM/native/build/lint/security,
актуальные PNG/provenance. В этом docs-only шаге повторены5 backend test
entry points,6 reference cases и security guards8 Driver/5 Admin — PASS.
OpenAPI/schema/local refs: PASS,4 valid/8 invalid examples. Это не runtime
проверка будущих endpoints; новая сборка/APK не создавалась.

Полный BEV обзор121 строк, переход старых записей и полный in_progress
dispatcher handoff остаются отдельными незавершёнными работами. Нет seed,
реальных документов, remote migration, merge/deploy. G1paused,D4–D7pending.


## Первый проверенный prerequisite после утверждения

[Backend PR37](https://github.com/lobanov0710-blip/uber-v3/pull/37):
first-save binding и OWNER-only protected powers. CI44078545851 SUCCESS;
[provenance и границы](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-classifier-binding/docs/VEHICLE_RULE_BINDING_VERIFICATION_2026-11-17.json).
Trusted assessment/API/admission/Admin UI — следующие части контракта,
не считаются готовыми по наличию binding. Новых Android binaries здесь нет.
## Текущий UI prerequisite — 2026-11-18

Backend trusted/public review и история теперь проверены в PR №43 (source `5836e183f86c656c17bf77f77bb94e968f730df5`, CI `44240856017`). Android Draft PR №17 реализует строгие DTO и readonly карточку текущей классификации/истории: CI `44245721289` attempt2 PASS, source `b7eb21f4a824ca3da51e7a0ed05a9158b3721739`. [Scope](ADMIN_CLASSIFICATION_READ_UI_2026-11-18.md), [receipt](ADMIN_CLASSIFICATION_READ_UI_VERIFICATION_2026-11-18.json). Следующий UI срез — форма параметров/условий и OWNER exception, confirmation/explicit retry/privacy; право вручную выбрать class не вводится. Авторство общих правил/UI и полная приёмка ещё впереди. Исторические статусы выше относятся к своим проверенным срезам и не отменяют этот checkpoint.
