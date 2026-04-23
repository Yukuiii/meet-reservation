package com.example.backend.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.entity.Notification;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.service.NotificationService;
import com.example.backend.vo.NotificationUnreadCountVO;
import com.example.backend.vo.NotificationVO;

import lombok.RequiredArgsConstructor;

/**
 * 站内通知服务实现。
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int IS_UNREAD = 0;
    private static final int IS_READ = 1;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final List<String> TYPE_TEXT_LIST = List.of(
            "系统通知", "紧急占用取消", "审核通过", "审核驳回"
    );

    private final NotificationMapper notificationMapper;

    @Override
    public void createNotification(Long userId, String title, String content,
                                   int type, Long reservationId) {
        validateUserId(userId);
        validateType(type);
        String cleanTitle = cleanRequiredText(title, "通知标题不能为空");
        String cleanContent = cleanRequiredText(content, "通知内容不能为空");

        Notification entity = new Notification();
        entity.setUserId(userId);
        entity.setTitle(cleanTitle);
        entity.setContent(cleanContent);
        entity.setType(type);
        entity.setReservationId(reservationId);
        entity.setIsRead(IS_UNREAD);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        notificationMapper.insert(entity);
    }

    @Override
    public List<NotificationVO> listUserNotifications(Long userId) {
        validateUserId(userId);
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)
        );
        return list.stream().map(this::toNotificationVO).toList();
    }

    @Override
    public NotificationUnreadCountVO getUnreadCount(Long userId) {
        validateUserId(userId);
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, IS_UNREAD)
        );
        NotificationUnreadCountVO vo = new NotificationUnreadCountVO();
        vo.setCount(count);
        return vo;
    }

    @Override
    public boolean markAsRead(Long userId, Long notificationId) {
        validateUserId(userId);
        validateNotificationId(notificationId);
        int rows = notificationMapper.update(
                new Notification(),
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, IS_UNREAD)
                        .set(Notification::getIsRead, IS_READ)
                        .set(Notification::getUpdatedAt, LocalDateTime.now())
        );
        return rows == 1;
    }

    @Override
    public void markAllAsRead(Long userId) {
        validateUserId(userId);
        notificationMapper.update(
                new Notification(),
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, IS_UNREAD)
                        .set(Notification::getIsRead, IS_READ)
                        .set(Notification::getUpdatedAt, LocalDateTime.now())
        );
    }

    /**
     * 将通知实体转换为前端展示对象。
     *
     * @param n 通知实体
     * @return 通知展示对象
     */
    private NotificationVO toNotificationVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setType(n.getType());
        vo.setTypeText(resolveTypeText(n.getType()));
        vo.setReservationId(n.getReservationId());
        vo.setIsRead(n.getIsRead() != null && n.getIsRead() == IS_READ);
        if (n.getCreatedAt() != null) {
            vo.setCreatedAt(n.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
        return vo;
    }

    /**
     * 根据通知类型解析展示文案。
     *
     * @param type 通知类型
     * @return 通知类型文案
     */
    private String resolveTypeText(Integer type) {
        if (type == null || type < 0 || type >= TYPE_TEXT_LIST.size()) {
            return "系统通知";
        }
        return TYPE_TEXT_LIST.get(type);
    }

    /**
     * 校验用户ID必须为正整数。
     *
     * @param userId 用户ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }
    }

    /**
     * 校验通知ID必须为正整数。
     *
     * @param notificationId 通知ID
     */
    private void validateNotificationId(Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new IllegalArgumentException("通知ID不合法");
        }
    }

    /**
     * 校验通知类型必须属于已定义范围。
     *
     * @param type 通知类型
     */
    private void validateType(int type) {
        if (type < 0 || type >= TYPE_TEXT_LIST.size()) {
            throw new IllegalArgumentException("通知类型不合法");
        }
    }

    /**
     * 清理并校验必填文本。
     *
     * @param value        原始文本
     * @param errorMessage 错误提示
     * @return 清理后的文本
     */
    private String cleanRequiredText(String value, String errorMessage) {
        String cleanValue = value == null ? "" : value.trim();
        if (!StringUtils.hasText(cleanValue)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return cleanValue;
    }
}
