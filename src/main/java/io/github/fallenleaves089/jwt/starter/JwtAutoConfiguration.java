package io.github.fallenleaves089.jwt.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
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
public class JwtAutoConfiguration {

    /**
     * 将 JwtUtil 暴露为 Bean，方便用户在业务代码中注入以生成 Token。
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil(JwtProperties properties) {
        return new JwtUtil(properties);
    }

    /**
     * 使用容器中的 JwtUtil 构建拦截器，允许用户通过自定义 Bean 替换 Token 实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthInterceptor authInterceptor(JwtUtil jwtUtil, JwtProperties properties,
                                           ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new AuthInterceptor(jwtUtil, properties, objectMapper);
    }

    /**
     * 注册 AuthInterceptor 拦截所有路径（/**）。
     */
    @Bean
    public WebMvcConfigurer jwtInterceptorConfigurer(AuthInterceptor authInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor)
                        .addPathPatterns("/**");
            }
        };
    }
}
