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


## Проверка предоставленной OneDrive ссылки — 2026-11-16

Владелец предоставил ссылку1drv.ms и поручил проверить её как возможный источник
модельного классификатора. Источник в журнале идентифицирован без share-token:
SHA256 URL88079d36eec6cccd80ba288962308d18c3d17d3f7ae61d0ed724635aec492497.
Полная ссылка/токен общего доступа в Git не публикуются.

Web opener сообщил unsupported/inaccessible URL; rendered read завершился
timeout25s. Обычный read-only HTTPS GET с перенаправлением на onedrive.live.com
получилHTTP403. Содержимое файла не прочитано, название/формат/таблицы/модели
не подтверждены. Причина403 (права, вход, политика сервера или среды) не
установлена; ссылка не объявляется недействительной или истёкшей.
После403 попытки доступа остановлены; login/изменение sharing/API обходы
не выполнялись. Для изучения потребуется предоставленный владельцем файл
PPTX/PDF в чате либо доступный ему экспорт, без передачи паролей/кодов входа.

Модельная таблица по этой ссылке не найдена и не опровергнута: содержимое
недоступно. Никаких данных источника/новых тарифных правил в справочник не
добавлялось. Эконом остаётся исключён; только Комфорт/Бизнес/Минивэн.
Executable code/API/schema/автомобили/phone whitelist не менялись;
новые APK/tests/merge/deploy не выполнялись. Это запись проверки доступа,
не аудит содержания презентации и не утверждение внешнего классификатора.


## Указан CSV vehicle-classifier-v1 — источник пока недоступен, 2026-11-16

Владелец уточнил: «Ветка Uber-v3, а документ docs/vehicle-classifier-v1.csv.
просмотри файл списка и его обнови». Поручение принято как чтение конкретного
исходника и исключение Эконома по его реальной структуре, без выдумывания строк.

GitHub contents GET для указанного пути в default main и рабочей
arch/g2-admin-assignment-read вернули404. Чтение raw URL через GitHub connector
также404; default code search имени файла — без результатов. Обновлён список
всех46 доступных веток uber-v3; recursive trees всех46 без truncation не
содержат CSV или совпадающего classifier path. В доступных локальных backend
worktrees/shared/library-files/attachments выполнен no-ignore filename search,
CSV не найден. Ветка с Uber в имени в TransferDriver не найдена, установленный
репозиторий uber-v3 определён как lobanov0710-blip/uber-v3.

Отсутствие файла вне доступного snapshot не утверждается. У владельца сразу
запрошен полный GitHub blob URL с названием ветки. Содержимое/схема/строки CSV
не прочитаны; обновление файла не выполнено и не выдаётся за готовое.
Новый фиктивный классификатор вместо исходника не создавался. После доступного
источника предстоит проверить кодировку/разделитель/header, тарифные значения,
отфильтровать только Эконом с сохранением остальных данных и проверить результат.

Во время восстановления helper состояния после смены окружения один metadata
вызов завершился TypeError; мутаций до исправления не было. Helper восстановлен,
последующие read-only проверки завершены. Code/schema/dependencies/автомобили/
тарифные правила/phone whitelist не менялись. Новые APK/tests/merge/deploy
не выполнялись; требуется точный доступный источник, не повторное утверждение
уже исключённого тарифа Эконом. G1paused/D4–D7pending.


## CSV классификатора доступен: Draft PR #31 и результаты проверки — 2026-11-16

Владелец предоставил Transfer_Servis_vehicle_whitelist_v1_draft _1_.csv после
предыдущего Git-поиска. Исходник целиком прочитан и побайтно сохранён в uber-v3,
ветка docs/vehicle-classifier-v1-20261116, docs/vehicle-classifier-v1.csv.
Draft PR: https://github.com/lobanov0710-blip/uber-v3/pull/31
Source f5716179b39ef4224c91cbf2378384dfc96032c3, Git tree
68a7454012979ee30ea0db92cb731289bdabb912 совпал с локальным индексом.
CSV SHA256 3f8297f7b750816e13a3ceb82182e10f33bc42cdc308feb8e8ae2f1fa27ecc7d.

116 строк / 39 марок: Комфорт70, Бизнес22, Минивэн24. Эконом отсутствует уже
в исходнике; удалено0 строк, изменено0 ячеек. Все статусы pending_approval.
19 документных проверок PASS; дополнительно JSON/локальные ссылки, append-only
журнал, Git diff whitespace с учётом CRLF и exact remote/local tree проверены.
Формульных префиксов, управляющих символов, дублей/пустых обязательных полей
не обнаружено. Полная методика/ограничения: docs/VEHICLE_CLASSIFIER_REVIEW_2026-11-16.md
и одноимённый JSON в PR31. Это не тесты runtime/APK и не утверждение допуска.

Belgee/TENET отсутствуют в утверждённом справочнике140 марок; их включение
увеличит состав до142 и требует решения владельца. EXEED/Exeed, Skoda/Škoda
и FAW/Bestune B70 требуют согласования обозначений перед внедрением.
13 примечаний с ограничениями сохранены; они не реализованы как правила.
Утверждение116 связок тариф/модель/минимальный год/примечание ещё не получено.
Марки, годы и тарифы по предположению не менялись. Предыдущий результат поиска
остаётся историческим: новый источник получен отдельно от проверенных веток.

При публикации первоначальный полный JSON журнала превысил лимит stdout;
парсер остановился до remote mutation. Повтор использовал remote base и
append-only delta; Git tree совпал. update_ref новой ветки вернул422 Reference
does not exist без изменения ref; затем использован create_branch на проверенный
commit. Публикация Draft PR31 завершена; scope — четыре документа, без удаления
старых строк журнала. Секреты/персональные данные не публиковались.

Код/API/БД/dependencies/реальные автомобили/phone whitelist не менялись.
Справочник140 не расширен автоматически, CSV не подключён к допуску.
G1paused, D4–D7pending; APK/runtime/live E2E/merge/deploy не выполнялись.
Следующий зависимый шаг: решение владельца по конкретным строкам и двум маркам.


## Предложены 15 дополнительных китайских моделей: порог2023 выбран владельцем — 2026-11-16

Владелец спросил о добавлении других китайских марок для Комфорта и Бизнеса.
Подготовлено предложение15 моделей: Комфорт6 (BAIC X7, Dongfeng Huge,
Forthing T5 EVO, JAC JS6, Bestune T90, Lynk & Co08), Бизнес9 (Exlantix ES,
NIO ET7, IM Motors L7, Luxeed S7, Stelato S9, Avatr12, Zeekr001, Xpeng P7,
Xiaomi SU7). Все15 марок присутствуют в утверждённом справочнике140.
Допуск моделей и предложенные условия ещё pending, утверждение марок не
использовалось как разрешение всех их моделей. Эконом не добавлялся.

На вопрос о подготовке порога года владелец выбрал «Единый порог: 2023 год».
Это решение для предложения15 новых строк; годы исходных116 не изменены.
Это не утверждение самого допуска/условий/внедрения и не год запуска модели.
В docs/vehicle-classifier-v2-proposed.csv сохранены исходные116 и добавлены15
pending_approval: 131 total, Комфорт76/Бизнес31/Минивэн24. Исходный v1 побайтно
не изменён и является точным префиксом v2; hash v1
3f8297f7b750816e13a3ceb82182e10f33bc42cdc308feb8e8ae2f1fa27ecc7d,
hash v2 eb175c7fa79a33ca3a6ee6546b717178b84722771ddbba408b7610ba734ebd99.
Все131 статуса pending. Предыдущее утверждение116 и Belgee/TENET не получено.

Предложение/первичные ссылки/условия/ограничения сохранены в
VEHICLE_CLASSIFIER_CHINA_EXTENSION_PROPOSAL_2026-11-16.md и JSON.
Производители подтверждают модели и описывают версии, а не тариф Transfer Servis.
Очная проверка посадки/второго ряда/комплектации не выполнялась; для Avatr12/
Zeekr001 отдельно предложено согласовать кузов. Новые требования D6 не введены.
Текущая веб-страница не доказывает оснащение каждого автомобиля2023 года.

18 документных проверок PASS; дополнительно Markdown/JSON/CSV parity,
hashes/local links проверены. API/code/DB/runtime/реальные автомобили/140makes/
phone whitelist не менялись. APK/runtime/live E2E/merge/deploy не выполнялись.
G1paused/D4–D7pending. Отобранные официальные источники указаны в таблице;
первый URL T90 был недоступен, правильная страница найдена с official home.
EXLANTIX ES перенаправлен с exeed.ru на esteo.ru; Zeekr007 дал только JS shell
и в предложение не включён. Большие поисковые ответы усекались в выводе;
целевые источники прочитаны отдельно. Access/security обходы не выполнялись.

Предложение опубликовано в существующем Draft PR31 (7 файлов документов).
Source 47dde60122dcb03734351c43c957603b8cca3291, tree 9aebdc3ed0510cec4fb8e267ba3f11366fd317e0.
Локальный индекс и remote tree совпали; scope нового commit — 4 docs файла,
без удаления старых записей и без изменения v1 CSV. Утверждение моделей pending.


## Владелец утвердил 15 китайских дополнений и условия — 2026-11-16

На прямой вопрос «Утверждаете эти 15 дополнений и предложенные условия?»
владелец ответил «Утверждаю». Утверждение относится к опубликованному предложению
47dde60122dcb03734351c43c957603b8cca3291, SHA256 CSV до решения
 eb175c7fa79a33ca3a6ee6546b717178b84722771ddbba408b7610ba734ebd99.
Состав: Комфорт6 — BAIC X7, Dongfeng Huge, Forthing T5 EVO, JAC JS6,
Bestune T90, Lynk & Co08; Бизнес9 — Exlantix ES, NIO ET7, IM Motors L7,
Luxeed S7, Stelato S9, Avatr12, Zeekr001, Xpeng P7, Xiaomi SU7.
Порог всех15 — 2023, перечисленные условия утверждены и сохранены.

В docs/vehicle-classifier-v2-proposed.csv ровно15 pending_approval заменены на
approved; для этих15 notes обозначают утверждённые условия и проверку конкретного
автомобиля. Марки/модели/годы/тарифы/сами условия неизменны. approved означает
решение владельца по модельному правилу, а не approved реального автомобиля.
Условие отдельного согласования кузова Avatr12/Zeekr001 не удалено молча.

Исходный v1 и первые116 байтовых строк v2 сохранены; 116 статусов остаются
pending_approval. Утверждение исходных116, добавление Belgee/TENET и прежние
расхождения обозначений не приписываются текущему ответу. Справочник140
не расширялся. В документе131 строка:76 Комфорт/31 Бизнес/24 Минивэн,
из них15 approved/116 pending. Эконом отсутствует.

Markdown/JSON дополнения получили точный ответ/дату/scope/source commit/hash
и статусы approved-15-model-rules-with-conditions. Старые18 checks предложения
сохранены исторически. Новые18 проверок PASS: v1 hash/116 исходных строк,
131/counts, ровно15 status transitions, точные pending/approved и6/9, minYear2023,
неизменность утверждённых полей/условий, только status/notes CSV, схема/required,
нет Эконома/дублей/формул/control chars, BOM/CRLF, conditions/parity/round-trip.
Дополнительно Markdown/JSON/CSV parity, ссылки, append-only журнал и whitespace
проверены. Hash v1:3f8297f7b750816e13a3ceb82182e10f33bc42cdc308feb8e8ae2f1fa27ecc7d;
hash v2 после решения:e394d8cea41376f16e2a35962b42af5de910ecb6a36bb70dca0847060768aa34.

Решение внесено в PRODUCT и план Android docs ветки и журналы проекта.
Утверждение15 правил/условий не используется как разрешение нового API,
подключения к runtime, merge или deploy. Код/API/БД/dependencies/реальные
автомобили/phone whitelist не менялись. Очная проверка автомобилей, APK/runtime/
live E2E не выполнялись. G1paused/D4–D7pending; merge/deploy не выполнялись.
Следующая зависимость полной таблицы — решения по исходным116 и Belgee/TENET;
новые15 правил повторного утверждения не требуют.


## Утверждены исходные116 правил и Belgee/TENET:131 правил/142 марки — 2026-11-16

Владелец прямо поручил: «Исходные 116 строк и добавление Belgee/TENET добавить
и утвердить». Это полное утверждение116 исходных связок тариф/марка/модель/
минимальный год/примечание в представленном виде и добавления двух марок.
Предыдущее утверждение15 дополнений/порога2023/условий действует без повтора.
В текущем docs/vehicle-classifier-v2-proposed.csv теперь131 approved/0 pending:
76 Комфорт,31 Бизнес,24 Минивэн. Эконом отсутствует. В первых116 строках
изменён только status; годы, марки, модели, тарифы и13 примечаний сохранены.
Ранее утверждённые15 строки и условия побайтно неизменны. Единый2023 относится
только к15 дополнениям, не заменяет годы исходных116.

CSV v1 остаётся неизменяемым архивом предоставленного вложения с первоначальными
pending statuses (не текущая нормативная таблица). Утверждение применяется к v2.
Hash v1:3f8297f7b750816e13a3ceb82182e10f33bc42cdc308feb8e8ae2f1fa27ecc7d.
Hash v2 до полного решения:e394d8cea41376f16e2a35962b42af5de910ecb6a36bb70dca0847060768aa34,
после:1e4955a6f7250f0e5183f99d105a5c5b235eaa51b7ee432e2f29ad2a69af4901.
Снимок перед решением: backend ba98b93285ade5706a64772d40b1f8d510d41387.

В справочник добавлены belgee/Belgee и tenet/TENET без новых aliases, в группу
regional_legacy для просмотра. Группы не определяют страну выпуска автомобиля.
Всего142, группы64/16/18/29/15; прежние140 записи и aliases неизменны.
Composition SHA256 прежнего140:20c245092f7299e051616d1ade2c71aab8bd233578c2f5c47edab721b54e6d46;
нового142:abc564d91b94a69e3c213f153fc2aa2179df74e992b226fe9f6a2e55178fd3b1.
Формат: UTF-8 compact JSON с sorted keys и records по id, поля id/name/group.
История первого утверждения140 сохранена. Названия EXEED/Skoda/FAW Bestune B70
утверждены в исходном виде: автоматической нормализации/объединения не было.
vehicleModelEligibilityApproved=true указывает только на131 утверждённое правило;
referenceMayNotServeAsVehicleAllowlist=true, класс из марки не выводится.

