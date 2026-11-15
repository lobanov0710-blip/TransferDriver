# Driver registration — native UI review, 2026-11-15

Статус: независимый reviewer вернул disposition **ship только для проверенных
исправлений** после post-fix recapture. Это не полная приёмка G2, live E2E или
разрешение deployment. Документ фиксирует verdict и свидетельства, не заменяя
утверждённое ТЗ, решения владельца или журнал.

## Основание и границы

Проверены [AGENTS.md](../AGENTS.md), [PRODUCT.md](../PRODUCT.md),
[границы среза](DRIVER_REGISTRATION_SCOPE_2026-11-15.md), утверждённое ТЗ,
D1–D3 и актуальные записи журнала. D1 предусматривает отдельный Admin Java/XML;
D4–D7 не утверждены, G1 приостановлен. Текущий Driver срез включает личные
данные, read-only подтверждённый телефон, несколько автомобилей, VIN,
раздельное сохранение и серверные статусы/revisions. GET me не возвращает
причины возврата элементов; UI не выдумывает их. Полный wizard, реальные
документы/биометрия, финальная подача и availability в этот срез не входят.

Это обычное расширение существующего Android Java/XML интерфейса. Новая
визуальная система, палитра, шрифты и библиотеки не утверждались. Incumbent
DESIGN.md отсутствует; DESIGN.md и `.impeccable/design.json` не создаются.
Launcher был недоступен; использован ранее объявленный fallback. Documenter
выполнен отдельным fresh generic agent по reference/degraded/documenter.md:
штатная роль harness недоступна. Native detector не запускался.

## Проверенные источники и сохранение системы

Сравнены текущие production XML/Java и локальная история: исходный экран
регистрации `04c0aef`, UI correction `1244d26`, fixture correction `c5d8ae4`,
окончательный source `49499fd`. Diff `1244d26..49499fd -- app/src/main` и
`04c0aef..49499fd -- admin/src/main` пуст: последующие изменения касаются
только tests/workflow/docs.
Проверены `app/src/main/res/values/themes.xml`, `values-night/themes.xml`,
`values/colors.xml`, `layout/activity_login.xml`, `layout/activity_driver_main.xml`,
`layout/activity_registration.xml`, `values/registration_strings.xml` и
`ui/registration/RegistrationActivity.java`. Сравнение с `9e65933` не показывает
изменений существующих themes/colors/login и dependency declarations
`app/build.gradle.kts` / `gradle/libs.versions.toml`; главный экран получает
кнопку входа в анкету. Это выборка UI, а не аудит всего приложения.

| Аспект | Наблюдаемое состояние |
|---|---|
| Палитра | Light/night наследуют `Theme.Material3.DayNight.NoActionBar`; активных overrides `colorPrimary` нет. Новый экран не вводит цветовых токенов. Violet/light и lavender/dark в снимках — наблюдаемое Material rendering, не новая утверждённая палитра. |
| Типографика | Системная Roboto/sp наследуется без собственного `fontFamily`. Регистрация использует Material `TitleLarge`, `BodyLarge`, `BodyMedium`, `LabelLarge`; новые значения размера/веса не назначены. |
| Компоновка | Нативный toolbar минимум 56dp, вертикальная прокрутка, внутренний отступ 16dp, поля с интервалом 12dp, интервалы секций до 24dp. Insets учитывают system bars/cutout и максимум IME/bottom inset. |
| Компоненты | Material `TextInputLayout`/`TextInputEditText`, обычные theme-resolved Button, native Spinner с явным начальным выбором. Поля/кнопки/spinner имеют заданный минимум 48dp; cancel использует `borderlessButtonStyle`. |
| Форма и состояния | Shape, focus/error и elevation наследуются от компонентов; собственные радиусы/тени не добавлены. Read-only телефон — TextView; ошибки полей, loading, reload, empty list и server statuses присутствуют в коде. |

Это описание реализации, не новые нормативные правила. Существующие login и
main используют явные размеры 28/16/14sp и 26/15/16sp соответственно, тогда как
новая регистрация использует semantic text appearances. Разнородность
incumbent типографики отмечена без её исправления или превращения в новый
обязательный type ramp. Прежние Driver lint warnings также не канонизируются
как правила и не исправляются в рамках этого документирования.

## Исходные визуальные свидетельства и correction

