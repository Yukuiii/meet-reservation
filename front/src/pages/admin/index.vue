<template>
  <view class="container">
    <view class="header">
      <text class="title">管理员中心</text>
      <view class="header-actions">
        <text class="header-action" @click="goToCalendar">日历</text>
        <text class="header-action" @click="refreshCurrentTab">刷新</text>
        <text class="header-action danger" @click="handleLogout">退出</text>
      </view>
    </view>

    <view class="tabs">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'review' }"
        @click="switchTab('review')"
      >
        预约审核
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'room' }"
        @click="switchTab('room')"
      >
        会议室管理
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'stats' }"
        @click="switchTab('stats')"
      >
        统计数据
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'emergency' }"
        @click="switchTab('emergency')"
      >
        紧急占用
      </view>
    </view>

    <scroll-view class="content" scroll-y>
      <view class="panel" v-if="activeTab === 'review'">
        <view class="loading-state" v-if="reviewLoading">
          <text>待审核预约加载中...</text>
        </view>

        <template v-else-if="pendingReservations.length > 0">
          <view
            class="review-card"
            v-for="item in pendingReservations"
            :key="item.id"
          >
            <view class="card-header">
              <text class="card-title">{{ item.title || '未命名会议' }}</text>
              <text class="status-tag pending">待审核</text>
            </view>
            <view class="card-line">预约编号：{{ item.reservationNo }}</view>
            <view class="card-line">申请人：{{ item.nickname || item.username || '-' }}</view>
            <view class="card-line">会议室：{{ item.roomName }}</view>
            <view class="card-line">日期：{{ item.date }}</view>
            <view class="card-line">时段：{{ item.timeSlot }}</view>
            <view class="card-line">参与人数：{{ item.attendeeCount || 0 }}人</view>
            <view class="card-line">事由：{{ item.purpose || '-' }}</view>
            <input
              class="reject-input"
              type="text"
              v-model="item.rejectReasonDraft"
              placeholder="驳回原因（不填默认：管理员驳回）"
              placeholder-class="placeholder"
            />
            <view class="card-actions">
              <button
                class="mini-btn reject"
                :disabled="reviewingId === item.id"
                @click="handleReview(item, false)"
              >
                驳回
              </button>
              <button
                class="mini-btn approve"
                :disabled="reviewingId === item.id"
                @click="handleReview(item, true)"
              >
                通过
              </button>
            </view>
          </view>
        </template>

        <view class="empty-state" v-if="!reviewLoading && pendingReservations.length === 0">
          <text>暂无待审核预约</text>
        </view>
      </view>

      <view class="panel" v-if="activeTab === 'room'">
        <view class="room-toolbar">
          <button class="action-btn" @click="openCreateRoom">新增会议室</button>
        </view>

        <view class="loading-state" v-if="roomLoading">
          <text>会议室数据加载中...</text>
        </view>

        <template v-else-if="roomList.length > 0">
          <view class="room-card" v-for="room in roomList" :key="room.id">
            <view class="card-header">
              <text class="card-title">{{ room.name }}</text>
              <text class="status-tag" :class="roomStatusClass(room.status)">
                {{ room.statusText }}
              </text>
            </view>
            <view class="card-line">容量：{{ room.capacity || 0 }}人</view>
            <view class="card-line">位置：{{ room.location || '-' }}</view>
            <view class="card-line">楼栋/楼层：{{ room.building || '-' }} / {{ room.floor || '-' }}</view>
            <view class="card-line">排序：{{ room.sortOrder || 0 }}</view>
            <view class="card-line">封面：{{ room.coverImage || '-' }}</view>
            <view class="card-line">描述：{{ room.description || '-' }}</view>
            <view class="equipment-row">
              <text
                class="equipment-tag"
                v-for="(name, index) in room.equipmentNames"
                :key="`${room.id}-${index}`"
              >
                {{ name }}
              </text>
              <text class="text-muted" v-if="!room.equipmentNames || room.equipmentNames.length === 0">
                未配置设备
              </text>
            </view>
            <view class="card-actions">
              <button class="mini-btn" @click="openEditRoom(room)">编辑</button>
              <button
                class="mini-btn disable"
                v-if="room.status !== 0"
                @click="disableRoom(room)"
              >
                停用
              </button>
            </view>
          </view>
        </template>

        <view class="empty-state" v-if="!roomLoading && roomList.length === 0">
          <text>暂无会议室数据</text>
        </view>
      </view>

      <view class="panel" v-if="activeTab === 'stats'">
        <view class="loading-state" v-if="statsLoading">
          <text>统计数据加载中...</text>
        </view>
        <view class="stats-grid" v-else>
          <view class="stat-card">
            <text class="stat-value">{{ stats.totalUsers }}</text>
            <text class="stat-label">用户总数</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.totalRooms }}</text>
            <text class="stat-label">会议室总数</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.normalRooms }}</text>
            <text class="stat-label">正常会议室</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.maintenanceRooms }}</text>
            <text class="stat-label">维护中会议室</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.disabledRooms }}</text>
            <text class="stat-label">停用会议室</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.totalReservations }}</text>
            <text class="stat-label">预约总数</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.pendingReservations }}</text>
            <text class="stat-label">待审核预约</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.approvedReservations }}</text>
            <text class="stat-label">已通过预约</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.rejectedReservations }}</text>
            <text class="stat-label">已拒绝预约</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ stats.cancelledReservations }}</text>
            <text class="stat-label">已取消预约</text>
          </view>
          <view class="stat-card full">
            <text class="stat-value">{{ stats.todayReservations }}</text>
            <text class="stat-label">今日预约数</text>
          </view>
        </view>
      </view>

      <view class="panel" v-if="activeTab === 'emergency'">
        <view class="form-item">
          <text class="label">会议室</text>
          <picker
            :range="emergencyRoomOptions"
            range-key="label"
            :value="emergencyRoomIndex"
            @change="handleEmergencyRoomChange"
          >
            <view class="picker-value">{{ emergencyRoomLabel || '请选择会议室' }}</view>
          </picker>
        </view>

        <view class="form-item">
          <text class="label">占用日期</text>
          <picker
            mode="date"
            :start="minDate"
            :value="emergencyForm.reservationDate"
            @change="handleEmergencyDateChange"
          >
            <view class="picker-value">{{ emergencyForm.reservationDate || '请选择日期' }}</view>
          </picker>
        </view>

        <view class="form-item">
          <text class="label">开始时间</text>
          <picker mode="time" :value="emergencyForm.startTime" @change="handleEmergencyStartTimeChange">
            <view class="picker-value">{{ emergencyForm.startTime || '请选择开始时间' }}</view>
          </picker>
        </view>

        <view class="form-item">
          <text class="label">结束时间</text>
          <picker mode="time" :value="emergencyForm.endTime" @change="handleEmergencyEndTimeChange">
            <view class="picker-value">{{ emergencyForm.endTime || '请选择结束时间' }}</view>
          </picker>
        </view>

        <view class="form-item column">
          <text class="label">占用主题</text>
          <input
            class="input"
            type="text"
            v-model="emergencyForm.title"
            placeholder="请输入占用主题"
            placeholder-class="placeholder"
          />
        </view>

        <view class="form-item column">
          <text class="label">占用说明</text>
          <textarea
            class="textarea"
            v-model="emergencyForm.purpose"
            placeholder="请输入占用说明"
            placeholder-class="placeholder"
          />
        </view>

        <view class="form-item">
          <text class="label">强制协调冲突</text>
          <switch
            :checked="emergencyForm.forceOverride"
            color="#007aff"
            @change="handleForceOverrideChange"
          />
        </view>

        <view class="form-item column" v-if="emergencyForm.forceOverride">
          <text class="label">冲突取消原因</text>
          <input
            class="input"
            type="text"
            v-model="emergencyForm.cancelReason"
            placeholder="请输入冲突取消原因"
            placeholder-class="placeholder"
          />
        </view>

        <button
          class="action-btn"
          :disabled="emergencySubmitting"
          @click="submitEmergencyOccupy"
        >
          {{ emergencySubmitting ? '提交中...' : '提交紧急占用' }}
        </button>
      </view>
    </scroll-view>

    <view class="modal-mask" v-if="showRoomModal" @click="closeRoomModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text>{{ roomForm.id ? '编辑会议室' : '新增会议室' }}</text>
        </view>

        <scroll-view class="modal-body" scroll-y>
          <view class="form-item column">
            <text class="label">名称</text>
            <input
              class="input"
              type="text"
              v-model="roomForm.name"
              placeholder="请输入会议室名称"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">容量</text>
            <input
              class="input"
              type="number"
              v-model="roomForm.capacity"
              placeholder="请输入容量"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">位置</text>
            <input
              class="input"
              type="text"
              v-model="roomForm.location"
              placeholder="例如：A栋3楼301室"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">楼栋</text>
            <input
              class="input"
              type="text"
              v-model="roomForm.building"
              placeholder="例如：A"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">楼层</text>
            <input
              class="input"
              type="text"
              v-model="roomForm.floor"
              placeholder="例如：3"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">封面图URL</text>
            <input
              class="input"
              type="text"
              v-model="roomForm.coverImage"
              placeholder="请输入图片URL"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">状态</text>
            <picker
              :range="roomStatusLabels"
              :value="roomStatusIndex"
              @change="handleRoomStatusChange"
            >
              <view class="picker-value">{{ roomStatusText(roomForm.status) }}</view>
            </picker>
          </view>

          <view class="form-item column">
            <text class="label">排序权重</text>
            <input
              class="input"
              type="number"
              v-model="roomForm.sortOrder"
              placeholder="默认0，越大越靠前"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">描述</text>
            <textarea
              class="textarea"
              v-model="roomForm.description"
              placeholder="请输入会议室描述"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">设备配置</text>
            <view class="equipment-selector">
              <view
                class="equipment-select-item"
                :class="{ selected: roomForm.equipmentIds.includes(item.id) }"
                v-for="item in equipmentOptions"
                :key="item.id"
                @click="toggleRoomEquipment(item.id)"
              >
                {{ item.name }}
              </view>
            </view>
          </view>
        </scroll-view>

        <view class="modal-footer">
          <button class="modal-btn cancel" @click="closeRoomModal">取消</button>
          <button class="modal-btn confirm" :disabled="roomSaving" @click="submitRoomForm">
            {{ roomSaving ? '保存中...' : '保存' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '../../utils/request'

const ROOM_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 2, label: '维护中' },
  { value: 0, label: '停用' }
]

