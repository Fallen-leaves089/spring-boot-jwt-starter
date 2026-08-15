# spring-boot-jwt-starter

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-fallen-leaves089%2Fspring--boot--jwt--starter-lightgrey?logo=github)](https://github.com/fallen-leaves089/spring-boot-jwt-starter)
[![Build](https://img.shields.io/github/actions/workflow/status/fallen-leaves089/spring-boot-jwt-starter/ci.yml?branch=main&logo=github)](https://github.com/fallen-leaves089/spring-boot-jwt-starter/actions)

Spring Boot JWT Starter | Path whitelist | Path traversal protection | Expired-token signaling

MIT License. Copyright (c) 2024 fallen-leaves089.

[中文说明](README.zh-CN.md)

---

## Features

- **Zero-code integration**: add the dependency and configure `jwt.secret` to enable authentication.
- **Automatic token validation**: intercepts all requests, skips whitelisted paths, and validates the `Authorization` header for the rest.
- **Path traversal protection**: rejects malicious paths containing `../` or `..\\`.
- **Expired-token signaling**: attaches a `TOKEN_EXPIRED` marker to 401 responses so clients can distinguish "not logged in" from "token expired".
- **User ID injection**: after validation, injects `userId` into `request.setAttribute("userId", ...)`.
- **JJWT 0.12.x**: uses the latest JJWT version with automatic HMAC-SHA signing.

---

## Dependency coordinates

This project is published through [JitPack](https://jitpack.io). Add the JitPack repository first.

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.fallen-leaves089</groupId>
    <artifactId>spring-boot-jwt-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.fallen-leaves089:spring-boot-jwt-starter:1.0.0")
}
```

> Requires Spring Boot 3.2.x and Java 17.

---

## Quick start

### 1. Minimal configuration

```yaml
# application.yml
jwt:
  secret: "your-256-bit-secret-key-at-least-32-characters-long"
```

This single setting enables JWT authentication. Default whitelisted paths:

- `/api/login`
- `/api/register`
- `/api/sms/*`
- `/swagger**`
- `/v3/**`

### 2. Generate a token

Inject `JwtUtil` into a controller to generate tokens:

```java
@RestController
public class LoginController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        // Validate username and password...
        Long userId = 1001L;
        String phone = "13800138000";

        String token = jwtUtil.generateToken(userId, phone);
        return Map.of("code", 200, "data", Map.of("token", token));
    }
}
```

### 3. Get the current user

Read the user ID from the request attribute in a controller:

```java
@GetMapping("/api/user/profile")
public Map<String, Object> profile(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    // Load user information...
    return Map.of("code", 200, "data", user);
}
```

### 4. Verify with curl

```bash
# Log in and obtain a token
curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# Access a protected endpoint with the token
curl -s http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer <token from the previous step>"

# A request without a token returns 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/user/profile
```

---

## Full configuration reference

```yaml
jwt:
  # Whether JWT authentication is enabled. Default: true.
  enabled: true

  # JWT signing secret. Required. Use a 256-bit or stronger key.
  secret: "your-256-bit-secret-key-at-least-32-characters-long"

  # Token expiration in seconds. Default: 86400 (24 hours).
  expiration: 86400

  # HTTP header that stores the token. Default: Authorization.
  header: "Authorization"

  # Token prefix. Default: "Bearer ".
  token-prefix: "Bearer "

  # Request attribute key for userId. Default: "userId".
  user-id-attribute: "userId"

  # Whitelisted paths (supports * wildcards). These paths skip token validation.
  exclude-paths:
    - /api/login
    - /api/register
    - /api/sms/*
    - /swagger**
    - /v3/**
    - /actuator/health
```

---

## 401 response format

When a request is unauthenticated, or the token is invalid/expired, the response is:

```json
{
  "code": 401,
  "msg": "Token无效或已过期",
  "data": {
    "code": "TOKEN_EXPIRED",
    "message": "Token已过期"
  }
}
```

Clients can check `data.code == "TOKEN_EXPIRED"` to decide whether the user should be redirected to log in again.

---

## Architecture

```
spring-boot-jwt-starter
├── JwtProperties            -- @ConfigurationProperties; manages all configuration
├── JwtUtil                  -- Token generation, parsing, and validation utility
├── AuthInterceptor          -- HandlerInterceptor: path whitelist + token validation + path traversal protection
├── TokenExpiredData         -- POJO for token-expired information
└── JwtAutoConfiguration     -- @AutoConfiguration; auto-configures and registers the interceptor
```

Loaded automatically through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`; no `@ComponentScan` required.

---

## Releasing

JitPack builds a release automatically from a Git tag.

```bash
git tag 1.0.0
git push origin 1.0.0
```

Then use:

```text
https://jitpack.io/#fallen-leaves089/spring-boot-jwt-starter/1.0.0
```

Maven Central publishing requires OSSRH credentials, signed artifacts, and source/javadoc jars.

---

## GitHub About

`Spring Boot JWT Starter | Path whitelist | Path traversal protection | Expired-token signaling`
