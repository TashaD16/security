# Security Multi-Module Application

Многомодульное Spring Boot приложение с централизованной логикой авторизации в модуле gateway, состоящее из модуля gateway и двух бизнес-модулей (moduleA и moduleB).

## Структура проекта

```
security-multimodule/
├── pom.xml                    # Родительский pom.xml
├── gateway/                   # Gateway модуль с централизованной безопасностью
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/SecurityConfig.java
│       │   └── security/CustomAuthorizationManager.java
│       └── resources/
│           └── application.properties
├── moduleA/                   # Модуль A (управление декларациями)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/modulea/
│       │   ├── ModuleAApplication.java
│       │   └── controller/
│       │       ├── ItemController.java
│       │       ├── ReportController.java
│       │       ├── DataController.java
│       │       └── DeclarationController.java
│       └── resources/
│           └── application.properties
└── moduleB/                   # Модуль B (управление товарами)
    ├── pom.xml
    └── src/main/
        ├── java/com/example/moduleb/
        │   ├── ModuleBApplication.java
        │   └── controller/
        │       ├── ItemController.java
        │       ├── ReportController.java
        │       ├── DataController.java
        │       └── WareController.java
        └── resources/
            └── application.properties
```

## Модули

### Gateway (Порт: 8082)
**Централизованный модуль безопасности**, который содержит:
- `CustomAuthorizationManager` - класс для кастомной логики авторизации
- Единую конфигурацию безопасности (`SecurityConfig`) для всех модулей
- Все правила авторизации для эндпоинтов moduleA и moduleB
- Управление пользователями и их правами доступа
- `SecurityWebFilterChain` для защиты всех API эндпоинтов

**Важно**: Gateway является единой точкой входа для всех запросов. Вся логика безопасности централизована здесь.

`CustomAuthorizationManager` содержит всю логику авторизации:
- Извлекает переменные пути (declarationId, wareId) из URI
- Проверяет права доступа на основе ролей и authorities
- Выполняет проверку владения ресурсами
- Логирует все операции авторизации

### ModuleA (Порт: 8080)
Модуль для управления декларациями со следующими эндпоинтами:

**Общие эндпоинты (70%):**
- `GET /api/items/list` - список элементов
- `POST /api/items/create` - создание элемента
- `PUT /api/items/{id}/update` - обновление элемента
- `DELETE /api/items/{id}/delete` - удаление элемента
- `GET /api/items/{id}/details` - детали элемента
- `GET /api/items/search` - поиск элементов
- `POST /api/reports/generate` - генерация отчета
- `GET /api/reports/list` - список отчетов
- `GET /api/reports/{id}/download` - скачивание отчета
- `POST /api/data/export` - экспорт данных
- `POST /api/data/import` - импорт данных

**Специфичные эндпоинты модуля A (30%):**
- `GET /api/declarations/{declarationId}/details` - детали декларации
- `POST /api/declarations/{declarationId}/approve` - утверждение декларации
- `POST /api/declarations/{declarationId}/submit` - отправка декларации
- `POST /api/declarations/{declarationId}/reject` - отклонение декларации
- `POST /api/declarations/{declarationId}/cancel` - отмена декларации

### ModuleB (Порт: 8081)
Модуль для управления товарами со следующими эндпоинтами:

**Общие эндпоинты (70%):** (те же, что и в moduleA)
- `GET /api/items/list`
- `POST /api/items/create`
- `PUT /api/items/{id}/update`
- `DELETE /api/items/{id}/delete`
- `GET /api/items/{id}/details`
- `GET /api/items/search`
- `POST /api/reports/generate`
- `GET /api/reports/list`
- `GET /api/reports/{id}/download`
- `POST /api/data/export`
- `POST /api/data/import`

**Специфичные эндпоинты модуля B (30%):**
- `GET /api/wares/{wareId}/details` - детали товара
- `GET /api/wares/{wareId}/inventory` - инвентаризация товара
- `PUT /api/wares/{wareId}/update` - обновление товара
- `POST /api/wares/{wareId}/reserve` - резервирование товара
- `POST /api/wares/{wareId}/release` - освобождение товара

## Архитектура безопасности

Вся логика безопасности централизована в модуле **gateway**. Это означает:

1. **Единая точка входа**: Gateway обрабатывает все запросы и применяет правила безопасности
2. **Централизованная конфигурация**: Все правила авторизации определены в одном месте (`gateway/src/main/java/com/example/gateway/config/SecurityConfig.java`)
3. **Упрощенная архитектура**: Бизнес-модули (moduleA и moduleB) не содержат конфигурации безопасности и сосредоточены на бизнес-логике
4. **Легкое обслуживание**: Изменения в правилах безопасности требуют изменений только в gateway модуле

## Тестовые пользователи

Все пользователи определены в gateway модуле:

- **admin** / admin123 - полные права (ADMIN)
  - Authorities: `ADMIN`, `READ_DECLARATION`, `WRITE_DECLARATION`, `APPROVE_DECLARATION`, `READ_WARE`, `WRITE_WARE`, `MANAGE_INVENTORY`

- **user1** / user123 - права на работу с декларациями
  - Authorities: `READ_DECLARATION`, `WRITE_DECLARATION`

- **approver** / approver123 - права на утверждение деклараций
  - Authorities: `READ_DECLARATION`, `APPROVE_DECLARATION`

- **user2** / user123 - права на работу с товарами
  - Authorities: `READ_WARE`, `WRITE_WARE`

- **inventory** / inventory123 - права на управление инвентарем
  - Authorities: `READ_WARE`, `MANAGE_INVENTORY`

## Кастомная авторизация

`CustomAuthorizationManager` (находится в модуле `gateway`) реализует следующие методы проверки доступа:

1. **checkReadDeclaration** - проверка права чтения декларации
   - Требует authority: `READ_DECLARATION` или `ADMIN`
   - Извлекает `declarationId` из пути
   - Проверяет владение декларацией

2. **checkWriteDeclaration** - проверка права записи декларации
   - Требует authority: `WRITE_DECLARATION` или `ADMIN`
   - Извлекает `declarationId` из пути
   - Проверяет владение декларацией

3. **checkApproveDeclaration** - проверка права утверждения декларации
   - Требует authority: `APPROVE_DECLARATION` или `ADMIN`
   - Извлекает `declarationId` из пути

4. **checkReadWare** - проверка права чтения товара
   - Требует authority: `READ_WARE` или `ADMIN`
   - Извлекает `wareId` из пути
   - Проверяет владение товаром

5. **checkWriteWare** - проверка права записи товара
   - Требует authority: `WRITE_WARE` или `ADMIN`
   - Извлекает `wareId` из пути
   - Проверяет владение товаром

6. **checkWareInventory** - проверка права управления инвентарем
   - Требует authority: `MANAGE_INVENTORY` или `ADMIN`
   - Извлекает `wareId` из пути

7. **checkGeneralAccess** - проверка общего доступа
   - Разрешает доступ всем аутентифицированным пользователям

### Извлечение переменных пути

`CustomAuthorizationManager` извлекает переменные пути (например, `declarationId`, `wareId`) из URI используя регулярные выражения:
- Для пути `/api/declarations/DECL-123/details` извлекается `DECL-123`
- Для пути `/api/wares/WARE-456/inventory` извлекается `WARE-456`

### Проверка владения

В текущей реализации используется упрощенная логика проверки владения:
- Декларация считается принадлежащей пользователю, если `declarationId` начинается с первой буквы имени пользователя
- Товар считается принадлежащим пользователю, если `wareId` начинается с первой буквы имени пользователя

**Пример:**
- Пользователь `admin` имеет доступ к декларациям, начинающимся с `A` (например, `A-DECL-001`)
- Пользователь `user1` имеет доступ к декларациям, начинающимся с `U` (например, `U-DECL-001`)

В реальном приложении эту логику следует заменить на проверку в базе данных.

## Сборка проекта

### Требования
- Java 17 или выше
- Maven 3.6 или выше

### Сборка всех модулей

```bash
# Из корневой директории проекта
mvn clean install
```

Эта команда:
1. Соберет `gateway` модуль
2. Соберет `moduleA`
3. Соберет `moduleB`
4. Создаст JAR файлы для каждого модуля

### Сборка отдельного модуля

```bash
# Сборка только gateway
cd gateway
mvn clean install

# Сборка только moduleA
cd moduleA
mvn clean install

# Сборка только moduleB
cd moduleB
mvn clean install
```

## Запуск приложения

### Запуск Gateway (обязательно запустить первым)

Gateway должен быть запущен первым, так как он обрабатывает всю логику безопасности:

```bash
# Из корневой директории
cd gateway
mvn spring-boot:run

# Или запуск собранного JAR
java -jar target/gateway-1.0.0.jar
```

Gateway будет доступен на порту **8082**: `http://localhost:8082`

### Запуск ModuleA

```bash
# Из корневой директории
cd moduleA
mvn spring-boot:run

# Или запуск собранного JAR
java -jar target/moduleA-1.0.0.jar
```

ModuleA будет доступен на порту **8080**: `http://localhost:8080`

### Запуск ModuleB

```bash
# Из корневой директории
cd moduleB
mvn spring-boot:run

# Или запуск собранного JAR
java -jar target/moduleB-1.0.0.jar
```

ModuleB будет доступен на порту **8081**: `http://localhost:8081`

### Запуск всех модулей одновременно

Модули можно запустить в отдельных терминалах:

