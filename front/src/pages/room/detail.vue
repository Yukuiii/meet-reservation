<template>
  <view class="container">
    <!-- 会议室图片 -->
    <image class="room-image" :src="roomInfo.image" mode="aspectFill"></image>

    <!-- 基本信息 -->
    <view class="info-section">
      <view class="room-header">
        <text class="room-name">{{ roomInfo.name }}</text>
        <text class="room-status" :class="roomInfo.status">
          {{ roomInfo.statusText }}
        </text>
      </view>
      <view class="info-row">
        <text class="label">容纳人数：</text>
        <text class="value">{{ roomInfo.capacity }}人</text>
      </view>
      <view class="info-row">
        <text class="label">所在位置：</text>
        <text class="value">{{ roomInfo.location }}</text>
      </view>
      <view class="info-row">
        <text class="label">设备配置：</text>
        <view class="equipment-list">
          <text
            class="equipment-tag"
            v-for="(item, index) in roomInfo.equipment"
            :key="index"
          >
            {{ item }}
          </text>
        </view>
      </view>
      <view class="info-row" v-if="roomInfo.description">
        <text class="label">会议室简介：</text>
        <text class="value">{{ roomInfo.description }}</text>
      </view>
    </view>

    <!-- 占用情况 -->
    <view class="schedule-section">
      <view class="section-title">实时占用状态</view>
      <view class="date-row">
        <text class="date-label">预约日期：</text>
        <picker mode="date" :value="selectedDate" :start="minDate" @change="handleDateChange">
          <view class="date-value">{{ selectedDate }}</view>
        </picker>
      </view>

      <view class="loading-state" v-if="scheduleLoading">
        <text>正在加载占用状态...</text>
      </view>

      <view class="occupied-list" v-else-if="scheduleList.length > 0">
        <view
          class="occupied-item"
          v-for="item in scheduleList"
          :key="item.id"
        >
          <text class="occupied-time">{{ item.startTime }}-{{ item.endTime }}</text>
          <text class="occupied-title">{{ item.title || item.statusText || '已占用' }}</text>
        </view>
      </view>
      <view class="day-empty" v-else>
        <text>当天暂无占用</text>
      </view>
    </view>

    <!-- 预约表单 -->
    <view class="booking-section">
      <view class="section-title">预约信息</view>
      <view class="form-item">
        <text class="label">开始时间：</text>
        <picker mode="time" :value="bookingForm.startTime" @change="handleStartTimeChange">
          <view class="picker-value">{{ bookingForm.startTime || '请选择开始时间' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">结束时间：</text>
        <picker mode="time" :value="bookingForm.endTime" @change="handleEndTimeChange">
          <view class="picker-value">{{ bookingForm.endTime || '请选择结束时间' }}</view>
        </picker>
      </view>
      <view class="rule-tip">
        <text>{{ reservationRuleText }}</text>
      </view>
      <view class="form-item">
        <text class="label">预约事由：</text>
        <input
          class="input"
          type="text"
          v-model="bookingForm.reason"
          placeholder="请输入预约事由"
          placeholder-class="placeholder"
        />
      </view>
      <view class="form-item">
        <text class="label">参与人数：</text>
        <input
          class="input"
          type="number"
          v-model="bookingForm.attendees"
          placeholder="请输入参与人数"
          placeholder-class="placeholder"
        />
      </view>
    </view>

    <!-- 底部按钮 -->
    <view class="bottom-bar">
      <button class="booking-btn" :disabled="submitting" @click="submitBooking">
        {{ submitting ? '提交中...' : '立即预约' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { buildAssetUrl, request } from '../../utils/request'

const DEFAULT_ROOM_IMAGE = '/images/meeting-room-default.jpg'

/**
 * 会议室ID。
 */
const roomId = ref(null)

/**
 * 会议室信息。
 */
const roomInfo = ref({
  id: null,
  name: '',
  capacity: 0,
  location: '',
  image: buildAssetUrl(DEFAULT_ROOM_IMAGE),
  equipment: [],
  status: 'available',
  statusText: '可预约',
  description: ''
})

/**
 * 预约日期。
 */
const selectedDate = ref('')

/**
 * 最小可预约日期。
 */
const minDate = ref('')

/**
 * 当日占用列表。
 */
const scheduleList = ref([])

/**
 * 预约表单。
 */
const bookingForm = reactive({
  reason: '',
  attendees: '',
  startTime: '09:00',
  endTime: '10:00'
})

/**
 * 预约规则。
 */
const reservationRule = ref({
  maxDurationMinutes: 120,
  minAdvanceMinutes: 0
})

/**
 * 占用状态加载中。
 */
const scheduleLoading = ref(false)

/**
 * 提交中。
 */
const submitting = ref(false)

/**
 * 预约规则文案。
 * @returns {String}
 */
const reservationRuleText = computed(() =>
  `单次最长${reservationRule.value.maxDurationMinutes}分钟，至少提前${reservationRule.value.minAdvanceMinutes}分钟预约`
)

/**
 * 页面初始化。
 * @param {Object} options 页面参数
 */
onLoad(async (options) => {
  if (!options.id) {
    uni.showToast({ title: '会议室参数错误', icon: 'none' })
    return
  }

  roomId.value = Number(options.id)
  const today = formatDate(new Date())
  selectedDate.value = today
  minDate.value = today

  await loadReservationRule()
  await loadRoomDetail()
  await loadRoomSchedule()
})

/**
 * 加载预约规则。
 */
async function loadReservationRule() {
  try {
    const rule = await request({
      url: '/api/reservation-rules',
      method: 'GET'
    })
    reservationRule.value = {
      maxDurationMinutes: rule.maxDurationMinutes || 120,
      minAdvanceMinutes: rule.minAdvanceMinutes || 0
    }
  } catch (error) {
    uni.showToast({ title: error.message || '预约规则加载失败', icon: 'none' })
  }
}

/**
 * 加载会议室详情。
 */
async function loadRoomDetail() {
  try {
    const room = await request({
      url: `/api/meeting-rooms/${roomId.value}`,
      method: 'GET'
    })

    roomInfo.value = {
      id: room.id,
      name: room.name || '',
      capacity: room.capacity || 0,
      location: room.location || '',
      image: buildAssetUrl(room.image || DEFAULT_ROOM_IMAGE),
      equipment: Array.isArray(room.equipment) ? room.equipment : [],
      status: room.status || 'available',
      statusText: room.statusText || '可预约',
      description: room.description || ''
    }
  } catch (error) {
    uni.showToast({ title: error.message || '会议室加载失败', icon: 'none' })
  }
}

/**
 * 加载指定日期的占用状态。
 */
async function loadRoomSchedule() {
  scheduleLoading.value = true
  try {
    const scheduleList = await request({
      url: `/api/reservations/schedule?roomId=${roomId.value}&date=${selectedDate.value}`,
      method: 'GET'
    })

    syncScheduleList(Array.isArray(scheduleList) ? scheduleList : [])
  } catch (error) {
    uni.showToast({ title: error.message || '占用状态加载失败', icon: 'none' })
    syncScheduleList([])
  } finally {
    scheduleLoading.value = false
  }
}

/**
 * 同步后端占用时段。
 * @param {Array} scheduleList 占用时段列表
 */
function syncScheduleList(nextScheduleList) {
  scheduleList.value = nextScheduleList
}

/**
 * 判断两个时间区间是否重叠。
 * @param {String} startA 区间A开始 HH:mm
 * @param {String} endA 区间A结束 HH:mm
 * @param {String} startB 区间B开始 HH:mm
 * @param {String} endB 区间B结束 HH:mm
 * @returns {Boolean}
 */
function isTimeOverlap(startA, endA, startB, endB) {
  return startA < endB && endA > startB
}

/**
 * 切换预约日期。
 * @param {Object} event 日期选择事件
 */
async function handleDateChange(event) {
  selectedDate.value = event.detail.value
  bookingForm.reason = ''
  bookingForm.attendees = ''
  await loadRoomSchedule()
}

/**
 * 选择开始时间。
 * @param {Object} event 时间选择事件
 */
function handleStartTimeChange(event) {
  bookingForm.startTime = event.detail.value
}

/**
 * 选择结束时间。
 * @param {Object} event 时间选择事件
 */
function handleEndTimeChange(event) {
  bookingForm.endTime = event.detail.value
}

/**
 * 提交预约。
 */
async function submitBooking() {
  if (!bookingForm.startTime || !bookingForm.endTime) {
    uni.showToast({ title: '请选择预约时间', icon: 'none' })
    return
  }

  if (bookingForm.startTime >= bookingForm.endTime) {
    uni.showToast({ title: '开始时间必须早于结束时间', icon: 'none' })
    return
  }

  const reason = bookingForm.reason.trim()
  if (!reason) {
    uni.showToast({ title: '请输入预约事由', icon: 'none' })
    return
  }

  const attendees = Number(bookingForm.attendees)
  if (!Number.isInteger(attendees) || attendees <= 0) {
    uni.showToast({ title: '参与人数需为正整数', icon: 'none' })
    return
  }
  if (attendees > roomInfo.value.capacity) {
    uni.showToast({
      title: `参与人数不能超过会议室容量(${roomInfo.value.capacity}人)`,
      icon: 'none'
    })
    return
  }

  const userInfo = uni.getStorageSync('userInfo') || {}
  if (!userInfo.id) {
    uni.showToast({ title: '请先登录后再预约', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/index' })
    }, 600)
    return
  }

  if (submitting.value) {
    return
  }

  const startTime = bookingForm.startTime
  const endTime = bookingForm.endTime
  if (isStartedTimeSlot(selectedDate.value, startTime)) {
    uni.showToast({ title: '今天已开始的时段不可预约', icon: 'none' })
    return
  }
  if (isBeforeMinAdvance(selectedDate.value, startTime)) {
    uni.showToast({ title: `需至少提前${reservationRule.value.minAdvanceMinutes}分钟预约`, icon: 'none' })
    return
  }
  if (calculateDurationMinutes(startTime, endTime) > reservationRule.value.maxDurationMinutes) {
    uni.showToast({ title: `单次预约不能超过${reservationRule.value.maxDurationMinutes}分钟`, icon: 'none' })
    return
  }
  if (hasScheduleConflict(startTime, endTime)) {
    uni.showToast({ title: '所选时间段已被占用', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await request({
      url: '/api/reservations',
      method: 'POST',
      data: {
        userId: userInfo.id,
        roomId: roomId.value,
        title: reason,
        purpose: reason,
        attendeeCount: attendees,
        reservationDate: selectedDate.value,
        startTime: `${startTime}:00`,
        endTime: `${endTime}:00`
      }
    })

    uni.showToast({ title: '预约提交成功', icon: 'success' })
    bookingForm.reason = ''
    bookingForm.attendees = ''
    navigateToMyReservations()
  } catch (error) {
    uni.showToast({ title: error.message || '预约提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

/**
 * 判断时段是否已经达到开始时间。
 * @param {String} dateText 日期字符串
 * @param {String} startTime 开始时间 HH:mm
 * @returns {Boolean}
 */
function isStartedTimeSlot(dateText, startTime) {
  return dateText === formatDate(new Date()) && startTime <= currentTimeText()
}

/**
 * 判断是否不满足最少提前预约时间。
 * @param {String} dateText 日期字符串
 * @param {String} startTime 开始时间 HH:mm
 * @returns {Boolean}
 */
function isBeforeMinAdvance(dateText, startTime) {
  const startDateTime = parseDateTime(dateText, startTime)
  const earliestTime = new Date(Date.now() + reservationRule.value.minAdvanceMinutes * 60 * 1000)
  return startDateTime < earliestTime
}

/**
 * 判断所选时间是否和已有占用冲突。
 * @param {String} startTime 开始时间 HH:mm
 * @param {String} endTime 结束时间 HH:mm
 * @returns {Boolean}
 */
function hasScheduleConflict(startTime, endTime) {
  return scheduleList.value.some(item => isTimeOverlap(startTime, endTime, item.startTime, item.endTime))
}

/**
 * 计算时长分钟数。
 * @param {String} startTime 开始时间 HH:mm
 * @param {String} endTime 结束时间 HH:mm
 * @returns {Number}
 */
function calculateDurationMinutes(startTime, endTime) {
  const [startHour, startMinute] = startTime.split(':').map(Number)
  const [endHour, endMinute] = endTime.split(':').map(Number)
  return (endHour * 60 + endMinute) - (startHour * 60 + startMinute)
}

/**
 * 解析日期时间。
 * @param {String} dateText 日期字符串
 * @param {String} timeText 时间字符串
 * @returns {Date}
 */
function parseDateTime(dateText, timeText) {
  const [year, month, day] = dateText.split('-').map(Number)
  const [hour, minute] = timeText.split(':').map(Number)
  return new Date(year, month - 1, day, hour, minute, 0)
}

/**
 * 获取当前时间字符串。
 * @returns {String}
 */
function currentTimeText() {
  const now = new Date()
  const hour = `${now.getHours()}`.padStart(2, '0')
  const minute = `${now.getMinutes()}`.padStart(2, '0')
  return `${hour}:${minute}`
}

/**
 * 日期格式化为 yyyy-MM-dd。
 * @param {Date} date 日期对象
 * @returns {String}
 */
function formatDate(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 跳转到我的预约页。
 */
function navigateToMyReservations() {
  setTimeout(() => {
    uni.switchTab({ url: '/pages/booking/index' })
  }, 600)
}
</script>

<style scoped>
.container {
  min-height: 100%;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
}

.room-image {
  width: 100%;
  height: 400rpx;
}

/* 基本信息 */
.info-section {
  background-color: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.room-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.room-status {
  font-size: 24rpx;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
}

.room-status.available {
  background-color: #e8f5e9;
  color: #4caf50;
}

.room-status.occupied {
  background-color: #ffebee;
  color: #f44336;
}

.room-status.reserved {
  background-color: #fff3e0;
  color: #ff9800;
}

.info-row {
  display: flex;
  margin-bottom: 16rpx;
  font-size: 28rpx;
}

.info-row .label {
  color: #666;
  width: 160rpx;
  flex-shrink: 0;
}

.info-row .value {
  color: #333;
  flex: 1;
}

.equipment-list {
  display: flex;
  flex-wrap: wrap;
  flex: 1;
}

.equipment-tag {
  font-size: 24rpx;
  color: #007aff;
  background-color: #e3f2fd;
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  margin-right: 12rpx;
  margin-bottom: 8rpx;
}

/* 占用情况 */
.schedule-section {
  background-color: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.date-row {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
  font-size: 28rpx;
}

.date-label {
  color: #666;
  width: 160rpx;
  flex-shrink: 0;
}

.date-value {
  padding: 10rpx 20rpx;
  background-color: #f5f5f5;
  border-radius: 8rpx;
  color: #007aff;
}

.loading-state {
  text-align: center;
  color: #999;
  padding: 40rpx 0;
}

.occupied-list {
  display: flex;
  flex-direction: column;
}

.occupied-item {
  display: flex;
  align-items: center;
  padding: 16rpx 18rpx;
  border-radius: 10rpx;
  background-color: #ffebee;
  margin-bottom: 12rpx;
}

.occupied-item:last-child {
  margin-bottom: 0;
}

.occupied-time {
  width: 190rpx;
  color: #f44336;
  font-size: 25rpx;
  font-weight: 600;
}

.occupied-title {
  flex: 1;
  color: #666;
  font-size: 25rpx;
}

.day-empty {
  text-align: center;
  color: #999;
  font-size: 26rpx;
  padding: 34rpx 0;
}

/* 预约表单 */
.booking-section {
  background-color: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
  font-size: 28rpx;
}

.form-item .label {
  color: #666;
  width: 160rpx;
  flex-shrink: 0;
}

.form-item .value {
  color: #007aff;
  flex: 1;
}

.picker-value {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  background-color: #f5f5f5;
  border-radius: 8rpx;
  color: #007aff;
  font-size: 28rpx;
  line-height: 72rpx;
}

.form-item .input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  background-color: #f5f5f5;
  border-radius: 8rpx;
  font-size: 28rpx;
}

.placeholder {
  color: #ccc;
}

.rule-tip {
  color: #888;
  font-size: 24rpx;
  line-height: 34rpx;
  margin: -8rpx 0 20rpx 160rpx;
}

/* 底部按钮 */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.booking-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background-color: #007aff;
  color: #fff;
  font-size: 32rpx;
  border-radius: 44rpx;
}

.booking-btn::after {
  border: none;
}

.booking-btn[disabled] {
  opacity: 0.7;
}
</style>
