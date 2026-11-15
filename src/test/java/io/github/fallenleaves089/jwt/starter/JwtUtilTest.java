package io.github.fallenleaves089.jwt.starter;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertFalse(jwtUtil.isExpired(token));
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
        assertFalse(jwtUtil.isExpired("not-a-jwt"));
    }

    @Test
    void expiredTokenShouldBeRejected() throws InterruptedException {
        properties.setExpiration(1);
        JwtUtil shortLived = new JwtUtil(properties);

        String token = shortLived.generateToken(1L, null);
        Thread.sleep(1100);

        assertNull(shortLived.parseToken(token));
        assertTrue(shortLived.isExpired(token));
    }

    @Test
    void missingSecretShouldFailFast() {
        properties.setSecret(null);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new JwtUtil(properties));
        assertTrue(error.getMessage().contains("jwt.secret"));
    }

    @Test
    void shortSecretShouldFailFast() {
        properties.setSecret("too-short");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new JwtUtil(properties));
        assertTrue(error.getMessage().contains("32 bytes"));
    }

    @Test
    void generatedTokenShouldContainConfiguredIssuerAndAudience() {
        JwtUtil configured = newJwtUtil("auth-service", "orders-api");

        String token = configured.generateToken(1001L, null);
        Claims claims = configured.parseToken(token);

        assertNotNull(claims);
        assertEquals("auth-service", claims.getIssuer());
        assertEquals(Set.of("orders-api"), claims.getAudience());
    }

    @Test
    void configuredIdentityShouldRejectTokenForAnotherService() {
        JwtUtil expected = newJwtUtil("auth-service", "orders-api");

        JwtUtil wrongIssuer = newJwtUtil("other-auth-service", "orders-api");
        assertNull(expected.parseToken(wrongIssuer.generateToken(1L, null)));

        JwtUtil wrongAudience = newJwtUtil("auth-service", "other-api");
        assertNull(expected.getUserId(wrongAudience.generateToken(1L, null)));
    }

    @Test
    void configuredIdentityShouldRejectTokenWithoutIssuerOrAudience() {
        JwtUtil expected = newJwtUtil("auth-service", "orders-api");
        String tokenWithoutIdentity = jwtUtil.generateToken(1L, null);

        assertNull(expected.parseToken(tokenWithoutIdentity));
        assertNull(expected.getUserId(tokenWithoutIdentity));
        assertFalse(expected.isExpired(tokenWithoutIdentity));
    }

    private JwtUtil newJwtUtil(String issuer, String audience) {
        JwtProperties configured = new JwtProperties();
        configured.setSecret(properties.getSecret());
        configured.setExpiration(3600);
        configured.setIssuer(issuer);
        configured.setAudience(audience);
        return new JwtUtil(configured);
    }
}
