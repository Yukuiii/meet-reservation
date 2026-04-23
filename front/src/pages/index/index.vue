<template>
  <view class="container">
    <!-- 搜索和筛选区域 -->
    <view class="filter-section">
      <view class="search-box">
        <input
          class="search-input"
          type="text"
          v-model="searchKeyword"
          placeholder="搜索会议室名称"
          placeholder-class="placeholder"
          @input="handleSearch"
        />
      </view>

      <!-- 筛选条件 -->
      <view class="filter-bar">
        <view class="filter-item" @click="showCapacityPicker = true">
          <text>{{ selectedCapacity || '容量' }}</text>
          <text class="arrow">▼</text>
        </view>
        <view class="filter-item" @click="showEquipmentPicker = true">
          <text>{{ selectedEquipment || '设备' }}</text>
          <text class="arrow">▼</text>
        </view>
        <view class="filter-item" @click="showLocationPicker = true">
          <text>{{ selectedLocation || '位置' }}</text>
          <text class="arrow">▼</text>
        </view>
        <view class="filter-item reset-btn" @click="resetFilter">
          <text>重置</text>
        </view>
      </view>
    </view>

    <!-- 会议室列表 -->
    <scroll-view class="room-list" scroll-y>
      <view class="loading-state" v-if="loading">
        <text>会议室信息加载中...</text>
      </view>

      <view
        class="room-card"
        v-for="room in filteredRoomList"
        :key="room.id"
        @click="goToRoomDetail(room.id)"
      >
        <image class="room-image" :src="room.image" mode="aspectFill"></image>
        <view class="room-info">
          <view class="room-header">
            <text class="room-name">{{ room.name }}</text>
            <text class="room-status" :class="room.status">
              {{ room.statusText }}
            </text>
          </view>
          <view class="room-detail">
            <text class="detail-item">容量：{{ room.capacity }}人</text>
            <text class="detail-item">位置：{{ room.location }}</text>
          </view>
          <view class="room-equipment">
            <text
              class="equipment-tag"
              v-for="(equip, index) in room.equipment"
              :key="index"
            >
              {{ equip }}
            </text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && filteredRoomList.length === 0">
        <text>暂无符合条件的会议室</text>
      </view>
    </scroll-view>

    <!-- 容量选择弹窗 -->
    <view class="picker-mask" v-if="showCapacityPicker" @click="showCapacityPicker = false">
      <view class="picker-content" @click.stop>
        <view class="picker-title">选择容量</view>
        <view
          class="picker-option"
          v-for="item in capacityOptions"
          :key="item.value"
          @click="selectCapacity(item)"
        >
          {{ item.label }}
        </view>
      </view>
    </view>

    <!-- 设备选择弹窗 -->
    <view class="picker-mask" v-if="showEquipmentPicker" @click="showEquipmentPicker = false">
      <view class="picker-content" @click.stop>
        <view class="picker-title">选择设备</view>
        <view class="picker-option disabled" v-if="equipmentOptions.length === 0">
          暂无设备选项
        </view>
        <view
          class="picker-option"
          v-for="item in equipmentOptions"
          :key="item.value"
          @click="selectEquipment(item)"
        >
          {{ item.label }}
        </view>
      </view>
    </view>

    <!-- 位置选择弹窗 -->
    <view class="picker-mask" v-if="showLocationPicker" @click="showLocationPicker = false">
      <view class="picker-content" @click.stop>
        <view class="picker-title">选择位置</view>
        <view class="picker-option disabled" v-if="locationOptions.length === 0">
          暂无位置选项
        </view>
        <view
          class="picker-option"
          v-for="item in locationOptions"
          :key="item.value"
          @click="selectLocation(item)"
        >
          {{ item.label }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { buildAssetUrl, request } from '../../utils/request'
import { refreshNotificationBadge } from '../../utils/notification'

const DEFAULT_ROOM_IMAGE = '/images/meeting-room-default.jpg'

/**
 * 搜索关键词。
 */
const searchKeyword = ref('')

/**
 * 筛选条件。
 */
const selectedCapacity = ref('')
const selectedCapacityValue = ref('')
const selectedEquipment = ref('')
const selectedEquipmentValue = ref('')
const selectedLocation = ref('')
const selectedLocationValue = ref('')

/**
 * 弹窗显示状态。
 */
const showCapacityPicker = ref(false)
const showEquipmentPicker = ref(false)
const showLocationPicker = ref(false)

/**
 * 容量选项。
 */
const capacityOptions = [
  { label: '10人以下', value: 'small', max: 10 },
  { label: '10-20人', value: 'medium', min: 10, max: 20 },
  { label: '20人以上', value: 'large', min: 20 }
]

/**
 * 设备选项（根据接口数据动态生成）。
 */
const equipmentOptions = ref([])

/**
 * 位置选项（根据接口数据动态生成）。
 */
const locationOptions = ref([])

/**
 * 会议室列表。
 */
const roomList = ref([])

/**
 * 加载状态。
 */
const loading = ref(false)

/**
 * 根据筛选条件过滤会议室列表。
 * @returns {Array} 过滤后的会议室列表
 */
const filteredRoomList = computed(() => {
  let result = roomList.value

  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    result = result.filter(room =>
      (room.name || '').toLowerCase().includes(keyword) ||
      (room.location || '').toLowerCase().includes(keyword)
    )
  }

  // 按容量筛选
  if (selectedCapacityValue.value) {
    const option = capacityOptions.find(
      o => o.value === selectedCapacityValue.value
    )
    if (option) {
      result = result.filter(room => {
        if (option.max && !option.min) {
          return room.capacity <= option.max
        } else if (option.min && option.max) {
          return room.capacity >= option.min && room.capacity <= option.max
        } else if (option.min && !option.max) {
          return room.capacity >= option.min
        }
        return true
      })
    }
  }

  // 按设备筛选
  if (selectedEquipmentValue.value) {
    result = result.filter(room =>
      Array.isArray(room.equipment) &&
        room.equipment.includes(selectedEquipmentValue.value)
      )
  }

  // 按位置筛选
  if (selectedLocationValue.value) {
    result = result.filter(room =>
      room.locationBuilding === selectedLocationValue.value
    )
  }

  return result
})