21 проверка полного CSV PASS;14 проверок справочника PASS. Проверены точные116
status transitions,131 approved, неизменность полей/годов/примечаний/15 строк,
структура/round-trip/BOM/CRLF, отсутствие Эконома/дублей/формул/control chars,
142 уникальные марки/ID, прежние140 данные, группы/190 search values/aliases,
составные hashes, JSON/CSV/Markdown parity, ссылки. Исторические проверки
предложения/частичного утверждения и snapshot поиска сохранены как исторические.
План, PRODUCT, документы/receipts и журналы обновлены по фактическому решению.

approved — статус модельного правила, не конкретного автомобиля. Проверки
конкретных автомобилей не выполнялись; условия, включая отдельное согласование
кузова Avatr12/Zeekr001, не удалялись. Подключение runtime/UI/API/aliases/search,
новые GET фотоконтроля, DB migration/реальные автомобили/phone whitelist не менялись.
APK/runtime/live E2E/merge/deploy не выполнялись. G1paused/D4–D7pending.
Повторное утверждение состава131 правил/142 марок не требуется.

Обычный git diff --check отметил CR в сохранённом CRLF CSV как trailing
whitespace. Повтор с cr-at-eol (остальные whitespace checks сохранены) PASS;
BOM/CRLF исходника не менялись, внешние пробелы в ячейках проверены отдельно.


## Продолжение плана: фотоконтроль подготовлен, требуется выбор объёма — 2026-11-16

На поручение «Продолжаем по плану» сверены AGENTS п.3, последние решения,
план/журнал, ТЗ §§14–15 и предложение следующего G2 среза.131 модельное правило
и142 марки утверждены полностью; повтор этого утверждения не запрашивается.
Следующий подготовленный срез — внеплановый фотоконтроль Admin. Два объёма
(каталог/список с2 GET либо действие из карточки регистрации без новых GET)
не выбраны владельцем. Прежний вариант A назначений и утверждение справочника
не использованы как разрешение новых GET. Рекомендуемый контракт полностью
подготовлен в G2_PHOTO_CONTROL_NEXT_SLICE_PROPOSAL_2026-11-16.md.

Независимая проверка выполнена на backend412591d0d633e25ead8596cb8121ea703f12ca09:
photoControlSupport.sqlite5 PASS и photoControlLifecycle.sqlite6 PASS,
failures/skipped/cancelled0, Node24.22.1. Семь source hashes совпали с предыдущим
readiness.2 из11 tests относятся к support. Проверены существующие service/SQLite
fixtures; новые GET, Android/native Worker, HTTP/MFA E2E, полная работа обоих
режимов на активной поездке не выполнялись и не заявлены PASS.
Новых дефектов этими проверками не воспроизведено; это не полный security audit.
В начальном rg был указан отсутствующий adminAssignmentService.js; повтор по
rg --files и действующим orderAssignment/availability/operational файлам выполнен.
Android поиск новых endpoint методов вернул0 совпадений, не сбой инструмента.

Readiness JSON обновлён с лог-хэшами и прежним историческим snapshot; план и
предложение согласованы с данными131/142. Общий каталог автомобилей требует
server IDs/revisions и не подменяется справочником марок/моделей. Код/API/БД/
реальные материалы/classifier runtime/phone whitelist не менялись. G1paused,
D4–D7pending; APK/runtime/merge/deploy/remote migrations не выполнялись.
Следующий зависимый шаг — явный выбор владельца из уже подготовленных вариантов;
до него реализация новых GET/UI не начата согласно AGENTS п.3.


## Утверждён следующий G2 срез: каталог автомобилей и список фотопроверок — 2026-11-16

На вопрос об утверждении варианта «Каталог и список проверок» по подготовленному
предложению владелец ответил «Утверждают каталог и список». Утверждён вариант
с2 GET по G2_PHOTO_CONTROL_NEXT_SLICE_PROPOSAL_2026-11-16.md, source
6f4e3caf0ce283bbcca086ccf9b9e18f48935796: метаданные/permissions/MFA/audit/
scoped cursor/параметры/UX назначения и проверки. Отдельный повтор не требуется.
Созданы изолированные arch/g2-admin-photo-control-read (backend) и
arch/g2-admin-photo-control-ui (Android). Merge/deploy не выполняются.

Backend: добавлены GET /v1/admin/vehicles и GET /v1/admin/vehicles/{id}/photo-checks.
Проекция исключает VIN/телефоны/имена/причины/photos/storage keys; чтение
проверяет актуальные vehicles.read (+vehicles.verify для списка проверок),
OWNER server MFA, строгие параметры/IDs/безопасные integer. Keyset limit1–50,
cursor scoped к фильтру/vehicle, audit без произвольного текста; ошибка audit
не выдаёт проекцию. Две SELECT для vehicleRevision/check page выполняются batch.
OWNER-only staging остаётся404. Действующие mutations сохраняются.

Новые9 SQLite/HTTP тестов PASS, миграции18 PASS, D3 preflight12 PASS.
Первый прогон новых tests выявил неполную SQLite D1 test adapter реализацию:
batch SELECT не возвращал rows, и проверка requestId ошибочно применялась к
старому staff/login. Адаптер дополнен чтением SELECT в той же транзакции,
проверка requestId ограничена v1; повтор9/9 PASS. Новый native workerd/D1
workflow добавлен для CI на реальном D1 adapter. Первые поиски упомянули
отсутствующие scripts Android и migration/adminAssignmentService имена;
пути уточнены через существующее дерево. Большой первый read был усечён,
зависимые фрагменты прочитаны отдельно.

Полный npm test локально дошёл до ownerMfaControlPlaneCli, где sandbox
spawnSync возвращает EPERM с пустыми потоками (подтверждено безопасным --help).
Эти checks не объявляются PASS и не ослабляются/не пропускаются; полный
прогон и native Worker/D1 выполняются в GitHub CI. Локальная Android/Gradle
socket граница известна ранее; сборка/lint/runtime выполняются через штатный CI.
Данные131/142 не меняются; G1paused/D4–D7pending, phone whitelist неизменён.
Реальные фото/private R2/biometrics/remote migrations/merge/deploy не выполнялись.


## Admin фотоконтроль: реализация UI и сценариев безопасности — 2026-11-16

Добавлены защищённые PhotoControlActivity (Java/XML/Material3 DayNight), каталог
с6 status filters, scoped paging, явно выбираемый автомобиль и список checks,
форма только complaint/manual с явным restriction mode и причиной1–500,
подтверждение конкретного vehicle/revision/последствий. Системные даты только
из server metadata; фотографии/причины других проверок не загружаются.
Никакого клиента monthly scheduling/approval/фиктивного passed.

PhotoControlRepository перед каждой GET/POST заново читает server me/grants/MFA
и проверяет исходную Keystore-сессию до/после transport. Phone test scope
не открывает раздел. Snapshot revision связывает список проверок с выбором;
изменение revision закрывает старый выбор. ACK проверяет vehicleId/kind/mode/
результат/revision и vehicleRevision+1. После ACK перечитывается список.
Сетевой неопределённый ответ хранит только исходный RAM intent: явный повтор
после нового подтверждения использует прежние account/scope/body/key/revision.
Обновление само не повторяет mutation.401/403/409 закрывают выбор. onStop
отменяет запросы и очищает причины/selection/intent/snapshots; диск, clipboard,
raw logging, autofill и IME personalization не используются. FLAG_SECURE
защищает экран и подтверждение; touch obscuring protection сохранён.

Добавлены9 JVM cases,6 actual Keystore/transport instrumentation cases и8
Activity cases, включая10 synthetic native PNG light/dark+font1.3. Сценарии
пока не объявлены PASS до CI. Workflow требует их исполнения и проверяет
PNG provenance. Existing owner-phone home test дополнен запретом новой кнопки.
Статические privacy/config guards распространены на новый XML/Activity.

При сверке двух серверных режимов воспроизведён старый дефект immediate phase
advance; исправление внесено в backend SQL отдельным commit080d4b49bb902d782efe2615d92bf05fa3d64dc6.
Проверено12 новых SQLite/HTTP cases, в том числе два режима и atomic race.
Native D1 harness также проверяет оба режима; нужен итоговый CI этого source.
Код классификатора131/142 не активирован; G1paused/D4–D7pending, OWNER-only
phone scope не расширялся, реальные материалы/merge/deploy не выполнялись.


## UI finish review: длинные режимы получили перенос строк — 2026-11-16

Независимый reviewer исходногоa460273f не обнаружил source-level session/permission
blocker, но потребовал проверить длинные режимы. Проверен SDK layout:
simple_spinner_dropdown_item задаёт singleLine=true/fixed height/marquee.
Системный однострочный шаблон применялся к selected view, что ограничивало
видимость полного последствия режима. Выбран минимальный fix: native TextView/
CheckedTextView wrap-content, singleLine=false, min48dp и существующая Material3
BodyLarge типографика для selected/dropdown. В форме и подтверждении оставлена
краткая make/model/plate/ID summary; полные даты/статусы остаются в read screens.
Native test проверяет перенос длинного immediate label. Это изменение UI требует
нового Android CI и кадров конечного source; предыдущий run не заменяет их.


## Исправлено UX-сообщение при истечении MFA в открытой форме — 2026-11-16

Независимый finish reviewer подтвердил P3: после истечения local MFA create()
останавливал команду, но общий validation catch показывал photo_required
вместо permission и оставлял форму. Обхода проверки/POST/утечки не было.
prepare теперь отдельно проверяет текущий access.canManagePhotoChecks перед
валидацией полей; закрывает старый выбор, очищает причину/снимки и показывает
существующее permission сообщение. Добавлен native regression с истёкшим
server MFA profile, заполненной корректной причиной,0 POST и очищенной формой.
Итого15 новых native cases (6 repository+9 Activity); новый source требует
свежего CI, предыдущий native run не объявляется проверкой этого изменения.


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

При подготовке docs-only публикации первый локальный JSON payload был
отклонён парсером из-за предупреждения оболочки перед JSON. Изменений remote
на этом шаге не было. Повтор с login=false дал корректный payload; создаваемые
GitHub trees побайтно совпали с локальным staged tree. Код/проверки не изменены.

## Следующий шаг после фотоконтроля — 2026-11-16

По поручению «Далее» сверены план, ТЗ §9, утверждения состава 142/131 и код
регистрации в ветках фотоконтроля. Driver и backend принимают текст марки и
модели; normalizeVehicle не содержит года выпуска. Текущий контракт не позволяет
проверять утверждённые модельные пороги года. Утверждение состава не включало
UI/API/миграцию/допуск. Подготовлено G2_VEHICLE_CATALOG_NEXT_2026-11-16.md:
вариант A — справочник марок с ручным вводом прочей марки; вариант B требует
отдельных решений о годе, отсутствующих моделях, примечаниях и модерации.
Рекомендуется A; выбор и внедрение ожидают явного утверждения владельца.

При чтении справочного предложения первая команда была направлена в backend,
где документа марок нет; путь исправлен и документ прочитан в Android worktree.
Это документальная сверка кода, не новые runtime тесты; код, данные и schema не
менялись. G1 paused, D4–D7 pending, OWNER-only scope и запрет merge/deploy
сохраняются; реальные материалы и удалённая инфраструктура не затрагивались.

## Разбор внедрения автомобильного справочника — 2026-11-16

По поручению владельца подготовлен VEHICLE_CATALOG_INTEGRATION_ANALYSIS_2026-11-16.md.
Проверены поля Driver, серверная нормализация и SQL изменения автомобиля,
исходная схема и CSV:131 правило, распределение76/31/24,28 непустых примечаний,
нет повторённых точных пар brand/model. Выявлена зависимость: текущий список
critical fields не включает make/model/vehicleClass; SQL сбрасывает указанные
статусы только при criticalChange. Эксплуатационный обход и иные защитные
контуры в этом документальном разборе не проверялись. Зафиксированы вопросы
о годе, неизвестных моделях, классе, повторной модерации и примечаниях.
При двух поисковых командах использованы несуществующие glob/путь в backend;
конкретные файлы после этого прочитаны по корректным путям. Проверка только
статическая и подсчёт CSV, runtime тестов или изменений кода нет. Все варианты
внедрения остаются предложениями до решений владельца. G1 paused; D4–D7 pending.

## Решение владельца: класс выбирает система — 2026-11-16

Владелец уточнил: «Сейчас водитель выбирает класс автомобиля - это не правильно.
Класс автомобиля должна выбирать система». Это утверждённое требование;
предложения ручного выбора водителем или администратором как окончательного
класса отозваны. На проверенном source Driver показывает classInput и передаёт
vehicleClass, backend принимает его, а vehicles.class NOT NULL; текущее
поведение этому требованию не соответствует. Удаление одного UI поля сломало бы
действующий контракт и само по себе не обеспечило бы системный выбор.

В анализ добавлено решение владельца. Сопоставление с 131 правилами требует
года, условий и решения для отсутствующих/неподходящих данных; ранее вопрос
о неизвестной модели остался без ответа. Модель хранения промежуточного
состояния, миграция и совместимость старых клиентов также не утверждены.
Runtime код, удалённая инфраструктура и данные не изменялись, тесты реализации
не выполнялись; G1 остаётся приостановлен. Дальнейший контракт подготовить
после решений об отсутствующих моделях и непроверенных условиях, без подмены
класса предположением. Запись в отдельной локальной ветке arch/g2-vehicle-catalog-next.

## Решение владельца: ручная проверка модели и года — 2026-11-16

Владелец: «если модели нет в утверждённом списке или она не соответствует
условию по году - отправлять на ручную проверку». Зафиксированы обе причины;
само направление не означает автоматического отказа, одобрения или выбора
класса. Разработан VEHICLE_SYSTEM_CLASSIFICATION_CONTRACT_PROPOSAL_2026-11-16.md
с явным разделением решения владельца и оставшихся вопросов об исходе проверки,
источнике года, условиях и повторной модерации. Анализ данных/кода опирается
на предыдущие записи; изменений runtime и прогонов тестов не производилось.

