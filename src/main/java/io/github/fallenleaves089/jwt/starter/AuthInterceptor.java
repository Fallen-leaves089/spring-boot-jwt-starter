package io.github.fallenleaves089.jwt.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

/**
 * JWT 鉴权拦截器 —— 拦截所有未在白名单中的请求，校验 Authorization 头中的 Token。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>路径遍历攻击防护（拒绝含 ../ 的路径）</li>
 *   <li>前缀匹配白名单（支持 * 通配符）</li>
 *   <li>OPTIONS 预检请求自动放行</li>
 *   <li>Token 校验失败返回 401 + TOKEN_EXPIRED 标识</li>
 *   <li>校验通过后将 userId 注入 request attribute</li>
 * </ul>
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, JwtProperties properties, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();

        // ── 路径遍历攻击防护 ──
        if (path.contains("../") || path.contains("..\\") || path.contains("%2e%2e")) {
            response.setStatus(400);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Map.of("code", 400, "msg", "非法请求路径")));
            return false;
        }

        // ── OPTIONS 预检直接放行 ──
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ── 白名单匹配 ──
        if (isExcluded(path)) {
            return true;
        }

        // ── 验证 Token ──
        String authHeader = request.getHeader(properties.getHeader());
        if (authHeader == null || !authHeader.startsWith(properties.getTokenPrefix())) {
            write401(response, "未登录或Token已过期");
            return false;
        }

        String token = authHeader.substring(properties.getTokenPrefix().length()).trim();
        if (token.isEmpty()) {
            write401(response, "Token不能为空");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            write401(response, "Token无效或已过期");
            return false;
        }

        // 注入 userId 到 request attribute
        request.setAttribute(properties.getUserIdAttribute(), userId);
        return true;
    }

    /** 判断路径是否在白名单中 */
    private boolean isExcluded(String path) {
        List<String> excludePaths = properties.getExcludePaths();
        for (String pattern : excludePaths) {
            if (pattern.contains("*")) {
                String regex = pattern.replace(".", "\\.").replace("*", ".*");
                if (path.matches(regex)) {
                    return true;
                }
            } else if (path.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    /** 写入 401 响应，附带 TOKEN_EXPIRED 标识 */
    private void write401(HttpServletResponse response, String message) throws Exception {
        TokenExpiredData data = new TokenExpiredData();
        response.setStatus(401);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", 401, "msg", message, "data", Map.of(
                        "code", data.getCode(),
                        "message", data.getMessage()))));
    }
}
