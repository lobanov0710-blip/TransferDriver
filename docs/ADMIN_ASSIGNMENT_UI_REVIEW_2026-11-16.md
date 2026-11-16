# G2 Admin manual assignment — UI record, 2026-11-16

Свежий документирующий проход Impeccable после финального исправления подтверждения.
Обычное расширение native Java/XML Admin: D1–D3 и вариант A утверждены;
G1 приостановлен, D4–D7 ожидают решений. Отчёт не расширяет утверждённый объём.

## Происхождение и пределы доказательств

Лично прочитаны `AGENTS.md`, `PRODUCT.md`,
`docs/ADMIN_ASSIGNMENT_UI_SCOPE_2026-11-16.md`,
`docs/ADMIN_ASSIGNMENT_UI_VERIFICATION_2026-11-16.json` и последние 100 строк
`docs/PROJECT_JOURNAL.md`; первичные `AssignmentActivity.java`,
`activity_assignment.xml`, `assignment_strings.xml`, `values/themes.xml`;
для сравнения — `AdminActivity.java` и `activity_admin.xml`; выборочно для
семантики действия — `AssignmentAction.java` и `AssignmentRepository.java`.
Java-файлы находятся в `admin/src/main/java/ru/transferservis/admin/{ui,data}`,
XML — в `admin/src/main/res/{layout,values}`. Лично открыты все 10 финальных
PNG ниже через `view_image`; локально сверены имена, размеры и SHA-256 с receipt.
Изображения не редактировались.

Прочитаны рекомендации Impeccable `reference/document.md`,
`reference/degraded/documenter.md`, `reference/android.md`. Launcher отсутствует;
reviewer и documenter работают через раскрытый generic-role fallback: harness
не предоставляет специализированный selector. Web detector для native Android
пропущен. Применена ветка сравнения и документирования обычного расширения.

По receipt и проверенному родительским агентом CI log: run
[43877768185](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43877768185),
job `133335205029`, source `86ae9b46fe6b7918eb28cf00b822a34f26f3b52c`,
tree `0ae36dab6748f49ddc73080a755d8d57b590d518` — SUCCESS:
291 JVM-выполнение (Driver 48×3, Admin 49×3), 84 native case (38+46),
все 6 build/lint вариантов, staging bundles/debug test APK и
security/endpoint/AppCheck/signing/UID gates; failures/errors/skipped — 0.
Документирующий агент эти проверки не запускал. Успешный lint не доказывает
отсутствие предупреждений; исторические Driver warnings не объявляются устранёнными.

Кадры получены на эмуляторе API35 x86_64 Pixel6, light/font 1.0 и dark/font 1.3:
реальные Activity/Keystore, synthetic HTTP вместо live backend. В test APK
`FLAG_SECURE` временно снимается и восстанавливается для capture.
Это не физический телефон, tablet, TalkBack или live acceptance; API26–29
и другие runtime не проверены этим набором. Контрастные коэффициенты не измерялись.
Отдельный visual-world brief, comp/seed/QUALITY BAR card отсутствуют и
для этого утверждённого расширения не требуются; creative-identity интервью не проводилось.

## Сопоставление с существующим интерфейсом

Пять строк системы по фактическому коду:

1. Палитра: унаследованная `Theme.Material3.DayNight.NoActionBar`; новых цветовых значений в проверенных assignment-ресурсах нет.
2. Типографика: системная, theme roles `TitleLarge`, `BodyLarge`, `BodyMedium`; локальных font family и размеров px нет.
3. Компоновка: вертикальный экран с MaterialToolbar, прокручиваемым содержимым и нижними действиями; поля корня 16dp плюс system/cutout/IME insets.
4. Компоненты: многострочные outlined MaterialButton для выбора, TextInputLayout/EditText для причины, MaterialAlertDialogBuilder для решения.
5. Правила этого среза: явный выбор заказа и связки, обязательная причина, отдельное подтверждение и явный повтор исходного действия; новые обще-системные named rules не вводятся.

Incumbent `activity_admin.xml` уже использует вертикальные формы, native
Button/Spinner, theme typography и ScrollView; его внутренний отступ — 24dp.
Новый экран сохраняет эту платформенную основу, добавляет toolbar и разделяет
прокручиваемый выбор/форму от нижних действий. Разница 24dp/16dp — наблюдаемая
локальная компоновка, не новый глобальный spacing token. В Admin отсутствует
`values-night/themes.xml`: dark scheme наследуется от DayNight, что видно в кадрах.

В XML минимальная высота основных кнопок и фильтра — 48dp, поля причины —
96dp; перед полем — 16dp. У динамических строк minimum height 48dp и верхний
margin 8dp, у текстовых пояснений margins 8dp сверху/снизу. Это значения исходников,
не измерение всех touch bounds. Строки допускают перенос без ellipsis;
на dark/font 1.3 заголовок выбора связки и пояснение блокировки переносятся.
В показанных кадрах текст причины, ID заказа, действия и кнопки диалога читаемы,
видимого обрезания этих образцов не обнаружено; произвольные длинные данные этим
не исчерпываются. Диалог имеет округлую форму и тональную поверхность темы.

