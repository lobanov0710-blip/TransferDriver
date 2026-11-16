# G2 Driver availability — native UI evidence, 2026-11-16

## Статус записи

**Итоговая запись по ограниченному contrast review.** CI `43779524059`, job
`133013316916`, source `e3c6f0c3bde6ac77c5527e4e69c7f65fecd07e69`, tree
`9425fc9174e786c7c2055158d52288a8c501b1d2` — SUCCESS по переданному результату.
Четыре replacement PNG второй коррекции повторно открыты documenter; их
SHA256 и размеры проверены локально. Все status icons и navigation handles
читаемы, включая Wi-Fi/notification в dark-offline.

Тот же независимый reviewer во втором scoring закрыл единственное замечание:
`RESOLVED`, точный disposition `ship`. Это verdict только для scored
system-bar contrast fix на представленном emulator/component-host evidence.
Он не означает приёмку всего Main, live/hardware проверку, security clearance
или разрешение production release, merge/deploy. Ранее записанные RUNNING и
PENDING относились к ожиданию второго результата и теперь являются историей.

Это отдельный свежий documenter-pass после корректировки. Harness не имеет
специализированного agent-type selector: использованы отдельные generic fresh
reviewer/documenter с инструкциями соответствующих ролей. Ранее раскрытый
fallback контекста: launcher был недоступен. Native XML/Java и actual native
captures являются основанием этой записи; browser rendering и web detector к
native Android не применялись.

## Утверждённая граница

Обычное локальное расширение существующего Android Java/XML Material3
DayNight, system Roboto и размеров текста в sp. Новый визуальный мир, palette,
шрифты, dependencies, comp/seed/qualitybar, DESIGN.md или sidecar не требуются
и этим pass не создаются. Инкумбентная система сохраняется.

Основание: AGENTS.md, PRODUCT.md и
[scope](DRIVER_AVAILABILITY_SCOPE_2026-11-15.md). Утверждены G2 Driver
registration → availability/manual assignment и D1–D3. G1 приостановлен;
D4–D7 не утверждены. Предложенный Admin manual-assignment read API version2
с вариантами A/B ожидает явного решения владельца; его реализация сюда не
входит. Новые backend endpoints, schema, deploy и merge не выполнялись.

Нужный путь: offline → выбор собственного approved автомобиля → точный slider
«Сдвиньте, чтобы выйти на линию» → server ack и authoritative GET → компактный
«Радар активен» с явным «Выйти с линии». Жест не устанавливает online
оптимистично. Offline сохраняет activeVehicleId; выбор автомобиля доступен
offline. Нет автоматического replay/refresh мутации. При отсутствии revision,
сбое, 401/409 или неопределённом результате controls закрываются до нового GET.
Сервер определяет допуск, verified и состояние; клиент их не выдумывает.

## Проверенные источники и фактическое устройство UI

Прочитаны:

- `AGENTS.md`, `PRODUCT.md`, `docs/DRIVER_AVAILABILITY_SCOPE_2026-11-15.md`;
- актуальные последние 80 строк `docs/PROJECT_JOURNAL.md` перед финализацией;
- `app/src/main/res/layout/view_driver_availability.xml` и
  `activity_driver_main.xml`;
- `app/src/main/java/ru/transferservis/driver/ui/main/MainWindow.java` и
  `AvailabilityPanel.java`;
- `app/src/androidTest/java/ru/transferservis/driver/data/registration/AvailabilityInstrumentationTest.java`;
- `app/src/main/res/values/themes.xml` и `values-night/themes.xml`;
- Impeccable `reference/document.md`, `reference/degraded/documenter.md` и
  `reference/android.md`.

Availability include стоит после registration/tools/logout, а взвешенная
область заказов сохраняет оставшееся пространство. Offline footer использует
native Spinner и Material Slider с минимумом 48dp; online footer — одну строку
radar с явной offline-кнопкой. Label/control связи, live-region сообщения и
единая функция перехода для touch и keyboard/TalkBack SET_PROGRESS видны в
исходниках. Это чтение кода, а не полная проверка TalkBack пользователем.

