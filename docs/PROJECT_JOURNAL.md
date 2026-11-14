# Журнал проекта Transfer Drivers

> **Главное правило владельца, 2026-11-14:** действовать строго по утверждённым правилам, ТЗ и журналу.
> Любые самовольные отклонения и решения за владельца запрещены. При неопределённости —
> только варианты для владельца; зависящие действия ждут его явного решения. [Правило для работы](../AGENTS.md).

> **Текущая работа:** по решению владельца G1 приостановлен; начат G2 / отдельный Transfer Admin.
> TOTP выбран для подготовки предложения; параметры/контракт MFA ещё не утверждены.
> **Актуальный индекс: 2026-11-14.** [План и матрица ТЗ](PROJECT_PLAN_V1_0.md) · [Текущий аудит](AUDIT_SPEC_PLAN_JOURNAL_2026-11-14.md)
> Реализация частичная; production-выпуск не готов. SDK config/локальные сборки получены;
> подпись/Play/App Check/private R2 и живой E2E ещё не подтверждены. D1–D3 утверждены, D4–D7 нет.
> Ниже сохранена история: слова «текущий»/«отсутствует» внутри старого checkpoint относятся к его дате.

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

## 2026-11-07 — D3 Android instrumentation security checkpoint

После D3 photo-control/support UI добавлен и фактически выполнен Android
instrumentation security suite.

Canonical branch содержит:
- `DriverUiSecurityInstrumentationTest`;
- обычный branch CI дополнительно собирает `:app:assembleDebugAndroidTest`,
  чтобы instrumentation sources/test APK всегда компилировались.

Проверки на Android emulator API 35:
1. `PhotoSanitizer` пересохраняет JPEG без GPS EXIF metadata;
2. private `FileProvider`:
   - `exported=false`;
   - `grantUriPermissions=true`;
   - разрешает только `cache/photo-control/`;
   - произвольный файл из остального app cache не выдаётся;
3. `PhotoChecksActivity`, `PhotoCheckActivity`,
   `SupportCasesActivity`, `SupportCaseActivity` подтверждены как
   `exported=false`.

Обычная canonical CI:
- code head перед этой записью:
  `8ba482d5cce746ec6877572a47c35ad54372d006`;
- GitHub Actions run 92 — PASS:
  JVM tests + debug APK + debug AndroidTest APK.

Emulator validation выполнена на временной ветке
`fix/d3-ui-instrumentation-ci`, которая отличается от canonical code только
workflow-файлом instrumentation:
- первый run 1 завершился FAIL из-за quoting shell-команды:
  Gradle получил `\` как имя task; emulator/KVM были исправны;
- workflow command исправлен без изменения app code;
- instrumentation run 2 — PASS;
- `:app:connectedDebugAndroidTest` — PASS;
- устройство: `test(AVD) - API 35`;
- `BUILD SUCCESSFUL in 1m 6s`.

Это подтверждает именно security/runtime invariants выше.
Полный пользовательский E2E с реальной камерой, STAGING private upload и
двусторонним support backend ещё не объявляется PASS.

D4 face/liveness по-прежнему не выбран и не реализован.
Merge и production deploy не выполнялись.

Следующий D3 checkpoint: STAGING E2E photo upload/support с тестовыми данными,
после чего D3 client flow можно считать готовым к переходу к D4.

## 2026-11-07 — D2 Android phone identity + D3 STAGING E2E gate

Перед authenticated D3 STAGING E2E устранён найденный D2 client gap:
backend уже поддерживал Firebase phone identity -> staff session, но Driver Android
ещё использовал legacy login/password.

### Build environment lock

Driver API endpoint разделён по build variant:

- debug -> `https://uber-v3-staging.lobanov0710.workers.dev/`;
- release -> `https://uber-v3.lobanov0710.workers.dev/`.

`ApiEndpointPolicy` принимает только заранее разрешённый exact host текущего
variant. Cross-environment credential routing запрещён.

CI теперь собирает:
- `:app:testDebugUnitTest`;
- `:app:assembleDebug`;
- `:app:assembleRelease`;
- `:app:assembleDebugAndroidTest`.

После сборки CI проверяет generated BuildConfig:
- debug содержит только STAGING endpoint;
- release содержит только production endpoint.

### D2 Firebase phone authentication

Добавлены:
- Firebase Authentication;
- Firebase App Check Play Integrity;
- `DriverPhoneNormalizer`;
- `DriverFirebaseSessionExchangeRequest`;
- `DriverFirebaseSessionRepository`;
- contract/unit tests для phone normalization и D2 exchange.

Driver login flow теперь:

1. ввод номера телефона;
2. Firebase Phone Auth отправляет SMS;
3. водитель вводит 6-значный код;
4. Android получает Firebase ID token;
5. Android получает Firebase App Check token;
6. Android отправляет:
   - header `X-Firebase-AppCheck`;
   - body `firebaseIdToken + installationId`;
   в `POST /v1/driver/session/exchange`;
