# План реализации ТЗ Transfer Driver v1.0

> **Главное правило:** только утверждённые правила, ТЗ и решения журнала; самовольные
> отклонения запрещены. Неутверждённые варианты предлагаются владельцу без выбора за него.
> [Обязательное правило работы](../AGENTS.md).

Актуализация: 2026-11-16. Основание — [утверждённое ТЗ](TRANSFER_DRIVER_TECHNICAL_SPEC_V1_0.md),
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
| 16 | Код UI/backend проверен на synthetic API35 | Выбор approved активного автомобиля и on_line/off_line реализованы в PR#9; смена закрыта на линии. CI43779524059 и последующие regression runs PASS. Live Driver E2E остаётся на паузе G1. |
| 17 | Код UI/backend проверен на synthetic API35 | PR #9: нижний slider, выбор approved авто, authoritative radar/offline, revision/idempotency/lifecycle. Sourcee3c6f0c/CI43779524059:246 JVM+67 runtime PASS; scoring ship только для исправленного контраста по четырём captures. Live Driver E2E ожидает G1. |
| 18 | Частично | Назначение driver+vehicle и assigned-only Driver list есть; self-take не используется. Native Admin UI назначения реализован в Draft PR#10; synthetic Activity/contract checkpoint проверен CI43877768185. Live E2E отдельно. |
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


Проверенный code checkpoint первого среза: source16cdc03e6e7b2830c88a9bda19ee370540d1dfb5,
CI43742661189 SUCCESS:231 JVM +56 API35 runtime, full lint6 suites PASS
(Driver0errors/54 прежних warnings; новые формы0issues, Admin0issues).
После native review исправлены контраст system bars и helper количества мест;
scoring verdict ship относится только к перечисленным исправлениям.
[Verification receipt](DRIVER_REGISTRATION_VERIFICATION_2026-11-15.json) и
[UI review](DRIVER_REGISTRATION_UI_REVIEW_2026-11-15.md) сохраняют ограничения.
Это завершение проверенного первого среза, не всего G2/мастера и не live E2E.
Следующая утверждённая часть — availability/manual assignment, G1 на паузе.


## Следующий срез G2: Driver availability и контракт Admin assignment