В начале работы вызов ожидания среды содержал ошибочный ID и был отклонён;
затем среда предоставила ready-контекст. Две попытки исправить строку документа
не применились из-за несовпадения контекста; правка сделана точным контекстом.
Отдельная локальная ветка; G1 paused, remote/merge/deploy не выполнялись.

## Решение владельца: допуск исключения администратором — 2026-11-16

Владелец: «Администратор может допустить автомобиль как исключение по марки
автомобиля и соответственно система по марке автомобиля соответствующей модели
определяет класс». Возможность исключения утверждена, класс выбирает система.
Документы обновлены без предположения об автоматическом определении соответствия
для отсутствующей в CSV модели. Проверено группирование всех131 строк по
точному brand: 53 марки, из них11 имеют модели разных классов; Toyota Camry —
Комфорт, Toyota Alphard — Минивэн. Одна марка недостаточна для однозначного
класса, поэтому требуется уточнение способа выбора соответствующей модели.
Для известной модели и года ниже порога соответствующее правило уже есть.
Год, текстовые условия и порядок повторной модерации остаются отдельными
открытыми вопросами. Runtime кода/данных/инфраструктуры не меняли; тесты
реализации не заявляются. Решение записано в локальной отдельной ветке.

## Решение владельца: соответствующая модель при исключении — 2026-11-16

На предложение «администратор указывает соответствующую модель из утверждённого
справочника, система берёт её класс» владелец ответил: «Такой порядок подходит.
Или как вариант администратор вручную может указать класс при исключении».
Первый способ подтверждён. Второй записан как предложенный вариант; неясно,
дополняет ли он первый или заменяет его, и является ли исключением из ранее
утверждённого системного выбора класса. До уточнения ручной выбор не внедряется.
Водитель класс не выбирает. Исходную модель и выбранное соответствие предлагается
хранить раздельно; это техническое предложение, не новое решение о допуске.

Обновлены контракт и анализ; runtime код/схема/данные не менялись, тесты
реализации не выполнялись. Остаются решения по году, условиям и повторной
модерации. Работа сохранена в отдельной локальной ветке; G1 остаётся на паузе.

## Итоговое решение: модель из справочника, класс определяет система — 2026-11-16

Владелец: «Оставим модель из справочника, а система определяет сама класс».
При исключении администратор указывает соответствующую модель из утверждённого
справочника, её класс определяет система. Предложенный ранее ручной выбор
класса администратором не выбран; водитель также класс не выбирает. Обновлены
проект контракта, анализ и предложение, история обсуждения сохранена в журнале.
Вопросы источника/подтверждения года, выполнения текстовых условий и повторной
модерации остаются открытыми. Код, инфраструктура и данные не изменялись;
runtime тесты не проводились. Запись находится в отдельной локальной ветке.

## Решение владельца: ввод и подтверждение года выпуска — 2026-11-16

На предложение «водитель указывает год выпуска при регистрации, а администратор
подтверждает его по документам» владелец ответил: «По году всё верно.
Утверждаю». Это правило утверждено; повторно его не запрашивать. Ранее
установленные правила ручной проверки модели/года и системного выбора класса
по справочнику сохраняются. Контракт, анализ и обзор актуализированы.

Порядок перехода существующих автомобилей без года, исправления ошибочного
года, подтверждения иных условий модели и повторной модерации отдельно не
определён; утверждение ввода года не означает утверждения этих вариантов.
Runtime код/схема/данные не менялись, тесты реализации не выполнялись.
Изменения и эта запись сохранены в локальной ветке arch/g2-vehicle-catalog-next.

## Уточнение владельца о типе кузова — 2026-11-17

Владелец: «В тарифах может быть как седан так и кроссовер». Записано: общее
ограничение тарифа только седанами вводить нельзя. Проверены примечания CSV
по словам «седан», «кузов», «кроссовер»: «только седан» содержат Mercedes-Benz
E-Class, BMW 5 Series и Audi A6; Avatr 12 и Zeekr 001 имеют отдельные условия
согласования кузова. Применимость указанных трёх прежних модельных ограничений
требует уточнения; CSV и иные утверждённые условия не изменялись. Общее
утверждение администраторской проверки условий этим ответом не предполагается.
Документальная проверка; runtime и тесты не изменялись. Локальная ветка.

## Уточнение области тарифа — требуется расшифровка, 2026-11-17

В ответ на вопрос об ограничениях кузова трёх моделей Бизнес владелец написал:
«Бизнес и минивэн не включаем. Только для комфорта». Неоднозначно, означает ли
это исключение тарифов Бизнес/Минивэн из продукта и справочника либо ограничение
текущего обсуждения/внедрения тарифом Комфорт. Ранее утверждены все три тарифа
и 131 правило: 76 Комфорт, 31 Бизнес, 24 Минивэн. Все три строки с пометкой
«только седан» относятся к Бизнес. Запрошено уточнение до изменения данных,
состава, кода или определения области внедрения; решение не предполагается.
Документальная сверка CSV; runtime тесты не выполнялись, изменения в локальной
ветке arch/g2-vehicle-catalog-next, G1 остаётся на паузе.

## Решение владельца: тип кузова по тарифам — 2026-11-17

Владелец уточнил: «Только седан в бизнесе, а в комфорте седан и кроссовер».
Утверждённое правило: Бизнес — только седан, Комфорт — седан и кроссовер.
Это уточняет предыдущую общую запись о кузовах; отдельного правила для
Минивэна этот ответ не устанавливает. Предыдущий вопрос об исключении тарифов
не считать подтверждением их удаления; речь владельца теперь явно о кузовах.

При проверке31 строки Бизнес обнаружены Li Auto L7/L9, ранее утверждённые с
условиями проверки салона, и Avatr 12/Zeekr 001 с отдельными условиями по кузову.
Необходимо сверить конкретные исполнения и уточнить обработку возможного
несоответствия новому правилу; автоматически переносить тариф, удалять строки
или считать исключения утверждёнными нельзя. CSV/runtime не изменялись,
проверка документальная. Решение и вопрос сохранены в локальной ветке.


## Решение владельца: Li Auto L7/L9 и тип силовой установки — 2026-11-17

Владелец: если L7/L9 только электромобили — исключить, если гибрид, дизель
или бензин — изменить правила. Официальные материалы Li Auto подтверждают
для изученных L7/L9 гибридную схему с 1,5-литровым бензиновым ДВС-генератором:
https://ir.lixiang.com/news-releases/news-release-details/li-auto-inc-launches-li-l7-five-seat-flagship-family-suv
https://ir.lixiang.com/static-files/743d9acf-2fdf-4fa2-8219-f28e41237bd6
При сохранении правила Бизнес только седан обе модели L7/L9 переведены в
Комфорт в backend-ветке arch/g2-li-auto-comfort. Их год2023, статус approved
и условие «Только комфортные комплектации; проверка салона» сохранены.
Проверка сравнения с предыдущим Git HEAD: 131 строка, изменены ровно два
значения тарифа, прочие поля и порядок сохранены; 78 Комфорт/29 Бизнес/
24 Минивэн, 131 approved. CSV SHA256
561c53347658f732724a78894dd5de933e1e634370730e7b0b46ac901cabcd93.
Архив v1 не менялся; исторические документы сохраняют старые показатели.
Подтверждать тип конкретного автомобиля по версии/документам; общая политика
для других электромобилей этим решением не установлена. Runtime/production
не менялись, сборка и live тесты не проводились. Документальные изменения в
отдельных локальных ветках; remote/merge/deploy не выполнялись.


## Решение владельца: Li Auto L7/L9 остаются в Бизнесе — 2026-11-17

Владелец уточнил: «кроссоверы Li Auto L7 и L9 оставляем в бизнесе». Это
конкретное исключение для L7/L9 из правила о седанах в Бизнесе. Предыдущий
перенос этих двух моделей в Комфорт отменён; L7 и L9 снова Бизнес, год от
2023, approved и прежнее условие проверки комплектации/салона сохранены.
Другие исключения данным решением не устанавливаются.

CSV проверен сравнением: ровно два обратных изменения тарифа, все прочие поля
сохранены; файл побайтно совпал с версией до переноса. 131 approved,
76 Комфорт / 31 Бизнес / 24 Минивэн; SHA256
1e4955a6f7250f0e5183f99d105a5c5b235eaa51b7ee432e2f29ad2a69af4901.
История в журнале сохранена; документ LI_AUTO_L7_L9_DECISION_2026-11-17.md
отмечает отмену переноса. Правила Avatr 12/Zeekr 001 отдельно не менялись.
Документальные изменения в локальных ветках arch/g2-li-auto-comfort и
arch/g2-vehicle-catalog-next; runtime, merge/deploy и live тесты не выполнялись.


## Открытые решения перед внедрением классификатора — 2026-11-17

По запросу владельца повторно сверены утверждённые решения и проект контракта.
Открыты: подтверждение и последствия невыполнения условий модели; переход для
уже зарегистрированных автомобилей без года; изменения ранее проверенных
марки/модели/года и активной машины; отдельное решение по кузову Avatr 12/
Zeekr 001; границы выбора соответствующей модели при исключении (включая
отсутствие подходящей модели); общий порядок для иных электромобилей, если
он нужен. Системный выбор класса, ввод/проверка года, ручная проверка и
исключение Li L7/L9 повторного утверждения не требуют. Техническое решение
схемы/API/миграции и проверок подготовить для рассмотрения согласно правилам
проекта; это перечень, не новое решение владельца. Код/данные не менялись,
сборки и runtime тесты не выполнялись. Запись в отдельной локальной ветке.


## Решения владельца: изменение автомобиля, исключения и электромобили — 2026-11-17

Владелец: при изменении марки/модели/года система повторно определяет класс,
направляет автомобиль на проверку; автомобиль не допускается на линию. При
исключении администратор может внести все необходимые параметры, после чего
класс определяет система. Ранее утверждённый выбор соответствующей модели
из справочника не отменён; уточнить его применение, если соответствия нет.
О влиянии запрета на уже выполняющуюся поездку решение отдельно не заявлено.
Другие только электромобили исключить из списка; тип конкретного исполнения
важен, поскольку у ряда моделей есть гибридные и полностью электрические версии.

Проверены источники, зафиксированные в backend документе
ELECTRIC_VEHICLE_REVIEW_2026-11-17.md; из локального актуального CSV исключены
10 подтверждённых BEV моделей: Mercedes-Benz EQS, BMW i5/i7, NIO ET7, IM Motors
L7, Luxeed S7, Xpeng P7, Xiaomi SU7, Zeekr 001/009. Сохраняются Li Auto L7/L9
как утверждённое исключение и модели со смешанными версиями; для последних BEV
должны исключаться по силовой установке конкретного автомобиля. Остальные
модели/годы не проверены полностью. По известным данным 009 может получить
будущий гибрид; сам по себе анонс не является допуском или запретом на будущее.

Сравнение CSV с HEAD до удаления: ровно10 удалений,121 оставшаяся строка
сохранена, 76 Комфорт/22 Бизнес/23 Минивэн, все approved; SHA256
553384af5b49252d3b3acbc3973ef6dae7fc40d348156debf4f0fab24c5ae880.
Проверка BEV не завершена, перечисленный состав промежуточный; поле типа
силовой установки в существующем CSV отсутствует, runtime enforcement нет.
Runtime, schema, real data, merge/deploy не изменялись; тесты продукта не
выполнялись. Две локальные отдельные ветки, G1 остаётся на паузе.


## Решение владельца: остановка и переназначение во время поездки — 2026-11-17

На вопрос о смене марки/модели/года во время уже начатой поездки владелец
ответил: «Лучше остановить и переназначить водителя». Зафиксированы остановка
выполнения поездки с прежним назначением и переназначение водителя; прежнее
правило не допускать автомобиль на линию до проверки сохраняется. Завершение
старым водителем не предполагается. Способ безопасной остановки, пассажирский
сценарий и ручной/автоматический выбор замены этим ответом не определены;
их нужно конкретизировать без подмены решения об остановке.

Текущий backend имеет сервис reassignOrderByAdmin с проверкой reassignable;
его пригодность для поездки в движении и действие на реальной инфраструктуре
в этом обзоре не подтверждены. Контракт обновлён, runtime/схема/данные не
изменялись, тесты реализации не выполнялись. Текущая проверка электромобилей
и локального списка из121 правила остаётся неполной.

Первая попытка git add содержала неверную дату в имени файла контракта;
файлы не были добавлены. Путь исправлен; проверка staged diff выполнена.


## Решение владельца: нового водителя выбирает диспетчер — 2026-11-17

Владелец уточнил: «Нового водителя только диспетчер вручную». При остановке
уже начатой поездки из-за изменения марки/модели/года переназначение выполняет
диспетчер вручную; автоматически выбирать нового водителя не разрешено.
Ранее утверждены остановка, повторная проверка и запрет выпуска автомобиля
на линию. Контракт обновлён, повторно эти решения не запрашивать.

Статическое чтение backend: существующий reassignOrderByAdmin проверяет право
orders.reassign, ревизию и статус заказа taken, а SQL — кандидата и активное
назначение. Проверка не доказывает пригодности для остановленной поездки с
пассажиром или live работы; безопасный порядок остановки и пассажирский
сценарий остаются для проработки. Код/схема/инфраструктура не менялись,
тесты реализации не выполнялись; отдельные локальные ветки, G1 на паузе.


## Открытые вопросы после решения о диспетчере — 2026-11-17

По запросу владельца сверены актуальный проект контракта и промежуточный обзор
силовых установок. Для дальнейшего проектирования уточняются: действия для
безопасной остановки и пассажира при замене водителя; переход для существующих
авто без подтверждённого года; подтверждение и последствия невыполнения
примечаний конкретной модели; действия если администратор не находит подходящую
модель для сопоставления; источник подтверждения типа силовой установки.
Решения об остановке, ручном выборе диспетчером, системном определении класса,
исключении BEV и проверке года не ставятся на повторное голосование. Проверка
электрических исполнений в 121 оставшейся строке ещё не завершена. Данная
запись не утверждает варианты решений. Runtime/данные не менялись, тестов
реализации нет; локальная отдельная ветка, G1 остаётся приостановленным.


## Решения владельца по пяти вопросам классификатора — 2026-11-17

