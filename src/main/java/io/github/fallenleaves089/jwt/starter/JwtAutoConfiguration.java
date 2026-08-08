package io.github.fallenleaves089.jwt.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JWT 鉴权自动配置类。
 * <p>
 * 通过 Spring Boot 的 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 机制自动加载，用户只需引入依赖并配置 {@code jwt.secret} 即可启用。
 * <p>
 * 可通过 {@code jwt.enabled=false} 关闭整个 JWT 鉴权功能。
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnProperty(prefix = "jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtAutoConfiguration implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public JwtAutoConfiguration(JwtProperties properties) {
        JwtUtil jwtUtil = new JwtUtil(properties);
        ObjectMapper objectMapper = new ObjectMapper();
        this.authInterceptor = new AuthInterceptor(jwtUtil, properties, objectMapper);
    }

    /**
     * 将 JwtUtil 暴露为 Bean，方便用户在业务代码中注入以生成 Token。
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil(JwtProperties properties) {
        return new JwtUtil(properties);
    }

    /**
     * 注册 AuthInterceptor 拦截所有路径（/**）。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**");
    }
}
