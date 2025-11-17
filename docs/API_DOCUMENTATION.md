# API Документация MockController

## Базовый URL

```
http://localhost:8080
```

## REST API Эндпоинты

### 1. Проверка обновлений конфигурации

Проверяет, требуется ли заглушке обновление конфигурации.

**Эндпоинт:** `POST /api/configs/checkUpdate`

**Описание:**
- Заглушка отправляет свой текущий конфиг и версию
- Сервис сравнивает версию и конфиг с сохраненными
- Если конфига нет и версия 1 - сохраняется автоматически
- Если версия 1, но конфиг отличается от стартового - обновляется стартовый и версия повышается
- Если версия заглушки ниже сохраненной - возвращается `needUpdate: true`

**Запрос:**

```json
{
  "SystemName": "auth-mock",
  "version": "v1",
  "config": {
    "delays": {
      "loginDelayMs": "1000",
      "tokenDelayMs": "500"
    },
    "stringParams": {
      "mode": "normal",
      "environment": "test"
    },
    "loggingLv": "ERROR"
  }
}
```

**Поля запроса:**
- `SystemName` (string, обязательное) - название заглушки
- `version` (string, обязательное) - текущая версия конфига заглушки (например, "v1", "1", "v2")
- `config` (object, обязательное) - текущий конфиг заглушки

**Ответ:**

```json
{
  "needUpdate": false,
  "currentVersion": "v1"
}
```

**Поля ответа:**
- `needUpdate` (boolean) - требуется ли обновление
  - `false` - обновление не требуется
  - `true` - требуется обновление (версия заглушки ниже сохраненной)
- `currentVersion` (string) - текущая версия конфига в сервисе

**Примеры ответов:**

1. **Первая регистрация (конфига нет, версия 1):**
```json
{
  "needUpdate": false,
  "currentVersion": "v1"
}
```
Конфиг автоматически сохраняется как стартовый.

2. **Конфиг совпадает, версия 1:**
```json
{
  "needUpdate": false,
  "currentVersion": "v1"
}
```

3. **Конфиг изменился, версия 1:**
```json
{
  "needUpdate": false,
  "currentVersion": "v2"
```
Стартовый конфиг обновлен, версия повышена.

4. **Требуется обновление (версия заглушки ниже):**
```json
{
  "needUpdate": true,
  "currentVersion": "v3"
}
```

**Примеры запросов:**

**PowerShell:**
```powershell
$body = @{
    SystemName = "auth-mock"
    version = "v1"
    config = @{
        delays = @{
            loginDelayMs = "1000"
            tokenDelayMs = "500"
        }
        stringParams = @{
            mode = "normal"
            environment = "test"
        }
        loggingLv = "ERROR"
    }
} | ConvertTo-Json -Depth 10

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/configs/checkUpdate" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$response | ConvertTo-Json
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/configs/checkUpdate \
  -H "Content-Type: application/json" \
  -d '{
    "SystemName": "auth-mock",
    "version": "v1",
    "config": {
      "delays": {
        "loginDelayMs": "1000",
        "tokenDelayMs": "500"
      },
      "stringParams": {
        "mode": "normal",
        "environment": "test"
      },
      "loggingLv": "ERROR"
    }
  }'
```

**HTTP:**
```http
POST /api/configs/checkUpdate HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "SystemName": "auth-mock",
  "version": "v1",
  "config": {
    "delays": {
      "loginDelayMs": "1000",
      "tokenDelayMs": "500"
    },
    "stringParams": {
      "mode": "normal",
      "environment": "test"
    },
    "loggingLv": "ERROR"
  }
}
```

---

### 2. Загрузка конфигурации

Загружает конфигурацию от заглушки для синхронизации.

**Эндпоинт:** `POST /api/configs`

**Описание:**
- Заглушка отправляет свой конфиг
- Сервис сравнивает с сохраненным стартовым конфигом
- Если стартовый конфиг изменился - обновляется и версия повышается
- Если текущий конфиг отличается - возвращается статус `UPDATE_AVAILABLE`

**Запрос:**

```json
{
  "SystemName": "auth-mock",
  "config": {
    "delays": {
      "loginDelayMs": "1000",
      "tokenDelayMs": "500"
    },
    "stringParams": {
      "mode": "normal",
      "environment": "test"
    },
    "loggingLv": "ERROR"
  }
}
```

**Поля запроса:**
- `SystemName` (string, обязательное) - название заглушки
- `config` (object, обязательное) - конфиг заглушки

**Ответ:**

