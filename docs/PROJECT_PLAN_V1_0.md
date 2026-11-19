# План реализации ТЗ Transfer Driver v1.0

> **Главное правило:** только утверждённые правила, ТЗ и решения журнала; самовольные
> отклонения запрещены. Неутверждённые варианты предлагаются владельцу без выбора за него.
> [Обязательное правило работы](../AGENTS.md).

Актуализация: 2026-11-14. Основание — [утверждённое ТЗ](TRANSFER_DRIVER_TECHNICAL_SPEC_V1_0.md),
[решения D1–D7](TECHNICAL_DECISIONS_V1_0_PROPOSED.md), журналы и фактический код.
Это сводный рабочий план существующего объёма, а не изменение ТЗ или новое утверждение.
D1–D3 утверждены; D4–D7 остаются неутверждёнными.

**Общий статус: частичная реализация; к production-выпуску не готово.**
Процент готовности не вычисляется: таблицы БД, API, UI и принятый E2E имеют разный вес.
Успешный CI не закрывает незавершённый пользовательский сценарий.

## Решение владельца 2026-11-14

G1 приостановлен. Владелец явно поручил начать G2. Активная работа — отдельный
Transfer Admin по D1–D3. Для OWNER выбран TOTP как направление подготовки
отдельного предложения; окончательные MFA API/параметры ещё не утверждены.
G1 не возобновляется без решения владельца; D4–D7 не утверждены.

## Как читать статусы

- «Код/частично» — есть реализация указанной части и проверки; весь раздел ТЗ не принят.
- «Отсутствует» — требуемая пользовательская функция не найдена в проверенном коде.
- «Блокировано» — нужна инфраструктура либо отдельное решение владельца.
- «Подтверждено» — проверено конкретное утверждение документа, а не весь продукт.

База аудита: Driver 0c7777025c7c58231efaedbb2b8d82c335807ce4;
backend 905c6ebe86907056f0d8be8f6f6edaff07b135e4.
Подробные замечания и границы проверки — [аудит 2026-11-14](AUDIT_SPEC_PLAN_JOURNAL_2026-11-14.md).

## Матрица всех разделов ТЗ

