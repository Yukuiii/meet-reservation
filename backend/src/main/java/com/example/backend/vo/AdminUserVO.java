package com.example.backend.vo;

import lombok.Data;

/**
 * 管理员账号视图对象。
 */
@Data
public class AdminUserVO {

    /**
     * 用户ID。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 状态码。
     */
    private Integer status;

    /**
     * 状态文案。
     */
    private String statusText;

    /**
     * 创建时间。
     */
    private String createdAt;
}