```json
{
  "status": "START_REGISTERED",
  "message": "Start config saved",
  "currentVersion": "v1"
}
```

**Поля ответа:**
- `status` (enum) - статус синхронизации:
  - `START_REGISTERED` - конфиг зарегистрирован впервые
  - `UPDATED_START_CONFIG` - стартовый конфиг обновлен
  - `UPDATE_AVAILABLE` - доступна новая версия конфига
  - `NO_CHANGES` - изменений нет
- `message` (string) - описание результата
- `currentVersion` (string) - текущая версия конфига в сервисе

**Примеры ответов:**

1. **Первая регистрация:**
```json
{
  "status": "START_REGISTERED",
  "message": "Start config saved",
  "currentVersion": "v1"
}
```

2. **Стартовый конфиг обновлен:**
```json
{
  "status": "UPDATED_START_CONFIG",
  "message": "Start config updated",
  "currentVersion": "v2"
}
```

3. **Доступна новая версия:**
```json
{
  "status": "UPDATE_AVAILABLE",
  "message": "New config version available",
  "currentVersion": "v3"
}
```

4. **Изменений нет:**
```json
{
  "status": "NO_CHANGES",
  "message": "No changes",
  "currentVersion": "v1"
}
```

**Примеры запросов:**

**PowerShell:**
```powershell
$body = @{
    SystemName = "auth-mock"
    config = @{
        delays = @{
            loginDelayMs = "1000"
            tokenDelayMs = "500"
        }
        stringParams = @{
            mode = "normal"
            environment = "test"
        }
        loggingLv = "ERROR"
    }
} | ConvertTo-Json -Depth 10

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/configs" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$response | ConvertTo-Json
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/configs \
  -H "Content-Type: application/json" \
  -d '{
    "SystemName": "auth-mock",
    "config": {
      "delays": {
        "loginDelayMs": "1000",
        "tokenDelayMs": "500"
      },
      "stringParams": {
        "mode": "normal",
        "environment": "test"
      },
      "loggingLv": "ERROR"
    }
  }'
```

---

### 3. Получение конфигурации

Получает конфигурацию заглушки по названию системы и опционально по версии.

**Эндпоинт:** `GET /api/configs/{systemName}`

**Параметры:**
- `systemName` (path, обязательное) - название заглушки
- `version` (query, необязательное) - версия конфига (например, "v1", "1", "v2"). Если не указана, возвращается текущая версия

**Описание:**
- Возвращает конфигурацию для указанной системы
- Если версия не указана - возвращается текущий конфиг
- Если указана текущая версия - возвращается текущий конфиг
- Если указана версия "v1" или "1" - возвращается стартовый конфиг
- Если указана несуществующая версия - возвращается текущий конфиг

**Ответ:**

```json
{
  "SystemName": "auth-mock",
  "version": "v2",
  "config": {
    "delays": {
      "loginDelayMs": "2000",
      "tokenDelayMs": "1000"
    },
    "stringParams": {
      "mode": "test",
      "environment": "production"
    },
    "loggingLv": "INFO"
  },
  "updatedAt": "2024-12-20T10:15:30Z"
}
```

**Поля ответа:**
- `SystemName` (string) - название системы
- `version` (string) - версия возвращенного конфига
- `config` (object) - конфигурация
- `updatedAt` (string) - время последнего обновления (ISO 8601)

**Примеры запросов:**

**cURL (текущая версия):**
```bash
curl -X GET http://localhost:8080/api/configs/auth-mock
```

**cURL (конкретная версия):**
```bash
curl -X GET "http://localhost:8080/api/configs/auth-mock?version=v1"
```

**PowerShell:**
```powershell
# Текущая версия
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/configs/auth-mock" `
    -Method Get

$response | ConvertTo-Json

# Конкретная версия
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/configs/auth-mock?version=v1" `
    -Method Get

