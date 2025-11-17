# MockController Client Library

Библиотека для подключения любой Spring Boot заглушки к MockController для централизованного управления конфигурацией.

## Описание

Эта библиотека позволяет любой заглушке автоматически:
- Отправлять свою конфигурацию в MockController при старте
- Периодически проверять наличие обновлений конфигурации
- Автоматически применять обновления без перезапуска
- Динамически изменять уровень логирования

## Быстрый старт

### 1. Скопируйте папку `config` в ваш проект

Скопируйте всю папку `src/main/java/com/mock/config/` в ваш проект, сохранив структуру пакетов.

### 2. Добавьте зависимости в `pom.xml`

Убедитесь, что в вашем проекте есть следующие зависимости:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
    </dependency>
</dependencies>
```

### 3. Настройте `application.yml`

Добавьте в ваш `application.yml`:

```yaml
spring:
  application:
    name: your-service-name  # Имя вашей заглушки

# Конфигурация MockController
mock-controller:
  url: http://localhost:8080  # URL вашего MockController
  check-interval-seconds: 5   # Интервал проверки обновлений (в секундах)

# Логирование
logging:
  level:
    com.yourpackage: INFO  # Пакет, для которого будет управляться уровень логирования
```

### 4. Добавьте аннотации в главный класс

```java
@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 5. Создайте сервис с конфигурируемыми параметрами

В вашем сервисе создайте поля, которые начинаются с `delay` (для числовых задержек) и `string` (для строковых параметров):

```java
@Service
public class YourService {
    
    // Параметры с префиксом "delay" - будут в delays
    private long delayResponse = 1000;
    private long delayProcessing = 500;
    
    // Параметры с префиксом "string" - будут в stringParams
    private String stringMode = "normal";
    private String stringStatus = "active";
    
    // Обычные поля не будут извлекаться
    private String someOtherField = "value";
    
    // Ваша бизнес-логика
    public void doSomething() {
        // Используйте delayResponse, stringMode и т.д.
    }
}
```

### 6. Инжектируйте зависимости

Библиотека автоматически создаст необходимые компоненты через Spring DI. Убедитесь, что ваш сервис доступен для инжекции:

```java
@Autowired
private YourService yourService;
```

## Как это работает

1. **При старте приложения:**
   - Библиотека автоматически извлекает все поля, начинающиеся с `delay` и `string`, из вашего сервиса
   - Отправляет конфигурацию в MockController через `/api/configs/checkUpdate`
   - Если MockController сообщает о необходимости обновления, загружает и применяет новый конфиг

2. **Периодическая проверка:**
   - Каждые N секунд (настраивается в `check-interval-seconds`) библиотека проверяет наличие обновлений
   - При обнаружении обновлений автоматически применяет их к вашему сервису

3. **Применение конфигурации:**
   - Библиотека использует рефлексию для обновления полей в вашем сервисе
   - Имена полей должны точно совпадать с ключами в конфигурации
   - Уровень логирования обновляется динамически через Logback

## Структура конфигурации

Конфигурация отправляется в MockController в следующем формате:

```json
{
  "SystemName": "your-service-name",
  "version": "v1",
  "config": {
    "delays": {
      "delayResponse": "1000",
      "delayProcessing": "500"
    },
    "stringParams": {
      "stringMode": "normal",
      "stringStatus": "active"
    },
    "loggingLv": "INFO"
  }
}
```

## Правила именования полей

- **Поля для задержек**: должны начинаться с `delay` (например: `delayResponse`, `delayHelloWorld`)
- **Поля для строковых параметров**: должны начинаться с `string` (например: `stringMode`, `stringStatus`)
- **Имена полей**: используются как ключи в конфигурации, поэтому должны точно совпадать

## Примеры использования

### Пример 1: Простая заглушка с задержками

```java
@Service
public class SimpleMockService {
    private long delayApiCall = 2000;
    private String stringResponse = "Success";
    
    public String processRequest() {
        try {
            Thread.sleep(delayApiCall);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return stringResponse;
    }
}
```

### Пример 2: Заглушка с несколькими параметрами

```java
@Service
public class ComplexMockService {
    // Delays
    private long delayLogin = 1000;
    private long delayLogout = 500;
    private long delayDataFetch = 2000;
    
    // String params
    private String stringAuthMode = "jwt";
    private String stringErrorCode = "NONE";
    private String stringUserRole = "admin";
    
    public AuthResponse login(String username) {
        // Используйте delayLogin и stringAuthMode
    }
}
```

## Настройка уровня логирования

Уровень логирования управляется через параметр `loggingLv` в конфигурации. Поддерживаемые значения:
- `ERROR`
- `WARN`
- `INFO`
- `DEBUG`
- `TRACE`

Уровень применяется к пакету, указанному в `logging.level.{package}` в `application.yml`.

## Устранение неполадок

### Поля не обновляются

- Убедитесь, что имена полей точно совпадают с ключами в конфигурации MockController
- Проверьте, что поля начинаются с `delay` или `string`
- Убедитесь, что поля объявлены как `private` (рефлексия работает с private полями)

### Конфигурация не отправляется

- Проверьте URL MockController в `application.yml`
- Убедитесь, что MockController запущен и доступен
- Проверьте логи на наличие ошибок подключения

### Уровень логирования не меняется

- Убедитесь, что в `application.yml` указан правильный пакет в `logging.level.{package}`
- Проверьте, что значение `loggingLv` в конфигурации соответствует одному из поддерживаемых уровней

## Дополнительная информация

Подробная документация по использованию библиотеки находится в [docs/USAGE.md](docs/USAGE.md).
