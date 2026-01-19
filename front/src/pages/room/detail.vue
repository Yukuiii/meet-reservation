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

    <!-- 今日预约情况 -->
    <view class="schedule-section">
      <view class="section-title">今日预约情况</view>
      <view class="time-slots">
        <view
          class="time-slot"
          v-for="(slot, index) in timeSlots"
          :key="index"
          :class="{ booked: slot.booked, selected: slot.selected }"
          @click="selectTimeSlot(index)"
        >
          <text class="slot-time">{{ slot.time }}</text>
          <text class="slot-status">{{ slot.booked ? '已预约' : '可预约' }}</text>
        </view>
      </view>
    </view>

    <!-- 预约表单 -->
    <view class="booking-section" v-if="selectedSlots.length > 0">
      <view class="section-title">预约信息</view>
      <view class="form-item">
        <text class="label">预约时段：</text>
        <text class="value">{{ selectedTimeText }}</text>
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
      <button class="booking-btn" @click="submitBooking">立即预约</button>
    </view>
  </view>
</template>

<script>
/**
 * 会议室详情页
 * @description 展示会议室详细信息，支持查看预约情况和提交预约
 */
export default {
  data() {
    return {
      // 会议室ID
      roomId: null,
      // 会议室信息（模拟数据）
      roomInfo: {
        id: 1,
        name: '第一会议室',
        capacity: 8,
        location: 'A栋3楼301室',
        image: 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=800&h=600&fit=crop',
        equipment: ['投影仪', '白板', '视频会议系统'],
        status: 'available',
        statusText: '空闲',
        description: '适合小型团队会议，配备高清投影仪和电子白板，支持远程视频会议。'
      },
      // 时间段列表
      timeSlots: [
        { time: '08:00-09:00', booked: false, selected: false },
        { time: '09:00-10:00', booked: true, selected: false },
        { time: '10:00-11:00', booked: true, selected: false },
        { time: '11:00-12:00', booked: false, selected: false },
        { time: '13:00-14:00', booked: false, selected: false },
        { time: '14:00-15:00', booked: true, selected: false },
        { time: '15:00-16:00', booked: false, selected: false },
        { time: '16:00-17:00', booked: false, selected: false },
        { time: '17:00-18:00', booked: false, selected: false }
      ],
      // 预约表单
      bookingForm: {
        reason: '',
        attendees: ''
      }
    }
  },

  computed: {
    /**
     * 已选择的时间段
     * @returns {Array} 选中的时间段索引数组
     */
    selectedSlots() {
      return this.timeSlots
        .map((slot, index) => ({ ...slot, index }))
        .filter(slot => slot.selected)
    },

    /**
     * 已选择的时间段文本
     * @returns {String} 格式化的时间段文本
     */
    selectedTimeText() {
      return this.selectedSlots.map(slot => slot.time).join('、')
    }
  },

  onLoad(options) {
    if (options.id) {
      this.roomId = options.id
      this.loadRoomDetail(options.id)
    }
  },

  methods: {
    /**
     * 加载会议室详情
     * @param {Number} id 会议室ID
     */
    loadRoomDetail(id) {
      // 模拟数据，根据ID返回不同的会议室信息
      const roomData = {
        1: {
          id: 1,
          name: '第一会议室',
          capacity: 8,
          location: 'A栋3楼301室',
          image: 'https://images.unsplash.com/photo-1497366216548-37526070297c?w=800&h=600&fit=crop',
          equipment: ['投影仪', '白板'],
          status: 'available',
          statusText: '空闲',
          description: '适合小型团队会议，配备高清投影仪和电子白板。'
        },
        2: {
          id: 2,
          name: '第二会议室',
          capacity: 15,
          location: 'A栋5楼502室',
          image: 'https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=800&h=600&fit=crop',
          equipment: ['投影仪', '视频会议', '音响系统'],
          status: 'occupied',
          statusText: '使用中',
          description: '中型会议室，配备专业视频会议系统，适合跨地区远程会议。'
        },
        3: {
          id: 3,
          name: '大型报告厅',
          capacity: 50,
          location: 'B栋1楼101室',
          image: 'https://images.unsplash.com/photo-1517502884422-41eaead166d4?w=800&h=600&fit=crop',
          equipment: ['投影仪', '白板', '视频会议', '音响系统'],
          status: 'available',
          statusText: '空闲',
          description: '大型报告厅，可容纳50人，适合公司大型会议、培训和演讲活动。'
        },
        4: {
          id: 4,
          name: '小型洽谈室',
          capacity: 6,
          location: 'B栋2楼205室',
          image: 'https://images.unsplash.com/photo-1462826303086-329426d1aef5?w=800&h=600&fit=crop',
          equipment: ['白板'],
          status: 'reserved',
          statusText: '已预约',
          description: '温馨小型洽谈室，适合商务洽谈和小组讨论。'
        },
        5: {
          id: 5,
          name: '多功能会议室',
          capacity: 25,
          location: 'C栋3楼308室',
          image: 'https://images.unsplash.com/photo-1503423571797-2d2bb372094a?w=800&h=600&fit=crop',
          equipment: ['投影仪', '视频会议', '音响系统'],
          status: 'available',
          statusText: '空闲',
          description: '多功能会议室，灵活布局，可根据需要调整座位排列。'
        },
        6: {
          id: 6,
          name: '培训室',
          capacity: 30,
          location: 'C栋4楼401室',
          image: 'https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=800&h=600&fit=crop',
          equipment: ['投影仪', '白板', '音响系统'],
          status: 'occupied',
          statusText: '使用中',
          description: '专业培训室，配备培训桌椅，适合内部培训和工作坊。'
        }
      }

      if (roomData[id]) {
        this.roomInfo = roomData[id]
      }
    },

    /**
     * 选择时间段
     * @param {Number} index 时间段索引
     */
    selectTimeSlot(index) {
      const slot = this.timeSlots[index]
      // 已预约的时间段不可选择
      if (slot.booked) {
        uni.showToast({ title: '该时段已被预约', icon: 'none' })
        return
      }
      slot.selected = !slot.selected
    },

    /**
     * 提交预约
     */
    submitBooking() {
      // 验证是否选择了时间段
      if (this.selectedSlots.length === 0) {
        uni.showToast({ title: '请选择预约时段', icon: 'none' })
        return
      }

      // 验证预约事由
      if (!this.bookingForm.reason.trim()) {
        uni.showToast({ title: '请输入预约事由', icon: 'none' })
        return
      }

      // 验证参与人数
      if (!this.bookingForm.attendees) {
        uni.showToast({ title: '请输入参与人数', icon: 'none' })
        return
      }

      const attendees = parseInt(this.bookingForm.attendees)
      if (attendees > this.roomInfo.capacity) {
        uni.showToast({
          title: `参与人数不能超过会议室容量(${this.roomInfo.capacity}人)`,
          icon: 'none'
        })
        return
      }

      // TODO: 调用预约接口
      uni.showToast({ title: '预约提交成功', icon: 'success' })

      // 重置表单
      setTimeout(() => {
        this.bookingForm = { reason: '', attendees: '' }
        this.timeSlots.forEach(slot => {
          if (slot.selected) {
            slot.selected = false
            slot.booked = true
          }
        })
      }, 1500)
    }
  }
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

/* 预约情况 */
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

.time-slots {
  display: flex;
  flex-wrap: wrap;
}

.time-slot {
  width: calc(33.33% - 16rpx);
  margin-right: 24rpx;
  margin-bottom: 20rpx;
  padding: 20rpx 0;
  text-align: center;
  background-color: #f5f5f5;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}

.time-slot:nth-child(3n) {
  margin-right: 0;
}

.time-slot.booked {
  background-color: #ffebee;
  color: #999;
}

.time-slot.selected {
  background-color: #e3f2fd;
  border-color: #007aff;
}

.slot-time {
  display: block;
  font-size: 26rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.time-slot.booked .slot-time {
  color: #999;
}

.slot-status {
  display: block;
  font-size: 22rpx;
  color: #4caf50;
}

.time-slot.booked .slot-status {
  color: #f44336;
}

.time-slot.selected .slot-status {
  color: #007aff;
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
</style>
