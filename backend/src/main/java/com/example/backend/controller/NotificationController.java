package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.service.NotificationService;
import com.example.backend.vo.NotificationUnreadCountVO;
import com.example.backend.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 站内通知控制器。
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 查询用户通知列表。
     *
     * @param userId 用户ID
     * @return 通知列表
     */
    @GetMapping
    public ApiResponse<List<NotificationVO>> listNotifications(@RequestParam Long userId) {
        try {
            return ApiResponse.success(notificationService.listUserNotifications(userId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询通知列表失败");
        }
    }

    /**
     * 查询未读通知数量。
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountVO> getUnreadCount(@RequestParam Long userId) {
        try {
            return ApiResponse.success(notificationService.getUnreadCount(userId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询未读数量失败");
        }
    }

    /**
     * 标记单条通知为已读。
     *
     * @param notificationId 通知ID
     * @param body           请求体（含 userId）
     * @return 统一响应
     */
    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId,
                                        @RequestBody Map<String, Long> body) {
        try {
            Long userId = body.get("userId");
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }
            boolean updated = notificationService.markAsRead(userId, notificationId);
            if (!updated) {
                return ApiResponse.fail("通知不存在或已读");
            }
            return ApiResponse.success("标记已读成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("标记已读失败");
        }
    }

    /**
     * 标记所有通知为已读。
     *
     * @param body 请求体（含 userId）
     * @return 统一响应
     */
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@RequestBody Map<String, Long> body) {
        try {
            Long userId = body.get("userId");
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }
            notificationService.markAllAsRead(userId);
            return ApiResponse.success("全部标记已读成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("全部标记已读失败");
        }
    }

    /**
     * 接受改约推荐。
     *
     * @param recommendationId 推荐ID
     * @param body             请求体（含 userId）
     * @return 新预约ID
     */
    @PostMapping("/recommendations/{recommendationId}/accept")
    public ApiResponse<Long> acceptRecommendation(@PathVariable Long recommendationId,
                                                  @RequestBody Map<String, Long> body) {
        try {
            Long userId = body.get("userId");
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }
            return ApiResponse.success("改约申请已提交", notificationService.acceptRecommendation(userId, recommendationId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("接受改约推荐失败");
        }
    }

    /**
     * 放弃改约推荐。
     *
     * @param recommendationId 推荐ID
     * @param body             请求体（含 userId）
     * @return 统一响应
     */
    @PostMapping("/recommendations/{recommendationId}/decline")
    public ApiResponse<Void> declineRecommendation(@PathVariable Long recommendationId,
                                                   @RequestBody Map<String, Long> body) {
        try {
            Long userId = body.get("userId");
            if (userId == null) {
                return ApiResponse.fail("用户ID不能为空");
            }
            boolean updated = notificationService.declineRecommendation(userId, recommendationId);
            if (!updated) {
                return ApiResponse.fail("改约推荐不存在或已处理");
            }
            return ApiResponse.success("已放弃推荐", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("放弃改约推荐失败");
        }
    }
}
