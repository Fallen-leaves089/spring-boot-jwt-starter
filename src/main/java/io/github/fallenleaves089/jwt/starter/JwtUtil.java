package io.github.fallenleaves089.jwt.starter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 —— 负责 Token 的生成、解析和 userId 提取。
 * 由 {@link JwtAutoConfiguration} 自动创建并注入。
 */
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret must be configured");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = properties.getExpiration() * 1000L;
    }

    /**
     * 生成 JWT Token。
     *
     * @param userId 用户 ID
     * @param phone  手机号（可选，存入 claims）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String phone) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token 并返回 Claims。解析失败返回 null。
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 判断 Token 是否已过期。
     * 无效 Token 和无法解析的 Token 返回 false，由调用方按“无效”处理。
     */
    public boolean isExpired(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(stripPrefix(token));
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 Token 中提取 userId。
     * 如果 Token 带有 Bearer 前缀会自动剥离。
     *
     * @param token JWT 字符串（可含 Bearer 前缀）
     * @return userId，解析失败返回 null
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(stripPrefix(token));
        if (claims != null) {
            String subject = claims.getSubject();
            if (subject == null) {
                return null;
            }
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 去掉 Token 的 Bearer 前缀（如果存在）。
     */
    private String stripPrefix(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
