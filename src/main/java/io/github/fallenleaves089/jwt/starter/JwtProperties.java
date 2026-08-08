package io.github.fallenleaves089.jwt.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT 鉴权配置属性。
 * 所有配置项均以前缀 {@code jwt} 开头，在 application.yml 中配置。
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT 签名密钥（必填，无默认值，生产环境请使用 256 位以上密钥） */
    private String secret;

    /** Token 过期时间（秒），默认 86400（24小时） */
    private long expiration = 86400;

    /** 存放 Token 的 HTTP 请求头名称，默认 Authorization */
    private String header = "Authorization";

    /** Token 前缀，默认 "Bearer "（注意末尾空格） */
    private String tokenPrefix = "Bearer ";

    /** 不需要 JWT 鉴权的路径前缀列表（支持通配符 *） */
    private List<String> excludePaths = new ArrayList<>();

    /** JWT 解析后 userId 注入 request attribute 的 key，默认 "userId" */
    private String userIdAttribute = "userId";

    // ── getters / setters ──

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public String getUserIdAttribute() {
        return userIdAttribute;
    }

    public void setUserIdAttribute(String userIdAttribute) {
        this.userIdAttribute = userIdAttribute;
    }
}
