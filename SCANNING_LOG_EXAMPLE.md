# Пример логов сканирования эндпоинтов

## 🔍 Что происходит при запуске приложения

### 1. Инициализация сканера (если используется улучшенная версия с @PostConstruct)
```
2024-01-XX XX:XX:XX - Initializing EndpointSecurityScanner - performing initial scan...
2024-01-XX XX:XX:XX - Performing endpoint security scan...
```

### 2. Поиск контроллеров

**Вариант A: Контроллеры найдены в ApplicationContext (быстрый путь)**
```
2024-01-XX XX:XX:XX - Found X controllers in ApplicationContext
2024-01-XX XX:XX:XX - Scanning X controllers for security annotations
```

**Вариант B: Контроллеры не найдены, сканирование classpath (медленный путь)**
```
2024-01-XX XX:XX:XX - No controllers found in ApplicationContext, scanning classpath...
2024-01-XX XX:XX:XX - Scanning XXX class files for controllers
2024-01-XX XX:XX:XX - Found controller class: com.example.modulea.controller.DeclarationController
2024-01-XX XX:XX:XX - Found controller class: com.example.moduleb.controller.WareController
...
2024-01-XX XX:XX:XX - Found X controllers in classpath scan
2024-01-XX XX:XX:XX - Scanning X controllers for security annotations
```

### 3. Обработка каждого контроллера (DEBUG уровень)
```
2024-01-XX XX:XX:XX - Processing controller: com.example.modulea.controller.DeclarationController
2024-01-XX XX:XX:XX - Base path for controller DeclarationController: /api/declarations
2024-01-XX XX:XX:XX - Found X methods in controller DeclarationController
2024-01-XX XX:XX:XX - Method getDeclarationDetails extracted 1 paths: [/api/declarations/{declarationId}/details]
```

### 4. Настройка безопасности для каждого эндпоинта
```
2024-01-XX XX:XX:XX - Auto-configured security for path: /api/declarations/{declarationId}/details using annotation from method: DeclarationController.getDeclarationDetails
2024-01-XX XX:XX:XX - Auto-configured permitAll for path: /api/public/health using annotation from method: DataController.health
```

### 5. Итоговый результат
```
2024-01-XX XX:XX:XX - Auto-configured security for X endpoint paths, Y permitAll paths
```

**Если используется улучшенная версия с кэшированием:**
```
2024-01-XX XX:XX:XX - Scan complete. Cached X secured paths and Y permitAll paths
```

---

## 📊 Ожидаемые значения

### Количество контроллеров:
- **moduleA**: 4 контроллера (DeclarationController, DataController, ItemController, ReportController)
- **moduleB**: 4 контроллера (WareController, DataController, ItemController, ReportController)
- **Итого**: 8 контроллеров

### Количество эндпоинтов:
- Зависит от количества методов с HTTP маппингами в каждом контроллере
- Ожидается примерно 20-30 эндпоинтов

---

## ⚠️ Проблемы, которые могут возникнуть

### 1. "No controllers found in ApplicationContext"
**Причина**: 
- Не добавлен `@ComponentScan` в `GatewayApplication`
- Нет зависимостей на `moduleA` и `moduleB` в `pom.xml`

**Решение**: 
- Добавить `@ComponentScan(basePackages = {"com.example.gateway", "com.example.modulea", "com.example.moduleb"})`
- Добавить зависимости в `gateway/pom.xml`

### 2. "Found 0 controllers in classpath scan"
**Причина**: 
- Модули не в classpath
- Неправильный путь сканирования

**Решение**: 
- Проверить, что модули собраны и включены в classpath
- Проверить логику `scanClasspathForControllers()`

### 3. "Could not load controller class"
**Причина**: 
- Проблемы с ClassLoader
- Классы не доступны

**Решение**: 
- Убедиться, что модули правильно подключены как зависимости

---

## 🔧 Как включить подробные логи

В `application.properties` уже настроено:
```properties
logging.level.com.example.gateway=DEBUG
logging.level.com.example.gateway.security=DEBUG
```

Это должно показывать все DEBUG сообщения из сканера.