Начат в отдельной arch/g2-availability-assignment (Draft PR#9), без возобновления
G1. Driver GET me/PUT availability уже предусмотрены D3: нижний slider и active
vehicle, no optimistic on_line, independent revision, idempotency и lifecycle
binding. На момент начала среза runtime/visual ожидались; прежний checkpoint остаётся
историческим результатом своего source.

Admin read API предложен, не утверждён:
[версия2, варианты A/B](ADMIN_ASSIGNMENT_READ_API_PROPOSAL_2026-11-15.md).
OWNER/DISPATCHER сохраняют auth category admin; первоначальное противоположное
утверждение исправлено в анализе и журнале. Зависимые endpoints не реализуются
без явного решения владельца. Синтетические проверки продолжаются независимо.


## Проверенный availability checkpoint — 2026-11-16

Source e3c6f0c3bde6ac77c5527e4e69c7f65fecd07e69, CI43779524059 SUCCESS:
246 JVM (48×3 Driver,34×3 Admin),67 API35 runtime (38/29), full6 lint/builds,
security/endpoint/AppCheck/signing/UID checks PASS. По actual native recaptures
same reviewer оценил исходное замечание system-bar contrast RESOLVED, disposition
ship только для этого исправления. Первый scoring был partial и сохранён в истории.
[Receipt](DRIVER_AVAILABILITY_VERIFICATION_2026-11-16.json) и
[UI record](DRIVER_AVAILABILITY_UI_REVIEW_2026-11-16.md) содержат provenance/limits.

Этот checkpoint закрывает synthetic component/layout проверку availability,
не live Driver E2E/physical device/full Main auth/orders routing и не весь G2.
G1paused, D4–D7pending. Следующий зависимый Admin manual assignment требует
явного выбора A/B из version2 read API proposal; общий «Продолжаем» не заменяет
этот выбор по AGENTS.md п.3. Новые endpoints/permissions/whitelist не включены.
Исторические54 Driver lint warnings не объявляются устранёнными; полный CI lint
PASS не выдаётся за новый warning count. Merge и production deploy отдельно.


## Admin assignment read API A — утверждён и проверен 2026-11-16

Владелец явно выбрал A: «Вариант A утверждаю». Предыдущие записи о pending A/B
являются историческими. Backend [Draft PR#30](https://github.com/lobanov0710-blip/uber-v3/pull/30),
source dda85d2937f385cc7b0cccbc88d4282d50481485 / CI43854087967 SUCCESS:
68 unit/router scripts,15 новых actual SQLite/HTTP cases,18 migrations,12 staging
preflight cases,5 native Worker/D1 checks, protected CLI и Worker dry-run bundle.
Новые GET orders/options, keyset pagination, minimal projection, current grants,
OWNER MFA, PIIfree read audit и active-account guard в atomic mutations готовы
в коде. API не является рейтингом/автоназначением или полной scheduling проверкой.

Следующий плановый срез — Android Admin UI ручного assign/reassign поверх
этого контракта: выбор заказа/связки, обязательная причина, подтверждение
человеком, revision/idempotency и обработка неопределённого результата. Этот UI
данным backend checkpoint не объявляется готовым. G1paused/D4–D7pending,
ADMIN_PHONE_TEST_ONLY whitelist неизменён. Merge/deploy отдельно.

## Native Admin manual assignment — проверенный срез, 2026-11-16

[Draft PR#10](https://github.com/lobanov0710-blip/TransferDriver/pull/10)
реализует следующий разрешённый срез после backend A: order list/options,
явные order+driver/vehicle, обязательную причину/подтверждение, reassign,
order revision, неизменяемый intent/key при явном повторе и session guards.
Одинаковые маршруты различаются полным orderId. OnStop очищает operational
state; owner_phone scope не получает entry/reads/mutations.

Source86ae9b46fe6b7918eb28cf00b822a34f26f3b52c,
CI43877768185 SUCCESS:291 JVM executions (48×3 Driver/49×3 Admin),84 API35
native (38/46),6 build/lint, bundles/debug test APKs, security/endpoint/AppCheck/
signing/UID gates.10 actual Admin PNG light/dark1.3 проверены после Material3
correction; тот же reviewer закрыл единственное замечание RESOLVED, disposition
ship только для scored confirmation correction. Receipt/UI record сохраняют
историю первоначального FIX и границы evidence.

Это synthetic Activity/component checkpoint, не live backend/hardware/TalkBack/
tablet приёмка и не закрытие всего G2. G1paused, D4–D7pending; whitelist
ADMIN_PHONE_TEST_ONLY не расширён. Backend только journal-sync, без нового кода.
Merge/deploy и расширение телефонного operational scope отдельно утверждаются.
CI debug APK имеет ephemeral CI identity; совместимость обновления телефонного
приложения не проверена. Исторические Driver lint warnings не объявлены устранёнными.

## Следующий выбор внутри G2 — подготовлен, не утверждён 2026-11-16

После manual assignment выполнена сверка оставшихся Admin функций с D3.
[Предложение фотоконтроля](G2_PHOTO_CONTROL_NEXT_SLICE_PROPOSAL_2026-11-16.md)
содержит два конкретных объёма: каталог автомобилей/список проверок с двумя
новыми minimal GET либо ограниченное создание из существующей карточки
регистрации. Рекомендуется первый, но владелец его ещё не выбрал. Прежнее
утверждение read API A назначений не распространяется на эти новые GET.

Существующие create/read/review photo-check endpoints проверены чтением кода;
две actual SQLite/service suites перепроверены локально:5+6=11 PASS, из них2
support regressions. [Readiness receipt](G2_PHOTO_CONTROL_READINESS_2026-11-16.json)
фиксирует source hashes и пределы: не Android/native Worker/HTTP E2E.
Исполняемый код и действующий телефонный scope не изменены. Реализация
зависит от явного выбора владельца по AGENTS.md п.3; G3/D4/D5 и G1 не запущены.
Строка16 матрицы исправлена по уже подтверждённому PR#9: Driver выбор активного
автомобиля существует; это исправление документации, не новая функция.

## Справочник марок — подготовлен для утверждения, 2026-11-16

По отдельному запросу владельца добавлены proposal Markdown/JSON на140 марок,
включая Haval/Chery:64 в расширенном китайском блоке,14 региональных/исторических,
18 японских/корейских,29 европейских и15 американских. [Полный список](VEHICLE_MAKES_CATALOG_PROPOSAL_2026-11-16.md).
Проверены140 уникальных названий/ID,5 групп,188 поисковых значений и совпадение
Markdown/JSON; alias Cherry ведёт к предлагаемому Chery. Это не тесты приложения.
Текущая форма/API принимает make/model свободным текстом1–80; whitelist не вводился.
Полный состав, модели и внедрение не утверждены, новые GET фотоконтроля также
остаются pending. Включение марки не даёт approved/тариф/приоритет/допуск.

## Решение владельца: состав140 марок утверждён, 2026-11-16

Явный ответ на предложение списка: «Состав справочника утверждают.».
Утверждён полный состав140 марок из опубликованного b5fa627ab31f5c8da65788d60e788f9ea1d824c5,
включая Haval/Chery; предыдущая запись pending состава теперь историческая.
Markdown/JSON получили approved-composition и запись решения, без изменения
самих140 строк,5 групп или предложенных поисковых вариантов.

Это утверждение состава справочника. Внедрение в UI/сервер, alias/search поведение,
модели, миграция существующих данных и выбор объёма/новых GET фотоконтроля
не утверждались этим сообщением. G1paused/D4–D7pending; merge/deploy не выполняются.

## Эконом исключён; поиск модельного классификатора — 2026-11-16

Владелец подтвердил отсутствие тарифа Эконом и поручил найти модельную таблицу
в репозитории. Проверены текущие снимки60 веток (14Android/46backend),821
уникальное текстовое содержимое и доступная локальная история751 commits.
Утверждённая модельная таблица не найдена; найденный econom — negative tests.
[Отчёт/пределы](VEHICLE_TARIFF_CLASSIFICATION_SEARCH_2026-11-16.md) и JSON receipt.

В коде/БД/выборе Android уже только comfort/business/minivan. Прямая проверка
текущих функций отвергает4 написания Эконома; pricing suite27 checks и
driverRegistrationService suite PASS. Это не проверка класса конкретной модели.
Справочник140 марок получил явный tariff scope: Эконом исключён, применение
как списка всех допущенных автомобилей запрещено. Сами марки не удалялись
без исходной модельной таблицы; будущий список допуска моделей требует
согласования. Код/существующие записи/whitelist не менялись, новый APK не собирался.

## Решение владельца: 15 китайских модельных правил утверждены — 2026-11-16

После предоставления CSV на116 строк подготовлено дополнение:6 Комфорт и9 Бизнес.
Владелец выбрал единый минимальный год2023, затем ответил «Утверждаю» на прямой
вопрос об этих15 дополнениях и предложенных условиях. [Полные правила](https://github.com/lobanov0710-blip/uber-v3/blob/docs/vehicle-classifier-v1-20261116/docs/VEHICLE_CLASSIFIER_CHINA_EXTENSION_PROPOSAL_2026-11-16.md)
и [CSV v2](https://github.com/lobanov0710-blip/uber-v3/blob/docs/vehicle-classifier-v1-20261116/docs/vehicle-classifier-v2-proposed.csv)
сохранены в Draft PR31:15 approved/116 pending, всего131 строка.

Утверждены только15 новых правил, порог и перечисленные условия; все15 марок
уже были в справочнике140. Условия проверки посадки/второго ряда/комплектации/
готовности к маршруту и отдельного согласования кузова Avatr12/Zeekr001 сохранены.
Автомобили не одобряются автоматически по статусу модельного правила.
Исходные116, Belgee/TENET и прежние обозначения марок требуют своего решения.
Классификатор не подключён к runtime, новые GET фотоконтроля не утверждены.

18 проверок после утверждения PASS: точная область15 статусов, сохранение116,
состав/годы/условия, CSV/JSON parity, структурные и формульные проверки.
Это проверки документов; APK/runtime/live E2E не выполнялись. G1paused,
D4–D7pending, merge/deploy отсутствуют. Повторное утверждение новых15 не требуется.

## Решение владельца: исходные116 и Belgee/TENET утверждены — 2026-11-16

Прямое поручение: «Исходные 116 строк и добавление Belgee/TENET добавить
и утвердить». Утверждены116 исходных связок тариф/марка/модель/год/примечание
в их представленном виде. Добавлены только Belgee/TENET; справочник теперь142
марки, старые140 записи/aliases неизменны. Вместе с прежними15 дополнениями
в актуальном CSV v2 **131 approved/0 pending**,76 Комфорт/31 Бизнес/24 Минивэн.
Порог2023 относится к15 дополнениям; исходные116 годы не заменялись этим порогом.

CSV v1 — неизменяемый архив входного вложения; текущее утверждение применяется
в v2. Условия всех131 правил сохранены, включая отдельное согласование кузова
Avatr12/Zeekr001. Исходные написания утверждены как представлены; автоматической
нормализации, объединения FAW/Bestune или миграций не выполнялось.

21 проверка полного CSV и14 проверок справочника прошли, включая точные116
status transitions, сохранение полей/13 примечаний/15 прежних строк,142 марки,
группы/уникальность/190 поисковых значений, Markdown/JSON/CSV parity и hashes.
Это утверждение документов, не рабочий допуск конкретных машин. Runtime import,
новые GET фотоконтроля и merge/deploy не выполнялись; G1paused/D4–D7pending.
Повторное утверждение этих131 правил и142 марок не требуется.


## Возврат к следующему срезу G2 после утверждения131/142 — 2026-11-16

Состав классификатора и справочника утверждён полностью, повтор не требуется.
Следующий подготовленный срез — внеплановый фотоконтроль Admin. Владелец
ещё не выбрал между «Каталог и список проверок» (два новых GET) и действием
из карточки регистрации (без новых GET). Рекомендуемый контракт/UX подготовлены
в G2_PHOTO_CONTROL_NEXT_SLICE_PROPOSAL_2026-11-16.md; общий запрос продолжать
не используется как самостоятельный выбор неутверждённого варианта по AGENTS п.3.
Справочник моделей не заменяет каталог конкретных автомобилей с ID/revision.

Независимая перепроверка существующего backend выполнена:11 SQLite/service
тестов PASS (5+6, включая2 support), семь source hashes неизменны. Readiness
receipt обновлён с сохранением истории. Это не новые GET/Android/HTTP/MFA E2E
и не полная проверка обоих режимов на активной поездке. Исполняемый код
не менялся; G1/D4–D7/OWNER-only scope остаются в прежних границах.


## G2 фотоконтроль: вариант утверждён, реализация начата — 2026-11-16

Ответ «Утверждают каталог и список» выбирает полный вариант с двумя GET,
метаданными, scoped pagination и явным назначением через существующий POST.
Рабочие ветки arch/g2-admin-photo-control-read (backend) и
arch/g2-admin-photo-control-ui (Android). После API и тестов — Admin UI,
JVM/native проверки и build/lint, затем независимый UI finish review/evidence.
Предыдущие pending записи исторические; повтор выбора не требуется.
G1paused/D4–D7pending, OWNER-only phone scope сохраняется. Merge/deploy отдельно.


## G2 каталог и список фотопроверок: проверенный component/synthetic срез — 2026-11-16

Утверждение «Утверждают каталог и список» реализовано в Draft PR12 (Android)
и PR32 (backend): безопасные metadata GET, каталог/история/явное создание
manual/complaint с подтверждением режима и причины, актуальными правами/MFA,
контролем ревизий и сессии, без автоматического повторения mutation.

Backend code080d4b49bb902d782efe2615d92bf05fa3d64dc6,
tree245d64159221583438201d2d51804064d8d45a60,
CI43947136962/job133579673172 SUCCESS:69 npm test scripts,
12 новых SQLite/HTTP cases,18 migrations/12 preflight,6 native Worker/D1
workflows,5 protected CLI scenarios, dependency audit0 и bundle dry-run PASS.
Исправлен воспроизведённый прежний immediate bypass перехода активной поездки;
atomic SQL также проверен при race. after_current_trip сохраняет текущую поездку.

Android codea0308bec394cd28d821f52bbcfdc05e5e09f4be7,
treed6de5082865d0932c2e754bffb2fd78dc179b951,
CI43950844261/job133592621453 SUCCESS:318 JVM executions (48×3 Driver,
58×3 Admin),99 native cases (38 Driver/61 Admin),6 build/lint variants и
security/config/variant gates PASS. Новые9 JVM cases ×3 и15 native cases.
Предыдущие Android runs43948185690/43949225817 отменены после исправлений,
не считаются финальным PASS. Локальные sandbox socket/spawn EPERM ограничения
не обходились; полноценные native/CLI проверки выполнены штатным CI.

Артефакты точного Android source загружены, ZIP CRC/path/size и GitHub SHA256
проверены: native artifact14267604781 содержит28 PNG, APK artifact14270860090.
Все10 новых native кадров просмотрены независимыми documenter/reviewer,
их SHA256 независимо совпали с CI logs. Светлая тема и dark/font1.3 читаемы;
полные режимы перенесены, подтверждение содержит автомобиль/основание/режим/
причину/действия. Reviewer: SHIP только для утверждённого component/synthetic
среза, подтверждённых blockers нет. Spinner finding закрыт кодом/кадрами;
MFA UX finding закрыт guard/очисткой и выполненным native regression.
Сокращение toolbar title при font1.3 не скрывает операционную информацию.

Receipt ADMIN_PHOTO_CONTROL_VERIFICATION_2026-11-16.json фиксирует проверенный
code SHA/tree отдельно от последующих docs-only commits. Исторические pending
записи и readiness11 не являются актуальным статусом реализации.
G1paused/D4–D7pending; OWNER-only operational whitelist не расширен (404/hidden).
Нет merge/deploy/remote migrations/реальных материалов/R2/биометрии/live phone
E2E или полного закрытия G2. CI debug подпись временная; обновление уже
установленного телефонного APK не проверялось. Прежние Driver lint warnings
не объявлены исправленными. Утверждённые131 правила/142 марки не менялись и
не активировались в runtime. Следующий зависимый шаг не выбирается за владельца.


## Системный класс и version store — 2026-11-17

Владелец утвердил системный выбор класса, ввод года водителем и подтверждение
по документам, ручную проверку неизвестной модели/неподходящего года;
администратор может внести параметры, добавить модель и изменить правило.
Изменённые правила применяются только к новым регистрациям; при отдельном
изменении марки/модели/года автомобиля используется текущая версия, автомобиль
проходит проверку без допуска на линию. В начатой поездке — безопасная
остановка, сообщение диспетчеру местоположения, замена только вручную
диспетчером, информирование пассажира. Существующие автомобили без
подтверждённого года на линию не допускаются. Невыполненное условие устранить,
если возможно, и рассмотреть исключение. Комфорт — седан/кроссовер; Бизнес —
седан с явным исключением Li Auto L7/L9. Полностью электрические исполнения
исключаются, тип проверяется по документам; полный обзор версий ещё не завершён.

Backend Draft PR33 добавляет изолированный evaluator и внутренний version
store с миграцией0019 без seed. Сохраняются неизменяемые версии и результаты
для ревизии автомобиля; текущая версия при новой ревизии и конфликты гонок.
Не подключены регистрация/год/серверная модерация/права каталога/линия/поездки/
Android. Candidate не является допуском. Локально7 evaluator+12 actual SQLite+
6 reference tests PASS,19 migration smoke и12 D3 preflight PASS. Первый
CI44051103873 прошёл эти проверки и native D1, но остановился на старом OWNER
phone fixture с18-migration цепочкой. Production guard сохранён; fixture
дополнен негативной проверкой отказа при19, новый CI44052040564 проверяет
sourceaef9dfc77dc861a46e97792a27773cb4c5f6542b. Окончательный статус CI
нужно читать из актуального receipt; этот checkpoint не утверждает PASS.
G1paused, D4–D7pending; merge/deploy/remote migration/реальные документы отсутствуют.