Владелец ответил на пять вопросов: 1) подтверждён безопасный порядок остановки:
водитель останавливается в безопасном месте и сообщает местоположение, диспетчер
вручную назначает замену и информирует пассажира; 2) ранее зарегистрированный
автомобиль без года не допускается на линию до указания и подтверждения года;
3) невыполненное условие устранить, если устраняемое, а также рассмотреть
исключение; 4) если подходящей модели нет, разрешить администратору внести
модель в список и изменить правило; 5) конкретное гибридное/электрическое
исполнение проверять по документам. Сохраняются системный выбор класса и
предыдущие решения владельца, исключение не становится автоматическим допуском.

Полные ответы отражены в Android-документе
VEHICLE_RULES_OWNER_DECISIONS_2026-11-17.md и проекте контракта. Нужны
технический дизайн изменения справочника, версия, права и аудит, совместимость
со старыми данными/клиентами и тесты; исполнение и результаты тестов пока
не заявлены. Проверка полностью электрических исполнений 121 оставшейся строки
ещё не завершена. Код, схема, реальная инфраструктура не менялись; две
локальные рабочие ветки, G1 на паузе.


## Продолжение: проверка справочника и технический проект — 2026-11-17

Владелец: «Продолжаем». Повторно сверены актуальные локальные ветки и решения.
Продолжена проверка установок: источники производителей подтверждают наличие
разных типов исполнения Citroën SpaceTourer и Opel Zafira (ссылки в backend
ELECTRIC_VEHICLE_REVIEW_2026-11-17.md); конкретные поколения/годы надо проверить.
Изменений списка в этом шаге нет. Ревизия BEV всех оставшихся строк не завершена.

Добавлена выполняемая документальная проверка
`python tests/vehicleClassifierReference.test.py -v`: 6 PASS. Проверены CSV
структура,121 approved/76/22/23, отсутствие10 установленных BEV, сохранение
моделей смешанных типов, двух Li Auto исключений,112 исходных правил и
оставшихся китайских дополнений с их утверждёнными условиями. Не проверены
все существующие силовые установки и runtime ограничения по конкретной версии.

Независимо выполнены существующие тесты backend: npm run test:driver-registration
PASS, npm run test:admin-authorization PASS, npm run test:admin-vehicle-read
12/12 PASS. Это регрессии старого кода, не подтверждение нового классификатора.
Подготовлен Android-документ VEHICLE_CLASSIFICATION_IMPLEMENTATION_DESIGN_2026-11-17.md:
системный класс, проверка установок, администрирование правил, миграция,
перепроверка при изменении и безопасная остановка с ручным назначением.
Технические интерфейсы, влияние изменений правил на действующий автопарк и
полная миграция требуют конкретизации; автоматический допуск не объявляется.
Runtime код, схема и инфраструктура не менялись; G1 paused; merge/deploy нет.


## Решение владельца: изменённые правила — новым регистрациям — 2026-11-17

На вопрос, отправлять ли ранее одобренные автомобили на повторную проверку
после изменения администратором правила модели или применять изменение только
к новым регистрациям, владелец ответил: «Только к новым регистрациям». Одно
изменение правила не запускает массовую повторную проверку ранее одобренных
автомобилей и не должно автоматически менять их класс. Не отменено отдельное
решение: при изменении марки/модели/года самого автомобиля повторно определить
класс, направить на проверку и не выпускать на линию. Выбор версии правила
при таком отдельном изменении не следует выдумывать: эти случаи различаются.

Технический проект и проект контракта актуализированы; нужна привязка принятого
правила/версии и тесты, исключающие ретроактивное применение. Код, schema,
продуктовые данные и инфраструктура не изменялись, новые runtime проверки
не выполнялись. BEV ревизия 121 оставшейся строки остаётся незавершённой.
Первый вызов ожидания среды содержал неверный идентификатор, был отклонён;
затем доступен готовый контекст. Локальные рабочие ветки, G1 на паузе.


## Начата серверная логика классификации — 2026-11-17

По поручению «Всё верно. Продолжаем» в backend локально добавлен изолированный
`src/core/vehicleClassification.js`, не подключённый к регистрации, маршрутам
или базе: по переданному доверенному сервером правилу и проверенным параметрам
возвращает candidate с версией, ручную проверку для отсутствующей модели или
недостаточного года, excluded для BEV. Candidate не является одобрением,
допуском на линию или подтверждением текстовых условий. Неизвестный тип
установки/ошибочный ввод безопасно отклоняются; неподтверждённый тип направляет
на ручную проверку без класса. `powertrainVerified` допустим только из серверной
модерации, не из запроса водителя; механизм этого доверия ещё не реализован.

Правило по конкретной версии передаётся извне: сохранение и выбор версии,
применение изменений только новым регистрациям и исторические автомобили ещё
не реализованы. Соответствие марки/модели точное, автоматические алиасы/догадки
не вводились. Смена полей/остановка поездки/назначение диспетчером, проверка
документов, управление справочником, миграция и запись событий не подключены.
Нельзя выпускать этот компонент как реализованное системное правило.

Добавлен `npm run test:vehicle-classification` в npm test. Локально 7/7
unit cases PASS, тесты справочника Python 6/6 PASS, существующие
test:driver-registration и test:admin-authorization PASS. Проверка снапшотов
подтверждает только чистую функцию, не БД/миграцию и не применение правила
только к новым регистрациям. Полный CI, native Worker/D1, интеграция, Android,
безопасность всего приложения и оставшиеся электроверсии не проверены этим
шагом. Код/тесты только в отдельной локальной ветке backend; обе журнальные
записи локальны, нет deploy/merge/реальных данных, G1 paused.


## Решение владельца и backend version-store PR33 — 2026-11-17

При изменении марки/модели/года самого автомобиля владелец выбрал текущую
версию справочника («По текущей версии»). Контракт и технический проект
обновлены; новая версия правила сама по себе не пересматривает весь автопарк.
На backend создана отдельная ветка arch/g2-classifier-version-store и Draft PR33:
https://github.com/lobanov0710-blip/uber-v3/pull/33 . Source
3583ca6f9c341730d2a18cb3dcdc63fe6ac6dcf2/tree2dd1f4c3db55e50f1a69c0a22654aa74c604ca67,
GitHub tree побайтно совпал с локальным. CI44051103873 запущен, итог ещё не
зафиксирован. Реализован внутренний version store, не подключённый к маршрутам.

Локально12 actual SQLite store cases,7 evaluator,6 reference PASS;19 migrations
smoke и12 D3 preflight PASS. Полный npm test остановился на OWNER CLI spawnSync
EPERM (sandbox); проверки не ослаблены и должны пройти в CI. Добавлен native
workerd/D1 workflow, включая immutable triggers. При самопроверке harness
исправлена race-инъекция после bind (native D1 возвращает отдельный statement).
Нет auth/ownership API или доверенной верификации установки из driver запроса,
нет регистрации/Android/линии/стопа/назначения/seed/runtime включения.
BEV ревизия121 строк остаётся незавершённой; merge/deploy/remote migration нет.


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


## Version store: финальный CI PASS — 2026-11-17

Code aef9dfc77dc861a46e97792a27773cb4c5f6542b,
tree c8bfbb39385e494e17112f2d0e87711cca0cf5dc,
CI44052040564/job133931991226 SUCCESS.71 npm test scripts,7 evaluator cases,
12 actual SQLite store cases,6 reference checks,19 migrations,12 preflight,
7 native local workerd/D1 workflows,6 protected CLI scenarios,audit0 и
Worker bundle dry-run PASS. Полный CI преодолел локальную spawnSync EPERM
границу без ослабления assertions. Guard OWNER setup по18-миграциям не
изменялся; шестой сценарий подтвердил отказ на19 до prompts/import/upload.
Первый failed CI сохранён в истории, не считается успешным.

Backend Draft PR33, Android docs Draft PR13. Опубликованные feature trees
сравнены с local staged trees; Android docs payload сначала превысил лимит
вывода и не распарсился, remote mutation не было; повторное чтение файлами/
частями успешно дало точное tree parity. Локальные source hashes в receipt
VEHICLE_CLASSIFICATION_VERSION_VERIFICATION_2026-11-17.json. Последующие
изменения только документация; проверенный code SHA указан отдельно.
Это внутренний компонент, не завершение регистрации/прав/MFA API/модерации/
линии/поездки/Android. Ни snapshot candidate, ни CSV не дают допуска на линию.
Не выполнялись seed, remote migration, реальные документы, merge/deploy.
Полный обзор электрических исполнений и интеграция остаются дальнейшей работой.


## Следующий срез: schema и чтение регистрации — 2026-11-17

По поручению «Далее» подготовлен backend Draft PR34
https://github.com/lobanov0710-blip/uber-v3/pull/34 в отдельной ветке
arch/g2-classifier-registration-schema. Миграция0020 поддерживает реальный
null-класс, model_year и powertrain без присвоения фиктивных значений. Старые
классы и связи сохраняются через DROP/ADD COLUMN без DROP TABLE vehicles.
Triggers запрещают approved при null-классе. Внутренний insert draft и
owner-bound metadata read подготовлены, GET Driver me включает новые поля.

8 actual SQLite/HTTP cases PASS,20 migration smoke PASS. На заполненной19
базе проверены все таблицы/связи/история и rollback. Первый расширенный тест
ошибочно вызвал новую read-функцию до миграции; проверка исправлена на прямое
сравнение старых данных и чтение новой функцией после0020. Существующие
registration и version-store tests PASS; локальный полный npm test остановлен
на известном CLI spawnSync EPERM, не ослаблен. Native D1 harness проверяет
реальный populated upgrade и отдельный откат намеренно ошибочной миграции.
Первый run44055638704 отменён после добавления rollback case; final source
0de3ffd83e9adebc1c2409bce9f0a2082733c3bd / tree1759e9b423af9e7693e0a2d7a6c61f4e668bb345,
CI44055860164 ещё выполняется. Local/remote source tree parity проверена.

Важно: публичные POST/PATCH всё ещё требуют прежний vehicleClass и не
принимают modelYear. Старый Android DTO не готов к null. Это storage/read
срез, не полная интеграция классификации/верификации документов/линии/поездки.
Не выполнялись remote migration, merge/deploy, seed и реальные документы.
Обзор BEV121 остаётся незавершённым, G1paused.


## Schema/чтение регистрации: итоговый CI PASS — 2026-11-17

Source0de3ffd83e9adebc1c2409bce9f0a2082733c3bd,
tree1759e9b423af9e7693e0a2d7a6c61f4e668bb345,
CI44055860164/job133944966595 SUCCESS.72 npm test scripts,8 новых actual
SQLite/HTTP cases,20 migrations,12 D3 preflight,6 reference tests,8 native
D1 workflows,6 protected CLI scenarios,audit0 и Worker dry-run PASS.
Native D1 подтвердил сохранность populated upgrade19→20, rollback намеренной
ошибки, null draft, owner-bound read и запрет approved без класса.
Первый run44055638704 отменён после добавления rollback проверки, не считается
финальным PASS. Локальный EPERM не обходился; полный CLI проверил CI.

Backend Draft PR34, local/remote tree parity проверена. Точные hashes и
границы в VEHICLE_REGISTRATION_SCHEMA_VERIFICATION_2026-11-17.json.
Следующие изменения только docs [skip ci], проверенный code SHA не подменять.
Публичные POST/PATCH сохраняют прежний driver-supplied vehicleClass и не
принимают modelYear; Android DTO ещё не готов к null. Реализованы только
storage/read prerequisite, не полная классификация, не проверка документов,
не line/trip guards. Не включать null drafts через старые клиенты.
Нет seed/полного BEV обзора/remote migration/merge/deploy/реальных данных,
G1paused. Далее согласованно обновить API записи/клиент и trusted moderation.


## Driver: начато согласованное обновление формы системного класса — 2026-11-17

По поручению «Далее» отдельная ветка arch/g2-driver-system-class-ui. Удалён
classInput/выбор класса и отправка vehicleClass; добавлен modelYear, read-only
серверный класс и явное отсутствие класса до проверки. DTO принимает null
для неутверждённого авто, сохраняет неизвестный старый год как null и отвергает
approved без класса/нецелый или строковый год. UI очищает год при onStop,
не сохраняет его в view state/autofill; приватность и session guards сохранены.

Добавлены3 JVM случая и3 native UI случая: смена года с ACKstop, потерянный
ACK без replay, ошибка чтения после ACKstop. Старый native create/edit scenario
теперь проверяет modelYear и отсутствие class в запросе. Добавлены2 actual
native class PNG light/dark1.3 и artifact tested Driver debug APK. Security
guards локально8 Driver/5 Admin PASS. Gradle/native на этой среде не объявлены
PASS; штатный CI обязателен. Первый большой read был усечён, нужные методы
прочитаны отдельно; один поиск tests/src-router выполнялся не в том repo,
исправлено чтением backend-файлов. Настройки/secret/provider flags не менялись.

Backend одновременно меняет контракт create/patch и атомарно сбрасывает
класс/линию при изменении марки/модели/года, запрещает дальнейшие phase/done
переходы; выбор замены остаётся ручным диспетчером. Доверенная модерация
года/установки и UI редактирования правил не включаются этим срезом, новый
автомобиль остаётся draft без класса. Нет новых миграций сверх0020, seed,
реальных документов/remote migration/merge/deploy; G1paused.


## Driver system class: Android/backend проверены — 2026-11-17

Android source58102355b24201f6eba06f2d5190341c7541be06,
tree104a42432e186fe881ecc1b0d89a9a6740d139f8, CI44060885312/job133962153709
SUCCESS. Driver51 JVM на каждый из3 variants, Admin58 на каждый:327 JVM
executions. Driver41/Admin61 native:102 runtime, без failure/error/skipped.
Сборки и lint debug/staging/release обеих apps, endpoint locks, отдельная
подпись/UID Admin, App Check policy, security guards8/5 PASS. Новый native
набор проверил ACKstop, потерю ACK без replay и failed GET после ACKstop.

Backend source57c17bbc67b5c8aef3f137d9c13341f619dbd8a2, CI44062029259/
job133966070138 SUCCESS:73 scripts,8 новых actual SQLite/HTTP cases,20
migrations,12 preflight,6 reference,9 native D1 workflows,6 protected CLI
scenarios,audit0,dry-run PASS. Draft PR35 зависит от schema PR34; Driver PR14
зависит от docs PR13. Полные source/artifact hashes и границы сохранены в
DRIVER_SYSTEM_CLASS_VERIFICATION_2026-11-17.json; проверенный code SHA отделён
от следующих docs-only commits. Source trees до публикации сверены побайтово.

