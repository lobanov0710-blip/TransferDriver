# G2 Transfer Admin — ручное назначение

Утверждённые D1–D3, ТЗ §§18/33, выбранный владельцем вариант A и backend PR#30
определяют следующий срез. Это обычное расширение native Java/XML Material3
DayNight Admin с системной типографикой; новые стиль/dependencies не вводятся.

Главный экран показывает «Назначения» только по server orders.read, вне
owner_phone scope. Private Activity не exported. Постраничный список без
самостоятельного ввода ID → конкретный заказ → связка driver+vehicle с
серверными блокировками → обязательная причина1–500 → защищённое подтверждение
человеком. Новый заказ использует assign, taken с assignment — reassign;
in_progress не получает исключений из действующего запрета. Первая строка
автоматически не выбирается. Полный opaque orderId из read DTO различает
одинаковые маршруты в списке/выборе/подтверждении; offline сам по себе не запрещён, D6 ranking нет.

Reads используют только вариант A: минимальные orders/options, cursor по20
строк, явные переходы вперёд/назад, фильтры состояний. Order/options revision
должны совпадать. Строгий immutable DTO отвергает лишние/приватные поля,
недопустимые типы/revisions/IDs/duplicate rows/unsafe cursor и противоречивый
eligible. Неизвестный blocker не отображает raw provider text и не разрешает
выбор. Телефоны/документы/VIN/биометрия/private URLs не запрашиваются.

Перед каждым GET/POST заново читается server me/permissions/MFA. Отдельный
клиент связан с исходными account/access/refresh и scope; guard проверяет
привязку до dispatch и после response. Ни automatic refresh, ни POST replay
нет. Reason/revision/body/key исходной операции неизменяемы. ACK проверяет
целевые IDs и orderRevision; текущее состояние затем читается снова.

При неопределённом результате исходный intent остаётся только в foreground
RAM, новые назначения закрыты. Явное обновление списка не отправляет POST;
повтор требует подтверждения и сохраняет первоначальные body/key. Исходный
выбор подписан как первоначальный, текущий status показывается только по
новому чтению. При401/403/409 controls/snapshots закрываются до обновления.
Подтверждённый ACK с последующим GET failure не повторяется как mutation.

onStop отменяет работу, invalidates scope и очищает данные/причину/диалог/
uncertain intent; при возвращении выполняется новый GET без POST replay.
Смена аккаунта/повторный вход не может превратить старое действие в запрос
нового аккаунта. Stale401 не удаляет следующую сессию. Ничего из operational
state не помещается в Bundle/Preferences/disk caches/clipboard/logs.
FLAG_SECURE, noExcludeDescendants Autofill, no saved input, no personalized
IME learning, защищённые positive-dialog touches, system bars/IME insets.

Проверки: pure JVM контракт/transport, actual Keystore + synthetic HTTP и
реальные ActivityScenario/Espresso lifecycle/IME/confirmation/assign/reassign/
uncertainty/paging/permissions/session tests на API35. Native captures:
orders/options/form/confirmation/uncertain в light и dark/font1.3.
Это Activity-level synthetic evidence, не live backend/hardware acceptance.
Capture временно clear/restore FLAG_SECURE только в test APK; сохраняются
неизменённые устойчивые native framebuffers, PNG signatures/CRC/digests проверяются.

CI проверяет оба приложения/варианты и старые security regressions. Свежие
finish reviewer и documenter по Impeccable §7 работают по реальным captures;
результаты и границы сохранены в [UI record](ADMIN_ASSIGNMENT_UI_REVIEW_2026-11-16.md)
и [verification receipt](ADMIN_ASSIGNMENT_UI_VERIFICATION_2026-11-16.json).
Launcher/generic-role fallback раскрыт; detector для native не применяется.

G1paused, D4–D7pending. Backend и ADMIN_PHONE_TEST_ONLY whitelist не меняются;
новые operational routes на текущем OWNER-only STAGING остаются закрыты.
Merge/deploy и расширение телефонного STAGING scope требуют отдельного решения.