7. backend остаётся authoritative по phone/role/driver identity;
8. полученная staff session сохраняется существующим `SecureSessionStore`
   через Android Keystore;
9. после успешного входа выполняется push registration.

Client body намеренно не содержит `phone`, `role` или `driverId`.

Legacy Driver-side `staff/login` удалён:
- `StaffApi.login(...)` отсутствует;
- `LoginRequest.java` удалён;
- `StaffRepository` оставлен только для logout;
- refresh/logout существующей staff-session сохранены.

### Privacy / App Check policy

- SMS code не сохраняется в Bundle/SharedPreferences;
- phone input имеет `saveEnabled=false` и не записывается в saved-state Bundle;
- Firebase ID token и App Check token не сохраняются и не логируются;
- после создания Firebase credential SMS поле очищается;
- raw Firebase exception text пользователю не выводится;
- App Check provider: **Play Integrity**;
- `firebase-appcheck-debug` намеренно не используется;
- CI fail-closed запрещает появление Firebase debug provider dependency.

Для реального STAGING E2E Play Integrity должен быть корректно настроен для
реального тестового канала/установки приложения. Synthetic CI Firebase config
проверяет compile/contracts, но не считается реальной Firebase identity проверкой.

### STAGING readiness result

Read-only backend audit показал, что текущий deployed STAGING ещё не готов к
authenticated D3 E2E:

- deployed Worker не содержит Driver v1 routes (`/v1/driver/me` -> 404);
- staging D1 имеет 13 pending migrations `0005`–`0017`;
- private `DRIVER_PRIVATE_FILES` R2 binding отсутствует;
- D2 Firebase project/app identifiers в staging Worker отсутствуют;
- `staff_accounts=0`;
- remote writes/deploy не выполнялись.

Backend отдельно подтвердил:
- static migration safety PASS;
- Time Travel available;
- schema-only export + local simulation `0005`–`0017` PASS;
- production не изменялся.

### Android CI

Промежуточные red checks:
- run 119: после удаления `LoginRequest` осталась legacy декларация
  `StaffApi.login(...)`; удалена;
- run 124: новый D2 contract test имел конфликт имён локальных переменных;
  исправлен без изменения production code.

Итоговый D2 Android code head перед этой записью:
`3ac62926ae18879e1c75eef0640a6fc203f0af5f`.

GitHub Actions run 125 — PASS:
- JVM tests — PASS;
- debug APK — PASS;
- release APK — PASS;
- debug AndroidTest APK — PASS;
- debug endpoint -> STAGING — PASS;
- release endpoint -> production — PASS;
- Play Integrity App Check policy — PASS.

### Следующий gate

До реального authenticated D3 STAGING E2E нужны отдельные подтверждённые
infrastructure actions:

1. подтвердить реальные Firebase project ID / project number / Driver app ID и
   App Check/Phone Auth configuration;
2. подтвердить отдельный private staging R2 bucket;
3. применить staging D1 migrations `0005`–`0017`;
4. сформировать staging Wrangler config, прошедший backend fail-closed preflight;
5. выполнить deployment **только** `uber-v3-staging`;
6. повторить route probe;
7. выполнить phone auth -> session exchange -> photo upload -> support E2E.

Ни migrations apply, ни R2 creation/binding, ни staging deploy в этом checkpoint
не выполнялись. Production не изменялся. D4–D7 не затрагивались.


## 2026-11-11 — Продолжение D3 STAGING gate и помощь владельцу с Firebase

Сверены актуальные ветки: Driver `8b99ad9d8522aeed900a1f89dbe53f695ce1bc8c`,
backend `8cc860443f43c25f5f4edf312f5934dfd1949c11`. Ранее завершённые terminal,
photo-control, support и Android этапы не выполнялись заново.

Владелец подтвердил в текущем сеансе:
- R2 права ещё не добавлены, согласованный private bucket не создан;
- существующий Firebase-проект есть, но требуется помощь найти его настройки;
- реальные Project ID / project number / Android app ID пока не получены.

Даны конкретные инструкции Firebase Console → проект → шестерёнка →
Project settings → General, затем Android-приложение ru.transferservis.driver.
Публичные identifiers не заменяются package name или CI fixtures.
Руководство владельца: `uber-v3/docs/D3_STAGING_OWNER_SETUP.md`.

Выполнено без remote writes:
- свежий baseline: 61 набор npm test PASS, 17 migrations smoke PASS;
- GET /v1/driver/me, /photo-checks, /support-cases, /uploads на STAGING — HTTP 404;
- воспроизведены 9 отрицательных subcases в 5 новых preflight regression tests;
- закрыты singular route, nested env overrides, дополнительные resource bindings,
  другой staging-named bucket и несовместимый Firebase app/project number;
- preflight теперь принимает только ранее согласованный private bucket;
- все 12 preflight tests PASS, git diff --check PASS;
- добавлены ignore-правила для Python __pycache__/pyc, без включения build artifacts.

