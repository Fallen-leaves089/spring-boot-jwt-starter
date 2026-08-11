package io.github.fallenleaves089.jwt.starter;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilTest {

    private JwtProperties properties;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-util-0123456789abcdef");
        properties.setExpiration(3600);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void generateTokenShouldBeParsable() {
        String token = jwtUtil.generateToken(1001L, "13800138000");
        assertNotNull(token);

        Claims claims = jwtUtil.parseToken(token);
        assertNotNull(claims);
        assertEquals("1001", claims.getSubject());
        assertEquals("13800138000", claims.get("phone"));
    }

    @Test
    void getUserIdShouldReturnUserId() {
        String token = jwtUtil.generateToken(42L, "13800138000");
        assertEquals(42L, jwtUtil.getUserId(token));
    }

    @Test
    void getUserIdShouldStripBearerPrefix() {
        String token = jwtUtil.generateToken(7L, null);
        assertEquals(7L, jwtUtil.getUserId("Bearer " + token));
    }

    @Test
    void parseTokenShouldReturnNullForInvalidToken() {
        assertNull(jwtUtil.parseToken("not-a-jwt"));
        assertNull(jwtUtil.getUserId("not-a-jwt"));
    }

    @Test
    void expiredTokenShouldBeRejected() throws InterruptedException {
        properties.setExpiration(1);
        JwtUtil shortLived = new JwtUtil(properties);

        String token = shortLived.generateToken(1L, null);
        Thread.sleep(1100);

        assertNull(shortLived.parseToken(token));
    }
}