Скачаны именно artifacts этого source/run:14319405601 nativePNG и14317963902
DriverAPK. ZIP SHA256 совпали с GitHub digest; safe extraction, PNG CRC/viewport
и hashes всех30 кадров совпали с CI log. Primary agent осмотрел4 изменённых
vehicle/class light/dark font1.3 кадров: год/подсказка/readonly class читаемы,
наложения/обрезки целевых строк не обнаружены. Это собственный осмотр4 кадров,
не независимый обзор всех30. Driver APK hash
a1d818962811625ff7506b710a34f04846003070a0f1e19ba5b133644a980829.
APK содержит synthetic Firebase/ephemeral debug signing; phone upgrade, live
Driver или production этим не проверены, устанавливать вместо релиза не поручалось.

Ошибки инструментов: повторный Admin security check сначала вызван по
несуществующему admin/tools пути; исправлен на tools/check_admin_security.py,
5/5 PASS. В metadata технического вывода загрузчика ошибочно выведены временные
ссылки двух synthetic CI ZIP; URL не включены в файлы/PR/receipt, далее только
file_id. Их указанное время expiry10:19:35UTC; это не реальные учётные данные
и не app/provider credentials. Не повторять полный metadata output. Backend
publish read был неполным; tree parity остановила обновление ветки, отдельное
чтение дало точное дерево. Полный local npm ограничен sandbox на OWNER CLI,
но CI проверил этот набор без ослаблений. Исторические pending записи выше
относятся к прежним checkpoint; новый write API уже не принимает driver class.

Далее по утверждённому плану: trusted Admin проверка года/силовой установки,
current-rule classification/snapshot, legacy year guard, правила/исключение;
также полный in_progress handoff/пассажирское уведомление и завершение BEV
обзора. Этот срез не завершает весь справочник или G2. Нет seed, реальных
документов, remote migration, merge/deploy; G1paused,D4–D7pending.

Финальный docs payload большого плана сначала превысил output limit и не
распарсился; remote mutation не выполнялась. Файлы перечитаны частями по5000
символов с проверкой длины, перед обновлением ветки проверяется точное tree.


## Следующий Admin/system-class этап: аудит и проект контракта — 2026-11-17

По поручению «Продолжаем» среда восстановлена wait_for_environment, сверены
AGENTS, ТЗ/D1–D3, owner decisions, план и финальные записи предыдущего этапа.
Отдельные ветки arch/g2-admin-classification-contract обоих repo созданы от
проверенных feature trees backend56693ae2/Driver2cebbc55; remote parents
5adfd2a6 и57f69b30 совпадают с локальными деревьями.

Сверка выявила8 открытых blockers следующего внедрения: Admin metadata/DTO
без года/trusted результата и null→empty class; approval и line/assignment SQL
без новых trusted guards;0019 FK/minYear не представляют модель-основание
исключения; общая revision не должна автоматически выбирать новое правило;
evaluator не подтверждает кузов/notes; права/API авторства не выбраны; BEV
обзор121 строк не завершён. Это не объявление исправления или production
эксплуатации найденного. Runtime код в этом шаге не меняется.

Подготовлены подробный ADMIN_VEHICLE_CLASSIFICATION_CONTRACT_PROPOSAL_2026-11-17.md,
машиночитаемый OpenAPI и audit JSON в backend; Android next-step документ
связывает их с формой/допуском и критериями приёмки. Предложены новые права
vehicle_rules.manage/vehicles.approve_exception без auto-grant; текущий
vehicles.verify для проверки параметров; правило закрепляется при первом
сохранении автомобиля, сохраняется при unrelated revisions, смена make/model/
year использует текущую версию. Сопоставление той же марки, immutable assessment,
условия/документы и hard BEV/business-body guards; класс только от сервера.
Старый approved route тоже обязан проверять trusted результат в будущей SQL.

Новые права и момент фиксации версии ожидают явного решения владельца. Это
не повтор бизнес-вопросов, уже решённых им. AGENTS п.3 запрещает заменять
отсутствующее решение предположением; owner decisions требуют согласованный
контракт правил до реализации. Независимая подготовка завершена до запроса;
handlers/grants/DDL/seed не вводились. Предлагаемый контракт не является
разрешением выпуска или скрытым расширением OWNER-only STAGING.

Проверки baseline сейчас:5 Node test entry points PASS (classifier,store,
Driver write contract,admin authorization,moderation flow),6 Python reference
cases PASS,8 Driver/5 Admin security guards PASS. OpenAPI3.1 JSON schemas и
local refs корректны,5 операций,4 valid/8 rejected synthetic request examples.
Это проверки документа/прежнего кода, не выполнение будущих API. Новых APK,
полного backend или Android CI в docs-only шаге нет. Предыдущие CI44062029259/
44060885312 остаются evidence своих точных code SHA, не новой реализации.

Первое агрегированное чтение превысило output limit; нужные источники
перечитаны малыми частями. Поиск включил отсутствующий driverEligibilityService
путь; проверены реальные availability/assignment источники. Секреты/приватные
материалы не читались. Все будущие runtime checks перечислены отдельно.
G1paused,D4–D7pending; нет remote state audit/migration/real documents/merge/
deploy. Полный dispatcher in_progress handoff/BEV обзор не завершены.


## Решение владельца: контракт утверждён, новые права диспетчерам запрещены — 2026-11-17

Ответ владельца: «Утверждают, но права диспетчерам не давать». Контракт
Admin vehicle classification утверждён с этой поправкой: vehicle_rules.manage
и vehicles.approve_exception диспетчерам не выдавать и не признавать как
разрешение, даже если такая запись ошибочно присутствует в staff_permissions.
Эти новые полномочия доступны OWNER в действующей модели с серверным MFA.
Ранее существующие операционные права диспетчера этим ответом не отзываются.
Фиксация версии при первом сохранении автомобиля утверждена; изменения
марки/модели/года используют текущую версию, unrelated revision её не меняет.
Повторное утверждение этих двух решений не требуется. Merge/deploy/seed и
расширение OWNER-only STAGING не разрешены; G1paused,D4–D7pending.

Начинается отдельный серверный prerequisite: immutable binding истории
марки/модели/года с атомарным выбором версии в D1 trigger первого сохранения
и фактической смены данных; защита новых OWNER-only прав на сервере и в БД.
Trusted assessment/review API, итоговый admission guard и Admin UI ещё впереди.
Это не автоматический допуск или объявление полной реализации контракта.


## Утверждённый binding/OWNER-only prerequisite проверен — 2026-11-17

Backend Draft PR37 https://github.com/lobanov0710-blip/uber-v3/pull/37
реализовал миграцию0021: immutable версия при первом сохранении машины, новая
привязка только при make/model/year edit, без backfill/фиктивного класса.
Новые protected grants DISPATCHER отклоняются сервером/SQL, включая Unicode
edge trim и старые ошибочные записи. Действующие операционные права сохранены.
Поправка владельца уже принята в AGENTS/контракте, повторно её не спрашивать.

Проверенный sourcea52b83ba1da0e8024946f2d92288e6f6e52cc383, treedad21bcb855f8d49bc2446f6bbfc5a0eb9745c55,
CI44078545851/job134022783461 SUCCESS:74 npm scripts,17 новых SQLite/HTTP,
21 migrations,12 preflight,6 reference,10 native D1 workflows,6 protected CLI,
audit0,Worker dry-run. Первый failed44077774303 нашёл native D1 отличие
meta.changes: trigger writes увеличивали счётчик и могли дать ошибку после
committed vehicle insert. Исправлено чтением top-level changes() в той же
batch; native Driver contract и новая regression PASS. Первый run не считается
успешным. Local sandbox OWNER CLI ограничение не ослаблялось — проверил CI.

Receipt: backend docs/VEHICLE_RULE_BINDING_VERIFICATION_2026-11-17.json.
В Android здесь изменены только документы, security guards8 Driver/5 Admin
PASS; новая сборка/APK/native UI не выполнялись. Binding ещё не trusted
assessment/год/класс/допуск: полноценные Admin API/проверка документов/условий,
admission guards/UI, BEV обзор/legacy plan/in_progress handoff остаются впереди.
Legacy current-head candidate helper не читает binding и не должен подключаться
к trusted review/admission. Нет seed/реальных материалов/remote migration/
merge/deploy/расширения OWNER-only STAGING; G1paused,D4–D7pending.


## Решение владельца: цвет кузова обязателен — 2026-11-17

Указание владельца: «Исправить: цвет кузова на обязательно». Обязательность
внесена без повторного согласования. Отдельные fix/vehicle-color-required
ветки созданы от проверенных backend PR37/Android docs PR15. Backend POST
требует строковый color1..80 после trim, запрещает null/missing/пустое/пробелы/
нестроковое/внутренние controls. PATCH проверяет итоговую запись: пропущенный
color сохраняет имеющийся действительный цвет, legacy без цвета нужно заполнить.
Readiness анкеты требует цвет; missingColor явно указывает причину. Нулевые
старые значения разрешены только для чтения/исправления, не выдумываются.
Нет массового исправления/снятия допуска или нового правила классификации.

Driver: «Цвет (необязательно)» заменён на «Цвет кузова», required validation
с понятной ошибкой. Existing native create/edit scenario расширен: blank/
whitespace не отправляются, очистка цвета при edit блокирует PATCH; валидный
цвет явно проверяется в body. DTO продолжает читать legacy null для заполнения.
Обновлены только synthetic fixtures, связанные с новым обязательным полем.
Новых палитр/списков цветов/библиотек/ролей/миграций нет.

10 actual API cases PASS (2 новых случая цвета),17 binding regressions,
existing registration/readiness PASS локально. Security guards8 Driver/5 Admin
PASS. В existing native D1 harness добавлен requiredColor check; полный backend
и Android CI ещё предстоят. Новый APK пока не объявлен проверенным.

Ошибки инструментов: команда создания Android worktree сначала ошибочно
вызвана в backend repo и остановилась на уже существующей ветке, исправлена
на правильный repo без изменения его кода. Поиск readiness/submit сначала
использовал несуществующие имена, реальные реализации найдены через rg в
driverPrivateUploadService; один Android read выполнен в backend cwd, затем
прочитан по правильному пути. До CI исправлено неверное ожидание строки ошибки
в native test (фактическая ошибка формы «Проверьте поля...»). Assertions не
ослаблялись. G1paused,D4–D7pending; нет seed/real data/remote migration/merge/deploy.


## Обязательный цвет: Android и backend CI PASS — 2026-11-17

Android sourcee5712c969049fb6670fd64863db189f3d32eb038,
tree0dc4e4c6ebee281e9aba2709e24aa82860971b65, CI44089001617/
job134058903623 SUCCESS.327 JVM executions (Driver51×3,Admin58×3),102
native (41+61), обе apps build/lint debug/staging/release, security8/5,
endpoint/signing/AppCheck gates PASS. Existing native create/edit case
реально исполнил проверки обязательного цвета: blank/whitespace/очистка
поля не отправляют POST/PATCH; цвет присутствует в валидном body. DTO
сохраняет чтение legacy null для исправления. Первый CI run успешен.

Backend sourcea8a67fd9aaaf32359aa04370ebece424709d87df, CI44088974938/
job134058811280 SUCCESS:74 scripts,10 actual write HTTP cases (2 новых),
17 binding regressions,21 migrations,12 preflight,6 reference,10 native
D1 workflows с requiredColor,6 protected CLI,audit0,Worker dry-run PASS.
Readiness не считает машину без цвета завершённой и возвращает missingColor.

Artifacts строго от этого Android source/run: nativePNG14331817179 и
DriverAPK14331382948. Native ZIP скачан, digest6b8e47562d6c0d3ed1ed03ede9a5a1d0437748b1dbdc8576eb6a72af3593453e
совпал с GitHub, safe extraction/ZIP CRC PASS, hashes всех30 PNG совпали с CI.
Primary agent осмотрел2 vehicle-class light/dark font1.3 кадра: «Цвет кузова»
читается без optional и без обрезки целевой подписи. Это не independent review
всех30. APK только перечислен по metadata с source/digest, локально не
скачивался; его phone-upgrade signature/live Driver не проверялись. Synthetic
Firebase/debug signing не выдаются за release. Raw signed download URLs
не выводились/не записывались; использован file_id загрузчика.

Draft PR16 Driver/PR38 backend, точные counts/source/artifact hashes и границы
в REQUIRED_VEHICLE_COLOR_VERIFICATION_2026-11-17.json и backend API receipt.
Далее docs-only checkpoint не меняет verified code SHA. Требование владельца
зафиксировано в плане/PRODUCT/scope и журналах. Нет новой миграции, дефолтного
цвета, палитры, массового снятия допуска, изменения класса/pinned rule/
OWNER-only прав. Это целевое исправление поля, не полное внедрение Admin
classification. G1paused,D4–D7pending, remote migration/merge/deploy нет.
## G2: начало Android-карточки классификации — 2026-11-18

Продолжен утверждённый Admin UI этап после backend PR №43 (source `5836e183f86c656c17bf77f77bb94e968f730df5`, CI `44240856017` SUCCESS). Сверены AGENTS.md, ТЗ/план, approved classification contract/OpenAPI и актуальный backend журнал. Отдельная ветка `arch/g2-admin-classification-read` от required-color PR №16: local baseline `024a4615023af879a37d34ff0d947d9ed88e2c66`, remote `db338ea6558aca5bc2f0ab1b9e8fba30db6c3dc6`, общий tree `9cac242a7038edb04933a49800ec3fcd435773d0`; чужие изменения не заменяются. Начат самостоятельный prerequisite интерфейса — карточка чтения; форма записи/исключений и авторство правил ещё следующие части.

Добавлены строгие immutable DTO текущих/введённых параметров, server result, evidence metadata, условий, rule и history. Null/legacy данные не превращаются в проверку, historical candidate не даёт текущего класса. Новый read-only repository проверяет actual session/token/grants/MFA и scope перед transport/после ответа, не refresh/replay, optional rule read требует vehicles.read. Карточка не экспортируется, открывается только для выбранного vehicle при vehicles.verify; новых dispatcher powers нет. OWNER-phone scope блокируется до нового metadata запроса. Model basis не подменяет actual make/model. Нет client class picker/POST/preview новых материалов; прежний sensitive preview сохраняет прежние отдельные права.