`AvailabilityPanel` скрывает controls во время запроса, после ack делает GET и
только затем render. `onStop` очищает snapshot/список/adapter/slider и отменяет
scope; callbacks проверяют generation и текущую session. Root использует
`importantForAutofill="noExcludeDescendants"`: профилактический fix, не ответ
на наблюдавшуюся exfiltration. В synthetic test проверяются очистка hierarchy
и этот атрибут; наблюдавшаяся утечка не заявляется.

`MainWindow` применяет systemBars/displayCutout/IME insets. Первая коррекция
задаёт native system-bar appearance на API30+, совместимый путь на API26–29.
Тема остаётся `Theme.Material3.DayNight.NoActionBar`. В light/dark theme файлах
нет новой palette. Availability использует Material textAppearance roles;
индикаторы/акценты наследуются от темы. Существующий main XML сохраняет
26sp bold title, 15sp status и 16sp empty-state. Эти incumbent значения не
объявляются новой универсальной type scale.

## Просмотренные native captures

Лично повторно просмотрены четыре replacement файла второй коррекции в
`.impeccable/review/`; каждый имеет проверенные размеры 1080×2400:

| PNG | Профиль evidence | Наблюдение |
| --- | --- | --- |
| `availability-offline-light.png` | API35 phone, 1080×2400, light, font 1.0 | Offline message, selected synthetic car и полный slider видимы; footer ниже logout; light bars читаемы. |
| `availability-online-light.png` | тот же light профиль | Компактные «Радар активен» / «Выйти с линии», увеличено место заказов; light bars читаемы. |
| `availability-offline-dark-large.png` | API35 phone, 1080×2400, dark, font 1.3 | Footer и slider видимы без наблюдаемого обрезания; Wi-Fi и notification icon теперь белые и читаемы, navigation handle читаем. |
| `availability-online-dark-large.png` | тот же dark/font 1.3 профиль | Radar/offline action видимы; соответствующие status icons белые. |

Локально вычисленные SHA256 совпали с переданными значениями:

| PNG | SHA256 |
| --- | --- |
| `availability-offline-light.png` | `57c5f1ce77b2bf89200e4a6e49e669cc24904bf8fd0ba707682e9b055ec0232d` |
| `availability-online-light.png` | `0e69dea24cd52ff9e3c4aa883fb514f723f56aabc7e6e7dff501853a361e7444` |
| `availability-offline-dark-large.png` | `6246d5a9bd0b13fcb4e55391d976c0107dc8b553894b0ad06bf21c75f047b55c` |
| `availability-online-dark-large.png` | `97a2edac53ae19561d1a07bf342affc6a72f129f58a3f30f7f5c18c3f3255e21` |

В этих четырёх captures не наблюдается новой batch regression; это также
совпадает с переданным заключением reviewer. Сравнение PNG не доказывает
точную причину прежнего частичного SystemUI tint.

Состояния и размеры профиля переданы исполнителем CI; сами PNG просмотрены
documenter. Это emulator/component-layout evidence: настоящий main XML и
`AvailabilityPanel` встроены в secure `RegistrationActivity` synthetic test
host. Тест вручную показывает main controls, tabs и synthetic empty-order
text; он не проходит реальный Main auth/order routing. Сеть mock, данные
synthetic, немокированная сеть запрещена test-only ProxySelector.
FLAG_SECURE временно снимается только для synthetic capture и возвращается
в finally. Live Driver E2E, hardware, производительность/жесты на физическом
устройстве и tablet layout не проверены. API26–29 путь прочитан в коде, его
runtime evidence здесь нет.

## Коррекции и provenance проверок

Следующие CI-результаты, artifact metadata и reviewer verdict переданы
родительским исполнителем; documenter не запускал builds/tests и не проверял
remote CI самостоятельно. Прочитанный journal tail подтверждает исходный PASS,
первый PARTIAL и обоснование второй синхронизации; итоговый CI/verdict получен
от исполнителя отдельно. Личная проверка documenter — код, PNG, их SHA256 и
размеры.

