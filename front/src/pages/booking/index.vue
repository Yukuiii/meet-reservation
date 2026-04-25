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
      <view
        class="tab-item"
        :class="{ active: currentTab === 'finished' }"
        @click="switchTab('finished')"
      >
        已完成
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
        <view class="card-footer" v-if="item.canCheckIn || item.canCancel || item.canReportRepair">
          <button
            class="check-in-btn"
            v-if="item.canCheckIn"
            size="mini"
            :disabled="signingInId === item.id"
            @click.stop="checkInBooking(item.id)"
          >
            {{ signingInId === item.id ? '签到中...' : '签到' }}
          </button>
          <button
            class="cancel-btn"
            v-if="item.canCancel"
            size="mini"
            @click.stop="cancelBooking(item.id)"
          >
            取消预约
          </button>
          <button
            class="repair-btn"
            v-if="item.canReportRepair"
            size="mini"
            @click.stop="openRepairModal(item)"
          >
            设备报修
          </button>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && currentList.length === 0">
        <text>暂无预约记录</text>
      </view>
    </scroll-view>

    <!-- 预约详情弹窗 -->
    <view class="detail-modal-mask" v-if="detailModalVisible" @click="closeDetailModal">
      <view class="detail-modal" @click.stop>
        <view class="detail-modal-title">预约详情</view>
        <scroll-view class="detail-modal-body" scroll-y>
          <view class="detail-row" v-for="(item, index) in detailItems" :key="`${item.label}-${index}`">
            <text class="detail-label">{{ item.label }}：</text>
            <text class="detail-value">{{ item.value }}</text>
          </view>
        </scroll-view>
        <view class="detail-modal-footer" @click="closeDetailModal">确定</view>
      </view>
    </view>

    <!-- 设备报修弹窗 -->
    <view class="detail-modal-mask" v-if="repairModalVisible" @click="closeRepairModal">
      <view class="detail-modal" @click.stop>
        <view class="detail-modal-title">设备报修</view>
        <scroll-view class="detail-modal-body" scroll-y>
          <view class="detail-row">
            <text class="detail-label">会议室：</text>
            <text class="detail-value">{{ repairTargetReservation.roomName || '-' }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">预约日期：</text>
            <text class="detail-value">{{ repairTargetReservation.date || '-' }}</text>
          </view>
          <view class="repair-form-item">
            <text class="repair-label">故障设备</text>
            <picker
              :range="repairEquipmentOptions"
              range-key="name"
              :value="repairEquipmentIndex"
              @change="handleRepairEquipmentChange"
            >
              <view class="repair-picker">
                {{ repairEquipmentLabel || (repairEquipmentLoading ? '加载中...' : '请选择设备') }}
              </view>
            </picker>
          </view>
          <view class="repair-form-item">
            <text class="repair-label">故障描述</text>
            <textarea
              class="repair-textarea"
              v-model="repairForm.description"
              placeholder="请描述设备故障现象"
              placeholder-class="placeholder"
            />
          </view>
        </scroll-view>
        <view class="repair-modal-footer">
          <button class="repair-modal-btn cancel" @click="closeRepairModal">取消</button>
          <button class="repair-modal-btn confirm" :disabled="repairSubmitting" @click="submitRepair">
            {{ repairSubmitting ? '提交中...' : '提交报修' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '../../utils/request'
import { refreshNotificationBadge } from '../../utils/notification'

/**
 * 当前选中的标签。
 */
const currentTab = ref('pending')

/**
 * 预约列表。
 */
const bookingList = ref([])

/**
 * 加载状态。
 */
const loading = ref(false)

/**
 * 预约详情弹窗可见状态。
 */
const detailModalVisible = ref(false)

/**
 * 预约详情项（按行渲染）。
 */
const detailItems = ref([])

/**
 * 设备报修弹窗可见状态。
 */
const repairModalVisible = ref(false)

/**
 * 当前报修目标预约。
 */
const repairTargetReservation = ref({})

/**
 * 报修设备选项。
 */
const repairEquipmentOptions = ref([])

/**
 * 报修设备加载状态。
 */
const repairEquipmentLoading = ref(false)

/**
 * 报修提交状态。
 */
const repairSubmitting = ref(false)

/**
 * 报修表单。
 */
const repairForm = ref({
  equipmentId: '',
  description: ''
})

/**
 * 正在签到的预约ID。
 */
const signingInId = ref(null)

/**
 * 根据当前标签筛选预约列表。
 * @returns {Array} 筛选结果
 */
const currentList = computed(() =>
  bookingList.value.filter(item => item.statusKey === currentTab.value)
)

/**
 * 报修设备选择索引。
 * @returns {Number} picker索引
 */
const repairEquipmentIndex = computed(() => {
  const index = repairEquipmentOptions.value.findIndex(item => item.id === repairForm.value.equipmentId)
  return index >= 0 ? index : 0
})

/**
 * 报修设备选择文案。
 * @returns {String} 设备文案
 */
const repairEquipmentLabel = computed(() => {
  const option = repairEquipmentOptions.value.find(item => item.id === repairForm.value.equipmentId)
  if (!option) {
    return ''
  }
  return option.quantity > 1 ? `${option.name}（${option.quantity}个）` : option.name
})

/**
 * 页面展示时刷新预约列表。
 */
onShow(() => {
  const userId = getCurrentUserId()
  if (userId) {
    refreshNotificationBadge(userId)
  }
  loadBookingList()
})

/**
 * 切换标签。
 * @param {String} tab 标签名称
 */
function switchTab(tab) {
  currentTab.value = tab
}

/**
 * 获取当前登录用户ID。
 * @returns {Number|null}
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
 * 加载当前用户预约列表。
 */
async function loadBookingList() {
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
      url: `/api/reservations/my?userId=${userId}`,
      method: 'GET'
    })
    bookingList.value = Array.isArray(list) ? list : []
  } catch (error) {
    uni.showToast({ title: error.message || '预约记录加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 查看预约详情。
 * @param {Number} id 预约ID
 */
async function goToDetail(id) {
  const userId = getCurrentUserId()
  if (!userId) {
    return
  }

  try {
    const detail = await request({
      url: `/api/reservations/${id}?userId=${userId}`,
      method: 'GET'
    })

    detailItems.value = buildDetailItems(detail)
    detailModalVisible.value = true
  } catch (error) {
    uni.showToast({ title: error.message || '详情加载失败', icon: 'none' })
  }
}

/**
 * 构建预约详情展示项。
 * @param {Object} detail 预约详情
 * @returns {Array<{label: String, value: String}>}
 */
function buildDetailItems(detail) {
  const nextDetailItems = [
    { label: '预约编号', value: detail.reservationNo || '-' },
    { label: '会议室', value: detail.roomName || '-' },
    { label: '日期', value: detail.date || '-' },
    { label: '时段', value: detail.timeSlot || '-' },
    { label: '参与人数', value: `${detail.attendees || 0}人` },
    { label: '状态', value: detail.statusText || '-' },
    { label: '事由', value: detail.purpose || '-' }
  ]

  // 仅在字段有值时展示可选信息，避免空行。
  if (detail.cancelReason) {
    nextDetailItems.push({ label: '取消原因', value: detail.cancelReason })
  }
  if (detail.rejectReason) {
    nextDetailItems.push({ label: '拒绝原因', value: detail.rejectReason })
  }
  if (detail.checkInAt) {
    nextDetailItems.push({ label: '签到时间', value: detail.checkInAt })
  }
  if (detail.remark) {
    nextDetailItems.push({ label: '备注', value: detail.remark })
  }
  return nextDetailItems
}

/**
 * 关闭预约详情弹窗并清理数据。
 */
function closeDetailModal() {
  detailModalVisible.value = false
  detailItems.value = []
}

/**
 * 打开设备报修弹窗。
 * @param {Object} item 预约项
 */
async function openRepairModal(item) {
  const userId = getCurrentUserId()
  if (!userId) {
    return
  }

  repairTargetReservation.value = item || {}
  repairForm.value = { equipmentId: '', description: '' }
  repairEquipmentOptions.value = []
  repairModalVisible.value = true
  repairEquipmentLoading.value = true
  try {
    const list = await request({
      url: `/api/equipment-repairs/options?userId=${userId}&reservationId=${item.id}`,
      method: 'GET'
    })
    repairEquipmentOptions.value = Array.isArray(list) ? list : []
    if (repairEquipmentOptions.value.length > 0) {
      repairForm.value.equipmentId = repairEquipmentOptions.value[0].id
    }
  } catch (error) {
    uni.showToast({ title: error.message || '设备加载失败', icon: 'none' })
  } finally {
    repairEquipmentLoading.value = false
  }
}

/**
 * 关闭设备报修弹窗。
 */
function closeRepairModal() {
  repairModalVisible.value = false
  repairTargetReservation.value = {}
  repairEquipmentOptions.value = []
  repairForm.value = { equipmentId: '', description: '' }
}

/**
 * 处理报修设备选择变化。
 * @param {Object} event picker事件
 */
function handleRepairEquipmentChange(event) {
  const option = repairEquipmentOptions.value[Number(event.detail.value)]
  if (option) {
    repairForm.value.equipmentId = option.id
  }
}

/**
 * 提交设备报修。
 */
async function submitRepair() {
  const userId = getCurrentUserId()
  if (!userId || repairSubmitting.value) {
    return
  }
  if (!repairForm.value.equipmentId) {
    uni.showToast({ title: '请选择故障设备', icon: 'none' })
    return
  }
  const description = (repairForm.value.description || '').trim()
  if (!description) {
    uni.showToast({ title: '请输入故障描述', icon: 'none' })
    return
  }

  repairSubmitting.value = true
  try {
    await request({
      url: '/api/equipment-repairs',
      method: 'POST',
      data: {
        userId,
        reservationId: repairTargetReservation.value.id,
        equipmentId: repairForm.value.equipmentId,
        description
      }
    })
    uni.showToast({ title: '报修已提交', icon: 'success' })
    closeRepairModal()
  } catch (error) {
    uni.showToast({ title: error.message || '提交报修失败', icon: 'none' })
  } finally {
    repairSubmitting.value = false
  }
}

/**
 * 预约签到。
 * @param {Number} id 预约ID
 */
async function checkInBooking(id) {
  const userId = getCurrentUserId()
  if (!userId || signingInId.value === id) {
    return
  }

  signingInId.value = id
  try {
    await request({
      url: `/api/reservations/${id}/check-in`,
      method: 'POST',
      data: {
        userId
      }
    })
    uni.showToast({ title: '签到成功', icon: 'success' })
    loadBookingList()
  } catch (error) {
    uni.showToast({ title: error.message || '签到失败', icon: 'none' })
  } finally {
    signingInId.value = null
  }
}

/**
 * 取消预约。
 * @param {Number} id 预约ID
 */
function cancelBooking(id) {
  const userId = getCurrentUserId()
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
        loadBookingList()
      } catch (error) {
        uni.showToast({ title: error.message || '取消失败', icon: 'none' })
      }
    }
  })
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

.status.finished {
  background-color: #e3f2fd;
  color: #1976d2;
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

.check-in-btn {
  font-size: 24rpx;
  color: #fff;
  background-color: #4caf50;
  border: none;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
}

.check-in-btn::after {
  border: none;
}

.check-in-btn[disabled] {
  opacity: 0.7;
}

.cancel-btn {
  font-size: 24rpx;
  color: #f44336;
  background-color: #ffebee;
  border: none;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  margin-left: 12rpx;
}

.cancel-btn::after {
  border: none;
}

.repair-btn {
  font-size: 24rpx;
  color: #007aff;
  background-color: #e3f2fd;
  border: none;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  margin-left: 12rpx;
}

.repair-btn::after {
  border: none;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}

/* 预约详情弹窗 */
.detail-modal-mask {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 48rpx;
  background-color: rgba(0, 0, 0, 0.45);
}

.detail-modal {
  width: 100%;
  max-height: 70vh;
  background-color: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.detail-modal-title {
  padding: 28rpx 32rpx 20rpx;
  text-align: center;
  font-size: 34rpx;
  font-weight: bold;
  color: #333;
}

.detail-modal-body {
  max-height: 560rpx;
  padding: 0 32rpx 20rpx;
  box-sizing: border-box;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f1f1f1;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  width: 170rpx;
  flex-shrink: 0;
  color: #666;
  font-size: 28rpx;
  line-height: 40rpx;
}

.detail-value {
  flex: 1;
  color: #333;
  font-size: 28rpx;
  line-height: 40rpx;
  word-break: break-all;
}

.detail-modal-footer {
  border-top: 1rpx solid #eee;
  text-align: center;
  color: #007aff;
  font-size: 30rpx;
  line-height: 96rpx;
  font-weight: 600;
}

.repair-form-item {
  padding: 16rpx 0;
}

.repair-label {
  display: block;
  color: #666;
  font-size: 26rpx;
  margin-bottom: 12rpx;
}

.repair-picker {
  min-height: 72rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 10rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #333;
  background-color: #fff;
}

.repair-textarea {
  width: 100%;
  min-height: 150rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 10rpx;
  padding: 16rpx 20rpx;
  box-sizing: border-box;
  background-color: #fff;
  font-size: 26rpx;
  color: #333;
}

.repair-modal-footer {
  display: flex;
  border-top: 1rpx solid #eee;
}

.repair-modal-btn {
  flex: 1;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 0;
  font-size: 28rpx;
}

.repair-modal-btn::after {
  border: none;
}

.repair-modal-btn.cancel {
  color: #666;
  background-color: #f8f8f8;
}

.repair-modal-btn.confirm {
  color: #fff;
  background-color: #007aff;
}

.repair-modal-btn[disabled] {
  opacity: 0.7;
}
</style>
