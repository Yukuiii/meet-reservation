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
        <view
          class="switch-item"
          :class="{ active: viewType === 'month' }"
          @click="changeViewType('month')"
        >
          按月
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
        <view class="month-panel" v-if="viewType === 'month'">
          <view class="month-header">
            <text class="month-nav" @click="changeMonth(-1)">上一月</text>
            <text class="month-title">{{ monthTitle }}</text>
            <text class="month-nav" @click="changeMonth(1)">下一月</text>
          </view>
          <view class="week-row">
            <text class="week-cell" v-for="item in weekLabels" :key="item">{{ item }}</text>
          </view>
          <view class="month-grid">
            <view
              class="month-cell"
              :class="{
                muted: !cell.currentMonth,
                reserved: cell.hasReservations,
                selected: cell.selected,
                today: cell.today
              }"
              v-for="cell in monthCells"
              :key="cell.date"
              @click="selectMonthDate(cell)"
            >
              <view class="date-badge">
                <text class="date-number">{{ cell.day }}</text>
              </view>
              <text class="date-count" v-if="cell.hasReservations">{{ cell.totalCount }}条</text>
            </view>
          </view>
        </view>

        <view class="day-card" v-for="day in displayedDayList" :key="day.date">
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

        <view class="empty-state" v-if="displayedDayList.length === 0">
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
 * 当前视图类型：day/week/month。
 */
const viewType = ref('month')

/**
 * 当前日历查询日期。
 */
const targetDate = ref('')

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
 * 月视图当前选中日期。
 */
const selectedMonthDate = ref('')

/**
 * 加载状态。
 */
const loading = ref(false)

/**
 * 星期标题。
 */
const weekLabels = ['一', '二', '三', '四', '五', '六', '日']

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
 * 月份标题。
 * @returns {String}
 */
const monthTitle = computed(() => {
  const baseDate = parseDate(targetDate.value || startDate.value || formatDate(new Date()))
  return `${baseDate.getFullYear()}年${baseDate.getMonth() + 1}月`
})

/**
 * 月历单元格。
 * @returns {Array}
 */
const monthCells = computed(() => buildMonthCells())

/**
 * 当前展示的分日列表。
 * @returns {Array}
 */
const displayedDayList = computed(() => {
  if (viewType.value === 'month') {
    if (!selectedMonthDate.value) {
      return []
    }
    return dayList.value.filter(day => day.date === selectedMonthDate.value && (day.totalCount || 0) > 0)
  }
  return dayList.value
})

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
  targetDate.value = formatDate(new Date())
  await loadCalendar()
})

/**
 * 拉取日历数据。
 */
async function loadCalendar() {
  loading.value = true
  try {
    const dateParam = targetDate.value ? `&date=${targetDate.value}` : ''
    const data = await request({
      url: `/api/reservations/calendar?userId=${userId.value}&viewType=${viewType.value}${dateParam}`,
      method: 'GET'
    })

    startDate.value = data.startDate || ''
    endDate.value = data.endDate || ''
    dayList.value = Array.isArray(data.days) ? data.days : []
    syncSelectedMonthDate()
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
  selectedMonthDate.value = ''
  if (!targetDate.value) {
    targetDate.value = formatDate(new Date())
  }
  await loadCalendar()
}

/**
 * 切换月份。
 * @param {Number} offset 月份偏移量
 */
async function changeMonth(offset) {
  const baseDate = parseDate(targetDate.value || formatDate(new Date()))
  baseDate.setMonth(baseDate.getMonth() + offset, 1)
  targetDate.value = formatDate(baseDate)
  selectedMonthDate.value = ''
  await loadCalendar()
}

/**
 * 选择月历日期。
 * @param {Object} cell 月历日期单元格
 */
function selectMonthDate(cell) {
  selectedMonthDate.value = cell.date
}

/**
 * 构建月历单元格。
 * @returns {Array} 月历单元格
 */
function buildMonthCells() {
  const baseDate = parseDate(targetDate.value || startDate.value || formatDate(new Date()))
  const monthStart = new Date(baseDate.getFullYear(), baseDate.getMonth(), 1)
  const firstWeekday = (monthStart.getDay() + 6) % 7
  const gridStart = new Date(monthStart)
  gridStart.setDate(monthStart.getDate() - firstWeekday)
  const countMap = buildDayCountMap()
  const todayText = formatDate(new Date())

  return Array.from({ length: 42 }, (_, index) => {
    const current = new Date(gridStart)
    current.setDate(gridStart.getDate() + index)
    const dateText = formatDate(current)
    const total = countMap.get(dateText) || 0
    return {
      date: dateText,
      day: current.getDate(),
      currentMonth: current.getMonth() === baseDate.getMonth(),
      today: dateText === todayText,
      selected: dateText === selectedMonthDate.value,
      totalCount: total,
      hasReservations: total > 0
    }
  })
}

/**
 * 同步月视图选中日期。
 */
function syncSelectedMonthDate() {
  if (viewType.value !== 'month' || !selectedMonthDate.value) {
    return
  }
  const exists = dayList.value.some(day => day.date === selectedMonthDate.value)
  if (!exists) {
    selectedMonthDate.value = ''
  }
}

/**
 * 构建日期预约数量映射。
 * @returns {Map} 日期预约数量映射
 */
function buildDayCountMap() {
  return new Map(dayList.value.map(day => [day.date, day.totalCount || 0]))
}

/**
 * 解析 yyyy-MM-dd 日期。
 * @param {String} value 日期字符串
 * @returns {Date} 日期对象
 */
function parseDate(value) {
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) {
    return new Date()
  }
  return new Date(year, month - 1, day)
}

/**
 * 日期格式化为 yyyy-MM-dd。
 * @param {Date} date 日期对象
 * @returns {String} 日期字符串
 */
function formatDate(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
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

.month-panel {
  background-color: #fff;
  border-radius: 12rpx;
  padding: 18rpx 14rpx 20rpx;
  margin-bottom: 16rpx;
}

.month-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8rpx 14rpx;
}

.month-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #222;
}

.month-nav {
  min-width: 108rpx;
  font-size: 24rpx;
  color: #d93025;
}

.month-nav:last-child {
  text-align: right;
}

.week-row,
.month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.week-cell {
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 24rpx;
  color: #666;
}

.month-cell {
  min-height: 82rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #333;
}

.month-cell.muted {
  color: #b8b8b8;
}

.date-badge {
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.month-cell.today .date-badge {
  border: 2rpx solid #d93025;
  box-sizing: border-box;
}

.month-cell.reserved .date-badge {
  background-color: #e53935;
  color: #fff;
  border-color: #e53935;
}

.month-cell.selected .date-badge {
  box-shadow: 0 0 0 6rpx rgba(229, 57, 53, 0.16);
}

.month-cell.selected:not(.reserved) .date-badge {
  background-color: #fff5f5;
  color: #d93025;
  border: 2rpx solid #d93025;
  box-sizing: border-box;
}

.date-number {
  font-size: 26rpx;
  font-weight: 500;
}

.date-count {
  margin-top: 4rpx;
  font-size: 18rpx;
  line-height: 22rpx;
  color: #e53935;
}

.month-cell.reserved .date-count {
  color: #e53935;
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
