# Журнал проекта Transfer Drivers

## 2026-10-31 — Правило разработки Production / STAGING

Зафиксировано решение владельца проекта:

- Production является основной линией дальнейшей разработки проекта.
- Для Android основная production-база: `TransferDriver/master`.
- Для backend основная production-база: `uber-v3/main`.
- Все новые функции разрабатываются в отдельных feature/arch-ветках, созданных от актуального production-кода.
- Изменения проходят тесты и Pull Request до слияния.
- Merge в `master/main` и production deploy выполняются только после отдельного подтверждения владельца проекта.
- STAGING сохраняется для будущих изолированных интеграционных и E2E-тестов.
- STAGING больше не является самостоятельным направлением разработки.
- Новые функции специально для STAGING не разрабатываются, если только это не требуется для конкретного тестового сценария.
- Существующая staging-инфраструктура и локальный `wrangler.staging.toml` сохраняются и не удаляются.
- Наличие ранее добавленной staging-функциональности в `uber-v3/main` само по себе не является основанием для её дальнейшего развития.
- При разработке production-функций STAGING или Worker Preview могут использоваться только как тестовая среда; продуктовая архитектура должна строиться вокруг production-кода.

### Текущий этап

ARCH-11.16 — уведомления водителя об изменении статуса заказа.

Для backend ветка `arch/11.16-order-status-push` должна быть синхронизирована с актуальным `uber-v3/main` перед продолжением E2E и перед последующим merge.

Для Android ветка `arch/11.16-order-status-receive` остаётся рабочей веткой этапа ARCH-11.16.

Это правило считать базовым для всех последующих этапов Transfer Drivers, пока владелец проекта явно не изменит его.


## 2026-11-05 — Утверждён UX селфи-проверки водителя

Зафиксировано решение владельца проекта по Transfer Driver:

- Экран результата селфи-проверки утверждён по принципу предоставленного референса, но в фирменном стиле Transfer Servis.
- Единый UX используется для первичной идентификации и для обязательной проверки водителя перед каждой поездкой к точке подачи.
- Процесс включает состояния: подготовка к съёмке, съёмка/liveness, проверка, результат.
- Успешный результат отображается только после серверного подтверждения личности.
- Для проверки перед поездкой серверное подтверждение должно быть связано с конкретными `driverId + orderId + vehicleId`.
- Только после успешной проверки разрешается переход к действию «Ехать к пассажиру» / `en_route_to_pickup`.
- При ошибках качества предлагается повторная съёмка; при несовпадении лица начало поездки блокируется и создаётся событие для администратора.
- Загрузка селфи из галереи для проверки перед поездкой не допускается.
- Сборка приложения, merge и production deploy на этом этапе не выполняются.


## 2026-11-06 — Утверждена административная модель OWNER / DISPATCHER

Зафиксированы решения владельца проекта по Transfer Admin:

- Главная административная учётная запись принадлежит владельцу проекта и имеет максимальный уровень доступа: OWNER / SUPERADMIN.
- OWNER имеет абсолютный контроль над заказами, водителями, автомобилями, регистрациями, документами, фотоконтролем, назначениями, пассажирскими заявками, сотрудниками, поддержкой, архивами, GPS, жалобами, тарифами, правилами приоритета, настройками, правами и audit.
- Ни один сотрудник не может изменить права OWNER или отключить его учётную запись.
- При росте количества заявок OWNER может создавать отдельные учётные записи DISPATCHER.
- Основная задача DISPATCHER — операционная обработка заказов, назначение/переназначение водителя и автомобиля, работа с пассажирами, водителями и поддержкой в рамках выданных прав.
- Права DISPATCHER должны управляться через granular permissions, а не только через имя роли.
- Чувствительные документы водителей, архив чатов и записи интернет-звонков недоступны диспетчеру по умолчанию и требуют отдельного разрешения OWNER.
- Все критические действия диспетчера обязательно журналируются в audit.
- DISPATCHER не может удалять или изменять собственные audit-записи.
- OWNER может в любой момент отменить или переопределить операционное решение диспетчера, изменить его права, заблокировать аккаунт и завершить его активные сессии.
- Рекомендательная система может ранжировать подходящих водителей и автомобили, но финальное назначение выполняет OWNER либо уполномоченный DISPATCHER.
- На текущем этапе сборка приложения, merge и production deploy не выполняются.


## 2026-11-06 — Утверждено ТЗ Transfer Driver v1.0

Владелец проекта явно утвердил техническое задание Transfer Driver в текущей редакции.

Зафиксировано:

- создан единый документ `docs/TRANSFER_DRIVER_TECHNICAL_SPEC_V1_0.md`;
- статус документа: **УТВЕРЖДЕНО**;
- утверждённые продуктовые и архитектурные решения считаются базовыми для дальнейшей реализации;
- изменения утверждённых правил требуют отдельного решения владельца и записи в журнале;
- детали реализации, перечисленные в разделе 39 ТЗ, прорабатываются отдельно и не должны менять утверждённую архитектуру без согласования;
- документация находится в отдельной ветке `docs/project-journal-production-policy` и входит в Draft PR #4;
- сборка APK не выполнялась;
- merge не выполнялся;
- production deploy и изменения production не выполнялись.

## 2026-11-06 — Начало реализации по утверждённому ТЗ v1.0

Основание: владелец поручил изучить утверждённое ТЗ, начать сборку приложений,
выполнять проверки и тесты, немедленно анализировать ошибки и утечки,
сразу запрашивать необходимое участие и вести журнал.

### Источники и границы

- ТЗ прочитано полностью, разделы 1–40. Источник: ветка
  `docs/project-journal-production-policy`, commit
  `2bf8a9b3194036ad716341d75f67494493b7c480`, blob ТЗ
  `7434dc0b1548b4f2c5382f12d6cc5399f93d0a84`.
- Android-база: `master`, commit `501481c20168c496e11ea21b7a7e82d047523788`.
- Создана отдельная ветка `arch/spec-v1-security-baseline` от этой базы;
  утверждённые документы скопированы из указанного commit без merge PR #4.
- Backend изучается по `uber-v3/main`;
  tree SHA `7fad96a63ac963e6c28bf03f21d52b90e5cb69ff`.
- PR #3 (ARCH-11.16), #4 (документация) и #5 (диагностика) не сливались.
- Разрешение на сборку получено в текущем поручении; предыдущие записи
  «сборка не выполняется» описывают прошлый этап утверждения.
- Разрешение на merge и production deploy не запрашивалось и не получалось.

### Ответы владельца в текущем сеансе

- Transfer Admin: отдельное Android-приложение, Java + XML.
- Решения раздела 39 ещё не утверждены. Владелец поручил подготовить
  конкретные предложения для утверждения. Серверные контракты и результаты
  identity/liveness не подменяются локальными предположениями.

### Исходная проверка

- В рабочей среде есть OpenJDK 21; проект требует daemon JDK 25,
  Gradle 9.6.0, AGP 9.4.1, Android SDK 37.
- Локальный Android SDK не обнаружен в стандартных каталогах.
- `app/google-services.json` отсутствует в Git; штатного источника Firebase
  в настройках среды не обнаружено. Рабочая Firebase-конфигурация необходима
  для полноценной сборки/проверки интеграции.
- Сетевая политика среды: unrestricted/enforced. Клонирование в стандартной
  песочнице завершилось ошибкой подключения к proxy; повтор со штатным
  дополнительным сетевым разрешением успешен. Proxy и TLS не обходились.
