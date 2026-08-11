# spring-boot-jwt-starter

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-Fallen-leaves089%2Fspring--boot--jwt--starter-lightgrey?logo=github)](https://github.com/Fallen-leaves089/spring-boot-jwt-starter)
[![Build](https://img.shields.io/github/actions/workflow/status/Fallen-leaves089/spring-boot-jwt-starter/ci.yml?branch=main&logo=github)](https://github.com/Fallen-leaves089/spring-boot-jwt-starter/actions)

Spring Boot JWT Starter | 白名单 | 路径遍历防护 | Token 过期区分

MIT License. Copyright (c) 2024 Fallen-leaves089.

---

## 功能

- **零代码接入**：引入依赖 + 配置 `jwt.secret` 即可启用
- **自动 Token 校验**：拦截所有请求，白名单路径跳过，其余校验 Authorization 头
- **路径遍历攻击防护**：拒绝含 `../` 或 `..\\` 的恶意路径
- **过期区分**：401 响应中附带 `TOKEN_EXPIRED` 标识，前端可据此区分"未登录"和"Token 过期"
- **userId 注入**：校验通过后将 userId 注入 `request.setAttribute("userId", ...)`
- **JJWT 0.12.x**：使用最新版 JJWT，密钥自动 HMAC-SHA

---

## 依赖坐标

### Maven

```xml
<dependency>
    <groupId>io.github.fallenleaves089</groupId>
    <artifactId>spring-boot-jwt-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.fallenleaves089:spring-boot-jwt-starter:1.0.0'
```

> 需要 Spring Boot 3.2.x + Java 17。

---

## 快速开始

### 1. 最小配置

```yaml
# application.yml
jwt:
  secret: "your-256-bit-secret-key-at-least-32-characters-long"
```

仅此一行，即可启用 JWT 鉴权。默认白名单路径：
- `/api/login`
- `/api/register`
- `/api/sms/*`
- `/swagger**`
- `/v3/**`

### 2. 生成 Token

在 Controller 中注入 `JwtUtil` 即可生成 Token：

```java
@RestController
public class LoginController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        // 校验用户名密码...
        Long userId = 1001L;
        String phone = "13800138000";

        String token = jwtUtil.generateToken(userId, phone);
        return Map.of("code", 200, "data", Map.of("token", token));
    }
}
```

### 3. 获取当前用户

在 Controller 中通过 request attribute 获取：

```java
@GetMapping("/api/user/profile")
public Map<String, Object> profile(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    // 查询用户信息...
    return Map.of("code", 200, "data", user);
}
```

### 4. 用 curl 验证

```bash
# 登录获取 Token
curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 携带 Token 访问受保护接口
curl -s http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer <上一步返回的 token>"

# 未携带 Token 时返回 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/user/profile
```

---

## 完整配置参考

```yaml
jwt:
  # 是否启用 JWT 鉴权，默认 true
  enabled: true

  # JWT 签名密钥（必填，建议 256 位以上）
  secret: "your-256-bit-secret-key-at-least-32-characters-long"

  # Token 过期时间（秒），默认 86400（24 小时）
  expiration: 86400

  # 存放 Token 的请求头，默认 Authorization
  header: "Authorization"

  # Token 前缀，默认 "Bearer "
  token-prefix: "Bearer "

  # userId 在 request attribute 中的 key，默认 "userId"
  user-id-attribute: "userId"

  # 白名单路径（支持 * 通配符），这些路径不校验 Token
  exclude-paths:
    - /api/login
    - /api/register
    - /api/sms/*
    - /swagger**
    - /v3/**
    - /actuator/health
```

---

## 401 响应格式

未登录或 Token 无效/过期时返回：

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

前端可根据 `data.code == "TOKEN_EXPIRED"` 判断是否需要引导用户重新登录。

---

## 架构说明

```
spring-boot-jwt-starter
├── JwtProperties            -- @ConfigurationProperties，统一管理所有配置
├── JwtUtil                  -- Token 生成/解析/验证工具
├── AuthInterceptor          -- HandlerInterceptor：路径白名单 + Token 校验 + 路径遍历防护
├── TokenExpiredData         -- Token 过期信息 POJO
└── JwtAutoConfiguration     -- @AutoConfiguration，自动装配 + 注册拦截器
```

通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 机制自动加载，无需 `@ComponentScan`。

---

## GitHub About 建议

`Spring Boot JWT Starter | 白名单 | 路径遍历防护 | Token 过期区分`