Activity/root/window защищены FLAG_SECURE, no autofill/content capture/save; process recreation закрывает контекст, route extras с opaque IDs удалены после открытия. onStop отменяет запросы, очищает данные/историю/cursor/views, MFA timer стирает видимую карточку без network. На новый session/account не переносится старый ответ; store не очищается из-за поздней ошибки. История и snapshot CAS дают безопасный conflict с явным перечитыванием. Scope/оставшиеся части: `ADMIN_CLASSIFICATION_READ_UI_2026-11-18.md`.

Подготовлены 10 новых JVM и 10 native cases, 6 PNG current/history/unverified в light/dark/font1.3, отдельный evidence verifier и mandatory CI presence checks. Пока выполнены локально только 8 Driver/5 Admin security guards, XML и git diff --check: PASS. Локальный Gradle-cache не найден, /root/.gradle недоступен; права не обходились, полная JVM/native/build/lint проверка передана стандартному GitHub CI. Не объявлять тесты успешными до результата. G1paused,D4–D7pending; нет seed/backfill/remote migration/merge/deploy/реальных документов/phone scope расширения. APK ещё не проверен; новая карточка не означает готовность формы review или всего классификатора.

## Android classification read: публикация и legacy Unicode color — 2026-11-18

Подготовлен [Draft PR №17](https://github.com/lobanov0710-blip/TransferDriver/pull/17), база PR №16. Первый source `21714e044d9f2cb2fa35baba6219004ba128dc1f`, local `c39b7d25f0782b6266b93e282db73a4902868339`, общий tree `2bfe14e6d278fcb9bb9d28c191c58b70a84adbbd`. CI `44244103943` / job `134598739968` пока на JVM/build/lint этапе, итог не подтверждён. Перед публикацией найден и удалён лишний новый blank EOF журнала; diff check затем PASS. Дополнительная ручная сверка выявила отображение Unicode-only legacy color как визуально пустой строки. Подпись исправлена на «Не указано» без изменения исходного DTO/серверных данных; native fixture использует NBSP/FEFF и отдельно проверяет подпись года/цвета. Новый source требует полного повторного CI; текущий run не является его доказательством. Новых бизнес-правил/грантов/мутаций не добавлено.

## Android classification read: lint выявил API compatibility error — 2026-11-18

Source `ccd3896f479682b68bb79fa7eb7958e34097b590`, local `552490361b3417955d6d1c335967e0bc99f0bcb8`, tree `4f6071201040a51d42de660a37f6a26a98adb927` опубликован с tree parity. Первый run `44244103943` отменён обновлением ветки и не считается финальной проверкой. CI `44244638273` / job `134600646023` FAILURE на admin:lintDebug: setImportantForContentCapture и соответствующая константа требуют API30, исходная ветка допускала29. SDK api-versions.xml подтвердил API30 также для XML attribute; первоначальное предположение об XML opt-out на29 исправлено. Дополнительный View opt-out теперь только API30+, FLAG_SECURE устанавливается до загрузки данных на всех поддерживаемых API, включая29. Ни lint baseline, ни suppress/ослабление проверок не добавлены. JVM/build этап начал выполняться, native/artifact/evidence до failure не запускались; полный PASS не объявляется, CI повторяется после исправления.

## Android classification read: Driver window focus в native run — 2026-11-18

Source `b7eb21f4a824ca3da51e7a0ed05a9158b3721739`, local `d5d89a6e94d2967d53065ac25b3201a26a178d83`, tree `0ca7f3fcf5589ee6b672f23d912bebbcccc75458`, parity подтверждена. CI `44245721289` / job `134604070012` FAILURE в Driver native до запуска Admin. Build/JVM/lint трёх вариантов, endpoint/signing/App Check checks прошли. Три Driver cases: nativeAvailabilityLight/DarkLargeFont не дождались window focus/SystemUI appearance, realSliderGesture получил RootViewWithoutFocusException с has-window-focus=false. Driver runtime/tests не менялись этим этапом. Проверен исходный await/capture и XML-отчёт: assertions не пропускаются, crash продукта в логе не обнаружен. Это свидетельство недоступного фокуса, а не доказанный новый дефект приложения. Планируется один повтор того же source на свежем emulator job; при повторении нужны отдельная диагностика окон/эмулятора, а не увеличение таймаутов/ослабление assertions. Admin native и артефакты этим run не подтверждены.

GitHub Actions rerun_workflow_job принят: тот же run `44245721289`, attempt2, новый job `134711676280`, source SHA не менялся. Полный workflow повторяется на свежем runner/emulator, тесты не исключены. Рабочая запись журнала до завершения CI локальная; runtime tree проверяемого source остаётся `0ca7f3fcf5589ee6b672f23d912bebbcccc75458`.

## G2: Android-карточка классификации проверена — 2026-11-18

Source `b7eb21f4a824ca3da51e7a0ed05a9158b3721739`, local `d5d89a6e94d2967d53065ac25b3201a26a178d83`, tree `0ca7f3fcf5589ee6b672f23d912bebbcccc75458`; CI `44245721289` attempt2 / job `134711676280` SUCCESS. 357 JVM executions (Driver51×3/Admin68×3), 112 native (Driver41/Admin71), все без failure/error/skip; обе apps build/lint debug/staging/release, security8/5, endpoint/signing isolation и App Check policy PASS. 20 успешных workflow steps; пропущен только диагностический Print instrumentation report on failure, а не тесты. Native подтвердил current/history разделение, legacy null/Unicode-only color, privilege/MFA/phone scope, cancel/session binding, stale revision, foreground wiping, MFA timer и реальные кадры. Ранее Driver focus failure не повторился на свежем emulator с тем же code SHA, assertions/timeouts не менялись.

Скачаны Admin debug APK archive и native evidence archive; оба digest/byte count совпали с GitHub и привязаны к проверенному SHA. 36 PNG 1080×2400 сверены с CI hashes/PNG verifier; 6 новых current/history/unverified в light/dark/font1.3 осмотрены напрямую: читаемые подписи/контраст/scroll, кандидат явно требует отдельного approved, отсутствие текущего proof не подменяется historical candidate. APK `ru.transferservis.admin` debuggable и подпись проверены apksigner/aapt2; signing/package isolation с Driver подтверждена CI. Driver APK metadata сохранена, отдельно не скачивалась: новых Driver runtime изменений нет. В репозитории только SHA/digest/provenance, без временных download URLs/credentials; real sensitive data не использовались. Exact hashes/артефакты/предыдущие сбои/границы — `ADMIN_CLASSIFICATION_READ_UI_VERIFICATION_2026-11-18.json`.

Draft PR №17 остаётся отдельным зависимым от №16; последующие docs-only commits не заменяют verified code SHA. Проверена карточка чтения, а не вся форма классификации. Далее parameter/conditions review и OWNER exception form с confirmation/явным same-key retry/foreground privacy, затем OWNER rule authoring UI. Full powertrain121/legacy inventory/live acceptance открыты. G1paused,D4–D7pending, нет новых dispatcher powers, phone scope расширения, real Firebase/device acceptance, seed/remote migration/merge/deploy. Debug APK в CI — с ephemeral подписью и synthetic config, не production release. Канонические журналы синхронизируются отдельными append-only записями.

### Подтверждение публикации UI card и синхронизации — 2026-11-18

Receipt/scope/план опубликованы docs-only commit `632d6ba200aa68d6c9fe0d3265040a753d6c75ea`, tree `3de2a5a71ba458f0916c22384dd6dbb31a8b2597`; удалённый receipt побайтно совпал с локальным. PR №17 обновлён итоговым поведением, проверенным code SHA/attempt2, provenance и открытой формой review; остаётся draft, не merged. Канонический TransferDriver `arch/spec-v1-security-baseline` — `4b9e445eea64c4e4c4c9d2033f30d967080ec429`, backend `arch/spec-v1-backend-phase1` — `6260f6aa16e03e72c94e889f162e72c715e63ea5`; чтение обоих refs подтвердило эти SHA. Каждый канонический commit меняет только PROJECT_JOURNAL.md: 11 добавленных строк, 0 удалённых, AGENTS/runtime не меняются. Ref updates с expected head без force. Локальный bytecode cache PNG verifier удалён; пользовательские данные/общие рабочие папки не затронуты. Этот checkpoint — только журнал, verified source `b7eb21f4a824ca3da51e7a0ed05a9158b3721739` и его тесты не подменяются docs SHA.

## G2: Android форма проверки параметров/условий и OWNER exception — 2026-11-18

Сверены AGENTS, ТЗ/план/журнал и утверждённый backend classification контракт/OpenAPI. Продолжен следующий этап PR17: отдельная arch/g2-admin-classification-review-form от local f8fc31a66e4c56a575c512dc804aadfbeaea5d52 / remote f087ca3f7ad8911e61b701299f6e82a6b7719636, общий tree575c63a48ef2c694ddae0341d253ad4c4496c0cf. Новых бизнес-решений не принято. Scope/ограничения: ADMIN_CLASSIFICATION_REVIEW_UI_2026-11-18.md.

Форма требует реальные параметры и две verified стороны СТС, явные conditions/OWNER waivers/reason/confirmation. Server выбирает класс; target pin сохраняется до key correction, basis отдельный. Новый OWNER-only grant не выдаётся dispatcher; actual access/session/MFA обновляются перед действием. Неизменяемый action/key, явный исходный retry без автоматического POST/refresh/GET; отдельное чтение состояния не rebases неизвестную запись. Wipe onStop/MFA timer, FLAG_SECURE, no persistence/autofill/saved state, scoped preview с прежними sensitive правами. Результат не ставит approved/online; ручной dispatcher handoff сохраняется.

При локальной сверке исправлены ошибочный route prefix driver (существующие driver IDs имеют staff_), пропущенная кнопка входа из старого Button-layout и повторное использование basis conditions при target pin=null. До публикации добавлены целевые JVM/native tests и обязательные CI case/PNG gates; прежние проверки не исключаются. Локально security8/5/XML/diff PASS; Gradle cache недоступен, не обходился. Полный CI ещё не выполнен, APK не подтверждён. G1paused,D4–D7pending; нет merge/deploy/remote migration/реальных данных/phone scope расширения.

### PR18: публикация и исправление capture helper — 2026-11-18

Draft PR18 создан, source4b1591719e204b26b5614011354112fc077c5a66/local2c208c3, tree9b884af6e602f5a3e2dff624331edcc0af5905f9 с parity. CI44304281095/job134801679536 начат; итог не подтверждён. Дополнительная сверка capture helper выявила: dialog имеет самостоятельный FLAG_SECURE, который тестовый helper ещё не снимал, поэтому PNG подтверждения мог быть скрыт. Исправлен только native helper: assert обоих secure flags, временное снятие только в synthetic тесте и восстановление finally для каждого окна. Production FLAG_SECURE не меняется. Следующий source заново проходит весь workflow; superseded run не будет его доказательством.

### PR18: native evidence выявил нестабильность формы — 2026-11-18

Source dc2b59907c8108ebc2b5018edf73f8ab5c974039 / local6f838abeeb7e637732058fc9935dbd437fecc1e8, tree def1b9685c1e63b537609df725221aa690ca7ce4. CI44304512138/job134802631038 FAILURE: Driver41/41 и Admin85/87 native PASS, 2 failures только nativeAssessmentLight/DarkLargeFont: Native framebuffer did not settle на первом кадре формы. Все 8 новых repository и 6 behavioural UI cases PASS (access/MFA/scope, cancellation, POST/retry, correction/exception, wipe). Build/JVM/lint3 variants и security/endpoint/signing/AppCheck до native прошли; full PASS и новые PNG artifacts этим run не подтверждены. Первый source/run44304281095 отменён обновлением ветки.

Сверка editable формы/capture helper: первое поле может сохранять фокус, мигающий cursor/IME не даёт статическому кадру выдержать 1 секунду. Это рабочая гипотеза причины двух capture failures, не доказанный backend дефект. Native helper теперь явно завершает ввод: переводит фокус на root, скрывает IME обычным Android API и проверяет отсутствие EditText focus перед raw screenshot. Сообщение failure включает имя кадра для диагностики. Проверка bitmap.sameAs/1s stability/8s budget, security assertions и production код не ослаблены. Следующий source должен заново пройти полный workflow и визуальную проверку; при повторном сбое нужна дополнительная диагностика, не обход gate.

### PR18: подпись неизвестной модели не обещает обход hard restrictions — 2026-11-18

Во время CI44307399886/source381522e3befda5c37ed900feac3db9ec3d62deb2 повторные build/JVM/lint/security/endpoint/signing/AppCheck прошли, native ещё выполняется. Ручная сверка выявила неточный текст assessment_no_rule: неизвестная модель описывалась как безусловный manual review, хотя BEV/body hard restrictions могут дать excluded. Подпись исправлена на отсутствие подтверждённого класса и окончательное решение сервера с обязательными ограничениями. Runtime/API/классификатор не менялись; никаких обещаний допуска, новых правил или client-side class нет. Workflow на предыдущем source будет superseded; новый resource source требует полного повторного CI. Не считать native/evidence успешными до его завершения.

### PR18: повторный Driver window-focus failure и безопасная диагностика — 2026-11-18

Source818bee8158618fe80716b4271e5304972a9f1ef0/local e7275ce19d266dbb204890a8f482bb83752e323a, tree932f2cb0dc2dd387eda7e2f643a52d102500f0f4; CI44308654388/job134816762534 FAILURE в Driver native до Admin. Driver38/41 PASS; nativeAvailabilityLight/DarkLargeFont не дождались window focus/SystemUI appearance, realSliderGestureUsesChosenVehicleAndOnlineHidesSwitch получил RootViewWithoutFocusException. Root visible1080x2400, has-window-focus=false, NOT_FOCUSABLE flag отсутствует. Это повтор ранее зарегистрированного intermittent baseline focus failure, не доказанный дефект Admin. Admin/evidence не запускались, не объявлены успешными. Build/JVM/lint/security/endpoint/signing/AppCheck прошли. Промежуточный CI44307399886 отменён resource исправлением.

Существующих логов недостаточно для определения окна, удерживающего фокус; blind rerun не выполнен. В synthetic Driver fixture добавлен failure-only filtered dump mCurrentFocus/mFocusedApp/lifecycle/power, без пользовательских полей/provider/SQL/logcat. Gesture/assertion/focus/color/timeouts сохранены, production Driver код не меняется. Admin capture failure сохраняет последний исходный native кадр с именем stage для диагностики, без редактирования изображения.

Native runner теперь выполняет независимые Driver и Admin suites последовательно даже при failure одного из них и в конце обязательно возвращает failure, если любая suite/PNG/adb проверка не прошла. Раздельные bash -e процессы сохраняют errexit; .validated только после обеих успешных suites и всех46 PNG. 5 новых control-flow tests проверили success, Driver/Admin failure, adb failure и PNG verifier failure: PASS. Данные диагностики загружаются отдельным failure artifact, не как успешное evidence. XML failure report расширен до1000 строк, чтобы не обрезать безопасную диагностику. Локальные security8/5, bash syntax/diff и runner5 PASS. Следующий source требует полного CI; root cause intermittent focus пока открыт, реальный дефект не объявляется устранённым без evidence.

Локальная adversarial проверка нового runner до native CI выявила ещё один риск повторного вызова: старые .validated/.driver-validated могли сохраниться от предыдущего успеха. Runner теперь удаляет только эти два собственных generated markers при старте и использует set -eu; все5 control-flow tests начинают со stale markers и проверяют их отсутствие при любом failure. PASS. Это не удаление отчётов/исходников и не ослабление проверки. Source7fd3db457001c75f75c6a86737ea14e95124f8e0 заменяется исправленным source до окончательного CI; финальным доказательством не считается.

## G2: Android review/OWNER exception form проверена — 2026-11-18

Source777822f597c8469e0148bb3978479e09ff5d453b/local7eace64fbf2ce33d935c1156bce5de0ad4953036, общий tree9979e97e0865a56932d75e4a6dc3a06c5bcb84e5. CI44311619540/job134827092493 SUCCESS:390 JVM (Driver51×3/Admin79×3),128 native (Driver41/Admin87), без failure/error/skip. Обе apps build/lint debug/staging/release, endpoint/signing isolation/AppCheck, security8/5 и runner5 PASS. Пропущены только failure-only diagnostic steps. Все прежние обязательные сценарии сохранены; независимые suites обе выполнены, validation marker создан после всех проверок.

Скачаны source-bound Admin APK archive14432436516 и PNG archive14432197106, digest/byte count совпали с GitHub;46 PNG1080×2400 сверены с CI SHA и PNG verifier.10 новых raw кадров напрямую осмотрены: light/dark/font1.3 form, exact rule/conditions/STS, confirmation, unknown outcome и explicit retry, recorded/manual dispatcher notice; читаемые controls/contrast/wrapping/scroll. Без синтеза/редактирования PNG. Admin package ru.transferservis.admin, debug certificate и APK signature проверены apksigner/aapt2. Driver artifact14431843210 сохранён только как metadata, отдельно не скачан: production Driver runtime не менялся. Временные signed URLs/credentials не выводились/не сохранялись. Полные SHA/artifacts/прошлые runs и границы в ADMIN_CLASSIFICATION_REVIEW_UI_VERIFICATION_2026-11-18.json.

Конкретные local findings устранены: route staff_ prefix, входная Button, target=null conditions, secure dialog capture, завершение text input перед кадром, подпись unknown model с hard restrictions, stale generated validation markers.10 новых capture frames теперь стабильны; точный animated region прежних failure PNG не был сохранён, первопричина cursor/IME остаётся обоснованной гипотезой. Intermittent baseline Driver focus не повторился в финальном run; точный focus owner старых сбоев неизвестен, добавлена failure-only фильтрованная диагностика. Не объявляется устранённым production Driver дефект или закрытой проблема CI focus. Driver gesture/focus/colors/timeouts и security flags не ослаблены.

Draft PR18 остаётся отдельной зависимой от readonly PR17. Далее OWNER rule authoring UI; full powertrain121/legacy inventory/live phone acceptance открыты. APK synthetic/ephemeral debug, не release и не доказательство совместимости подписи с телефоном. G1paused,D4–D7pending; нет phone scope расширения, реальных документов, seed/backfill/remote migration/merge/deploy. Receipt/plan/scope обновлены docs-only после проверенного source; канонические журналы синхронизируются отдельными append-only записями.

### PR18: подтверждение публикации и синхронизации журналов — 2026-11-18

Receipt/plan/scope/next-step опубликованы docs-only commit d5d14005426fb472dc90b670bf8980b3806f24f9, общий tree2240a4ddc3a4acc2881b536c3f22c50cea1877b7; источник CI777822f597c8469e0148bb3978479e09ff5d453b не подменён документационным SHA. Итоговое описание PR18 содержит390 JVM/128 native/46 PNG, ограничения APK/phone и открытый intermittent focus; PR остаётся draft, не merged.

Канонические журналы: TransferDriver arch/spec-v1-security-baseline — 0f34d7728fa5b4b755115dbda8c5d62688cb2615; backend arch/spec-v1-backend-phase1 — 5e2515c279e69809d736a4714ddc773220304121. Проверка commit API перед ref update: в каждом ровно PROJECT_JOURNAL.md,13 добавленных строк,0 удалённых, без AGENTS/runtime. Updates с expected head и force=false; отдельный journal checkpoint не является merge/deploy или новым проверенным code source. После публикации выполняется контроль refs/receipt bytes и чистоты отдельной рабочей ветки.

## G2: начало OWNER редактора правил автомобиля — 2026-11-18

Продолжен следующий этап утверждённого classification контракта после проверенного PR18/source777822f597c8469e0148bb3978479e09ff5d453b/CI44311619540. Сверены AGENTS, ТЗ28–30, план/журнал и backend approved contract/OpenAPI/actual RuleWrite service. Новая отдельная arch/g2-owner-vehicle-rule-editor от localfa2cfd916fb798d2611112095ebf2c0ca3b86e34 / remoteb6d143c945e05cf3b4bf80c45de59f781b654d12, общий tree38084fdfa9c31029e22dc43537c1fd2d046becdb. Новых бизнес-правил/ролей/прав не принято. Scope: OWNER_VEHICLE_RULE_EDITOR_2026-11-18.md.

Реализованы exact make/model lookup, immutable snapshot current/null, OWNER actual MFA guard, явные year/class/body/nullable mini-van policy/whole condition rows/reason, confirmation и new immutable rule version POST. Класс правила — настройка общего справочника OWNER, не ручной класс автомобиля. Dispatcher с ошибочным новым grant и phone scope блокируются. Existing pins не пересчитываются, hard BEV/body остаются на сервере. ACK strict values/version/revision/condition IDs. Unknown outcome сохраняет только original body/key в foreground; separate explicit read не rebases, repeat только explicit confirmation/fresh auth. No refresh/auto POST/GET. Privacy/cancel/onStop/MFA wipe; store чужой новой сессии не очищается.

Подготовлены11 JVM,15 native (7 repository/8 UI),10 raw frame cases и обязательные CI case/PNG gates; прежние128 native/46 PNG не исключены. Local security8/5, native runner5, XML/bash/diff PASS. При pre-publish сверке исправлен тест ограничения16KiB: каждый condition теперь ровно1000 chars, чтобы проверять общий payload limit, а не случайный per-condition length failure. Revision MAX закрыт до подготовки записи. Полные JVM/build/lint/native/artefact checks пока не выполнены; отправляются в CI, локальный недоступный Gradle cache не обходился. G1paused,D4–D7pending; no merge/deploy/seed/remote migration/phone scope expansion/live data. Intermittent Driver focus root cause по-прежнему открыт, диагностика и все assertions/timeouts сохранены.

### OWNER rule editor: первый полный CI PASS и уточнение ошибки записи — 2026-11-18

Source4bea0c17823958a549163c46341306e7edb71a45/local85dd1f7, общий tree143e6da9d096d8f79146149a3ad7bf1818e8bdeb. CI44318363459/job134850400369 SUCCESS:423 JVM (Driver51×3/Admin90×3),143 native (Driver41/Admin102), без failure/error/skip; обе apps build/lint3 variants, security8/5/runner5, endpoint/signing/AppCheck и56 mandatory PNG PASS. Intermittent Driver focus не повторился; причина старого сбоя не объявляется устранённой. APK/PNG archives этого промежуточного source отдельно не скачивались.

Дополнительная ручная сверка выявила misleading status для definitive write4xx: общий текст «не удалось прочитать» описывал отказ записи как read failure. Исправлен на безопасное «сервер не принял новую версию; проверьте утверждённую марку/параметры/условия и выполните поиск заново». Raw provider text/code не выводится; conflict409 и unknown IO/408/5xx остаются раздельными. В существующий native case добавлены400 rejection/no retry/no commit и явный owner_phone denial до нового rule GET. Это не изменение бизнес-правил, permissions/API или body/key. Local Admin security5/XML/diff PASS. Новый UI source требует полного повторного CI и provenance; первый зелёный run не выдаётся за его проверку.

## G2: OWNER редактор правил автомобиля проверен — 2026-11-18

Source739059daa09341fd3239644b1ab26c7f8061b1b8/localdf542224809859c2d43e9b0b22a125d7aa763f53, общий treec5a30e7188a4225478e6d1129d886b344fae3b9d. CI44321910346/job134862796335 SUCCESS:423 JVM (Driver51×3/Admin90×3),143 native (Driver41/Admin102), без failure/error/skip. Обе apps build/lint debug/staging/release, security8/5, runner5, API endpoint/signing isolation/AppCheck PASS. Пропущены только failure-only diagnostic steps. Первый source4bea/CI44318363459 тоже прошёл полный workflow; итоговый source дополнительно проверяет definitive400 rejection/no commit/no retry и phone scope до rule GET. В этом этапе failed/cancelled CI нет; это не стирает исторические intermittent Driver focus failures.

Реализованы exact current/null lookup перед записью, обязательные explicit rule inputs/whole conditions/reason, OWNER actual MFA/phone/Dispatcher guards, immutable expectedCurrentVersion/body/key, confirmation и explicit same-key retry после fresh auth. Current read после unknown не rebases action. New immutable rule не меняет существующие привязки/одобрение/линию автомобилей; класс конкретного автомобиля выбирает сервер. ACK strict values/revision/version/condition IDs. Wipe onStop/MFA, no SavedState/autofill/cache, FLAG_SECURE и scoped cancellation/поздняя смена session. Backend/API/grants/dependencies не изменялись.

Скачаны source-bound Admin APK archive14435749011 и PNG archive14435374402; digest/byte count совпали с GitHub.56 PNG1080×2400 hashes сверены с CI и всеми6 PNG verifier,10 новых raw native frames light/dark/font1.3 напрямую осмотрены: form/whole conditions/body choices/confirmation/unknown result/recorded version. Контраст/переносы/scroll и управляющие кнопки читаемы, изображения не редактировались и не синтезировались. APK ru.transferservis.admin, подпись/debug certificate проверены apksigner/aapt2. Driver artifact14435549451 только metadata, отдельно не скачан. При первом открытии локальных кадров исправлено имя extraction-каталога verification script (assessment-review→rule-editor); PNG/APK bytes не менялись, digest/PNG/signature затем проверены по исправленному пути. Raw signed URLs/credentials не выводились/не сохранялись. Exact source hashes/artifacts/сертификат/границы — OWNER_VEHICLE_RULE_EDITOR_VERIFICATION_2026-11-18.json.

Local finding: подпись definitive rejected write исправлена с «ошибка чтения» на безопасный отказ новой версии, отдельный native400/phone assertion PASS. Все прежние обязательные128 native/46 PNG сохранены. Intermittent baseline Driver focus не повторился, но его root cause остаётся открытым; existing diagnostics/gesture/focus/colors/timeouts не ослаблены. Synthetic/ephemeral debug APK не production release и не proof совместимости обновления телефона; live/phone acceptance не выполнялась.

Draft PR19 зависим от PR18. Readonly PR17 + review/exception PR18 + OWNER rule editor PR19 реализуют согласованные Android UI части в отдельных draft ветках. Full powertrain121/legacy inventory/live acceptance ещё открыты; G1paused,D4–D7pending. Нет merge/deploy/remote migration/seed/backfill/phone allowlist расширения/реальных документов/новых бизнес-решений. Receipt/plan/scope/next-step публикуются docs-only после verified code; канонические журналы синхронизируются append-only отдельно.

### OWNER rule editor: подтверждение публикации и синхронизации — 2026-11-18

Receipt/plan/scope/next-step опубликованы docs-only commit686b6476895372d7bce1ca6d1838e846f8361fae, общий tree2db1cba7ab48cba477bd3ca7e2d1f534a1259533. Проверенный source739059daa09341fd3239644b1ab26c7f8061b1b8/CI44321910346 не подменён docs SHA. Итоговый PR19 содержит423 JVM/143 native/56 PNG и ограничения live/phone/ephemeral signing; draft/open, не merged.

Канонические журналы: TransferDriver arch/spec-v1-security-baseline — 6bc0369766c85d92f98e318305408174a9df50bc; backend arch/spec-v1-backend-phase1 — 883aade2d6afb695e63a7c895ccdf8e14b048308. Каждый commit проверен через API до ref update: ровно PROJECT_JOURNAL.md,15 added/0 deleted, без AGENTS/runtime; expected head + force=false. После публикации сверяются refs/receipt bytes/неизменность runtime после CI и чистота отдельной ветки. Это журналирование, не merge/deploy/активация правил.


## G2: readiness audit и локальная инвентаризация — 2026-11-18

По команде владельца «Далее» сверены AGENTS, утверждённый контракт, план и журналы.
Продолжена следующая часть после OWNER editor PR19. Новая отдельная ветка
`arch/g2-classifier-readiness-audit`; backend parent PR43 head
`84bcb31655b9a952816a226c1807d5a243e20bcb`, Android docs parent PR19 head
`eaea54a7bfb64439eec08bb60d55cb2188341f5d`. Heads сверены read-only через GitHub.

CSV121 SHA256 `553384af5b49252d3b3acbc3973ef6dae7fc40d348156debf4f0fab24c5ae880`
сохранён: 76/22/23, годы/поля/порядок без изменений, архив116 не изменён.
Новый реестр учитывает121 правило: 9 mixed families с частичными первичными
свидетельствами,112 pending; полного покрытия всех поколений/лет/рынков нет ни
у одной строки. Уточнён источник SpaceTourer: бензин в статье относится к
Berlingo. S9 получает первичный HIMA source; Avatr12 имеет hybrid trim, но кузов
и фактический автомобиль не объявлены проверенными. BEV-удаления/L7-L9 сохранены.

Подготовлен offline Python aggregate inventory: существующий закрытый snapshot,
mode=ro+immutable/query_only, одна read transaction, fixed SQL/deny writes и
functions/ATTACH, reject WAL/sidecars, безопасный отказ без raw error/path/PII,
18 счётчиков, всегда admissionEvaluated=false/activationAuthorized=false.
Только синтетические файлы; реальная legacy inventory НЕ выполнена. Данные
snapshot не запрашивались/не загружались. Runtime/API/schema/Android не менялись.

Локально11 новых tests,24 migrations,12 preflight,6 reference PASS. Новые tests
добавлены в штатный CI. При подготовке исправлены ошибки синтетической фикстуры:
число owner-binding columns, длина VIN, уникальность document object key; первые
запуски были FAIL, после исправления11 PASS. Локальный общий npm остановился
на существующем ownerMfaControlPlaneCli: spawnSync EPERM/пустые streams в этой
среде; это не PASS и не доказательство дефекта runtime. Полный Node22/native
D1/CLI/dependency audit/Worker dry-run ожидается в GitHub CI, ограничения не обходились.

Подробности: VEHICLE_CLASSIFIER_READINESS_2026-11-18.md и machine-readable
VEHICLE_POWERTRAIN_AUDIT_2026-11-18.json в backend. До активации нужны полный
powertrain обзор, реальная защищённая инвентаризация и рассмотрение плана пробелов.
Нет seed/backfill/remote migration/merge/deploy/нового APK/расширения OWNER-phone
или dispatcher powers. G1paused,D4–D7pending. Android только docs, прежняя
проверка PR19 сохраняется отдельно. CI/публикация фиксируются следующим checkpoint.


## G2 readiness: совместимость inventory до миграций — 2026-11-18

Первый source `8fbc3c3e4930c3efe11cc3e37a34a5b8799f8c04`, tree
`7ee1245a91be11f0b6dcb6d750dffd05bda04024`, draft backend PR44: CI
`44333828910` / job `134904994763` SUCCESS (все30 steps):79 npm commands,
11 новых inventory tests,24 migrations,12 preflight,6 reference,15 native D1
workflows,6 actual Wrangler CLI scenarios, audit0, Worker dry-run PASS.
Raw logs проверены; real Cloudflare requests0 в CLI/новых classification scenarios.
Android docs-only draft PR20 опубликован отдельно от проверенной сборки PR19.

При сверке порядка внедрения найдено ограничение первого inventory: требовал
новую схему0024, хотя legacy нужно оценивать до миграций. Исправлено: поддержка
базовой Driver schema и последовательно отсутствующих слоёв классификатора,
явные schemaCapabilities; частично сломанный уже существующий слой отклоняется.
Год/доказательства/кандидаты не выдумываются. Ноль записанных BEV/holds при
отсутствующем слое не является подтверждением отсутствия проблем. Пустой цвет
включает Unicode whitespace по правилам trim. Добавлены реальные синтетические
pre-classifier, broken trust и whitespace cases:14 tests PASS локально.

Первая CI проверка11 tests сохранена как успешная предыдущая версия. Изменённый
инструмент проходит новый полный CI; старый PASS ему не приписывается. Пределы
121 audit/реальной inventory/активации и запреты публикации чувствительных данных,
G1paused,D4–D7pending сохраняются. Ошибки patch context при обновлении отчёта не
изменили файлов; контекст перечитан, документ обновлён. Бизнес-правила не менялись.


## G2 readiness: финальная проверка исправленного inventory — 2026-11-18

Source `a3dc5bd299cd063bbeee4f1195ac5d6e656faabb`, tree `cac28aa06c0688af58efb228869ff99add848e7d`,
local `bf550cd5ff47d56ab825070149fed8169fbe48a3`: tree parity подтверждена. CI
`44334924707` / job `134908905914` SUCCESS, все31 steps:
14 inventory tests,79 npm-команд,24 migrations,12 preflight,6 reference,
15 native workerd/D1 workflows,6 protected CLI scenarios с actual Wrangler4.125.0,
dependency audit0 и Worker bundle dry-run PASS. Raw decoded job log и точный
head проверены. Первая CI44333828910 с11 inventory tests также SUCCESS;
локальный EPERM и исправленные ошибки фикстур не скрыты предыдущими записями.
Уточнение счётчика шагов: GitHub добавляет финальный Complete job; прежние30
в предварительной записи были без него. Финальный API возвращает31 SUCCESS.

Новая проверка подтверждает чтение pre-classifier snapshot без миграций,
явные schemaCapabilities, безопасный отказ для повреждённого слоя/WAL/sidecars,
отсутствие raw PII/errors, запрет записи/ATTACH/extension, expiry без invalidation,
immutable hold/offline, отсутствие выдуманного года/pin/trust. Runtime/schema/
зависимости backend не менялись; после CI публикуются только docs/receipt.
JSON receipt: VEHICLE_CLASSIFIER_READINESS_VERIFICATION_2026-11-18.json.

Draft backend PR44 и Android docs PR20 остаются открытыми; не merged.
CSV121 и архив116 сохранены. 9 семейств имеют частичные первичные свидетельства,
112 pending в реестре; это не исчерпывающий обзор годов/поколений/рынков.
Реальная inventory/обработка legacy/активация/live acceptance остаются открытыми.
Нет real sensitive data, remote migration, seed/backfill, deployment, нового APK,
расширения OWNER-phone или dispatcher powers. Android docs-only; отдельный
проверенный PR19 baseline сохраняется. G1paused,D4–D7pending.
Следующий шаг — продолжить powertrain evidence и подготовку защищённой реальной
инвентаризации в рамках отдельно утверждённого доступа; новые допуски не назначены.


## G2: первый powertrain проход всех 121 семейств — 2026-11-18

По команде «Далее» сверены AGENTS, актуальный план, журнал и remote feature/
canonical heads. Продолжены существующие отдельные draft ветки PR44/PR20.
G1paused,D4–D7pending; новые производственные действия не разрешались.

К 9 исходным первичным источникам добавлены102: всего111. Для каждой121 строки
найдено хотя бы частичное первичное свидетельство ДВС/гибридного исполнения;
строк без семейного источника0, исчерпывающе проверенных диапазонов лет/рынков/
поколений0. Это первый проход семейств, не снятие activation gate и не проверка
фактических авто. Revision2 ledger сохраняет все поля121 CSV и archive116/hash.

Lexus ES имеет BEV/HEV в первичной публикации; существующий hard BEV guard и
каталог не менялись. BMW i5/i7, EQV/eVito, ID.Buzz и иные соседние названия не
превращены в alias. SAIC G10 MY2025 даёт точное имя и petrol/diesel; LDV G10+
cargo/вторичные продавцы отклонены как доказательство passenger Maxus G10.
Точная таблица Opel Italy ZAFIRA LIFE diesel найдена в первичном индексе;
прямой fetch PDF timeout, полного чтения не заявлено. Публикации, indexed
excerpts и доступные PDF passages различены evidenceAccess. Некоторые ранние
источники лежат вне minYear, это явно ограничено scope; годы не выдумываются.

Подготовка поиска выявила пустые выдачи по неверным/узким доменам,404,403 и
redirect; использованы доступные первичные источники/индекс с явными пределами,
не обход авторизации. В локальном черновике исправлено разбиение FAW / Bestune
B70; Bestune/T90 отдельно как утверждено, actual CSV не менялся. Raw статьи/
руководства не копируются в Git; сохраняются короткие пересказы/URL/дата/пределы.

Найден вопрос: официальные UNI-V/B70 названы лифтбеками при разрешённых
sedan/crossover в Комфорте. Утверждённого тождества в repo не найдено.
Владельцу сразу отправлен вопрос: разрешить отдельный liftback только Комфорт
или сохранить ограничения. Ответ пока ожидается; вариант не выбран, время не
считается согласием. Зависимые enum/schema/API/body-rule changes не начаты.
См. VEHICLE_BODY_POLICY_CLARIFICATION_2026-11-18.md.

Изменения только docs. Новый полный Android/backend CI не требуется для этих
текстов; проверенный runtime source a3dc5bd299cd063bbeee4f1195ac5d6e656faabb /
CI44334924707 остаётся отдельным baseline, не объявлен тестом новых ссылок.
Проверки целостности данных/границ и публикация фиксируются следующим checkpoint.
Нет real vehicle data, remote inventory, seed/backfill/migration/merge/deploy,
нового APK/активации/расширения OWNER-phone или dispatcher powers.


## G2: проверки revision 2 powertrain evidence — 2026-11-18

PASS:121 исходных rule rows/порядок/поля совпадают с актуальным CSV; архив116 и
оба фиксированных SHA256 сохранены.111 уникальных source IDs и HTTPS URL без
embedded credentials; все row references разрешаются, пары brand/model точные,
9 прежних source records сохранены.121 partial,0 exhaustive,activation blocked,
actual legacy inventory false. Ledger SHA256
`8df887adebca225eb9b6c3fc0b08351f384ad2623849ba11307538d0257883ff`.
6 existing vehicle reference tests PASS. Runtime tool/test/workflow SHA256
сверены с receipt source a3dc5bd299cd063bbeee4f1195ac5d6e656faabb — без изменений;
в diff только docs. diff --check PASS в обоих worktrees. Новые Android/backend
CI/APK не запускались и не объявляются проверкой этих документов. Исторический
receipt остаётся привязанным к прежнему source, новый ledger проверен отдельно.
Вопрос лифтбеков pending; никакое решение за владельца не принято. Публикация
в PR44/PR20 и append-only канонические журналы выполняется после этих проверок.


## G2: опубликован первый проход 121 семейства — 2026-11-18

Backend docs source `0fd9cd414fbedb02ac43c3a59ee906664e0cd73a`, tree `81197f2d8f63c026af10d73d265fdfb9b910e61b`,
local `35e736a8de39cc828735ff6e0c8b4d543235e165`; Android docs source `7267d682caaebc96253ae0e2b9e63dc876443eae`,
tree `6400a0f667d50c440f2c925f826f8a092d8fd600`. Локальные/remote trees совпали при публикации;
PR44/PR20 остаются draft, remote branch обновлялись с expected-head без force.
Все121 строки имеют частичное первичное свидетельство;111 источников,
0 исчерпывающих диапазонов. EvidenceAccess/пределы индекса/timeout сохранены.
Verification receipt: VEHICLE_POWERTRAIN_EVIDENCE_VERIFICATION_2026-11-18.json.
6 reference PASS и целостность ledger/hash/ссылок/исходных строк PASS. Runtime
hashes прежнего CI44334924707 source a3dc5bd299cd063bbeee4f1195ac5d6e656faabb
не изменились. Этот этап docs-only, нового полного CI/Android APK нет.
Вопрос лифтбеков ожидает решения владельца; body enum/guards и business/minivan
не менялись. Исчерпывающий обзор/actual legacy inventory/активация не закрыты.
G1paused,D4–D7pending; нет seed/backfill/remote migration/merge/deploy/real PII.
Канонические журналы синхронизируются отдельным append-only checkpoint.


## G2: OWNER утвердил лифтбек только в Комфорте — 2026-11-18

В ответ на предложенный выбор владелец сказал «Только в комфорте». Решение
явное, повторного утверждения не требуется: отдельный liftback разрешён только
для comfort, включая запрет business/minivan/OWNER exception bypass. Сохранены
Li Auto L7/L9 crossover exceptions, BEV запрет, pin/history, проверка года/
документов, class вычисляет backend; авто не получает допуск автоматически.
Обновлены AGENTS и решение. Новые отдельные worktrees/ветки
arch/g2-comfort-liftback созданы поверх проверенных PR44/PR20. G1paused,D4–D7pending.

Перед реализацией исследована SQLite миграция с FK ON: простая замена parent
таблицы с RESTRICT children завершилась FK failure только в in-memory probe;
данные проекта не затронуты. Выбран явный атомарный rebuild трёх связанных
assessment/invalidation/hold таблиц с сохранением идентификаторов, всех значений,
rowid, FK и NOT NULL/CHECK; временного кодирования liftback как other не будет.
Populated upgrade/rollback и запрет записи/изменения истории обязательны в tests.
Backend/Admin проверки и полные CI ещё не выполнены для нового изменения.
Remote migration/seed/backfill/merge/deploy/реальные данные не выполняются.


## G2: подготовлена реализация liftback только для comfort — 2026-11-18

Backend enum/API/decision/SQL guard и миграция0025, Admin DTO/body selector/
readonly label/OWNER checkbox/confirmation обновлены в новых отдельных ветках.
minivan null policy не обходит liftback restriction. Старые definitions/pins и
неявный fallback2 сохранены, SQL требует явного liftback в definition. Driver
runtime не меняется. Body metadata ledger теперь отражает утверждение OWNER,
но ranges/actual inventory/activation остаются открытыми.

Локально2 новых populated upgrade/HTTP/SQL/policy tests PASS,16 trusted assessment
и9 admission cases PASS,25 migration smoke/12 preflight/14 inventory/6 reference
PASS. Android security8/5 и native runner5 PASS. Исправлена ошибка нового replay
test: requestId каждого HTTP запроса новый, неизменный receipt сравнивается
отдельно; серверный replay не менялся. Один локальный запуск неверного имени
adminVehicleRules.test.mjs получил MODULE_NOT_FOUND; правильный npm script
adminVehicleRuleApi запускается отдельно, это не дефект runtime/успешный тест.
3 JVM и2 native UI cases добавлены, полные Android/backend CI ещё pending.
Screenshots используют liftback в тех же synthetic states; никаких real data.
Описание/пределы: COMFORT_LIFTBACK_IMPLEMENTATION_2026-11-18.md.


Локальный правильный `npm run test:admin-vehicle-rules` завершён:20 tests PASS. Новый liftback workflow повторён после добавления HTTP unknown-body отрицательного случая:2 tests PASS. Перед публикацией diff --check PASS; полные CI и Android JVM/native остаются pending.