- В проекте имеется только шаблонный unit-тест сложения.
- Обнаружено: `DriverNotificationManager.notifyNewOrders` для одного заказа
  выводит маршрут/адреса пассажира в системное уведомление.
- Базовая серверная авторизация поддерживает `driver/admin`; это ещё не
  OWNER/DISPATCHER с granular permissions из нового ТЗ.

Результаты исправлений, фактически выполненных тестов и препятствий
добавляются ниже по мере проверки. Успешная сборка пока не подтверждена.

### Выполненные исправления и проверки

- Устранён вывод маршрута пассажира в уведомления: только общий текст и количество.
- Единый SecureHttpClient применяется к основному API и refresh-клиенту:
  точный HTTPS origin, TLS 1.2/1.3, запрет redirects и transport retry,
  общий call timeout 30 секунд, без HTTP cache и logging interceptor.
- FLAG_SECURE включён на трёх объявленных экранах; сохранение пароля в View state отключено.
- Manifest явно запрещает cleartext и связывает backup/data extraction rules.
  Из cloud backup и device transfer исключены все 9 доменов app storage.
- Канонический UUID validator используется для orderId из FCM и installationId.
- Добавлены ignore-правила для Firebase config, service accounts, .env и signing keys.
- 10 JUnit-тестов: PASS. Компилировались реальные production Java-классы,
  включая SecureHttpClient, с Java release 11. Сетевые негативные тесты проверяют
  отказ до отправки запроса и отсутствие URL/credentials в тексте ошибки.
- 6 offline security configuration/source guards: PASS.
- `uber-v3`: штатные 26 наборов `npm test` прошли; production API не вызывался.
- `git diff --check` и `bash -n tools/run_jvm_security_tests.sh`: PASS.
- Все 40 разделов сопоставлены с кодом в `IMPLEMENTATION_REVIEW_V1_0.md`.
- Предложения раздела 39 оформлены в `TECHNICAL_DECISIONS_V1_0_PROPOSED.md`;
  статус НЕ УТВЕРЖДЕНО. Подмена server verification локальным результатом не выполнялась.

### Ошибки и ограничения текущего сеанса

- Две подготовленные операции apply_patch не прошли проверку контекста/повторного
  пути; изменения ими не применились. Исправленные операции применены и проверены.
- Первый запуск javac не состоялся: системный Java 21 оказался runtime без javac.
  Ошибки загрузки тестовых классов того запуска не являются результатом тестирования.
  Затем использован установленный штатным Gradle toolchain JDK 25.0.3;
  все 10 тестов успешно скомпилированы и выполнены.
- Wrapper 9.6.0 загружен с проверкой distribution SHA-256 из проекта.
  Java требует явной proxy-настройки. Загрузка plugin сначала не удалась;
  для daemon JDK 25 добавлен штатный системный CA truststore и proxy properties
  в локальный GRADLE_USER_HOME. TLS verification не отключалась.
- Затем AGP не мог писать в read-only /home/agent/.android. Использован
  ANDROID_USER_HOME=/workspace/.android-transferdriver без изменения HOME.
- Последняя попытка `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
  завершилась `SDK location not found`. Эти три Gradle-задачи не выполнены.
  Android SDK и emulator в среде пока не установлены.
- Владелец сообщил, что Firebase-конфигурации пока нет и её нужно создать.
  Переданы конкретные шаги и package ID в предложении D1. Доступа к Google-аккаунту
  нет; Firebase-проект, реальные SMS и временный google-services.json не создавались.
- APK, release-подпись, runtime/E2E, полная CVE-проверка и SBOM не подтверждены.
- Существующее самостоятельное принятие заказа Driver противоречит новому ТЗ:
  требуется согласованная миграция backend+клиента, это блокирует новый выпуск.
- Admin ещё не реализован: платформа утверждена, серверная модель OWNER/permissions
  и API находятся в подготовленном пакете на техническое утверждение владельца.
- Backend-код, production, master/main и существующие PR не изменялись.

### Воспроизведение проверок

- `python3 tools/check_security.py` — стандартная библиотека Python.
- `bash tools/run_jvm_security_tests.sh <каталог jar>` — JDK (javac), JUnit 4.13.2,
  Hamcrest 1.3, OkHttp 3.14.9, Okio 1.17.2 из Maven Central.
  OkHttp соответствует опубликованной зависимости Retrofit 2.12.0.
- Локальные dependency/build logs находятся вне Git, в `/workspace/scratch`.
  В журнал/PR переносятся результаты без credentials и реальных персональных данных.
- Android Gradle требует Android SDK 37, рабочей Firebase-конфигурации и JDK 25;
  параметры proxy/CA задаются локально, не входят в исходный код приложения.


### Сохранение результата для разбора

- Создана удалённая ветка `arch/spec-v1-security-baseline` и Draft PR #6:
  https://github.com/lobanov0710-blip/TransferDriver/pull/6
- Первый commit изменений: `e6111317685ce86a0e0680404ac38ed1663a9d36`.
- В PR сохранены исходники, тесты, утверждённое ТЗ, журнал, полный разбор
  требований и предложения для отдельного технического утверждения.
- Повторный полный запуск backend `npm test` подтвердил exit code 0 и все 26 наборов.
- Перед публикацией 28 текстовых файлов перенесены через подключённый GitHub API;
  build caches, загруженные jar, локальные proxy/CA settings и logs в Git не включались.
- Владельцу передаётся запрос на утверждение D1–D3 и D6–D7; D4 требует испытания
  поставщика, D5 — отдельного правового решения. До этих решений соответствующие
  новые функции не реализуются с предположительными правами/verification.
- Merge, deploy, выпуск рабочего APK и создание Firebase-проекта не выполнены.


## 2026-11-07 — Утверждены технические решения D1–D3

Владелец проекта утвердил D1–D3 из `docs/TECHNICAL_DECISIONS_V1_0_PROPOSED.md`.

Утверждено:

- **D1 — приложения и Firebase:** Driver сохраняет package `ru.transferservis.driver`; Transfer Admin проектируется отдельно на Java + XML с отдельным package/app data/Keystore/session context; Firebase Phone Auth вводится для Driver.
- **D2 — Firebase identity → staff session:** Firebase ID token не используется как staff JWT; backend выполняет exchange и после собственной проверки выдаёт существующую access JWT + rotating refresh session; roles/status остаются server-authoritative.
- **D3 — D1 и API:** утверждена additive-модель новых сущностей `driver_profiles`, `vehicles`, `document_resources`, `driver_availability`, `order_assignments`, `identity_verifications`, `vehicle_photo_checks`, `staff_permissions`, support/chat/calls/audit и базовые группы `/v1` API-контрактов.

Обязательные условия реализации:
- additive migrations;
- deny-by-default authorization;
- mutation с Idempotency-Key и revision/optimistic locking;
- атомарные проверки assignment/permissions;
- audit критических операций;
- негативные contract tests на подмену role/phone/driverId/vehicleId, replay и доступ к чужим ресурсам.

D4–D7 не утверждены и остаются отдельными решениями.

Утверждение D1–D3 разрешает переход к backend Phase 1 в отдельной ветке. Merge и production deploy не разрешались.


## 2026-11-07 — Backend Phase 1A started after D1–D3 approval

После утверждения D1–D3 начата реализация backend foundation в отдельной ветке `uber-v3/arch/spec-v1-backend-phase1`.

Создан Draft PR `uber-v3 #28`:
- additive migration `migrations/0005_driver_platform_core.sql`;
- добавлены базовые сущности: `driver_profiles`, `vehicles`, `document_resources`, `driver_availability`, `staff_owner_binding`, `staff_permissions`, `order_assignments`, `identity_verifications`, `vehicle_photo_checks`, `support_cases`, `chat_messages`, `calls`, `audit_events`;
- DB-инварианты связывают active vehicle и assignment vehicle с тем же driver;
- допускается только один текущий assignment на order;
- identity verification привязана к точной связке assignment/order/driver/vehicle;
- добавлен migration contract test и включён в общий backend `npm test`.

