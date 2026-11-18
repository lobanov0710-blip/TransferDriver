# G2: готовность каталога и старых записей — 2026-11-18

После проверенного OWNER editor (draft PR19, source
`739059daa09341fd3239644b1ab26c7f8061b1b8` — см. точный SHA в его verification JSON)
начата следующая часть плана: powertrain audit и подготовка read-only legacy inventory.
Android runtime, APK, allowlist OWNER-phone и права диспетчера не менялись.

Основной [отчёт backend](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-classifier-readiness-audit/docs/VEHICLE_CLASSIFIER_READINESS_2026-11-18.md)
и [реестр 121 строки](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-classifier-readiness-audit/docs/VEHICLE_POWERTRAIN_AUDIT_2026-11-18.json).
9 семейств имеют частичные первичные свидетельства, 112 pending; полностью
проверенными по всем годам/поколениям/рынкам не объявлены никакие строки.
CSV, годы и правила не изменены. Инструмент локального чтения проверяется на
синтетической базе; реальная legacy inventory и готовность к активации не установлены.

Далее: завершить доказательства по оставшимся исполнениям; подготовить защищённое
чтение реальных legacy и конкретный план устранения пробелов для рассмотрения.
Seed, remote migration, merge, deployment и активация требуют отдельных решений.
G1 на паузе; D4–D7 ожидают утверждения. Этот checkpoint не является новым APK
или повторной Android-приёмкой. Результаты backend CI фиксируются отдельно в журнале.
