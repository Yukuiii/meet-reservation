package com.example.backend.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.entity.MeetingRoom;
import com.example.backend.entity.Notification;
import com.example.backend.entity.Reservation;
import com.example.backend.entity.ReservationRecommendation;
import com.example.backend.mapper.MeetingRoomMapper;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.mapper.ReservationMapper;
import com.example.backend.mapper.ReservationRecommendationMapper;
import com.example.backend.service.NotificationService;
import com.example.backend.vo.NotificationUnreadCountVO;
import com.example.backend.vo.NotificationVO;
import com.example.backend.vo.ReservationRecommendationVO;

import lombok.RequiredArgsConstructor;

/**
 * 站内通知服务实现。
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int IS_UNREAD = 0;
    private static final int IS_READ = 1;
    private static final int RECOMMENDATION_PENDING = 0;
    private static final int RECOMMENDATION_ACCEPTED = 1;
    private static final int RECOMMENDATION_DECLINED = 2;
    private static final int RESERVATION_PENDING = 0;
    private static final int RESERVATION_APPROVED = 1;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final List<String> TYPE_TEXT_LIST = List.of(
            "系统通知", "紧急占用取消", "审核通过", "审核驳回"
    );

    private final NotificationMapper notificationMapper;
    private final ReservationRecommendationMapper recommendationMapper;
    private final ReservationMapper reservationMapper;
    private final MeetingRoomMapper meetingRoomMapper;

    @Override
    public Long createNotification(Long userId, String title, String content,
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
        return entity.getId();
    }

    @Override
    public List<NotificationVO> listUserNotifications(Long userId) {
        validateUserId(userId);
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)
        );
        Map<Long, ReservationRecommendation> recommendationMap = buildRecommendationMap(list);
        Map<Long, MeetingRoom> roomMap = buildRecommendedRoomMap(recommendationMap);
        return list.stream()
                .map(item -> toNotificationVO(item, recommendationMap.get(item.getId()), roomMap))
                .toList();
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long acceptRecommendation(Long userId, Long recommendationId) {
        validateUserId(userId);
        validateRecommendationId(recommendationId);

        ReservationRecommendation recommendation = recommendationMapper.selectById(recommendationId);
        if (recommendation == null || !userId.equals(recommendation.getUserId())) {
            throw new IllegalArgumentException("改约推荐不存在");
        }
        if (!Integer.valueOf(RECOMMENDATION_PENDING).equals(recommendation.getStatus())) {
            throw new IllegalArgumentException("改约推荐已处理");
        }

        Reservation original = reservationMapper.selectById(recommendation.getOriginalReservationId());
        if (original == null) {
            throw new IllegalArgumentException("原预约不存在");
        }
        MeetingRoom room = meetingRoomMapper.selectById(recommendation.getRecommendedRoomId());
        if (room == null || !Integer.valueOf(1).equals(room.getStatus())) {
            throw new IllegalArgumentException("推荐会议室暂不可用");
        }
        ensureRecommendationSlotAvailable(recommendation);

        int claimed = recommendationMapper.update(
                new ReservationRecommendation(),
                new LambdaUpdateWrapper<ReservationRecommendation>()
                        .eq(ReservationRecommendation::getId, recommendationId)
                        .eq(ReservationRecommendation::getUserId, userId)
                        .eq(ReservationRecommendation::getStatus, RECOMMENDATION_PENDING)
                        .set(ReservationRecommendation::getStatus, RECOMMENDATION_ACCEPTED)
                        .set(ReservationRecommendation::getUpdatedAt, LocalDateTime.now())
        );
        if (claimed != 1) {
            throw new IllegalArgumentException("改约推荐已处理");
        }

        Reservation replacement = new Reservation();
        replacement.setReservationNo(generateReservationNo());
        replacement.setUserId(userId);
        replacement.setRoomId(recommendation.getRecommendedRoomId());
        replacement.setTitle(original.getTitle());
        replacement.setPurpose(original.getPurpose());
        replacement.setAttendeeCount(original.getAttendeeCount());
        replacement.setReservationDate(recommendation.getReservationDate());
        replacement.setStartTime(recommendation.getStartTime());
        replacement.setEndTime(recommendation.getEndTime());
        replacement.setStatus(RESERVATION_PENDING);
        replacement.setReviewerId(null);
        replacement.setReviewedAt(null);
        replacement.setRemark("用户接受紧急占用推荐改约，待管理员审核");
        replacement.setCreatedAt(LocalDateTime.now());
        replacement.setUpdatedAt(LocalDateTime.now());

        int inserted = reservationMapper.insert(replacement);
        if (inserted != 1 || replacement.getId() == null) {
            throw new IllegalStateException("创建改约预约失败，请稍后重试");
        }

        recommendationMapper.update(
                new ReservationRecommendation(),
                new LambdaUpdateWrapper<ReservationRecommendation>()
                        .eq(ReservationRecommendation::getId, recommendationId)
                        .set(ReservationRecommendation::getAcceptedReservationId, replacement.getId())
                        .set(ReservationRecommendation::getUpdatedAt, LocalDateTime.now())
        );
        markNotificationRead(recommendation.getNotificationId(), userId);
        return replacement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean declineRecommendation(Long userId, Long recommendationId) {
        validateUserId(userId);
        validateRecommendationId(recommendationId);
        ReservationRecommendation recommendation = recommendationMapper.selectById(recommendationId);
        if (recommendation == null || !userId.equals(recommendation.getUserId())) {
            return false;
        }
        int rows = recommendationMapper.update(
                new ReservationRecommendation(),
                new LambdaUpdateWrapper<ReservationRecommendation>()
                        .eq(ReservationRecommendation::getId, recommendationId)
                        .eq(ReservationRecommendation::getUserId, userId)
                        .eq(ReservationRecommendation::getStatus, RECOMMENDATION_PENDING)
                        .set(ReservationRecommendation::getStatus, RECOMMENDATION_DECLINED)
                        .set(ReservationRecommendation::getUpdatedAt, LocalDateTime.now())
        );
        if (rows == 1) {
            markNotificationRead(recommendation.getNotificationId(), userId);
            return true;
        }
        return false;
    }

    /**
     * 将通知实体转换为前端展示对象。
     *
     * @param n                 通知实体
     * @param recommendation    改约推荐
     * @param recommendedRoomMap 推荐会议室映射
     * @return 通知展示对象
     */
    private NotificationVO toNotificationVO(Notification n,
                                            ReservationRecommendation recommendation,
                                            Map<Long, MeetingRoom> recommendedRoomMap) {
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
        if (recommendation != null) {
            vo.setRecommendation(toRecommendationVO(
                    recommendation,
                    recommendedRoomMap.get(recommendation.getRecommendedRoomId())
            ));
        }
        return vo;
    }

    /**
     * 构建通知ID到改约推荐的映射。
     *
     * @param notifications 通知列表
     * @return 推荐映射
     */
    private Map<Long, ReservationRecommendation> buildRecommendationMap(List<Notification> notifications) {
        List<Long> notificationIds = notifications.stream()
                .map(Notification::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (notificationIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return recommendationMapper.selectList(
                        new LambdaQueryWrapper<ReservationRecommendation>()
                                .in(ReservationRecommendation::getNotificationId, notificationIds)
                )
                .stream()
                .collect(Collectors.toMap(
                        ReservationRecommendation::getNotificationId,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    /**
     * 构建推荐会议室映射。
     *
     * @param recommendationMap 推荐映射
     * @return 会议室映射
     */
    private Map<Long, MeetingRoom> buildRecommendedRoomMap(Map<Long, ReservationRecommendation> recommendationMap) {
        List<Long> roomIds = recommendationMap.values().stream()
                .map(ReservationRecommendation::getRecommendedRoomId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return meetingRoomMapper.selectBatchIds(roomIds)
                .stream()
                .collect(Collectors.toMap(MeetingRoom::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 将推荐实体转换为展示对象。
     *
     * @param recommendation 推荐实体
     * @param room           推荐会议室
     * @return 推荐展示对象
     */
    private ReservationRecommendationVO toRecommendationVO(ReservationRecommendation recommendation,
                                                           MeetingRoom room) {
        ReservationRecommendationVO vo = new ReservationRecommendationVO();
        vo.setId(recommendation.getId());
        vo.setRoomId(recommendation.getRecommendedRoomId());
        vo.setRoomName(room != null ? room.getName() : "会议室");
        vo.setDate(recommendation.getReservationDate() == null
                ? ""
                : recommendation.getReservationDate().format(DATE_FORMATTER));
        vo.setTimeSlot(recommendation.getStartTime().format(TIME_FORMATTER)
                + "-" + recommendation.getEndTime().format(TIME_FORMATTER));
        vo.setStatus(recommendation.getStatus());
        vo.setStatusText(resolveRecommendationStatusText(recommendation.getStatus()));
        vo.setAcceptedReservationId(recommendation.getAcceptedReservationId());
        return vo;
    }

    /**
     * 校验推荐时段是否仍可预约。
     *
     * @param recommendation 改约推荐
     */
    private void ensureRecommendationSlotAvailable(ReservationRecommendation recommendation) {
        Long conflictCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getRoomId, recommendation.getRecommendedRoomId())
                        .eq(Reservation::getReservationDate, recommendation.getReservationDate())
                        .in(Reservation::getStatus, 0, RESERVATION_APPROVED)
                        .lt(Reservation::getStartTime, recommendation.getEndTime())
                        .gt(Reservation::getEndTime, recommendation.getStartTime())
        );
        if (conflictCount != null && conflictCount > 0) {
            throw new IllegalArgumentException("推荐时段已被占用，请放弃该推荐");
        }
    }

    /**
     * 将推荐关联通知标记为已读。
     *
     * @param notificationId 通知ID
     * @param userId         用户ID
     */
    private void markNotificationRead(Long notificationId, Long userId) {
        if (notificationId == null || notificationId <= 0) {
            return;
        }
        notificationMapper.update(
                new Notification(),
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getUserId, userId)
                        .set(Notification::getIsRead, IS_READ)
                        .set(Notification::getUpdatedAt, LocalDateTime.now())
        );
    }

    /**
     * 生成预约编号。
     *
     * @return 预约编号
     */
    private String generateReservationNo() {
        String timePart = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "R" + timePart + randomPart;
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
     * 解析推荐状态文案。
     *
     * @param status 推荐状态
     * @return 状态文案
     */
    private String resolveRecommendationStatusText(Integer status) {
        if (Integer.valueOf(RECOMMENDATION_ACCEPTED).equals(status)) {
            return "已同意";
        }
        if (Integer.valueOf(RECOMMENDATION_DECLINED).equals(status)) {
            return "已放弃";
        }
        return "待处理";
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
     * 校验推荐ID必须为正整数。
     *
     * @param recommendationId 推荐ID
     */
    private void validateRecommendationId(Long recommendationId) {
        if (recommendationId == null || recommendationId <= 0) {
            throw new IllegalArgumentException("推荐ID不合法");
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