GitHub Actions:
- Backend tests run #63: **PASS**;
- tested head: `78e5b09338a8a4964b9533783ee76ebfc3b1b248`.

Phase 1A не изменяет текущие production order transitions и пока не реализует Firebase exchange или новые `/v1` endpoints.

Следующий этап: Phase 1B — Firebase identity → staff session exchange с server-authoritative verification и negative/replay contract tests.

Merge и production deploy не выполнялись.


## 2026-11-07 — Backend Phase 1B: Firebase identity → staff session implemented

В ветке `uber-v3/arch/spec-v1-backend-phase1` продолжена реализация утверждённого D2.

Реализовано:
- `POST /v1/driver/session/exchange`;
- серверная проверка Firebase Authentication ID token:
  - RS256/kid/signature;
  - aud/iss/sub;
  - exp/iat/auth_time;
  - только `firebase.sign_in_provider=phone`;
  - номер телефона принимается только из подписанного Firebase token claim;
- Firebase App Check через отдельный `X-Firebase-AppCheck`, fail-closed;
- raw Firebase ID/App Check tokens не сохраняются в D1 и не логируются;
- replay-защита Firebase ID token через SHA-256 digest;
- существующая staff access JWT + rotating refresh session используется без отдельной параллельной session architecture;
- новый водитель создаётся с `registration_status=draft` и `identity_status=pending`; подтверждение телефона не даёт доступ к линии/заказам;
- клиентские поля `phone`, `role`, `driverId` в exchange request отвергаются;
- блокированный staff account не получает новую session;
- rate-limit:
  - installation: 5/min;
  - phone: 5/min;
  - отдельный IP bucket;
  - progressive delay;
  - в D1 сохраняются только digest bucket keys, без raw phone/IP/installation;
- добавлены additive migrations `0006_driver_firebase_auth.sql` и `0007_driver_exchange_rate_limits.sql`;
- VIN в migration 0005 исправлен: обязательный, но не UNIQUE, так как уникальность VIN владельцем отдельно не утверждалась;
- CI дополнен offline SQLite smoke-проверкой всех D1 migrations.

Проверки:
- GitHub Actions Backend tests run #84: **PASS**;
- head: `dc924d5429de98a87298dbfa86e6a68d9bceb1de`;
- SQL migration smoke: PASS;
- полный backend `npm test`: PASS;
- Firebase ID token crypto tests: PASS;
- Firebase App Check crypto tests: PASS;
- session exchange negative/replay tests: PASS;
- rate-limit tests: PASS;
- migration privacy/invariant tests: PASS.

Ограничение:
- реальный E2E Firebase Phone Auth/App Check не выполнен, так как production Firebase project/app credentials/config в backend ветку не добавлялись и deploy не выполнялся.

Merge и production deploy не выполнялись.


## 2026-11-07 — Backend Phase 1C foundation: driver profile and vehicles

В `uber-v3/arch/spec-v1-backend-phase1` реализован следующий этап D3.

Добавлено:
- migration `0008_driver_registration_details.sql`;
- в `driver_profiles`: `full_name`, `birth_date`, `revision`, `submitted_at`;
- в `vehicles`: `usage_basis`;
- отдельная `vehicle_photos` с утверждёнными 7 типами фото:
  - left;
  - front;
  - right;
  - rear;
  - open_trunk;
  - rear_cabin;
  - front_cabin;
- `GET /v1/driver/me`;
- `PUT /v1/driver/registration`;
- `POST /v1/driver/vehicles`;
- `PATCH /v1/driver/vehicles/{id}`.

Инварианты:
- driverId не принимается от клиента; используется только authenticated staff principal;
- профиль изменяется через `expectedRevision`;
- vehicle update изменяется через `expectedRevision`;
- несколько автомобилей на одного водителя разрешены;
- повторный VIN сервером не запрещается, так как уникальность VIN отдельно не утверждалась;
- критические изменения VIN/госномера/usage basis переводят approved автомобиль в `pending_review`;
- критическое изменение текущего active vehicle запрещается до выхода с линии;
- suspended/rejected vehicle нельзя редактировать обычным self-service PATCH;
- клиент не может установить registration status/approval самостоятельно.

Проверки:
- GitHub Actions Backend tests run #89: **PASS**;
- head: `bd9470a425f41c0274c68a3b02bbd3916574aaee`;
- D1 migration smoke: PASS;
- полный backend test suite: PASS;
- driver registration service contracts: PASS;
- duplicate VIN allowed contract: PASS;
- revision conflict: PASS;
- active vehicle critical-change block: PASS.

Не реализовано в этом подэтапе:
- private upload issuance/storage;
- обязательный комплект документов;
- registration submit completeness;
- административная модерация документов/автомобиля.

Merge и production deploy не выполнялись.


## 2026-11-07 — Backend Phase 1D: private uploads and registration submission

В `uber-v3/arch/spec-v1-backend-phase1` реализован следующий этап утверждённого D3.

Реализовано:
- migration `0009_driver_private_uploads.sql`;
- одноразовые `driver_upload_sessions` с атомарным `claim_id`;
- `POST /v1/driver/uploads` — выдача ограниченной upload-session;
- `PUT /v1/driver/uploads/{id}` — приватная бинарная загрузка;
- private object storage через fail-closed binding `DRIVER_PRIVATE_FILES`;
- постоянный публичный URL не создаётся и не возвращается;
- `Content-Length` проверяется до чтения binary body;
- тип/размер/назначение/vehicle ownership проверяются сервером;
- при ошибке после object write выполняется best-effort компенсация object/claim;
- metadata сохраняется в `document_resources` / `vehicle_photos`;
- audit: `driver.private_resource_uploaded`;
- чистая проверка полноты регистрации;
- `POST /v1/driver/registration/submit`;
- обязательные driver materials: passport main/registration, passport selfie, identity reference selfie, driver licence front/back;
- для автомобиля: usage basis, STS front/back, 7 утверждённых фото;
- `rental_contract` обязателен только при `usage_basis=rental`;
- хотя бы один автомобиль должен быть полностью готов;
- неполный дополнительный автомобиль не блокирует submit;
- успешный submit переводит профиль в `submitted`, а полные автомобили — в `pending_review`;
- `submitted → under_review` намеренно оставлен административному этапу;
- audit: `driver.registration_submitted`.

Storage configuration не добавлялась в production: реальный R2 bucket и `DRIVER_UPLOAD_MAX_BYTES` должны быть явно настроены для выбранного окружения перед E2E/deploy.

Проверки:
- GitHub Actions Backend tests run #101: **PASS**;
- head: `bb4bbdfd1cc3d3f40d32d3c3cd2c48f76f803d39`;
- D1 migration smoke: PASS;
- полный backend `npm test`: PASS;
- one-time upload lifecycle: PASS;
- повторный PUT: blocked;
- registration readiness: PASS;
- rental-contract conditional rule: PASS;
- incomplete extra vehicle does not block: PASS;
- registration submit transaction: PASS.