/**
 * 创建默认统计对象。
 * @returns {Object}
 */
function createDefaultStats() {
  return {
    totalUsers: 0,
    totalRooms: 0,
    normalRooms: 0,
    maintenanceRooms: 0,
    disabledRooms: 0,
    totalReservations: 0,
    pendingReservations: 0,
    approvedReservations: 0,
    rejectedReservations: 0,
    cancelledReservations: 0,
    todayReservations: 0
  }
}

/**
 * 创建默认会议室表单。
 * @returns {Object}
 */
function createDefaultRoomForm() {
  return {
    id: null,
    name: '',
    capacity: '',
    location: '',
    building: '',
    floor: '',
    description: '',
    coverImage: '',
    status: 1,
    sortOrder: 0,
    equipmentIds: []
  }
}

/**
 * 创建默认紧急占用表单。
 * @returns {Object}
 */
function createDefaultEmergencyForm() {
  return {
    roomId: '',
    reservationDate: '',
    startTime: '09:00',
    endTime: '10:00',
    title: '紧急占用',
    purpose: '',
    forceOverride: false,
    cancelReason: '因管理员紧急占用，原预约已协调取消'
  }
}

/**
 * 管理员页面
 * @description 审核预约、管理会议室、查看统计并处理紧急占用
 */