| № ТЗ | Статус на дату аудита | Реализовано / осталось для приёмки |
|---|---|---|
| 1 | Частично | Backend определяет права и состояния. Полный контур четырёх систем, включая отдельный Admin, ещё не завершён. |
| 2 | Код; E2E блокирован | Phone Auth, App Check и exchange → staff session реализованы. Настроить подпись/канал/App Check и подтвердить настоящий вход; он не даёт автоматический допуск. |
| 3 | Частично | Backend draft/update/submit и completeness есть; мастера регистрации в Driver нет. Реальные документы/биометрия требуют D5. |
| 4 | Частично | Серверные профильные поля есть; нужны UI ввода/исправления и сквозная проверка сохранения. |
| 5 | Частично; D4/D5 | Типы passport_selfie и identity_reference_selfie, private upload/readiness предусмотрены. Полный capture/identity/consent flow не принят. |
| 6 | Блокировано D4/D5 | Серверная модель есть; выбранного и интегрированного face/liveness provider нет. Административный review не заменяет автоматическую проверку. |
| 7 | Частично; D4/D5 | Challenge и SQL binding/state guard есть; пути получения настоящего verified и trip-selfie UI нет. Галерея и локальный успех не допускаются. |
| 8 | Частично | Backend учитывает обе стороны ВУ; нет полного Driver upload/edit/review UX и E2E. |
| 9 | Частично | Серверная модель нескольких автомобилей, VIN и revision есть; Driver UI автомобилей не реализован. |
| 10 | Частично | СТС, usage_basis и условный rental_contract проверяются сервером; нужны UI и private-storage E2E. |
| 11 | Частично; D5/D6 | Дополнительные документы предусмотрены; юридический состав и применение в приоритете не утверждены. |
| 12 | Частично | Семь типов фото и серверная полнота есть. Photo-control UI есть; это не мастер первоначальной регистрации и не trip-selfie. |
| 13 | Частично; D6 | Фото багажника предусмотрено; окончательная шкала вместимости и интеграция в подбор не приняты. |
| 14 | Код; E2E блокирован | Backend monthly due/grace 24h/eligibility и Driver photo-control UI реализованы. Нужны private R2, cron/deployment и сквозные проверки. |
| 15 | Код/частично | Серверные внеплановые проверки и ограничения есть; отдельного Admin UI и приёмки обоих режимов на тестовой среде нет. |
| 16 | Частично | Backend activeVehicleId/off_line/active-assignment guards есть; выбора автомобиля и смены в Driver UI нет. |
| 17 | Частично | Серверный допуск availability реализован. Нижний slider «На линии» / «Радар активен» и привязка к server state отсутствуют. |
| 18 | Частично | Назначение driver+vehicle и assigned-only Driver list есть; self-take не используется. Нужен отдельный Admin UI назначения и E2E. |
| 19 | Отсутствует; D6 | Серверные guards назначения не являются рекомендательной системой. Нужны утверждённые правила, объяснимый список кандидатов и UI. |
| 20 | Не завершено; D6 | Полные фильтры обязательных/предпочтительных пожеланий в пользовательском подборе не подтверждены; требуется отдельная приёмка. |
| 21 | Частично; D4 | Backend phases и запрет движения без verified есть; Driver phase API/UI отсутствует. Terminal mutations реализованы отдельно и не заменяют phase flow. |
| 22 | Несоответствие | openRoute всегда передаёт pickup → destination в Yandex Maps. Требуются текущая позиция → pickup/destination по server phase и приоритет Навигатор/Карты. |
| 23 | Отсутствует; D7 | Нет полного GPS foreground-service/ingestion/assignment revoke flow. geo.js не является такой реализацией. |
| 24 | Частично | ACTION_DIAL есть; server assignment access проверяется. Нужна сквозная проверка отзыва доступа при reassign и смене состояния. |
| 25 | Отсутствует; D5 | Passenger ↔ Driver order-chat, его архив и 30 календарных дней не реализованы как полный сценарий. Support chat — другой контур. |
| 26 | Отсутствует; D5 | Нет полноценного VoIP/signaling/TURN/записи/архива. Таблица calls не закрывает требование; сроки и правовые условия требуют решения. |
| 27 | Частично | Driver support cases/messages и backend admin lifecycle/retention есть; Admin-приложение, все каналы связи и реальный E2E не завершены. |
| 28 | Код/первый срез G2 | Добавлен отдельный :admin, Java + XML, ru.transferservis.admin. Реализованы staff login, собственный server profile и read-only очередь dispatcher; полный набор разделов Admin не завершён. |
| 29 | Частично; MFA не утверждена | Server OWNER binding есть. Новый Admin показывает только собственный OWNER profile, operational UI закрыт до MFA. TOTP предложение подготовлено отдельно; серверная MFA ещё не внедрена. |
| 30 | Частично | Серверные granular permissions и Admin read-only очередь dispatcher с drivers.read реализованы. Управление сотрудниками, остальные разделы и приёмка полного набора прав впереди. |
| 31 | Частично | Backend queue/review/item moderation и Admin read-only очередь submitted/under_review/changes_required есть. Модерация/возврат элементов, остальные статусы и исправления Driver UI не завершены. |
| 32 | Частично | Backend статусы/модерация/сброс соответствующих одобрений есть; Driver/Admin UX и E2E не приняты. |
| 33 | Частично | Assignment revision, reassign, audit и отзыв серверного доступа есть. GPS/order-chat/новая trip-selfie цепочка ещё не завершены. |
| 34 | Частично | Реализованные mutations/доступ к private resources аудируются. Нет полного Admin audit/архивного UX и всех будущих событий GPS/VoIP/identity. |
| 35 | Частично | HTTPS/origin locks, Keystore AES-GCM AAD v2, private uploads, PII-free push/log guards есть. Полный будущий data lifecycle и E2E не проверены. |
| 36 | Код; E2E блокирован | Firebase identity обменивается на access JWT + rotating refresh; token types разделены. Подтвердить живой вход/refresh/logout. |
| 37 | Соблюдается в проверенных PR | Feature branches, Draft PR #6/#28, merge отсутствует. Production release/deploy не выполняются без отдельного решения владельца. |
| 38 | Частично | Продуктовые решения не отменены. Полное выполнение зависит от незавершённых разделов выше; сводный список не означает их приёмку. |
| 39 | Частично утверждено | D1–D3 утверждены 2026-11-07; D4/D5/D6/D7 ожидают отдельных решений. Конкретизация не должна изменять ТЗ. |
| 40 | Подтверждено | ТЗ утверждено 2026-11-06. Git blob 7434dc0b1548b4f2c5382f12d6cc5399f93d0a84 совпал; исходный текст не менялся. |

