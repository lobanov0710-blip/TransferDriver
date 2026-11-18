# G2: готовность каталога и старых записей — 2026-11-18

После проверенного OWNER editor (draft PR19, source
`739059daa09341fd3239644b1ab26c7f8061b1b8` — см. точный SHA в его verification JSON)
начата следующая часть плана: powertrain audit и подготовка read-only legacy inventory.
Android runtime, APK, allowlist OWNER-phone и права диспетчера не менялись.

Основной [отчёт backend](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-classifier-readiness-audit/docs/VEHICLE_CLASSIFIER_READINESS_2026-11-18.md)
и [реестр 121 строки](https://github.com/lobanov0710-blip/uber-v3/blob/arch/g2-classifier-readiness-audit/docs/VEHICLE_POWERTRAIN_AUDIT_2026-11-18.json).
Первый проход расширен до всех 121 строк: найдены частичные первичные
свидетельства для каждой, 111 источников всего. Исчерпывающее покрытие всех лет,
поколений и рынков не подтверждено ни для одной строки. Источники и пределы
доступа указаны в revision 2 реестра. Каталог/годы/правила не менялись.
Реальная legacy inventory и активация не выполнены.

Вопрос лифтбеков закрыт: владелец ответил «Только в комфорте». В отдельных
ветках arch/g2-comfort-liftback реализован тип liftback для comfort с запретом
business/minivan/OWNER exception bypass. Проверки новых runtime изменений
ведутся отдельно; массовой смены pins/правил, seed и активации нет.
Подробности: [утверждённое решение](VEHICLE_BODY_POLICY_CLARIFICATION_2026-11-18.md)
и [реализация](COMFORT_LIFTBACK_IMPLEMENTATION_2026-11-18.md).

Далее: завершить доказательства по оставшимся исполнениям; подготовить защищённое
чтение реальных legacy и конкретный план устранения пробелов для рассмотрения.
Seed, remote migration, merge, deployment и активация требуют отдельных решений.
G1 на паузе; D4–D7 ожидают утверждения. Этот checkpoint не является новым APK
или повторной Android-приёмкой. Результаты backend CI фиксируются отдельно в журнале.
