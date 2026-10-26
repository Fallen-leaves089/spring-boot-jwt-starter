package io.github.fallenleaves089.jwt.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthInterceptorTest {

    private JwtProperties properties;
    private JwtUtil jwtUtil;
    private AuthInterceptor interceptor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-auth-interceptor-0123456789abcdef");
        properties.setExpiration(3600);
        jwtUtil = new JwtUtil(properties);
        interceptor = new AuthInterceptor(jwtUtil, properties, objectMapper);
    }

    @Test
    void missingTokenShouldReturnTokenMissingCode() throws Exception {
        MockHttpServletRequest request = request("/api/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertEquals("TOKEN_MISSING", dataCode(response));
    }

    @Test
    void invalidTokenShouldReturnTokenInvalidCode() throws Exception {
        MockHttpServletRequest request = request("/api/user/profile");
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertEquals("TOKEN_INVALID", dataCode(response));
    }

    @Test
    void expiredTokenShouldReturnTokenExpiredCode() throws Exception {
        properties.setExpiration(-1);
        JwtUtil expiredJwtUtil = new JwtUtil(properties);
        String token = expiredJwtUtil.generateToken(1L, null);
        AuthInterceptor expiredInterceptor = new AuthInterceptor(expiredJwtUtil, properties, objectMapper);
        MockHttpServletRequest request = request("/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(expiredInterceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertEquals("TOKEN_EXPIRED", dataCode(response));
    }

    @Test
    void validTokenShouldInjectUserId() throws Exception {
        String token = jwtUtil.generateToken(42L, null);
        MockHttpServletRequest request = request("/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(42L, request.getAttribute("userId"));
    }

    @Test
    void excludedPathShouldSkipAuthentication() throws Exception {
        properties.setExcludePaths(List.of("/api/login"));
        MockHttpServletRequest request = request("/api/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    private MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private String dataCode(MockHttpServletResponse response) throws Exception {
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        return body.path("data").path("code").asText();
    }
}
