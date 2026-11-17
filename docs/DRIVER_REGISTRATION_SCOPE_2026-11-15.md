# G2: анкета и автомобили Driver — границы первого среза

Основание: утверждённое ТЗ §§4, 9, 10, 31–32, D1–D3 и разрешённая
последовательность Admin moderation → Driver registration/vehicles →
availability/manual assignment. OWNER подтвердил новый TOTP и вход на телефоне.
G1 приостановлен. D4–D7 не утверждены. Документ конкретизирует выполненный
срез, не изменяет утверждённые требования и не означает полной приёмки G2.

## Первоначальный API (исторический checkpoint 2026-11-15)

Контракт записи ниже заменён срезом системного класса 2026-11-17; см. актуальный
раздел в конце документа. Старые CI и screenshots относятся к своему source.

| Операция | Контракт |
|---|---|
| Получить анкету/автомобили | GET /v1/driver/me |
| Личные данные | PUT /v1/driver/registration: fullName, birthDate ISO, region, expectedRevision |
| Создать автомобиль | POST /v1/driver/vehicles: VIN, plate, make, model, optional color, vehicleClass, capacity, baggageCapacity, usageBasis |
| Изменить автомобиль | PATCH /v1/driver/vehicles/{id}: те же поля + expectedRevision |

Телефон только из подтверждённого server profile, не редактируется и не входит
в mutation. Только draft/changes_required позволяют редактировать личные данные;
suspended/rejected автомобили — просмотр. VIN обязателен, 17 латинских букв/цифр
без I/O/Q. Сервер принимает окончательные решения о доступе и статусе. Каждая
машина сохраняется и проверяется отдельно. Старое nullable usageBasis допускается
при чтении; при сохранении нужно выбрать действующее значение.

GET me не содержит причин возврата документов/элементов: UI показывает только
доступные server statuses, не создаёт причины из предположений. Полный wizard,
реальные документы, identity и окончательная подача требуют следующих срезов/
решений; кнопка финального submit в этом срезе не добавляется.

## Интерфейс и безопасность

Расширение существующего native Android Java/XML + Material3 DayNight, Roboto/sp,
действующая тема; без новых библиотек, брендинга, шрифтов и стилистических решений.
Нативные persistent labels, поля с ошибками, spinner с явным выбором, read-only
телефон, загрузка/пустой список/ошибки/конфликт, insets/IME/back, цели от 48dp.
Неизвестные статусы, чужой профиль/машина, некорректные revisions отклоняются.

Отдельный минимальный transport использует существующую SecureHttpClient policy:
HTTPS/origin allowlist, no redirects/retries/cache/logging, bounded responses.
Сохранение не вызывает автоматический refresh/replay после 401 или обрыва.
Запрос привязан к исходным staffId/access/refresh и lifecycle epoch. При смене
сессии или отмене старый запрос/ответ не может стать действием другого аккаунта.
После неопределённого результата формы закрываются до явного reload; серверный
список нужно проверить до повторного создания. Server revision конфликт также
требует reload. После успешной записи выполняется GET authoritative state.

FLAG_SECURE/no-backup/non-exported сохраняются. Поля не входят в SavedState/
autofill/preferences; onStop очищает ввод и данные, отменяет/инвалидирует операции.
Пользователь заранее видит, что несохранённый ввод очищается при уходе. Уже
отправленная запись могла завершиться на сервере; следующий вход делает GET.

## Проверки и ограничения

JVM: границы profile/vehicles, чужие/дублирующиеся записи, revisions/statuses,
nullable legacy usage, VIN, строгий календарь/формат даты/контрольные символы.
Android instrumentation: реальный Keystore + mock transport; одиночные POST/PUT/
PATCH, 401/обрыв/409/redirect/oversized response, смена аккаунта/relogin/запоздалый
ответ/отменённая очередь, полный UI save profile/create+edit vehicle, очистка
при background и отсутствие несохранённого имени в Parcel SavedState.

Нативные screenshots создаёт только androidTest APK с synthetic account и
interceptor, без действительной live session. Для снимка test временно снимает
FLAG_SECURE и в finally восстанавливает/проверяет его; production/debug app не
содержит этой возможности. CI извлекает только четыре заранее названных PNG
fixtures (light и dark/font 1.3), не app storage целиком. Upload action pinned
к проверенному официальному v4.6.2 commit.

Driver live STAGING не проверяется: ADMIN_PHONE_TEST_ONLY остаётся включённым,
Driver routes изолированы по предыдущему решению. Синтетический Firebase CI
config пригоден только для тестов, такой APK не выдаётся за рабочий live build.
Релизная подпись, Play/App Check/R2 и остальные G1 пункты остаются отложенными.

После аудита HTTP503 shared clients обеих apps помечают mutation bodies
isOneShot=true: actual loopback HTTP тест воспроизвёл старый повтор и доказал
одиночную отправку после исправления. No-retry flag сам по себе для 503
Retry-After:0 недостаточен. Визуальное evidence принимается только после
проверки реального PNG; target APK остаётся установлен только на ephemeral
CI emulator до извлечения четырёх synthetic fixtures.

## Актуальный зависимый срез 2026-11-17

Driver PR14 согласован с backend PR35 поверх schema0020 из PR34. POST передаёт
modelYear (целое число), vehicleClass отсутствует. PATCH передаёт те же поля
и expectedRevision. Класс только читается с сервера; null означает ожидание
системного результата после проверки. Старый отсутствующий год не выдумывается;
для сохранения формы его нужно указать. Год подтверждает Admin по документам.

Смена марки/модели/года сбрасывает класс/допуск на сервере. ACK с
requiresDispatcherReassignment показывает безопасную остановку, передачу
местоположения диспетчеру и ручной выбор замены. Потерянный ACK/ошибка чтения
после такого ACK не вызывают повтор mutation и не отменяют указание остановки.
Production FLAG_SECURE сохранён; два новых PNG снимаются только test fixture.

Trusted moderation/классификация текущим правилом, полный in_progress handoff,
пассажирское уведомление и legacy admission guard не завершены этим срезом.
Итоговые CI/PNG/APK/source hashes — в DRIVER_SYSTEM_CLASS_VERIFICATION_2026-11-17.json.