Firebase Basics применена для подготовки получения настоящего SDK config:
официальная CLI 15.35.0 проверена, login:list не вернул авторизованных аккаунтов.
Никакой проект/приложение Firebase автоматически не выбирались и не создавались.
Правило skill: “If no Project ID was provided, you MUST pause”; владелец выбрал
существующий проект, дальнейший выбор ожидает его подтверждённый Project ID.
Секреты и SMS-коды у владельца не запрашивались.

Cloud desktop capability enabled/unknown, но browser-control tools отсутствуют:
не доступны cua_repl.js и open_in_codex. Проверенная среда:
ccarenv_b64_Y2NhcmVudl9lNDkzYjZiZTVjNDg4MTkxYTVjYzA0YTBlMDExN2NmYg.
Владелец получил путь к настройкам в своём браузере; автоматическое открытие
Firebase Console не объявлялось успешным.

Данные о применении STAGING migrations 0005–0017 взяты из последнего backend
журнала; repeat apply не запускался. R2 create, config binding, STAGING deploy
и authenticated E2E в этом сеансе не выполнялись. Production не изменялся.
Текущий gate требует реальных Firebase identifiers и прав/ресурса R2;
после них продолжается уже согласованный staging-only preflight/deploy/E2E.
D4–D7 остаются неутверждёнными.


### Проверка CI текущего checkpoint

GitHub Actions backend run 241 — PASS:
https://github.com/lobanov0710-blip/uber-v3/actions/runs/43037859840

Проверен commit `868e940cb4ac7e6ad96c28c42c7aecc695620f1a`:
locked install, dependency audit, 17 migrations, 12 staging preflight tests,
61 backend test suites, local workerd/D1 и Worker dry-run — PASS.
Настоящий Firebase project/app не выбран, R2 bucket не создан, deploy/E2E
не выполнялись. Следующее необходимое действие владельца — сообщить Firebase
identifiers из настроек существующего проекта и подготовить R2 права/bucket.


## 2026-11-11–12 — Аудит всего проекта по Ponytail с немедленным исправлением

Запрос владельца: проверить весь проект по Ponytail, сразу исправить замечания
и зафиксировать каждое в журнале. Применены entry-ponytail, ponytail и ponytail-audit;
правило skill «report only» переопределено явным поручением владельца исправлять.
Полный перечень, пути и метод проверки: [PONYTAIL_AUDIT_2026-11-12.md](PONYTAIL_AUDIT_2026-11-12.md).

Baseline: Driver `7a8fe6c8faa33b458a79569a80faaacdb4847503`, backend
`868e940cb4ac7e6ad96c28c42c7aecc695620f1a`. Обе feature ветки сверены с GitHub.
Удалены недостижимая старая модерация, её SQL fake, unused HTTP helper, массовое
уведомление, шаблонный экран/тест и сломанный ручной JVM runner. Canonical digest
переведён на Object.fromEntries; native notification tag/Intent.data заменяют hash ID.

Дополнительный review подтвердил и исправил: digest collision для __proto__,
несовпадение FCM контрактов, неатомарные IV/ciphertext и stale session rotation/clear,
повтор запроса под другим водителем после смены login, moderation replay после
смены revision/status, несовпадение audit/review target type, зеркальные EXIF
ориентации, необработанное отсутствие camera Activity и устаревшие source guards.
Каждое замечание раскрыто в отчёте. Итог code/tests/CI: -937 строк, 0 новых deps.

Проверено локально: 61 backend suite PASS, 17 migrations PASS, 12 staging preflight
PASS, npm audit 0 vulnerabilities, 14 mutationReplay SQLite tests PASS,
moderation policy PASS, 6 Android security configuration checks PASS,
git diff --check PASS. Android regression tests добавлены в API 35 emulator CI.
Итоговая debug/release/AndroidTest сборка, runtime Keystore/EXIF и backend CI
проверяются после публикации; они не объявляются выполненными этой записью.
Попытки локального workerd/dry-run были прерваны; свежий PASS для них ожидается в CI.

R2 bucket/permissions и Firebase identifiers по-прежнему требуют владельца.
Authenticated D3 E2E не выполнен; CI Firebase fixture — только compile fixture.
Production, deploy, migrations apply, merge и неутверждённые D4–D7 не изменялись.


### 2026-11-12 — Ponytail: итог всех доступных проверок

Android code head `3273bfc25316e790f26286ba5477c7037f710014`:
https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43224019792
— SUCCESS. JVM tests; debug/release/AndroidTest APK; 6 source security checks;
STAGING/production endpoint locks; Play Integrity policy; API 35
connectedDebugAndroidTest (в APK включены regression tests Keystore,
stale rotation/logout/new login/account switch и 8 EXIF orientations) — PASS.

