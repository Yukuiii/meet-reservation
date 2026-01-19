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
      <view
        class="booking-card"
        v-for="item in currentList"
        :key="item.id"
        @click="goToDetail(item.id)"
      >
        <view class="card-header">
          <text class="room-name">{{ item.roomName }}</text>
          <text class="status" :class="item.status">{{ item.statusText }}</text>
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
            <text class="label">预约事由：</text>
            <text class="value">{{ item.reason }}</text>
          </view>
        </view>
        <view class="card-footer" v-if="item.status === 'pending' || item.status === 'approved'">
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
      <view class="empty-state" v-if="currentList.length === 0">
        <text>暂无预约记录</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
/**
 * 我的预约页面
 * @description 展示用户的预约记录，支持按状态筛选和取消预约
 */
export default {
  data() {
    return {
      // 当前选中的标签
      currentTab: 'pending',
      // 预约列表（模拟数据）
      bookingList: [
        {
          id: 1,
          roomName: '第一会议室',
          date: '2026-01-20',
          timeSlot: '09:00-10:00',
          reason: '项目周会',
          attendees: 6,
          status: 'pending',
          statusText: '待审核'
        },
        {
          id: 2,
          roomName: '大型报告厅',
          date: '2026-01-21',
          timeSlot: '14:00-16:00',
          reason: '季度总结会议',
          attendees: 35,
          status: 'approved',
          statusText: '已通过'
        },
        {
          id: 3,
          roomName: '小型洽谈室',
          date: '2026-01-19',
          timeSlot: '10:00-11:00',
          reason: '客户洽谈',
          attendees: 4,
          status: 'approved',
          statusText: '已通过'
        },
        {
          id: 4,
          roomName: '第二会议室',
          date: '2026-01-18',
          timeSlot: '15:00-17:00',
          reason: '技术分享会',
          attendees: 12,
          status: 'cancelled',
          statusText: '已取消'
        },
        {
          id: 5,
          roomName: '培训室',
          date: '2026-01-22',
          timeSlot: '09:00-12:00',
          reason: '新员工培训',
          attendees: 20,
          status: 'pending',
          statusText: '待审核'
        }
      ]
    }
  },

  computed: {
    /**
     * 根据当前标签筛选预约列表
     * @returns {Array} 筛选后的预约列表
     */
    currentList() {
      return this.bookingList.filter(item => item.status === this.currentTab)
    }
  },

  methods: {
    /**
     * 切换标签
     * @param {String} tab 标签名称
     */
    switchTab(tab) {
      this.currentTab = tab
    },

    /**
     * 跳转到预约详情
     * @param {Number} id 预约ID
     */
    goToDetail(id) {
      // TODO: 跳转到预约详情页
      uni.showToast({ title: '查看详情', icon: 'none' })
    },

    /**
     * 取消预约
     * @param {Number} id 预约ID
     */
    cancelBooking(id) {
      uni.showModal({
        title: '提示',
        content: '确定要取消该预约吗？',
        success: (res) => {
          if (res.confirm) {
            // 更新预约状态
            const booking = this.bookingList.find(item => item.id === id)
            if (booking) {
              booking.status = 'cancelled'
              booking.statusText = '已取消'
            }
            uni.showToast({ title: '已取消预约', icon: 'success' })
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
