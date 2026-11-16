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
[PR#30](https://github.com/lobanov0710-blip/uber-v3/pull/30); отдельный Admin UI
назначения остаётся следующим плановым срезом.

G1 приостановлен. Реальные документы/биометрия и решения D4–D7 не утверждены.
Полный мастер и финальная подача регистрации в этом срезе не реализованы.
GET me не содержит причин возврата элементов: UI их не выдумывает.
Driver проверяется на синтетическом транспорте; изоляция OWNER-only STAGING
не снимается для обхода паузы G1. Merge и production deploy требуют отдельного
решения владельца.
