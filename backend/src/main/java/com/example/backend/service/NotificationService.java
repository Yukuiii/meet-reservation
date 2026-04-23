package com.example.backend.service;

import com.example.backend.vo.NotificationUnreadCountVO;
import com.example.backend.vo.NotificationVO;

import java.util.List;

/**
 * 站内通知服务接口。
 */
public interface NotificationService {

    /**
     * 创建并保存一条通知。
     *
     * @param userId        接收通知的用户ID
     * @param title         通知标题
     * @param content       通知内容
     * @param type          通知类型
     * @param reservationId 关联预约ID（可空）
     */
    Long createNotification(Long userId, String title, String content,
                            int type, Long reservationId);

    /**
     * 查询指定用户的通知列表（按时间倒序）。
     *
     * @param userId 用户ID
     * @return 通知列表
     */
    List<NotificationVO> listUserNotifications(Long userId);

    /**
     * 查询指定用户的未读通知数量。
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    NotificationUnreadCountVO getUnreadCount(Long userId);

    /**
     * 将单条通知标记为已读。
     *
     * @param userId         用户ID
     * @param notificationId 通知ID
     * @return 是否实际更新通知
     */
    boolean markAsRead(Long userId, Long notificationId);

    /**
     * 将指定用户的所有通知标记为已读。
     *
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 接受一条改约推荐并创建替代预约。
     *
     * @param userId           用户ID
     * @param recommendationId 推荐ID
     * @return 新预约ID
     */
    Long acceptRecommendation(Long userId, Long recommendationId);

    /**
     * 放弃一条改约推荐。
     *
     * @param userId           用户ID
     * @param recommendationId 推荐ID
     * @return 是否实际更新推荐状态
     */
    boolean declineRecommendation(Long userId, Long recommendationId);
}
