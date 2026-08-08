package io.github.fallenleaves089.jwt.starter;

/**
 * Token 过期时的附加信息，嵌入 401 响应的 data 字段。
 * 前端可据此区分"未登录"和"Token 已过期"。
 */
public class TokenExpiredData {

    private final String code;
    private final String message;

    public TokenExpiredData() {
        this("TOKEN_EXPIRED", "Token已过期");
    }

    public TokenExpiredData(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