Просмотрены все четыре **pre-fix** native API35 снимка 1080×2400 из успешного
CI `43736924677`, source `5cb6d60060c491e4d0768c90d1446beeacfd1e00`.
Каталог: `/workspace/artifacts/transfer-driver-registration-2026-11-15/.impeccable/review/`.
По журналу прошли PNG signature/IHDR/dimensions/chunk CRC/IEND; documenter
выполнил просмотр и чтение SHA256, проверки CI не перезапускал. Ниже только
исторические pre-fix hashes: файлы в каталоге уже заменены post-fix набором.
Исходный действительный pre-fix архив `native-evidence.zip` сохранён.

| Pre-fix файл | SHA256 просмотренного файла |
|---|---|
| `profile-light.png` | `225f0aa30572cd3a921843b01e201242dcd3865b79aa950bf83160deadf5ee08` |
| `profile-dark-large.png` | `64208d90a718de2307d0c5cca8474a7afc511c38c536dec99a833c5f0cc7c8b9` |
| `vehicle-light.png` | `a6c7b534159a17b1a2de900b2c92acd000e23230ca9d528b5d426d7f1fcc36ce` |
| `vehicle-dark-large.png` | `3a1f9e1e47967a4451b2c76610cc145faa5bafe5b9d1da70d1b358597db6781a` |

Light и dark/font1.3 показывают сохранение native Material компонентов и
типографики. В pre-fix profile заметен недостаточный контраст status bar;
в dark/font1.3 vehicle длинный hint скрывает диапазон 1–20. Эти дефекты
соответствуют disposition `fix` fresh reviewer, а не системным решениям.
Его третий пункт — фиксация утверждённых фактов в PRODUCT.md — выполнен без
добавления брендинга или продуктовых решений.

Проверенный diff UI correction задаёт appearance status/navigation foreground
через `WindowInsetsControllerCompat` по resolved DayNight; label сокращён до
«Пассажирских мест», диапазон «От 1 до 20» вынесен в постоянный Material helper.
Сам source diff подтверждает наличие исправления; его bounded визуальная
приёмка по новым снимкам зафиксирована отдельно в заключительном разделе.
Следующий fixture-only commit изменяет lifecycle/font-scale synchronization и
запрещает unmocked test network через test-only ProxySelector; production UI
между этими двумя commits не меняется.

Снимки создаёт androidTest с синтетическими данными/transport. Только test APK
временно снимает FLAG_SECURE и восстанавливает/проверяет его в finally;
production screenshot hook отсутствует. Первый ошибочный artifact с adb text
под расширением `.png` отвергнут; он не используется как визуальное evidence.

## Provenance сборок и границы подтверждения

Публикационная сверка родительского процесса установила равенство local/remote
trees; remote SHA не являются локальными refs этого checkout:

| Local → remote | Tree |
|---|---|
| `04c0aef` → `5cb6d60060c491e4d0768c90d1446beeacfd1e00` | `fae2d2d4f41923bdde0e7ebb2bb1a47ba85b8a7e` |
| `1244d26` → `5448f31c755843b1a98a2f0ae3b35aa5d6e12339` | `79f50e85a30ba5aa488cecba1594a635f98faace` |
| `c5d8ae4` → `505454a52a257a986391e8999dd416f5afb31eab` | `df81bfd0d2d6ef142d9de786fff6042c2b5e093e` |
| `49499fd` → `16cdc03e6e7b2830c88a9bda19ee370540d1dfb5` | `078a54b798d1911e6f15ac91891bc08afed84321` |

По переданному artifact receipt текущий Driver APK:
`/workspace/artifacts/transfer-driver-registration-2026-11-15/round1/TransferDriver-debug.apk`,
SHA256 `2940d9f67f47a27e8a8609f77a3835e16026f20f19688931beb0c2d3c11d4461`.
Production UI source — `5448f31c755843b1a98a2f0ae3b35aa5d6e12339`;
последующие sources меняют только tests/workflow/factual docs.
Admin отдельно: `/workspace/artifacts/transfer-driver-registration-2026-11-15/TransferAdmin-debug.apk`,
SHA256 `43bfbb4daad747a53d231a2ffb3518ad3a5fd8e125d3a3712a519cbef85348d5`,
source `5cb6d60060c491e4d0768c90d1446beeacfd1e00`; поздние corrections
Admin production code не меняют. Эти конкретные локальные APK bytes не объявляются
CI runtime-tested: CI самостоятельно собирает соответствующий source.

