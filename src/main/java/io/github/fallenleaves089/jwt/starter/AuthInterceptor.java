package io.github.fallenleaves089.jwt.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * JWT 鉴权拦截器 —— 拦截所有未在白名单中的请求，校验 Authorization 头中的 Token。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>路径遍历攻击防护（拒绝含 ../ 的路径）</li>
 *   <li>前缀匹配白名单（支持 * 通配符）</li>
 *   <li>OPTIONS 预检请求自动放行</li>
 *   <li>Token 校验失败时按原因返回 TOKEN_MISSING / TOKEN_INVALID / TOKEN_EXPIRED</li>
 *   <li>校验通过后将 userId 注入 request attribute</li>
 * </ul>
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final Pattern DOT_DOT_SEGMENT = Pattern.compile("(^|[/\\\\])\\.\\.([/\\\\]|$)");

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
        if (containsTraversal(path)) {
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
            write401(response, "未登录", "TOKEN_MISSING", "请先登录");
            return false;
        }

        String token = authHeader.substring(properties.getTokenPrefix().length()).trim();
        if (token.isEmpty()) {
            write401(response, "Token不能为空", "TOKEN_INVALID", "Token无效");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            boolean expired = jwtUtil.isExpired(token);
            write401(response,
                    expired ? "Token已过期" : "Token无效",
                    expired ? "TOKEN_EXPIRED" : "TOKEN_INVALID",
                    expired ? "Token已过期" : "Token无效");
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
                if (matchesWildcard(pattern, path)) {
                    return true;
                }
            } else if (path.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rejects dot-dot path segments after up to three rounds of percent-decoding.
     * This covers mixed-case encodings and double-encoded traversal attempts while
     * leaving other percent-encoded characters alone.
     */
    private boolean containsTraversal(String path) {
        String current = path;
        for (int i = 0; i < 3; i++) {
            if (current.indexOf('\0') >= 0 || DOT_DOT_SEGMENT.matcher(current).find()) {
                return true;
            }
            String decoded;
            try {
                decoded = UriUtils.decode(current, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                return true;
            }
            if (decoded.equals(current)) {
                return false;
            }
            current = decoded;
        }
        return DOT_DOT_SEGMENT.matcher(current).find();
    }

    /**
     * Matches a whitelist pattern where {@code *} is the only wildcard.
     * All other characters are treated literally, including regex metacharacters.
     */
    private boolean matchesWildcard(String pattern, String path) {
        String[] parts = pattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(parts[i]));
        }
        return path.matches(regex.toString());
    }

    /** 写入 401 响应，data.code 标明失败原因 */
    private void write401(HttpServletResponse response, String message,
                          String code, String dataMessage) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", 401, "msg", message, "data", Map.of(
                        "code", code,
                        "message", dataMessage))));
    }
}