```bash
# Терминал 1 - Gateway (запустить первым)
cd gateway && mvn spring-boot:run

# Терминал 2 - ModuleA
cd moduleA && mvn spring-boot:run

# Терминал 3 - ModuleB
cd moduleB && mvn spring-boot:run
```

**Важно**: Gateway должен быть запущен первым, так как он централизует всю логику безопасности.

## Тестирование API

Все запросы должны проходить через gateway (порт 8082), где применяются правила безопасности.

### Использование curl

#### Gateway - Примеры запросов

**1. Получение деталей декларации (требует READ_DECLARATION):**
```bash
curl -u user1:user123 \
  http://localhost:8082/api/declarations/U-DECL-001/details
```

**2. Утверждение декларации (требует APPROVE_DECLARATION):**
```bash
curl -u approver:approver123 -X POST \
  http://localhost:8082/api/declarations/DECL-123/approve
```

**3. Получение инвентаря товара (требует MANAGE_INVENTORY):**
```bash
curl -u inventory:inventory123 \
  http://localhost:8082/api/wares/WARE-001/inventory
```

**4. Обновление товара (требует WRITE_WARE):**
```bash
curl -u user2:user123 -X PUT \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated Ware","quantity":50}' \
  http://localhost:8082/api/wares/WARE-001/update
```

**5. Список элементов (требует аутентификации):**
```bash
curl -u user1:user123 \
  http://localhost:8082/api/items/list
```

### Использование Postman или другого REST клиента

1. Настройте Basic Authentication:
   - Username: один из тестовых пользователей
   - Password: соответствующий пароль

2. Отправляйте запросы на gateway (порт 8082):
   - Базовый URL: `http://localhost:8082`

3. Проверяйте ответы:
   - `200 OK` - доступ разрешен
   - `401 Unauthorized` - неверные учетные данные
   - `403 Forbidden` - доступ запрещен (нет нужных прав)

## Логирование

Приложение использует SLF4J для логирования. В `application.properties` настроен уровень логирования:
- `DEBUG` для пакетов приложения и безопасности
- Все операции авторизации логируются в `CustomAuthorizationManager`

Логи включают:
- Проверяемый путь
- Имя пользователя
- Извлеченные переменные пути
- Решение об авторизации (разрешен/запрещен)

## Архитектурные решения

1. **Многомодульность**: Использование Maven multi-module для разделения функциональности
2. **Централизованная безопасность**: Вся логика безопасности вынесена в модуль gateway
3. **Общий модуль**: Общая логика авторизации (CustomAuthorizationManager) вынесена в модуль `common`
4. **Реактивная архитектура**: Использование Spring WebFlux для реактивных REST API
5. **Кастомная авторизация**: Индивидуальная защита каждого эндпоинта через `SecurityWebFilterChain`
6. **Извлечение переменных пути**: Динамическое извлечение параметров из URI для проверки доступа
7. **Расширяемость**: Легко добавить новые методы проверки в `CustomAuthorizationManager`

## Расширение функциональности

### Добавление новой проверки авторизации

1. Добавьте новый метод в `CustomAuthorizationManager` (модуль `gateway`):
```java
public AuthorizationDecision checkCustomOperation(
        Authentication authentication,
        AuthorizationContext context) {
    // Ваша логика проверки
}
```

2. Добавьте правило в `SecurityConfig` модуля `gateway`:
```java
.pathMatchers("/api/custom/{id}/operation")
    .access((auth, ctx) -> Mono.just(authorizationManager.checkCustomOperation(auth, ctx)))
```

### Изменение логики проверки владения

Замените методы `checkDeclarationOwnership` и `checkWareOwnership` в `CustomAuthorizationManager` на реальные запросы к базе данных.

### Добавление нового пользователя

Добавьте нового пользователя в метод `userDetailsService()` в `SecurityConfig` модуля `gateway`.

## Troubleshooting

### Проблема: Модуль не компилируется
- Убедитесь, что все зависимости установлены: `mvn clean install` из корневой директории
- Проверьте версию Java: `java -version` (должна быть 17+)

### Проблема: Порт уже занят
- Измените порт в `application.properties`: `server.port=8083`
- Или остановите процесс, использующий порт

### Проблема: 403 Forbidden при наличии правильных credentials
- Проверьте, что пользователь имеет необходимые authorities
- Проверьте логи gateway для детальной информации об авторизации
- Убедитесь, что путь к эндпоинту правильный

### Проблема: Переменные пути не извлекаются
- Проверьте формат пути (должен соответствовать паттерну)
- Проверьте логи для отладки извлечения переменных

### Проблема: Gateway не применяет правила безопасности
- Убедитесь, что gateway запущен
- Проверьте, что запросы идут через gateway (порт 8082)
- Проверьте конфигурацию SecurityConfig в gateway модуле

## Лицензия

Этот проект создан в образовательных целях.
