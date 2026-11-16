# G2 Driver availability — текущий срез

Утверждённые основания: ТЗ §§16–18, D1–D3, журнал и принятая последовательность
registration/vehicles → availability/manual assignment. G1 на паузе; новый
Admin read API только предложен в отдельном version2 документе, не реализован.

GET me: собственные profile.registrationStatus, identityStatus,
availability.state/activeVehicleId/revision и собственные vehicles.
PUT /v1/driver/availability: activeVehicleId, requestedState, expectedRevision;
Idempotency-Key=availability-UUID. State не устанавливается клиентом после
жеста: после ack выполняется authoritative GET, только тогда показан Радар.
Off_line сохраняет текущий ID, выбор другого авто только offline; backend
атомарно проверяет статус/identity/фотоконтроль/ownership/занятость/revision.
Нет null/неизвестной revision — нет догадки revision1. Нет авто/допуска —
отображается причина по данным сервера, verified/approved клиент не создаёт.

Операции bound к staff/access/refresh/epoch, no auto refresh/replay, no redirects,
HTTPS allowlist, bounded responses, one-shot bodies. На401/409/неопределённый
результат controls закрываются до GET, новый key автоматически не создаётся.
OnStop очищает snapshot/выбор, invalidates/cancels. Main onReady не отправляет
запрос, если lifecycle ещё не STARTED; initial auth result deferred до onStart.

Native Java/XML Material3 DayNight/system typography, без новых libraries/
палитры/редизайна. Footer после main controls внизу, offline native slider +
выбор approved авто, online компактный radar/offline control. Slider keyboard/
TalkBack и swipe используют одну функцию server request. Edge-to-edge insets
и контраст bars соответствуют текущей теме. Нормальное обновление — при возврате
и main pull-refresh; отдельный refresh доступен для error/missing states.

Проверки:5 новых JVM boundary cases;11 native cases на реальных Keystore и
mock HTTP, full main XML/panel в secure test host. Main auth/order routing
не подменяется как проверенный live flow: это component/layout evidence.
Ошибки: допуск/identity/photo/active assignment/revision/session replacement,
ack loss, late401, background, Parcel, one-shot401/503, реальный swipe,
TalkBack setProgress, offline/online light +dark/font1.3 screenshots.
Test-only ProxySelector запрещает немокированную сеть. Только8 фиксированных
PNG передаются в CI evidence, signature/IHDR/CRC/IEND проверяются до marker.
FLAG_SECURE меняется только в synthetic test capture и восстанавливается.

Реальные документы/биометрия/новый identity provider не добавляются. D4–D7
не утверждены, OWNER-only STAGING flag не меняется. Live Driver E2E/physical
device/production release не заявляются; merge/deploy отдельно.