Дополнительный аудит phone uniqueness:
- текущая глобальная защита номера не ослаблялась автоматически;
- безопасная политика повторного использования номера после disabled/suspended отдельно не определена ТЗ;
- ослабление сейчас могло бы позволить обход административной блокировки новой регистрацией.

Merge и production deploy не выполнялись.


## 2026-11-07 — Backend Phase 1D completed; admin moderation API started

В ветке `uber-v3/arch/spec-v1-backend-phase1` продолжена реализация утверждённых D1–D3.

Phase 1D:
- реализованы one-time private upload sessions;
- private resources сохраняются только через будущий приватный R2 binding `DRIVER_PRIVATE_FILES`;
- постоянные public URL не используются;
- upload session атомарно claim-ится до записи объекта;
- повторное использование consumed upload запрещено;
- при ошибке finalization выполняется best-effort compensation;
- добавлены документы водителя и автомобиля;
- добавлены 7 обязательных типов фото автомобиля;
- добавлен `POST /v1/driver/registration/submit`;
- completeness check требует профиль, обязательные документы и минимум один полный автомобиль;
- rental contract обязателен только при `usage_basis=rental`;
- успешная отправка переводит регистрацию в `submitted`, готовые автомобили в `pending_review`;
- действия аудируются.

Runtime intentionally fail-closed:
- без `DRIVER_PRIVATE_FILES` upload не выдаётся;
- без `DRIVER_UPLOAD_MAX_BYTES` бинарная загрузка не разрешается;
- фактический R2 bucket и byte limit не создавались и не выбирались автоматически.

Admin foundation:
- реализован server-authoritative OWNER/DISPATCHER overlay через `staff_owner_binding` и `staff_permissions`;
- OWNER имеет полный override;
- dispatcher получает только явные granular permissions;
- legacy staff/order admin paths усилены permission checks;
- добавлена очередь регистрации `GET /v1/admin/registrations`;
- добавлен старт модерации `POST /v1/admin/registrations/{driverId}/review`;
- просмотр очереди требует `drivers.read`;
- старт проверки требует OWNER либо sensitive permission `drivers.verify_documents` / `drivers.verify_identity`;
- transition `submitted -> under_review` выполняется с `expectedRevision`, moderation history и audit.

Проверки:
- GitHub Actions Backend tests run #111: **PASS**;
- head: `a234340f2b6142ccd0f497772f5f0a431c6cad49`;
- D1 migration smoke: PASS;
- полный backend `npm test`: PASS;
- private upload lifecycle/readiness/submit tests: PASS;
- OWNER/DISPATCHER authorization tests: PASS;
- admin registration moderation permissions tests: PASS.

Остаётся:
- решения по отдельным элементам: identity/document/vehicle/vehicle_photo;
- changes_required/approved/rejected moderation flow;
- private resource read endpoint для уполномоченного администратора;
- фактическая R2/Firebase конфигурация и E2E.

Merge и production deploy не выполнялись.


## 2026-11-07 — Item moderation, idempotency and protected private reads

В `uber-v3/arch/spec-v1-backend-phase1` завершён следующий backend-подэтап D3.

Item-level moderation:
- добавлен review detail для регистрации без раскрытия private object keys;
- документы модерируются с `drivers.verify_documents`;
- vehicle и vehicle photos — с `vehicles.verify`;
- identity manual `verified` запрещён до утверждения и реализации D4;
- финальное registration `approved` fail-closed:
  - требуется `identity_status=verified`;
  - обязательные driver documents должны быть verified;
  - требуется минимум один approved vehicle;
- `changes_required` и `rejected` поддерживаются как moderation decisions;
- документы и vehicle photos получили собственный `revision`;
- модерируемые сущности используют internal `last_review_id` для точной связи state change → moderation history → audit.

Idempotency:
- добавлена `mutation_idempotency`;
- raw `Idempotency-Key` не сохраняется;
- сохраняются SHA-256 key/request digests;
- internal claim защищает concurrent retries;
- повтор ключа с другим запросом отвергается;
- moderation mutation routes требуют `Idempotency-Key`.

Private resource access:
- добавлен защищённый backend/R2 read endpoint;
- public/permanent URL не создаётся;
- passport/licence documents требуют `drivers.verify_documents`;
- identity selfie resources требуют `drivers.verify_identity`;
- vehicle photos требуют `vehicles.verify`;
- OWNER имеет override;
- успешный private read записывается в audit;
- response использует `no-store`, `nosniff`, без filename/private object key.

Также исправлен CORS contract:
- разрешены `Idempotency-Key`, `X-Firebase-AppCheck`;
- объявлены используемые GET/POST/PUT/PATCH/DELETE/OPTIONS.

Проверки:
- GitHub Actions Backend tests run #123: **PASS**;
- head: `1acc3b95ee0ace26a8f8e125a79e1e8569e324e3`;
- D1 migration smoke: PASS;
- полный backend `npm test`: PASS;
- mutation idempotency tests: PASS;
- item moderation policy tests: PASS;
- D4 identity fail-closed test: PASS;
- private resource permission/audit tests: PASS;
- CORS contract tests: PASS.

Merge и production deploy не выполнялись.

Следующий release-blocker: заменить driver self-take модель на server-authoritative assignment `order + driver + vehicle`.


## 2026-11-07 — Server-authoritative order assignment and targeted Driver push

В `uber-v3/arch/spec-v1-backend-phase1` закрыт основной blocker аудита: Driver больше не выбирает свободный заказ самостоятельно.

Assignment:
- добавлены `orders.revision` и internal `last_assignment_id`;
- добавлены `POST /v1/admin/orders/{orderId}/assign` и `/reassign`;
- assign требует `orders.assign`, reassign — `orders.reassign`;
- обязательны `Idempotency-Key`, `expectedRevision`, reason;
- assignment связывает `order + driver + vehicle`;
- hard gates: driver registration approved, identity verified, vehicle approved и принадлежит driver;
- если Driver online с другим active vehicle, назначение выбранного другого автомобиля запрещается;
- reassign атомарно отзывает старый assignment и создаёт новый;
- state change, assignment, audit и idempotency выполняются одной D1-транзакцией;
- D6 ranking/ETA/service-buffer не реализовывались, так как D6 не утверждён.

Legacy self-selection removed:
- Driver `new -> taken` через `/orders/{id}/status` теперь запрещён;
- legacy `GET /orders` больше не показывает Driver свободные `new` заказы;
- Driver получает только назначенные ему заказы;
- добавлен `GET /v1/driver/assignments`.

Push:
- широковещательный `new_order` удалён;
- создание неназначенного заказа не отправляет Driver push;
- push-target query требует конкретный accountId и активную session family;
- assign: targeted `order_assigned` только новому Driver;
- reassign: `order_assignment_revoked` старому Driver + `order_assigned` новому;
- payload содержит только opaque routing/state metadata, без passenger PII;
- idempotent replay не создаёт повторный push;
- push остаётся best-effort и не влияет на успешность D1 assignment.

Проверки:
- GitHub Actions Backend tests run #140: **PASS**;
- head: `83b4b289fc288368fbcd94bd0cb13f3abd6b47ef`;
- migration smoke: PASS;
- полный backend `npm test`: PASS;
- assigned-only Driver visibility: PASS;
- Driver self-take rejection: PASS;
- assignment/reassignment transaction tests: PASS;
- assignment permission/eligibility tests: PASS;
- targeted push selection/scheduling: PASS;
- staging FCM remains disabled: PASS.

