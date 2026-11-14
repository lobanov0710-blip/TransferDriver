# Аудит ТЗ, плана и журналов — 2026-11-14

## Результат

**ТЗ не изменено; реализация частичная; production-выпуск не готов.**
Журналы подтверждают развитие D1–D3 и исправления безопасности, но не полную
приёмку ТЗ. Успешные проверки относятся к реализованному объёму. Данных для
заявления «все требования выполнены» или «утечек нет во всех сценариях» нет.

## Источники и границы

Проверены ТЗ §1–40, первоначальный implementation review/порядок этапов,
решения D1–D7, история checkpoint в обоих журналах, последние owner setup/
STAGING preflight/signing инструкции, build receipt и код ключевых API/UI/guards.
Сверены GitHub heads, Draft PR, CI jobs/logs; выполнены новые локальные проверки.
Это аудит соответствия и доказательств готовности с выборочной трассировкой кода,
не новый полный построчный code review, pentest или live Firebase/Cloudflare audit.

| Источник | Проверенная база |
|---|---|
| Driver branch | arch/spec-v1-security-baseline, 0c7777025c7c58231efaedbb2b8d82c335807ce4 |
| Backend branch | arch/spec-v1-backend-phase1, 905c6ebe86907056f0d8be8f6f6edaff07b135e4 |
| Утверждённое ТЗ | 2026-11-06, blob 7434dc0b1548b4f2c5382f12d6cc5399f93d0a84; точное совпадение |
| D1–D3 | Утверждены 2026-11-07; D4–D7 не утверждены |
| PR #6 / PR #28 | Open, Draft, merged=false; базы master/main |

Локальные checkpoint commits имеют другие SHA, но их Git trees точно совпали
с опубликованными базами. Новый checkpoint меняет документацию; application code,
утверждённое ТЗ, инфраструктура и настройки аккаунтов не изменяются.

## Замечания по приоритету

### A1 — Высокий: полный пользовательский объём ТЗ ещё отсутствует

Отдельного Admin нет: settings.gradle.kts:26 содержит только include(:app).
Driver имеет login/orders/photo-control/support UI, но не полный мастер регистрации,
карточки автомобилей, activeVehicle/availability slider и phase flow. Backend для
многих из этих функций уже реализован — отсутствие UI не означает отсутствие API.
Passenger order-chat, GPS foreground flow, VoIP/архив и рекомендательный подбор
не реализованы как полные функции. Таблицы chat_messages/calls и отдельный support
chat не закрывают ТЗ §23/25/26/28.

Последствие: нельзя выдавать Driver APK за выполнение всего ТЗ или обеих apps.
Действие: матрица каждого раздела и конкретные критерии в PROJECT_PLAN_V1_0.md;
завершать G2–G4, затем общую приёмку G5. В этом аудите функции не имитировались.

### A2 — Высокий: живой D3 E2E и выпуск блокированы настройками

Настоящий SDK config получен, локальные APK/AAB уже собраны. Блокер «нет Firebase
identifiers/config/Android SDK» закрыт 2026-11-12. По последним ответам владельца
App Check не настроен, upload key отсутствует, Play Console/канал не подтверждены,
R2 permissions/bucket не готовы. Обновлений от владельца после этого нет.
Новый live infrastructure probe в этом аудите не выполнялся: исторический HTTP 404
от 2026-11-11 не выдаётся за измерение 2026-11-14.

Подтверждение поездки также блокирует отсутствие D4 provider. Это предусмотренный
защитный отказ: driverTripOperationalService.js:9 и SQL verificationGuard в
core/driverTripOperationalRepository.js:788 не позволяют начинать движение без
verified с точным assignment/revision/driver/order/vehicle binding и сроком.
Это не доказательство найденного обхода; положительный trip flow ещё не реализован.

Действие: G1 с тестовыми данными и участием владельца; затем отдельные D4/D5
для настоящей identity-проверки. D1–D3 не заменяют эти решения. Ранее применённые
STAGING migrations 0005–0017 не применять повторно без проверки pending set.

### A3 — Высокий: навигация не соответствует §22 ТЗ

OrderDetailsActivity.java:1056, openRoute(): формируется Yandex Maps rtext=from~to
для любого активного заказа; выбор цели не зависит от authoritative trip phase,
текущая позиция не задаёт начало маршрута и приоритет Yandex Navigator не реализован.
OrderApi использует assignments/terminal status и не содержит phase endpoint.

ТЗ требует текущая позиция → pickup до поездки и → destination после её начала.
Действие: задача G3 с phase API/UI и двумя сценариями навигации; не объявлять
существующий ACTION_VIEW полным выполнением §22. Product code в документальном
аудите не менялся; этот пункт остаётся открытым функциональным замечанием.

### A4 — Средний: нет единого актуального плана и точки входа в историю

