# spring-boot-jwt-starter

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/fallen-leaves089/spring-boot-jwt-starter/ci.yml?branch=main&logo=github)](https://github.com/fallen-leaves089/spring-boot-jwt-starter/actions)

Spring Boot JWT Starter | 白名单 | 路径遍历防护 | 401 原因区分

MIT License. Copyright (c) 2024 fallen-leaves089.

> 需要 JWT 和安全过滤器一起用？使用
> [`spring-boot-security-starter`](https://github.com/fallen-leaves089/spring-boot-security-starter)。

## 30 秒看懂

- **解决什么**：给 Spring MVC 项目加一层轻量 JWT 鉴权，不用自己写拦截器。
- **你能得到**：Token 校验、显式白名单、路径遍历防护、401 原因区分、`userId` 注入。
- **适合**：Spring Boot 3.2.x / Java 17 的 Servlet 应用。
- **不适合**：WebFlux/响应式应用、完整用户体系、分布式 Token 撤销。

## 最短演示

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

可运行的 Controller 和 `curl` 示例见[快速开始](#快速开始)。

---

## 功能

- **零代码接入**：引入依赖，配置 `jwt.secret`，并在 `jwt.exclude-paths` 中列出公开路径
- **自动 Token 校验**：拦截所有请求，白名单路径跳过，其余校验 Authorization 头
- **感知 context-path**：`jwt.exclude-paths` 按应用内相对路径匹配，不需要重复 `server.servlet.context-path`
- **可替换 Token 实现**：用户自定义的 `JwtUtil` Bean 会被拦截器直接复用，不再创建第二个实例
- **路径遍历攻击防护**：最多进行三轮百分号解码，拒绝混合大小写和双重编码的 `..` 路径段
- **密钥启动校验**：`jwt.secret` 缺失或不足 32 字节时启动直接失败，并给出明确提示
- **401 原因区分**：返回 `TOKEN_MISSING`、`TOKEN_INVALID` 或 `TOKEN_EXPIRED`，前端可据此决定是否重新登录
- **userId 注入**：校验通过后将 userId 注入 `request.setAttribute("userId", ...)`
- **JJWT 0.13.x**：使用 JJWT 0.13.x 进行 HMAC-SHA 签名/解析

---

## 依赖坐标

本项目通过 [JitPack](https://jitpack.io) 发布，使用时先添加 JitPack 仓库。

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

> 需要 Spring Boot 3.2.x + Java 17。

---

## 快速开始

### 1. 最小配置

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

配置密钥后即启用鉴权。密钥至少 32 字节，否则启动直接失败。默认不放行任何路径，请显式列出所有公开接口。

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

# 白名单路径（按应用内相对路径匹配，支持 * 通配符，其他字符按字面量处理），默认空；这些路径不校验 Token
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

响应体通过 `data.code` 区分原因：

| 情况 | `data.code` | `data.message` |
|------|-------------|----------------|
| 未携带或格式不匹配的 `Authorization` 头 | `TOKEN_MISSING` | 请先登录 |
| Token 格式错误或无效 | `TOKEN_INVALID` | Token 无效 |
| Token 已过期 | `TOKEN_EXPIRED` | Token 已过期 |

Token 过期时示例：

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

前端收到以上任一 `data.code` 都应引导重新登录；`TOKEN_EXPIRED` 还可用于触发刷新 Token 流程。

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

## 发布

JitPack 会根据 Git tag 自动构建发布。Git tag 使用 `v1.1.2`，上面的依赖坐标仍使用 `1.1.2`。

```bash
git tag v1.1.2
git push origin v1.1.2
```

随后可使用：

```text
https://jitpack.io/#fallen-leaves089/spring-boot-jwt-starter/1.1.2
```

发布到 Maven Central 需要仓库凭据、签名制品以及 source/javadoc jar。

---

## GitHub About 建议

`Spring Boot JWT Starter | 白名单 | 路径遍历防护 | 401 原因区分`