Backend code/CI head `ea1109107b479e33b914aec047aeaa8f3daae76f`:
https://github.com/lobanov0710-blip/uber-v3/actions/runs/43224400939
— SUCCESS. npm ci без lifecycle scripts, audit 0 vulnerabilities, 17 migrations,
12 preflight tests, 61 suites, local workerd/D1 и Worker dry-run — PASS.
Лог workerd: first=success, loser=existing_idempotency, stale=conflict,
auditRows=1, result=PASS. Node20 warning после обновления actions отсутствует.
Первый backend run 43224042256 также PASS; обновление действий не меняет backend.

Оба отчёта синхронизированы. Итог code/tests/CI относительно baseline:
-936 строк (-8 Android, -928 backend), 0 новых dependencies; документация
в подсчёт не включена. git diff --check — PASS. Все подтверждённые замечания
этого аудита исправлены. Исторические записи до финального CI остаются в журнале.

Эта финальная публикация меняет только отчёт/журнал; результаты выше относятся
к указанным code heads. Не выполнен и не объявляется PASS реальный Firebase/R2
E2E: требуется участие владельца с Firebase identifiers и R2 permissions/bucket.
Production, STAGING deployment, migration apply, merge и D4–D7 не изменялись.


## 2026-11-12 — Ponytail: второй проход по всему коду

Явное поручение владельца: повторно проверить весь код, исправить найденные баги.
Baseline GitHub: Driver 7b5cb03312bf5822eeef0d444e39fdac8f9db2fe,
backend 50d14fc1cb2a03f8acd89421b449f6ef69c319a8. Перед работой подтверждено
точное совпадение локальных git tree с опубликованными. Оригинальные checkout
и чужие ветки не изменялись. Применены Ponytail entry/audit/full; исправления
разрешены прямым поручением пользователя, включая bugs/security review.

Исправлены: JWT parse exceptions с фрагментами входа в logs; Telegram network
errors с bot URL, upstream description/statusText и chat ID в logs/исключениях;
raw Error.name в двух Mapbox transport catch; безусловная очистка сессии из шести
Android экранов после stale authenticationRequired. Авторитетная условная очистка
в auth/session слое сохранена. Удалены повторяющиеся UI clear wrappers.

Red → green: новые privacy tests и Android source guard воспроизвели дефекты
до исправления. Добавлен HTTP interceptor runtime regression для смены A → B
перед ответом 401. CI теперь проверяет реальные JUnit XML, ненулевые counts,
отсутствие failures/errors/skips и запуск этой новой проверки.

Локально PASS: 61 npm suite; 6 privacy checks; 17 migrations; 12 preflight tests;
127 JS/MJS syntax checks; 25 Android XML; 7 Android security guards; diff --check.
Итоговая Android debug/release/AndroidTest сборка, JVM, API 35 instrumentation,
backend clean-install/audit/local workerd/dry-run ещё ожидаются в CI; эта запись
не объявляет их PASS. Детали: [PONYTAIL_AUDIT_2026-11-12_FOLLOWUP.md](PONYTAIL_AUDIT_2026-11-12_FOLLOWUP.md).
Production source -78 строк, tests/guards/CI +115; общий diff +37, dependencies +0.

Firebase/R2 owner blockers сохраняются: реальный E2E не запускался, credential
bypass не применялся. Production/deploy/merge/remote migrations/D4–D7 не менялись.


### 2026-11-12 — Второй Ponytail audit: итоговые CI результаты

Android code `9fde9e363f42dc21fb58824bfed0237535a6b5a8`:
https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43229116512
— SUCCESS. Debug/release/AndroidTest APK, 7 source/config security checks,
endpoint variant locks и Play Integrity provider policy — PASS.
Фактические JUnit XML подтверждены CI: JVM_TESTS=PASS count=29,
ANDROID_TESTS=PASS count=10 (API 35), failures/errors/skips отсутствуют.
Новый delayedHttpUnauthorizedPreservesTheSubsequentLogin найден в реальных
runtime testcases; его отсутствие теперь проваливает CI.

Backend code `fe8bcb60f0ca0af9138ea1ceedc9fc3cac3f8580`:
https://github.com/lobanov0710-blip/uber-v3/actions/runs/43229194856
— SUCCESS. npm ci без lifecycle scripts, npm audit 0 vulnerabilities,
61 test suite (включая 6 privacy regressions), 17 migrations, 12 staging
preflight tests, local workerd/D1 atomic audit и Worker dry-run — PASS.
Workerd: first=success, loser=existing_idempotency, stale=conflict, auditRows=1.

Изменения опубликованы в существующих arch/spec-v1-* ветках, PR #6 и #28
остаются open/draft. Все подтверждённые замечания текущего прохода исправлены;
данные о возможном фактическом раскрытии production credentials не получены,
аудит подтверждает устранение дефектов кода, не историческое отсутствие утечек.
Финальная запись меняет только документацию; проверены указанные code SHA.
Реальный Firebase/Phone Auth/App Check/R2 E2E всё ещё BLOCKED внешней настройкой;
production, deployment, merge и remote migration apply не выполнялись.


