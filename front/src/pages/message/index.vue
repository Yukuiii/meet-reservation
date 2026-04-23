<template>
  <view class="container">
    <view class="message-header">
      <view class="header-title">
        <text class="title-text">消息</text>
        <text class="unread-text" v-if="unreadCount > 0">{{ unreadCount }} 条未读</text>
      </view>
      <button
        class="read-all-btn"
        :class="{ disabled: !hasUnread || markingAll }"
        :disabled="!hasUnread || markingAll"
        @click="markAllAsRead"
      >
        全部已读
      </button>
    </view>

    <scroll-view class="message-list" scroll-y>
      <view class="loading-state" v-if="loading">
        <text>消息加载中...</text>
      </view>

      <view
        class="message-card"
        :class="{ unread: !item.isRead }"
        v-for="item in notifications"
        :key="item.id"
        @click="markAsRead(item)"
      >
        <view class="message-main">
          <view class="card-title-row">
            <view class="unread-dot" v-if="!item.isRead"></view>
            <text class="card-title">{{ item.title }}</text>
          </view>
          <text class="card-content">{{ item.content }}</text>
          <view class="recommendation-box" v-if="item.recommendation">
            <view class="recommendation-title">
              <text>推荐改约</text>
              <text class="recommendation-status">{{ item.recommendation.statusText }}</text>
            </view>
            <text class="recommendation-line">
              {{ item.recommendation.roomName }} · {{ item.recommendation.date }} {{ item.recommendation.timeSlot }}
            </text>
            <view class="recommendation-actions" v-if="item.recommendation.status === 0">
              <button
                class="recommendation-btn accept"
                size="mini"
                :disabled="handlingRecommendationId === item.recommendation.id"
                @click.stop="acceptRecommendation(item.recommendation)"
              >
                同意改约
              </button>
              <button
                class="recommendation-btn decline"
                size="mini"
                :disabled="handlingRecommendationId === item.recommendation.id"
                @click.stop="declineRecommendation(item.recommendation)"
              >
                放弃
              </button>
            </view>
          </view>
          <view class="card-footer">
            <text class="type-tag" :class="getTypeClass(item.type)">{{ item.typeText }}</text>
            <text class="created-at">{{ item.createdAt }}</text>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="!loading && notifications.length === 0">
        <text>暂无消息</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '../../utils/request'
import { refreshNotificationBadge } from '../../utils/notification'

/**
 * 通知列表。
 */
const notifications = ref([])

/**
 * 列表加载状态。
 */
const loading = ref(false)

/**
 * 全部已读提交状态。
 */
const markingAll = ref(false)

/**
 * 当前处理中的推荐ID。
 */
const handlingRecommendationId = ref(null)

/**
 * 是否存在未读通知。
 * @returns {Boolean} 是否有未读消息
 */
const hasUnread = computed(() => notifications.value.some(item => !item.isRead))

/**
 * 当前未读通知数量。
 * @returns {Number} 未读消息数
 */
const unreadCount = computed(() => notifications.value.filter(item => !item.isRead).length)

/**
 * 页面展示时加载通知列表。
 */
onShow(() => {
  loadNotifications()
})

/**
 * 获取当前登录用户ID。
 * @returns {Number|null} 用户ID
 */
function getCurrentUserId() {
  const userInfo = uni.getStorageSync('userInfo') || {}
  const userId = Number(userInfo.id)
  if (!Number.isInteger(userId) || userId <= 0) {
    return null
  }
  return userId
}

/**
 * 加载当前用户的通知列表。
 */