export default {
  data() {
    return {
      // 当前标签
      activeTab: 'review',
      // 管理员ID
      adminUserId: null,
      // 审核模块
      reviewLoading: false,
      reviewingId: null,
      pendingReservations: [],
      // 会议室模块
      roomLoading: false,
      roomList: [],
      equipmentOptions: [],
      showRoomModal: false,
      roomSaving: false,
      roomForm: createDefaultRoomForm(),
      // 统计模块
      statsLoading: false,
      stats: createDefaultStats(),
      // 紧急占用模块
      emergencySubmitting: false,
      emergencyForm: createDefaultEmergencyForm(),
      minDate: ''
    }
  },

  /**
   * 页面加载。
   */
  async onLoad() {
    if (!this.ensureAdminLogin()) {
      return
    }

    this.minDate = this.formatDate(new Date())
    this.emergencyForm.reservationDate = this.minDate
    await this.loadAllData()
  },

  computed: {
    /**
     * 会议室状态文案列表（用于picker）。
     * @returns {Array}
     */
    roomStatusLabels() {
      return ROOM_STATUS_OPTIONS.map(item => item.label)
    },

    /**
     * 当前会议室状态在picker中的索引。
     * @returns {Number}
     */
    roomStatusIndex() {
      const index = ROOM_STATUS_OPTIONS.findIndex(item => item.value === Number(this.roomForm.status))
      return index >= 0 ? index : 0
    },

    /**
     * 紧急占用会议室选项。
     * @returns {Array}
     */
    emergencyRoomOptions() {
      return this.roomList
        .filter(item => Number(item.status) !== 0)
        .map(item => ({
          label: `${item.name}（${item.location || '未知位置'}）`,
          value: item.id
        }))
    },

    /**
     * 紧急占用会议室picker索引。
     * @returns {Number}
     */
    emergencyRoomIndex() {
      const index = this.emergencyRoomOptions.findIndex(item => item.value === this.emergencyForm.roomId)
      return index >= 0 ? index : 0
    },

    /**
     * 紧急占用会议室标签。
     * @returns {String}
     */
    emergencyRoomLabel() {
      const room = this.emergencyRoomOptions.find(item => item.value === this.emergencyForm.roomId)
      return room ? room.label : ''
    }
  },

  methods: {
    /**
     * 跳转到日历视图。
     */
    goToCalendar() {
      uni.navigateTo({ url: '/pages/calendar/index' })
    },

    /**
     * 校验管理员登录态。
     * @returns {Boolean}
     */
    ensureAdminLogin() {
      const userInfo = uni.getStorageSync('userInfo') || {}
      const userId = Number(userInfo.id)
      const role = Number(userInfo.role)

      if (!Number.isInteger(userId) || userId <= 0 || role !== 1) {
        uni.showToast({ title: '请使用管理员账号登录', icon: 'none' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/login/index' })
        }, 800)
        return false
      }

      this.adminUserId = userId
      return true
    },

    /**
     * 加载管理员页面全部数据。
     */
    async loadAllData() {
      await Promise.all([
        this.loadPendingReservations(),
        this.loadMeetingRooms(),
        this.loadEquipmentOptions(),
        this.loadStats()
      ])
    },

    /**
     * 切换标签并刷新对应数据。
     * @param {String} tab 标签名
     */
    async switchTab(tab) {
      this.activeTab = tab
      await this.refreshCurrentTab()
    },

    /**
     * 刷新当前标签数据。
     */
    async refreshCurrentTab() {
      if (this.activeTab === 'review') {
        await this.loadPendingReservations()
        await this.loadStats()
        return
      }
      if (this.activeTab === 'room') {
        await Promise.all([this.loadMeetingRooms(), this.loadEquipmentOptions(), this.loadStats()])
        return
      }
      if (this.activeTab === 'stats') {
        await this.loadStats()
        return
      }
      if (this.activeTab === 'emergency') {
        await Promise.all([this.loadMeetingRooms(), this.loadStats()])
      }
    },

    /**
     * 加载待审核预约。
     */
    async loadPendingReservations() {
      this.reviewLoading = true
      try {
        const list = await request({
          url: `/api/admin/reservations/pending?adminUserId=${this.adminUserId}`,
          method: 'GET'
        })

        const finalList = Array.isArray(list) ? list : []
        // 为每条预约补充前端驳回原因草稿字段，避免污染后端真实数据结构。
        this.pendingReservations = finalList.map(item => ({
          ...item,
          rejectReasonDraft: ''
        }))
      } catch (error) {
        uni.showToast({ title: error.message || '加载待审核预约失败', icon: 'none' })
      } finally {
        this.reviewLoading = false
      }
    },

    /**
     * 审核预约。
     * @param {Object} item 预约项
     * @param {Boolean} approved 是否通过
     */
    handleReview(item, approved) {
      const actionText = approved ? '通过' : '驳回'
      const rejectReason = approved ? '' : ((item.rejectReasonDraft || '').trim() || '管理员驳回')

      uni.showModal({
        title: '确认操作',
        content: `确定${actionText}该预约申请吗？`,
        success: async (res) => {
          if (!res.confirm) {
            return
          }

          if (this.reviewingId) {
            return
          }

          this.reviewingId = item.id
          try {
            await request({
              url: `/api/admin/reservations/${item.id}/review`,
              method: 'POST',
              data: {
                adminUserId: this.adminUserId,
                approved,
                rejectReason
              }
            })
            uni.showToast({ title: '审核完成', icon: 'success' })
            await Promise.all([this.loadPendingReservations(), this.loadStats()])
          } catch (error) {
            uni.showToast({ title: error.message || '审核失败', icon: 'none' })
          } finally {
            this.reviewingId = null
          }
        }
      })
    },

    /**
     * 加载会议室列表。
     */
    async loadMeetingRooms() {
      this.roomLoading = true
      try {
        const list = await request({
          url: `/api/admin/meeting-rooms?adminUserId=${this.adminUserId}`,
          method: 'GET'
        })
        this.roomList = Array.isArray(list) ? list : []

        const roomOptionExists = this.emergencyRoomOptions.some(
          item => item.value === this.emergencyForm.roomId
        )
        if (!roomOptionExists) {
          this.emergencyForm.roomId = this.emergencyRoomOptions.length > 0
            ? this.emergencyRoomOptions[0].value
            : ''
        }
      } catch (error) {
        uni.showToast({ title: error.message || '加载会议室失败', icon: 'none' })
      } finally {
        this.roomLoading = false
      }
    },

    /**
     * 加载设备选项。
     */
    async loadEquipmentOptions() {
      try {
        const list = await request({
          url: `/api/admin/equipments?adminUserId=${this.adminUserId}`,
          method: 'GET'
        })
        this.equipmentOptions = Array.isArray(list) ? list : []
      } catch (error) {
        uni.showToast({ title: error.message || '加载设备选项失败', icon: 'none' })
      }
    },

    /**
     * 加载统计数据。
     */
    async loadStats() {
      this.statsLoading = true
      try {
        const stats = await request({
          url: `/api/admin/stats?adminUserId=${this.adminUserId}`,
          method: 'GET'
        })
        this.stats = { ...createDefaultStats(), ...(stats || {}) }
      } catch (error) {
        uni.showToast({ title: error.message || '加载统计数据失败', icon: 'none' })
      } finally {
        this.statsLoading = false
      }
    },

    /**
     * 打开新增会议室弹窗。
     */
    openCreateRoom() {
      this.roomForm = createDefaultRoomForm()
      this.showRoomModal = true
    },

    /**
     * 打开编辑会议室弹窗。
     * @param {Object} room 会议室数据
     */
    openEditRoom(room) {
      this.roomForm = {
        id: room.id,
        name: room.name || '',
        capacity: `${room.capacity || ''}`,
        location: room.location || '',
        building: room.building || '',
        floor: room.floor || '',
        description: room.description || '',
        coverImage: room.coverImage || '',
        status: Number(room.status),
        sortOrder: room.sortOrder || 0,
        equipmentIds: Array.isArray(room.equipmentIds) ? [...room.equipmentIds] : []
      }
      this.showRoomModal = true
    },

    /**
     * 关闭会议室弹窗。
     */
    closeRoomModal() {
      if (this.roomSaving) {
        return
      }
      this.showRoomModal = false
    },

    /**
     * 切换会议室设备选中状态。
     * @param {Number} equipmentId 设备ID
     */
    toggleRoomEquipment(equipmentId) {
      const index = this.roomForm.equipmentIds.indexOf(equipmentId)
      if (index >= 0) {
        this.roomForm.equipmentIds.splice(index, 1)
      } else {
        this.roomForm.equipmentIds.push(equipmentId)
      }
    },

    /**
     * 修改会议室状态。
     * @param {Object} event picker事件
     */
    handleRoomStatusChange(event) {
      const index = Number(event.detail.value)
      const option = ROOM_STATUS_OPTIONS[index]
      this.roomForm.status = option ? option.value : 1
    },

    /**
     * 提交会议室表单。
     */
    async submitRoomForm() {
      const finalName = (this.roomForm.name || '').trim()
      const finalLocation = (this.roomForm.location || '').trim()
      const finalCapacity = Number(this.roomForm.capacity)
      const finalSortOrder = Number(this.roomForm.sortOrder || 0)

      if (!finalName) {
        uni.showToast({ title: '请输入会议室名称', icon: 'none' })
        return
      }
      if (!Number.isInteger(finalCapacity) || finalCapacity <= 0) {
        uni.showToast({ title: '容量需为正整数', icon: 'none' })
        return
      }
      if (!finalLocation) {
        uni.showToast({ title: '请输入会议室位置', icon: 'none' })
        return
      }
      if (this.roomSaving) {
        return
      }

      const payload = {
        adminUserId: this.adminUserId,
        name: finalName,
        capacity: finalCapacity,
        location: finalLocation,
        building: (this.roomForm.building || '').trim(),
        floor: (this.roomForm.floor || '').trim(),
        description: (this.roomForm.description || '').trim(),
        coverImage: (this.roomForm.coverImage || '').trim(),
        status: Number(this.roomForm.status),
        sortOrder: Number.isNaN(finalSortOrder) ? 0 : finalSortOrder,
        equipmentIds: this.roomForm.equipmentIds
      }

      this.roomSaving = true
      try {
        if (this.roomForm.id) {
          await request({
            url: `/api/admin/meeting-rooms/${this.roomForm.id}`,
            method: 'PUT',
            data: payload
          })
          uni.showToast({ title: '编辑成功', icon: 'success' })
        } else {
          await request({
            url: '/api/admin/meeting-rooms',
            method: 'POST',
            data: payload
          })
          uni.showToast({ title: '新增成功', icon: 'success' })
        }

        this.showRoomModal = false
        await Promise.all([this.loadMeetingRooms(), this.loadStats()])
      } catch (error) {
        uni.showToast({ title: error.message || '保存失败', icon: 'none' })
      } finally {
        this.roomSaving = false
      }
    },

    /**
     * 停用会议室。
     * @param {Object} room 会议室数据
     */
    disableRoom(room) {
      uni.showModal({
        title: '确认停用',
        content: `确定停用会议室【${room.name}】吗？`,
        success: async (res) => {
          if (!res.confirm) {
            return
          }

          try {
            await request({
              url: `/api/admin/meeting-rooms/${room.id}/disable`,
              method: 'POST',
              data: {
                adminUserId: this.adminUserId
              }
            })
            uni.showToast({ title: '停用成功', icon: 'success' })
            await Promise.all([this.loadMeetingRooms(), this.loadStats()])
          } catch (error) {
            uni.showToast({ title: error.message || '停用失败', icon: 'none' })
          }
        }
      })
    },

    /**
     * 紧急占用会议室选择变化。
     * @param {Object} event picker事件
     */
    handleEmergencyRoomChange(event) {
      const index = Number(event.detail.value)
      const option = this.emergencyRoomOptions[index]
      if (option) {
        this.emergencyForm.roomId = option.value
      }
    },

    /**
     * 紧急占用日期变化。
     * @param {Object} event picker事件
     */
    handleEmergencyDateChange(event) {
      this.emergencyForm.reservationDate = event.detail.value
    },

    /**
     * 紧急占用开始时间变化。
     * @param {Object} event picker事件
     */
    handleEmergencyStartTimeChange(event) {
      this.emergencyForm.startTime = event.detail.value
    },

    /**
     * 紧急占用结束时间变化。
     * @param {Object} event picker事件
     */
    handleEmergencyEndTimeChange(event) {
      this.emergencyForm.endTime = event.detail.value
    },

    /**
     * 切换是否强制协调冲突。
     * @param {Object} event switch事件
     */
    handleForceOverrideChange(event) {
      this.emergencyForm.forceOverride = !!event.detail.value
    },

    /**
     * 提交紧急占用。
     */
    async submitEmergencyOccupy() {
      if (!this.emergencyForm.roomId) {
        uni.showToast({ title: '请选择会议室', icon: 'none' })
        return
      }
      if (!this.emergencyForm.reservationDate) {
        uni.showToast({ title: '请选择占用日期', icon: 'none' })
        return
      }
      if (!this.emergencyForm.startTime || !this.emergencyForm.endTime) {
        uni.showToast({ title: '请选择占用时段', icon: 'none' })
        return
      }
      if (this.emergencyForm.startTime >= this.emergencyForm.endTime) {
        uni.showToast({ title: '开始时间必须早于结束时间', icon: 'none' })
        return
      }
      if (!(this.emergencyForm.title || '').trim()) {
        uni.showToast({ title: '请输入占用主题', icon: 'none' })
        return
      }
      if (!(this.emergencyForm.purpose || '').trim()) {
        uni.showToast({ title: '请输入占用说明', icon: 'none' })
        return
      }
      if (this.emergencySubmitting) {
        return
      }

      this.emergencySubmitting = true
      try {
        const result = await request({
          url: '/api/admin/reservations/emergency-occupy',
          method: 'POST',
          data: {
            adminUserId: this.adminUserId,
            roomId: this.emergencyForm.roomId,
            reservationDate: this.emergencyForm.reservationDate,
            startTime: `${this.emergencyForm.startTime}:00`,
            endTime: `${this.emergencyForm.endTime}:00`,
            title: (this.emergencyForm.title || '').trim(),
            purpose: (this.emergencyForm.purpose || '').trim(),
            forceOverride: this.emergencyForm.forceOverride,
            cancelReason: (this.emergencyForm.cancelReason || '').trim()
          }
        })

        const lines = [
          `预约编号：${result.reservationNo || '-'}`,
          `检测冲突：${result.conflictCount || 0}条`,
          `协调取消：${result.cancelledCount || 0}条`,
          `结果：${result.statusText || '紧急占用成功'}`
        ]
        uni.showModal({
          title: '提交成功',
          content: lines.join('\n'),
          showCancel: false
        })
        await Promise.all([this.loadPendingReservations(), this.loadStats()])
      } catch (error) {
        const message = error.message || '提交失败'
        // 未开启强制协调时若存在冲突，引导管理员一键切换为强制模式。
        if (!this.emergencyForm.forceOverride && message.includes('冲突预约')) {
          uni.showModal({
            title: '检测到冲突',
            content: `${message}\n是否开启强制协调并重新提交？`,
            success: (res) => {
              if (res.confirm) {
                this.emergencyForm.forceOverride = true
              }
            }
          })
        } else {
          uni.showToast({ title: message, icon: 'none' })
        }
      } finally {
        this.emergencySubmitting = false
      }
    },

    /**
     * 会议室状态对应样式类。
     * @param {Number} status 状态码
     * @returns {String}
     */
    roomStatusClass(status) {
      if (Number(status) === 1) {
        return 'normal'
      }
      if (Number(status) === 2) {
        return 'maintenance'
      }
      return 'disabled'
    },

    /**
     * 会议室状态文案转换。
     * @param {Number} status 状态码
     * @returns {String}
     */
    roomStatusText(status) {
      const option = ROOM_STATUS_OPTIONS.find(item => item.value === Number(status))
      return option ? option.label : '未知状态'
    },

    /**
     * 格式化日期。
     * @param {Date} date 日期对象
     * @returns {String}
     */
    formatDate(date) {
      const year = date.getFullYear()
      const month = `${date.getMonth() + 1}`.padStart(2, '0')
      const day = `${date.getDate()}`.padStart(2, '0')
      return `${year}-${month}-${day}`
    },

    /**
     * 退出登录。
     */
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定退出管理员登录吗？',
        success: (res) => {
          if (!res.confirm) {
            return
          }
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.removeStorageSync('loginType')
          uni.reLaunch({ url: '/pages/login/index' })
        }
      })
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