## 2026-11-12 — Владелец предоставил Firebase project/app identifiers

Источник — сообщение владельца в текущем сеансе:
- Project name / App nickname: Transfer Driver;
- Project ID / FIREBASE_PROJECT_ID: transfer-driver-1590d;
- Project number / FIREBASE_PROJECT_NUMBER: 502279442547;
- Android App ID / FIREBASE_DRIVER_APP_ID: 1:502279442547:android:0cfc722af33b476bc41a06;
- package: ru.transferservis.driver.

Локально подтверждены корректный формат, одинаковый project number в App ID
и совпадение package с Android applicationId. Это данные владельца; независимая
проверка Firebase API и настоящий SDK config ещё ожидаются. API key не придумывался,
CI compile fixture не выдавался за рабочую конфигурацию.

Применён Firebase Basics. Node 24.22.1 и официальная Firebase CLI 15.36.0 проверены.
CLI login:list: No authorized accounts. Firebase MCP tools не обнаружены.
Владелец выбрал вариант 1: получить SDK config на своём компьютере. Подготовлены
точные официальные CLI login/apps:sdkconfig команды с указанными project/app ID.
Пароли, OAuth-коды и service-account private keys не запрашивались.
Обновлено руководство backend docs/D3_STAGING_OWNER_SETUP.md и preflight checkpoint.
Добавлены ignore-правила для Firebase local/debug artifacts и SDK config.

Существующий проект/приложение не создавались повторно; удалённые настройки
Firebase/Cloudflare не изменялись. R2 rights/bucket ещё не готовы по последнему
подтверждению. Phone Auth/App Check, настоящая SDK-сборка и authenticated E2E
пока не объявляются PASS. Сохранены production/D4–D7 ограничения.

Проверки checkpoint: identifiers приняты существующим fail-closed preflight
в локальном candidate config (только совместимость формата; существование R2
или привязка Firebase API этим не подтверждаются); 12 preflight regression tests
PASS; git check-ignore для SDK config/Firebase cache/debug logs PASS;
git diff --check PASS. Application source и реальные backend bindings не менялись.


## 2026-11-12 — Получен настоящий Firebase SDK config, первая локальная сборка

Владелец передал google-services.json файлом (696 bytes) после официальной CLI
загрузки на Windows. Несмотря на последующий libuv Assertion failed в Windows,
JSON корректен и совпадает с ранее подтверждёнными project ID/number/app ID/package:
transfer-driver-1590d / 502279442547 /
1:502279442547:android:0cfc722af33b476bc41a06 / ru.transferservis.driver.
Проверены схема, формат client API key и отсутствие service-account/private-key полей.
Файл размещён только в gitignored app/google-services.json с mode 0600. Содержимое
и API key не включаются в Git, journal или tool output.

Владелец подтвердил: Authentication Phone включён; App Check не настроен;
Google Play Console/внутренний тестовый канал пока не подтверждены. Выбраны ОБА
канала: Google Play и прямая установка APK. Release/upload-ключа нет; владелец
попросил безопасную инструкцию. Подготовлен Android docs/RELEASE_SIGNING_OWNER_SETUP.md:
ключ генерируется у владельца; пароли/private keystore в чат не запрашиваются.

В managed workspace установлен официальный Android SDK Platform 37.0 и Build Tools
37.0.0. Command-line tools ZIP проверен по checksum официального Google repository;
новый Android CLI использован с no-metrics. TLS/proxy trust сохранены.
Первый запуск Gradle wrapper без execute bit завершился exit 126; далее запуск
через bash без изменения mode/source. SDK/Firebase/App Check обходы не применялись.

Первый build с РЕАЛЬНЫМ SDK config PASS: testDebugUnitTest, assembleDebug,
assembleRelease, assembleDebugAndroidTest; отдельно bundleRelease/signingReport PASS.
Фактические JUnit XML: 29 JVM tests, failures/errors/skips 0; 7 security guards PASS.
Проверено совпадение Firebase resources в debug APK, release APK и release AAB
с полученным JSON, включая API key (значение не логируется). Debug APK signature
v2 PASS; merged manifest: package, allowBackup=false, usesCleartextTraffic=false,
networkSecurityConfig PASS. Release APK/AAB пока unsigned, не объявляются готовыми
для публикации. Runtime instrumentation с этой конфигурацией и authenticated E2E
ещё не выполнялись. Хеши первых артефактов — Android docs/REAL_FIREBASE_BUILD_2026-11-12.json.

При подготовке Play test channel выявлен отсутствующий вариант: debug использует
STAGING, но не подходит для Play release; обычный release использует PRODUCTION.
Добавляется native staging buildType на основе release: DEBUG=false,
STAGING endpoint, та же applicationId и Play Integrity provider; versionName suffix
-staging, без придумывания production ключа или смены существующих endpoint locks.
Проверки новой staging сборки и соответствующий CI ещё НЕ объявлены PASS.
Первый testStagingUnitTest: task отсутствовал; в текущем AGP по умолчанию создаётся
только debug JVM test component. Добавляется явное включение host tests для вариантов.

