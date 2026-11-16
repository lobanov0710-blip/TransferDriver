# Transfer Driver — утверждённый контекст

Источник требований: [ТЗ v1.0](docs/TRANSFER_DRIVER_TECHNICAL_SPEC_V1_0.md).
Работать только по нему, утверждённым решениям и [журналу](docs/PROJECT_JOURNAL.md),
согласно [AGENTS.md](AGENTS.md). Этот файл не вводит новых решений.

Driver и отдельный Transfer Admin — Android, Java + XML (D1). Существующий
интерфейс использует native Material3 DayNight и системную типографику.
Отдельный визуальный стиль, шрифты, палитра и новые библиотеки не утверждались.

В G2 утверждена последовательность: Admin moderation → Driver registration/
vehicles → availability/manual assignment. OWNER сообщил об успешном подключении
нового TOTP и входе в Admin на телефоне; это не полная приёмка G2.

Текущий Driver срез: личные данные, подтверждённый телефон только для чтения,
несколько автомобилей, обязательный VIN, отдельное сохранение и серверные
статусы/версии. [Контракты и границы](docs/DRIVER_REGISTRATION_SCOPE_2026-11-15.md).
Backend определяет доступ и допуск; клиент не назначает одобрение или verified.

Текущий независимый Driver срез: нижний slider выбора on_line после выбора
одобренного автомобиля, compact Радар активен по authoritative server state,
off_line с сохранением текущего автомобиля. Используются существующие D3
GET me/PUT availability, revision и Idempotency-Key.
[Границы](docs/DRIVER_AVAILABILITY_SCOPE_2026-11-15.md). Проверенный synthetic checkpoint и его ограничения
зафиксированы в [receipt](docs/DRIVER_AVAILABILITY_VERIFICATION_2026-11-16.json). Admin manual assignment read API version2, вариант A,
явно утверждён владельцем 2026-11-16. Два GET реализованы и проверены в backend
[PR#30](https://github.com/lobanov0710-blip/uber-v3/pull/30). Отдельный Admin UI
ручного назначения реализован в [Draft PR#10](https://github.com/lobanov0710-blip/TransferDriver/pull/10):
явный выбор заказа и driver+vehicle, причина, подтверждение, order revision и
прежний ключ при явном повторе неопределённого результата.
[Границы среза](docs/ADMIN_ASSIGNMENT_UI_SCOPE_2026-11-16.md) сохраняют
операционные маршруты закрытыми на OWNER-only STAGING; реализация не означает
live приёмку, разрешение merge/deploy или завершение всего G2.

G1 приостановлен. Реальные документы/биометрия и решения D4–D7 не утверждены.
Полный мастер и финальная подача регистрации в этом срезе не реализованы.
GET me не содержит причин возврата элементов: UI их не выдумывает.
Driver проверяется на синтетическом транспорте; изоляция OWNER-only STAGING
не снимается для обхода паузы G1. Merge и production deploy требуют отдельного
решения владельца.

Состав [справочника140 марок](docs/VEHICLE_MAKES_CATALOG_PROPOSAL_2026-11-16.md)
явно утверждён владельцем 2026-11-16, включая Haval/Chery. Утверждение
относится к составу списка; модели, внедрение/поиск и новые GET фотоконтроля
этим решением не утверждены. Записи автомобилей и правила допуска не менялись.
