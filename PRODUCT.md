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

Состав [справочника142 марок](docs/VEHICLE_MAKES_CATALOG_PROPOSAL_2026-11-16.md)
утверждён владельцем 2026-11-16: исходные140, включая Haval/Chery, затем
Belgee/TENET по прямому поручению. Прежние140 записи и aliases сохранены.
Внедрение UI/API, поиск и новые GET фотоконтроля этим решением не утверждены.

Владелец подтвердил исключение тарифа **Эконом**. Поддерживаются только
Комфорт/Бизнес/Минивэн. Справочник марок не является допуском всех моделей марки.
[Поиск в репозиториях](docs/VEHICLE_TARIFF_CLASSIFICATION_SEARCH_2026-11-16.md)
фиксирует исторический результат до передачи владельцем CSV на116 строк.
Тариф модели и список исключаемых машин не назначаются по догадке.

Позднее 2026-11-16 владелец ответил «Утверждаю» на вопрос о15 китайских
дополнениях и условиях:6 Комфорт/9 Бизнес, минимальный год2023.
[Утверждённые строки и условия](https://github.com/lobanov0710-blip/uber-v3/blob/docs/vehicle-classifier-v1-20261116/docs/VEHICLE_CLASSIFIER_CHINA_EXTENSION_PROPOSAL_2026-11-16.md)
зафиксированы в [Draft PR31](https://github.com/lobanov0710-blip/uber-v3/pull/31).
Следующим сообщением «Исходные 116 строк и добавление Belgee/TENET добавить
и утвердить» владелец утвердил исходные116 правила/годы/примечания и две марки.
В актуальном CSV v2 теперь131 approved/0 pending:76 Комфорт/31 Бизнес/24 Минивэн.
В исходных116 в v2 изменены только статусы; прежние15 правила и их условия
неизменны. CSV v1 сохранён как архив вложения с первоначальными статусами.
Названия EXEED/Skoda/FAW Bestune B70 утверждены в исходном написании; изменения
aliases/нормализации/существующих регистрационных данных не выполнялись.
Условие отдельного согласования кузова Avatr12/Zeekr001 сохранено. approved
относится к модельному правилу; проверка конкретного автомобиля и серверная
модерация обязательны. Подключение к runtime и новые GET фотоконтроля этим
утверждением не разрешены; merge/deploy не выполнялись.


Владелец 2026-11-16 явно утвердил «Каталог и список проверок» для следующего
G2 фотоконтроля по предложению source6f4e3caf0ce283bbcca086ccf9b9e18f48935796.
Разрешены два предложенных metadata GET и Admin UI создания проверки; выбор
варианта повторно не запрашивать. Реальные материалы/допуск/OWNER-only phone
scope не расширяются. Следовать утверждённым permissions/MFA/audit/no-store/
scoped pagination и foreground-only/no replay правилам предложения.


Проверенный итог 2026-11-16: утверждённый component/synthetic срез реализован,
backend CI43947136962 и Android CI43950844261 SUCCESS; независимый finish
review без blockers. Точные code SHA/tree,318 JVM/99 native,69 backend scripts,
12 новых SQLite/HTTP tests и provenance28/10PNG/APK сохранены в
[verification receipt](docs/ADMIN_PHOTO_CONTROL_VERIFICATION_2026-11-16.json).
Это не полная приёмка G2/live phone/release; G1paused, OWNER-only scope,
D4–D7pending и запрет merge/deploy без отдельного решения сохраняются.
