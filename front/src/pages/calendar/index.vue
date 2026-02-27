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

      <view class="picker-row">
        <text class="picker-label">日期</text>
        <picker mode="date" :value="selectedDate" @change="handleDateChange">
          <view class="picker-value">{{ selectedDate }}</view>
        </picker>
      </view>

      <view class="picker-row">
        <text class="picker-label">会议室</text>
        <picker
          :range="roomOptions"
          range-key="label"
          :value="roomPickerIndex"
          @change="handleRoomChange"
        >
          <view class="picker-value">{{ selectedRoomLabel }}</view>
        </picker>
      </view>
    </view>

    <view class="summary-panel">
      <text class="summary-range">{{ rangeText }}</text>
      <text class="summary-count">共 {{ filteredTotalCount }} 条预约</text>
    </view>

    <scroll-view class="calendar-scroll" scroll-y>
      <view class="loading-state" v-if="loading">
        <text>日历数据加载中...</text>
      </view>

      <template v-else>
        <view class="day-card" v-for="day in filteredDays" :key="day.date">
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

        <view class="empty-state" v-if="filteredDays.length === 0">
          <text>暂无日历数据</text>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script>
import { request } from '../../utils/request'

/**
 * 日历页面
 * @description 展示会议室预约按日/周视图，并支持按会议室筛选
 */
export default {
  data() {
    return {
      // 当前用户ID
      userId: null,
      // 当前视图类型：day/week
      viewType: 'day',
      // 选中的目标日期
      selectedDate: '',
      // 会议室选项
      roomOptions: [
        { value: '', label: '全部会议室' }
      ],
      // 选中的会议室ID（空字符串表示全部）
      selectedRoomId: '',
      // 日历区间
      startDate: '',
      endDate: '',
      // 分日数据
      dayList: [],
      // 加载状态
      loading: false
    }
  },

  /**
   * 页面加载。
   */
  async onLoad() {
    const userInfo = uni.getStorageSync('userInfo') || {}
    const userId = Number(userInfo.id)
    if (!Number.isInteger(userId) || userId <= 0) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/login/index' })
      }, 600)
      return
    }

    this.userId = userId
    this.selectedDate = this.formatDate(new Date())
    await Promise.all([this.loadRoomOptions(), this.loadCalendar()])
  },

  computed: {
    /**
     * 会议室筛选器索引。
     * @returns {Number}
     */
    roomPickerIndex() {
      const index = this.roomOptions.findIndex(
        item => String(item.value) === String(this.selectedRoomId)
      )
      return index >= 0 ? index : 0
    },

    /**
     * 当前选中的会议室名称。
     * @returns {String}
     */
    selectedRoomLabel() {
      const item = this.roomOptions[this.roomPickerIndex]
      return item ? item.label : '全部会议室'
    },

    /**
     * 区间文案。
     * @returns {String}
     */
    rangeText() {
      if (!this.startDate) {
        return ''
      }
      if (this.startDate === this.endDate) {
        return this.startDate
      }
      return `${this.startDate} - ${this.endDate}`
    },

    /**
     * 按会议室筛选后的分日数据。
     * @returns {Array}
     */
    filteredDays() {
      if (!this.selectedRoomId) {
        return this.dayList
      }
      const roomId = Number(this.selectedRoomId)
      return this.dayList.map(day => {
        const items = Array.isArray(day.items)
          ? day.items.filter(item => Number(item.roomId) === roomId)
          : []
        return {
          ...day,
          items,
          totalCount: items.length
        }
      })
    },

    /**
     * 筛选后的预约总数。
     * @returns {Number}
     */
    filteredTotalCount() {
      return this.filteredDays.reduce((total, day) => total + (day.totalCount || 0), 0)
    }
  },

  methods: {
    /**
     * 加载会议室选项。
     */
    async loadRoomOptions() {
      try {
        const list = await request({
          url: '/api/meeting-rooms',
          method: 'GET'
        })
        const roomList = Array.isArray(list) ? list : []
        this.roomOptions = [
          { value: '', label: '全部会议室' },
          ...roomList.map(item => ({
            value: item.id,
            label: item.name
          }))
        ]
      } catch (error) {
        uni.showToast({ title: error.message || '会议室选项加载失败', icon: 'none' })
      }
    },

    /**
     * 拉取日历数据。
     */
    async loadCalendar() {
      this.loading = true
      try {
        const data = await request({
          url: `/api/reservations/calendar?userId=${this.userId}&viewType=${this.viewType}&date=${this.selectedDate}`,
          method: 'GET'
        })

        this.startDate = data.startDate || this.selectedDate
        this.endDate = data.endDate || this.selectedDate
        this.dayList = Array.isArray(data.days) ? data.days : []
      } catch (error) {
        uni.showToast({ title: error.message || '日历数据加载失败', icon: 'none' })
        this.dayList = []
      } finally {
        this.loading = false
      }
    },

    /**
     * 切换视图类型。
     * @param {String} viewType 视图类型
     */
    async changeViewType(viewType) {
      if (this.viewType === viewType) {
        return
      }
      this.viewType = viewType
      await this.loadCalendar()
    },

    /**
     * 切换日期。
     * @param {Object} event 日期选择事件
     */
    async handleDateChange(event) {
      this.selectedDate = event.detail.value
      await this.loadCalendar()
    },

    /**
     * 切换会议室筛选。
     * @param {Object} event 会议室选择事件
     */
    handleRoomChange(event) {
      const index = Number(event.detail.value)
      const option = this.roomOptions[index]
      this.selectedRoomId = option ? option.value : ''
    },

    /**
     * 日期格式化为 yyyy-MM-dd。
     * @param {Date} date 日期对象
     * @returns {String}
     */
    formatDate(date) {
      const year = date.getFullYear()
      const month = `${date.getMonth() + 1}`.padStart(2, '0')
      const day = `${date.getDate()}`.padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
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
  margin-bottom: 20rpx;
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

.picker-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.picker-row:last-child {
  margin-bottom: 0;
}

.picker-label {
  width: 110rpx;
  color: #666;
  font-size: 26rpx;
}

.picker-value {
  flex: 1;
  min-height: 68rpx;
  line-height: 68rpx;
  border-radius: 8rpx;
  background-color: #f7f7f7;
  padding: 0 18rpx;
  font-size: 26rpx;
  color: #333;
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