| Этап | Переданное evidence | Статус |
| --- | --- | --- |
| Исходная проверка | CI `43776899566`, source `12fd1c2`; 246 JVM + 67 runtime, первые 8 валидных PNG | SUCCESS CI; свежий независимый reviewer вернул пять секций, disposition `fix`, одно finding: ненадёжный исходный контраст system bars. |
| Первая коррекция | CI `43778224200`, job `133009304957`, source `800065ead78621e62b10a0ad78e083a4635d8a00`; 246 JVM + 67 runtime и 6 lint/build/security guards | SUCCESS CI; тот же reviewer, round 1: PARTIAL, disposition `fix`. Light status/dark navigation исправлены, dark-offline Wi-Fi/notification ещё чёрные. Это совпадает с лично просмотренными PNG. |
| Вторая коррекция | source `e3c6f0c3bde6ac77c5527e4e69c7f65fecd07e69`, tree `9425fc9174e786c7c2055158d52288a8c501b1d2`, CI `43779524059`, job `133013316916` | SUCCESS: 246 JVM (Driver 48×3, Admin 34×3), 67 runtime (Driver 38, Admin 29), 0 failures/skipped; 6 lint/builds, security 7+5, endpoints/AppCheck/signing/UID PASS. Same reviewer second scoring: contrast RESOLVED, disposition `ship` только в ограниченной evidence scope. |

Вторая коррекция меняет только screenshot synchronization и journal: poll
реальных неизменённых native frames каждые 200ms, требование 1s одинаковых
frames, предел 8s. Код `stableScreenshot()` прочитан. Production app sources
остаются как на `800065e` по переданному контексту. Новая синхронизация не
перекрашивает evidence и сохраняет устойчиво неверный цвет для review.

Исторический архив первой коррекции:
`/workspace/artifacts/transfer-driver-availability-2026-11-15/native-evidence-final.zip`,
artifact `14187751524`, переданный SHA256
`d02ea094bb90c75f914b485f715c76edf72458822ad8e9a4770bf41e8905ddf3`.
Финальный архив второй коррекции:
`/workspace/artifacts/transfer-driver-availability-2026-11-15/native-evidence-round2.zip`,
artifact `14188321054`, 882728 bytes, переданный SHA256
`921c49e9470bd5e16faec27130e6d996366f765482f6c873db5c601c839deb0f`.
Переданный eight-PNG validator: PASS. Scope описывает передачу только
8 фиксированных PNG с проверкой signature/IHDR/CRC/IEND; собственный просмотр
documenter ограничен четырьмя availability replacement PNG выше. Archive digest
и validator не пересчитывались documenter. Старые пути в `.impeccable/review/`
теперь содержат round2 captures; round1 сохранён в отдельном архиве.

54 historical Driver lint warnings относятся к прежнему local lint, а не к
текущему CI warning count. Нулевой текущий warning count здесь не заявляется.

## Пять строк incumbent system и граница результата

Palette: inherited Material3 DayNight surface/primary/text roles, без новой palette.

Type: system Roboto, Material textAppearance roles в footer, incumbent main sp values.

Layout rule: availability после tools/logout, orders получают оставшееся место.

State rule: radar только после authoritative GET; ошибки не имитируют online.

Platform rule: native controls, минимум 48dp у новых controls, system/IME insets.

Не canonized: чёрные status icons в dark-offline первой коррекции были defect,
а не правилом системы; на round2 evidence замечание закрыто reviewer и лично
видно устранение контраста. Documenter не менял app code или design system.
Новый universal visual rule из прежнего дефекта не создаётся. Ограниченный
reviewer disposition `ship` не даёт production release/merge/deploy approval
и не означает полную приёмку G2; live Main auth/orders, hardware/tablet и
неутверждённые продуктовые решения остаются за границей этой записи.
