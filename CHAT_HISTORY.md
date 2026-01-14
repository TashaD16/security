# История разработки проекта Security Multi-Module

## Обзор проекта

Разработка модульного Spring Boot приложения с двумя модулями (moduleA и moduleB) и централизованной системой безопасности в модуле gateway.

## Основные этапы разработки

### 1. Начальная структура проекта

Создана многомодульная Maven структура:
- `common` - общий модуль с аннотациями безопасности
- `moduleA` - модуль A с контроллерами
- `moduleB` - модуль B с контроллерами
- `gateway` - модуль с централизованной конфигурацией безопасности

### 2. Перенос логики безопасности в gateway

**Задача**: Вынести всю логику безопасности из модулей в отдельный модуль gateway.

**Изменения**:
- Перемещен `CustomAuthorizationManager` из `common` в `gateway`
- Удалены конфигурации безопасности из `moduleA` и `moduleB`
- Все контроллеры теперь используют аннотации из модуля `common`

### 3. Рефакторинг SecurityConfig

**Задача**: Оптимизировать конфигурацию безопасности, используя method references.

**Изменения**:
- Изменена конфигурация `.access()` для использования method references
- Пример: `.access(authorizationManager::checkGeneralAccess)`

### 4. Внедрение аннотаций безопасности

**Задача**: Использовать аннотации на контроллерах для автоматической конфигурации безопасности.

**Созданные аннотации в модуле `common`**:
- `@RequiresReadDeclaration` - для чтения деклараций
- `@RequiresWriteDeclaration` - для записи деклараций
- `@RequiresApproveDeclaration` - для одобрения деклараций
- `@RequiresReadWare` - для чтения товаров
- `@RequiresWriteWare` - для записи товаров
- `@RequiresWareInventory` - для управления инвентарем
- `@RequiresGeneralAccess` - для общих операций
- `@PermitAll` - для публичных эндпоинтов (без аутентификации)

### 5. Автоматическое сканирование эндпоинтов

**Задача**: Автоматически конфигурировать SecurityWebFilterChain на основе аннотаций без ручного указания `.pathMatchers()`.

**Решение**: Создан класс `EndpointSecurityScanner`

**Функциональность**:
- Автоматическое сканирование контроллеров из ApplicationContext
- Поддержка сканирования через classpath для модулей, не находящихся в том же ApplicationContext
- Извлечение путей из аннотаций @GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @PatchMapping
- Сопоставление путей с методами авторизации на основе аннотаций безопасности
- Создание ReactiveAuthorizationManager для каждого эндпоинта

### 6. Исправление ошибок компиляции

#### Ошибка 1: Несовместимость типов в SecurityConfig
**Проблема**: Метод `.access()` ожидает `ReactiveAuthorizationManager<AuthorizationContext>`, а передавался `BiFunction`.

**Решение**: Изменен тип возвращаемого значения `EndpointSecurityScanner.scanEndpoints()` с `BiFunction` на `ReactiveAuthorizationManager<AuthorizationContext>`.

#### Ошибка 2: Реализация интерфейса AuthorizationManager
**Проблема**: `CustomAuthorizationManager` реализовывал `AuthorizationManager<AuthorizationContext>`, но метод `check()` имел неправильную сигнатуру.

**Решение**: Удалена реализация интерфейса `AuthorizationManager`. `CustomAuthorizationManager` теперь обычный класс с методами, которые возвращают `Mono<AuthorizationDecision>`.

#### Ошибка 3: Неправильная обработка Mono<Authentication>
**Проблема**: `ReactiveAuthorizationManager.check()` принимает `Mono<Authentication>`, а методы `CustomAuthorizationManager` ожидают `Authentication`.

**Решение**: В `EndpointSecurityScanner` используются лямбда-выражения с `flatMap` для распаковки `Mono<Authentication>`:
```java
return (authenticationMono, context) -> authenticationMono
    .flatMap(authentication -> authorizationManager.checkReadDeclaration(authentication, context));
```

### 7. Добавление аннотации @PermitAll для публичных эндпоинтов

**Задача**: Добавить возможность помечать эндпоинты как публичные (доступные без аутентификации).

**Решение**: 
- Создана аннотация `@PermitAll` в модуле `common`
- Создан класс `EndpointSecurityScanResult` для хранения результатов сканирования (защищенные и публичные пути)
- Обновлен `EndpointSecurityScanner` для раздельной обработки публичных и защищенных эндпоинтов
- Обновлен `SecurityConfig` для применения `.permitAll()` к публичным эндпоинтам

**Изменения**:
- Аннотация `@PermitAll` помечает методы контроллеров для публичного доступа
- `EndpointSecurityScanner.scanEndpoints()` теперь возвращает `EndpointSecurityScanResult` с двумя списками: защищенные пути и публичные пути
- В `SecurityConfig` сначала применяется `.permitAll()` для публичных эндпоинтов, затем `.access()` для защищенных, и в конце `.anyExchange().authenticated()` для остальных

