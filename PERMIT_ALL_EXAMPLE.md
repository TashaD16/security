# Примеры использования @PreAuthorize для @PermitAll

## ✅ Вариант 1: Встроенная функция Spring Security (Рекомендуется)

Используйте встроенную функцию `permitAll()` в SpEL выражении:

```java
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @PreAuthorize("permitAll()")
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        return Mono.just(ResponseEntity.ok(response));
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "1.0.0");
        return Mono.just(ResponseEntity.ok(response));
    }
}
```

## ✅ Вариант 2: Настройка в SecurityConfig

Если у вас много публичных эндпоинтов, можно настроить через `pathMatchers`:

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeExchange(exchanges -> {
            // Публичные эндпоинты (без аутентификации)
            exchanges.pathMatchers(
                "/actuator/**",
                "/api/public/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll();
            
            // Все остальные требуют аутентификации
            // Авторизация через @PreAuthorize на методах
            exchanges.anyExchange().authenticated();
        })
        .httpBasic(httpBasic -> {})
        .build();
}
```

## 📊 Сравнение подходов

| Подход | Когда использовать | Преимущества |
|--------|-------------------|--------------|
| `@PreAuthorize("permitAll()")` | Отдельные публичные методы | Декларативно, видно прямо на методе |
| `pathMatchers(...).permitAll()` | Много публичных эндпоинтов по паттерну | Централизованная настройка |

## 🎯 Рекомендация

**Используйте `@PreAuthorize("permitAll()")`** для публичных эндпоинтов:
- ✅ Декларативно - видно прямо на методе
- ✅ Не нужно менять SecurityConfig при добавлении новых публичных методов
- ✅ Единый подход к безопасности (все через аннотации)

## 📝 Примеры использования в вашем проекте

### Пример 1: Health check endpoint
```java
@RestController
@RequestMapping("/api/public")
public class HealthController {

    @PreAuthorize("permitAll()")
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of("status", "UP")));
    }
}
```

### Пример 2: Публичная информация
```java
@PreAuthorize("permitAll()")
@GetMapping("/api/public/info")
public Mono<ResponseEntity<Map<String, Object>>> getPublicInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put("version", "1.0.0");
    info.put("name", "Security API");
    return Mono.just(ResponseEntity.ok(info));
}
```

## ⚠️ Важно

`permitAll()` - это встроенная функция Spring Security SpEL, она:
- ✅ Разрешает доступ без аутентификации
- ✅ Не требует наличия `authentication` объекта
- ✅ Работает с методной безопасностью для WebFlux