Merge и production deploy не выполнялись.


## 2026-11-07 — Driver availability and trip operational verification gate

В `uber-v3/arch/spec-v1-backend-phase1` реализованы следующие утверждённые D3-контракты.

Driver availability:
- добавлен `PUT /v1/driver/availability`;
- input: `activeVehicleId + requestedState + expectedRevision`;
- driverId определяется только из authenticated staff session;
- mutation требует `Idempotency-Key`;
- `on_line` разрешён только при:
  - registration_status=approved;
  - identity_status=verified;
  - approved vehicle, принадлежащем Driver;
- переключение active vehicle из состояния `on_line` на другой автомобиль запрещено; сначала требуется перейти `off_line`;
- состояние, audit и idempotency меняются атомарно;
- D6 ranking/timeouts и D7 GPS-параметры не добавлялись.

Проверка availability:
- GitHub Actions run #145: **PASS**;
- head контрольной точки: `25597b68ec3f149ee22a080c6ac8bad1a5a5e50b`.

Trip verification / operational phases:
- migration `0014_assignment_operational_phases.sql`;
- assignment phases:
  - assigned;
  - en_route_to_pickup;
  - arrived;
  - in_trip;
- добавлена `assignment_revision` в trip verification binding;
- добавлен `POST /v1/driver/assignments/{id}/verification-challenges`;
- challenge создаётся только для собственного active assignment в фазе `assigned`;
- challenge binding: assignment/order/driver/vehicle + assignment revision + server timestamps;
- повторная активная pending challenge на ту же assignment revision блокируется;
- точный TTL не выбран автоматически: runtime требует `TRIP_VERIFICATION_CHALLENGE_TTL_SECONDS`, иначе fail-closed 503;
- raw challenge secret не возвращается и не хранится; D1 хранит только digest;
- публичного API для установки `verified` нет, так как D4 face/liveness provider ещё не утверждён;
- добавлен `POST /v1/driver/assignments/{id}/phase`;
- `assigned -> en_route_to_pickup` требует exact verificationSessionId со state=verified и полной tuple-binding;
- `en_route_to_pickup -> arrived -> in_trip` выполняются последовательно с expectedRevision;
- переход `in_trip` атомарно переводит core order `taken -> in_progress`;
- legacy Driver `taken -> in_progress` через `/orders/{id}/status` запрещён, поэтому selfie gate нельзя обойти старым API;
- Idempotency-Key и audit обязательны.

Проверка trip operational:
- GitHub Actions run #152: **PASS**;
- head: `c82da0db757e9a3d056ed8b3fd041c7410cd9aaa`;
- D1 migration smoke: PASS;
- полный backend `npm test`: PASS;
- challenge binding tests: PASS;
- verified-session SQL gate: PASS;
- client cannot set verification result: PASS;
- legacy in_progress bypass rejection: PASS;
- in_trip -> core in_progress atomic test: PASS.

Ограничение:
- до отдельного утверждения D4 реальный face/liveness provider и переход verification state в `verified` намеренно отсутствуют;
- фактическое значение challenge TTL не настраивалось;
- merge/deploy не выполнялись.

Следующий consistency-fix: синхронизировать terminal core states `done/canceled` с `order_assignments.state`.


## 2026-11-07 — Terminal order/assignment lifecycle synchronized

В `uber-v3/arch/spec-v1-backend-phase1` устранён consistency-риск terminal order states.

Изменено:
- legacy repository self-take `takeOrderForDriver()` удалён;
- repository больше не разрешает прямой `taken -> in_progress`; старт поездки остаётся только через verified assignment phase;
- `in_progress -> done` теперь одной D1 batch-транзакцией:
  - переводит core order в `done`;
  - переводит active `in_trip` assignment в `completed`;
- admin `canceled` одной D1 batch-транзакцией:
  - переводит order в `canceled`;
  - active assignment переводит в `revoked` и устанавливает `revoked_at`;
- pre-migration/legacy orders без `order_assignments` остаются совместимыми: terminal core transition допускает 0 assignment changes;
- terminal repository дополнительно требует корректный assignment phase для нового assignment-backed flow.

Проверки:
- GitHub Actions Backend tests run #155: **PASS**;
- head: `0d764bc4ce4b60360448ec54b61d8d7f03727e8d`;
- D1 migration smoke: PASS;
- полный backend `npm test`: PASS;
- direct repository `taken -> in_progress` rejection: PASS;
- done -> assignment completed: PASS;
- canceled -> assignment revoked: PASS;
- legacy order compatibility: PASS;
- router batch fixtures: PASS.

Merge и production deploy не выполнялись.

Следующий hardening-пункт D3: привести публичные terminal mutations `done/canceled` к обязательным `Idempotency-Key + expectedRevision + audit` перед дальнейшим расширением support/photo-control API.

## 2026-11-07 — Аудит репозиториев и исправления по его результатам

Основание: владелец поручил провести аудит и продолжить согласно ТЗ и аудиту.
Проверены актуальные Driver `7ff12dab4d7392ab4bb785d8388045345274216b`
и backend `0d764bc4ce4b60360448ec54b61d8d7f03727e8d`.
D1–D3 уже утверждены; повторное утверждение не запрашивалось.
D4–D7 по-прежнему не утверждены. Использованы отдельные worktree, прежние
локальные изменения не перезаписаны.

### Найдено и исправлено

- Реальный SQLite воспроизвёл ложные success audit/idempotency при проигравшем
  claim и stale revision в availability и фазах поездки. Связаны SQL-операции
  через `changes()` внутри одной batch-транзакции; после commit JS-проверка
  больше не является единственной защитой.
- Idempotency replay перенесён перед изменяемыми revision/state-предусловиями
  в assign, reassign, availability, phase, verification challenge.
  Текущая авторизация и ownership остаются до выдачи сохранённого результата.
- Запрещена смена/сброс active vehicle при активном assignment.
- Переход off_line разрешён после отзыва approval, без ослабления допуска on_line.
- Из обычных логов убраны адреса маршрута и raw Error в router/worker/staff auth.
  Неиспользуемый HTTP helper больше не пишет URL/body/exception в лог.
- Для всех JSON API-ответов принудительно установлены no-store/nosniff.
- npm audit первоначально: 4 high entries в toolchain dependency tree.
  Scoped Miniflare overrides: sharp 0.35.5, undici 7.30.1, обновлён lock.
  Повторная проверка: 0 известных advisories.
- OSV выявил GHSA-3cqm-mf7h-prrj (OkHttp 3.14.9) и
  GHSA-w33c-445m-f8w7 (Okio 1.17.2). Android явно использует OkHttp 4.12.0
  и Okio JVM 3.6.0; JVM runner обновлён на новые jar/Kotlin stdlib.
- CI backend расширен npm ci --ignore-scripts, npm audit --audit-level=high
  и Worker bundle --dry-run. Lifecycle scripts пакетов не выполнялись.

### Проверки

- До исправлений: 5 failing availability-сценариев, воспроизведение phase race,
  4 failing privacy-теста. После исправлений все эти сценарии проходят.
- Backend `npm test`: 57 наборов PASS, включая 19 новых сценариев
  (15 реальных SQLite + 4 privacy). Проверены rollback при audit failure,
  проигравшие claims, stale keys, changed-body replay, отзыв permissions,
  повторы фаз arrived/in_trip и challenge после изменения фазы.