Основания: Android settings.gradle.kts, интерфейсы StaffApi/OrderApi/DriverV1Api,
пакеты ui/login, ui/main, ui/details, ui/photo, ui/support; backend router.js,
core/driverRegistration*, core/admin*, core/orderAssignment*, core/driverAvailability*,
core/driverTripOperational*, core/vehiclePhotoControl*, core/supportCaseLifecycle*,
index.js и migrations 0005–0017. Наличие сущности либо теста не считается готовностью UI/E2E.

## Следующие этапы и критерии завершения

| Этап | Исполнитель / зависимость | Результат, необходимый для завершения |
|---|---|---|
| G0. Документы | Разработка; этот аудит | Единый текущий план, матрица 1–40, исправленная навигация по историческим отчётам и журналам. Выполнено этим checkpoint. |
| G1. D3 STAGING E2E | Владелец: подпись/Play/App Check/R2; разработка: preflight и проверки | Настоящий вход → staff session → photo upload → refresh → support/history, проверки чужих ресурсов/replay/отзыва прав и очистка тестовых данных. Только тестовые материалы. |
| G2. Driver + отдельный Admin | Разработка в рамках D1–D3; реальные чувствительные данные зависят от D5 | Driver registration/vehicle/availability UI; Admin Java + XML со своими package/app data/Keystore/session/signing, OWNER/DISPATCHER, очередь модерации и назначения. Вход OWNER и усиление MFA — первый Admin этап по D1. |
| G3. Identity и поездка | Владелец: отдельные D4/D5; затем разработка | Реальный provider result, capture-only, anti-replay, assignment binding, manual review/security event, phase UI/API; без verified движение остаётся запрещено. Исправить навигацию по ТЗ §22 и проверить обе цели. |
| G4. Оставшиеся разделы | Владелец: D5/D6/D7 по соответствующим функциям; разработка | Рекомендации/пожелания/таймауты, GPS, passenger chat/архив, VoIP/запись/архив, support calls и полный audit. До реализации подтвердить относящиеся к функции правила хранения/поставщика. |
| G5. Приёмка и release | Разработка; отдельное решение владельца о merge/deploy | Полный lint обеих Android apps; актуальные dependency inventories/advisories для debug/staging/release; signed artifacts/provenance; реальные устройства и E2E всех обязательных сценариев; решение по PR и отдельно production deploy. |

G2 не требует ожидания выбора liveness SDK для нечувствительных экранов и контрактов.
Он не разрешает подделывать verified, собирать реальные документы без D5 или
считать Admin готовым после создания пустого модуля. Дополнительный staging buildType
используется для конкретного изолированного теста D3, а не отдельной продуктовой ветки.

## Участие владельца сейчас

1. Создать upload key у себя по [инструкции подписи](RELEASE_SIGNING_OWNER_SETUP.md).
   Ключ .p12 и пароли остаются у владельца; публичный сертификат можно передать отдельно.
2. Подтвердить Play Console/внутренний канал, app signing SHA-1/SHA-256 и App Check
   для выбранных Google Play + direct APK каналов. Backend verification не отключается.
3. Создать/подтвердить private R2 bucket transfer-servis52-driver-private-staging
   либо добавить нужные права credential; секреты передаются только защищённым способом.
4. Отдельно согласовать D4/D5/D6/D7 перед соответствующими этапами. Аудит не является согласием.

SDK config и Firebase identifiers уже получены — повторно просить их сейчас не требуется.
По последнему ответу владельца App Check/ключ/R2 не готовы; изменений после ответа
не подтверждено. В этом аудите удалённая инфраструктура не перепроверялась.
Миграции 0005–0017 уже применялись к STAGING 2026-11-07: перед возможным новым apply
проверяется фактический pending set; повторный apply не запускается по старым заметкам.


## G2: liftback только Комфорт — проверенный checkpoint 2026-11-19

