# Подробное руководство по использованию библиотеки MockController Client

Эта библиотека предназначена для подключения Spring Boot заглушек к [MockController](https://github.com/Diavolos88/mocController) - сервису для централизованного управления конфигурациями заглушек в нагрузочном тестировании.

## Содержание

1. [Архитектура библиотеки](#архитектура-библиотеки)
2. [Установка и настройка](#установка-и-настройка)
3. [Создание конфигурируемого сервиса](#создание-конфигурируемого-сервиса)
4. [Конфигурация приложения](#конфигурация-приложения)
5. [Работа с MockController](#работа-с-mockcontroller)
6. [Расширенные возможности](#расширенные-возможности)
7. [Отладка и логирование](#отладка-и-логирование)

## Архитектура библиотеки

Библиотека состоит из следующих компонентов:

### MockControllerClientBase
Базовый класс, от которого наследуется ваш сервис. Просто наследуйтесь от этого класса - вся логика синхронизации выполняется автоматически через `ConfigAggregator`.

### ConfigAggregator
Централизованный компонент, который:
- Автоматически находит все сервисы, наследующиеся от `MockControllerClientBase`
- Собирает конфигурацию от всех сервисов в один общий конфиг
- Отправляет объединенную конфигурацию в MockController
- Применяет обновления ко всем соответствующим сервисам
- Управляет уровнем логирования
- Поддерживает типы: `long` (delays), `int` (intParams), `boolean` (booleanVariables), `String` (stringParams)

### AppConfig
Читает `spring.application.name` из конфигурации для идентификации заглушки.

### LoggingConfig
Читает уровень логирования из `logging.logback.level` для динамического управления.

### MockControllerConfig
Настройки подключения к MockController (URL, интервал проверки, healthcheck, таймауты).

**Ссылка на MockController:** [https://github.com/Diavolos88/mocController](https://github.com/Diavolos88/mocController)

## Установка и настройка

### Шаг 1: Копирование файлов

Скопируйте следующие файлы из папки `config` в ваш проект:

```
src/main/java/com/mock/config/
├── AppConfig.java
├── ConfigAggregator.java
├── LoggingConfig.java
├── MockControllerClientBase.java
└── MockControllerConfig.java
```

**Важно:** Сохраните структуру пакетов или измените `package` в каждом файле на ваш пакет.

### Шаг 2: Обновление зависимостей

Убедитесь, что в `pom.xml` есть:

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

### Шаг 3: Настройка главного класса

Добавьте аннотации:

```java
@SpringBootApplication
@EnableConfigurationProperties  // Для работы с @ConfigurationProperties
@EnableScheduling                 // Для периодической проверки обновлений
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## Создание конфигурируемого сервиса

### Наследование от MockControllerClientBase

Ваш сервис должен наследоваться от `MockControllerClientBase` и быть помечен аннотацией `@Service`:

```java
import com.mock.config.MockControllerClientBase;
import org.springframework.stereotype.Service;

@Service
public class YourService extends MockControllerClientBase {
    // Ваши поля и методы
}
```

### Правила именования полей

Библиотека автоматически извлекает поля по следующим правилам:

1. **Поля для задержек (delays):**
   - Должны начинаться с `delay`, `Delay` или `DELAY`
   - Должны быть числового типа (`long`, `int`, `Integer`, `Long`)
   - Примеры: `delayResponse`, `DELAY_ALL_RESPONSE`, `DelayApiCall`

2. **Поля для целочисленных параметров (intParams):**
   - Должны начинаться с `int`, `Int` или `INT`
   - Должны быть числового типа (`int`, `Integer`)
   - Примеры: `intStatusCode`, `INT_RESPONSE_CODE`, `IntUserId`

3. **Поля для boolean параметров (booleanVariables):**
   - Должны начинаться с `is`, `Is` или `IS`
   - Должны быть типа `boolean` или `Boolean`
   - Примеры: `isHealthTrue`, `IS_ENABLED`, `IsDataAvailable`

4. **Поля для строковых параметров (stringParams):**
   - Должны начинаться с `string`, `String` или `STRING`
   - Должны быть типа `String`
   - Примеры: `stringMode`, `STRING_HEALTH_RS`, `StringStatus`

5. **Обычные поля:**
   - Поля, не начинающиеся с указанных префиксов, игнорируются
   - Могут использоваться для внутренней логики
   - **Регистр**: поддерживаются переменные как с маленькой, так и с большой буквы

### Пример сервиса

```java
import com.mock.config.MockControllerClientBase;
import org.springframework.stereotype.Service;

@Service
public class PaymentMockService extends MockControllerClientBase {
    
    // Delays - будут автоматически извлекаться
    private long delayPaymentProcessing = 1500;
    private long delayValidation = 300;
    private long delayNotification = 200;
    
    // String params - будут автоматически извлекаться
    private String stringPaymentStatus = "success";
    private String stringErrorCode = "NONE";
    private String stringCurrency = "USD";
    
    // Обычные поля - не будут извлекаться
    private final PaymentRepository repository;
    private static final String VERSION = "1.0";
    
    @Autowired
    public PaymentMockService(PaymentRepository repository) {
        this.repository = repository;
    }
    
    public PaymentResponse processPayment(PaymentRequest request) {
        // Используем delayPaymentProcessing
        try {
            Thread.sleep(delayPaymentProcessing);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Используем stringPaymentStatus и stringErrorCode
        // Все параметры автоматически синхронизируются с MockController!
        return PaymentResponse.builder()
            .status(stringPaymentStatus)
            .errorCode(stringErrorCode)
            .currency(stringCurrency)
            .build();
    }
}
```

### Типы данных

Библиотека поддерживает следующие типы для полей `delay`:
- `long` / `Long`
- `int` / `Integer`

Для полей `string` поддерживается только `String`.

## Конфигурация приложения

### application.yml

```yaml
# Имя вашей заглушки (используется как SystemName)
spring:
  application:
    name: payment-mock-service

# Настройки подключения к MockController
mock-controller:
  url: http://localhost:8080              # URL MockController
  check-interval-seconds: 5                # Интервал проверки обновлений

# Логирование
logging:
  logback:
    level: INFO                             # Уровень логирования для управления через MockController
  level:
    com.yourpackage: INFO                   # Пакет для управления уровнем логирования
    org.springframework: WARN
```

### Важные параметры

- **`spring.application.name`**: Используется как идентификатор заглушки в MockController
- **`mock-controller.url`**: Адрес вашего MockController сервера
- **`mock-controller.check-interval-seconds`**: Как часто проверять обновления (в секундах)
- **`logging.level.{package}`**: Пакет, для которого будет управляться уровень логирования

## Работа с MockController

### Процесс синхронизации

1. **При старте приложения:**
   ```
   Заглушка → Извлечение конфигурации → POST /api/configs/checkUpdate
   MockController → Ответ: needUpdate=true/false, currentVersion=vX
   Если needUpdate=true → GET /api/configs/{systemName}?version={version}
   Заглушка → Применение конфигурации
   ```

2. **Периодическая проверка:**
   ```
   Каждые N секунд → POST /api/configs/checkUpdate
   Если needUpdate=true → Загрузка и применение нового конфига
   ```

### Формат конфигурации

Конфигурация отправляется в следующем формате:

```json
{
  "SystemName": "payment-mock-service",
  "version": "v1",
  "config": {
    "delays": {
      "delayPaymentProcessing": "1500",
      "delayValidation": "300",
      "delayNotification": "200"
    },
    "intParams": {
      "intStatusCode": "200",
      "intUserId": "12345"
    },
    "booleanVariables": {
      "isHealthTrue": "true",
      "isDataAvailable": "false"
    },
    "stringParams": {
      "stringPaymentStatus": "success",
      "stringErrorCode": "NONE",
      "stringCurrency": "USD"
    },
    "loggingLv": "INFO"
  }
}
```

### Применение обновлений

При получении обновления библиотека:
1. Находит соответствующие поля в вашем сервисе по имени
2. Преобразует значения к нужному типу
3. Устанавливает новые значения через рефлексию
4. Обновляет уровень логирования (если изменился)

## Расширенные возможности

### Добавление новых параметров

Чтобы добавить новый параметр, просто создайте поле в вашем сервисе:

```java
@Service
public class YourService {
    // Новый параметр задержки
    private long delayNewFeature = 1000;
    
    // Новый строковый параметр
    private String stringNewMode = "default";
    
    // Библиотека автоматически обнаружит эти поля
}
```

После перезапуска заглушки новые параметры будут отправлены в MockController.

### Управление уровнем логирования

Уровень логирования можно изменить через MockController. Поддерживаемые значения:
- `ERROR` - только ошибки
- `WARN` - предупреждения и ошибки
- `INFO` - информационные сообщения, предупреждения и ошибки
- `DEBUG` - подробная отладочная информация
- `TRACE` - максимально подробная информация

Изменение применяется динамически без перезапуска.

### Обработка ошибок

Библиотека обрабатывает следующие ошибки:
- **Недоступность MockController** - логируется как предупреждение, приложение продолжает работать нормально
- **Таймауты подключения** - настроены таймауты (10 сек на подключение, 10 сек на чтение), не блокируют работу
- **Ошибки при применении конфигурации** - логируются как предупреждения, остальные поля применяются
- **Отсутствие полей в сервисе** - логируется как предупреждение, поле пропускается
- **Невалидные значения** - значения, которые не могут быть применены (например, слишком большое для int), пропускаются с предупреждением, остальные значения применяются
- **Все исключения обрабатываются** - работа заглушки не прерывается при ошибках MockController

**Важно:** Заглушка полностью функциональна даже при полной недоступности MockController. Все эндпоинты работают с текущими значениями переменных.

#### Обработка невалидных значений

Если значение не может быть применено (например, слишком большое для int, невалидный формат для boolean), оно:
- Пропускается с логированием предупреждения
- Не влияет на применение остальных значений
- Поле сохраняет свое текущее значение
- Приложение продолжает работать нормально

Примеры невалидных значений:
- Для `int` поля: значение больше `2147483647` или меньше `-2147483648`
- Для `boolean` поля: значение не `"true"` или `"false"`
- Пустые или `null` значения
- Невалидные числа (например, `"abc123"` для int)

## Отладка и логирование

### Включение отладочного логирования

Добавьте в `application.yml`:

```yaml
logging:
  level:
    com.mock.config: DEBUG  # Для библиотеки
    com.yourpackage: DEBUG  # Для вашего кода
```

### Проверка работы библиотеки

1. **Проверка отправки конфигурации:**
   - При старте приложения должны появиться логи о подключении к MockController
   - Проверьте логи MockController на наличие входящих запросов

2. **Проверка применения обновлений:**
   - Измените конфигурацию в MockController
   - Подождите интервал проверки (по умолчанию 5 секунд)
   - Проверьте логи на сообщение "Config applied successfully"

3. **Проверка извлечения полей:**
   - Убедитесь, что все нужные поля начинаются с `delay` или `string`
   - Проверьте, что имена полей точно совпадают с ключами в MockController

### Частые проблемы

**Проблема:** Поля не обновляются после изменения в MockController

**Решение:**
- Проверьте, что имена полей точно совпадают (регистр важен)
- Убедитесь, что поля объявлены как `private`
- Проверьте логи на наличие предупреждений о не найденных полях

**Проблема:** Конфигурация не отправляется

**Решение:**
- Проверьте URL MockController
- Убедитесь, что MockController запущен
- Проверьте сетевую доступность
- Посмотрите логи на наличие ошибок подключения

**Проблема:** Уровень логирования не меняется

**Решение:**
- Убедитесь, что в `application.yml` указан правильный пакет
- Проверьте, что значение `loggingLv` корректно
- Убедитесь, что используется Logback (не другой логгер)

## Заключение

Библиотека предоставляет простой способ подключения любой Spring Boot заглушки к MockController для централизованного управления конфигурацией. Следуя этому руководству, вы сможете быстро интегрировать библиотеку в свой проект.