IMPLEMENTATION_REVIEW_V1_0.md отражает 2026-11-06: перечисляет отсутствующие Phone
Auth/API/сборки, которые позже реализованы. AUDIT_2026-11-07.md содержит прежние
release blockers, часть которых закрыта. В начале журнала «Текущий этап» относится
к ARCH-11.16 от 2026-10-31; внизу уже D3/STAGING. Датированные записи объясняют
эволюцию, но при чтении отдельного документа легко получить устаревший статус.

Исправлено документально: добавлены текущий план/матрица 1–40 и этот аудит,
указатели историчности в первоначальном review/audit, актуальный индекс обоих
журналов и STAGING preflight. История, даты утверждений и прежние результаты
не переписаны. Названия функций не подменены декларацией готовности.

### A5 — Средний: подтверждённый quality gate неполон для release

Сохранённый Android dependency/OSV inventory от 2026-11-07 покрывает 129 компонентов
конфигурации debugRuntimeClasspath с 0 matches на ту дату. В нём отсутствуют
firebase-auth и firebase-appcheck-playintegrity, уже добавленные в app/build.gradle.kts.
Это пробел покрытия; конкретная новая CVE этим аудитом не установлена.
Новый полный lint не запускался: прошлый дополнительный lintStaging был отменён
пользователем; выполненный lintVital не является полным lint всех вариантов.
Backend npm audit 0 в проверенном CI — другой граф и не закрывает Android coverage.

Действие перед G5: актуализировать inventories/advisory checks всех используемых
Android variants, выполнить полный lint Driver/Admin и device/E2E проверки.
Сегодняшний аудит не присваивает этим невыполненным проверкам PASS.

### A6 — Низкий: встроенная трассировка сборок ограничена

Receipt честно фиксирует AGP metadata NO_VALID_GIT_FOUND. Ссылка на source commit,
хеши пяти source/workflow files и шести артефактов есть; 2026-11-14 они независимо
совпали. Это сохраняет внешнюю трассировку, но не является встроенной signed CI
attestation. Действие для release: воспроизводимая сборка из подтверждённого commit,
владельческая подпись/защищённая CI-среда и проверяемый release provenance.

## Что уже подтверждено

- ТЗ сохранилось побайтно; последовательность разделов 1–40 полная.
- Backend: Firebase token/App Check exchange, private upload/readiness/moderation,
  OWNER/permissions, assignments/availability/phases, photo-control и support foundations.
- Driver: Phone Auth/exchange, assigned-only orders/terminal recovery,
  vehicle photo-control и support/chat UI. Это не trip liveness или passenger chat.
- Keystore AES-GCM с backend-bound AAD v2; HTTPS/origin/replay guards;
  защищённые экраны/backup/notifications и session-race regressions проверяются.
- Firebase identifiers и настоящий SDK config получены; API key не помещён в Git.
- Debug APK подписан debug key; staging/release APK/AAB unsigned по receipt.
  Это не готовые подписанные production-релизы.

## Доказательства проверок

Новые локальные проверки 2026-11-14:

| Проверка | Результат |
|---|---|
| Backend npm test, Node 24.22.1 / npm 11.9.0 | 61 suite PASS; выполнены все команды test chain |
| tools/test_migrations.py | 17 migrations PASS; только локальный SQLite |
| tests/d3StagingPreflight.test.py | 12 tests PASS; локальная проверка, без remote writes |
| Android tools/check_security.py | 7 tests PASS |
| Android source XML parse | 25 XML PASS |
| Spec hash, receipt/source/artifact integrity | 40 разделов, исходный blob, 5 source hashes и 6 artifact hashes PASS |
| Документация | Матрица 1–40 без пропусков, существующие локальные ссылки, git diff --check |

Повторно проверены существующие CI именно базовых head commits, не выданы за
новые прогоны 2026-11-14:

- [Android run 43316231722](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43316231722),
  job 131524852748, SUCCESS: JVM 29 debug + 29 staging + 29 release, Android API 35
  12 tests, security guards, сборки и endpoint/App Check policy. Проверены логи counts.
- [Backend run 43316251413](https://github.com/lobanov0710-blip/uber-v3/actions/runs/43316251413),
  job 131524919923, SUCCESS: locked install, npm audit 0 vulnerabilities, 17 migrations,
  12 preflight tests, suites, local workerd/D1 и dry-run; deployment не выполняется.

Новый APK build, полный lint, свежий Android advisory scan, runtime с настоящим
Firebase config, authenticated E2E, live R2 privacy checks и pentest не выполнялись.
Ранее выполненный CI использует synthetic Firebase fixture; он не подтверждает
настройки Firebase проекта, SMS или готовность Play Integrity для реального канала.

## Продолжение

Следовать [PROJECT_PLAN_V1_0.md](PROJECT_PLAN_V1_0.md): G1 — owner setup и D3 E2E;
G2 — недостающие Driver/Admin сценарии; G3 — утверждённый identity/phase/navigation;
G4 — оставшиеся GPS/chat/VoIP/ranking; G5 — полная приёмка и отдельное решение о
merge/deploy. Участие владельца перечислено в плане и signing/owner setup guides.
Производственные настройки, deploy, merge, remote migrations и D4–D7 этим аудитом
не изменены и не утверждены.
