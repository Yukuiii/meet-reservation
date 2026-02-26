package com.example.backend.vo;

import lombok.Data;

/**
 * 登录响应数据。
 */
@Data
public class LoginResponse {

    /**
     * 登录令牌。
     */
    private String token;

    /**
     * 登录用户信息。
     */
    private LoginUserInfo userInfo;
}
