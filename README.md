# spring-boot-jwt-starter

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/fallen-leaves089/spring-boot-jwt-starter/ci.yml?branch=main&logo=github)](https://github.com/fallen-leaves089/spring-boot-jwt-starter/actions)

Spring Boot JWT Starter | Path whitelist | Path traversal protection | Distinct 401 reasons

MIT License. Copyright (c) 2024 fallen-leaves089.

[中文说明](README.zh-CN.md)

> Need JWT and the security filters together? Use
> [`spring-boot-security-starter`](https://github.com/fallen-leaves089/spring-boot-security-starter).

## 30-second overview

- **Problem solved:** adds JWT authentication to a Spring MVC app without writing an interceptor.
- **What you get:** token validation, explicit path whitelist, path-traversal rejection, distinct 401 reasons, and `userId` injection.
- **Use it when:** you have a Spring Boot 3.2.x / Java 17 servlet app and want a small JWT layer.
- **Not for:** WebFlux/reactive apps, a complete user/login system, or distributed token revocation.

## Shortest demo

```yaml
jwt:
  secret: "${JWT_SECRET:replace-with-a-32-byte-secret}"
  exclude-paths:
    - /api/login
```

```java
String token = jwtUtil.generateToken(1001L, "13800138000");
Long userId = (Long) request.getAttribute("userId");
```

See [Quick start](#quick-start) for a runnable controller and `curl` example.

---

## Features

- **Zero-code integration**: add the dependency, set `jwt.secret`, and list public paths in `jwt.exclude-paths`.
- **Automatic token validation**: intercepts all requests, skips whitelisted paths, and validates the `Authorization` header for the rest.
- **Context-path aware**: `jwt.exclude-paths` entries are matched against the application-relative path, so they do not repeat `server.servlet.context-path`.
- **Customizable token logic**: a user-defined `JwtUtil` bean is reused by the interceptor instead of creating a second instance.
- **Path traversal protection**: rejects dot-dot path segments after up to three rounds of percent-decoding, including mixed-case and double-encoded forms.
- **Secret validation**: startup fails fast with a clear message when `jwt.secret` is missing or shorter than 32 bytes.
- **Distinct 401 reasons**: returns `TOKEN_MISSING`, `TOKEN_INVALID`, or `TOKEN_EXPIRED` so clients can decide whether to log in again.
- **User ID injection**: after validation, injects `userId` into `request.setAttribute("userId", ...)`.
- **JJWT 0.13.x**: HMAC-SHA token signing and parsing.

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
    <version>1.1.2</version>
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
    implementation("com.github.fallen-leaves089:spring-boot-jwt-starter:1.1.2")
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
  exclude-paths:
    - /api/login
    - /api/register
    - /api/sms/*
    - /swagger**
    - /v3/**
```

Authentication is enabled after setting the secret. The secret must be at least 32 bytes; startup fails fast otherwise. No path is excluded by default; list every public endpoint explicitly.

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

# Whitelisted paths relative to the application context (supports * wildcards;
# all other characters are literal). Default: empty. These paths skip token validation.
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

The response body uses `data.code` to distinguish the reason:

| Situation | `data.code` | `data.message` |
|-----------|-------------|----------------|
| Missing or unmatched `Authorization` header | `TOKEN_MISSING` | Please log in |
| Malformed or invalid token | `TOKEN_INVALID` | Token is invalid |
| Expired token | `TOKEN_EXPIRED` | Token has expired |

Example for an expired token:

```json
{
  "code": 401,
  "msg": "Token已过期",
  "data": {
    "code": "TOKEN_EXPIRED",
    "message": "Token已过期"
  }
}
```

Clients should redirect to login for any of these values; `TOKEN_EXPIRED` additionally indicates that a refresh-token flow may be useful.

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

JitPack builds a release automatically from a Git tag. Use the `v` prefix for the tag; the dependency coordinate above remains `1.1.2`.

```bash
git tag v1.1.2
git push origin v1.1.2
```

Then use:

```text
https://jitpack.io/#fallen-leaves089/spring-boot-jwt-starter/1.1.2
```

Publishing to Maven Central requires repository credentials, signed artifacts, and source/javadoc jars.

---

## GitHub About

`Spring Boot JWT Starter | Path whitelist | Path traversal protection | Distinct 401 reasons`
