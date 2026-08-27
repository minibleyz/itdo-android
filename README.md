# itdo — нативный Android-клиент (Kotlin + Jetpack Compose, Material3)

Каркас Android-приложения под существующий PHP-бэкенд соцсети (папка `api/`
из веб-версии, https://itdo.bleyzos.ru). Дизайн намеренно минимальный —
используются стандартные компоненты `androidx.compose.material3:material3`.

## ⚠️ Один файл нужно переместить вручную

GitHub App, которым залит этот код, не имеет прав на запись в
`.github/workflows/`, поэтому workflow-файл лежит в корне репозитория как
`workflow-build.yml`. Чтобы автосборка APK заработала:

1. В репозитории на GitHub создайте файл `.github/workflows/build.yml`
2. Скопируйте туда содержимое `workflow-build.yml`
3. Удалите `workflow-build.yml` из корня (необязательно, но для порядка)

Сделать это можно прямо в вебе GitHub: `Add file → Create new file`,
путь `.github/workflows/build.yml`, вставить содержимое.

## Что уже сделано и реально работает

- Авторизация (`auth/login.php`, `auth/register.php`, `auth/me.php`, Bearer-токен)
- Хранение токена в DataStore, автоподстановка `Authorization: Bearer` во все запросы
- Лента постов: чтение (`feed/get.php`), лайк/дизлайк, публикация поста
- Профиль пользователя + выход
- Список диалогов и переписка (`messages/conversations.php`, `messages/get.php`, `messages/send.php`)
- Pixel Battle: просмотр поля и установка пикселя (`pixelbattle/board.php`, `place.php`)
- Заглушка админ-раздела со списком реальных админ-эндпоинтов (видна только `is_admin` пользователям)

## Что нужно доделать перед продакшеном

1. **hCaptcha.** Бэкенд требует `h-captcha-response` при каждом логине по
   паролю (см. `api/auth/login.php`). В `LoginScreen` сейчас поле для токена
   вводится вручную — нужно встроить hCaptcha-виджет (WebView с JS-мостом)
   и подставлять токен автоматически. Задайте `HCAPTCHA_SITE_KEY` в
   `app/build.gradle.kts`.
2. **`API_BASE_URL`** в `app/build.gradle.kts` уже указывает на
   `https://itdo.bleyzos.ru/api/` — поменяйте, если нужно.
3. **Сверка полей JSON.** Модели в `data/model/Models.kt` — рабочее
   предположение по структуре ответов PHP. Реальные ключи могут отличаться
   (регистр, вложенность) — сверьте по факту через curl/Postman к своему
   бэкенду и поправьте `@SerialName`.
4. **Realtime.** В вебе используется `api/ws/*` (WebSocket) для тайпинга,
   live-обновлений ленты, звонков и pixel battle. В этой версии — обычный
   polling. Для полноценного паритета нужен WebSocket-клиент (OkHttp WS)
   плюс, отдельно, WebRTC-стек (`api/calls/*`, `infra/coturn/`) для звонков —
   это отдельный большой блок работы (сигналинг + `org.webrtc` библиотека).
5. **Админка.** В `api/admin/` больше 20 эндпоинтов (логи, баны по IP/устройству,
   монеты, автомодерация, рассылки и т.д.) — сейчас список только показан,
   экраны под каждый раздел нужно добавлять по мере необходимости.
6. **Медиа-загрузка** (`upload_media.php`, `upload_voice.php`, `upload_music.php`)
   не реализована — нужен `MultipartBody` + выбор файла из галереи/камеры.
7. **Push-уведомления** (`api/notifications/`) — не реализовано, нужен FCM
   либо polling `notifications/*`.

## Сборка

Локально (нужен Android Studio / Android SDK):
```
gradle assembleDebug
```
(В репозитории нет `gradlew`/gradle-wrapper — при первом открытии в Android
Studio студия сама предложит сгенерировать wrapper, либо используйте
глобально установленный Gradle.)

### Или без своей машины — через GitHub Actions

После того как вы переместите `workflow-build.yml` в `.github/workflows/build.yml`
(см. раздел выше), при каждом пуше в `main` будет запускаться сборка debug APK
на серверах GitHub. Готовый файл можно скачать во вкладке **Actions** →
выбранный запуск → **Artifacts** → `itdo-debug-apk`.

Запустить сборку вручную: **Actions → Build APK → Run workflow**.