/**
 * 页面加载时拉取会议室列表数据。
 */
onLoad(() => {
  loadRoomList()
})

/**
 * 页面显示时刷新会议室列表。
 */
onShow(() => {
  loadRoomList()
  refreshMessageBadge()
})

/**
 * 刷新当前用户的消息角标。
 */
function refreshMessageBadge() {
  const userInfo = uni.getStorageSync('userInfo') || {}
  if (userInfo.id) {
    refreshNotificationBadge(userInfo.id)
  }
}

/**
 * 拉取会议室列表。
 */
async function loadRoomList() {
  loading.value = true
  try {
    const list = await request({
      url: '/api/meeting-rooms',
      method: 'GET'
    })
    roomList.value = Array.isArray(list)
      ? list.map(item => ({
        ...item,
        image: buildAssetUrl(item.image || DEFAULT_ROOM_IMAGE)
      }))
      : []
    buildFilterOptions()
  } catch (error) {
    uni.showToast({ title: error.message || '加载会议室失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 根据会议室数据构建筛选选项。
 */
function buildFilterOptions() {
  const equipmentSet = new Set()
  const buildingSet = new Set()

  // 聚合所有设备与楼栋，保证筛选项与服务端数据一致。
  roomList.value.forEach(room => {
    if (Array.isArray(room.equipment)) {
      room.equipment.forEach(item => {
        if (item) {
          equipmentSet.add(item)
        }
      })
    }
    if (room.locationBuilding) {
      buildingSet.add(room.locationBuilding)
    }
  })

  equipmentOptions.value = Array.from(equipmentSet)
    .sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
    .map(item => ({ label: item, value: item }))

  locationOptions.value = Array.from(buildingSet)
    .sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
    .map(item => ({
      label: item.endsWith('栋') ? item : `${item}栋`,
      value: item
    }))

  // 如果接口返回的数据中不存在当前已选条件，自动清空，避免筛选锁死。
  if (selectedEquipmentValue.value && !equipmentSet.has(selectedEquipmentValue.value)) {
    selectedEquipment.value = ''
    selectedEquipmentValue.value = ''
  }
  if (selectedLocationValue.value && !buildingSet.has(selectedLocationValue.value)) {
    selectedLocation.value = ''
    selectedLocationValue.value = ''
  }
}

/**
 * 处理搜索输入。
 */
function handleSearch() {
  // 搜索逻辑已在 computed 中实现
}

/**
 * 选择容量。
 * @param {Object} item 选中的容量选项
 */
function selectCapacity(item) {
  selectedCapacity.value = item.label
  selectedCapacityValue.value = item.value
  showCapacityPicker.value = false
}

/**
 * 选择设备。
 * @param {Object} item 选中的设备选项
 */
function selectEquipment(item) {
  selectedEquipment.value = item.label
  selectedEquipmentValue.value = item.value
  showEquipmentPicker.value = false
}

/**
 * 选择位置。
 * @param {Object} item 选中的位置选项
 */
function selectLocation(item) {
  selectedLocation.value = item.label
  selectedLocationValue.value = item.value
  showLocationPicker.value = false
}

/**
 * 重置筛选条件。
 */
function resetFilter() {
  searchKeyword.value = ''
  selectedCapacity.value = ''
  selectedCapacityValue.value = ''
  selectedEquipment.value = ''
  selectedEquipmentValue.value = ''
  selectedLocation.value = ''
  selectedLocationValue.value = ''
}

/**
 * 跳转到会议室详情页。
 * @param {Number} id 会议室ID
 */
function goToRoomDetail(id) {
  uni.navigateTo({
    url: `/pages/room/detail?id=${id}`
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

/* 筛选区域 */
.filter-section {
  background-color: #fff;
  padding: 20rpx;
}

.search-box {
  margin-bottom: 20rpx;
}

.search-input {
  width: 100%;
  height: 72rpx;
  padding: 0 24rpx;
  background-color: #f5f5f5;
  border-radius: 36rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.placeholder {
  color: #999;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
}

.filter-item {
  flex: 1;
  height: 60rpx;
  line-height: 60rpx;
  text-align: center;
  font-size: 26rpx;
  color: #333;
  background-color: #f5f5f5;
  border-radius: 8rpx;
  margin-right: 16rpx;
}

.filter-item:last-child {
  margin-right: 0;
}

.filter-item .arrow {
  font-size: 20rpx;
  margin-left: 8rpx;
  color: #999;
}

.reset-btn {
  flex: 0.6;
  background-color: #007aff;
  color: #fff;
}

/* 会议室列表 */
.room-list {
  flex: 1;
  padding: 20rpx;
  box-sizing: border-box;
}

.room-card {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.room-image {
  width: 200rpx;
  height: 200rpx;
  flex-shrink: 0;
}

.room-info {
  flex: 1;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-name {
  flex: 1;
  min-width: 0;
  margin-right: 12rpx;
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-status {
  flex-shrink: 0;
  font-size: 24rpx;
  padding: 4rpx 16rpx;
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

.room-detail {
  font-size: 26rpx;
  color: #666;
}

.detail-item {
  margin-right: 20rpx;
}

.room-equipment {
  display: flex;
  flex-wrap: wrap;
}

.equipment-tag {
  font-size: 22rpx;
  color: #007aff;
  background-color: #e3f2fd;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
  margin-right: 12rpx;
  margin-top: 8rpx;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}

.loading-state {
  text-align: center;
  padding: 100rpx 0;
  color: #666;
  font-size: 28rpx;
}

/* 选择器弹窗 */
.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.picker-content {
  width: 100%;
  background-color: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.picker-title {
  text-align: center;
  padding: 30rpx;
  font-size: 32rpx;
  font-weight: bold;
  border-bottom: 1rpx solid #eee;
}

.picker-option {
  padding: 30rpx;
  text-align: center;
  font-size: 30rpx;
  border-bottom: 1rpx solid #eee;
}

.picker-option:active {
  background-color: #f5f5f5;
}

.picker-option.disabled {
  color: #999;
}
</style>
