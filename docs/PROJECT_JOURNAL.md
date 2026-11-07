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
