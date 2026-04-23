-- ============================================
-- 会议室预约平台数据库设计
-- 数据库类型：MySQL 8.0+
-- 字符集：utf8mb4
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `meeting` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `meeting`;

-- ============================================
-- 1. 用户表
-- 存储用户基本信息及角色
-- ============================================
CREATE TABLE `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(64) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `role` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
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
-- 3. 设备类型表
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
-- 4. 会议室设备关联表
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
-- 5. 预约表
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

-- 插入默认设备类型（可重复执行）
INSERT INTO `equipment` (`id`, `name`, `icon`, `description`, `status`) VALUES
(1, '投影仪', NULL, '高清投影设备', 1),
(2, '白板', NULL, '可书写白板', 1),
(3, '电视', NULL, '大屏显示器', 1),
(4, '视频会议系统', NULL, '远程视频会议设备', 1),
(5, '音响系统', NULL, '扩音设备', 1),
(6, '空调', NULL, '独立空调', 1),
(7, '电话会议系统', NULL, '多方电话会议设备', 1),
(8, '无线投屏', NULL, '支持无线投屏', 1)
ON DUPLICATE KEY UPDATE
`name` = VALUES(`name`),
`icon` = VALUES(`icon`),
`description` = VALUES(`description`),
`status` = VALUES(`status`);

-- 插入初始用户（密码均为SHA-256，明文：admin123 / 123456）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `role`, `status`) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '系统管理员', '13800000001', 'admin@meeting.com', 1, 1),
(2, 'zhangsan', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张三', '13800000002', 'zhangsan@meeting.com', 0, 1),
(3, 'lisi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '李四', '13800000003', 'lisi@meeting.com', 0, 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`nickname` = VALUES(`nickname`),
`phone` = VALUES(`phone`),
`email` = VALUES(`email`),
`role` = VALUES(`role`),
`status` = VALUES(`status`);

-- 插入初始会议室
INSERT INTO `meeting_room` (`id`, `name`, `capacity`, `location`, `building`, `floor`, `description`, `cover_image`, `status`, `sort_order`) VALUES
(1, '第一会议室', 8, 'A栋3楼301室', 'A', '3', '适合小型团队例会，配备基础投影设备。', '/images/rooms/room-1.jpg', 1, 100),
(2, '第二会议室', 15, 'A栋5楼502室', 'A', '5', '中型会议室，适合跨团队评审。', '/images/rooms/room-2.jpg', 1, 90),
(3, '大型报告厅', 50, 'B栋1楼101室', 'B', '1', '大型宣讲与培训场地，支持远程会议。', '/images/rooms/room-3.jpg', 1, 120),
(4, '小型洽谈室', 6, 'B栋2楼205室', 'B', '2', '适合商务洽谈及一对一会议。', '/images/rooms/room-4.jpg', 1, 80),
(5, '多功能会议室', 25, 'C栋3楼308室', 'C', '3', '支持多种布局，适配培训、讨论与评审。', '/images/rooms/room-5.jpg', 1, 95),
(6, '培训室', 30, 'C栋4楼401室', 'C', '4', '适合部门培训和工作坊活动。', '/images/rooms/room-6.jpg', 1, 85)
ON DUPLICATE KEY UPDATE
`name` = VALUES(`name`),
`capacity` = VALUES(`capacity`),
`location` = VALUES(`location`),
`building` = VALUES(`building`),
`floor` = VALUES(`floor`),
`description` = VALUES(`description`),
`cover_image` = VALUES(`cover_image`),
`status` = VALUES(`status`),
`sort_order` = VALUES(`sort_order`);

-- 插入会议室设备关联
INSERT INTO `room_equipment` (`id`, `room_id`, `equipment_id`, `quantity`) VALUES
(1, 1, 1, 1),
(2, 1, 2, 1),
(3, 2, 1, 1),
(4, 2, 4, 1),
(5, 2, 5, 1),
(6, 3, 1, 1),
(7, 3, 2, 2),
(8, 3, 4, 1),
(9, 3, 5, 1),
(10, 4, 2, 1),
(11, 5, 1, 1),
(12, 5, 4, 1),
(13, 5, 5, 1),
(14, 6, 1, 1),
(15, 6, 2, 1),
(16, 6, 5, 1)
ON DUPLICATE KEY UPDATE
`room_id` = VALUES(`room_id`),
`equipment_id` = VALUES(`equipment_id`),
`quantity` = VALUES(`quantity`);

-- 插入预约示例数据（用于演示待审核/已通过/已取消）
INSERT INTO `reservation` (
    `id`, `reservation_no`, `user_id`, `room_id`, `title`, `purpose`, `attendee_count`,
    `reservation_date`, `start_time`, `end_time`, `status`,
    `reject_reason`, `reviewer_id`, `reviewed_at`, `cancel_reason`, `cancelled_at`, `remark`
) VALUES
(1, 'R202602260001AA', 2, 1, '项目晨会', '项目周会讨论迭代进度', 6, CURDATE(), '09:00:00', '10:00:00', 1, NULL, 1, NOW(), NULL, NULL, '管理员已通过'),
(2, 'R202602260002BB', 2, 1, '技术评审', '新版本技术方案评审', 8, CURDATE(), '14:00:00', '15:00:00', 0, NULL, NULL, NULL, NULL, NULL, '待管理员审核'),
(3, 'R202602260003CC', 3, 2, '客户沟通会', '与客户确认需求细节', 10, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '12:00:00', 1, NULL, 1, NOW(), NULL, NULL, '管理员已通过'),
(4, 'R202602260004DD', 3, 3, '部门培训', '内部培训活动', 30, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '15:00:00', '17:00:00', 3, NULL, NULL, NULL, '用户临时有事取消', NOW(), '已取消')
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`room_id` = VALUES(`room_id`),
`title` = VALUES(`title`),
`purpose` = VALUES(`purpose`),
`attendee_count` = VALUES(`attendee_count`),
`reservation_date` = VALUES(`reservation_date`),
`start_time` = VALUES(`start_time`),
`end_time` = VALUES(`end_time`),
`status` = VALUES(`status`),
`reject_reason` = VALUES(`reject_reason`),
`reviewer_id` = VALUES(`reviewer_id`),
`reviewed_at` = VALUES(`reviewed_at`),
`cancel_reason` = VALUES(`cancel_reason`),
`cancelled_at` = VALUES(`cancelled_at`),
`remark` = VALUES(`remark`);

-- ============================================
-- 站内通知表
-- ============================================
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收通知的用户ID',
    `title` VARCHAR(128) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(1024) NOT NULL COMMENT '通知内容',
    `type` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '通知类型：0-系统通知，1-紧急占用取消，2-审核通过，3-审核驳回',
    `reservation_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联预约ID',
    `is_read` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_type` (`type`),
    KEY `idx_reservation_id` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='站内通知表';
