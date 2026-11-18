# Android Admin: проверка параметров и OWNER-исключения

Продолжение утверждённого classification контракта после readonly PR17 и backend PR43. Отдельная ветка arch/g2-admin-classification-review-form. Это реализация согласованного API, без новых бизнес-решений.

Неэкспортируемая форма открывается для выбранного автомобиля из существующей регистрации. Вводятся фактические марка/модель/год, подтверждённые исполнение/кузов, отдельно выбираются обе verified стороны СТС этого автомобиля. Класс вычисляет сервер. Чтение содержимого СТС требует прежних sensitive прав, отдельно от vehicles.verify. Нет client trust flags/class/actor в POST. Цвет сохраняется прежней отдельной обязательной операцией; форма классификации его не подменяет.

Обычная проверка сохраняет target pin; изменение make/model/year загружает текущую точную модель. Неизвестная модель остаётся manual review. OWNER с actual server MFA может выбрать отдельную модель-основание той же марки, явно разрешить недостаточный год и конкретные unmet условия. Dispatcher не получает новых прав даже при ошибочном grant. Исходный target pin и basis rule version передаются раздельно. Условия без решения остаются unverified. Любое изменение параметров после загрузки требует явно загрузить правило заново. BEV/body restrictions проверяет backend, исключение их не обходит.

Перед POST показывается подтверждение исходных параметров, СТС/revisions, решений/waivers и причины. Snapshot и idempotency key неизменны. Нет автоматического POST/refresh/перечитывания после write. Потерянный/некорректный ACK, IO/408/5xx оставляют только исходный action в foreground RAM: отдельное явное чтение состояния не меняет его, повтор требует нового подтверждения и fresh access/MFA. Backend CAS/replay остаётся окончательным guard. Успех не равен approved/online; server flag вызывает notice о безопасной остановке и ручной замене диспетчером. Нет автоматического назначения.

FLAG_SECURE, no save/autofill/content capture, opaque route extras удаляются. onStop отменяет операции и стирает inputs/reason/evidence/conditions/preview/dialog/unknown action. Декодированные картинки ограничены существующим decoder, bytes стираются, bitmap освобождается. MFA timer стирает форму без network. Нет persistence/telemetry новых данных, refresh или очистки новой чужой сессии из-за позднего ответа.

Проверки: 11 новых JVM cases, 8 repository + 8 UI native cases (включая 10 native PNG light/dark/font1.3); предыдущие обязательные gates не ослаблены. Локальные security8/5, XML/diff PASS. Полные JVM/build/lint/native и provenance ожидают CI; не считать их выполненными до receipt.

Следующий отдельный шаг: OWNER rule authoring UI. G1paused, D4–D7pending; full powertrain121/legacy inventory/live phone acceptance открыты. Нет phone allowlist расширения, реальных документов, seed/backfill/remote migration/merge/deploy. Debug CI APK с ephemeral подписью не production release.
