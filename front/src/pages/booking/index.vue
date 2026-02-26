<template>
  <view class="container">
    <!-- 标签切换 -->
    <view class="tabs">
      <view
        class="tab-item"
        :class="{ active: currentTab === 'pending' }"
        @click="switchTab('pending')"
      >
        待审核
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'approved' }"
        @click="switchTab('approved')"
      >
        已通过
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'cancelled' }"
        @click="switchTab('cancelled')"
      >
        已取消
      </view>
    </view>

    <!-- 预约列表 -->
    <scroll-view class="booking-list" scroll-y>
      <view class="loading-state" v-if="loading">
        <text>正在加载预约记录...</text>
      </view>

      <view
        class="booking-card"
        v-for="item in currentList"
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <view class="card-header">
          <text class="room-name">{{ item.roomName }}</text>
          <text class="status" :class="item.statusKey">{{ item.statusText }}</text>
        </view>
        <view class="card-body">
          <view class="info-row">
            <text class="label">预约日期：</text>
            <text class="value">{{ item.date }}</text>
          </view>
          <view class="info-row">
            <text class="label">预约时段：</text>
            <text class="value">{{ item.timeSlot }}</text>
          </view>
          <view class="info-row">
            <text class="label">参与人数：</text>
            <text class="value">{{ item.attendees }}人</text>
          </view>
          <view class="info-row">
            <text class="label">预约事由：</text>
            <text class="value">{{ item.purpose }}</text>
          </view>
        </view>
        <view class="card-footer" v-if="item.statusKey === 'pending' || item.statusKey === 'approved'">
          <button
            class="cancel-btn"
            size="mini"
            @click.stop="cancelBooking(item.id)"
          >
            取消预约
          </button>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && currentList.length === 0">
        <text>暂无预约记录</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { request } from '../../utils/request'

/**
 * 我的预约页面
 * @description 展示用户预约记录，支持按状态筛选、取消预约与查看详情
 */
export default {
  data() {
    return {
      // 当前选中的标签
      currentTab: 'pending',
      // 预约列表
      bookingList: [],
      // 加载状态
      loading: false
    }
  },

  /**
   * 页面展示时刷新预约列表。
   */
  onShow() {
    this.loadBookingList()
  },

  computed: {
    /**
     * 根据当前标签筛选预约列表。
     * @returns {Array} 筛选结果
     */
    currentList() {
      return this.bookingList.filter(item => item.statusKey === this.currentTab)
    }
  },

  methods: {
    /**
     * 切换标签。
     * @param {String} tab 标签名称
     */
    switchTab(tab) {
      this.currentTab = tab
    },

    /**
     * 获取当前登录用户ID。
     * @returns {Number|null}
     */
    getCurrentUserId() {
      const userInfo = uni.getStorageSync('userInfo') || {}
      const userId = Number(userInfo.id)
      if (!Number.isInteger(userId) || userId <= 0) {
        return null
      }
      return userId
    },

    /**
     * 加载当前用户预约列表。
     */
    async loadBookingList() {
      const userId = this.getCurrentUserId()
      if (!userId) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/login/index' })
        }, 600)
        return
      }

      this.loading = true
      try {
        const list = await request({
          url: `/api/reservations/my?userId=${userId}`,
          method: 'GET'
        })
        this.bookingList = Array.isArray(list) ? list : []
      } catch (error) {
        uni.showToast({ title: error.message || '预约记录加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    /**
     * 查看预约详情。
     * @param {Number} id 预约ID
     */
    async goToDetail(id) {
      const userId = this.getCurrentUserId()
      if (!userId) {
        return
      }

      try {
        const detail = await request({
          url: `/api/reservations/${id}?userId=${userId}`,
          method: 'GET'
        })

        const lines = [
          `预约编号：${detail.reservationNo || '-'}`,
          `会议室：${detail.roomName || '-'}`,
          `日期：${detail.date || '-'}`,
          `时段：${detail.timeSlot || '-'}`,
          `参与人数：${detail.attendees || 0}人`,
          `状态：${detail.statusText || '-'}`,
          `事由：${detail.purpose || '-'}`
        ]
        if (detail.cancelReason) {
          lines.push(`取消原因：${detail.cancelReason}`)
        }
        if (detail.rejectReason) {
          lines.push(`拒绝原因：${detail.rejectReason}`)
        }
        if (detail.remark) {
          lines.push(`备注：${detail.remark}`)
        }

        uni.showModal({
          title: '预约详情',
          content: lines.join('\n'),
          showCancel: false
        })
      } catch (error) {
        uni.showToast({ title: error.message || '详情加载失败', icon: 'none' })
      }
    },

    /**
     * 取消预约。
     * @param {Number} id 预约ID
     */
    cancelBooking(id) {
      const userId = this.getCurrentUserId()
      if (!userId) {
        return
      }

      uni.showModal({
        title: '提示',
        content: '确定要取消该预约吗？',
        success: async (res) => {
          if (!res.confirm) {
            return
          }
          try {
            await request({
              url: `/api/reservations/${id}/cancel`,
              method: 'POST',
              data: {
                userId,
                cancelReason: '用户主动取消'
              }
            })
            uni.showToast({ title: '已取消预约', icon: 'success' })
            this.loadBookingList()
          } catch (error) {
            uni.showToast({ title: error.message || '取消失败', icon: 'none' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

/* 标签切换 */
.tabs {
  display: flex;
  background-color: #fff;
  padding: 0 20rpx;
}

.tab-item {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #007aff;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 60rpx;
  height: 6rpx;
  background-color: #007aff;
  border-radius: 3rpx;
}

/* 预约列表 */
.booking-list {
  flex: 1;
  padding: 20rpx;
}

.loading-state {
  text-align: center;
  color: #999;
  font-size: 28rpx;
  padding: 80rpx 0;
}

.booking-card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.room-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.status {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.status.pending {
  background-color: #fff3e0;
  color: #ff9800;
}

.status.approved {
  background-color: #e8f5e9;
  color: #4caf50;
}

.status.cancelled {
  background-color: #f5f5f5;
  color: #999;
}

.card-body {
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  font-size: 26rpx;
  margin-bottom: 12rpx;
}

.info-row .label {
  color: #999;
  width: 140rpx;
}

.info-row .value {
  color: #333;
  flex: 1;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 20rpx;
  border-top: 1rpx solid #eee;
}

.cancel-btn {
  font-size: 24rpx;
  color: #f44336;
  background-color: #ffebee;
  border: none;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
}

.cancel-btn::after {
  border: none;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}
</style>