.header {
  background-color: #007aff;
  padding: 24rpx 24rpx 18rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 34rpx;
  color: #fff;
  font-weight: bold;
}

.header-actions {
  display: flex;
  align-items: center;
}

.header-action {
  font-size: 26rpx;
  color: #e9f1ff;
  margin-left: 24rpx;
}

.header-action.danger {
  color: #ffd8d8;
}

.tabs {
  display: flex;
  background-color: #fff;
  border-bottom: 1rpx solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  height: 86rpx;
  line-height: 86rpx;
  font-size: 26rpx;
  color: #666;
}

.tab-item.active {
  color: #007aff;
  font-weight: bold;
  border-bottom: 4rpx solid #007aff;
}

.content {
  flex: 1;
  padding: 20rpx;
  box-sizing: border-box;
}

.panel {
  padding-bottom: 30rpx;
}

.room-toolbar {
  margin-bottom: 20rpx;
}

.action-btn {
  height: 80rpx;
  line-height: 80rpx;
  background-color: #007aff;
  color: #fff;
  font-size: 30rpx;
  border-radius: 12rpx;
}

.action-btn::after {
  border: none;
}

.action-btn[disabled] {
  opacity: 0.7;
}

.review-card,
.room-card {
  background-color: #fff;
  border-radius: 14rpx;
  padding: 22rpx;
  margin-bottom: 16rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  max-width: 520rpx;
}

