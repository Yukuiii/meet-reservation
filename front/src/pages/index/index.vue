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
      <view class="empty-state" v-if="filteredRoomList.length === 0">
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

<script>
/**
 * 首页 - 会议室列表
 * @description 展示会议室列表，支持按容量、设备、位置筛选
 */
export default {
  data() {
    return {
      // 搜索关键词
      searchKeyword: '',
      // 筛选条件
      selectedCapacity: '',
      selectedCapacityValue: '',
      selectedEquipment: '',
      selectedEquipmentValue: '',
      selectedLocation: '',
      selectedLocationValue: '',
      // 弹窗显示状态
      showCapacityPicker: false,
      showEquipmentPicker: false,
      showLocationPicker: false,
      // 容量选项
      capacityOptions: [
        { label: '10人以下', value: 'small', max: 10 },
        { label: '10-20人', value: 'medium', min: 10, max: 20 },
        { label: '20人以上', value: 'large', min: 20 }
      ],
      // 设备选项
      equipmentOptions: [
        { label: '投影仪', value: 'projector' },
        { label: '白板', value: 'whiteboard' },
        { label: '视频会议', value: 'video' },
        { label: '音响系统', value: 'audio' }
      ],
      // 位置选项
      locationOptions: [
        { label: 'A栋', value: 'A' },
        { label: 'B栋', value: 'B' },
        { label: 'C栋', value: 'C' }
      ],
      // 会议室列表（模拟数据）
      roomList: [
        {
          id: 1,
          name: '第一会议室',
          capacity: 8,
          location: 'A栋3楼',
          locationBuilding: 'A',
          image: 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=400&h=400&fit=crop',
          equipment: ['投影仪', '白板'],
          status: 'available',
          statusText: '空闲'
        },
        {
          id: 2,
          name: '第二会议室',
          capacity: 15,
          location: 'A栋5楼',
          locationBuilding: 'A',
          image: 'https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=400&h=400&fit=crop',
          equipment: ['投影仪', '视频会议', '音响系统'],
          status: 'occupied',
          statusText: '使用中'
        },
        {
          id: 3,
          name: '大型报告厅',
          capacity: 50,
          location: 'B栋1楼',
          locationBuilding: 'B',
          image: 'https://images.unsplash.com/photo-1517502884422-41eaead166d4?w=400&h=400&fit=crop',
          equipment: ['投影仪', '白板', '视频会议', '音响系统'],
          status: 'available',
          statusText: '空闲'
        },
        {
          id: 4,
          name: '小型洽谈室',
          capacity: 6,
          location: 'B栋2楼',
          locationBuilding: 'B',
          image: 'https://images.unsplash.com/photo-1462826303086-329426d1aef5?w=400&h=400&fit=crop',
          equipment: ['白板'],
          status: 'reserved',
          statusText: '已预约'
        },
        {
          id: 5,
          name: '多功能会议室',
          capacity: 25,
          location: 'C栋3楼',
          locationBuilding: 'C',
          image: 'https://images.unsplash.com/photo-1503423571797-2d2bb372094a?w=400&h=400&fit=crop',
          equipment: ['投影仪', '视频会议', '音响系统'],
          status: 'available',
          statusText: '空闲'
        },
        {
          id: 6,
          name: '培训室',
          capacity: 30,
          location: 'C栋4楼',
          locationBuilding: 'C',
          image: 'https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=400&h=400&fit=crop',
          equipment: ['投影仪', '白板', '音响系统'],
          status: 'occupied',
          statusText: '使用中'
        }
      ]
    }
  },

  computed: {
    /**
     * 根据筛选条件过滤会议室列表
     * @returns {Array} 过滤后的会议室列表
     */
    filteredRoomList() {
      let result = this.roomList

      // 按关键词搜索
      if (this.searchKeyword) {
        const keyword = this.searchKeyword.toLowerCase()
        result = result.filter(room =>
          room.name.toLowerCase().includes(keyword)
        )
      }

      // 按容量筛选
      if (this.selectedCapacityValue) {
        const option = this.capacityOptions.find(
          o => o.value === this.selectedCapacityValue
        )
        if (option) {
          result = result.filter(room => {
            if (option.max && !option.min) {
              return room.capacity < option.max
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
      if (this.selectedEquipmentValue) {
        const equipName = this.equipmentOptions.find(
          o => o.value === this.selectedEquipmentValue
        )?.label
        if (equipName) {
          result = result.filter(room =>
            room.equipment.includes(equipName)
          )
        }
      }

      // 按位置筛选
      if (this.selectedLocationValue) {
        result = result.filter(room =>
          room.locationBuilding === this.selectedLocationValue
        )
      }

      return result
    }
  },

  methods: {
    /**
     * 处理搜索输入
     */
    handleSearch() {
      // 搜索逻辑已在 computed 中实现
    },

    /**
     * 选择容量
     * @param {Object} item 选中的容量选项
     */
    selectCapacity(item) {
      this.selectedCapacity = item.label
      this.selectedCapacityValue = item.value
      this.showCapacityPicker = false
    },

    /**
     * 选择设备
     * @param {Object} item 选中的设备选项
     */
    selectEquipment(item) {
      this.selectedEquipment = item.label
      this.selectedEquipmentValue = item.value
      this.showEquipmentPicker = false
    },

    /**
     * 选择位置
     * @param {Object} item 选中的位置选项
     */
    selectLocation(item) {
      this.selectedLocation = item.label
      this.selectedLocationValue = item.value
      this.showLocationPicker = false
    },

    /**
     * 重置筛选条件
     */
    resetFilter() {
      this.searchKeyword = ''
      this.selectedCapacity = ''
      this.selectedCapacityValue = ''
      this.selectedEquipment = ''
      this.selectedEquipmentValue = ''
      this.selectedLocation = ''
      this.selectedLocationValue = ''
    },

    /**
     * 跳转到会议室详情页
     * @param {Number} id 会议室ID
     */
    goToRoomDetail(id) {
      uni.navigateTo({
        url: `/pages/room/detail?id=${id}`
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
}

.room-card {
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
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.room-status {
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
</style>
