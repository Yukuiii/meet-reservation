package com.example.backend.service.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * 用户账号校验与安全工具。
 */
public final class UserAccountSupport {

    /**
     * 中国大陆手机号格式。
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private UserAccountSupport() {
    }

    /**
     * 校验用户名。
     *
     * @param username 用户名
     */
    public static void validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (username.length() < 2 || username.length() > 20) {
            throw new IllegalArgumentException("用户名长度需在2-20位之间");
        }
    }

    /**
     * 校验手机号。
     *
     * @param phone 手机号
     */
    public static void validatePhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
    }

    /**
     * 校验密码。
     *
     * @param password 密码
     */
    public static void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new IllegalArgumentException("密码长度需在6-20位之间");
        }
    }

    /**
     * 对密码进行 SHA-256 加密。
     *
     * @param rawPassword 原始密码
     * @return 加密后的16进制字符串
     */
    public static String encryptPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            // 将每个字节转为两位十六进制，确保长度固定为64位。
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }
}
