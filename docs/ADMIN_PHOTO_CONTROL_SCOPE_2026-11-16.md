# G2 Admin — каталог автомобилей и внеплановый фотоконтроль

Владелец утвердил вариант «Каталог и список проверок», ответ:
«Утверждают каталог и список», 2026-11-16. Proposal source
6f4e3caf0ce283bbcca086ccf9b9e18f48935796. Реализация в отдельных Draft PR12/32.

## Поведение

Вход из server-authorized профиля Admin: автомобили по существующим статусам,
явный выбор конкретного server vehicle, metadata список прошлых/текущих проверок.
Чтобы читать каталог, нужны vehicles.read; список checks и создание требуют
также vehicles.verify. OWNER каждый раз подтверждается текущим server MFA.
OWNER-only phone test scope не показывает вход и не разрешает операции.

Создать можно только complaint/manual: основание и режим выбираются явно,
причина1–500 после trim. Monthly не создаётся этим UI. Подтверждение показывает
make/model/plate/vehicle ID, основание, полный текст режима и причину.
Фотографии, private URLs, чужие причины/review notes не загружаются.
Списки имеют явное scoped paging, без auto-selection автомобиля/режима.

## Сессия и результат

Перед каждым GET/POST выполняется server me и сравнение исходной Keystore-сессии;
этот же epoch/token проверяется перед transport и после ответа. Никакой
автоматической смены аккаунта, refresh или mutation replay. Старый ответ не
может заменить данные новой сессии или удалить последующий вход.

Список checks должен совпасть по vehicle ID и vehicleRevision выбранного
автомобиля; иначе409 закрывает выбор. Mutation использует vehicle revision,
не photo-check revision. ACK сверяется по ID/kind/mode/pending/ревизиям;
затем перечитывается checks. Неизвестный результат сохраняет исходные RAM
account/scope/vehicle/revision/body/key. Обновление — только чтение, повтор
возможен исключительно после нового явного подтверждения прежнего намерения.
401/403/409 закрывают старый выбор; подтверждённый ACK не превращается в
повторную mutation, если последующее чтение не удалось.

onStop отменяет pending requests и очищает reason/selection/снимки/intent.
Состояние формы не сохраняется в view hierarchy/preferences/файлы/clipboard.
FLAG_SECURE действует для Activity и диалога, autofill/IME personalization
выключены, obscured-touch защита включена. Native multiline spinner items
сохраняют полные ограничения при крупном шрифте; причина не публикуется в логах.

## Проверки и пределы

Backend verified source080d4b49bb902d782efe2615d92bf05fa3d64dc6,
CI43947136962/job133579673172 SUCCESS:69 scripts,12 новых SQLite/HTTP cases,
18 migrations/12 preflight,6 native Worker/D1 workflows,5 CLI scenarios,
audit0/dry-run. Исправлен прежний immediate phase bypass: atomic SQL не даёт
продолжать operational phase до успешной проверки, а after_current_trip
позволяет продолжить текущую поездку. Отмена/terminal completion не добавлялись.

Android source a0308bec394cd28d821f52bbcfdc05e5e09f4be7,
CI43950844261/job133592621453 SUCCESS:318 JVM executions
(Driver48×3/Admin58×3),99 native cases (Driver38/Admin61), все6 вариантов
build/lint и security guards PASS. Новые9 JVM cases выполняются в3 вариантах,
15 native cases подтверждены отчётами. Загружены и сверены с GitHub digest
APK и28 native PNG; все10 новых кадров (light/dark font1.3) независимо
просмотрены, SHA256 сверены с CI. Finish verdict: SHIP для утверждённого
component/synthetic среза, blocker findings нет. Оба замечания по spinner и
истечению MFA закрыты финальными кодом/CI/кадрами. См.
[receipt](ADMIN_PHOTO_CONTROL_VERIFICATION_2026-11-16.json) и
[handoff](ADMIN_PHOTO_CONTROL_HANDOFF_2026-11-16.md).

G1 остаётся paused, D4–D7pending. Реальные материалы/биометрия/R2 E2E не
включались. Классификатор131 правил/142 марки не подключён к runtime этим
срезом. OWNER-only staging whitelist не расширен, merge/deploy отсутствуют.
Debug CI signing identity временная; совместимость обновления установленного
телефонного APK не проверена. Это component/synthetic срез, не полная приёмка G2.
