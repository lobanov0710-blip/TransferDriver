# G2: проверка диапазонов и подготовка закрытой инвентаризации

После QA source088c8053 / CI44508544855 следующий независимый этап утверждённого
плана — powertrain ranges121 и actual legacy inventory. OWNER private signing/
физическая установка QA и права нового тестового аккаунта остаются открытыми;
они не подменяются новым созданием OWNER/сбросом TOTP или расширением phone scope.

[Backend Draft PR46](https://github.com/lobanov0710-blip/uber-v3/pull/46) исправляет
подтверждённую ошибку offline inventory: отсутствующий/NULL vehicle reference
не должен исчезать из problem counts online/active assignment. Два новых regression
сначала FAIL, после двух LEFT JOIN исправлений все16 PASS; существующие6 reference
и25 migrations PASS. Full backend source1882d622 / CI44518440499 SUCCESS;
точный receipt с последующей проверкой документов хранится в backend ветке.
Это offline audit tool, не смена правил допуска/данных автомобилей.

[Новый range review](https://github.com/lobanov0710-blip/uber-v3/blob/30b720ad0a48c9a368c66dcf07e56d8cd6ebfadb/docs/VEHICLE_POWERTRAIN_RANGE_REVIEW_2026-11-19.md)
детализирует десять mixed-powertrain семейств:18 первичных источников и20 ограниченных
наблюдений с датами/рынками. Нынешняя модификация не доказывает весь диапазон
фактических годов. Каталог121/архив116/минимальные годы и условия сохранены;
исчерпывающее покрытие всех лет/поколений/рынков по-прежнему не завершено.
Лифтбек уже утверждён только в Комфорте; прежние pending body заметки исторические.

Владелец сообщил: готового снимка нет, подготовить порядок для утверждения.
[Предложение закрытого чтения](https://github.com/lobanov0710-blip/uber-v3/blob/30b720ad0a48c9a368c66dcf07e56d8cd6ebfadb/docs/VEHICLE_LEGACY_INVENTORY_READ_PLAN_2026-11-19.md):
один из5 фиксированных aggregate SELECT на точном STAGING D1,18 счётчиков в
приватном JSON на Windows владельца. Для чата/журнала — квитанция без самих чисел,
personal identifiers/credentials/provider output. Реальные запросы/экспорт не
выполнялись; remote CLI готовится только после явного утверждения этого варианта.

Android runtime и сборка не менялись. QA остаётся на своём отдельном verified
source; повторной Android/native/UI приёмки этот docs-only checkpoint не заявляет.
G1paused,D4–D7pending; нет classifier activation/seed/remote migration/merge/deploy
или нового Dispatcher права. Следующие решения опираются на закрытый отчёт и
оставшиеся range gaps; ни наличие инструмента, ни source observation не дают допуска.