async function loadNotifications() {
  const userId = getCurrentUserId()
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/index' })
    }, 600)
    return
  }

  loading.value = true
  try {
    const list = await request({
      url: `/api/notifications?userId=${userId}`,
      method: 'GET'
    })
    notifications.value = Array.isArray(list) ? list : []
    refreshNotificationBadge(userId)
  } catch (error) {
    uni.showToast({ title: error.message || '消息加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 将单条通知标记为已读。
 * @param {Object} item 通知对象
 */
async function markAsRead(item) {
  if (!item || item.isRead) {
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    return
  }

  try {
    await request({
      url: `/api/notifications/${item.id}/read`,
      method: 'POST',
      data: { userId }
    })
    notifications.value = notifications.value.map(notification =>
      notification.id === item.id
        ? { ...notification, isRead: true }
        : notification
    )
    refreshNotificationBadge(userId)
  } catch (error) {
    uni.showToast({ title: error.message || '标记已读失败', icon: 'none' })
  }
}

/**
 * 将当前用户全部通知标记为已读。
 */
async function markAllAsRead() {
  const userId = getCurrentUserId()
  if (!userId || !hasUnread.value || markingAll.value) {
    return
  }

  markingAll.value = true
  try {
    await request({
      url: '/api/notifications/read-all',
      method: 'POST',
      data: { userId }
    })
    notifications.value = notifications.value.map(item => ({
      ...item,
      isRead: true
    }))
    refreshNotificationBadge(userId)
    uni.showToast({ title: '已全部标记为已读', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  } finally {
    markingAll.value = false
  }
}

/**
 * 接受改约推荐。
 * @param {Object} recommendation 推荐对象
 */
async function acceptRecommendation(recommendation) {
  const userId = getCurrentUserId()
  if (!userId || !recommendation || handlingRecommendationId.value) {
    return
  }

  handlingRecommendationId.value = recommendation.id
  try {
    await request({
      url: `/api/notifications/recommendations/${recommendation.id}/accept`,
      method: 'POST',
      data: { userId }
    })
    uni.showToast({ title: '已提交改约申请', icon: 'success' })
    await loadNotifications()
  } catch (error) {
    uni.showToast({ title: error.message || '改约失败', icon: 'none' })
  } finally {
    handlingRecommendationId.value = null
  }
}

/**
 * 放弃改约推荐。
 * @param {Object} recommendation 推荐对象
 */
async function declineRecommendation(recommendation) {
  const userId = getCurrentUserId()
  if (!userId || !recommendation || handlingRecommendationId.value) {
    return
  }

  handlingRecommendationId.value = recommendation.id
  try {
    await request({
      url: `/api/notifications/recommendations/${recommendation.id}/decline`,
      method: 'POST',
      data: { userId }
    })
    uni.showToast({ title: '已放弃推荐', icon: 'success' })
    await loadNotifications()
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  } finally {
    handlingRecommendationId.value = null
  }
}

/**
 * 获取通知类型标签样式。
 * @param {Number} type 通知类型
 * @returns {String} 样式类名
 */
function getTypeClass(type) {
  return `type-${Number(type) || 0}`
}
</script>

<style scoped>
.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 30rpx;
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
}

.header-title {
  min-width: 0;
  flex: 1;
}

.title-text {
  display: block;
  color: #333;
  font-size: 36rpx;
  font-weight: bold;
  line-height: 48rpx;
}

.unread-text {
  display: block;
  margin-top: 6rpx;
  color: #007aff;
  font-size: 24rpx;
  line-height: 32rpx;
}

.read-all-btn {
  flex-shrink: 0;
  min-width: 156rpx;
  height: 64rpx;
  margin: 0;
  padding: 0 24rpx;
  color: #fff;
  background-color: #007aff;
  border-radius: 8rpx;
  font-size: 26rpx;
  line-height: 64rpx;
}

.read-all-btn.disabled {
  color: #999;
  background-color: #eee;
}

.read-all-btn::after {
  border: none;
}

.message-list {
  flex: 1;
  padding: 20rpx;
  box-sizing: border-box;
}

.loading-state,
.empty-state {
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
  text-align: center;
}

.message-card {
  display: flex;
  margin-bottom: 20rpx;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 16rpx;
  box-sizing: border-box;
}

.message-card.unread {
  border-left: 6rpx solid #007aff;
}

.message-main {
  flex: 1;
  min-width: 0;
}

.card-title-row {
  display: flex;
  align-items: center;
  margin-bottom: 14rpx;
}

.unread-dot {
  flex-shrink: 0;
  width: 14rpx;
  height: 14rpx;
  margin-right: 12rpx;
  border-radius: 50%;
  background-color: #007aff;
}

.card-title {
  flex: 1;
  min-width: 0;
  color: #333;
  font-size: 32rpx;
  font-weight: bold;
  line-height: 42rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-content {
  display: block;
  color: #666;
  font-size: 28rpx;
  line-height: 42rpx;
  word-break: break-all;
}

.recommendation-box {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 8rpx;
  background-color: #f6f9ff;
  border: 1rpx solid #dbeafe;
}

.recommendation-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #1d4ed8;
  font-size: 26rpx;
  font-weight: 600;
  line-height: 36rpx;
}

.recommendation-status {
  color: #666;
  font-size: 24rpx;
  font-weight: normal;
}

.recommendation-line {
  display: block;
  margin-top: 8rpx;
  color: #333;
  font-size: 26rpx;
  line-height: 38rpx;
}

.recommendation-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14rpx;
}

.recommendation-btn {
  height: 56rpx;
  line-height: 56rpx;
  margin: 0 0 0 14rpx;
  padding: 0 20rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
}

.recommendation-btn::after {
  border: none;
}

.recommendation-btn.accept {
  color: #fff;
  background-color: #007aff;
}

.recommendation-btn.decline {
  color: #666;
  background-color: #eef0f3;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.type-tag {
  flex-shrink: 0;
  max-width: 220rpx;
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
  line-height: 32rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-0 {
  color: #607d8b;
  background-color: #eceff1;
}

.type-1 {
  color: #d32f2f;
  background-color: #ffebee;
}

.type-2 {
  color: #2e7d32;
  background-color: #e8f5e9;
}

.type-3 {
  color: #ef6c00;
  background-color: #fff3e0;
}

.created-at {
  min-width: 0;
  margin-left: 16rpx;
  color: #999;
  font-size: 24rpx;
  line-height: 32rpx;
  text-align: right;
}
</style>
