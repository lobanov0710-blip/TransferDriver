# План реализации ТЗ Transfer Driver v1.0

> **Главное правило:** только утверждённые правила, ТЗ и решения журнала; самовольные
> отклонения запрещены. Неутверждённые варианты предлагаются владельцу без выбора за него.
> [Обязательное правило работы](../AGENTS.md).

Актуализация: 2026-11-15. Основание — [утверждённое ТЗ](TRANSFER_DRIVER_TECHNICAL_SPEC_V1_0.md),
[решения D1–D7](TECHNICAL_DECISIONS_V1_0_PROPOSED.md), журналы и фактический код.
Это сводный рабочий план существующего объёма, а не изменение ТЗ или новое утверждение.
D1–D3 утверждены; D4–D7 остаются неутверждёнными.

**Общий статус: частичная реализация; к production-выпуску не готово.**
Процент готовности не вычисляется: таблицы БД, API, UI и принятый E2E имеют разный вес.
Успешный CI не закрывает незавершённый пользовательский сценарий.

## Решение владельца 2026-11-14

G1 приостановлен. Владелец явно поручил начать G2. Рабочая ветка Admin —
[arch/g2-transfer-admin](https://github.com/lobanov0710-blip/TransferDriver/tree/arch/g2-transfer-admin). Активная работа — отдельный
Transfer Admin по D1–D3. Параметры/API TOTP утверждены 2026-11-14, реализованы и
проверены; 2026-11-15 владелец подтвердил подключение нового TOTP и вход на телефоне.
Это приёмка OWNER phone login по сообщению владельца, не полного G1/G2.
Далее выполняется ранее утверждённый Driver registration/vehicles в отдельной
[ветке arch/g2-driver-registration](https://github.com/lobanov0710-blip/TransferDriver/tree/arch/g2-driver-registration),
[Draft PR #8](https://github.com/lobanov0710-blip/TransferDriver/pull/8).
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
| 3 | Частично | Backend draft/update/submit и completeness есть; первый UI срез личных данных/автомобилей добавлен в PR #8, полный мастер ещё не готов. Реальные документы/биометрия требуют D5. |
| 4 | Частично | Java/XML UI личных данных/read-only телефона и серверных статусов добавлен в G2 PR #8. Синтетические проверки не заменяют Driver live E2E; причины возврата не входят в GET me. |
| 5 | Частично; D4/D5 | Типы passport_selfie и identity_reference_selfie, private upload/readiness предусмотрены. Полный capture/identity/consent flow не принят. |
| 6 | Блокировано D4/D5 | Серверная модель есть; выбранного и интегрированного face/liveness provider нет. Административный review не заменяет автоматическую проверку. |
| 7 | Частично; D4/D5 | Challenge и SQL binding/state guard есть; пути получения настоящего verified и trip-selfie UI нет. Галерея и локальный успех не допускаются. |
| 8 | Частично | Backend учитывает обе стороны ВУ; нет полного Driver upload/edit/review UX и E2E. |
| 9 | Частично | Добавлен Driver UI списка/создания/изменения нескольких автомобилей, обязательный VIN и independent revision/server status (PR #8). Документы/история/полная приёмка и live E2E ещё не завершены. |
| 10 | Частично | Основание использования выбирается в Driver; СТС и условный rental_contract проверяются сервером. Их полный capture/upload UX и private-storage E2E не завершены, реальные материалы зависят от D5. |
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
| 28 | Код/первый срез G2 | Добавлен отдельный :admin, Java + XML, ru.transferservis.admin. Реализованы staff login/MFA, server profile и Admin moderation UI с пятью фильтрами, карточкой, поэлементными решениями и историей; финальный CI/runtime этого среза PASS. Полный набор разделов Admin не завершён. |
| 29 | Код MFA проверен; E2E не выполнен | Параметры/API TOTP MFA утверждены. Server challenge/enrollment/TOTP/recovery/evidence и Admin flow проверены локально и в CI/runtime. Deploy/remote migration не выполнялись. Полный OWNER UX остаётся частью G2. |
| 30 | Частично | Серверные granular permissions и Admin read-only очередь dispatcher с drivers.read реализованы. Управление сотрудниками, остальные разделы и приёмка полного набора прав впереди. |
| 31 | Частично | Добавлены пять фильтров очереди, карточка/история, review/возврат элементов и итоговое решение. Admin UI проверен; актуальные source/CI и количества приведены в DRIVER_REGISTRATION_VERIFICATION_2026-11-15.json. Driver исправления и live E2E не завершены; реальная identity verification зависит от D4/D5. |
| 32 | Частично | Backend статусы/модерация/сброс соответствующих одобрений есть; Driver показывает серверный статус каждого автомобиля и редактирует отдельную revision. Полная приёмка Driver/Admin UX и live E2E ещё не выполнена. |
| 33 | Частично | Assignment revision, reassign, audit и отзыв серверного доступа есть. GPS/order-chat/новая trip-selfie цепочка ещё не завершены. |
| 34 | Частично | Реализованные mutations/доступ к private resources аудируются. Нет полного Admin audit/архивного UX и всех будущих событий GPS/VoIP/identity. |
| 35 | Частично | HTTPS/origin locks, Keystore AES-GCM AAD v2, private uploads, PII-free push/log guards есть. Полный будущий data lifecycle и E2E не проверены. |
| 36 | Код; E2E блокирован | Firebase identity обменивается на access JWT + rotating refresh; token types разделены. Подтвердить живой вход/refresh/logout. |
| 37 | Соблюдается в проверенных PR | Feature branches, Draft PR #6/#28 и G2 #7/#29; merge отсутствует. Production release/deploy не выполняются без отдельного решения владельца. |
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

## Решение владельца для следующего шага G2

Подготовлено [предложение TOTP MFA OWNER](ADMIN_MFA_TOTP_PROPOSAL.md).
Параметры и API явно утверждены владельцем 2026-11-14. Server MFA реализована
в arch/g2-owner-totp-mfa; OWNER operational UI открывается только после серверной
проверки MFA, не по одному имени роли.
MFA source проверен: Android c6badd3a / CI 43608123183 — 69 Admin JVM + 20 runtime,
Driver 87 JVM + 12 runtime. Backend 1598dff3 / CI 43608125351 — 65 test scripts,
18 migrations, 12 preflight, native workerd HTTP/D1 и dry-run PASS.
Admin full lint трёх variants — 0 issues. OSV scan 6 runtime configurations,
152 Maven components — 0 опубликованных advisories на момент проверки.
Сводный отчёт: [OWNER_MFA_VERIFICATION_2026-11-15.json](OWNER_MFA_VERIFICATION_2026-11-15.json).
Среда выполнения восстановлена 2026-11-15; полный build receipt и runtime inventory опубликованы отдельными документами.
Это историческая проверка указанного source, не полная приёмка G2. Позднее
OWNER-only STAGING подготовлен на компьютере владельца; новый TOTP и вход
подтверждены им 2026-11-15. Driver live E2E остаётся блокирован G1.
Далее по G2 — оставшийся Driver registration/vehicle/availability UX и
Admin moderation/assignment UX в рамках D1–D3. Ограничение execution environment снято после проверки команд, workspace и сохранности артефактов.

## Отложенные настройки G1 и решения будущих этапов

G1 приостановлен; следующий перечень не является поручением сейчас возобновить его.


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

## Завершённый срез G2 от 2026-11-15

Admin moderation source d469b42c / CI 43704273792 и backend 51d21da7 /
CI 43702881770 — SUCCESS; результаты и ограничения в журнале, provenance в
[ADMIN_MODERATION_BUILD_2026-11-15.json](ADMIN_MODERATION_BUILD_2026-11-15.json).
Следующий шаг принятой рекомендации: Driver анкета/черновик/автомобили/VIN и
исправления по замечаниям Admin; затем availability/manual assignment.
Только разрешённые D1–D3 части и синтетические материалы до D4/D5/G1.


## Уточнённый текущий шаг от 2026-11-15

По поручению владельца перед следующим Driver UI срезом подготовить проверку
Admin/OWNER на STAGING. Приватный control plane — Windows PowerShell владельца;
G1 остаётся на паузе. Сначала проверенный пакет и read-only deployment plan,
затем разбор фактического состояния и отдельное утверждение remote изменений.
TOTP на том же телефоне: RAM-only IDs/исходный TTL, без seed/кодов в фоне,
отмена незавершённых запросов — явно утверждено владельцем.
[Процедура](OWNER_PHONE_STAGING_WINDOWS.md). Это не завершение G1/G2.


## Текущий срез G2 после подтверждения OWNER phone login

Первый Driver срез: profile draft/save и несколько автомобилей/create/edit/VIN,
серверные статусы/версии. Контракт и ограничения —
[DRIVER_REGISTRATION_SCOPE_2026-11-15.md](DRIVER_REGISTRATION_SCOPE_2026-11-15.md).
Далее в утверждённой последовательности — availability/manual assignment.
Срезы полного wizard, причин возврата элементов и загрузок не считаются
завершёнными: действующий GET me не даёт причины, D4/D5 ещё не утверждены.
ADMIN_PHONE_TEST_ONLY не снимается для тестирования Driver UI. Реальный Driver
E2E не выполняется обходом паузы G1; локальные/CI проверки используют синтетику.
Результаты нового source/CI и остающиеся предупреждения фиксируются в журнале
и отдельном verification receipt после фактического выполнения проверок.
