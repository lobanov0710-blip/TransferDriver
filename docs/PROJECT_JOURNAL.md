# Журнал проекта Transfer Drivers

> **Главное правило владельца, 2026-11-14:** действовать строго по утверждённым правилам, ТЗ и журналу.
> Любые самовольные отклонения и решения за владельца запрещены. При неопределённости —
> только варианты для владельца; зависящие действия ждут его явного решения. [Правило для работы](../AGENTS.md).

> **Текущая работа:** по решению владельца G1 приостановлен; начат G2 / отдельный Transfer Admin.
> Параметры/API TOTP MFA OWNER явно утверждены; реализация в arch/g2-transfer-admin
> и backend arch/g2-owner-totp-mfa. Локальные проверки и source CI/runtime PASS; live E2E не выполнялся.
> **Актуальный индекс: 2026-11-15.** [План и матрица ТЗ](https://github.com/lobanov0710-blip/TransferDriver/blob/arch/g2-transfer-admin/docs/PROJECT_PLAN_V1_0.md) · [Базовый аудит 2026-11-14](AUDIT_SPEC_PLAN_JOURNAL_2026-11-14.md)
> Реализация частичная; production-выпуск не готов. SDK config/локальные сборки получены;
> подпись/Play/App Check/private R2 и живой E2E ещё не подтверждены. D1–D3 утверждены, D4–D7 нет.
> Ниже сохранена история: слова «текущий»/«отсутствует» внутри старого checkpoint относятся к его дате.

> **Последний завершённый срез:** Admin registration moderation; source CI d469b42c / 51d21da7 PASS.

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


### 2026-11-14 — Выделена ветка Admin и подтверждён первый срез G2

Владелец спросил о создании отдельной GitHub ветки для Admin; ветка создана:
https://github.com/lobanov0710-blip/TransferDriver/tree/arch/g2-transfer-admin
Начальная точка c33baab21acb6f57700a14bcda4d0fffff8de5c9 — уже опубликованный
текущий код Admin после исправления parser apksigner. История общей ветки не
переписана, первые Admin commits c9b144f3/c33baab2 в ней сохранены. Дальнейшая
работа Admin ведётся в arch/g2-transfer-admin; общая ветка получает только запись
журнала со ссылкой. Локальная работа переключена на новую ветку без потери изменений.

Проверенный CI того же точного source SHA (перед выделением ветки):
https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43587414797
job 132424791093, SUCCESS. Логи JUnit/guards сверены:
Admin JVM 17 debug + 17 staging + 17 release = 51 PASS; API 35 runtime 13 PASS;
Driver JVM 29+29+29 = 87 PASS, runtime 12 PASS. Failures/errors/skips отсутствуют.
5 Admin + 7 Driver source guards PASS. Full Admin lint debug/staging/release —
0 issues. Проверены разные packages и debug certificates; runtime подтвердил разные
UID, encrypted round-trip/logout, foreign AAD, driver prefs isolation, OWNER UI gate,
positive dispatcher queue/refresh/logout, permission revoke и stale refresh/401.
Backend docs-only checkpoint 2128ce18 / run 43587101473 SUCCESS; backend source
и auth/MFA API не изменялись. Локальная финальная Admin сборка: PASS, 194 tasks.

Локальные debug/staging/release APK, staging/release AAB и AndroidTest APK проверены:
Admin package, no shared UID, backup/cleartext disabled, network config, debug flag;
в Admin APK нет Driver/Firebase classes. Debug v2 signature verified; Admin debug
SHA-256 ea4f14836b45719946857ce90a3fd8399a088b6ecca76f0a82db26cfa9c71a8b
отличается от Driver. Staging/release APK/AAB unsigned. Хеши и внешняя source
provenance — docs/ADMIN_BUILD_2026-11-14.json; бинарные артефакты вне Git.

Незавершённые проверки не объявляются PASS: дополнительный runtime dependency
inventory command отменён пользователем при вопросе о ветке; лог не создан,
свежего OSV/advisory scan нет. Реальный backend E2E не выполнялся. G1 остаётся
приостановлен; Cloudflare/Firebase/remote migration apply/deploy/merge не выполнялись.
TOTP выбран для подготовки предложения, но ADMIN_MFA_TOTP_PROPOSAL.md ещё требует
явного утверждения параметров/API; server MFA не реализована. Client OWNER gate
не подменяет server enforcement. G2 завершён только в описанном первом срезе;
остальные Admin/Driver функции плана не объявляются готовыми.


## 2026-11-14 — Параметры и API TOTP OWNER утверждены; начата реализация

Прямое решение владельца: «Предложение параметров и API TOTP утверждаю».
Утверждён ADMIN_MFA_TOTP_PROPOSAL.md: RFC 6238 SHA-1/6 digits/30s ±1, 20-byte seed,
AES-256-GCM server encryption, одноразовые counters/challenges/recovery, TTL/limits,
protected control-plane bootstrap/reset, evidence для OWNER sessions и legacy paths.
Это отдельное подтверждение MFA к D1; D4–D7 не утверждаются этим решением.
G1 остаётся приостановлен. Merge/deploy/remote migration apply не разрешены.

Backend MFA разрабатывается в новой arch/g2-owner-totp-mfa от
731e313c9e5caa116c8a092729f6fef79e119407. Android остаётся в arch/g2-transfer-admin.
План: добавить local-only миграцию/crypto/transactions/enforcement и отрицательные
SQL/workerd tests, затем подключить Admin login/enrollment/recovery без сохранения
seed/кодов в Android persisted state. Реальный OWNER key/seed/credentials не создаются
и не запрашиваются в чате. Новые проверки пока не объявляются PASS.

### 2026-11-14 — G2: реализация утверждённого OWNER MFA и локальные проверки

Добавлены серверные password challenge без session, enrollment через защищённый
local control plane, TOTP/recovery login, централизованное evidence enforcement
для всех OWNER staff paths, включая legacy, и сохранение предела MFA при refresh.
Новая additive migration 0018 применяется только к локальным тестовым DB.
Одноразовый proof/challenge, session, evidence и audit фиксируются атомарно;
сбой audit откатывает выдачу. Bootstrap/reset не имеют public HTTP endpoint.

Admin подключён к утверждённым API: challenge/enrollment/recovery находятся только
в volatile state; seed/коды не сохраняются в Bundle/preferences/clipboard/logs,
очищаются при уходе в фон. OWNER доступен только после typed server MFA profile.
Отмена/поздний ответ не восстанавливает старую session; dispatcher auth сохранена.

В проверках исправлены: привязка количества numbered SQL bindings к native D1;
неподходящий запуск unbundled Worker в harness; фиксированные rate buckets заменены
скользящими интервалами с проверкой календарной границы. Старые router mocks явно
содержат действующее server evidence; отсутствие/просрочка отдельно отклоняются
на настоящих SQLite и public HTTP тестах. Защита не отключалась.

Локально PASS: 65 backend test scripts, включая 18 новых MFA scenarios;
local workerd HTTP/D1 (enrollment/login/profile, concurrent TOTP/recovery — один
победитель, refresh expiry/reset); Worker dry-run без deployment.
Admin: 23 JVM test на каждом debug/staging/release = 69 PASS, failures/errors/skips 0;
194 Gradle tasks, debug/staging/release APK, staging/release AAB и AndroidTest APK
собраны; полный lint трёх variants — 0 issues. Новые 20 Admin runtime tests пока
только скомпилированы: их выполнение и полный source CI ещё ожидаются.

Диагностика среды: CLI child process и local workerd первоначально получили sandbox
EPERM при loopback; повторены через штатное дополнительное разрешение сети.
Первый dry-run не смог создать Wrangler config вне workspace; XDG_CONFIG_HOME
перенесён в доступный workspace, повторный dry-run PASS. Никаких remote действий.
G1 приостановлен, реального OWNER encryption key/seed/credentials нет; live E2E,
production deployment, merge и remote migration apply не выполнялись.


## 2026-11-15 — G2 OWNER MFA: итог CI и аудит зависимостей

Прямое утверждение владельца параметров/API TOTP от 2026-11-14 выполнено в
отдельных ветках arch/g2-transfer-admin и arch/g2-owner-totp-mfa.
Общие arch/spec-v1-* ветки получают только эту запись журнала; код MFA в них
не переносится. D4–D7 по-прежнему не утверждены, G1 остаётся приостановлен.

Проверены job logs точных source SHA:
- Android c6badd3a13bdad89140e43df09b181bfd85ec069:
  https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43608123183
  job 132487059220 — SUCCESS. Admin JVM 23 debug + 23 staging + 23 release = 69;
  API 35 runtime 20. Driver JVM 29+29+29 = 87, runtime 12.
  Executed reports без failures/errors/skips. Проверены volatile seed/recovery
  в настоящей Activity, отмена/поздние ответы, authoritative server evidence,
  разные package/UID/debug certificates, endpoint locks и session isolation.
  5 Admin + 7 Driver guards PASS; полный Admin lint трёх variants — 0 issues.
- Backend 1598dff3e17799947c4eedc79f3c22ae68a8a256:
  https://github.com/lobanov0710-blip/uber-v3/actions/runs/43608125351
  job 132487065843 — SUCCESS. Выполнены 65 test scripts, включая 18 новых
  MFA scenarios; 18 local migrations, 12 preflight — PASS.
  Lockfile install и npm audit: 0 vulnerabilities.
  Оба native workerd/D1 harness и Worker dry-run PASS без deployment.
  Проверены HTTP enrollment/login/profile, один победитель concurrent TOTP/recovery,
  replay, expiry, reset, rollback при audit failure и sliding failure limits.

После прерывания прежнего дополнительного OSV-запроса его результат не
объявлялся PASS. Повторный запрос выполнен 2026-11-15: фактически разрешены
6 runtime configurations app/admin debug/staging/release, 152 уникальных Maven
components; 0 опубликованных OSV advisories на момент проверки. Это не pentest
и не полный аудит build plugins. Новых dependencies для MFA нет.

Локально подготовлены и проверены 6 новых Admin artifacts:
debug/staging/release APK, staging/release AAB и AndroidTest APK.
Проверены manifests, no shared UID, backup/cleartext disabled, отсутствие
Driver/Firebase classes, разные debug signatures, unsigned staging/release,
variant endpoint locks и SHA-256. Новый набор хранится отдельно:
 /workspace/artifacts/transfer-admin-mfa-2026-11-14/
Старый первый набор не перезаписан. Debug APK: 9018498 bytes,
SHA-256 01a6198216124b122271685cc2a7f084249d6c96989089ca6b152033962529d7.

Дополнительный artifact verifier первоначально ошибочно требовал отсутствия
PRODUCTION строки в debug DEX: debug содержит обе публичные константы origin
policy. Проверка уточнена по фактическому BuildConfig/endpoint policy;
minified staging/release содержат только свой endpoint. Это диагностический
false positive; продуктовый код из-за него не менялся.

Созданы Draft PR для отдельного просмотра изменений:
- Admin https://github.com/lobanov0710-blip/TransferDriver/pull/7
- Backend https://github.com/lobanov0710-blip/uber-v3/pull/29
Слияния нет. Расхождение только журнала в Admin PR согласовано сохранением всей
истории в общей и Admin ветках; исходный код не меняется, история Git не переписана.

Во время финализации execution environment стала unavailable. Статус Cloud
environment сообщает running/connected, но exec_command по-прежнему возвращает
"No usable execution environment is available". Изменения среды/прав не делались.
Все source/test commits и CI logs уже находятся в GitHub.
Публикация полного локального build receipt, component inventory и локальная
Git checkpoint-сверка отложены до восстановления execution environment;
они не объявляются завершёнными. Сводный проверяемый отчёт опубликован отдельно:
docs/OWNER_MFA_VERIFICATION_2026-11-15.json в ветке Admin. Дальнейшие локальные
сборки требуют доступной execution environment; проверки не отключаются.

ТЗ не изменено: blob 7434dc0b1548b4f2c5382f12d6cc5399f93d0a84.
Перед потерей execution environment проверено совпадение всех изменённых
функциональных файлов с протестированными GitHub source SHA; последующие
изменения касаются только документов. Реальные OWNER key/seed/credentials
не создавались и не публиковались. Миграция 0018 применена только к эфемерным
локальным test DB. Remote migration apply, deployment, merge и live E2E
не выполнялись. Полный G2 и production-выпуск не объявляются готовыми.



## 2026-11-15 — Среда выполнения восстановлена; отложенные отчёты доступны

После повторного подключения exec_command снова работает. Тот же cloud environment
имеет running/connected; workspace /workspace доступен для чтения/записи.
Проверены Node 24.22.1, Python 3.12.14, сохранённый JDK 25.0.3. Gradle 9.6.0
успешно запускается с ранее установленными project env paths; SDK platform
android-37.0 и Build Tools 37.0.0 присутствуют. Первый запуск без GRADLE_USER_HOME
попытался создать wrapper lock в недоступном /home/agent/.gradle; повтор с
/workspace/.gradle-transferdriver PASS. Первоначальная диагностическая проверка
ожидала android-37; фактический установленный SDK называется android-37.0.
Это уточнения команд проверки среды, продуктовый код не менялся. Оба audit checkout,
SDK/cache и старые логи/артефакты сохранились; права, secrets и конфигурация среды
не менялись. Повторная проверка SHA-256/размеров всех 6 новых Admin artifacts и
35 исходных файлов из build receipt — PASS. Старый набор артефактов сохранён.

Восстановлены подготовленные до потери среды полные документы Android ветки:
- docs/ADMIN_MFA_BUILD_2026-11-14.json — все 6 файлов, размеры, SHA-256, source
  provenance, debug certificates и результаты CI; копия receipt рядом с бинарными файлами.
- docs/ADMIN_DRIVER_RUNTIME_AUDIT_2026-11-15.json — полный разрешённый inventory
  6 app/admin runtime configurations, 152 уникальных Maven components, OSV 0 advisories
  на дату исходного запроса. Новый OSV-запрос при восстановлении не выполнялся.
- docs/OWNER_MFA_VERIFICATION_2026-11-15.json и план обновлены: блокер execution
  environment снят; опубликованы ссылки на полный receipt и inventory.

Сверена актуальная GitHub документация после прошлого docs-only checkpoint;
её история сохранена. Непубликованные локальные варианты документов перед сверкой
сохранены в /workspace/scratch/pre-recovery-docs-2026-11-15/. Функциональный код
после успешных source CI c6badd3a / 1598dff3 не изменялся; повторные полные тесты
только из-за восстановления среды не запускались. Эта запись не является новым E2E.
G1 остаётся приостановлен. Merge, deploy, remote migration apply и создание
реальных OWNER keys/credentials не выполнялись. Локальная фиксация исходников
и сверка дерева выполняются отдельно от изменения удалённых рабочих веток.


## 2026-11-15 — Утверждена рекомендация продолжить G2; начало Admin moderation

Владелец: «Продолжаем согласно рекомендации». Принята предложенная очередность:
Admin moderation → Driver registration/vehicles → availability/manual assignment,
с проверками и фиксацией результатов каждого законченного сценария. Текущий срез —
Admin карточка регистрации, поэлементные решения, возврат с причиной и история.
G1 не возобновляется, D4–D7 этим не утверждаются. Только синтетические fixtures.

Проверены ТЗ §§28–35, D1–D3 и существующие review/resource APIs. История уже входит
в review bundle; новые history endpoints/таблицы не требуются. Найдены пробелы:
queue filter исключает approved/rejected из пяти требуемых очередей;
private document read не требует отдельного documents.read_sensitive. Исправления
входят в текущий срез вместе с отрицательными тестами. Ручное identity verified
по-прежнему запрещено; просмотр/одобрение не заменяет D4 provider result.
Изменения разрабатываются в текущих отдельных G2 ветках; merge/deploy/remote
migration не выполняются. Результаты новых тестов пока не объявляются PASS.


### 2026-11-15 — Admin moderation: код и локальные проверки подготовлены

UI: пять фильтров (submitted/under_review/changes_required/approved/rejected),
карточка и история, начало проверки, отдельные решения по документам/фото/авто,
итоговое решение. Подтверждение документа/фото требует предварительного preview;
identity verified вручную недоступно, итоговое одобрение зависит от backend readiness.
Preview — только bounded JPEG/PNG/WebP в памяти, без URL/export/cache/внешнего viewer.
Карточка/причина/image/pending action исключены из saved state и очищаются в фоне.
Каждый вызов читает свежие permissions; response/session binding предотвращает
переход позднего запроса на другую учётную запись. POST не повторяется через refresh;
явный повтор при неизвестном результате сохраняет исходные actor/body/revision/key.

Backend: approved/rejected разрешены как явные queue filters; default active queue
не изменена. Для sensitive document read требуется отдельное documents.read_sensitive
вместе с соответствующим verify_documents/verify_identity. Три review mutation JSON
ограничены 4096 bytes; для возврата/отклонения причина обязательна и на сервере.
Новых routes, migrations, dependencies и identity provider нет.

PASS: Admin JVM 31 debug + 31 staging + 31 release = 93, failures/errors/skips 0;
full lint всех трёх variants — 0 issues. Backend 66 test scripts — PASS, включая
3 новых HTTP/SQLite scenarios. Native local workerd HTTP/D1 сценарий — PASS:
partial return сохраняет verified элементы, replay не дублирует audit, отзыв прав
блокирует повтор, ручное identity approval запрещено. Аудит failure rollback
и oversized/missing reason также проверены через настоящий router/SQLite.
Новые Android runtime tests скомпилированы; их выполнение ожидается в source CI.
Финальная APK/AAB сборка ещё выполняется, артефакты пока не объявлены готовыми.

В ходе проверок исправлены устаревшее ожидание теста (approved ранее был запрещён)
и единственный lint UnusedResources без подавления проверок. Сообщение
ADMIN ITEM MODERATION ERROR ожидаемо в negative test с намеренным audit failure;
оно не содержит DB exception/PII. Security guards 5 Admin + 7 Driver PASS.
G1 приостановлен; remote DB/R2, реальные документы/credentials, merge/deploy не трогались.
Ограничения: очередь показывает до 50 записей запроса, history до 200; остальные
Admin разделы, Driver исправления и живой E2E остаются дальнейшими этапами G2/G1.


### 2026-11-15 — Android runtime CI выявил сбой; расширена диагностика

Android source 894fe1c5 / CI 43702428479: JVM/build/guards/signatures PASS,
connected Admin runtime FAILED; runtime PASS не заявляется. Failure-report step
ранее печатал только app/Driver XML, поэтому подробности Admin отсутствуют.
Добавлен вывод обоих отчётов в существующий failure step. Это изменение диагностики,
не отключение/пропуск тестов. Новый CI нужен для точной причины и исправления.


### 2026-11-15 — Причина Android runtime failure установлена

Diagnostic source dd4911bb / CI 43703195348 вывел Admin XML: 27 tests,
26 PASS, один failure в новом moderation UI test. Espresso RootViewPicker
выбрал BASE_APPLICATION root без window focus при открытом AlertDialog
(ModerationUiInstrumentationTest.java:60, RootViewWithoutFocusException).
Оба действия подтверждения теперь явно ищут isDialog() root, сохраняя реальный
Espresso click и ожидание focus. Focus/security checks, FLAG_SECURE и остальные
27 тестов не ослабляются/не пропускаются. Все 5 repository moderation tests,
background/saved-state privacy test и прежние MFA/isolation tests уже PASS.
App code этим test-only исправлением не меняется; новый полный CI ещё ожидается.


### 2026-11-15 — Строгая проверка типа причины при финальном review

После первого source CI backend d1f9e1e / 43702432031 SUCCESS дополнительный
проход выявил: normalizeNote преобразовывал JSON object/boolean/number в строку.
Теперь причина принимает только string (null/undefined остаются отсутствием note);
возврат/отклонение без непустой строки запрещены. Расширены реальные HTTP tests
неверных типов; 3 HTTP scenarios, moderation policy и replay regressions PASS.
Изменённый backend source требует свежего полного CI; прошлый PASS за него не выдаётся.
Android source 894fe1c5 и его APK этим серверным исправлением не меняются.


## 2026-11-15 — G2 Admin moderation: итоговая проверка и готовые артефакты

Проверенный Android source d469b42c60fd1c6c33ba91120920e0d1aa709929:
https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43704273792
job 132781893105 — SUCCESS. Admin JVM 31+31+31 = 93, API 35 runtime 27;
Driver JVM 29+29+29 = 87, runtime 12. Executed reports без failures/errors/skips.
Выполнены все новые repository/UI tests: fresh permissions, sensitive read denial,
явный retry после потерянного acknowledgement с тем же body/key, partial return,
401/account replacement, snapshot-bound image, saved-state/background privacy.
Полный lint Admin debug/staging/release — 0 issues. Security guards 5+7,
package/signature/UID isolation, endpoint locks и Driver App Check policy PASS.

Проверенный backend source 51d21da7e7338a7b3a85982c06922fc200963e80:
https://github.com/lobanov0710-blip/uber-v3/actions/runs/43702881770
job 132777627061 — SUCCESS. 66 test scripts, 18 local migration smoke checks,
12 preflight tests; три native workerd/D1 harness (availability, OWNER MFA,
moderation HTTP workflow), Worker dry-run — PASS. Npm install/audit: 0 vulnerabilities.
Внутри нового HTTP suite 3 сценария включают audit rollback, reason/type/body bounds;
никакие security checks не отключались. Предыдущие Android CI failures 43702428479
и diagnostic 43703195348 сохранены в истории выше и не выданы за PASS.

Новый итоговый набор из 6 файлов:
/workspace/artifacts/transfer-admin-moderation-verified-2026-11-15/
Debug/staging/release APK, staging/release AAB и AndroidTest APK проверены:
manifest/package, no shared UID, backup/cleartext disabled, разные Admin/Driver
certificates, v2 signature debug и AndroidTest (cert совпадает с Admin), unsigned
staging/release APK/AAB, правильные variant endpoints, отсутствие Driver/Firebase
classes и SHA-256 всех файлов. Source hashes 45 файлов сохранены.
Первоначальный набор transfer-admin-moderation-2026-11-15 и старые MFA/первый
Admin наборы не перезаписаны. Полный receipt:
docs/ADMIN_MODERATION_BUILD_2026-11-15.json в Android ветке, копия рядом с APK.
Dependencies не менялись; Android OSV inventory от 2026-11-15 (6 configurations,
152 components, 0 advisories на момент запроса) сохранён и не выдаётся за новый scan.

Draft PR #7 TransferDriver / #29 uber-v3 обновлены под итоговый объём: OWNER MFA
и registration moderation. Подробный API/permissions/retry контракт — backend
/docs/ADMIN_MODERATION_API.md. После проверенных source SHA публикуются только
документы/receipt. Общие baseline-ветки получают только журнал, код moderation
остаётся в отдельных G2 ветках. История Git не переписывается.

ТЗ не менялось: blob 7434dc0b1548b4f2c5382f12d6cc5399f93d0a84.
Этим завершён описанный Admin moderation срез, а не весь G2/production.
Ограничения: до 50 строк очереди без cursor pagination, до 200 review events;
остальные Admin разделы и Driver анкета/автомобили/исправления/availability ещё
не завершены. Следующий утверждённый шаг рекомендации — Driver registration/
vehicles UI на синтетических данных с серверными статусами и revisions.
D4–D7 не утверждены. G1 приостановлен. Реальные документы/биометрия, storage/OWNER
secrets, live server E2E, remote migrations, merge и deploy не выполнялись.


## 2026-11-15 — Подготовка OWNER phone test на STAGING по решению владельца

Владелец поручил подготовить настройку для тестирования на телефоне и уточнил:
«Только Admin/OWNER на STAGING; Driver G1 оставить на паузе»; приватные действия
выполняются на его Windows PowerShell. Полный Driver G1 не возобновляется.
Отдельно утверждён TOTP на том же телефоне: в RAM можно удерживать только служебные
challenge/enrollment IDs до их исходного срока в 5 минут; seed/введённые коды
очищаются, сохранения на диск нет, незавершённые сетевые операции отменяются.
Это явное уточнение владельца к прежнему полному сбросу transient MFA flow в фоне.

Cloudflare CLI этой среды: not authenticated. Текущий backend STAGING независимо
не подтверждён: два unauthenticated GET получили HTTP 403 text/plain от envoy,
а не JSON Worker; это не свидетельство состояния deployed API/БД.
Remote DB/account/secrets не читаются по догадке и не изменялись.

Подготовка: отдельный fail-closed OWNER-only STAGING scope (без Driver/public/legacy
операций), защищённый local Windows CLI control plane без public bootstrap/reset
endpoint, точные target guards и проверяемый deployment plan. Общий D3 preflight
не ослабляется; отсутствие R2/Firebase не объявляется готовностью Driver E2E.
До отдельного подтверждения выполняются только локальная подготовка и тесты.


### 2026-11-15 — OWNER phone: реализация и локальные проверки

Выполнено утверждённое уточнение same-phone TOTP: opaque IDs/исходный TTL
сохраняются только в RAM, seed/поля ввода очищаются, сохранение view state
отключено. Срок контролируется также monotonic clock, переключение его не
продлевает; таймер/возврат в foreground очищает просроченные IDs. Pending auth
отменяется через OkHttp, generation исключает позднее сохранение сессии.
Recovery-коды по-прежнему не удерживаются при уходе в фон.

OWNER-only STAGING закрывает Driver/public/legacy/private-resource routes,
non-OWNER login/refresh/authorization и Driver/support cron jobs. Существующие
регистрации скрыты. Fixture/data upload не создаются. Публичного bootstrap/reset
нет. Windows CLI закреплён за известными STAGING DB/KV/host, вывод провайдера
не печатается; секрет передаётся через stdin, password verifier/bootstrap digest
записываются атомарным D1 import с audit. TTL bootstrap берётся по часам БД.
Существующий OWNER/factor/key не заменяется. Подготовлен Windows runbook; первый
этап — read-only plan. До разбора фактического плана remote writes не разрешены.

Локально: Admin debug/staging/release сборки, JVM 31×3, три lint — PASS; guard
проверки Admin 5 и Driver 7 — PASS. Backend полный npm test (67 test scripts),
миграции 18, неизменённый D3 preflight 12 tests, native workerd/D1 availability,
OWNER MFA, moderation и новый OWNER phone flow — PASS; npm audit 0 vulnerabilities;
Worker bundle dry-run PASS. Новый ownerPhoneSetup: 8 tests PASS (exact target,
route/role/data isolation, atomic provisioning/audit rollback, no factor reset,
scheduler isolation, private CLI non-TTY, unexpected bindings).

В ходе проверки child-process CLI test сначала не выполнился: sandbox EPERM при
spawnSync; повтор с разрешённым network capability выполнил реальный subprocess
и прошёл. Исправлен scheduler test: вместо проглатываемого mock batch error
проверяется успешный batch ровно четырёх MFA-only SQL statements. Android runtime
ещё требует свежего CI; локальная компиляция AndroidTest не выдаётся за его запуск.
Remote migrations, secrets, account provisioning, deployment и merge не выполнялись.


### 2026-11-15 — OWNER phone: финальная проверка и пакет для владельца

Android source 8a948591e6fa8bbbc3dd0b79f25ce27913761323, CI 43714614881 /
job 132813325050 — SUCCESS. Admin: JVM 31×3=93, API35 runtime 29, lint 0
issues во всех трёх вариантах. Driver regression: JVM 29×3=87, API35 runtime 12.
Итого 180 JVM и 41 runtime test, без failures/errors/skips. Required-name guard
подтвердил выполнение samePhoneSwitchPreservesChallengeWithoutCodesOrExtendedExpiry,
enrollmentBackgroundKeepsOnlyOpaqueIdsAndNeverSeed, retainedChallengeExpiresWithoutAnotherNetworkRequest
и прежних MFA/session/moderation checks. Endpoint locks, независимые signatures/UID,
Play Integrity policy и security guards — PASS.

Backend source 550d2dda8f86242a8608b9daaee2c64bff0e9c49, CI 43714618395 /
job 132813334993 — SUCCESS. 67 test scripts (новый ownerPhoneSetup 8 tests),
18 migration files, 12 D3 preflight tests, четыре native workerd/D1 harnesses,
Worker dry-run и npm audit 0 vulnerabilities. D3 gate не ослаблен.

В отдельной папке artifacts/transfer-admin-owner-phone-2026-11-15 приготовлены
новый signed debug APK для STAGING, unsigned staging/release APK/AAB и test APK,
receipt, SHA256SUMS и owner-phone-setup.zip. Прежние наборы не перезаписывались.
Проверены manifest/package/no-backup/TLS/endpoint, отсутствие Firebase/Driver
classes, APK v2 signature; AndroidTest подписан тем же Admin сертификатом,
Driver — другим. Проверка содержимого всех архивов не обнаружила загруженный
Firebase SDK key Driver (само значение не выводилось).

Setup ZIP: 177 файлов, 95 source hashes; нет production wrangler.toml, .git,
node_modules, credentials или private config. Распакованный пакет прошёл private
TTY init с синтетическим public Account ID: только локальный exact-STAGING config,
без сети/remote writes. Подмена source file блокирует init до config/network.
Windows PowerShell непосредственно в этой Linux-среде не запускался; реальная
авторизация Cloudflare и hosted-state не проверены и не объявляются готовыми.

OWNER_PHONE_STAGING_WINDOWS.md описывает первый шаг владельца: npm ci --ignore-scripts,
init, Cloudflare browser login, read-only plan. В чат — только
.owner-phone/admin-phone-plan.public.json. Изменяющие schema/mfa-key/deploy/owner
шаги после разбора отчёта и отдельного утверждения; аккаунт/пароль/factor не
заменяются автоматически. Публичного bootstrap/reset и обхода MFA нет.

Сборки debug предназначены для теста, release/upload key ещё не создан.
Live server E2E, private storage/реальные материалы и тест на телефоне владельца
не выполнены. Dependencies не менялись; прежний OSV inventory 152 components/
6 configurations/0 advisories не выдаётся за новый scan. G1 на паузе, D4–D7
не утверждены. ТЗ не менялось (blob 7434dc0b1548b4f2c5382f12d6cc5399f93d0a84).
Публикуются только рабочие feature ветки и journal-only baseline updates; merge,
remote migrations/secrets/OWNER provisioning и deployment не выполнялись.


## 2026-11-15 — Получен и разобран OWNER phone public plan

Владелец передал admin-phone-plan.public.json, SHA256
1ee6fadc8b3a3f375c66f5d15a48a84a528ed23515beaf20dbf715bffdc41726.
Файл разобран как данные без выполнения содержимого. Source 550d2dda8f86242a8608b9daaee2c64bff0e9c49,
Worker/host/D1 совпадают с утверждённым STAGING; единственная pending migration
0018_owner_totp_mfa.sql. По отчёту OWNER binding отсутствует, MFA factors 0,
JWT_SECRET и ADMIN_MFA_ENCRYPTION_KEY отсутствуют; remoteWritesPerformed=false,
G1 остаётся приостановленным. Отчёт не подтверждает отсутствие staff accounts.
KV ID/value APP_ENV не входят в public JSON, exact check выполняет ранее выданный
CLI; независимое live чтение из среды ассистента не проводилось.

Обнаружен пробел setup: deploy правильно блокируется без JWT_SECRET, но нет
отдельного безопасного action создания. Подготовлен jwt-key с независимыми
CSPRNG 32 bytes через Wrangler stdin; значения не выводятся/не сохраняются и
не передаются в argv. Существующие JWT/MFA keys не ротируются, чужие secret names
запрещены, без reviewed schema/typed confirmation записи невозможны, MFA factors
без исходного key не сбрасываются. Remote записи не выполнялись.

Предложен конкретный этап для утверждения: 0018 → jwt-key → mfa-key → plan →
OWNER-only deploy → приватная настройка выбранного владельцем OWNER account/TOTP.
Подробный разбор: OWNER_PHONE_PLAN_REVIEW_2026-11-15.md. Передача файла сама по
себе не разрешает этот этап; прежняя read-only граница сохраняется. APK/Android
код и контракт API не меняются. Production/G1/R2/Firebase/merge вне объёма.

Локально ownerPhoneSetup — 11 tests PASS: добавлены crypto roundtrip JWT/MFA
для независимых ключей, stdin-only transport, сохранение existing secrets,
pending schema/unknown name/factor guards, denied confirmation и отсутствие
автоповтора ambiguous provider failure. При подготовке нового теста сначала
перепутан порядок аргументов signJWT/verifyJWT; исправлен вызов теста по существующей
сигнатуре, код JWT не изменялся. Свежий backend CI ещё ожидается; прежний Android
CI остаётся применим к неизменённому APK/source.

Уточнение безопасности порядка: установленный Wrangler 4.125.0 secret put
использует PUT /workers/scripts/.../secrets, относится к действующему Worker и
предупреждает о риске accidental deployment при работе с versions. Чтобы новый
JWT не активировал прежние маршруты до ограничения scope, предыдущая предложенная
последовательность заменена на 0018 → isolate → jwt-key → mfa-key → plan → owner.
Это исправление подготовки, удалённые действия не выполнялись. `isolate` требует
OWNER=null, factors=0, обоих отсутствующих ключей и applied schema; обычный deploy
сохраняет ключевые readiness guards. Перед каждым новым secret put проверяются
HTTP scope header/закрытые Driver routes. Дополнительные tests проверяют оба
варианта deploy, запрещённые initial states и отсутствие secret writes при
неподтверждённой изоляции; локально 13 tests PASS. В ходе правки test source
исправлена лишняя закрывающая скобка, найденная node --check; production JWT код
и Worker runtime не изменялись.


### 2026-11-15 — Разбор плана и обновлённая OWNER setup подготовка завершены

Итоговый setup source 01bbcc8acf5228ba7735969a8fbbf0cb4e8671ba, backend
CI 43719538291 / job 132828305901 — SUCCESS: 67 test scripts, ownerPhoneSetup
15 tests, 18 migrations, 12 unchanged D3 preflight tests, четыре native workerd/D1
harnesses, Worker dry-run и npm audit 0 vulnerabilities. Предшествующий source
6ec22a78 / CI 43719298434 (14 phone tests) тоже прошёл; последняя правка дополнительно
принудительно включает WRANGLER_LOG_SANITIZE=true. Реальный child-process test
подтверждает sanitization даже при inherited false. Raw provider output не
выдаётся пользователю; временный private log удаляется.

Обновлённый архив хранится отдельно: transfer-admin-owner-phone-jwt-2026-11-15/
owner-phone-setup.zip, 179 файлов, 96 protected source hashes. Проверен после
распаковки: private TTY init создаёт exact STAGING config только локально с
синтетическим Account ID; подмена source блокируется до config/network. ZIP
не содержит production config/credentials/.git/node_modules или загруженный
Driver Firebase SDK key. SHA256 и tested-source provenance —
OWNER_PHONE_SETUP_BUILD_2026-11-15.json и SHA256SUMS. Предыдущий пакет сохранён.
Исходный Admin APK не менялся (source 8a948591, CI 43714614881). Worker src,
миграции и зависимости также не изменялись; исправления только control plane/tests/docs.

Итоговый предложенный порядок после явного утверждения: schema(0018) → isolate
(подтверждённое пустое OWNER/key state) → jwt-key → mfa-key → plan → owner.
Ни secret put, ни Owner provision не выполняются без проверки OWNER-only scope.
Точный account/пароль выбирает владелец в приватном терминале. Cloudflare remote
write/deploy/merge не выполнялись; реальный Windows run этого обновления, live
server E2E и телефон владельца ещё не проверены. G1 на паузе.

Прерванный пользовательским сообщением командный вызов не применил правку
документов; это проверено по состоянию файлов и запись повторена. Команда
«продолжаем» обработана как продолжение текущей подготовки; remote этап остаётся
на отдельной границе подтверждения из выданного runbook. В journals сохраняется
история промежуточного порядка, актуальная последовательность указана выше.


## 2026-11-15 — Владелец явно утвердил remote STAGING этап

Ответ владельца: «утверждаю» на конкретный этап после разбора public plan.
Разрешены: применение единственной pending 0018_owner_totp_mfa.sql; развёртывание
OWNER-only режима на uber-v3-staging с проверкой закрытия Driver/public/legacy/
private-file routes; создание независимых отсутствующих JWT_SECRET и
ADMIN_MFA_ENCRYPTION_KEY; приватная настройка выбранного владельцем OWNER
account/TOTP и проверка входа на телефоне. Актуальная последовательность:
schema → isolate → jwt-key → mfa-key → plan → owner. Повторное согласование
этого объёма в чате не требуется. Точные private CLI phrases остаются защитой
от выбора неверной среды. Account/password/seed/bootstrap/recovery — только
в приватном терминале/приложении владельца, в журнал не включаются.

Цель неизменна: Worker uber-v3-staging / утверждённые STAGING DB/KV. Production,
merge, возобновление Driver G1, R2/Firebase и password/factor/key reset не
разрешены этим ответом. При отличии фактического состояния работают существующие
fail-closed guards; их обход не разрешён. Выполнение в согласованном режиме:
Windows PowerShell владельца, его существующая приватная Cloudflare авторизация.
Проверенный пакет source 01bbcc8acf5228ba7735969a8fbbf0cb4e8671ba /
CI 43719538291 SUCCESS (15 setup tests и полный backend набор); ZIP SHA256
27a6469315a5a9d841dd0269d1154648f5be6453be5842008a2f6fb649962ff7
повторно сверен перед передачей команд. Код/APK/артефакты не меняются.

Подготовлена OWNER_PHONE_STAGING_APPROVED_STEPS_2026-11-15.md с командами
в согласованном порядке и точными фразами. Из среды ассистента remote actions
не выполнялись: приватные действия исполняет владелец на своём компьютере.
Утверждение записано как разрешение, а не как свидетельство успешного deployment.
Факт migration/isolation/secrets/OWNER/TOTP/телефона будет записан после
безопасного отчёта о выполнении; при ошибке — action name и plan без секретов.
G1 остаётся на паузе.


## 2026-11-15 — Разобрана остановка private CLI на init

Владелец сообщил фиксированное OWNER PHONE SETUP FAILED at init и уточнил:
ошибка после ввода public Account ID; Test-Path wrangler.admin-phone.staging.json
возвращает True. До запроса ID уже пройдены private TTY/source manifest checks.
В коде init запись выполняется writeFile(flag='wx'), существующий config не
перезаписывается. В распакованном проверенном ZIP с synthetic Account ID
воспроизведено: первый init PASS, повторный init после ID выдаёт тот же generic
failure; байты ранее созданного config не изменяются, remote calls отсутствуют.
Это подтверждает блокировку повторного init при существующем config; исходный
скрытый exception пользователя независимо не получен, Account ID в чат не запрошен.

Безопасный следующий шаг — пропустить ненужное создание и выполнить plan.
Plan заново проверяет exact STAGING config/source/bindings/schema и не делает
remote writes. Config не удаляется/не перезаписывается, private TTY и source
checks не отключаются. После успешного plan владелец продолжает уже утверждённый
этап с schema; нового согласования не требуется. При ошибке plan — только action
name/public report без raw credentials/provider output. В инструкциях добавлено
пояснение про повторный init. Код/пакет/подпись/API не меняются; прежние 15 setup
tests/CI 43719538291 остаются применимы, заново за исполненные не выдаются.
G1 на паузе; migration/deploy/keys/OWNER/phone success не подтверждены этим
сообщением. Remote действия из среды ассистента не выполнялись.


## 2026-11-15 — После existing-config init выполнен read-only plan

Владелец прислал успешный CLI итог: Reading the exact STAGING target; credentials
and provider output stay private / READ-ONLY plan complete. Это результат
plan после рекомендации использовать уже существующий config вместо повторного
init. Config не удалялся/не перезаписывался по инструкции; факт байтового
состояния файла на ПК владельца ассистентом не проверялся. В сообщении нет
обновлённого public JSON: конкретные текущие pending/owner/key flags и source SHA
по одному stdout не переутверждаются. CLI plan по своему контракту remote writes
не выполняет; deployment/schema/keys/OWNER/TOTP/телефон этим не подтверждены.

Фраза Do not run write actions before approval — стандартный текст CLI.
Явное утверждение владельца от 2026-11-15 уже записано и действует на
schema → isolate → jwt-key → mfa-key → plan → owner только STAGING; нового
согласования этого объёма не требуется. Следующие команды выдаются с условием
последовательного успешного исполнения и сохранением всех exact-target/schema/
route-isolation guards. Сначала schema (единственная допустимая pending 0018),
потом isolate и лишь после подтверждения закрытых routes — ключи/OWNER.
При ошибке остановка и безопасная диагностика без вывода credentials.
Фактические изменения на Cloudflare выполняет владелец в private PowerShell,
а не среда ассистента. G1 остаётся на паузе. Код/пакет не менялись.


## 2026-11-15 — OWNER остановился на строчном n; причина установлена

Владелец сообщил generic OWNER PHONE SETUP FAILED at owner, затем уточнил:
ошибка до PROVISION, последним заполнен только выбор N/E. В присланном тексте
видно `...? n`, после чего failure. В первоначальном CLI choice сравнивался
строго с 'N'/'E'; строчная n попадала в Explicit account choice required.
На этом участке owner provisioning не отправлял import SQL, account/binding/
bootstrap не создавались. Это относится к данному запуску owner, не отрицает
ранее выполненные приватные schema/isolation/key шаги; их итог отдельно не получен.

Немедленное безопасное продолжение текущим проверенным пакетом: повторить owner
и ввести один символ N латиницей, без кавычек. После этой подтверждённой ранней
ошибки неоднозначного provisioning write не было, поэтому такой повтор допустим.
Пароли/логин/ID/seed/bootstrap не запрашивались в чат. Роль выбирает сам владелец.

Исправление подготовки: explicit choice нормализуется toUpperCase после existing
trim. Принимаются n/N/e/E; другие значения по-прежнему отклоняются. Default
account choice, bypass TTY/config/source/schema/scope/confirmation и reset не
добавлены. Критерии логина, пароля, MFA и атомарный import не изменялись.

Локальная проверка actual CLI через pseudo-TTY с synthetic read-only Cloudflare
boundary: старый n воспроизвёл исходную ошибку; исправленные n/N открывают
New OWNER login, e/E — Exact existing active admin account ID, x отклоняется.
Всего 6 terminal scenarios; password не вводился, PROVISION/import не достигнуты,
реальных запросов/секретов/записей нет. Existing ownerPhoneSetup 15 tests — PASS;
node --check/git diff --check — PASS. Свежий backend CI ожидается. APK/Worker
runtime/миграции/зависимости не меняются; remote success на ПК владельца не
выдаётся за проверенный факт. Утверждение STAGING этапа действует, G1 на паузе.


### 2026-11-15 — Исправление регистра OWNER choice проверено и упаковано

Итоговый source 5f07f1f5bbdbe7ec0324ed7586394afd8b8d5634, backend
CI 43724115538 / job 132842190133 — SUCCESS: 67 test scripts, 15 phone setup
tests, 18 migrations, 12 D3 preflight tests, четыре native workerd/D1 harnesses,
Worker dry-run, npm audit 0 vulnerabilities. Manual actual-CLI pseudo-TTY checks:
6 scenarios, original n failure, fixed n/N/e/E allowed to their respective next
prompt, invalid x denied; Cloudflare boundary synthetic/read-only, passwords
не вводились, provisioning SQL/confirmation не достигнуты.

Исправленный архив отдельно: transfer-admin-owner-phone-choice-2026-11-15/
owner-phone-setup.zip, 179 файлов, 96 protected hashes. Проверены manifest hashes,
private TTY init с synthetic public Account ID, rejection подменённого source
до network/config и отсутствие загруженного Firebase SDK key Driver в ZIP.
Receipt OWNER_PHONE_CHOICE_FIX_2026-11-15.json; архив SHA256:
6baf6e894cb6b680c3457e85a2be1c6b3ad9ac9a886f38c308b799578566ae61.
Прежние пакеты сохранены. Для немедленного продолжения исходным проверенным
пакетом достаточно заглавной N; новое развёртывание Worker/пересборка APK/
отключение checks/редактирование private config или source manifest не нужны.

Remote миграции/isolation/keys на ПК владельца не перепроверялись ассистентом.
Сбой данного owner наступил до записи, поэтому предписан повтор с N. Если уже
достигнут PROVISION/import, действует прежнее правило: не повторять запись
вслепую, сначала read-only plan. Утверждение STAGING остаётся действующим, G1
на паузе; production/merge/reset этим исправлением не выполнялись.


## 2026-11-15 — Сбой OWNER после PROVISION: запись появилась, bootstrap не выдан

Владелец прислал сообщение FAILED at owner уже после точной фразы PROVISION
STAGING OWNER <private account ID>. ID/логин/пароль/коды в журнал не копируются.
Сразу предписан только read-only plan, без слепого повтора provisioning.
Получен новый admin-phone-plan.public.json, SHA256
9c0aeca6ae4f33f46b79967cbd2d11d28c790e3e9440f1d33270ea3ca5b4fbe7.
Данные разобраны как JSON, не как инструкции. Source 01bbcc8a соответствует
выданному v2 пакету; target прежний STAGING. По отчёту: pending=[], ownerExists=true,
ownerFactorCount=0, оба JWT/MFA secret names present, ADMIN_PHONE_TEST_ONLY binding
присутствует, G1 на паузе. remoteWritesPerformed=false относится к самому plan,
не означает отсутствия записей предыдущего owner/schema/key action. Значения
секретов и flag не раскрываются JSON, их guards выполняет ранее проверенный CLI.

Найдена и воспроизведена ошибка control plane: pinned Wrangler 4.125.0 SQL file
import с --json пишет progress строки из отдельного CLI logger/spinner до JSON.
OwnerPhoneWrangler.run(...,{json:true}) пытается JSON.parse всего stdout и
отклоняет успешно завершённый import до postcondition query/выдачи bootstrap.
Отдельный опыт с настоящим Wrangler и локальным synthetic API подтвердил:
strict JSON parsing отвергает ответ completed import; та же команда с проверкой
exit status без разбора write stdout завершается успешно. Реальных Cloudflare
запросов/секретов в опыте нет, fixture SQL SELECT 1. Это согласуется с ownerExists
после сообщения об ошибке; сырые provider logs владельца не запрашивались.

Исправлен только вызов write command: stdout import не используется как
доказательство. Exit status по-прежнему проверяется; отдельный strict-JSON D1
SELECT обязан подтвердить текущий active OWNER, новый bootstrap digest и exact
unexpired expiry. Лишь после этого код показывается в private TTY. Все read
commands сохраняют строгий JSON parsing. Атомарный D1 import/rollback, factor
guards, пароли и permissions не изменены. Для текущего состояния после проверки
исправления используется существующий OWNER, без создания второй учётной записи,
смены пароля или сброса TOTP. Выдаётся новый bootstrap через прежний утверждённый
control plane, исходный потерянный plaintext по digest не восстанавливается.

Добавлена полноценная regression проверка реального CLI/private PTY/реального
Wrangler с локальным synthetic SQLite/D1 HTTP boundary: old committed failure,
новый и existing OWNER success, wrong postcondition и import rollback. Проводится
локальный запуск; результаты/CI пока не объявляются успешными. При подготовке
fixture первый запуск не достиг owner prompt из-за отсутствующей служебной
таблицы d1_migrations в mock DB; tracking table добавлена только в тестовую
среду, production schema не менялась. APK/Worker runtime/миграции/зависимости
не меняются. Утверждение STAGING действует, G1 остаётся на паузе.

Локальный protected-CLI regression теперь PASS для всех 5 сценариев: legacy
committed failure (owner/bootstrap записаны, код не выдан, postcondition read
ещё не вызван), new_success, existing_success без второй account,
wrong_postcondition без выдачи кода, import_failure с rollback. Используется
реальный Wrangler 4.125.0 и локальный SQLite/D1 API; real Cloudflare requests 0,
raw terminal output не раскрывается, hidden synthetic password не echo. Existing
ownerPhoneSetup 15 tests PASS, syntax/whitespace checks PASS. Новый regression
добавлен в CI после native OWNER phone check; финальный полный CI ещё ожидается.


### 2026-11-15 — Post-import исправление проверено, подготовлено продолжение

Source a7ed6128dbd5416b1c4db5210accfacebec3377e, backend CI 43726075123 /
job 132848172485 — SUCCESS: 67 test scripts, 15 setup tests, 18 migrations,
12 D3 preflight tests, четыре native workerd/D1 harnesses, Worker dry-run,
npm audit 0 vulnerabilities. Дополнительно новая CI проверка реального protected
CLI + реального Wrangler 4.125.0 + synthetic loopback SQLite/D1 API: 5 scenarios
PASS — legacy_committed_failure, new_success, existing_success, wrong_postcondition,
import_failure. Negative legacy control воспроизводит записанный OWNER/bootstrap
без вывода кода. Positive existing flow не создаёт вторую account; wrong
postcondition/import failure не выдают код, audit failure откатывает запись.
Real Cloudflare requests 0, password echo/raw terminal disclosure отсутствуют.

Исправленный пакет отдельно: transfer-admin-owner-phone-import-2026-11-15/
owner-phone-setup.zip, 182 файла, 96 source hashes. Receipt —
OWNER_PHONE_IMPORT_FIX_2026-11-15.json. Проверены source hashes, private TTY init
с synthetic Account ID, отказ на подменённый source до config/network, отсутствие
production config и загруженного Driver Firebase key в ZIP. Архив SHA256:
b4f813c07c78f3bd2de7fd97869610f71e24aece43d8f1e4eae22e7db62d5079.
Предыдущие пакеты сохранены; private Windows run этого исправления ещё ожидается.

Выдана OWNER_PHONE_POST_IMPORT_RECOVERY_2026-11-15.md: новый ZIP в новую папку,
локальное повторное использование уже проверенного config (CLI валидирует его
заново), npm ci --ignore-scripts → plan → owner. Ожидается Existing OWNER found;
не повторяются schema/isolate/key creation, не создаётся другой account, пароль
не сбрасывается. Новая попытка выдаст новый bootstrap только для active OWNER
без factor после authoritative digest/expiry read. Затем те же login/password
на телефоне, новый bootstrap, TOTP/recovery и новый вход. Секреты только в
private TTY/UI. Актуальные remote/phone результаты будут записаны после ответа
владельца. Утверждение STAGING действует, G1 на паузе, APK/Worker/API/миграции/
зависимости не меняются. Production/merge/reset не выполнялись.

При публикации source первый JS orchestration вызов не выполнился из-за
синтаксической ошибки конкатенации до tool actions; исправлен и повторён,
репозитории проверены сопоставлением staged tree с remote proposed tree.


## 2026-11-15 — Повтор после обновления выполнялся из старой вложенной папки

Владелец сообщил, что распаковал обновление в owner-phone-setup с заменой
похожих файлов, затем прислал npm ci success (37 added / 38 audited / 0
vulnerabilities по его выводу), успешный plan и повторный generic owner failure
после PROVISION. В prompt PowerShell рабочая папка — вложенная
owner-phone-setup/owner-phone-approved. Private home path и actual staff ID
из сообщения в журнал не копируются.

Запрошены только публичные sourceCommit из двух owner-phone-release.json.
Владелец подтвердил: текущая вложенная папка =
01bbcc8acf5228ba7735969a8fbbf0cb4e8671ba (старый import JSON parsing);
родительская owner-phone-setup = a7ed6128dbd5416b1c4db5210accfacebec3377e
(проверенное исправление post-import). Таким образом повтор запускал прежний
CLI из другой директории, новый исправленный CLI ещё не проверен на его Windows.
По одному повторному failure точный итог второго import не объявляется:
успех account/bootstrap/телефона требует новой authoritative проверки.

Продолжение: перейти явно в родительскую owner-phone-setup; сохранить
существующий config там либо скопировать только локально проверенный config
из owner-phone-approved, если в родительской папке его нет; не перезаписывать
имеющийся config и не редактировать source/manifest. Затем npm ci --ignore-scripts,
plan (проверит все source hashes и exact target), и после успеха owner. Ожидается
Existing OWNER found; новый account/пароль/factor/keys/schema/isolate не создаются
и не сбрасываются. Утверждение этого STAGING восстановления действует, повторного
согласования не нужно. При ошибке plan — остановка, не обход guards.

Код/запечатанный пакет не менялись: по-прежнему source a7ed6128 / CI 43726075123
SUCCESS, 15 setup tests + 5 protected CLI scenarios и полный backend набор.
Это не новый запуск CI и не независимый scan пользовательского Windows.
Remote действий из среды ассистента не выполнялось, G1 на паузе.


## 2026-11-15 — Разъяснён ввод TOTP: два приложения; возможное раскрытие pending seed

Владелец сообщил о цифровой клавиатуре, затем уточнил: приложение Transfer Admin,
bootstrap принимается, затруднение возникло с буквенной строкой после этого.
В сообщение включена потенциально секретная строка настройки. Её значение,
фрагменты и fingerprint в журнал/файлы/репозиторий/инструменты не копируются.
Владелец отдельно подтвердил: 6-значным кодом подключение ещё не подтверждено,
recovery-коды не получены. Точный live factor state не читается ассистентом.

Проверены исходники и фактически переданный signed debug APK: SHA256
f7d42523fb5f16803d4bd52c03a9b0f462f041e1911c858799944f2c48f35fd2
соответствует receipt/source 8a948591. apkanalyzer binary XML и aapt2 resource
mapping подтвердили bootstrapCode inputType=0x81 (textPassword), maxLength=100;
totpCode/enrollmentCode inputType=0x12 (numberPassword), maxLength=6. Java не
переводит bootstrap в number. Numeric TOTP input здесь предусмотрен контрактом;
факт неисправной клавиатуры на bootstrap не установлен. Установленный на телефоне
APK независимо не считан. Код/IME/security guards не изменяются по предположению.

Дан порядок двух приложений: Admin показывает секретный Base32 key → отдельное
TOTP приложение (например Google Authenticator), manual setup с этим key и time
based mode → шесть цифр обратно в Admin. Bootstrap из private Windows вводится
только в Код подключения. Буквенный key не предназначен для цифрового поля Admin.
TOTP SHA1/6 digits/30s и challenge 5 минут остаются по утверждённой политике.

Потенциально раскрытый pending key не используется дальше. Пока enrollment не
подтверждён, предложен новый bootstrap через утверждённый protected owner action
для того же существующего OWNER, затем новый login/enrollment. Реальный CLI
сам проверит active owner/no factor; при уже enrolled factor остановится без
reset. Пароль, account, серверные JWT/MFA encryption keys не меняются. Не требуется
новое согласование прежнего STAGING setup объёма; active MFA reset не разрешён.

Проверка с synthetic account/keys в in-memory SQLite на actual migrations и
actual protected bootstrap SQL/MFA services: reissue удаляет старые challenges,
FK ON DELETE CASCADE удаляет pending enrollments; old enrollment confirm и old
bootstrap отвергаются, fresh key отличается, новый enrollment работает.
Account id/password_hash/token_version/role/status сохраняются. PASS, raw secrets
не выводились, actual user string не использовалась, реальных Cloudflare calls 0.
Это локальная проверка, не подтверждение remote invalidation на ПК владельца.

Добавлена OWNER_TOTP_TWO_APPS_GUIDE_2026-11-15.md с последовательностью действий
и безопасным перевыпуском. Перед переключением на том же телефоне seed следует
приватно переписать: Admin очищает его в фоне, IDs сохраняет только до исходных
5 минут. Значения ключей/OTP/bootstrap/recovery в чат не присылать. Фактический
перевыпуск/phone login ожидаются от владельца. G1 остаётся на паузе.


## 2026-11-15 — Владелец подтвердил новый TOTP и вход; продолжение G2

Владелец подтвердил: после перевыпуска новый TOTP подключён, вход в Transfer
Admin на телефоне выполнен. Это результат проверки владельцем; независимого
чтения live factor/session или новой полной STAGING E2E проверки не проводилось.
Значения seed/bootstrap/OTP/recovery не запрашиваются и не фиксируются.
Задача восстановления OWNER phone login закрывается по подтверждению владельца.

Продолжается ранее утверждённая последовательность G2: после Admin moderation
— Driver registration/vehicles, затем availability/manual assignment. Для
анкеты и автомобилей создана отдельная ветка arch/g2-driver-registration.
Используются действующие D3 API, Java/XML, существующие Material-компоненты.
Реальные документы/биометрия не собираются: D4–D7 не утверждены, G1 на паузе.
Флаг ADMIN_PHONE_TEST_ONLY не снимается; Driver UI проверяется на синтетическом
транспорте, а не обходом изоляции STAGING. Merge и production deploy не разрешены.

В текущем GET /v1/driver/me есть profile/vehicles/status/revision, но нет
причин возврата отдельных элементов. Не придумывать эти данные в UI; расширение
контракта требует отдельной конкретизации. Первый срез: сохранение личных
данных, read-only подтверждённый телефон, список/создание/изменение автомобилей,
VIN, серверные статусы, optimistic revision conflict и защита от повтора mutation.
Отправка документов и финальная подача всей регистрации в этот срез не входят.


## 2026-11-15 — G2 Driver: первый срез анкеты и автомобилей, перед runtime CI

Добавлены Java/XML формы с read-only подтверждённым телефоном, личными данными,
несколькими автомобилями, VIN, статусами и independent expectedRevision.
Серверные правила draft/changes_required и suspended/rejected сохраняются.
Mutation transport без refresh/replay/redirect/cache использует существующий
HTTPS allowlist и привязку staffId/access/refresh. При анализе lifecycle найден
риск запуска ещё не начавшейся задачи после cancelAll: исправлен operation epoch,
инвалидируемый onStop; старый scope не работает после rebind. Формы и серверные
данные очищаются onStop, input не входит в autofill/SavedState. Неопределённый
результат записи требует явный GET/reload перед следующим сохранением.

Совместимость: API допускает null usageBasis старого автомобиля. Чтение этого
случая разрешено, при сохранении обязателен явный выбор. Причины исправлений
не возвращаются GET me и не выдумываются в UI. Финальный submit, документы,
identity и availability не реализуются в этом первом срезе.

Локально build debug/staging/release, unit tests трёх variants и lint трёх
variants прошли на промежуточном source. После lifecycle/compatibility правок
повторены debug unit/compile androidTest/lint: 40 JVM cases PASS (29 прежних +
11 новых); instrumentation APK с 15 новыми cases компилируется, runtime ещё
не запускался. Security guards: Driver 7 + Admin 5 PASS. Четыре действующих
backend registration service/readiness/submit/migration contract scripts PASS.
Новых dependencies/API/schema/Worker/deploy изменений нет.

Начальный lint выявил 3 API28-only heading attributes и private back resource
в новом UI: исправлено через ViewCompat и собственный vector. В новом срезе
lint warnings/errors 0. Общий Driver debug lint: 0 errors / 54 прежних warnings
(в том числе unused resources, available upgrades, старые UI/locale замечания).
Они не объявляются исправленными или полноценным security clearance; dependency
upgrade не выполняется вне текущего scope. Имеются безопасные build notices о
metrics path/deprecated APIs/native strip, не failures.

CI расширен полным Driver lint и обязательными runtime cases. Native screenshots
light/dark font1.3 создаются только test APK с synthetic account/transport:
временно clear FLAG_SECURE в test и restore/assert в finally; в production app
нет screenshot bypass. Извлекаются только четыре фиксированных PNG, без app
storage/конфигурации/ключей. Ожидается runtime CI; его успех пока не заявляется.
Границы/контракты: DRIVER_REGISTRATION_SCOPE_2026-11-15.md. G1 на паузе.


### Дополнительный аудит: автоматический HTTP 503 replay

Проверен bytecode actual locked OkHttp 4.12.0: ветка 503 Retry-After:0 не
проверяет retryOnConnectionFailure. Реальный loopback HTTP fixture воспроизвёл
две отправки обычного тела при retryOnConnectionFailure=false. Это не mock
application interceptor, который обходит внутренний retry.

Устранено в общих HTTP builders Driver и Admin: тело помечается isOneShot=true
с делегированием contentType/contentLength/isDuplex/writeTo; HTTP-клиент
прекращает follow-up до повторной отправки. HTTPS/origin policy, deadlines и
серверные/API контракты не меняются. Новые зависимости не добавляются.
Это исправление найденного дефекта безопасности, а не новое продуктовое решение.

В обоих модулях actual HTTP regression сравнивает обычное тело (2 requests)
и защищённое (1 request, no priorResponse); также проверяется применение
защиты общим builder до транспорта для POST/PUT/PATCH. Локальные полные debug
наборы теперь Driver 43 + Admin 34 JVM PASS; instrumentation compile, оба debug
lint PASS. Driver lint 0 errors / 54 warnings, новые формы без замечаний; Admin
0 issues. Исходный CI 43735703470 запущен до этого исправления; его результат
не будет выдан за проверку окончательного source. Окончательный CI ожидается.


### Runtime первого source и обнаруженная непригодность screenshot evidence

CI 43735703470 / job 132877757100, source 6cc32fb745bae905541fd0325ca4cd42b9a71a65:
SUCCESS. Driver JVM 40×3=120, runtime API35 27; Admin JVM 31×3=93, runtime 29.
Новые 15 Driver cases фактически выполнены, включая обе формы/сохранение,
cancelled epoch/rebind и background/Parcel. Это source до HTTP503 исправления.

Выгружен artifact 14170790867 (754 bytes), digest совпал с GitHub. Все четыре
файла .png по 50 bytes содержали лишь диагностический текст run-as об отсутствии
пакета. Эти файлы отвергнуты как screenshots; визуальная проверка не объявляется
выполненной. Причина: connected test удалил target APK до run-as extraction;
adb stdout перенаправился в файл без проверки формата. CI success сам по себе
оказался недостаточным доказательством визуального результата.

Исправление: CLI-only android.injected.androidTest.leaveApksInstalledAfterRun=true
для Driver test task на ephemeral CI emulator (параметр подтверждён в actual
AGP9.4.1 BooleanOption); production app/settings не меняются. Добавлена проверка
PNG signature/IHDR/native dimensions/chunk CRC/IEND перед upload. Validator
локально отвергает фактически полученный ложный PNG. Финальный CI должен
проверить HTTP503 fix и новые полноценные screenshots.


## 2026-11-15 — Проверка G2 Driver и общая защита HTTP503: финальный code CI

Android Draft PR #8: https://github.com/lobanov0710-blip/TransferDriver/pull/8
Проверенный source 5cb6d60060c491e4d0768c90d1446beeacfd1e00, tree
fae2d2d4f41923bdde0e7ebb2bb1a47ba85b8a7e. CI 43736924677 / job132881464115
SUCCESS: Driver43 JVM×3=129 + API35 runtime27; Admin34 JVM×3=102 + runtime29.
Итого231 JVM +56 runtime, без failed/skipped. Full lint3 variants обеих apps:
Driver0errors/54 прежних warnings в каждом, новые формы0issues; Admin0issues.
Локальная повторная сборка и те же6 JVM suites/6 lint suites прошли. Driver7+
Admin5 security guards и4 backend registration contract scripts PASS.

Нативный artifact14171191658 (544933bytes) checksum совпал:
218740e98b81bb7106046c30666e5eb028d1c6f06ec5ab511c932ba549d548e1.
Четыре actual API35 screenshots1080×2400 прошли signature/IHDR/chunk CRC/IEND
и локальную повторную проверку. Предыдущий artifact с adb diagnostic text
остался отвергнутым, его CI success не использован как визуальная приёмка.

Собран Admin debug STAGING с прежним отдельным сертификатом (публичный SHA256
ea4f14836b45719946857ce90a3fd8399a088b6ecca76f0a82db26cfa9c71a8b),
взятым только из прежнего локального ignored signing store. APK SHA256
43bfbb4daad747a53d231a2ffb3518ad3a5fd8e125d3a3712a519cbef85348d5.
Driver debug APK SHA256 a299c3a7ba3de59d15bc74a041f3c2cfb3bb9ad0c24d68c6a1111e039b6b8157;
сертификат отличается от Admin. APK подписи проверены apksigner37. Его новый
формат V2 Signer потребовал исправить локальный разбор public digest; первая
попытка receipt завершилась StopIteration, ключи не раскрывались/не менялись.
Это локальные сборки того же source; CI самостоятельно собрал/проверил source,
а не эти конкретные локальные APK bytes. Driver live E2E не выполнялся.

Загруженный Firebase SDK config key проверен на отсутствие во всех tracked
файлах (сам ключ не печатался). Документы ТЗ не изменены: blob
7434dc0b1548b4f2c5382f12d6cc5399f93d0a84. OWNER подтвердил новый TOTP/вход;
G1 остаётся на паузе, ADMIN_PHONE_TEST_ONLY не снимается. Реальные документы/
биометрия, release signing, merge и production deploy не выполнялись.

Первый fresh Impeccable reviewer завершился без результата из-за service usage
limit; это не review pass. После команды владельца «далее» повторяется тот же
safe review packet, без user secrets/входных ключей и без fork исходной беседы.
Итог UI review и необходимые исправления ещё будут зафиксированы отдельно.


### Independent native finish review — fix round 1

После service quota interruption fresh reviewer повторно получил тот же safe
packet и4 проверенных native captures, без fork исходной беседы и без секретов.
Возврат: disposition fix;5 контрактных sections получены. Harness использовал
generic fresh agent по fallback finish-reviewer reference Impeccable, штатный
тип агента/launcher недоступен; независимость контекста сохранена.

Два material UI дефекта: системная строка состояния имела недостаточный
контраст в light/dark, а hint количества мест обрезал диапазон1–20 при font1.3.
В sampled code новых material ошибок auth/mutations reviewer не обнаружил;
это не независимый scan всего проекта и не proof отсутствия всех уязвимостей.

Исправления одним batch: WindowInsetsControllerCompat задаёт foreground
status/navigation bars по resolved DayNight; краткий label Пассажирских мест,
диапазон От1до20 вынесен в постоянный Material helper. Две существующие native
fixture cases теперь дополнительно проверяют bar appearances, реальный fontScale
и измеренную ширину hint/постоянный helper. Production FLAG_SECURE сохраняется.
PRODUCT.md фиксирует только факты утверждённого scope, не вводит новых решений
и не создаёт новый дизайн. Требуется rebuild/recapture тех же4paths и verdict
того же reviewer; исправления до recapture не объявляются принятыми.


### Проверка UI fix round1: runtime failure и синхронизация fixture

Source5448f31c755843b1a98a2f0ae3b35aa5d6e12339, CI43738660853/job132886782876
завершился FAILURE на Driver connected test. Build/unit/lint этап прошёл;
nativeSyntheticScreensDarkLargeFont не дождался safe UI state Статус анкеты
за исходные8секунд. Проверки appearance/helper ещё не были достигнуты в этом
case. Это не PASS и не подтверждение post-fix screenshots; предыдущие снимки
не используются для приёмки новых UI правок.

Анализ fixture: settings font_scale обновляется асинхронно, а прежний teardown
вызывал finish и менял DayNight/font до фактического уничтожения Activity.
Ссылка на старый экран могла очищаться при последующей configuration recreation.
Устранена эта гонка условий: перед launch явно ожидается observed fontScale и
main idle; перед изменением конфигурации в teardown ожидается isDestroyed.
Ожидания по-прежнему bounded8s, assertions/cases не удалены, retry-until-green
и ослабления условий нет. Причина failure предполагается по механизму fixture;
новый runtime прогон обязан подтвердить результат, диагноз не выдаётся за
независимый live trace.

Дополнительно test-only ProxySelector запрещает любое немокированное сетевое
обращение создаваемых registration clients (без вывода URI/headers), прежний
selector восстанавливается в finally. Это защита тестовой среды от accidental
запроса при recreation, не production transport hook. Production code/permissions/
TLS/FLAG_SECURE с предыдущего source не менялись. Instrumentation APK повторно
компилируется; окончательный CI/recapture/verdict ещё ожидаются.


### Следующий runtime: Driver PASS, Admin dialog focus failure

CI43739771085/job132890126120, source505454a52a257a986391e8999dd416f5afb31eab:
Driver connected27 cases PASS после fixture synchronization/network denial;
все4PNG1080×2400 прошли validator. Следующий Admin connected этап FAILURE:
ModerationUiInstrumentationTest line61 получил RootViewWithoutFocusException
на первом confirmation dialog (10s). Остальные material assertions не удаляются.
Общий CI не объявляется успешным. Снимки были извлечены, но upload стоял под
обычным success условием и после позднего Admin failure не выполнился; image
bytes для post-fix review ещё не получены.

Проверено по emulator logs: screen_off_timeout уже2147483647, device menu
keyevent уже выполнен runner; гипотеза screen timeout не подтверждается.
Не добавляются обходы app auth/permission/keyguard и не повышаются таймауты.
В тесте Admin model-ready ранее не означал window-focused: programmatic click
мог создать dialog до фокуса Activity, а isDialog matcher мог выбрать root
без фокуса. Добавлены event-based ожидание фактического parent window focus
(те же10s) и matcher реально focused dialog. Espresso click и все проверки
uncertain action/reused key/partial correction остаются обязательными. Это
исправление synchronization fixture, не изменение production UI/модерации.

Workflow пишет marker только после успешного PNG validator; upload разрешён
после позднего failure только при наличии этого marker и без cancellation.
Содержимое upload по-прежнему только4 synthetic PNG. Это сохраняет evidence,
не меняет failure status и не делает незавершённый test suite успешным.
Admin instrumentation APK локально собран PASS (19s). Production исходники
Driver/Admin после предыдущего UI fix не меняются. Новый полный CI ожидается.


## 2026-11-15 — Первый срез G2 Driver: проверенный checkpoint и ограниченный UI verdict

Окончательный tested source16cdc03e6e7b2830c88a9bda19ee370540d1dfb5,
tree078a54b798d1911e6f15ac91891bc08afed84321; CI43742661189/job132899016826
SUCCESS. Driver43JVM×3=129 +27API35runtime, Admin34×3=102 +29runtime:
231 JVM +56 runtime, failed/skipped0. Полные lint6suites/endpoint locks/
отдельные package+UID+signing/App Check policy/security guards PASS. Driver
имеет54 прежних lint warnings и0errors, новые registration files0issues;
Admin lint0issues. Четыре backend registration contract scripts проверены
локально PASS, backend production code/API/schema не менялись.

Два промежуточных runtime failures (dark fixture и Admin dialog focus)
не скрыты/не объявлены успехом: исправлены наблюдаемые условия синхронизации
fixture без удаления cases/assertions, увеличения timeout или auth bypass.
Окончательный прогон выполнил все прежние и новые cases. По сравнению с
source5448f31c после исправления native UI production sources Driver/Admin
не менялись: дальнейшие changes только tests/workflow/docs.

Post-fix native artifact14173720607 (525678bytes), archive SHA256
72c114fe0aadf374745b7acbc8d65cd5d6bf08bb13dea36cf7e3b8dc04399077
проверен после скачивания. Четыре actual PNG1080×2400 light/dark/font1.3
валидированы и заново просмотрены по тем же путям. Прежние pre-fix captures
и отвергнутый adb-text artifact не используются как post-fix evidence.

Тот же independent fresh reviewer вернул disposition ship в scoring pass:
контраст system bars, полный label+permanent helper1–20, factual PRODUCT.md
и confirmation evidence — все4resolved; регрессий от этого batch не заметил.
Это verdict только о перечисленных исправлениях, не scan всего проекта,
не physical-device/live Driver E2E, не полная приёмка G2/production approval.
Использован generic fresh agent по fallback Impeccable role contract;
штатный launcher/agent-type отсутствовал, первый service-quota failure описан
ранее. Отдельный documenter сохранил incumbent Material3 DayNight, без нового
DESIGN/world/palette/fonts/dependencies, факты/ограничения — в UI review.

Результат: личные данные/read-only подтверждённый телефон, несколько машин,
обязательный VIN, раздельные revisions/statuses/save; single-send HTTP503
защита общих clients обеих apps и session/lifecycle binding registration.
GET me не даёт причин исправлений — UI их не выдумывает; полный wizard/
документы/биометрия/submit ещё не закончены. Technical response bounds
1MiB/200vehicles отражены в receipt, это не изменение server model.

Artifacts сохранены раздельно, старые не перезаписаны. Driver UI-fix debug APK
SHA2562940d9f67f47a27e8a8609f77a3835e16026f20f19688931beb0c2d3c11d4461
(source5448f31c, тот же production tree в final source); Admin STAGING debug
SHA25643bfbb4daad747a53d231a2ffb3518ad3a5fd8e125d3a3712a519cbef85348d5
(source5cb6d600, production Admin не менялся). Admin certificate совпадает
с ранее выданным, обновление предусматривается поверх без удаления данных.
Exact local APK bytes не объявляются CI-tested: CI собирал тот же source сам.
Driver live backend остаётся недоступен в утверждённом OWNER-only STAGING
scope: G1 на паузе, флаг изоляции не снимается.

Полные facts/hashes/provenance/limitations опубликованы в Android feature:
https://github.com/lobanov0710-blip/TransferDriver/blob/arch/g2-driver-registration/docs/DRIVER_REGISTRATION_VERIFICATION_2026-11-15.json
https://github.com/lobanov0710-blip/TransferDriver/blob/arch/g2-driver-registration/docs/DRIVER_REGISTRATION_UI_REVIEW_2026-11-15.md
Draft PR#8: https://github.com/lobanov0710-blip/TransferDriver/pull/8

Утверждённое ТЗ не изменено (blob7434dc0b1548b4f2c5382f12d6cc5399f93d0a84).
D4–D7 не утверждены. Владелец подтвердил новый TOTP и phone login; это
owner-reported acceptance, независимый live factor read не выполнялся.
Следующая уже утверждённая часть — availability/manual assignment.
Merge/production deploy не выполняются без отдельного решения владельца.
Финальные commits меняют только документы/журналы и помечаются [skip ci]:
проверки закреплены за exact code source выше, повтор unchanged code не нужен.


## 2026-11-15 — G2 availability/manual assignment: следующий срез, без возобновления G1

Владелец поручил «далее». По принятой последовательности после анкеты/автомобилей
начата Driver availability в отдельной arch/g2-availability-assignment от
проверенного registration checkpoint. ТЗ §§16–18 и D3: выбор одобренного авто
перед on_line, нижний slider, Радар активен только по backend state, off_line
перед сменой и запрет смены при активном назначении. Реальные Driver routes на
OWNER-only STAGING не открываются; D4–D7/G1/merge/production решения не меняются.

Driver реализован по существующим GET /v1/driver/me и PUT /v1/driver/availability:
independent availability.revision, Idempotency-Key, исходный staff/access/refresh
и lifecycle epoch; без auth refresh/HTTP replay/optimistic radar. Off_line
сохраняет activeVehicleId, не очищает его при активной assignment. Нет approved
авто/identity/revision — нет локального разрешения on_line; окончательный допуск
и busy/photo checks серверные. Неизвестный результат требует explicit GET.
Background удаляет snapshot/выбор и инвалидирует операции; поздний auth callback
не начинает availability network вне STARTED lifecycle. Нативный main footer
сворачивается online и размещён с учётом system bars/IME; keys/data не пишутся
в SavedState/autofill/logs.

Admin read-контракт ещё не выбран. Подготовлено
ADMIN_ASSIGNMENT_READ_API_PROPOSAL_2026-11-15.md version2 и async вопрос A/B.
Ошибка первоначального анализа: GET /orders НЕ запрещён OWNER/DISPATCHER из-за
роли. core/adminAuthorization.js и AdminSession подтвердили legacy auth category
admin с отдельными server operational role/permissions; неверное утверждение
отозвано, предложение и вопрос исправлены ДО реализации API. Действительные
пробелы: нет selection snapshot driver+vehicle; legacy orders extra PII и cutoff
1000 без cursor. A:2 минимальных paginated GET; B:1 options GET + старый список
с явными ограничениями. Пока решения нет, зависящие endpoints не реализуются;
RAW ID/revision ручной ввод или новые moderation grants не используются как обход.
Основание — AGENTS.md п.3, новые детали требуют явного решения владельца.

Локально debug build/instrumentation compile/48 Driver JVM и lint PASS:
0errors/54 прежних warnings. Driver7+Admin5 security guards PASS. Backend
availability service/repository/actualSQLite(7cases) и assignment service/
repository/migration —6 scripts PASS. Backend code/API/schema не менялись.
Добавлены5 JVM availability boundary cases и11 native protocol/lifecycle/UI cases
(включая настоящий Slider swipe и TalkBack setProgress). Runtime пока не запускался.
Native main XML/panel тестируется в отдельном secure host с synthetic transport;
это layout/component evidence, не live Main login/orders routing. Немокированная
сеть запрещена test-only guard; никаких production test hooks/реальных данных.
Screenshot fixture временно clear/restore FLAG_SECURE только в test APK.
CI запланирован с8 fixed PNG paths/validator marker и regression обеих apps.

При предыдущей попытке tooling прерван пользователем во время запуска compile:
изменения теста сохранились, compile log не появился. После «Далее» состояние
сверено, сборка запущена заново и завершилась PASS(42s). Это не отмена G2 и
не утверждение pending Admin API. Окончательный runtime/visual verdict ожидаются.


### Availability runtime1: неверный вызов accessibility provider в test fixture

Source208ebfad4235f1ab5f29396347083c941bf2647c, CI43752677606/job132929903193:
FAILURE на Driver connected.11 новых cases реально выполнены:4PASS (включая
настоящий Slider swipe и выбор другого авто),7FAIL в общем onlineAction helper.
Старые27 Driver cases PASS; Admin runtime ещё не запускался. Build/unit/lint
этап прошёл. Никакой визуальный/полный runtime PASS пока не заявляется.

По actual locked Material1.14/AndroidX core1.18 bytecode проверено: метод
AccessibilityNodeProviderCompat.performAction у wrapper возвращает false,
не делегируя обёрнутому framework provider. Test вызывал этот stub из
ViewCompat.getAccessibilityNodeProvider; это неверная тестовая точка входа.
Исправлено на slider.getAccessibilityNodeProvider().performAction, то есть
реальный Android virtual-thumb ACTION_SET_PROGRESS. Assertions, ожидаемый
backend state/допуск/одиночная отправка и реальный swipe case сохранены.
Production app code не меняется; новый instrumentation compile/CI ожидаются.


### Возобновление после tooling interruption; проверка через утверждённый CI

После команды «Продолжаем» сверены git status/test helper/logs. Исправленный
framework provider вызов сохранился, но предыдущая попытка compile log не
создала: tooling была прервана до команды. Локальный повтор без дополнительных
разрешений, с --offline, завершился до compilation: Gradle FileLockContentionHandler
не смог определить usable wildcard IP. Это sandbox/network limitation служебного
сокета, не ошибка Java и не успешная локальная компиляция. Sandbox/сетевые
настройки не менялись. Следующая полная проверка — существующий авторизованный
GitHub CI build/runtime pipeline; результат будет привязан к новому source.
Локальная предыдущая variants сборка staging/release/unit/lint PASS(3m15s),
но она предшествовала test-only correction. Admin read API всё ещё не выбран;
команда продолжения не трактуется как новое утверждение A/B.


### Availability runtime2: реальный lifecycle вместо ручного onStop в fixture

Source87b0acb949b31acffd3aa83b7e8e143ed1f01045, CI43774408323/job132997285290:
FAILURE на одном из11 новых native cases.10PASS, включая настоящий swipe,
framework accessibility action, отсутствие optimistic radar, ack loss/late401.
backgroundClearsVehicleAndInvalidatesPendingOperation ожидал GONE, получил
VISIBLE. Build/unit/lint этап PASS; общий runtime/visual acceptance не объявлен.

Fixture вручную вызывал Instrumentation.callActivityOnStop. Такой вызов метода
не воспроизводит framework Activity lifecycle transition/ON_STOP Registry,
к которому подписан DefaultLifecycleObserver панели (наAPI35 dispatch через
pre-stopped callbacks). Прежний Registration тест собственный onStop проверял
напрямую, но это не корректный lifecycle тест observer-компонента.

Fixture теперь запускается ActivityScenario и переводится moveToState(CREATED);
дополнительно assert фактического Lifecycle.State.CREATED перед проверкой
GONE/empty vehicle/FLAG_SECURE. Teardown закрывает scenario; сброс выбора и
инвалидация незавершённой mutation, assertion count/таймауты не ослаблены.
Production Java/XML/security policy не меняются, новый полный CI обязателен.


### Privacy review: исключение availability subtree из Autofill

Обнаружено несоответствие source/scope формулировке: saveEnabled=false не
отключает Android Autofill. Новый Spinner мог участвовать в Autofill structure
со списком автомобилей, хотя локальное сохранение/Parcel уже исключено.
Исправлено root importantForAutofill=noExcludeDescendants для availability
subtree (API26, соответствует minSdk), не затрагивая TalkBack/slider accessibility.
Это не утверждение фактически произошедшей утечки; устранена возможность
непредусмотренной передачи новых полей autofill service. Новых permissions/
SDK/dependencies нет. Требуется compile/runtime нового source; текущий CI
проверяет предыдущий source, его результат не будет выдан за эту правку.


### Availability runtime3: проверка очищенных данных вместо cached Spinner count

Sourcef15aa56a0af983bd8330544ef955af8f1fe600f9, CI43775678761/job133001307324:
FAILURE в одном из11 новых native cases.10PASS; фактический lifecycle CREATED
и GONE уже подтверждены. Background case затем ожидал getCount=0, получил3.
Проверен AOSP android-15.0.0_r1 AbsSpinner.setAdapter/resetList/getCount и
AdapterView.getSelectedItem: null adapter удаляет selection/children, однако
mItemCount в этой ветке не сбрасывается. Числовой cached count не означает
сохранение списка автомобилей. Тест проверяет фактические privacy условия:
null adapter, null selectedItem, null selectedView, CREATED/GONE/FLAG_SECURE,
отмену одного pending PUT. Assertions на данные сохранены, timeout не увеличен.
Добавлен assert importantForAutofill=NO_EXCLUDE_DESCENDANTS. Новый source включает
ранее найденную privacy XML правку; build/unit/lint/runtime будут проверены CI.
Локальная Gradle compilation остаётся ограничена sandbox service socket;
повторные запросы сетевых разрешений не используются для обхода ограничения.


### 2026-11-16 — Availability CI PASS и независимый UI review: fix

Source12fd1c216d7284f783f28f527820d5ff0d93eafa/tree4f6e65daeb5a9f825239d413d442820f1eaa9320,
CI43776899566/job133005125151 SUCCESS. Реально выполнены246 JVM (Driver48×3,
Admin34×3) и67 API35 runtime (Driver38, Admin29), skipped/failures отсутствуют.
Полные6 lint suites/builds/endpoints/AppCheck/package/signing/UID/security guards
PASS. Это включает XML Autofill fix и все11 новых availability native cases.
Local7+5 security guards PASS. Локальный текущий compile не заявляется из-за
sandbox service socket; CI — фактическая проверка текущего кода.54 старых Driver
lint warnings известны из предшествовавших локальных6 suites, не объявляются
устранёнными; CI lint PASS сам по себе не даёт новый warning count.

Artifact14186633067,876738bytes, SHA256
eb14a3425dc519a2e33a956e7b8d90e35ddaf1e1aebc71c34be623e57242c951:
8 actual native PNG1080×2400 проверены signature/IHDR/CRC/IEND и archive digest,
просмотрены. Это component/layout evidence в secure host с synthetic transport,
не live Main auth/orders routing. FLAG_SECURE clear/restore — только test APK.

По требованию Impeccable new-work §7 запущен независимый fresh reviewer без
истории. Harness не позволяет выбрать shipped agent type: generic agent использовал
fallback finish-reviewer contract, native platform/craft-floor,5 sections.
Disposition fix: панель fits offline/online light/dark/font1.3; initial system
bars имеют неверный контраст (offline-light status white, offline-dark nav black),
в online captures контраст правильный. Это window/host integration finding,
не clipping нового компонента; полная UI acceptance пока не заявляется.

Проверен locked AndroidX core1.18 bytecode Impl30.setAppearanceLight: wrapper с
Window пишет legacy decorView flags, а wrapper без Window вызывает platform
setSystemBarsAppearance. ДляAPI30+ MainWindow теперь явно использует platform
appearance, API26–29 сохраняет compat path. Это исправляет прямую настройку
контраста; точная причина compositor timing по одним PNG не установлена.
Fixture перед снимком проверяет window focus и platform appearance mask, затем
ждёт UI idle вместо произвольного250ms; состояние не подменяет. Все4 native
captures будут заменены по тем же путям и возвращены тому же reviewer для scoring
единственного замечания. Новый source/full CI и actual screenshots обязательны.
G1paused, Admin A/Bpending; deploy/merge/backend code/schema не меняются.


### Availability visual fix round1: CI PASS, scoring PARTIAL

Source800065ead78621e62b10a0ad78e083a4635d8a00/treea3a842095e7bd7807c44583087a2fc489245e209,
CI43778224200/job133009304957 SUCCESS:246 JVM+67 runtime, full6 lint/builds и
security/endpoint/signing/UID guards PASS. Artifact14187751524,876930bytes,
SHA256d02ea094bb90c75f914b485f715c76edf72458822ad8e9a4770bf41e8905ddf3;
8 PNG validated,4 availability replacements inspected. Same reviewer scoring:
PARTIAL, disposition fix. Light status icons и dark navigation handle исправлены;
на первом dark offline capture Wi-Fi/notification icons остаются black, в
следующем online — white. Единственное замечание contrast остаётся открытым,
UI ship не заявляется. Platform appearance/focus assertions прошли, однако
они и accessibility idle не доказывают завершённый render всех SystemUI layers.

Round2 меняет только screenshot synchronization: перед сохранением требуется
не менее1s одинаковых actual native framebuffers (poll200ms, bounded8s).
Никакого ожидания желаемого цвета, recolor/crop/image generation, принудительных
OS/theme/demo icon settings либо изменения server offline state нет. Если
неверный цвет устойчив, он попадёт в PNG и reviewer сможет снова отклонить.
Точная причина SystemUI частичного tint по PNG не доказана; это проверка
устойчивого состояния compositor, не утверждение об исправленном production баге.
Production Java/XML остаются source800065e; окна/FLAG_SECURE/security guards
не ослабляются. Новый CI,4 замены по тем же путям, same-reviewer scoring требуются.
Если второй verdict оставит замечание, по Impeccable §7 будет предъявлен владельцу
открытый результат и выбор дальнейшего раунда; бесконечный polish не выполняется.


### 2026-11-16 — G2 availability: итог проверенного среза

Рабочая ветка arch/g2-availability-assignment, Draft PR#9:
https://github.com/lobanov0710-blip/TransferDriver/pull/9 .
Проверенный code source e3c6f0c3bde6ac77c5527e4e69c7f65fecd07e69,
tree9425fc9174e786c7c2055158d52288a8c501b1d2; CI43779524059/job133013316916
SUCCESS:246 JVM (Driver48×3, Admin34×3),67 API35 runtime (Driver38,Admin29),
0fail/skipped. Полные6 lint suites, сборки debug/staging/release и staging AAB,
security guards7+5, endpoint/AppCheck/package/signing/UID checks PASS.
Исторические54 Driver lint warnings из предыдущих local suites остаются
известным ограничением; CI lint PASS не выдаётся за новый warning count.

Driver: нижний slider/approved-car selector, radar только после server ack+GET,
off_line сохраняет ID текущего авто. Revision/idempotency/session epoch,
no auto refresh/replay; background отменяет pending work/очищает данные.
Availability subtree исключён из Autofill; фактическая утечка не установлена.
Нет новых dependencies/backend API/schema/deploy/merge. Production app tree
cad37e46bc1200df7a850bf5cec0a1d02847d814; Admin production tree
5e612dfc4e515aaf46ecdd99814548d8a44dd817 не менялся.

Промежуточные CI43752677606/43774408323/43775678761 FAIL сохранены в истории:
test fixture accessibility stub, ручной onStop вместо lifecycle и cached
Spinner count. Исправлены без удаления assertions/кейсов. CI43776899566 и
43778224200 затем SUCCESS246+67, но визуальный review выявил initial system-bar
contrast. Fresh generic reviewer/documenter по Impeccable fallback; первый
scoring PARTIAL/fix. После API30+ platform appearance и test-only ожидания
устойчивого unmodified framebuffer второй scoring RESOLVED, disposition ship
ТОЛЬКО для исходного contrast finding. Все4 actual native captures просмотрены,
новых регрессий batch reviewer не наблюдал. Это не full-product security clearance.

Artifact14188321054,882728bytes, SHA256
921c49e9470bd5e16faec27130e6d996366f765482f6c873db5c601c839deb0f.
8 fixed PNG1080×2400 signature/IHDR/CRC/IEND/archive-digest PASS.
Provenance: docs/DRIVER_AVAILABILITY_VERIFICATION_2026-11-16.json и
docs/DRIVER_AVAILABILITY_UI_REVIEW_2026-11-16.md в Draft PR#9.
Это main XML/panel в secure synthetic host, не full Main auth/order routing,
не live Driver/physical-device/tablet E2E. CI Firebase config синтетический,
новый user-installable Driver APK этим checkpoint не выдавался.
Локальный текущий compile ограничен Gradle sandbox service socket; CI —
фактическая проверка current source.6 прежних backend contract scripts, включая
7 actual SQLite cases, PASS на неизменённом backend code; повтор не требовался.

G1 остаётся на паузе; D4–D7 не утверждены. ADMIN_PHONE_TEST_ONLY неизменён.
Admin assignment read API version2 A/B всё ещё ожидает явного выбора владельца.
Исправлено первоначальное неверное утверждение о роли legacy GET /orders:
OWNER/DISPATCHER сохраняют auth-category admin; реальный пробел — selection
snapshot, cutoff1000 и extra PII старого списка. Предложение A:2 minimal
paginated GET; B:1 options GET + legacy limitations. Зависящие новые endpoints
не реализованы. Основание AGENTS.md п.3: «Не выполнять предложенный вариант до
утверждения». Общие команды продолжения не трактуются как выбор A/B.
Финальные docs/journal updates — [skip ci], без изменения проверенного app/test/
workflow кода; результаты CI относятся к exact code source выше.


### 2026-11-16 — Утверждён и проверен вариант A Admin assignment read API

Решение владельца: «Вариант A утверждаю». Утверждена version2 предложения
ADMIN_ASSIGNMENT_READ_API_PROPOSAL_2026-11-15.md: два minimal paginated GET.
Прежние pending A/B записи являются историей, решение A теперь явное.
Backend ветка arch/g2-admin-assignment-read, Draft PR#30:
https://github.com/lobanov0710-blip/uber-v3/pull/30 .
Code source dda85d2937f385cc7b0cccbc88d4282d50481485,
tree3fe4f5bb685d221ac7f1d6b92fedd636ec06e32c,
CI43854087967/job133253772715 SUCCESS.

GET /v1/admin/orders и GET /v1/admin/orders/{id}/assignment-options реализованы:
keyset cursor/limit1–50/default20, строгие scope/types/unknown/duplicate params,
минимальные SQL projections, server current grants и OWNER MFA, no-store,
обязательный read audit без контактов/cursor/provider/SQL text.
Actual test перечислил1106 orders без legacy cutoff1000. Options читает одну
страницу одним SELECT с общей mutation SQL eligibility projection, без N+1,
score/rank/автовыбора/новых D6 правил. Offline сам по себе не исключается.

Обнаружен и исправлен существующий D3 defect account active: precheck и atomic
assign/reassign теперь требуют role=driver/status=active. Read/mutation используют
общую функцию blocker codes. Actual SQLite race после precheck подтверждает
отказ обоих UPDATE при отключении кандидата без изменения order/audit.
Старая фраза «atomic busy» уточнена: current-assignment unique index относится
к order_id, новые критерии scheduling/future assignment не добавлялись.

Полный CI: все68 unit/router scripts PASS, включая15 новых actual SQLite/HTTP
cases;18 migration checks,12 fail-closed staging tests, npm audit0 vulnerabilities.
Все5 native Worker/D1 checks (availability,OWNER MFA,moderation,new read API,
OWNER phone scope), protected CLI5 scenarios и Worker dry-run bundle PASS.
Новый bundled Worker/D1 smoke подтвердил minimal payload/pagination/offline/
disabled-driver/current grants/OWNER MFA/read audit4rows. Deploy не выполнялся.

Локально15 новых cases и8 связанных regression suites PASS; полный npm suite
остановился на sandbox spawnSync EPERM старых CLI тестов, native Miniflare —
на listen EPERM127.0.0.1, Wrangler dry-run — global config path. Эти ограничения
не скрывались/не обходились; обязательные проверки затем реально прошли в CI.
Первоначальные2 FAIL новых fixtures (неверный audit action-prefix и INSERT
без списка колонок staff_accounts) исправлены, assertions/кейсы сохранены.
Контракт: docs/ADMIN_ASSIGNMENT_READ_API.md; provenance:
docs/ADMIN_ASSIGNMENT_READ_VERIFICATION_2026-11-16.json в backend PR#30.

G1paused, D4–D7pending, ADMIN_PHONE_TEST_ONLY whitelist неизменён: оба новых GET
по-прежнему недоступны на текущем OWNER-only STAGING. Реальные Cloudflare
ресурсы, schema/dependencies и Android production code не менялись.
Admin UI ручного назначения — следующий плановый срез, этим API checkpoint
не объявляется готовым; новый APK не выпускался. Merge/deploy требуют отдельного
решения. Финальные docs/journal commits [skip ci] не меняют проверенный код;
результаты относятся к exact code source выше.


### 2026-11-16 — Начат утверждённый Admin assignment UI

После «Далее» начат следующий срез в отдельной arch/g2-admin-assignment-ui
от Android checkpoint с утверждением A; backend PR#30/CI43854087967 уже PASS.
Реализуются только утверждённые GET + существующие assign/reassign: native
Material3 Java/XML, выбор заказа/driver+vehicle, обязательная причина и
подтверждение человеком, cursor pagination и исходный Idempotency-Key.
Новая private Activity не exported, FLAG_SECURE, Autofill/saved-state excluded,
foreground-only snapshots/reason/uncertain action; onStop cancel/clear. Отдельный
клиент не refresh/replay, привязан к account/access/refresh/scope и guard до
HTTP dispatch. Каждый read/POST заново получает server me/permissions/MFA.
OWNER phone scope не открывает operational UI/API. Backend/whitelist не меняются.

Impeccable ordinary extension: launcher отсутствует, fallback раскрыт, прочитаны
PRODUCT/native incumbent/new-work/craft-floor/Android guidance. Редизайн, новые
палитра/шрифты/dependencies и concept tournament не вводятся: flow задан ТЗ/D3/A.
Новый экран пока не проверен сборкой/runtime; тесты/native captures и fresh
finish reviewer/documenter обязательны. G1paused/D4–D7pending/merge+deploy отдельно.

Локальная проверка G2 Admin assignment UI: 15 JVM-тестов контракта/транспорта
прошли на текущих исходниках (включая проверку ACK и прежнего ключа повтора).
Компиляция выполнена прямым javac/JUnit с Android SDK и ранее проверенным
публичным debug BuildConfig; это не сборка APK и не проверка вариантов Android.
AAPT2 скомпилировал ресурсы; проверены 252 ссылки R.string/R.id без пропусков.
Ранее выполненные security guards: Admin 5 и Driver 7 — PASS.
Локальный Gradle недоступен из-за запрета service socket в sandbox; повторных
обходов/ослабления sandbox нет. Полная сборка, lint, варианты и runtime
проверяются отдельным GitHub Actions run; результат ещё не объявлен.

Обнаружена UI-неоднозначность до финального review: разные заказы с одинаковыми
маршрутом/датой/тарифом не различались человеком по подписи строки.
Исправлено: полный opaque orderId из утверждённого read DTO показан в списке,
выборе и подтверждении; новых полей/endpoint/персональных данных нет.
Добавлен native regression с двумя одинаковыми маршрутами, без auto-selection.
Первый CI43874308929 успел подтвердить build/lint всех6 variants и policy checks;
его runtime ещё выполнялся. Для изменённого исходника требуется новый полный CI,
старый run не является доказательством исправления.

CI43874308929 завершён как cancelled механизмом branch concurrency после
публикации исправления идентификации заказов. Runtime этого run не объявляется
PASS. Текущий source1a26fb5950eab915f05230e306b05c60fb6d6041, tree
cefec0f0c65786d32955f2c9caafaf5a8e508e04, CI43875519388 — RUNNING.
Draft PR#10: https://github.com/lobanov0710-blip/TransferDriver/pull/10,
base arch/g2-availability-assignment; merge/deploy не выполнялись.

Обновлённый CI43875519388 прошёл полный build/lint6 variants и policy gates;
API35 instrumentation ещё выполняется. PRODUCT.md отражает реализованный
срез и его границы; live/hardware/G2 acceptance не заявлены.
Служебные ошибки публикации: connector отклонил encoded branch URL400,
head безопасно прочитан через fetch_commit; большой read JSON был усечён,
разбор остановился до записи. Публикация повторена меньшими пакетами с точным
сравнением local/remote Git tree. Секретные конфиги и ключи не публиковались.

CI43875519388 / job133327445986 — SUCCESS; source
1a26fb5950eab915f05230e306b05c60fb6d6041, tree
cefec0f0c65786d32955f2c9caafaf5a8e508e04. Фактические отчёты:
Driver48 JVM×3, Admin49 JVM×3 =291 выполнений; API35 Driver38+Admin46 =84
runtime cases; без failures/errors/skipped. Все6 build/lint вариантов,
bundleStaging, security guards, endpoint/AppCheck/signing/UID policy PASS.
Получены18 actual native PNG; проверены ZIP SHA256, PNG signatures/CRC/IEND,
1080×2400 и SHA256. Десять Admin кадров лично открыты до передачи reviewer:
orders/options/form/confirmation/uncertain в light и dark/font1.3.
Это actual Activity с synthetic HTTP/Keystore, не live backend/phone acceptance.
Скачанный CI APK: package ru.transferservis.admin, min26/target37; подпись
проверена apksigner. Это ephemeral CI debug key; совместимость обновления
телефонной установки не заявлена. Локальный metadata check сначала ожидал
старое имя sdkVersion; AAPT2 37 выводит minSdkVersion. Проверка исправлена,
фактический minSdk26 подтверждён; изменения приложения для этого не потребовались.

Impeccable fresh finish reviewer /root/assignment_finish_reviewer, generic-role
fallback без истории, проверил все10 native кадров и первичные Java/XML.
Disposition FIX: единственное material замечание — square legacy AppCompat
confirmation вместо Material3 dialog. Остальные проверенные элементы matrix
match/adaptation; это не security clearance и не hardware/TalkBack/tablet review.
Исправлен только builder на уже имеющийся MaterialAlertDialogBuilder;
secure-window/obscured-touch/body/key/confirmation/cancel поведение сохранено.
Требуются новый полный CI, recaptures по тем же путям и scoring того же reviewer.
Новый визуальный мир/dependencies/DESIGN.md не вводятся; native detector не запускался.

Текущий Material3 correction: source86ae9b46fe6b7918eb28cf00b822a34f26f3b52c,
tree0ae36dab6748f49ddc73080a755d8d57b590d518, CI43877768185/job133335205029.
Промежуточный checkpoint синхронизирован append-only в canonical журналы
Android ec8eafff7cb04211ecec306328ee8cf3976c344b и backend
c8d4fdf6f609f9a6e88f1027661868d047c4890b, backend рабочая ветка
c67ea497a33c6faa18490fe690c7f5a1df8c4f2e. На canonical/work backend менялся
только журнал; diff проверен на единственный файл и отсутствие удалений,
ref с expected SHA. Ни исполняемый backend код, ни STAGING не изменены.

Финальный CI43877768185/job133335205029 — SUCCESS для Material3 correction,
source86ae9b46fe6b7918eb28cf00b822a34f26f3b52c,
tree0ae36dab6748f49ddc73080a755d8d57b590d518. Заново выполнены291 JVM
(Driver48×3/Admin49×3),84 API35 native (38/46), все6 build/lint, bundles,
security/endpoint/AppCheck/signing/UID checks; failures/errors/skipped0.
Повторно получены18PNG, проверены ZIP SHA256 и PNG headers/CRC/IEND; все10
Admin captures повторно открыты по прежним путям. Light/font1.0 и dark/font1.3,
1080×2400; изменённое окно реально показывает Material3 shape/tonal surface.
Тот же reviewer: единственное замечание RESOLVED, introduced visual regressions
не наблюдал; disposition SHIP только для scored confirmation correction.
Это не новый полный security/UI/live/hardware аудит. Fresh documenter следует.

Финальный CI debug APK проверен: package ru.transferservis.admin, min26/target37,
apksigner signature PASS, SHA256
cc72db7148f9b75a3f6158e4795bd42c53bc0fe4ed294803ba0ae44c5de9854b.
Ключ ephemeral CI, совместимость обновления на телефоне не заявлена.
Artifact14231790908 (18PNG) и14231448349 (APK) относятся к точному source86ae;
срок хранения7 дней указан в verification receipt. Секреты в artifacts отсутствуют:
использованы synthetic данные, CI SDK config и отдельная CI debug identity.
GitHub не отдавал незавершённый job log (404 BlobNotFound); после завершения
журнал получен и проверен. Служебный JS metadata helper исправлен без URL global;
временные download URLs не печатались. Ошибка формата apply_patch для JSON
receipt остановилась до записи; заменена одной атомарной Update File операцией.


## G2 Admin manual assignment — финальный synthetic checkpoint, 2026-11-16

Android Draft PR#10 https://github.com/lobanov0710-blip/TransferDriver/pull/10,
source86ae9b46fe6b7918eb28cf00b822a34f26f3b52c,
tree0ae36dab6748f49ddc73080a755d8d57b590d518,
CI43877768185/job133335205029 SUCCESS. Заново291 JVM executions
(Driver48×3/Admin49×3),84 API35 runtime (Driver38/Admin46), все6 build/lint,
bundles/debug test APKs, security/endpoint/AppCheck/signing/UID gates;
failures/errors/skipped0. Предыдущие RUNNING и reviewer FIX — история.

Устранены UI-неоднозначность одинаковых маршрутов (полный orderId) и
legacy confirmation (существующий Material3 builder).18 actual native PNG
проверены по ZIP digest/PNG CRC/IEND/dimensions/SHA256;10 Admin кадров лично
открыты. Тот же independent reviewer закрыл единственное material замечание
RESOLVED, disposition SHIP только для scored confirmation correction, без
наблюдённых introduced visual regressions. Fresh documenter отдельно прочитал
код/контекст и все10 кадров, проверил hashes; UI record завершён. Generic-role
fallback раскрыт, native detector не применялся, DESIGN/system files не менялись.

Receipt: https://github.com/lobanov0710-blip/TransferDriver/blob/arch/g2-admin-assignment-ui/docs/ADMIN_ASSIGNMENT_UI_VERIFICATION_2026-11-16.json
UI record: https://github.com/lobanov0710-blip/TransferDriver/blob/arch/g2-admin-assignment-ui/docs/ADMIN_ASSIGNMENT_UI_REVIEW_2026-11-16.md
План и PRODUCT отражают реализованный срез и границы. Финальная публикация —
только документы [skip ci]; исполняемый код остаётся точно проверенным source86ae.

Это synthetic Activity/Keystore/HTTP evidence, не live backend/hardware/tablet/
TalkBack acceptance и не завершение всего G2. G1paused, D4–D7pending. Backend
кроме журнала не менялся; ADMIN_PHONE_TEST_ONLY whitelist не расширён,
operational routes остаются404 на телефонном OWNER-only STAGING. Merge/deploy,
remote migrations, реальные документы/биометрия/назначения не выполнялись.
CI debug APK подписан ephemeral CI key; совместимость обновления телефона не
проверена. Артефакты14231790908/14231448349 хранятся до2026-11-23; receipt
содержит checksums/provenance. Lint PASS не означает ноль исторических warnings.

## Продолжение плана после назначений — readiness фотоконтроля, 2026-11-16

Поручение владельца: «Далее по плану». До изменений прочитаны AGENTS.md,
ТЗ §§14–15/28–30, D1–D3/D4–D7, актуальные план и хвосты обоих журналов.
GitHub heads сверены: Android ff628f71a561841a5d0ae80fef3ea2b6ba2de549,
backend506d7523c61e86cfb703a9a0eaffae5d36787294. Предыдущий native assignment
checkpoint завершён; синтетический PASS не закрывает весь G2.

Проверена готовность следующего возможного Admin среза §15. В router/service
есть POST create, GET известной photo-check и POST review; каталог всех авто
и GET коллекции проверок отсутствуют (коллекция сейчас POST-only).
RegistrationReview содержит server vehicle ID/revision, поэтому возможен
ограниченный сценарий из карточки, но он не заменяет самостоятельный каталог.
Order-scoped assignment-options не переиспользуется как каталог фотоконтроля,
дополнительные moderation permissions ради этого не назначаются.

Подготовлены docs/G2_PHOTO_CONTROL_NEXT_SLICE_PROPOSAL_2026-11-16.md и
G2_PHOTO_CONTROL_READINESS_2026-11-16.json. Два конкретных варианта:
каталог+список с двумя minimal paginated GET (рекомендуется) либо ограниченное
создание из карточки без новых GET. Ни один не выбран/не реализован. Нужен
отдельный выбор объёма/новых контрактов по AGENTS.md п.3; прежнее утверждение
назначений A не распространяется на предложение фотоконтроля.

Независимая проверка existing backend выполнена Node24.22.1 локально:
photoControlSupport.sqlite.test.mjs5 PASS (включая2 support cases),
photoControlLifecycle.sqlite.test.mjs6 PASS. Итого11,failures/skipped/cancelled0,
на actual SQLite synthetic fixtures. Пределы: не новые GET, не Android,
не native Worker, не HTTP/MFA E2E, не exhaustive проверка обоих restriction
modes. Source hashes и log digests сохранены в readiness receipt. Новых тестов
ради документации не добавлялось; новый APK/полная CI сборка не требовались
и не объявляются выполненными. Обнаруженный пробел чтения не выдаётся за
доказанную уязвимость. Правила grace24h и два режима ТЗ не изменялись.

Работа ведётся в отдельной docs ветке arch/g2-admin-photo-control-proposal,
base arch/g2-admin-assignment-ui. План получил pending proposal и исправление
устаревшей строки16: выбор активного автомобиля уже реализован в PR#9.
ТЗ, production code, dependencies, migrations и телефонный whitelist не менялись.
G1paused/D4–D7pending; реальные фото/документы/биометрия/удалённые ресурсы не
использовались, merge/deploy не выполнялись. Зависимая реализация ожидает
явного решения владельца; подготовка и проверка предложения завершены.

## Запрошенный расширенный список марок — 2026-11-16

Запрос владельца: «Каталог автомобилей нужно расширить. Haval, Cherry и т.д.
Подготовь расширенный список для утверждения». Запрос выполнен как подготовка
состава справочника, без автоматического внедрения или утверждения новых API.
Прочитаны AGENTS.md, актуальный pending фотоконтроль proposal/план/журнал,
ТЗ§9, действующая Driver форма/DTO и backend normalizeVehicle/migration0005.
Подтверждено: текущие make/model — свободные строки1–80, закрытого выбора
марок в этой форме нет; Haval/Chery не исключаются её проверками.

Созданы VEHICLE_MAKES_CATALOG_PROPOSAL_2026-11-16.md и JSON:140 марок в5
группах (64/14/18/29/15), включая Haval и предлагаемое отображение Chery.
Cherry/Черри/Чери предложены как поисковые варианты. Предусмотрено раздельное
сохранение связанных марок/суббрендов и старых названий; automatic rewrite
существующих записей/регистрационных документов не выполнялся. Для отсутствующей
марки предложен ручной ввод с действующей проверкой авто, без hard whitelist.
Модели/поколения/комплектации этим списком не утверждаются. Названия не дают
одобрения, класс/вместимость/допуск и ранжирование остаются серверными по ТЗ.

Проверки данных: JSON parse,140 уникальных ID/названий,5 group counts,
188 уникальных поисковых значений без межбрендовых коллизий, length≤80 и
отсутствие управляющих символов. Явно проверены Haval/Хавал и Chery/Cherry/
Черри/Чери; все140 строк Markdown совпали с JSON. Links/diff проверены.
Внешняя рыночная полнота/наличие всех марок в продаже не проверялись и не
заявлены. Список вручную подготовлен для утверждения владельцем.
Новых unit/runtime тестов и APK build нет: менялись только документы/данные
предложения, executable code не трогался. Предыдущие11 SQLite cases —
история readiness фотоконтроля, не новые проверки справочника или приложения.

Рабочая docs ветка/PR#11 сохранены; план и предложение фотоконтроля содержат
ссылку на отдельный справочник. Полный состав/внедрение ожидают утверждения.
ТЗ/API/schema/dependencies/phone whitelist не менялись, G1paused/D4–D7pending,
merge/deploy не выполнялись. Фактические автомобили или credentials в
справочник/журналы не добавлялись; использованы только названия марок.

## Утверждение состава справочника марок — 2026-11-16

Владелец ответил на предложение140 марок: «Состав справочника утверждают.».
Ответ принят как явное утверждение состава опубликованного списка b5fa627a,
повторное подтверждение не запрашивается. Haval и Chery входят в утверждённые140.
До изменения статуса сверены AGENTS.md, текущие документы/план/журнал и GitHub
heads; рабочая ветка arch/g2-admin-photo-control-proposal, Draft PR#11.

JSON получил status=approved-composition, дату, точную формулировку решения,
ссылку на утверждённый source и SHA256 состава (id/name/group, сортировка по id).
Markdown, PRODUCT и план фиксируют утверждённый состав; в фотоконтроль proposal
уточнена независимость решения о марках. Сами140 строк/5 групп и aliases не
изменялись. Старые pending записи журнала сохранены как история.

Объём утверждения не расширен: implementationChanged=false и
photoControlApiApprovalImplied=false; модели, внедрение, alias/search поведение,
новые GET/выбор объёма фотоконтроля и миграция данных остаются отдельными решениями.
Поведение допуска/тарифов/приоритетов не менялось. Реальные данные автомобилей
не переписывались, API/schema/dependencies/phone whitelist не трогались.

Проверены равенство makes/groups/proposedBehavior с предыдущим commit,
140 unique IDs/names, group counts,188 поисковых значений, Markdown/JSON parity,
approval metadata/snapshot checksum и links/diff. Это docs-only фиксация решения;
новые APK/JVM/native/backend tests не запускались и не заявляются выполненными.
G1paused/D4–D7pending, merge/deploy не выполнялись. Реализация состава не
объявляется готовой только на основании его утверждения.
Служебный JS скрипт публикации сначала остановился на SyntaxError до выполнения
любых вложенных инструментов/записей. Синтаксис исправлен; утверждённый состав
не менялся, ref обновляется только после проверки полного Git tree.

## Исключение Эконома и поиск классификатора по репозиториям — 2026-11-16

Владелец указал исключить Эконом, поскольку этот тариф исключён в сервисе.
На вопрос об источнике модели/года/тарифа ответил: «нужно поискать по репозиторию».
Указание Эконома принято без повторного утверждения. Сначала выполнены read-only
поиски кода, файлов данных, документов, веток и доступной истории обоих repo.

Проверены текущие снимки всех60 доступных GitHub branches (14TransferDriver,
46uber-v3), recursive trees без truncation,821 уникальное текстовое содержимое
(376/445):776 прочитаны из локальных Git blobs, недостающие45 через GitHub.
CSV/XLS/XLSX/ODS/PDF/DOC/DOCX/архивов с таблицей в этих снимках нет.
Локальная reachable history:215+536 commits и538+736 text blobs. Девять
Granta совпадений — variable grantA в push test, не модели. Именованные
кандидаты каталогов — только ранее подготовленные нами файлы справочника марок.
GitHub default-branch search эконом/econom/каталог/solaris/vehicleClass также
не дал классификатора; econom найден только в negative pricing/orders/router tests.
Утверждённая таблица марка→модель→год/комплектация→тариф в этом объёме не найдена.
Полная удалённая ancestry всех веток, private D1/R2 и внешние документы не
проверялись; отсутствие таблицы вне указанного объёма не утверждается.

Найдено фактически: pricing.js, normalizeVehicle, migration0005 и Android
Registration.CLASSES/registration_class_choices содержат только Комфорт/Бизнес/
Минивэн. Existing pricing suite27 checks PASS; driverRegistrationService suite
PASS (одна отчётная группа, не число assertions). Прямой probe getTariffs/
normalizeTariff/calculatePrice подтвердил3 тарифа и отказ для econom/economy/
ECONOMY/Эконом. Это не автоматическая классификация конкретных автомобилей.
Source/log hashes и60 heads — VEHICLE_TARIFF_CLASSIFICATION_SEARCH_2026-11-16.json;
объяснение результатов — одноимённый Markdown. Ошибочные wildcard пути в первых
rg вызовах не имели файлов; последующие полные Git/tree/content scans выполнены
по существующим источникам. Секреты/provider/SQL output в отчёты не включались.

Справочник140 марок уточнён: tariffPolicy исключает Эконом, разрешает только
comfort/business/minivan, referenceMayNotServeAsVehicleAllowlist=true,
vehicleModelEligibilityApproved=false. Сами makes/groups и прежний composition
approval сохранены: в исходном списке вообще нет моделей или связи с тарифами.
Удалять марки целиком по предположению нельзя; конкретные допустимые модели
должны быть определены отдельной утверждённой таблицей. Ограничение Эконома
внесено в PRODUCT/план/справочник; новых бизнес-критериев/годов отсечения нет.

Изменялись документы, не executable code/API/schema/dependencies/реальные записи.
Новый APK/native Worker/Android runtime не запускались и не заявляются PASS.
G1paused/D4–D7pending, phone whitelist неизменён, merge/deploy не выполнялись.
Готовый фильтр эконом-моделей и пересмотренный список допущенных авто не
объявляются реализованными: источник модельных правил в repo не найден.