R2 permission/private bucket и remote STAGING readiness всё ещё требуют владельца.
Firebase/Cloudflare remote writes, deploy/merge/migration apply и D4–D7 не изменялись.


### 2026-11-12 — Изоляция сессий при смене подписанной сборки

Staging variant локально собран: testDebugUnitTest/testStagingUnitTest/
testReleaseUnitTest и assembleStaging/bundleStaging PASS. Изначально два теста
staging упали из-за предположения DEBUG=false → PRODUCTION. Исправлено ожидание
по BUILD_TYPE с отказом для неизвестных вариантов; DEBUG проверяется отдельно.

Проверка обновления между одинаково подписанными staging/production пакетами
выявила риск: старый AES-GCM blob не был привязан к адресу backend. AAD теперь
v2 + ApiEndpointPolicy.BASE_URL. При смене среды или чтении legacy v1 сессия
удаляется и требуется новый вход. Не добавлены зависимости, debug provider или
настраиваемые пользователем endpoints. Добавлены два Android runtime regression
tests: чужой backend AAD и legacy AAD при сохранённых preferences/Keystore;
положительный round-trip и очистка проверяются тем же тестом.
Повторная сборка всех артефактов после security fix и новый CI ещё выполняются;
результат runtime до завершения CI не объявляется PASS.


### 2026-11-12 — Финальная локальная сборка после security fix

Code/workflow опубликованы в Android feature branch commit
5cd807568b7b84b9e151867eb930cfbadad4c086; Draft PR остаётся без merge.
С реальным Firebase config после AAD fix прошли 87 JVM tests (29 debug + 29
staging + 29 release), failures/errors/skips 0. Повторно собраны debug/staging/
release APK, staging/release AAB и debug AndroidTest APK; Gradle PASS, 189 tasks.
7 source security guards и git diff --check PASS; staging/release lintVital PASS.
Полный дополнительный lintStaging не выполнен: запрос запуска отменён пользователем,
лог запуска не создан. Это не включается в перечень успешных проверок.

Проверены все три APK manifests: package, allowBackup=false,
usesCleartextTraffic=false, networkSecurityConfig; staging/release non-debug.
Firebase project ID/app ID/API key совпали с реальным config во всех APK/AAB;
значение API key не логировалось. Debug APK v2 signature PASS; staging/release
APK и AAB подтверждённо unsigned. Endpoint locks: debug/staging → STAGING,
release → PRODUCTION. Скомпилированный AndroidTest APK содержит обе новые regression
проверки. APK/AAB сохранены вне Git; SHA-256 и source file hashes — Android
REAL_FIREBASE_BUILD_2026-11-12.json. Встроенный AGP VCS metadata сообщает
NO_VALID_GIT_FOUND; точная ссылка на опубликованный source и хеши записаны отдельно.
Первоначальные артефакты сохранены отдельно как first-build; актуальные имеют
новые хеши после security fix. Инструкция подписи содержит хеш staging AAB.

GitHub Actions run 43314469461 запущен; runtime результат ещё ожидается.
Он использует synthetic config только для compile/runtime security tests;
локальные артефакты используют настоящий SDK config. Реальный Phone/App Check
E2E не выполнен. Owner signing/App Check/Play setup и private R2 остаются gate.


### 2026-11-12 — CI security regressions подтверждены

Android source commit 5cd807568b7b84b9e151867eb930cfbadad4c086:
https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43314469461
job 131518735357, conclusion SUCCESS. Логи проверены: JVM_TESTS=PASS count=29,
STAGING_JVM_TESTS=PASS count=29, RELEASE_JVM_TESTS=PASS count=29,
ANDROID_TESTS=PASS count=12 (API 35); 7 source guards, endpoint locks,
Play Integrity provider policy и сборки всех вариантов PASS. CI report guard
подтверждает выполнение новых tests sessionFromAnotherBackendRequiresLogin и
legacySessionWithoutBackendBindingRequiresLogin, а также прежней HTTP 401/session
race regression. Пропусков/failures/errors в test XML нет. CI использует synthetic
Firebase config, authenticated E2E не подменяется этими результатами.

Итог: non-debug STAGING APK/AAB готовы к подписи владельцем; сессия привязана
к backend через AES-GCM AAD v2. Локальные APK/AAB собраны с настоящим Firebase
config и имеют отдельный receipt с хешами; debug APK предназначен для проверки UI,
staging/release unsigned не предназначены для установки/публикации до подписи.
Пользователю передаются артефакты и безопасная инструкция создания upload key
у себя. Private keystore/пароли не запрашиваются и не создавались в workspace.
Следующий gate: owner signing/Play App Signing + App Check для обоих каналов,
private R2 permissions/bucket, затем проверка STAGING deployment и реальный E2E.
Backend source неизменён; повтор 61 backend suites только из-за docs не требуется.
Финальная публикация этого checkpoint меняет только документацию/журналы;
production/deploy/merge/migrations и D4–D7 не изменялись.


