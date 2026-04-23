<template>
  <view class="container">
    <!-- 用户信息 -->
    <view class="user-section">
      <image class="avatar" :src="userInfo.avatar" mode="aspectFill"></image>
      <view class="user-info">
        <text class="username">{{ userInfo.name }}</text>
        <text class="department">{{ userInfo.department }}</text>
      </view>
    </view>

    <!-- 预约统计 -->
    <view class="stats-section">
      <view class="stat-item">
        <text class="stat-value">{{ stats.total }}</text>
        <text class="stat-label">总预约</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.pending }}</text>
        <text class="stat-label">待审核</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.approved }}</text>
        <text class="stat-label">已通过</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.cancelled }}</text>
        <text class="stat-label">已取消</text>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-section">
      <view class="menu-item" @click="goToBookingList">
        <text class="menu-icon">📋</text>
        <text class="menu-text">我的预约</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToCalendar">
        <text class="menu-icon">🗓️</text>
        <text class="menu-text">日历视图</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" v-if="isAdmin" @click="goToAdmin">
        <text class="menu-icon">🛠️</text>
        <text class="menu-text">管理员后台</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '../../utils/request'
import { refreshNotificationBadge } from '../../utils/notification'

/**
 * 用户信息。
 */
const userInfo = reactive({
  name: '未登录用户',
  department: '普通用户',
  avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop'
})

/**
 * 预约统计。
 */
const stats = reactive({
  total: 15,
  pending: 2,
  approved: 10,
  cancelled: 3
})

/**
 * 是否管理员。
 */
const isAdmin = ref(false)

/**
 * 页面显示时同步登录态。
 */
onShow(async () => {
  loadUserInfo()
  const userId = getCurrentUserId()
  if (userId) {
    refreshNotificationBadge(userId)
  }
  await loadReservationStats()
})

/**
 * 获取当前登录用户ID。
 * @returns {Number|null}
 */
function getCurrentUserId() {
  const storageUserInfo = uni.getStorageSync('userInfo') || {}
  const userId = Number(storageUserInfo.id)
  if (!Number.isInteger(userId) || userId <= 0) {
    return null
  }
  return userId
}

/**
 * 加载本地缓存的用户信息。
 */
function loadUserInfo() {
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }

  const storageUserInfo = uni.getStorageSync('userInfo') || {}
  const role = Number(storageUserInfo.role)
  userInfo.name = storageUserInfo.nickname || storageUserInfo.username || '未命名用户'
  userInfo.department = role === 1 ? '系统管理员' : '普通用户'
  isAdmin.value = role === 1
}

/**
 * 加载预约统计数据。
 */
async function loadReservationStats() {
  const userId = getCurrentUserId()
  if (!userId) {
    return
  }

  try {
    const list = await request({
      url: `/api/reservations/my?userId=${userId}`,
      method: 'GET'
    })
    const reservationList = Array.isArray(list) ? list : []

    // 根据状态键统计个人预约数据。
    stats.total = reservationList.length
    stats.pending = reservationList.filter(item => item.statusKey === 'pending').length
    stats.approved = reservationList.filter(item => item.statusKey === 'approved').length
    stats.cancelled = reservationList.filter(item => item.statusKey === 'cancelled').length
  } catch (error) {
    // 统计失败不阻塞页面主流程，这里仅静默处理。
  }
}

/**
 * 跳转到我的预约。
 */
function goToBookingList() {
  uni.switchTab({ url: '/pages/booking/index' })
}

/**
 * 跳转到日历视图。
 */
function goToCalendar() {
  uni.navigateTo({ url: '/pages/calendar/index' })
}

/**
 * 跳转到管理员后台。
 */
function goToAdmin() {
  uni.navigateTo({ url: '/pages/admin/index' })
}

/**
 * 退出登录。
 */
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        // 清除登录状态
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.removeStorageSync('loginType')
        // 跳转到登录页
        uni.reLaunch({ url: '/pages/login/index' })
      }
    }
  })
}
</script>

<style scoped>
.container {
  min-height: 100%;
  background-color: #f5f5f5;
}

/* 用户信息 */
.user-section {
  display: flex;
  align-items: center;
  padding: 40rpx 30rpx;
  background: linear-gradient(135deg, #007aff 0%, #00c6ff 100%);
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.user-info {
  margin-left: 30rpx;
}

.username {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 10rpx;
}

.department {
  display: block;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 预约统计 */
.stats-section {
  display: flex;
  background-color: #fff;
  padding: 30rpx 0;
  margin-bottom: 20rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
  border-right: 1rpx solid #eee;
}

.stat-item:last-child {
  border-right: none;
}

.stat-value {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #007aff;
  margin-bottom: 8rpx;
}

.stat-label {
  display: block;
  font-size: 24rpx;
  color: #999;
}

/* 功能菜单 */
.menu-section {
  background-color: #fff;
  margin-bottom: 20rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #eee;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.menu-arrow {
  font-size: 32rpx;
  color: #ccc;
}

/* 退出登录 */
.logout-section {
  padding: 40rpx 30rpx;
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background-color: #fff;
  color: #f44336;
  font-size: 32rpx;
  border-radius: 12rpx;
}

.logout-btn::after {
  border: none;
}
</style>