$response | ConvertTo-Json
```

**Ошибки:**
- `404 Not Found` - конфиг с указанным названием системы не найден
- `500 Internal Server Error` - внутренняя ошибка сервера

**Примечания:**
- Версия может быть указана в форматах: "v1", "1", "v2", "2" и т.д.
- Если запрошена несуществующая версия, возвращается текущий конфиг
- Стартовый конфиг (версия 1) всегда доступен, если система зарегистрирована

---

### 4. Планирование отложенного обновления конфигурации

Планирует обновление конфигурации на указанное время в будущем.

**Эндпоинт:** `POST /api/configs/schedule`

**Описание:**
- Позволяет запланировать обновление конфигурации на определенное время
- Обновление будет применено автоматически в указанное время
- Можно указать комментарий для описания изменения
- Поддерживается несколько запланированных обновлений для одной системы

**Запрос:**

```json
{
  "SystemName": "auth-mock",
  "scheduledTime": "14:30:00 25-12-2024",
  "config": {
    "delays": {
      "loginDelayMs": "2000",
      "tokenDelayMs": "1000"
    },
    "stringParams": {
      "mode": "test",
      "environment": "production"
    },
    "loggingLv": "INFO"
  },
  "comment": "Увеличение задержек для тестирования"
}
```

**Поля запроса:**
- `SystemName` (string, обязательное) - название заглушки
- `scheduledTime` (string, обязательное) - дата и время в формате `HH:mm:ss dd-MM-yyyy` (например, "14:30:00 25-12-2024")
- `config` (object, обязательное) - конфигурация, которая будет применена
- `comment` (string, необязательное) - комментарий к запланированному обновлению

**Ответ:**

```json
{
  "success": true,
  "message": "Обновление успешно запланировано",
  "updateId": "550e8400-e29b-41d4-a716-446655440000",
  "scheduledTime": "14:30:00 25-12-2024"
}
```

**Поля ответа:**
- `success` (boolean) - успешно ли запланировано обновление
- `message` (string) - сообщение о результате
- `updateId` (string) - уникальный идентификатор запланированного обновления
- `scheduledTime` (string) - запланированное время обновления

**Примеры запросов:**

**cURL:**
```bash
curl -X POST http://localhost:8080/api/configs/schedule \
  -H "Content-Type: application/json" \
  -d '{
    "SystemName": "auth-mock",
    "scheduledTime": "14:30:00 25-12-2024",
    "config": {
      "delays": {
        "loginDelayMs": "2000",
        "tokenDelayMs": "1000"
      },
      "stringParams": {
        "mode": "test"
      },
      "loggingLv": "INFO"
    },
    "comment": "Увеличение задержек"
  }'
```

**PowerShell:**
```powershell
$body = @{
    SystemName = "auth-mock"
    scheduledTime = "14:30:00 25-12-2024"
    config = @{
        delays = @{
            loginDelayMs = "2000"
            tokenDelayMs = "1000"
        }
        stringParams = @{
            mode = "test"
        }
        loggingLv = "INFO"
    }
    comment = "Увеличение задержек"
} | ConvertTo-Json -Depth 10

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/configs/schedule" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

$response | ConvertTo-Json
```

**Ошибки:**
- `400 Bad Request` - неверный формат даты или дата в прошлом
- `500 Internal Server Error` - внутренняя ошибка сервера

---

### 5. Получение списка запланированных обновлений

Возвращает список всех запланированных обновлений для указанной системы.

**Эндпоинт:** `GET /api/configs/{systemName}/scheduled`

**Параметры:**
- `systemName` (path) - название заглушки

**Описание:**
- Возвращает все запланированные обновления для указанной системы
- Обновления отсортированы по времени выполнения (от ближайших к дальним)

**Ответ:**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "systemName": "auth-mock",
    "scheduledTime": "14:30:00 25-12-2024",
    "comment": "Увеличение задержек",
    "createdAt": "10:15:00 20-12-2024"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "systemName": "auth-mock",
    "scheduledTime": "18:00:00 25-12-2024",
    "comment": null,
    "createdAt": "11:20:00 20-12-2024"
  }
]
```

**Поля ответа:**
- `id` (string) - уникальный идентификатор обновления
- `systemName` (string) - название системы
- `scheduledTime` (string) - запланированное время обновления
- `comment` (string, nullable) - комментарий к обновлению
- `createdAt` (string) - время создания запланированного обновления

**Примеры запросов:**

**cURL:**
```bash
curl -X GET http://localhost:8080/api/configs/auth-mock/scheduled
```

**PowerShell:**
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/configs/auth-mock/scheduled" `
    -Method Get

$response | ConvertTo-Json
```

---

### 6. Отмена запланированного обновления

Отменяет запланированное обновление по его идентификатору.

**Эндпоинт:** `DELETE /api/configs/scheduled/{updateId}`

**Параметры:**
- `updateId` (path) - уникальный идентификатор запланированного обновления

**Описание:**
- Отменяет указанное запланированное обновление
- Обновление удаляется из списка и не будет выполнено

**Ответ:**

```json
{
  "success": true,
  "message": "Запланированное обновление отменено",
  "updateId": "550e8400-e29b-41d4-a716-446655440000",
  "scheduledTime": null
}
```

**Поля ответа:**
- `success` (boolean) - успешно ли отменено обновление
- `message` (string) - сообщение о результате
- `updateId` (string) - идентификатор отмененного обновления
- `scheduledTime` (string, nullable) - всегда null для отмененных обновлений

**Примеры запросов:**

**cURL:**
```bash
curl -X DELETE http://localhost:8080/api/configs/scheduled/550e8400-e29b-41d4-a716-446655440000
```

**PowerShell:**
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/configs/scheduled/550e8400-e29b-41d4-a716-446655440000" `
    -Method Delete

