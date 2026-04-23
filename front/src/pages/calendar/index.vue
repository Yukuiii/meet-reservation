<template>
  <view class="container">
    <view class="filter-panel">
      <view class="view-switch">
        <view
          class="switch-item"
          :class="{ active: viewType === 'day' }"
          @click="changeViewType('day')"
        >
          按日
        </view>
        <view
          class="switch-item"
          :class="{ active: viewType === 'week' }"
          @click="changeViewType('week')"
        >
          按周
        </view>
      </view>
    </view>

    <view class="summary-panel">
      <text class="summary-range">{{ rangeText }}</text>
      <text class="summary-count">共 {{ totalCount }} 条预约</text>
    </view>

    <scroll-view class="calendar-scroll" scroll-y>
      <view class="loading-state" v-if="loading">
        <text>日历数据加载中...</text>
      </view>

      <template v-else>
        <view class="day-card" v-for="day in dayList" :key="day.date">
          <view class="day-header">
            <text class="day-date">{{ day.date }}</text>
            <text class="day-week">{{ day.weekDay }}</text>
            <text class="day-count">{{ day.totalCount }} 条</text>
          </view>

          <view class="day-empty" v-if="day.totalCount === 0">
            <text>暂无预约</text>
          </view>

          <view
            class="reservation-item"
            v-for="item in day.items"
            :key="item.id"
          >
            <view class="item-main">
              <text class="item-time">{{ item.timeSlot }}</text>
              <text class="item-title">{{ item.title || '未命名会议' }}</text>
            </view>
            <view class="item-sub">
              <text class="item-room">{{ item.roomName }}</text>
              <text class="item-status" :class="item.statusKey">{{ item.statusText }}</text>
            </view>
          </view>
        </view>

        <view class="empty-state" v-if="dayList.length === 0">
          <text>暂无日历数据</text>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '../../utils/request'

/**
 * 当前用户ID。
 */
const userId = ref(null)

/**
 * 当前视图类型：day/week。
 */
const viewType = ref('day')

/**
 * 日历区间。
 */
const startDate = ref('')
const endDate = ref('')

/**
 * 分日数据。
 */
const dayList = ref([])

/**
 * 加载状态。
 */
const loading = ref(false)

/**
 * 区间文案。
 * @returns {String}
 */
const rangeText = computed(() => {
  if (!startDate.value) {
    return ''
  }
  if (startDate.value === endDate.value) {
    return startDate.value
  }
  return `${startDate.value} - ${endDate.value}`
})

/**
 * 预约总数。
 * @returns {Number}
 */
const totalCount = computed(() =>
  dayList.value.reduce((total, day) => total + (day.totalCount || 0), 0)
)

/**
 * 页面加载。
 */
onLoad(async () => {
  const userInfo = uni.getStorageSync('userInfo') || {}
  const storageUserId = Number(userInfo.id)
  if (!Number.isInteger(storageUserId) || storageUserId <= 0) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/index' })
    }, 600)
    return
  }

  userId.value = storageUserId
  await loadCalendar()
})

/**
 * 拉取日历数据。
 */
async function loadCalendar() {
  loading.value = true
  try {
    const data = await request({
      url: `/api/reservations/calendar?userId=${userId.value}&viewType=${viewType.value}`,
      method: 'GET'
    })

    startDate.value = data.startDate || ''
    endDate.value = data.endDate || ''
    dayList.value = Array.isArray(data.days) ? data.days : []
  } catch (error) {
    uni.showToast({ title: error.message || '日历数据加载失败', icon: 'none' })
    dayList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 切换视图类型。
 * @param {String} nextViewType 视图类型
 */
async function changeViewType(nextViewType) {
  if (viewType.value === nextViewType) {
    return
  }
  viewType.value = nextViewType
  await loadCalendar()
}
</script>

<style scoped>
.container {
  min-height: 100%;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.filter-panel {
  background-color: #fff;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.view-switch {
  display: flex;
  border-radius: 10rpx;
  overflow: hidden;
  background-color: #f3f4f6;
}

.switch-item {
  flex: 1;
  text-align: center;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  color: #666;
}

.switch-item.active {
  background-color: #007aff;
  color: #fff;
  font-weight: bold;
}

.summary-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24rpx 16rpx;
}

.summary-range {
  color: #666;
  font-size: 24rpx;
}

.summary-count {
  color: #007aff;
  font-size: 24rpx;
}

.calendar-scroll {
  flex: 1;
  padding: 0 20rpx 20rpx;
  box-sizing: border-box;
}

.day-card {
  background-color: #fff;
  border-radius: 14rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.day-header {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.day-date {
  font-size: 30rpx;
  color: #333;
  font-weight: bold;
  margin-right: 12rpx;
}

.day-week {
  font-size: 24rpx;
  color: #666;
  margin-right: auto;
}

.day-count {
  font-size: 24rpx;
  color: #999;
}

.reservation-item {
  padding: 16rpx;
  border-radius: 10rpx;
  background-color: #f8f9fb;
  margin-bottom: 12rpx;
}

.reservation-item:last-child {
  margin-bottom: 0;
}

.item-main {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}

.item-time {
  width: 190rpx;
  font-size: 24rpx;
  color: #007aff;
}

.item-title {
  flex: 1;
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

.item-sub {
  display: flex;
  align-items: center;
}

.item-room {
  flex: 1;
  font-size: 24rpx;
  color: #666;
}

.item-status {
  font-size: 22rpx;
  border-radius: 14rpx;
  padding: 4rpx 12rpx;
}

.item-status.pending {
  color: #ff9800;
  background-color: #fff3e0;
}

.item-status.approved {
  color: #43a047;
  background-color: #e8f5e9;
}

.item-status.finished {
  color: #666;
  background-color: #f1f1f1;
}

.day-empty {
  text-align: center;
  color: #999;
  font-size: 24rpx;
  padding: 20rpx 0;
}

.loading-state,
.empty-state {
  text-align: center;
  color: #999;
  font-size: 28rpx;
  padding: 100rpx 0;
}
</style>
