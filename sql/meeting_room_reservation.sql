-- ============================================
-- 微信小程序会议室预约平台数据库设计
-- 数据库类型：MySQL 8.0+
-- 字符集：utf8mb4
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `meeting_reservation` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `meeting_reservation`;

-- ============================================
-- 1. 用户表
-- 存储微信用户基本信息及角色
-- ============================================
CREATE TABLE `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid` VARCHAR(64) NOT NULL COMMENT '微信OpenID',
    `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID（多平台场景）',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_phone` (`phone`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ============================================
-- 2. 会议室表
-- 存储会议室基本信息
-- ============================================
CREATE TABLE `meeting_room` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
    `name` VARCHAR(64) NOT NULL COMMENT '会议室名称',
    `capacity` INT UNSIGNED NOT NULL COMMENT '容纳人数',
    `location` VARCHAR(128) NOT NULL COMMENT '位置描述（如：A栋3楼301）',
    `building` VARCHAR(32) DEFAULT NULL COMMENT '所属楼栋',
    `floor` VARCHAR(16) DEFAULT NULL COMMENT '所在楼层',
    `description` TEXT DEFAULT NULL COMMENT '会议室描述',
    `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图片URL',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常，2-维护中',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重（越大越靠前）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_capacity` (`capacity`),
    KEY `idx_building_floor` (`building`, `floor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议室表';

-- ============================================
-- 3. 会议室图片表
-- 支持会议室多图展示
-- ============================================
CREATE TABLE `room_image` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `room_id` BIGINT UNSIGNED NOT NULL COMMENT '会议室ID',
    `image_url` VARCHAR(512) NOT NULL COMMENT '图片URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议室图片表';

-- ============================================
-- 4. 设备类型表
-- 定义可用的设备类型
-- ============================================
CREATE TABLE `equipment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `name` VARCHAR(32) NOT NULL COMMENT '设备名称（如：投影仪、白板）',
    `icon` VARCHAR(256) DEFAULT NULL COMMENT '设备图标URL',
    `description` VARCHAR(128) DEFAULT NULL COMMENT '设备描述',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备类型表';

-- ============================================
-- 5. 会议室设备关联表
-- 会议室与设备的多对多关系
-- ============================================
CREATE TABLE `room_equipment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `room_id` BIGINT UNSIGNED NOT NULL COMMENT '会议室ID',
    `equipment_id` BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    `quantity` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '设备数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_equipment` (`room_id`, `equipment_id`),
    KEY `idx_equipment_id` (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议室设备关联表';

-- ============================================
-- 6. 预约表
-- 核心业务表，存储预约信息
-- ============================================
CREATE TABLE `reservation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    `reservation_no` VARCHAR(32) NOT NULL COMMENT '预约编号（业务唯一标识）',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '预约用户ID',
    `room_id` BIGINT UNSIGNED NOT NULL COMMENT '会议室ID',
    `title` VARCHAR(128) NOT NULL COMMENT '会议主题',
    `purpose` TEXT DEFAULT NULL COMMENT '预约事由/会议内容',
    `attendee_count` INT UNSIGNED NOT NULL COMMENT '参与人数',
    `reservation_date` DATE NOT NULL COMMENT '预约日期',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已通过，2-已拒绝，3-已取消，4-已完成',
    `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    `reviewer_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    `cancel_reason` VARCHAR(256) DEFAULT NULL COMMENT '取消原因',
    `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reservation_no` (`reservation_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_status` (`status`),
    KEY `idx_reservation_date` (`reservation_date`),
    KEY `idx_room_date_time` (`room_id`, `reservation_date`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认设备类型
INSERT INTO `equipment` (`name`, `icon`, `description`) VALUES
('投影仪', NULL, '高清投影设备'),
('白板', NULL, '可书写白板'),
('电视', NULL, '大屏显示器'),
('视频会议系统', NULL, '远程视频会议设备'),
('音响系统', NULL, '扩音设备'),
('空调', NULL, '独立空调'),
('电话会议系统', NULL, '多方电话会议设备'),
('无线投屏', NULL, '支持无线投屏');
