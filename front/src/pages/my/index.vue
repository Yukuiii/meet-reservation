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
      <view class="menu-item" @click="goToNotification">
        <text class="menu-icon">🔔</text>
        <text class="menu-text">消息通知</text>
        <text class="menu-badge" v-if="unreadCount > 0">{{ unreadCount }}</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToSettings">
        <text class="menu-icon">⚙️</text>
        <text class="menu-text">设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToAbout">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script>
/**
 * 个人中心页面
 * @description 展示用户信息、预约统计和功能菜单
 */
export default {
  data() {
    return {
      // 用户信息（默认数据）
      userInfo: {
        name: '未登录用户',
        department: '普通用户',
        avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop'
      },
      // 预约统计
      stats: {
        total: 15,
        pending: 2,
        approved: 10,
        cancelled: 3
      },
      // 未读消息数
      unreadCount: 3
    }
  },

  /**
   * 页面显示时同步登录态。
   */
  onShow() {
    this.loadUserInfo()
  },

  methods: {
    /**
     * 加载本地缓存的用户信息。
     */
    loadUserInfo() {
      const token = uni.getStorageSync('token')
      if (!token) {
        uni.reLaunch({ url: '/pages/login/index' })
        return
      }

      const storageUserInfo = uni.getStorageSync('userInfo') || {}
      this.userInfo = {
        name: storageUserInfo.nickname || storageUserInfo.username || '未命名用户',
        department: storageUserInfo.role === 1 ? '系统管理员' : '普通用户',
        avatar: this.userInfo.avatar
      }
    },

    /**
     * 跳转到我的预约
     */
    goToBookingList() {
      uni.switchTab({ url: '/pages/booking/index' })
    },

    /**
     * 跳转到消息通知
     */
    goToNotification() {
      uni.showToast({ title: '消息通知', icon: 'none' })
    },

    /**
     * 跳转到设置
     */
    goToSettings() {
      uni.showToast({ title: '设置', icon: 'none' })
    },

    /**
     * 跳转到关于我们
     */
    goToAbout() {
      uni.showToast({ title: '关于我们', icon: 'none' })
    },

    /**
     * 退出登录
     */
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            // 清除登录状态
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            // 跳转到登录页
            uni.reLaunch({ url: '/pages/login/index' })
          }
        }
      })
    }
  }
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

.menu-badge {
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  font-size: 22rpx;
  color: #fff;
  background-color: #f44336;
  border-radius: 18rpx;
  padding: 0 10rpx;
  margin-right: 10rpx;
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
