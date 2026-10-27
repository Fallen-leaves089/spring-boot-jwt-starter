package io.github.fallenleaves089.jwt.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtAutoConfiguration.class)
            .withPropertyValues("jwt.secret=test-secret-key-for-auto-config-0123456789abcdef");

    @Test
    void shouldUseCustomJwtUtilBean() {
        JwtUtil customJwtUtil = mock(JwtUtil.class);
        when(customJwtUtil.getUserId("custom-token")).thenReturn(99L);

        runner.withBean(JwtUtil.class, () -> customJwtUtil).run(context -> {
            AuthInterceptor interceptor = context.getBean(AuthInterceptor.class);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");
            request.addHeader("Authorization", "Bearer custom-token");

            assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
            assertEquals(99L, request.getAttribute("userId"));
        });
    }
}