Утверждённое правило реализовано в backend/SQL и Android Admin, без массового
изменения versions/pins. Draft PR45/21: backend CI44359299631 и Android
CI44488353051 SUCCESS.432 JVM/145 native,обе apps3 variants build/lint,
security8/5,runner6/readiness15,56 PNG hashes/8 raw changed frames/debug APK
проверены. Прежние failures и точные источники сохранены в журнале и
[receipt](https://github.com/lobanov0710-blip/TransferDriver/blob/arch/g2-comfort-liftback/docs/COMFORT_LIFTBACK_VERIFICATION_2026-11-19.json). Это завершает проверку реализации поправки кузова.

Full classifier readiness/activation не завершены. Следующий прежний объём:
покрытие121 семейства по годам/поколениям/рынкам и защищённая actual legacy
inventory, затем конкретный план обработки пробелов для рассмотрения.
G1paused,D4–D7pending. Merge/deploy/remote migration/seed/activation отдельно.


## Admin phone QA: вариант A утверждён — 2026-11-19

Владелец утвердил отдельный ru.transferservis.admin.qa / Transfer Admin QA,
STAGING, debuggable=false,постоянный приватный QA key на своём Windows.
Подготовить unsigned artifact/инструкцию и проверить coexistence,разные UID/dataDir,
обычное обновление QA тем же ключом и отказ чужой подписи. Телефонный debug APK
с одноразовой CI-подписью больше не выдаётся как обновление. Старый Admin/данные
сохраняются. Физическая приёмка и приватная подпись требуют участия владельца;
они не считаются выполненными synthetic CI. G1paused,phone scope без расширения.


## Admin phone QA: CI/upgrade checkpoint — 2026-11-19

Утверждённый A реализован в Draft PR22,source088c8053/CI44508544855 SUCCESS.
525 JVM/145 native,7variants build/lint и real emulator QA installation checks:
coexistence/distinct UID+dataDir/nondebug/same-key2→3/foreign-key rejection/
original Admin marker preserved PASS. Unsigned QA archive проверен offline;
[receipt](ADMIN_PHONE_QA_VERIFICATION_2026-11-19.json),
[инструкция Windows](ADMIN_QA_SIGNING_WINDOWS.md). Следующий зависимый шаг —
приватное создание/сохранение QA key у владельца,локальная подпись и физическая
установка/вход. Существующий OWNER подходит;нового аккаунта не создано,его
роль/схема доступа требуют уточнения владельца. G1paused,phone scope прежний,
нет classifier activation/merge/deploy/remote writes.


## G2: dated ranges / orphan-safe inventory — 2026-11-19

Backend Draft PR46 source1882d622 / CI44518440499 SUCCESS:orphan/NULL ссылки
теперь входят в проблемные счётчики закрытого inventory.16inventory/6reference/
25migrations/80npm/16native D1/6protected CLI/audit0/dry-run PASS.
Уточнены10семейств по18первичным источникам/20bounded observations;каталог121/
архив116/годы сохранены,remaining111 ranges и exhaustive0 остаются открытыми.
Владелец подтвердил отсутствие snapshot и поручил подготовить порядок для
утверждения. Подготовлено предложение A:18aggregate counts внутри STAGING D1,
закрытый JSON на Windows/public receipt без чисел;5фиксированных SQL проверены
на synthetic schemas. Remote CLI/чтение/экспорт ещё не выполняются до решения.
[Checkpoint](G2_CLASSIFIER_RANGE_INVENTORY_2026-11-19.md). Далее — решение по
закрытому чтению,оставшиеся range proofs,затем план устранения пробелов;
activation/seed/migrations/merge/deploy отдельно. G1paused,D4–D7pending.


## G2: вариант A закрытой STAGING инвентаризации утверждён — 2026-11-19

OWNER: «Утверждают вариант А». Разрешён private Windows CLI: точные STAGING
метаданные и один SELECT с18агрегатами; закрытый JSON, public receipt без чисел.
Full SQLite export не включён. Реальное чтение ещё не выполнено. Реализация
проверяется в отдельном Draft backend PR47; пакет не выдаётся до результатов CI.
G1paused,D4–D7pending; нет remote writes/activation/migrations/merge/deploy.


## G2: private inventory вариант A — проверен и готов владельцу — 2026-11-19

Backend Draft PR47, source6df1c9f / CI44607369124 Linux+Windows SUCCESS.
21 actual-Wrangler CLI / 9 Windows policy+NTFS ACL cases,81npm scripts и
остальные обязательные backend gates PASS. ZIP 16files/15hashes сверён с
CI digest и независимыми Windows/local builds. Ошибки SDK JSON/PSModulePath/
PTY harness исправлены и отражены в журнале и квитанции.
[Пакет и порядок запуска](G2_PRIVATE_INVENTORY_HANDOFF_2026-11-19.md),
[проверки](PRIVATE_VEHICLE_INVENTORY_VERIFICATION_2026-11-19.json).
Следующий зависимый шаг — приватный запуск владельцем на Windows с
существующей авторизацией Cloudflare; передать только receipt.public.json.
Actual data ещё не читались. Full classifier readiness и111range followups
остаются открытыми. G1paused,D4–D7pending; без remote writes/migrations/
activation/merge/deploy и изменения APK/аккаунтов/ключей/прав.