## 2026-11-14 — Аудит ТЗ, плана и журналов проекта

Основание: владелец поручил проверить ТЗ, план и записи, провести аудит и записать
результат. Проверены утверждённые §1–40, решения D1–D7, исторические review/audit,
checkpoint обоих журналов, owner/signing/preflight документы и ключевые code paths.
Базы GitHub: Driver 0c7777025c7c58231efaedbb2b8d82c335807ce4;
backend 905c6ebe86907056f0d8be8f6f6edaff07b135e4. Локальные checkpoint tree SHA
совпали с опубликованными. PR #6/#28 открыты, Draft, merged=false.
ТЗ неизменно: blob 7434dc0b1548b4f2c5382f12d6cc5399f93d0a84 совпал, 40 разделов.

Результат: частичная реализация; не готово к полному production-выпуску.
A1 (high): нет отдельного Admin и ряда Driver UI, GPS/passenger chat/VoIP/ranking.
A2 (high): реальный Firebase/App Check/R2 E2E и подпись не подтверждены; D4 provider
отсутствует, guard движения без verified сохранён. Owner сведения о ключе/App Check/
Play/R2 — последние подтверждения 2026-11-12, а не новая remote проверка сегодня.
A3 (high): openRoute в OrderDetailsActivity строит pickup→destination независимо
от фазы; не соответствует ТЗ §22. Открыто как функциональная задача G3.
A4 (medium): рабочий обзор/план и вход в историю устарели. Исправлено документально:
создан текущий PROJECT_PLAN_V1_0.md с матрицей всех 40 разделов и этапами G0–G5;
добавлены индекс/исторические указатели, прежние события не переписаны.
A5 (medium): Android OSV inventory 129 debug components не включает добавленные
Auth/App Check; полный lint не подтверждён. Новой CVE не установлено; это пробел
release coverage, который нельзя закрывать npm audit или source guards.
A6 (low): embedded AGP provenance NO_VALID_GIT_FOUND; внешние receipt/source/artifact
hashes есть и совпали. Улучшение release provenance включено в G5.

Новые локальные результаты: 61 backend suite PASS (все test chain commands),
17 migration smoke PASS, 12 preflight tests PASS; 7 Android source/config guards,
25 XML parse, проверка 5 receipt source hashes и 6 artifact SHA-256 PASS.
Проверены существующие CI базовых head commits: Android run 43316231722 /
job 131524852748 SUCCESS, JVM 29+29+29 и API 35 runtime 12, build/endpoint/provider
checks PASS; backend run 43316251413 / job 131524919923 SUCCESS, npm audit 0,
17 migrations, 12 preflight, suites/workerd/dry-run PASS. Логи/step results сверены;
это ранее выполненные CI, не новые прогоны от 2026-11-14.

Создан AUDIT_SPEC_PLAN_JOURNAL_2026-11-14.md с доказательствами и ограничениями.
SDK config/идентификаторы и локальные сборки больше не являются открытым блокером.
Следующий gate: owner signing/Play/App Check/private R2 → D3 STAGING E2E;
далее недостающие Driver/Admin сценарии и отдельные D4–D7 перед соответствующими
функциями. Реальные документы/биометрия/VoIP не собирались; verified не имитировался.

Изменения этого аудита — только документация. Новый APK build/full lint/Android
advisory scan/live Firebase/R2 checks/authenticated E2E/pentest не запускались.
Секреты, конфиги и бинарные артефакты в Git не добавляются. ТЗ, product code,
remote migrations, deploy, merge и production не меняются. D4–D7 не утверждены.

Документальные проверки завершены PASS: матрица содержит каждый §1–40 ровно один
раз, локальные Markdown-ссылки разрешаются, исходный ТЗ не изменён, кандидаты Git
содержат только Markdown и не содержат Firebase API key; git diff --check PASS.
PR mergeability на базовом snapshot clean: Android master
501481c20168c496e11ea21b7a7e82d047523788; backend main
7fad96a63ac963e6c28bf03f21d52b90e5cb69ff. Это не разрешение на merge.


## 2026-11-14 — Главное обязательное правило владельца

Владелец прямо установил:

> Действуй согласно утверждённого правила, ТЗ и записям журнала проекта. Любое отклонение запрещено! Додумывать за меня не нужно. Можешь только предложить варианты.

Требование принято без изменения смысла. Зафиксировано в корневом AGENTS.md
обоих рабочих репозиториев и в начале обоих журналов, чтобы последующие работы
начинались со сверки этого правила, ТЗ и утверждённых решений. Сводный план и
выводы аудита не являются разрешением на отступление или новым утверждением.
При неоднозначности/противоречии/недостающем решении зависящие действия
приостанавливаются, владельцу предлагаются варианты; выбор остаётся за ним.