**Логика работы**:
- Эндпоинты с `@PermitAll` → доступны без аутентификации
- Эндпоинты с другими аннотациями безопасности → требуют аутентификации и авторизации
- Эндпоинты без аннотаций → требуют аутентификации (`.anyExchange().authenticated()`)

### 8. Рефакторинг кода для соответствия принципам SOLID, DRY, KISS

**Задача**: Улучшить качество кода, устранить дублирование и улучшить поддерживаемость.

**Проблемы, выявленные при анализе кода**:
1. Дублирование кода в методах `check*` класса `CustomAuthorizationManager`
2. Отсутствие констант для строковых литералов (authority strings)
3. Дублирование логики проверки владения ресурсами

**Выполненный рефакторинг**:

#### CustomAuthorizationManager - устранение дублирования:
- **Добавлены константы** для authority strings:
  - `AUTHORITY_ADMIN`, `AUTHORITY_READ_DECLARATION`, `AUTHORITY_WRITE_DECLARATION`, и т.д.
  - `PATH_VAR_DECLARATION_ID`, `PATH_VAR_WARE_ID` для имен переменных пути
- **Вынесена общая логика** в метод `checkAccess()`:
  - Универсальный метод для проверки доступа с authority и опциональной проверкой владения
  - Принимает `BiFunction<String, String, Boolean>` для проверки владения
- **Добавлены вспомогательные методы**:
  - `isAuthenticated()` - проверка аутентификации
  - `hasAuthority()` - проверка authority
  - `extractPathVariableFromContext()` - извлечение переменной из контекста
  - `getUsername()` - получение username из authentication
- **Объединены методы проверки владения**:
  - `checkDeclarationOwnership()` и `checkWareOwnership()` объединены в один метод `checkResourceOwnership(String username, String resourceId)`

**Результаты рефакторинга**:
- ✅ Устранено дублирование кода (принцип DRY)
- ✅ Добавлены константы (Java Code Conventions)
- ✅ Улучшена читаемость и поддерживаемость кода
- ✅ Упрощена логика методов `check*` (принцип KISS)
- ✅ Код стал более модульным и расширяемым

**Примечание**: Метод `extractAuthorizationMethod()` в `EndpointSecurityScanner` оставлен без изменений, так как рефакторинг с использованием Map усложнил бы код и нарушил принцип KISS. Текущая реализация проста и понятна.

## Структура файлов

### Gateway Module
- `SecurityConfig.java` - централизованная конфигурация безопасности
- `CustomAuthorizationManager.java` - логика авторизации
- `EndpointSecurityScanner.java` - сканер для автоматической конфигурации
- `EndpointSecurityScanResult.java` - класс результата сканирования (защищенные и публичные пути)

### Common Module
- Аннотации безопасности в `com.example.common.security.annotation`

### ModuleA и ModuleB
- Контроллеры с аннотациями безопасности
- Удалены конфигурации безопасности (перенесены в gateway)

## Ключевые технические решения

1. **Централизованная безопасность**: Вся логика безопасности сосредоточена в модуле gateway
2. **Аннотации**: Использование кастомных аннотаций для декларативного определения требований безопасности
3. **Автоматическое сканирование**: EndpointSecurityScanner автоматически находит эндпоинты и настраивает безопасность
4. **Реактивность**: Использование Spring WebFlux и ReactiveAuthorizationManager
5. **Гибкость**: Поддержка как сканирования через ApplicationContext, так и через classpath
6. **Публичные эндпоинты**: Поддержка аннотации @PermitAll для эндпоинтов, доступных без аутентификации

## Коммиты

1. `459e074` - "Add automatic endpoint security scanning - remove manual pathMatchers() configuration"
2. `2cc2f71` - "Fix ReactiveAuthorizationManager - use flatMap for Mono<Authentication>"
3. `eabe1f3` - "Add @PermitAll annotation for public endpoints"

## Результат

Полностью рабочая система безопасности с:
- Автоматической конфигурацией на основе аннотаций
- Централизованным управлением в модуле gateway
- Гибкой архитектурой, поддерживающей несколько модулей
- Реактивной моделью безопасности Spring WebFlux
- Поддержкой публичных эндпоинтов через аннотацию @PermitAll
- Разделением публичных и защищенных эндпоинтов при сканировании
- Рефакторингом кода для соответствия принципам SOLID, DRY, KISS
- Константами для authority strings (Java Code Conventions)
- Устраненным дублированием кода в CustomAuthorizationManager

## Использование аннотаций

### Пример использования @PermitAll

```java
@RestController
@RequestMapping("/api/public")
public class PublicController {
    
    @PermitAll
    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> getPublicInfo() {
        // Этот эндпоинт доступен без аутентификации
    }
}
```

### Пример использования защищенных аннотаций

```java
@RestController
@RequestMapping("/api/declarations")
public class DeclarationController {
    
    @RequiresReadDeclaration
    @GetMapping("/{declarationId}/details")
    public Mono<ResponseEntity<Map<String, Object>>> getDetails(@PathVariable String declarationId) {
        // Этот эндпоинт требует аутентификации и права READ_DECLARATION
    }
}
```