.card-line {
  font-size: 25rpx;
  color: #666;
  margin-top: 8rpx;
  word-break: break-all;
}

.status-tag {
  font-size: 22rpx;
  border-radius: 16rpx;
  padding: 4rpx 14rpx;
}

.status-tag.pending {
  background-color: #fff3e0;
  color: #ff9800;
}

.status-tag.normal {
  background-color: #e8f5e9;
  color: #43a047;
}

.status-tag.maintenance {
  background-color: #fff8e1;
  color: #f59f00;
}

.status-tag.disabled {
  background-color: #efefef;
  color: #999;
}

.card-actions {
  margin-top: 18rpx;
  display: flex;
  justify-content: flex-end;
}

.mini-btn {
  min-width: 144rpx;
  height: 62rpx;
  line-height: 62rpx;
  font-size: 24rpx;
  border-radius: 10rpx;
  margin-left: 12rpx;
  background-color: #f3f4f6;
  color: #333;
}

.mini-btn::after {
  border: none;
}

.mini-btn.approve {
  background-color: #007aff;
  color: #fff;
}

.mini-btn.reject,
.mini-btn.disable {
  background-color: #ef5350;
  color: #fff;
}

.mini-btn[disabled] {
  opacity: 0.7;
}

.reject-input,
.input {
  width: 100%;
  height: 72rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 10rpx;
  padding: 0 20rpx;
  box-sizing: border-box;
  background-color: #fff;
  font-size: 26rpx;
  color: #333;
}