Это документационная фиксация прямого требования владельца. Содержимое ТЗ,
application code и статусы утверждения D1–D7 не меняются. Merge, deploy,
remote migrations и изменения инфраструктуры не выполняются.
Отдельный инструмент постоянной памяти ChatGPT в текущем наборе инструментов
не доступен; постоянная проектная запись обеспечена файлами репозиториев.


## 2026-11-14 — G1 приостановлен владельцем; начало G2 / Transfer Admin

Прямое решение владельца: «G1 пока приостановим. Начни с G2».
G1 и его live probe не продолжаются. Предыдущий запрос запуска read-only probe
отменён пользователем; файл результата отсутствует, новый HTTP статус не утверждается.
Разрешено начало G2 в рамках D1–D3: отдельный Admin Java + XML, package
ru.transferservis.admin, раздельные app data/Keystore/session/signing.

Выявлено, что D1 относит MFA OWNER к первому этапу Admin, но backend метода ещё
не имеет. На вопрос «какой вариант подготовить для отдельного технического
утверждения» владелец выбрал вариант 1 — TOTP + backup codes. Это выбор направления
подготовки предложения, не утверждение ещё не представленных API/параметров.
Готовится ADMIN_MFA_TOTP_PROPOSAL.md. До утверждения/реализации MFA рабочие разделы
OWNER в новом клиенте не открываются. Независимая часть: отдельная сборка,
защищённая staff-сессия, авторитетный /v1/admin/me и read-only очередь регистрации
для dispatcher с drivers.read; новые backend контракты не придумываются.
D4–D7, production, merge/deploy/migration apply не разрешены и не меняются.


### 2026-11-14 — G2: первый функциональный срез Admin подготовлен

Добавлен native :admin Java + XML / ru.transferservis.admin. Отдельные app data,
prefs admin_secure_session_v1, Keystore alias transfer_admin_session_key_v1 и debug
signing identity (.local-signing/admin-debug.keystore, gitignored). Release/upload
keys не создаются. Firebase Driver конфиг и зависимости не используются Admin.

Текущий контракт без изменений backend: POST /staff/login, /staff/refresh,
/staff/logout; GET /v1/admin/me и /v1/admin/registrations?limit=50. Legacy auth role
admin не превращается в OWNER. Profile accountId должен совпадать с staff session;
operational role/permissions загружаются с backend и не сохраняются как grants.
OWNER видит собственный profile, но operational UI/queue закрыт до MFA. Dispatcher
получает только read-only очередь при актуальном drivers.read; backend повторно
проверяет права. Модерация, документы, назначения и остальные разделы ещё впереди.

Сессия AES-256-GCM/AAD с Admin package и backend origin; отдельный ключ, backup/
transfer запрещены, system CA only, redirects/retry запрещены, response body bounded
1 MiB. FLAG_SECURE, login/password/PII не сохраняются в View state; application logs
и внешняя телеметрия не добавлены. Logout очищает storage/ключ; stale refresh и
поздний 401 не переходят в новую учётную запись. Не подходящая legacy роль не
сохраняется; выданная отклонённая сессия отзывается best-effort.

Локальные Admin JVM tests: 17 debug + 17 staging + 17 release PASS. Полный lint
всех трёх вариантов — 0 issues. MissingApplicationIcon устранён platform icon;
для durable logout синхронный commit сохранён, точечная ApplySharedPref performance
подсказка помечена с причиной, не отключением security check. Тема — существующая
Material3 base Driver, без утверждения нового фирменного дизайна. Source guards:
5 Admin + прежние 7 Driver PASS. Debug/staging/release APK/AAB собираются, AndroidTest
APK с 13 новыми проверками подготовлен; финальные runtime/CI результаты ещё ожидаются.
В CI добавлены проверки разных packages/debug certificates и runtime UID isolation,
positive dispatcher queue/refresh/logout, revoke/role/MFA-gate/session races.

ADMIN_MFA_TOTP_PROPOSAL.md содержит конкретные RFC 6238 параметры, bootstrap/
recovery, challenge/API и server evidence policy. Статус НЕ УТВЕРЖДЕНО: выбор
TOTP для подготовки не означает согласие со всеми деталями. Backend MFA не внедрена,
его прежние OWNER endpoints этим клиентским ограничением не защищены — это остаётся
серверной задачей после отдельного решения владельца. G1 остаётся приостановлен.


### 2026-11-14 — Уточнение проверки сертификатов Build Tools 37

Локальный artifact verifier остановился на разборе fingerprint: новый apksigner
выводит «V2 Signer: certificate ...», а не старое «Signer #1 certificate ...».
Сама v2-подпись проверилась успешно. Исправлен форматный parser в verifier и CI:
извлекается именно certificate SHA-256 digest с проверкой единственного значения;
различие сертификатов Driver/Admin остаётся обязательным, проверка не отключена.
Application code/артефакты этим исправлением не меняются; CI перезапускается.