$response | ConvertTo-Json
```

**Ошибки:**
- `404 Not Found` - обновление с указанным ID не найдено
- `500 Internal Server Error` - внутренняя ошибка сервера

---

## Веб-интерфейс (Web UI)

### 1. Главная страница

Отображает список всех зарегистрированных заглушек.

**Эндпоинт:** `GET /`

**Описание:**
- Показывает все зарегистрированные заглушки
- Каждая заглушка - ссылка на страницу редактирования конфига

**Пример:**
```
http://localhost:8080
```

---

### 2. Страница конфигурации

Отображает и позволяет редактировать конфигурацию заглушки.

**Эндпоинт:** `GET /configs/{systemName}`

**Параметры:**
- `systemName` (path) - название заглушки (URL-encoded)

**Описание:**
- Отображает текущую версию конфига
- Показывает все параметры сгруппированные по типам:
  - `delays` - целочисленные значения задержек
  - `stringParams` - строковые параметры
  - `loggingLv` - уровень логирования (ERROR, WARN, INFO, DEBUG)
- Рядом с каждым параметром показывается стартовое значение
- Можно кликнуть на стартовое значение для автоматической подстановки
- Кнопка "Сохранить изменения" - обновляет конфиг
- Кнопка "Откатить к стартовому" - возвращает конфиг к стартовому состоянию
- Кнопка "Отложенное сохранение" - позволяет запланировать обновление на определенное время с комментарием
- Отображает список всех запланированных обновлений с возможностью отмены

**Пример:**
```
http://localhost:8080/configs/auth-mock
```

---

### 3. Обновление конфигурации

Обновляет конфигурацию через веб-форму.

**Эндпоинт:** `POST /configs/{systemName}?action=update`

**Параметры:**
- `systemName` (path) - название заглушки (URL-encoded)
- `action=update` (query) - действие обновления

**Форма:**
- `delay_{key}` - значения задержек (например, `delay_loginDelayMs=2000`)
- `string_{key}` - строковые параметры (например, `string_mode=test`)
- `loggingLv` - уровень логирования (ERROR, WARN, INFO, DEBUG)

**Описание:**
- Принимает данные из формы
- Обновляет конфиг и увеличивает версию на 1
- Перенаправляет на страницу конфига

**Пример:**
```
POST /configs/auth-mock?action=update
Content-Type: application/x-www-form-urlencoded

delay_loginDelayMs=2000&delay_tokenDelayMs=1000&string_mode=test&loggingLv=INFO
```

---

### 4. Откат к стартовому конфигу

Возвращает конфигурацию к стартовому состоянию.

**Эндпоинт:** `POST /configs/{systemName}?action=revert`

**Параметры:**
- `systemName` (path) - название заглушки (URL-encoded)
- `action=revert` (query) - действие отката

**Описание:**
- Устанавливает текущий конфиг равным стартовому
- Увеличивает версию на 1
- Перенаправляет на страницу конфига

**Пример:**
```
POST /configs/auth-mock?action=revert
```

---

### 5. Планирование отложенного обновления (Web UI)

Планирует обновление конфигурации через веб-форму.

**Эндпоинт:** `POST /configs/{systemName}?action=schedule`

**Параметры:**
- `systemName` (path) - название заглушки (URL-encoded)
- `action=schedule` (query) - действие планирования

**Форма:**
- `scheduledDateTime` (string, обязательное) - дата и время в формате `HH:mm:ss dd-MM-yyyy` (например, "14:30:00 25-12-2024")
- `scheduleComment` (string, необязательное) - комментарий к обновлению
- `delay_{key}` - значения задержек (например, `delay_loginDelayMs=2000`)
- `string_{key}` - строковые параметры (например, `string_mode=test`)
- `loggingLv` - уровень логирования (ERROR, WARN, INFO, DEBUG)

**Описание:**
- Принимает данные из формы
- Планирует обновление конфига на указанное время
- Перенаправляет на страницу конфига

**Пример:**
```
POST /configs/auth-mock?action=schedule
Content-Type: application/x-www-form-urlencoded