.placeholder {
  color: #b0b0b0;
}

.textarea {
  width: 100%;
  min-height: 120rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 10rpx;
  padding: 16rpx 20rpx;
  box-sizing: border-box;
  background-color: #fff;
  font-size: 26rpx;
  color: #333;
}

.equipment-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 12rpx;
}

.equipment-tag {
  font-size: 22rpx;
  color: #007aff;
  background-color: #e6f0ff;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
  margin-right: 12rpx;
  margin-top: 8rpx;
}

.text-muted {
  color: #999;
  font-size: 24rpx;
}

.stats-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.stat-card {
  width: calc(50% - 10rpx);
  background-color: #fff;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  padding: 24rpx 0;
  text-align: center;
}

.stat-card.full {
  width: 100%;
}

.stat-value {
  display: block;
  font-size: 40rpx;
  color: #007aff;
  font-weight: bold;
  margin-bottom: 6rpx;
}

.stat-label {
  display: block;
  font-size: 24rpx;
  color: #666;
}

.form-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  background-color: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
}

.form-item.column {
  display: block;
}

.label {
  font-size: 26rpx;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.picker-value {
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

.loading-state,
.empty-state {
  text-align: center;
  color: #999;
  font-size: 28rpx;
  padding: 80rpx 0;
}

.modal-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-content {
  width: 680rpx;
  height: 86vh;
  max-height: 86vh;
  background-color: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 26rpx;
  font-size: 30rpx;
  font-weight: bold;
  text-align: center;
  border-bottom: 1rpx solid #f0f0f0;
}

.modal-body {
  flex: 1;
  min-height: 0;
  height: 0;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
  padding: 18rpx;
  box-sizing: border-box;
}

.modal-btn {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 10rpx;
  font-size: 28rpx;
}

.modal-btn::after {
  border: none;
}

.modal-btn.cancel {
  background-color: #f0f2f5;
  color: #333;
  margin-right: 12rpx;
}

.modal-btn.confirm {
  background-color: #007aff;
  color: #fff;
}

.modal-btn[disabled] {
  opacity: 0.7;
}

.equipment-selector {
  display: flex;
  flex-wrap: wrap;
}

.equipment-select-item {
  padding: 8rpx 18rpx;
  border-radius: 8rpx;
  border: 1rpx solid #d9d9d9;
  font-size: 24rpx;
  color: #666;
  margin-right: 12rpx;
  margin-bottom: 12rpx;
}

.equipment-select-item.selected {
  border-color: #007aff;
  color: #007aff;
  background-color: #e6f0ff;
}
</style>