- Все 14 D1 migrations: PASS, foreign_key_check чистый.
- Android: 10 JUnit PASS на новых HTTP jar; 6 configuration/source guards PASS.
- Gradle debugRuntimeClasspath: BUILD SUCCESSFUL, 129 компонентов;
  OSV batch для всех 129: 0 advisories. Это проверка графа, не APK.
- npm ci --ignore-scripts и wrangler deploy --dry-run: PASS;
  bundle сохранён локально, deployment не выполнялся.
- git diff --check и bash -n runner: PASS.
- Ограниченный поиск ключевых литералов не нашёл полных приватных ключей;
  PEM-marker в firebaseMessaging — шаблон разбора, не secret.

### Ошибки среды и оставшиеся ограничения

- Первый Node --test запуск в sandbox завершился ошибкой test subprocess;
  прямой node tests/... воспроизвёл реальные ошибки и используется в npm scripts.
- npm cache по умолчанию был read-only; применён npm_config_cache в /workspace.
  HOME не изменялся; proxy и TLS-проверка сохранены.
- Первоначальная Android-попытка в sandbox не смогла открыть служебный socket;
  повтор со штатным сетевым разрешением дошёл до SDK location not found.
- Android SDK/Firebase config отсутствуют: APK, Android lint/unit/instrumentation
  и реальный Firebase/R2/D1 E2E не подтверждены. Поддельный config не создавался.
- Нет production deployment, merge, настройки реальных credentials или
  выбора D4/D5/D6/D7 вместо владельца.

Подробности, приоритеты и ограничения: `docs/AUDIT_2026-11-07.md`.
Инвентаризация Android graph: `docs/AUDIT_ANDROID_COMPONENTS_2026-11-07.json`.
Backend inventory: `uber-v3/docs/AUDIT_DEPENDENCIES_2026-11-07.json`.
Исправления сохраняются в существующих Draft PR Driver #6 и backend #28.

Следующий D3-подэтап остаётся: terminal mutations с обязательными
Idempotency-Key/expectedRevision/audit; затем полный photo-check eligibility
и миграция Android на assigned-only /v1 flow. Эти блокеры не объявляются закрытыми.

### Проверка опубликованных изменений и CI