Исторический полный успешный baseline CI `43736924677`: 231 JVM tests
(Driver 43×3, Admin 34×3), 56 runtime (27/29), шесть lint suites;
Driver 0 errors / 54 прежних warnings / 0 новых registration issues,
Admin 0 issues. Это historical baseline до UI correction.
Первый post-fix CI `43738660853` завершился failure: dark fixture не дождался
safe UI state за исходные 8s до новых appearance/helper assertions.
Post-fix runtime/captures этим запуском не подтверждены. Fixture synchronization
исправлена без удаления cases, ослабления assertions или увеличения 8s deadlines.

Следующий CI `43739771085` / job `132890126120`, source
`505454a52a257a986391e8999dd416f5afb31eab`: Driver 27 runtime PASS и четыре
PNG validation PASS, затем Admin `ModerationUiInstrumentationTest` line61
завершился `RootViewWithoutFocusException` на dialog. Общий CI — failure;
upload после позднего failure был пропущен, post-fix image bytes не получены.
Test-only исправление ожидает фактический parent window focus по событию и
выбирает focused dialog; Espresso click, предел 10s и все assertions сохранены.
Workflow теперь допускает сохранение четырёх PNG после позднего failure только
при наличии `.validated` marker успешного validator и без cancellation.
Это не меняет failure status или критерии успешности tests.

## Окончательные свидетельства и ограниченная приёмка

Окончательный CI `43742661189` / job `132899016826`, source
`16cdc03e6e7b2830c88a9bda19ee370540d1dfb5`: **SUCCESS**. По переданным итогам
проверки выполнены 231 JVM tests (Driver 43×3, Admin 34×3), 56 runtime
(27/29), шесть full lint suites, security/endpoints/signing checks.
Driver: 0 errors / 54 прежних lint warnings / 0 новых registration issues;
Admin: 0 issues. Новых dependencies или изменений theme identity нет.

Получен artifact `14173720607`, 525678 bytes:
`/workspace/artifacts/transfer-driver-registration-2026-11-15/native-evidence-round1.zip`,
SHA256 `72c114fe0aadf374745b7acbc8d65cd5d6bf08bb13dea36cf7e3b8dc04399077`.
Четыре post-fix PNG API35 1080×2400 прошли signature/IHDR/native dimensions/
chunk CRC/IEND validation. Documenter просмотрел все четыре обновлённых файла
по прежнему каталогу `.impeccable/review/` и непосредственно сверил SHA256
файлов и архива; это другой набор bytes, чем исторические снимки выше.

| Post-fix файл | SHA256 просмотренного файла |
|---|---|
| `profile-light.png` | `e5b4fa71ed44cfdfd5543656146f5f65843ac77fc1983c55c533ca0a72d46329` |
| `profile-dark-large.png` | `f4574e6da0d6d6f81a43f8fd95d5b64de5d327977290cb7884f8cf9359fb5f83` |
| `vehicle-light.png` | `428b64d99b5efbf7c4d261b6abd179f59f06953aca658fc941a009787f12f044` |
| `vehicle-dark-large.png` | `c10c37ba73d24adaacbbba8786a32f0eef680bae11394832f7332c9f2380bbfd` |

Новые снимки сохраняют прежние компоненты, цвета и типографику. В light
foreground status bar тёмный, в dark — светлый. В обеих vehicle captures
полностью видны «Пассажирских мест» и постоянный helper «От 1 до 20», включая
font1.3. Эта сверка documenter подтверждает соответствие текста новым файлам,
не подменяя scoring reviewer.

Тот же независимый fresh reviewer оценил повторное evidence и вернул:

| Проверенный пункт | Verdict reviewer |
|---|---|
| Контраст system bars во всех четырёх captures | RESOLVED |
| Полный capacity label и постоянный helper 1–20 в обеих vehicle captures, включая font1.3 | RESOLVED |
| Factual PRODUCT.md без новых решений | RESOLVED |
| Confirmation evidence, сохранение production FLAG_SECURE | RESOLVED |

Reviewer не наблюдал регрессий от этого batch и вернул точный disposition
`ship`, ограниченный перечисленными исправлениями. Этот verdict не означает
полной приёмки G2, live/physical-device проверки или разрешения merge/deploy.

Tablet, physical-device и live Driver E2E evidence отсутствуют. Настоящие
credentials, SDK configs, signing files и посторонние пользовательские artifacts
в этом pass не читались. Builds/tests этим documenter не запускались.
Merge и production deploy не выполнялись и требуют отдельного решения владельца.
