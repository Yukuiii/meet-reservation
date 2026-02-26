package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体，对应 user 表。
 */
@Data
@TableName("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名。
     */
    @TableField("username")
    private String username;

    /**
     * 密码。
     */
    @TableField("password")
    private String password;

    /**
     * 用户昵称。
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL。
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 手机号。
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱。
     */
    @TableField("email")
    private String email;

    /**
     * 角色：0-普通用户，1-管理员。
     */
    @TableField("role")
    private Integer role;

    /**
     * 状态：0-禁用，1-正常。
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