- Driver audit commit: `42e6730cff4d7233fca1625f81002bc28a9e12b3` (PR #6).
- Backend audit commit: `e774e4fe6e25956aff5e8a474ffdb1e5808134cc` (PR #28).
- CI run 156 выявил несовместимость npm 10.9.9 со scoped-overrides lock,
  проверенным локально на npm 11.9.0. Это ошибка воспроизводимости установки,
  а не провал пройденных локальных regression tests.
- Исправление `0e43d577d8a74e2df09f9cea025df4597653b83a` закрепляет npm 11.9.0,
  packageManager/engines и SHA actions, отключает сохранение checkout credentials.
- GitHub Actions run 157: PASS. Установка lock, audit, migrations, все 57 наборов
  тестов и Worker bundle прошли на GitHub runner Node 22.
- Дополнительно выполнена локальная проверка availability SQL в workerd/D1:
  success / existing_idempotency / conflict, ровно одна audit-запись.
  Проверка вынесена в `uber-v3/tools/check_availability_d1.mjs` и добавлена в CI.
- При подготовке локального Miniflare smoke были отклонены устаревшие параметры
  конструктора; использован штатный convertV4MiniflareOptions из установленного
  Miniflare 5. Проверки и security guards не отключались.
- Production, master/main, merge и deploy не изменялись.


### Финальная проверка backend CI run 158

- https://github.com/lobanov0710-blip/uber-v3/actions/runs/42369380643 — PASS.
- Проверен commit `26ca78ed3d84a3ee77c86ee386bf512812d36e1a`.
- Node 22 / npm 11.9.0: locked install, audit, все миграции, 57 наборов,
  отдельная проверка local workerd/D1 и Worker dry-run прошли.
- Исправления опубликованы в PR #28; Android зависимости, отчёт и этот журнал —
  в PR #6. Оба PR остаются Draft, merge/deploy не выполнялись.

## 2026-11-07 — P2: защита окна диалога подтверждения заказа

Исправлен P2 в Android Driver:
- в `OrderDetailsActivity` диалог подтверждения перехода статуса теперь сначала создаётся через `MaterialAlertDialogBuilder.create()`;
- до `show()` на собственное окно диалога устанавливается `WindowManager.LayoutParams.FLAG_SECURE`;
- сохранена защита базового `SecureActivity`, но диалог больше не полагается только на флаг Activity;
- изменение ограничено окном подтверждения и не меняет контракт переходов статусов или backend.

Проверка исходника:
- `FLAG_SECURE` применяется к `confirmationDialog.getWindow()` до `confirmationDialog.show()`;
- null-window обрабатывается fail-safe без падения;
- merge и production deploy не выполнялись.

Дальнейшая работа остаётся по утверждённому ТЗ и журналу: D1–D3 утверждены; D4–D7 не выбираются без отдельного решения владельца. Android затем должен быть переведён на assigned-only `/v1` flow после закрытия текущих D3 backend-блокеров.

## 2026-11-07 — D3: terminal mutations hardened

В backend `uber-v3/arch/spec-v1-backend-phase1` закрыт обязательный D3 blocker
для публичных terminal mutations `done/canceled`.

Контракт `POST /orders/{orderId}/status` теперь требует:
- `Idempotency-Key`;
- `expectedRevision`;
- текущую server-authoritative staff authorization.

Success mutation атомарно связывает D1 idempotency claim, изменение `orders`,
terminal state `order_assignments`, success `audit_events` и сохранение
idempotent response. `done` доступен только authenticated driver своей поездки;
`canceled` — OWNER/DISPATCHER с `orders.cancel`. Stale revision, losing claim
и changed-body replay не создают ложный success audit. Raw idempotency key в D1
не сохраняется; migration 0015 хранит только opaque internal claim marker.

Проверки backend:
- 15 migrations PASS;
- terminal real-SQLite suite 7/7 PASS;
- полный npm test PASS;
- npm audit PASS;
- local workerd/D1 audit check PASS;
- Worker dry-run PASS;
- GitHub Actions run 173 PASS, tested code head
  `1f115c517be8cf1635b9b2f14531c0e4dec9f51a`.

Merge/deploy не выполнялись. D4–D7 остаются неутверждёнными.
Следующий D3-подэтап: server-authoritative photo-control eligibility, затем
support/photo-control API и миграция Android к assigned-only `/v1` flow.

## 2026-11-07 — D3: photo-control eligibility и support API foundation

В `uber-v3/arch/spec-v1-backend-phase1` после terminal hardening начат
следующий утверждённый D3-блок.

Photo-control:
- добавлен `POST /v1/admin/vehicles/{vehicleId}/photo-checks`;
- обязательны staff authorization, `vehicles.verify`, `Idempotency-Key`
  и `expectedRevision`;
- monthly check получает строго 24 часа grace согласно ТЗ;
- complaint/manual поддерживают режимы `after_current_trip` и `immediate`;
- eligibility встроена непосредственно в D1 SQL для `on_line`,
  assign и reassign;
- grace разрешает работу до срока, после grace новые выход на линию/назначение
  запрещены;
- внеплановый blocking check запрещает новые назначения немедленно;
- активная поездка автоматически не отменяется без отдельного admin решения.

Support:
- добавлен `POST /v1/driver/support-cases`;
- только собственный Driver case;
- категории соответствуют ТЗ;
- обязательный `Idempotency-Key`;
- чужой order нельзя привязать к case;
- support API не предоставляет отмену заказа;
- create фиксирует `revision=1`, success mutation проходит через audit.

Проверки backend:
- migration smoke 16/16 PASS;
- полный npm test PASS;
- photo-control/support real-SQLite 5/5 PASS;
- workerd/D1 check PASS;
- Worker dry-run PASS;
- GitHub Actions run 188 PASS,
  tested head `4b8de9e44d44098917909fafcb9e81fbf5e13838`.

Это foundation, а не объявление полного photo-control/support завершённым.
Открыты: review/result photo-check, привязка private контрольных фото,
monthly due generation и дальнейший support lifecycle/chat.
VoIP/retention/юридические решения D5 не реализовывались.
Merge/deploy не выполнялись; D4–D7 остаются неутверждёнными.

## 2026-11-07 — D3: photo-control lifecycle и Driver↔Admin support/chat

Backend `uber-v3/arch/spec-v1-backend-phase1` завершил следующие утверждённые
D3 server-authoritative контуры.

### Photo-control lifecycle

- private контрольные фото связаны с конкретным `photoCheckId`;
- registration photos и photo-control photos изолированы друг от друга;
- Driver получает только собственные открытые проверки;
- Admin с `vehicles.verify` получает bundle проверки и выполняет review;
- review mutation требует `Idempotency-Key + expectedRevision + audit`;
- PASS допускается только при полном наборе 7 контрольных фото;
- после PASS backend фиксирует last-pass и следующий due;
- следующий due рассчитывается как один календарный месяц с end-of-month clamp;
- initial vehicle approval также создаёт первый server due;
- hourly system cron materializes due checks;
- `on_line`/assign/reassign дополнительно fail-closed по stored due + 24h,
  поэтому пропуск cron не открывает просроченный автомобиль.

Photo-control lifecycle SQLite: 6/6 PASS. Migration smoke: 17/17.
GitHub Actions run 206: PASS на
`9e93988b509e26acbcc4de6e6be3c11c870d0c48`.

### Driver ↔ Admin support/chat

Добавлены client-facing backend contracts:
- Driver list/detail своих support cases;
- Driver message в собственный case;
- Admin list/detail, reply и status;
- dispatcher permission: `support.manage`;
- OWNER override остаётся server-authoritative.

Message/status mutations требуют `Idempotency-Key + expectedRevision` и audit.
Закрытый case не принимает новые сообщения. Ни один support endpoint не меняет
order status и не даёт Driver право отмены заказа.

Support chat live-D1 retention: 30 календарных дней согласно утверждённому ТЗ.
Expired message скрывается по server timestamp и удаляется scheduled cleanup.
Chat body не сохраняется в idempotency result; после retention удаляется и
message-operation request digest. Backup retention/VoIP/legal policy этим этапом
не утверждаются и не реализуются.

Support lifecycle SQLite: 5/5 PASS. Полный npm test/workerd/D1/Worker dry-run PASS.
GitHub Actions run 216: PASS на
`92cb7013c934125b243a911e7bf188dbeebf9efd`.

Merge/deploy не выполнялись. D4–D7 остаются неутверждёнными.
Следующий D3 этап для Android: перейти на готовые assigned-only/photo-check/support
`/v1` contracts и закрепить contract/instrumentation tests без локального
дублирования серверных прав.

## 2026-11-07 — D3 Android alignment: assigned-only + hardened terminal + photo/support contracts

Android `TransferDriver/arch/spec-v1-security-baseline` приведён к уже готовым
server-authoritative D3 backend contracts без обхода D4.

### Заказы

Legacy driver flow больше не используется приложением:

- удалён API read `GET /orders`;
- источник списка — только `GET /v1/driver/assignments`;
- nested assignment response адаптируется в UI model с обязательными
  `orderRevision`, `assignmentRevision`, `assignmentId`, `phase`,
  `phaseUpdatedAt` и `vehicleId`;
- главный экран по умолчанию открывает active assigned orders;
- Driver больше не получает/не принимает свободные `new` заказы через Android;
- driver-side `new -> taken` удалён;
- legacy `taken -> in_progress` удалён.

Для `taken` UI показывает disabled действие **«Требуется фотопроверка»**.
До отдельного утверждения D4 приложение не создаёт fake `verified`, не обходит
verification challenge и не подменяет liveness локальным флагом.

### Terminal done

Сохранён только разрешённый terminal transition `in_progress -> done`.

Android отправляет:
- `Idempotency-Key`;
- `expectedRevision`;
- body `status=done`.

Idempotency key сделан replay-stable:
`android-done-<SHA-256(operation + orderId + expectedRevision)>`.
Повтор того же логического запроса после неопределённого network outcome получает
тот же key; изменение revision создаёт другой key.

### Photo-control / Support data contracts

Добавлены Java DTO/Retrofit contracts для:
- `GET /v1/driver/photo-checks`;
- `GET /v1/driver/support-cases`;
- `POST /v1/driver/support-cases`;
- `GET /v1/driver/support-cases/{caseId}`;
- `POST /v1/driver/support-cases/{caseId}/messages`.

Support message contract содержит обязательные `Idempotency-Key` и
`expectedRevision`.

UI photo-control/support пока намеренно не объявляется завершённым:
сначала зафиксирован безопасный data-contract layer. Camera/private-upload UI
будет подключаться без изменения D4 face/liveness решения.

### Regression/CI

Добавлены JVM contract tests:
- `DriverOrderApiContractTest`;
- `DriverV1ApiContractTest`;
- `IdempotencyKeyFactoryTest`.

Они фиксируют отсутствие legacy `getOrders`, assigned-only URL,
terminal `Idempotency-Key + expectedRevision`, driver photo/support URLs и
стабильность terminal idempotency key.

Добавлен branch CI:
- pinned `actions/checkout@v5`;
- pinned `actions/setup-java@v5`;
- Java 17 + Gradle cache;
- synthetic Firebase config только для CI, без production credentials;
- `clean :app:testDebugUnitTest :app:assembleDebug`;
- concurrency дедуплицирует push/PR runs одного head.

Промежуточные CI failures использовались как red checks:
- run 26: обнаружен отсутствующий import `StaffOrderDto`;
- run 34: обнаружен неверный escaping canonical separator в
  `IdempotencyKeyFactory`;
оба исправлены без ослабления контрактов.

Итоговый code head:
`9e86d874c38ad32dac8bb49bd153a8e77c270dca`.

GitHub Actions run 36:
- `:app:testDebugUnitTest` — PASS;
- `:app:assembleDebug` — PASS;
- `BUILD SUCCESSFUL`.

Backend final D3 head `91da54124c999d4a88b60eceee456fea357236b2`
ранее подтверждён GitHub Actions run 218 — PASS.

Merge и production deploy не выполнялись.
D4–D7 остаются неутверждёнными.

Следующий безопасный этап: Android UI для server-backed photo-control/support
(список проверок, private photo upload flow, support list/detail/chat) без
реализации/выбора face-liveness provider до отдельного утверждения D4.

## 2026-11-07 — D3 Android UI: photo-control + support/chat

Завершён следующий рекомендованный D3 client stage поверх уже проверенных
server-authoritative contracts.

### Photo-control UI

Добавлены:
- раздел **«Фотоконтроль»** из главного Driver screen;
- список собственных открытых `/v1/driver/photo-checks`;
- восстановление фактически принятых backend ракурсов через `uploadedKinds`;
- detail screen с 7 обязательными ракурсами:
  `left/front/right/rear/open_trunk/rear_cabin/front_cabin`;
- повторная съёмка для `failed/manual_review`;
- server refresh после network uncertainty, чтобы не считать локальный state
  источником истины.

Private upload flow:
1. Android запрашивает upload session через `POST /v1/driver/uploads`;
2. backend возвращает relative private PUT path;
3. Android отправляет бинарный JPEG через authenticated `PUT`;
4. permanent/public URL в Android не создаётся.

Camera privacy hardening:
- снимок создаётся только во внутреннем cache через restricted `FileProvider`;
- FileProvider разрешён только для `cache/photo-control/`;
- перед upload изображение декодируется, orientation применяется локально,
  JPEG пересохраняется и EXIF metadata не переносится;
- максимальная сторона при пересохранении ограничена 2048 px;
- временные файлы удаляются после подтверждённого результата/выхода;
- незавершённая operation сохраняет private cache path при configuration change;
- old cache cleanup удаляет файлы старше 24 часов;
- photo screens наследуют `SecureActivity` / `FLAG_SECURE`.

D4 face/liveness не реализован и не подменён vehicle photo-control.

### Support UI / chat

Добавлены:
- раздел **«Поддержка»** из главного Driver screen;
- список собственных support cases;
- создание case по утверждённым категориям;
- detail screen и Driver↔Admin chat;
- closed case блокирует отправку на клиенте, backend остаётся authoritative;
- support API не меняет order state и не предоставляет Driver отмену заказа.

Idempotency:
- create-case mutation сохраняет тот же random key при network uncertainty;
- message mutation сохраняет `Idempotency-Key + message + expectedRevision`
  до подтверждённого результата;
- pending message identity сохраняется через configuration change;
- при 409 клиент очищает pending mutation и перечитывает server state.

### API/client contracts

Добавлены Java DTO/Repository contracts для:
- `GET /v1/driver/photo-checks`;
- `POST /v1/driver/uploads`;
- private `PUT` по relative upload path;
- support list/create/detail/message APIs.

`DriverV1ApiContractTest` расширен private-upload contract:
- issue endpoint фиксирован;
- PUT использует `@Url`;
- `Content-Type` обязателен.

Backend recovery contract:
- `uploadedKinds` добавлен в `GET /v1/driver/photo-checks`;
- backend GitHub Actions run 220 — PASS на
  `abb787ddd1017c0b7e0a20ca68b62a530f8eea44`;
- journal-only final backend head
  `634043b6daf60b980622d89f3380d9f72f81a4de`;
- backend run 222 — PASS.

### Android CI

CI упрощён до одного push validation для рабочих `arch/**` / `fix/**`
веток. Это устранило двойные push+PR runs, которые взаимно отменяли друг друга.

Android code head перед этой записью:
`9c96eeaba7612b76fd88b1bfcda5093592c29024`.

GitHub Actions run 89:
- `:app:testDebugUnitTest` — PASS;
- `:app:assembleDebug` — PASS;
- `BUILD SUCCESSFUL in 2m 6s`.

Ограничение проверки:
- JVM contracts и debug assembly подтверждены;
- camera launch, FileProvider grant, реальный private upload и UI interaction
  на Android emulator/device этим GitHub runner не выполнялись.
  Не считать их runtime-PASS до отдельного instrumentation/E2E прогона.

Merge и production deploy не выполнялись.
D4–D7 остаются неутверждёнными.

Следующий безопасный D3 шаг:
1. добавить instrumentation tests для photo-control/support UI;
2. провести STAGING E2E private photo upload + support chat;
3. после подтверждения D3 client flow вернуться к D4 как к release blocker
   начала поездки.

## 2026-11-10 — D3 Android security instrumentation расширена, emulator PASS

В отдельной ветке `fix/d3-ui-instrumentation-ci` расширены существующие
instrumentation tests без изменения production, R2, D4–D7 или архитектуры.

Реальное чтение исходников выявило пробел `PhotoSanitizer`: EXIF ориентации
с зеркалированием (flip/transpose/transverse) не применялись к пикселям.
Добавлена нормализация всех восьми стандартных EXIF orientations перед
созданием JPEG. Исходные EXIF/GPS сведения не копируются в результат.

Добавлены Android-эмуляторные тесты:
- четырёхквадрантная проверка horizontal flip, clockwise 90°, transpose;
- fail-closed на пустом файле;
- фактический `FLAG_SECURE` окна LoginActivity до/после recreation;
- наследование SecureActivity пятью экранами order/photo/support;
- ранее существующие GPS metadata, FileProvider boundary и manifest
  non-exported проверки сохранены.

Первый полный run [42862000650](
https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42862000650)
выявил 9/10 PASS, 1/10 FAIL: тест требовал EXIF orientation=NORMAL (1)
на JPEG без ориентационного тега, где Android возвращал UNDEFINED (0).
Действительная проверка transform/pixel прошла; assertion исправлена на
нормальную/отсутствующую ориентацию. В workflow добавлен вывод XML отчёта
только при ошибке; сохранён исходный failing run для аудита.

Итоговые проверки кода на `060a06a250fdd6ee5335633105c854992b6caa04`:
- Android branch tests run
  https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42862576550 — PASS;
- D3 UI instrumentation run
  https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42862576549 — PASS;
- 10 Android instrumentation tests выполнялись на API 35 emulator,
  `connectedDebugAndroidTest` — PASS;
- сборка и JVM-контракты в отдельном CI — PASS.

Граница выполненной приёмки: подтверждены локальные Android security
properties, EXIF-санитизация, FileProvider и базовый secure window.
Это НЕ подтверждает end-to-end камеру/внешнее camera app, серверный
`uploadedKinds` recovery, HTTP 409 support replay, реальный
photo upload, chat interaction, Firebase и STAGING E2E.

R2 пока не создаётся, не подключается и не вызывается. D4 face/liveness
не затрагивается. Merge в master, production deployment и удалённые
D1 migrations не выполнялись. Следующий необходимый шаг — изолированные
UI/contract tests support/photo recovery с синтетическим backend, затем
узкий STAGING support chat E2E без private photo storage.

## 2026-11-10 — D3 support mutation isolated contract tests

В ветке `fix/d3-ui-instrumentation-ci` добавлен package-private
конструктор `DriverV1Repository(DriverV1Api)` для подстановки
синтетического API в JVM-тестах. Публичный конструктор продолжает
использовать штатный `ApiClient.getDriverV1Api()`.

Новый `DriverV1RepositorySupportContractTest` проверяет:
1. локальную валидацию обязательных `caseId/body/expectedRevision/Idempotency-Key`
   без сетевого обращения при некорректных аргументах;
2. точную передачу key/revision/body и единственный callback onConflict
   при HTTP 409 (без автоматического повтора);
3. синтетический сетевой сбой и возможность повторной отправки с тем же
   key/revision по решению вызывающего кода.

Ни один тест не использует настоящий backend, реальные токены,
production, STAGING или R2. Это repository contract coverage,
а не подтверждение UI-09/08 или STAGING E2E.
GitHub CI для SHA `3eedfcfe98f09ee4198d7c1028dd34ed99344996`:
- Android branch tests: https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42865226999
- D3 UI instrumentation: https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42865227127

Статус на момент записи: проверки запущены, результат ещё не подтверждён.
Merge, production deploy, D1 migration, R2 provisioning и D4 не выполнялись.