Семантика, подтверждённая чтением выбранных исходников: первая строка не выбирается
автоматически; полный opaque orderId показан в выборе и подтверждении. Недопущенная
связка показана текстом с пояснением; offline сам по себе не запрещает выбор.
Причина обязательна, после trim — 1–500 символов. Действие хранит исходные
order revision/body/key; при неопределённости UI подписывает первоначальный выбор,
закрывает новые назначения, отделяет GET обновления от подтверждаемого повтора.
Repository повторно запрашивает server me/grants/MFA перед чтением/командой;
onStop отменяет работу и очищает выбор, причину, диалог и uncertain intent.
В коде видны `FLAG_SECURE`, защита positive touches, запрет сохранения ввода и
Autofill, accessibility heading/live region. Это выборочное сопоставление UI
и поведения, не полный security audit и не пользовательская проверка TalkBack.

## Финальные native captures

Все файлы находятся в `.impeccable/review/`; каждый лично просмотрен,
имеет размер **1080×2400** и совпавший SHA-256. Источник — artifact
`14231790908`, ZIP digest
`4b7ad2b8b071127e7007007256b6cc10cbaace73ba9b00afff9384d75ea72760`;
метаданные и срок хранения приведены в [receipt](ADMIN_ASSIGNMENT_UI_VERIFICATION_2026-11-16.json).

| Файл | SHA-256 |
| --- | --- |
| assignment-orders-light.png | `d5aeb9af8c681288185364167faa69b28e95e82c2fe5b1af09c4050298c8c4b3` |
| assignment-orders-dark-large.png | `0d6342f4781b801393d1e2c1b83339c9d6a5d985fb32005a28fa87f725a1e822` |
| assignment-options-light.png | `27392edb6e40f91e581a4c0fecf1a0104a749b156e851c18bd6a7932d3cdced2` |
| assignment-options-dark-large.png | `e3c5fd859d0ec41334aaee2f55a1e5ef105f930d4bcd26f84ef4a11b9d24a5f7` |
| assignment-form-light.png | `f2ceb8ae81f82c319ec2f49c549289a418e93a254bba2be19ff6ca6eaee19c44` |
| assignment-form-dark-large.png | `da126581e58ab70ee36b38aa0e9dc3b7edd1bc168cb25d73644326411805c48a` |
| assignment-confirm-light.png | `5d02d1f44f75920b5635408b0e97a0c74412d225e655b2a1c4178266e69d3943` |
| assignment-confirm-dark-large.png | `47fcfce6d018b537dbc8e0167d35fc4ab0e902a6bcb4b662f90df40dc6dcff48` |
| assignment-uncertain-light.png | `d6ffa470c82c0b0f80b1bc925d830a370d5819cc561184b4cb1cc6831d9a02eb` |
| assignment-uncertain-dark-large.png | `668528f52d92a899bf9a7dfb0298f1d90bc775ddbe67e29d5864137fe4ecce99` |

## История review и итог документации

Согласно журналу/receipt, свежий `/root/assignment_finish_reviewer` сначала
проверил 10 исходных кадров и первичные Java/XML и вынес **FIX** с одним
material замечанием: квадратное legacy AppCompat подтверждение. Единственная
коррекция — существующий MaterialAlertDialogBuilder; secure-window,
obscured-touch, body/key, confirmation/cancel поведение сохранено.
Тот же reviewer оценил финальные recaptures по тем же 10 путям:
**RESOLVED**, introduced visual regressions не наблюдал; **SHIP только для
scored confirmation correction**. Родительский агент повторно открыл финальные
10 кадров перед вердиктом. Документирующий проход лично видел финальные кадры;
историю первоначального замечания переносит из журнала/receipt, не выдаёт за
собственный просмотр первоначальных файлов. Вердикт не означает приёмку всей
поверхности, security clearance, live/hardware acceptance или завершение G2.

`DESIGN.md` и `.impeccable/design.json` в корне и Admin отсутствуют; в этом
проходе не создавались и не менялись. Новый мир, шрифты, палитра, зависимости
и исправление чужого drift не вводились. Не канонизировано: исправленное legacy
подтверждение и прежние incumbent Headline5/Headline6 aliases не становятся
новыми правилами; оставшийся incumbent код вне этого узкого среза не ремонтировался.

Operational routes остаются 404 на OWNER-only STAGING; backend и whitelist
этим проходом не менялись. Merge/deploy, live назначения и hardware acceptance
не выполнялись. Записан только этот UI record; журнал, план, PRODUCT и receipt
обновляет родительский агент отдельно.