scheduledDateTime=14:30:00 25-12-2024&scheduleComment=Увеличение задержек&delay_loginDelayMs=2000&delay_tokenDelayMs=1000&string_mode=test&loggingLv=INFO
```

---

### 6. Отмена запланированного обновления (Web UI)

Отменяет запланированное обновление через веб-интерфейс.

**Эндпоинт:** `POST /configs/{systemName}?action=cancelSchedule`

**Параметры:**
- `systemName` (path) - название заглушки (URL-encoded)
- `action=cancelSchedule` (query) - действие отмены
- `updateId` (form parameter) - идентификатор запланированного обновления

**Описание:**
- Отменяет указанное запланированное обновление
- Перенаправляет на страницу конфига

**Пример:**
```
POST /configs/auth-mock?action=cancelSchedule
Content-Type: application/x-www-form-urlencoded

updateId=550e8400-e29b-41d4-a716-446655440000
```

---

## Структура конфигурации

Конфигурация заглушки представляет собой JSON объект со следующей структурой:

```json
{
  "delays": {
    "loginDelayMs": "1000",
    "tokenDelayMs": "500",
    "refreshDelayMs": "300"
  },
  "stringParams": {
    "mode": "normal",
    "environment": "test",
    "region": "us-east"
  },
  "loggingLv": "ERROR"
}
```

**Поля:**
- `delays` (object) - задержки в миллисекундах (целочисленные значения)
- `stringParams` (object) - строковые параметры
- `loggingLv` (string) - уровень логирования, возможные значения:
  - `ERROR`
  - `WARN`
  - `INFO`
  - `DEBUG`

---

## Управление версиями

- Версия хранится в поле `version` объекта `StoredConfig` (не в самом конфиге)
- Версия начинается с 1 для первой регистрации
- Версия увеличивается на 1 при каждом обновлении через UI или при изменении стартового конфига
- Формат версии: `v1`, `v2`, `v3`, и т.д.

**Логика версионирования:**
1. Первая регистрация → версия 1
2. Обновление через UI → версия +1
3. Откат к стартовому → версия +1
4. Изменение стартового конфига (через checkUpdate) → версия +1

---

## Коды ошибок

Все эндпоинты возвращают HTTP 200 OK при успешном выполнении.

При ошибках:
- **400 Bad Request** - неверный формат запроса
- **404 Not Found** - конфиг не найден (для веб-интерфейса)
- **500 Internal Server Error** - внутренняя ошибка сервера

---

## Примеры использования

### Сценарий 1: Первая регистрация заглушки

1. Заглушка отправляет `checkUpdate` с версией 1:
```json
POST /api/configs/checkUpdate
{
  "SystemName": "auth-mock",
  "version": "v1",
  "config": { ... }
}
```

2. Сервис отвечает:
```json
{
  "needUpdate": false,
  "currentVersion": "v1"
}
```

3. Конфиг автоматически сохраняется.

---

### Сценарий 2: Обновление конфига через UI

1. Открыть страницу конфига: `http://localhost:8080/configs/auth-mock`
2. Изменить значения параметров
3. Нажать "Сохранить изменения"
4. Версия увеличивается на 1

---

### Сценарий 3: Проверка обновлений

1. Заглушка отправляет `checkUpdate` с версией 1, но конфиг изменился:
```json
POST /api/configs/checkUpdate
{
  "SystemName": "auth-mock",
  "version": "v1",
  "config": { ... новый конфиг ... }
}
```

2. Сервис отвечает:
```json
{
  "needUpdate": false,
  "currentVersion": "v2"
}
```

3. Стартовый конфиг обновлен, версия повышена.

---

### Сценарий 4: Требуется обновление

1. Заглушка отправляет `checkUpdate` с версией 2, а в сервисе версия 3:
```json
POST /api/configs/checkUpdate
{
  "SystemName": "auth-mock",
  "version": "v2",
  "config": { ... }
}
```

2. Сервис отвечает:
```json
{
  "needUpdate": true,
  "currentVersion": "v3"
}
```

3. Заглушка должна запросить новый конфиг (эндпоинт будет реализован позже).

---

## Примечания

- Все названия заглушек (`SystemName`) автоматически санитизируются (специальные символы заменяются на `_`)
- Конфиги сохраняются локально в `data/configs/{systemName}.json`
- Версия хранится отдельно от конфига в поле `version` объекта `StoredConfig`
- Конфиг не содержит поля "Config version" - версия управляется только сервисом

