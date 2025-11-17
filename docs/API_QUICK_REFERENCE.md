# API Быстрая справка

## REST API

### Проверка обновлений
```bash
POST /api/configs/checkUpdate
```

**PowerShell:**
```powershell
$body = @{
    SystemName = "auth-mock"
    version = "v1"
    config = @{
        delays = @{ loginDelayMs = "1000" }
        stringParams = @{ mode = "normal" }
        loggingLv = "ERROR"
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/api/configs/checkUpdate" `
    -Method Post -ContentType "application/json" -Body $body
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/configs/checkUpdate \
  -H "Content-Type: application/json" \
  -d '{"SystemName":"auth-mock","version":"v1","config":{"delays":{"loginDelayMs":"1000"},"stringParams":{"mode":"normal"},"loggingLv":"ERROR"}}'
```

**Ответ:**
```json
{
  "needUpdate": false,
  "currentVersion": "v1"
}
```

---

### Загрузка конфига
```bash
POST /api/configs
```

**PowerShell:**
```powershell
$body = @{
    SystemName = "auth-mock"
    config = @{
        delays = @{ loginDelayMs = "1000" }
        stringParams = @{ mode = "normal" }
        loggingLv = "ERROR"
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/api/configs" `
    -Method Post -ContentType "application/json" -Body $body
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/configs \
  -H "Content-Type: application/json" \
  -d '{"SystemName":"auth-mock","config":{"delays":{"loginDelayMs":"1000"},"stringParams":{"mode":"normal"},"loggingLv":"ERROR"}}'
```

**Ответ:**
```json
{
  "status": "START_REGISTERED",
  "message": "Start config saved",
  "currentVersion": "v1"
}
```

---

### Получение конфигурации
```bash
GET /api/configs/{systemName}?version=v1
```

**cURL (текущая версия):**
```bash
curl -X GET http://localhost:8080/api/configs/auth-mock
```

**cURL (конкретная версия):**
```bash
curl -X GET "http://localhost:8080/api/configs/auth-mock?version=v1"
```

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
      "mode": "test"
    },
    "loggingLv": "INFO"
  },
  "updatedAt": "2024-12-20T10:15:30Z"
}
```

---

### Планирование отложенного обновления
```bash
POST /api/configs/schedule
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/configs/schedule \
  -H "Content-Type: application/json" \
  -d '{"SystemName":"auth-mock","scheduledTime":"14:30:00 25-12-2024","config":{"delays":{"loginDelayMs":"2000"},"stringParams":{"mode":"test"},"loggingLv":"INFO"},"comment":"Увеличение задержек"}'
```

**Ответ:**
```json
{
  "success": true,
  "message": "Обновление успешно запланировано",
  "updateId": "550e8400-e29b-41d4-a716-446655440000",
  "scheduledTime": "14:30:00 25-12-2024"
}
```

---

### Получение списка запланированных обновлений
```bash
GET /api/configs/{systemName}/scheduled
```

**cURL:**
```bash
curl -X GET http://localhost:8080/api/configs/auth-mock/scheduled
```

**Ответ:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "systemName": "auth-mock",
    "scheduledTime": "14:30:00 25-12-2024",
    "comment": "Увеличение задержек",
    "createdAt": "10:15:00 20-12-2024"
  }
]
```

---

### Отмена запланированного обновления
```bash
DELETE /api/configs/scheduled/{updateId}
```

**cURL:**
```bash
curl -X DELETE http://localhost:8080/api/configs/scheduled/550e8400-e29b-41d4-a716-446655440000
```

**Ответ:**
```json
{
  "success": true,
  "message": "Запланированное обновление отменено",
  "updateId": "550e8400-e29b-41d4-a716-446655440000",
  "scheduledTime": null
}
```

---

## Веб-интерфейс

- **Главная:** `http://localhost:8080`
- **Конфиг:** `http://localhost:8080/configs/{systemName}`

---

## Статусы ответов

### checkUpdate
- `needUpdate: false` - обновление не требуется
- `needUpdate: true` - требуется обновление

### uploadConfig
- `START_REGISTERED` - первая регистрация
- `UPDATED_START_CONFIG` - стартовый конфиг обновлен
- `UPDATE_AVAILABLE` - доступна новая версия
- `NO_CHANGES` - изменений нет

---

## Структура конфига

```json
{
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
```

**Уровни логирования:** ERROR, WARN, INFO, DEBUG

