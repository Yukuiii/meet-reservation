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
        :class="{ active: activeTab === 'equipment' }"
        @click="switchTab('equipment')"
      >
        设备管理
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'repair' }"
        @click="switchTab('repair')"
      >
        设备报修
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'admin-user' }"
        @click="switchTab('admin-user')"
      >
        管理员
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
        <view class="review-toolbar" v-if="!reviewLoading && pendingReservations.length > 0">
          <view class="review-toolbar-info">
            <text class="review-toolbar-title">已选 {{ selectedReservationCount }} / {{ pendingReservations.length }}</text>
            <text class="review-toolbar-tip">自动审核会通过可预约申请，并驳回冲突或不可预约申请</text>
          </view>
          <view class="review-toolbar-actions">
            <button class="toolbar-btn" :disabled="reviewActionBusy" @click="toggleAllReviewSelection">
              {{ allReviewReservationsSelected ? '清空选择' : '全选' }}
            </button>
            <button class="toolbar-btn approve" :disabled="reviewActionBusy" @click="handleBatchReview(true)">
              批量通过
            </button>
            <button class="toolbar-btn reject" :disabled="reviewActionBusy" @click="handleBatchReview(false)">
              批量驳回
            </button>
            <button class="toolbar-btn auto" :disabled="reviewActionBusy" @click="handleAutoReview">
              自动审核
            </button>
          </view>
          <input
            class="batch-reject-input"
            type="text"
            v-model="batchRejectReason"
            placeholder="批量驳回原因"
            placeholder-class="placeholder"
          />
        </view>

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
              <view class="review-title-row">
                <view
                  class="select-box"
                  :class="{ checked: isReservationSelected(item.id) }"
                  @click.stop="toggleReservationSelection(item)"
                >
                  <text v-if="isReservationSelected(item.id)">✓</text>
                </view>
                <text class="card-title">{{ item.title || '未命名会议' }}</text>
              </view>
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
              v-if="item.canReview"
              class="reject-input"
              type="text"
              v-model="item.rejectReasonDraft"
              placeholder="驳回原因（不填默认：管理员驳回）"
              placeholder-class="placeholder"
            />
            <view class="card-actions" v-if="item.canReview">
              <button
                class="mini-btn reject"
                :disabled="reviewingId === item.id || reviewActionBusy"
                @click="handleReview(item, false)"
              >
                驳回
              </button>
              <button
                class="mini-btn approve"
                :disabled="reviewingId === item.id || reviewActionBusy"
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
            <image
              class="room-cover-preview"
              v-if="room.coverImage"
              :src="displayCoverImage(room.coverImage)"
              mode="aspectFill"
            />
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

      <view class="panel" v-if="activeTab === 'equipment'">
        <view class="room-toolbar">
          <button class="action-btn" @click="openCreateEquipment">新增设备</button>
        </view>

        <view class="loading-state" v-if="equipmentLoading">
          <text>设备数据加载中...</text>
        </view>

        <template v-else-if="equipmentList.length > 0">
          <view class="room-card" v-for="item in equipmentList" :key="item.id">
            <view class="card-header">
              <text class="card-title">{{ item.name }}</text>
              <text class="status-tag" :class="equipmentStatusClass(item.status)">
                {{ item.statusText }}
              </text>
            </view>
            <view class="card-line">图标：{{ item.icon || '-' }}</view>
            <view class="card-line">描述：{{ item.description || '-' }}</view>
            <view class="card-actions">
              <button class="mini-btn" @click="openEditEquipment(item)">编辑</button>
              <button
                class="mini-btn disable"
                v-if="Number(item.status) !== 0"
                @click="disableEquipment(item)"
              >
                停用
              </button>
            </view>
          </view>
        </template>

        <view class="empty-state" v-if="!equipmentLoading && equipmentList.length === 0">
          <text>暂无设备数据</text>
        </view>
      </view>

      <view class="panel" v-if="activeTab === 'repair'">
        <view class="loading-state" v-if="repairLoading">
          <text>报修记录加载中...</text>
        </view>

        <template v-else-if="repairList.length > 0">
          <view class="room-card" v-for="item in repairList" :key="item.id">
            <view class="card-header">
              <text class="card-title">{{ item.equipmentName }}</text>
              <text class="status-tag" :class="repairStatusClass(item.status)">
                {{ item.statusText }}
              </text>
            </view>
            <view class="card-line">报修编号：{{ item.repairNo || '-' }}</view>
            <view class="card-line">会议室：{{ item.roomName || '-' }}</view>
            <view class="card-line">预约编号：{{ item.reservationNo || '-' }}</view>
            <view class="card-line">报修用户：{{ item.nickname || item.username || '-' }}</view>
            <view class="card-line">报修时间：{{ item.createdAt || '-' }}</view>
            <view class="card-line">故障描述：{{ item.description || '-' }}</view>
            <view class="card-line" v-if="item.fixedAt">修复时间：{{ item.fixedAt }}</view>
            <view class="card-line" v-if="item.fixRemark">修复备注：{{ item.fixRemark }}</view>
            <view class="card-actions" v-if="Number(item.status) === 0">
              <button
                class="mini-btn approve"
                :disabled="resolvingRepairId === item.id"
                @click="resolveRepair(item)"
              >
                {{ resolvingRepairId === item.id ? '处理中...' : '标记已修复' }}
              </button>
            </view>
          </view>
        </template>

        <view class="empty-state" v-if="!repairLoading && repairList.length === 0">
          <text>暂无报修记录</text>
        </view>
      </view>

      <view class="panel" v-if="activeTab === 'admin-user'">
        <view class="room-toolbar">
          <button class="action-btn" @click="openCreateAdmin">新增管理员</button>
        </view>

        <view class="loading-state" v-if="adminLoading">
          <text>管理员数据加载中...</text>
        </view>

        <template v-else-if="adminList.length > 0">
          <view class="room-card" v-for="item in adminList" :key="item.id">
            <view class="card-header">
              <text class="card-title">{{ item.nickname || item.username }}</text>
              <text class="status-tag" :class="adminStatusClass(item.status)">
                {{ item.statusText }}
              </text>
            </view>
            <view class="card-line">用户名：{{ item.username || '-' }}</view>
            <view class="card-line">昵称：{{ item.nickname || '-' }}</view>
            <view class="card-line">手机号：{{ item.phone || '-' }}</view>
            <view class="card-line">邮箱：{{ item.email || '-' }}</view>
            <view class="card-line">创建时间：{{ item.createdAt || '-' }}</view>
            <view class="card-actions">
              <button class="mini-btn" @click="openEditAdmin(item)">编辑</button>
              <button
                class="mini-btn disable"
                v-if="Number(item.id) !== Number(adminUserId)"
                @click="deleteAdmin(item)"
              >
                删除
              </button>
            </view>
          </view>
        </template>

        <view class="empty-state" v-if="!adminLoading && adminList.length === 0">
          <text>暂无管理员账号</text>
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
        <text class="form-tip">
          开启后将取消冲突的待审核/已通过预约，并自动通知受影响用户。
        </text>

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
            <text class="label">封面图</text>
            <view class="upload-action-row">
              <button
                class="upload-btn primary"
                :disabled="roomCoverUploading"
                @click="chooseRoomCoverImage"
              >
                {{ roomCoverUploading ? '上传中...' : '上传本地图片' }}
              </button>
              <button
                class="upload-btn"
                :disabled="roomCoverUploading || !roomForm.coverImage"
                @click="clearRoomCoverImage"
              >
                清空封面
              </button>
            </view>
            <input
              class="input"
              type="text"
              :value="roomForm.coverImage"
              placeholder="请先上传封面图"
              placeholder-class="placeholder"
              disabled
            />
            <image
              class="room-cover-preview form"
              v-if="roomForm.coverImage"
              :src="displayCoverImage(roomForm.coverImage)"
              mode="aspectFill"
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

    <view class="modal-mask" v-if="showEquipmentModal" @click="closeEquipmentModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text>{{ equipmentForm.id ? '编辑设备' : '新增设备' }}</text>
        </view>

        <scroll-view class="modal-body" scroll-y>
          <view class="form-item column">
            <text class="label">设备名称</text>
            <input
              class="input"
              type="text"
              v-model="equipmentForm.name"
              placeholder="请输入设备名称"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">图标URL</text>
            <input
              class="input"
              type="text"
              v-model="equipmentForm.icon"
              placeholder="请输入图标URL（选填）"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">状态</text>
            <picker
              :range="equipmentStatusLabels"
              :value="equipmentStatusIndex"
              @change="handleEquipmentStatusChange"
            >
              <view class="picker-value">{{ equipmentStatusText(equipmentForm.status) }}</view>
            </picker>
          </view>

          <view class="form-item column">
            <text class="label">设备描述</text>
            <textarea
              class="textarea"
              v-model="equipmentForm.description"
              placeholder="请输入设备描述（选填）"
              placeholder-class="placeholder"
            />
          </view>
        </scroll-view>

        <view class="modal-footer">
          <button class="modal-btn cancel" @click="closeEquipmentModal">取消</button>
          <button class="modal-btn confirm" :disabled="equipmentSaving" @click="submitEquipmentForm">
            {{ equipmentSaving ? '保存中...' : '保存' }}
          </button>
        </view>
      </view>
    </view>

    <view class="modal-mask" v-if="showAdminModal" @click="closeAdminModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text>{{ adminForm.id ? '编辑管理员' : '新增管理员' }}</text>
        </view>

        <scroll-view class="modal-body" scroll-y>
          <view class="form-item column">
            <text class="label">用户名</text>
            <input
              class="input"
              type="text"
              v-model="adminForm.username"
              placeholder="请输入管理员用户名"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">昵称</text>
            <input
              class="input"
              type="text"
              v-model="adminForm.nickname"
              placeholder="不填默认使用用户名"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">手机号</text>
            <input
              class="input"
              type="number"
              v-model="adminForm.phone"
              placeholder="请输入手机号"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">邮箱</text>
            <input
              class="input"
              type="text"
              v-model="adminForm.email"
              placeholder="选填"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">密码</text>
            <input
              class="input"
              password
              type="text"
              v-model="adminForm.password"
              :placeholder="adminForm.id ? '不修改密码请留空' : '请输入登录密码'"
              placeholder-class="placeholder"
            />
          </view>

          <view class="form-item column">
            <text class="label">状态</text>
            <picker
              :range="adminStatusLabels"
              :value="adminStatusIndex"
              @change="handleAdminStatusChange"
            >
              <view class="picker-value">{{ adminStatusText(adminForm.status) }}</view>
            </picker>
          </view>
        </scroll-view>

        <view class="modal-footer">
          <button class="modal-btn cancel" @click="closeAdminModal">取消</button>
          <button class="modal-btn confirm" :disabled="adminSaving" @click="submitAdminForm">
            {{ adminSaving ? '保存中...' : '保存' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, toRefs } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { buildAssetUrl, request, uploadFile } from '../../utils/request'

const DEFAULT_ROOM_IMAGE = '/images/meeting-room-default.jpg'

const ROOM_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 2, label: '维护中' },
  { value: 0, label: '停用' }
]

const EQUIPMENT_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 0, label: '停用' }
]

const ADMIN_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 0, label: '禁用' }
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
 * 创建默认设备表单。
 * @returns {Object}
 */
function createDefaultEquipmentForm() {
  return {
    id: null,
    name: '',
    icon: '',
    description: '',
    status: 1
  }
}

/**
 * 创建默认管理员表单。
 * @returns {Object}
 */
function createDefaultAdminForm() {
  return {
    id: null,
    username: '',
    nickname: '',
    phone: '',
    email: '',
    password: '',
    status: 1
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

const page = reactive({
  // 当前标签
  activeTab: 'review',
  // 管理员ID
  adminUserId: null,
  // 审核模块
  reviewLoading: false,
  reviewingId: null,
  selectedReservationIds: [],
  batchReviewing: false,
  autoReviewing: false,
  batchRejectReason: '管理员批量驳回',
  pendingReservations: [],
  // 会议室模块
  roomLoading: false,
  roomList: [],
  equipmentOptions: [],
  showRoomModal: false,
  roomSaving: false,
  roomCoverUploading: false,
  roomForm: createDefaultRoomForm(),
  // 设备模块
  equipmentLoading: false,
  equipmentList: [],
  showEquipmentModal: false,
  equipmentSaving: false,
  equipmentForm: createDefaultEquipmentForm(),
  // 设备报修模块
  repairLoading: false,
  repairList: [],
  resolvingRepairId: null,
  // 管理员模块
  adminLoading: false,
  adminList: [],
  showAdminModal: false,
  adminSaving: false,
  adminForm: createDefaultAdminForm(),
  // 统计模块
  statsLoading: false,
  stats: createDefaultStats(),
  // 紧急占用模块
  emergencySubmitting: false,
  emergencyForm: createDefaultEmergencyForm(),
  minDate: '',

  /**
   * 页面加载。
   */
  async onLoad() {
    if (!page.ensureAdminLogin()) {
      return
    }

    page.minDate = page.formatDate(new Date())
    page.emergencyForm.reservationDate = page.minDate
    await page.loadAllData()
  },

  /**
   * 会议室状态文案列表（用于picker）。
   * @returns {Array}
   */
  get roomStatusLabels() {
      return ROOM_STATUS_OPTIONS.map(item => item.label)
  },

  /**
   * 当前会议室状态在picker中的索引。
   * @returns {Number}
   */
  get roomStatusIndex() {
      const index = ROOM_STATUS_OPTIONS.findIndex(item => item.value === Number(page.roomForm.status))
      return index >= 0 ? index : 0
  },

  /**
   * 设备状态文案列表（用于picker）。
   * @returns {Array}
   */
  get equipmentStatusLabels() {
      return EQUIPMENT_STATUS_OPTIONS.map(item => item.label)
  },

  /**
   * 当前设备状态在picker中的索引。
   * @returns {Number}
   */
  get equipmentStatusIndex() {
      const index = EQUIPMENT_STATUS_OPTIONS.findIndex(item => item.value === Number(page.equipmentForm.status))
      return index >= 0 ? index : 0
  },

  /**
   * 管理员状态文案列表（用于picker）。
   * @returns {Array}
   */
  get adminStatusLabels() {
      return ADMIN_STATUS_OPTIONS.map(item => item.label)
  },

  /**
   * 当前管理员状态在picker中的索引。
   * @returns {Number}
   */
  get adminStatusIndex() {
      const index = ADMIN_STATUS_OPTIONS.findIndex(item => item.value === Number(page.adminForm.status))
      return index >= 0 ? index : 0
  },

  /**
   * 已选择待审核预约数量。
   * @returns {Number}
   */
  get selectedReservationCount() {
      return page.selectedReservationIds.length
  },

  /**
   * 可审核预约ID列表。
   * @returns {Array}
   */
  get reviewableReservationIds() {
      return page.pendingReservations
        .filter(item => item.canReview)
        .map(item => item.id)
  },

  /**
   * 是否已选择全部可审核预约。
   * @returns {Boolean}
   */
  get allReviewReservationsSelected() {
      const reviewableIds = page.reviewableReservationIds
      return reviewableIds.length > 0
        && reviewableIds.every(id => page.selectedReservationIds.includes(id))
  },

  /**
   * 审核操作是否忙碌。
   * @returns {Boolean}
   */
  get reviewActionBusy() {
      return !!page.reviewingId || page.batchReviewing || page.autoReviewing
  },

  /**
   * 紧急占用会议室选项。
   * @returns {Array}
   */
  get emergencyRoomOptions() {
      return page.roomList
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
  get emergencyRoomIndex() {
      const index = page.emergencyRoomOptions.findIndex(item => item.value === page.emergencyForm.roomId)
      return index >= 0 ? index : 0
  },

  /**
   * 紧急占用会议室标签。
   * @returns {String}
   */
  get emergencyRoomLabel() {
      const room = page.emergencyRoomOptions.find(item => item.value === page.emergencyForm.roomId)
      return room ? room.label : ''
  },


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

      page.adminUserId = userId
      return true
    },

    /**
     * 加载管理员页面全部数据。
     */
    async loadAllData() {
      await Promise.all([
        page.loadPendingReservations(),
        page.loadMeetingRooms(),
        page.loadEquipmentList(),
        page.loadEquipmentOptions(),
        page.loadAdminList(),
        page.loadStats()
      ])
    },

    /**
     * 切换标签并刷新对应数据。
     * @param {String} tab 标签名
     */
    async switchTab(tab) {
      page.activeTab = tab
      await page.refreshCurrentTab()
    },

    /**
     * 刷新当前标签数据。
     */
    async refreshCurrentTab() {
      if (page.activeTab === 'review') {
        await page.loadPendingReservations()
        await page.loadStats()
        return
      }
      if (page.activeTab === 'room') {
        await Promise.all([page.loadMeetingRooms(), page.loadEquipmentOptions(), page.loadStats()])
        return
      }
      if (page.activeTab === 'equipment') {
        await Promise.all([page.loadEquipmentList(), page.loadEquipmentOptions(), page.loadMeetingRooms()])
        return
      }
      if (page.activeTab === 'repair') {
        await page.loadRepairList()
        return
      }
      if (page.activeTab === 'admin-user') {
        await Promise.all([page.loadAdminList(), page.loadStats()])
        return
      }
      if (page.activeTab === 'stats') {
        await page.loadStats()
        return
      }
      if (page.activeTab === 'emergency') {
        await Promise.all([page.loadMeetingRooms(), page.loadStats()])
      }
    },

    /**
     * 加载待审核预约。
     */
    async loadPendingReservations() {
      page.reviewLoading = true
      try {
        const list = await request({
          url: `/api/admin/reservations/pending?adminUserId=${page.adminUserId}`,
          method: 'GET'
        })

        const finalList = Array.isArray(list) ? list : []
        // 为每条预约补充前端驳回原因草稿字段，避免污染后端真实数据结构。
        page.pendingReservations = finalList.map(item => ({
          ...item,
          rejectReasonDraft: ''
        }))
        const availableIds = new Set(page.pendingReservations.map(item => item.id))
        page.selectedReservationIds = page.selectedReservationIds.filter(id => availableIds.has(id))
      } catch (error) {
        uni.showToast({ title: error.message || '加载待审核预约失败', icon: 'none' })
      } finally {
        page.reviewLoading = false
      }
    },

    /**
     * 判断预约是否已被选择。
     * @param {Number} reservationId 预约ID
     * @returns {Boolean}
     */
    isReservationSelected(reservationId) {
      return page.selectedReservationIds.includes(reservationId)
    },

    /**
     * 切换预约选择状态。
     * @param {Object} item 预约项
     */
    toggleReservationSelection(item) {
      if (!item || !item.canReview || page.reviewActionBusy) {
        return
      }
      if (page.isReservationSelected(item.id)) {
        page.selectedReservationIds = page.selectedReservationIds.filter(id => id !== item.id)
        return
      }
      page.selectedReservationIds = [...page.selectedReservationIds, item.id]
    },

    /**
     * 切换全选当前可审核预约。
     */
    toggleAllReviewSelection() {
      if (page.reviewActionBusy) {
        return
      }
      if (page.allReviewReservationsSelected) {
        page.selectedReservationIds = []
        return
      }
      page.selectedReservationIds = [...page.reviewableReservationIds]
    },

    /**
     * 批量审核选中的预约。
     * @param {Boolean} approved 是否通过
     */
    handleBatchReview(approved) {
      if (page.selectedReservationIds.length === 0) {
        uni.showToast({ title: '请先选择预约', icon: 'none' })
        return
      }
      const actionText = approved ? '通过' : '驳回'
      const rejectReason = (page.batchRejectReason || '').trim() || '管理员批量驳回'

      uni.showModal({
        title: '确认批量审核',
        content: `确定批量${actionText}${page.selectedReservationIds.length}条预约申请吗？`,
        success: async (res) => {
          if (!res.confirm || page.reviewActionBusy) {
            return
          }

          page.batchReviewing = true
          try {
            const result = await request({
              url: '/api/admin/reservations/review/batch',
              method: 'POST',
              data: {
                adminUserId: page.adminUserId,
                reservationIds: page.selectedReservationIds,
                approved,
                rejectReason
              }
            })
            page.showBatchReviewResult('批量审核结果', result)
            await Promise.all([page.loadPendingReservations(), page.loadStats()])
          } catch (error) {
            uni.showToast({ title: error.message || '批量审核失败', icon: 'none' })
          } finally {
            page.batchReviewing = false
          }
        }
      })
    },

    /**
     * 自动审核当前待审核预约。
     */
    handleAutoReview() {
      if (page.pendingReservations.length === 0) {
        uni.showToast({ title: '暂无待审核预约', icon: 'none' })
        return
      }

      uni.showModal({
        title: '确认自动审核',
        content: '系统将通过可预约申请，并驳回已开始、冲突或会议室不可用的申请。',
        success: async (res) => {
          if (!res.confirm || page.reviewActionBusy) {
            return
          }

          page.autoReviewing = true
          try {
            const result = await request({
              url: '/api/admin/reservations/review/auto',
              method: 'POST',
              data: {
                adminUserId: page.adminUserId
              }
            })
            page.showBatchReviewResult('自动审核结果', result)
            await Promise.all([page.loadPendingReservations(), page.loadStats()])
          } catch (error) {
            uni.showToast({ title: error.message || '自动审核失败', icon: 'none' })
          } finally {
            page.autoReviewing = false
          }
        }
      })
    },

    /**
     * 展示批量审核结果。
     * @param {String} title 弹窗标题
     * @param {Object} result 批量结果
     */
    showBatchReviewResult(title, result) {
      uni.showModal({
        title,
        content: page.formatBatchReviewResult(result || {}),
        showCancel: false
      })
    },

    /**
     * 格式化批量审核结果。
     * @param {Object} result 批量结果
     * @returns {String}
     */
    formatBatchReviewResult(result) {
      const failureMessages = Array.isArray(result.failureMessages) ? result.failureMessages : []
      const lines = [
        `处理总数：${result.totalCount || 0}条`,
        `成功：${result.successCount || 0}条`,
        `通过：${result.approvedCount || 0}条`,
        `驳回：${result.rejectedCount || 0}条`,
        `失败：${result.failedCount || 0}条`
      ]
      if (failureMessages.length > 0) {
        lines.push('失败明细：')
        lines.push(...failureMessages.slice(0, 3))
        if (failureMessages.length > 3) {
          lines.push(`其余${failureMessages.length - 3}条请刷新后重试`)
        }
      }
      return lines.join('\n')
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

          if (page.reviewingId) {
            return
          }

          page.reviewingId = item.id
          try {
            await request({
              url: `/api/admin/reservations/${item.id}/review`,
              method: 'POST',
              data: {
                adminUserId: page.adminUserId,
                approved,
                rejectReason
              }
            })
            uni.showToast({ title: '审核完成', icon: 'success' })
            await Promise.all([page.loadPendingReservations(), page.loadStats()])
          } catch (error) {
            uni.showToast({ title: error.message || '审核失败', icon: 'none' })
          } finally {
            page.reviewingId = null
          }
        }
      })
    },

    /**
     * 加载会议室列表。
     */
    async loadMeetingRooms() {
      page.roomLoading = true
      try {
        const list = await request({
          url: `/api/admin/meeting-rooms?adminUserId=${page.adminUserId}`,
          method: 'GET'
        })
        page.roomList = Array.isArray(list) ? list : []

        const roomOptionExists = page.emergencyRoomOptions.some(
          item => item.value === page.emergencyForm.roomId
        )
        if (!roomOptionExists) {
          page.emergencyForm.roomId = page.emergencyRoomOptions.length > 0
            ? page.emergencyRoomOptions[0].value
            : ''
        }
      } catch (error) {
        uni.showToast({ title: error.message || '加载会议室失败', icon: 'none' })
      } finally {
        page.roomLoading = false
      }
    },

    /**
     * 加载设备管理列表。
     */
    async loadEquipmentList() {
      page.equipmentLoading = true
      try {
        const list = await request({
          url: `/api/admin/equipments/manage?adminUserId=${page.adminUserId}`,
          method: 'GET'
        })
        page.equipmentList = Array.isArray(list) ? list : []
      } catch (error) {
        uni.showToast({ title: error.message || '加载设备列表失败', icon: 'none' })
      } finally {
        page.equipmentLoading = false
      }
    },

    /**
     * 加载设备报修列表。
     */
    async loadRepairList() {
      page.repairLoading = true
      try {
        const list = await request({
          url: `/api/admin/equipment-repairs?adminUserId=${page.adminUserId}`,
          method: 'GET'
        })
        page.repairList = Array.isArray(list) ? list : []
      } catch (error) {
        uni.showToast({ title: error.message || '加载报修记录失败', icon: 'none' })
      } finally {
        page.repairLoading = false
      }
    },

    /**
     * 标记报修记录已修复。
     * @param {Object} item 报修记录
     */
    resolveRepair(item) {
      uni.showModal({
        title: '确认修复',
        content: `确定已修复【${item.equipmentName || '设备'}】吗？`,
        success: async (res) => {
          if (!res.confirm || page.resolvingRepairId) {
            return
          }

          page.resolvingRepairId = item.id
          try {
            await request({
              url: `/api/admin/equipment-repairs/${item.id}/resolve`,
              method: 'POST',
              data: {
                adminUserId: page.adminUserId,
                fixRemark: '管理员确认已修复'
              }
            })
            uni.showToast({ title: '已标记修复', icon: 'success' })
            await page.loadRepairList()
          } catch (error) {
            uni.showToast({ title: error.message || '操作失败', icon: 'none' })
          } finally {
            page.resolvingRepairId = null
          }
        }
      })
    },

    /**
     * 加载设备选项。
     */
    async loadEquipmentOptions() {
      try {
        const list = await request({
          url: `/api/admin/equipments?adminUserId=${page.adminUserId}`,
          method: 'GET'
        })
        page.equipmentOptions = Array.isArray(list) ? list : []
      } catch (error) {
        uni.showToast({ title: error.message || '加载设备选项失败', icon: 'none' })
      }
    },

    /**
     * 加载管理员列表。
     */
    async loadAdminList() {
      page.adminLoading = true
      try {
        const list = await request({
          url: `/api/admin/admin-users?adminUserId=${page.adminUserId}`,
          method: 'GET'
        })
        page.adminList = Array.isArray(list) ? list : []
      } catch (error) {
        uni.showToast({ title: error.message || '加载管理员失败', icon: 'none' })
      } finally {
        page.adminLoading = false
      }
    },

    /**
     * 加载统计数据。
     */
    async loadStats() {
      page.statsLoading = true
      try {
        const stats = await request({
          url: `/api/admin/stats?adminUserId=${page.adminUserId}`,
          method: 'GET'
        })
        page.stats = { ...createDefaultStats(), ...(stats || {}) }
      } catch (error) {
        uni.showToast({ title: error.message || '加载统计数据失败', icon: 'none' })
      } finally {
        page.statsLoading = false
      }
    },

    /**
     * 打开新增会议室弹窗。
     */
    openCreateRoom() {
      page.roomForm = createDefaultRoomForm()
      page.roomCoverUploading = false
      page.showRoomModal = true
    },

    /**
     * 打开编辑会议室弹窗。
     * @param {Object} room 会议室数据
     */
    openEditRoom(room) {
      page.roomForm = {
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
      page.roomCoverUploading = false
      page.showRoomModal = true
    },

    /**
     * 关闭会议室弹窗。
     */
    closeRoomModal() {
      if (page.roomSaving || page.roomCoverUploading) {
        return
      }
      page.showRoomModal = false
    },

    /**
     * 会议室封面地址转换为可访问URL。
     * @param {String} coverImage 原始封面地址
     * @returns {String}
     */
    displayCoverImage(coverImage) {
      return buildAssetUrl(coverImage || DEFAULT_ROOM_IMAGE)
    },

    /**
     * 选择并上传会议室封面图。
     */
    chooseRoomCoverImage() {
      if (page.roomCoverUploading) {
        return
      }

      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const filePath = Array.isArray(res.tempFilePaths) ? res.tempFilePaths[0] : ''
          if (!filePath) {
            uni.showToast({ title: '未选择有效图片', icon: 'none' })
            return
          }

          page.roomCoverUploading = true
          try {
            const uploadedUrl = await uploadFile({
              url: '/api/admin/meeting-rooms/cover',
              filePath,
              name: 'file',
              formData: {
                adminUserId: `${page.adminUserId}`
              }
            })
            page.roomForm.coverImage = uploadedUrl || ''
            uni.showToast({ title: '上传成功', icon: 'success' })
          } catch (error) {
            uni.showToast({ title: error.message || '上传封面失败', icon: 'none' })
          } finally {
            page.roomCoverUploading = false
          }
        },
        fail: (err) => {
          const errMsg = (err && err.errMsg) || ''
          if (errMsg.includes('cancel')) {
            return
          }
          uni.showToast({ title: '选择图片失败', icon: 'none' })
        }
      })
    },

    /**
     * 清空会议室封面图。
     */
    clearRoomCoverImage() {
      if (page.roomCoverUploading) {
        return
      }
      page.roomForm.coverImage = ''
    },

    /**
     * 打开新增设备弹窗。
     */
    openCreateEquipment() {
      page.equipmentForm = createDefaultEquipmentForm()
      page.showEquipmentModal = true
    },

    /**
     * 打开编辑设备弹窗。
     * @param {Object} equipment 设备数据
     */
    openEditEquipment(equipment) {
      page.equipmentForm = {
        id: equipment.id,
        name: equipment.name || '',
        icon: equipment.icon || '',
        description: equipment.description || '',
        status: Number(equipment.status)
      }
      page.showEquipmentModal = true
    },

    /**
     * 关闭设备弹窗。
     */
    closeEquipmentModal() {
      if (page.equipmentSaving) {
        return
      }
      page.showEquipmentModal = false
    },

    /**
     * 打开新增管理员弹窗。
     */
    openCreateAdmin() {
      page.adminForm = createDefaultAdminForm()
      page.showAdminModal = true
    },

    /**
     * 打开编辑管理员弹窗。
     * @param {Object} admin 管理员数据
     */
    openEditAdmin(admin) {
      page.adminForm = {
        id: admin.id,
        username: admin.username || '',
        nickname: admin.nickname || '',
        phone: admin.phone || '',
        email: admin.email || '',
        password: '',
        status: Number(admin.status)
      }
      page.showAdminModal = true
    },

    /**
     * 关闭管理员弹窗。
     */
    closeAdminModal() {
      if (page.adminSaving) {
        return
      }
      page.showAdminModal = false
    },

    /**
     * 修改管理员状态。
     * @param {Object} event picker事件
     */
    handleAdminStatusChange(event) {
      const index = Number(event.detail.value)
      const option = ADMIN_STATUS_OPTIONS[index]
      page.adminForm.status = option ? option.value : 1
    },

    /**
     * 提交管理员表单。
     */
    async submitAdminForm() {
      const finalUsername = (page.adminForm.username || '').trim()
      const finalPhone = (page.adminForm.phone || '').trim()
      const finalPassword = (page.adminForm.password || '').trim()

      if (!finalUsername) {
        uni.showToast({ title: '请输入管理员用户名', icon: 'none' })
        return
      }
      if (!finalPhone) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return
      }
      if (!page.adminForm.id && !finalPassword) {
        uni.showToast({ title: '请输入登录密码', icon: 'none' })
        return
      }
      if (page.adminSaving) {
        return
      }

      const payload = {
        adminUserId: page.adminUserId,
        username: finalUsername,
        nickname: (page.adminForm.nickname || '').trim(),
        phone: finalPhone,
        email: (page.adminForm.email || '').trim(),
        password: finalPassword,
        status: Number(page.adminForm.status)
      }

      page.adminSaving = true
      try {
        if (page.adminForm.id) {
          await request({
            url: `/api/admin/admin-users/${page.adminForm.id}`,
            method: 'PUT',
            data: payload
          })
          if (Number(page.adminForm.id) === Number(page.adminUserId)) {
            const currentUserInfo = uni.getStorageSync('userInfo') || {}
            uni.setStorageSync('userInfo', {
              ...currentUserInfo,
              username: finalUsername,
              nickname: payload.nickname || finalUsername,
              phone: finalPhone,
              role: 1
            })
          }
          uni.showToast({ title: '编辑成功', icon: 'success' })
        } else {
          await request({
            url: '/api/admin/admin-users',
            method: 'POST',
            data: payload
          })
          uni.showToast({ title: '新增成功', icon: 'success' })
        }

        page.showAdminModal = false
        await Promise.all([page.loadAdminList(), page.loadStats()])
      } catch (error) {
        uni.showToast({ title: error.message || '保存失败', icon: 'none' })
      } finally {
        page.adminSaving = false
      }
    },

    /**
     * 删除管理员。
     * @param {Object} admin 管理员数据
     */
    deleteAdmin(admin) {
      uni.showModal({
        title: '确认删除',
        content: `确定删除管理员【${admin.nickname || admin.username}】吗？`,
        success: async (res) => {
          if (!res.confirm) {
            return
          }

          try {
            await request({
              url: `/api/admin/admin-users/${admin.id}/delete`,
              method: 'POST',
              data: {
                adminUserId: page.adminUserId
              }
            })
            uni.showToast({ title: '删除成功', icon: 'success' })
            await Promise.all([page.loadAdminList(), page.loadStats()])
          } catch (error) {
            uni.showToast({ title: error.message || '删除失败', icon: 'none' })
          }
        }
      })
    },

    /**
     * 修改设备状态。
     * @param {Object} event picker事件
     */
    handleEquipmentStatusChange(event) {
      const index = Number(event.detail.value)
      const option = EQUIPMENT_STATUS_OPTIONS[index]
      page.equipmentForm.status = option ? option.value : 1
    },

    /**
     * 提交设备表单。
     */
    async submitEquipmentForm() {
      const finalName = (page.equipmentForm.name || '').trim()

      if (!finalName) {
        uni.showToast({ title: '请输入设备名称', icon: 'none' })
        return
      }
      if (page.equipmentSaving) {
        return
      }

      const payload = {
        adminUserId: page.adminUserId,
        name: finalName,
        icon: (page.equipmentForm.icon || '').trim(),
        description: (page.equipmentForm.description || '').trim(),
        status: Number(page.equipmentForm.status)
      }

      page.equipmentSaving = true
      try {
        if (page.equipmentForm.id) {
          await request({
            url: `/api/admin/equipments/${page.equipmentForm.id}`,
            method: 'PUT',
            data: payload
          })
          uni.showToast({ title: '编辑成功', icon: 'success' })
        } else {
          await request({
            url: '/api/admin/equipments',
            method: 'POST',
            data: payload
          })
          uni.showToast({ title: '新增成功', icon: 'success' })
        }

        page.showEquipmentModal = false
        await Promise.all([
          page.loadEquipmentList(),
          page.loadEquipmentOptions(),
          page.loadMeetingRooms()
        ])
      } catch (error) {
        uni.showToast({ title: error.message || '保存失败', icon: 'none' })
      } finally {
        page.equipmentSaving = false
      }
    },

    /**
     * 停用设备。
     * @param {Object} equipment 设备数据
     */
    disableEquipment(equipment) {
      uni.showModal({
        title: '确认停用',
        content: `确定停用设备【${equipment.name}】吗？`,
        success: async (res) => {
          if (!res.confirm) {
            return
          }

          try {
            await request({
              url: `/api/admin/equipments/${equipment.id}/disable`,
              method: 'POST',
              data: {
                adminUserId: page.adminUserId
              }
            })
            uni.showToast({ title: '停用成功', icon: 'success' })
            await Promise.all([
              page.loadEquipmentList(),
              page.loadEquipmentOptions(),
              page.loadMeetingRooms()
            ])
          } catch (error) {
            uni.showToast({ title: error.message || '停用失败', icon: 'none' })
          }
        }
      })
    },

    /**
     * 切换会议室设备选中状态。
     * @param {Number} equipmentId 设备ID
     */
    toggleRoomEquipment(equipmentId) {
      const index = page.roomForm.equipmentIds.indexOf(equipmentId)
      if (index >= 0) {
        page.roomForm.equipmentIds.splice(index, 1)
      } else {
        page.roomForm.equipmentIds.push(equipmentId)
      }
    },

    /**
     * 修改会议室状态。
     * @param {Object} event picker事件
     */
    handleRoomStatusChange(event) {
      const index = Number(event.detail.value)
      const option = ROOM_STATUS_OPTIONS[index]
      page.roomForm.status = option ? option.value : 1
    },

    /**
     * 提交会议室表单。
     */
    async submitRoomForm() {
      const finalName = (page.roomForm.name || '').trim()
      const finalLocation = (page.roomForm.location || '').trim()
      const finalCapacity = Number(page.roomForm.capacity)
      const finalSortOrder = Number(page.roomForm.sortOrder || 0)

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
      if (page.roomSaving) {
        return
      }
      if (page.roomCoverUploading) {
        uni.showToast({ title: '封面图上传中，请稍候', icon: 'none' })
        return
      }

      const payload = {
        adminUserId: page.adminUserId,
        name: finalName,
        capacity: finalCapacity,
        location: finalLocation,
        building: (page.roomForm.building || '').trim(),
        floor: (page.roomForm.floor || '').trim(),
        description: (page.roomForm.description || '').trim(),
        coverImage: (page.roomForm.coverImage || '').trim(),
        status: Number(page.roomForm.status),
        sortOrder: Number.isNaN(finalSortOrder) ? 0 : finalSortOrder,
        equipmentIds: page.roomForm.equipmentIds
      }

      page.roomSaving = true
      try {
        if (page.roomForm.id) {
          await request({
            url: `/api/admin/meeting-rooms/${page.roomForm.id}`,
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

        page.showRoomModal = false
        await Promise.all([page.loadMeetingRooms(), page.loadStats()])
      } catch (error) {
        uni.showToast({ title: error.message || '保存失败', icon: 'none' })
      } finally {
        page.roomSaving = false
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
                adminUserId: page.adminUserId
              }
            })
            uni.showToast({ title: '停用成功', icon: 'success' })
            await Promise.all([page.loadMeetingRooms(), page.loadStats()])
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
      const option = page.emergencyRoomOptions[index]
      if (option) {
        page.emergencyForm.roomId = option.value
      }
    },

    /**
     * 紧急占用日期变化。
     * @param {Object} event picker事件
     */
    handleEmergencyDateChange(event) {
      page.emergencyForm.reservationDate = event.detail.value
    },

    /**
     * 紧急占用开始时间变化。
     * @param {Object} event picker事件
     */
    handleEmergencyStartTimeChange(event) {
      page.emergencyForm.startTime = event.detail.value
    },

    /**
     * 紧急占用结束时间变化。
     * @param {Object} event picker事件
     */
    handleEmergencyEndTimeChange(event) {
      page.emergencyForm.endTime = event.detail.value
    },

    /**
     * 切换是否强制协调冲突。
     * @param {Object} event switch事件
     */
    handleForceOverrideChange(event) {
      page.emergencyForm.forceOverride = !!event.detail.value
    },

    /**
     * 提交紧急占用。
     */
    async submitEmergencyOccupy() {
      if (!page.emergencyForm.roomId) {
        uni.showToast({ title: '请选择会议室', icon: 'none' })
        return
      }
      if (!page.emergencyForm.reservationDate) {
        uni.showToast({ title: '请选择占用日期', icon: 'none' })
        return
      }
      if (!page.emergencyForm.startTime || !page.emergencyForm.endTime) {
        uni.showToast({ title: '请选择占用时段', icon: 'none' })
        return
      }
      if (page.emergencyForm.startTime >= page.emergencyForm.endTime) {
        uni.showToast({ title: '开始时间必须早于结束时间', icon: 'none' })
        return
      }
      if (page.isStartedTimeSlot(page.emergencyForm.reservationDate, page.emergencyForm.startTime)) {
        uni.showToast({ title: '今天已开始的时段不可紧急占用', icon: 'none' })
        return
      }
      if (!(page.emergencyForm.title || '').trim()) {
        uni.showToast({ title: '请输入占用主题', icon: 'none' })
        return
      }
      if (!(page.emergencyForm.purpose || '').trim()) {
        uni.showToast({ title: '请输入占用说明', icon: 'none' })
        return
      }
      if (page.emergencySubmitting) {
        return
      }

      page.emergencySubmitting = true
      try {
        const result = await request({
          url: '/api/admin/reservations/emergency-occupy',
          method: 'POST',
          data: {
            adminUserId: page.adminUserId,
            roomId: page.emergencyForm.roomId,
            reservationDate: page.emergencyForm.reservationDate,
            startTime: `${page.emergencyForm.startTime}:00`,
            endTime: `${page.emergencyForm.endTime}:00`,
            title: (page.emergencyForm.title || '').trim(),
            purpose: (page.emergencyForm.purpose || '').trim(),
            forceOverride: page.emergencyForm.forceOverride,
            cancelReason: (page.emergencyForm.cancelReason || '').trim()
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
          showCancel: false,
          success: () => {
            page.goToCalendar()
          }
        })
        await Promise.all([page.loadPendingReservations(), page.loadStats()])
      } catch (error) {
        const message = error.message || '提交失败'
        // 未开启强制协调时若存在冲突，引导管理员一键切换为强制模式。
        if (!page.emergencyForm.forceOverride && message.includes('冲突预约')) {
          uni.showModal({
            title: '检测到冲突',
            content: `${message}\n是否开启强制协调并重新提交？`,
            success: (res) => {
              if (res.confirm) {
                page.emergencyForm.forceOverride = true
              }
            }
          })
        } else {
          uni.showToast({ title: message, icon: 'none' })
        }
      } finally {
        page.emergencySubmitting = false
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
     * 设备状态对应样式类。
     * @param {Number} status 状态码
     * @returns {String}
     */
    equipmentStatusClass(status) {
      return Number(status) === 1 ? 'normal' : 'disabled'
    },

    /**
     * 报修状态对应样式类。
     * @param {Number} status 状态码
     * @returns {String}
     */
    repairStatusClass(status) {
      return Number(status) === 1 ? 'normal' : 'pending'
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
     * 设备状态文案转换。
     * @param {Number} status 状态码
     * @returns {String}
     */
    equipmentStatusText(status) {
      const option = EQUIPMENT_STATUS_OPTIONS.find(item => item.value === Number(status))
      return option ? option.label : '未知状态'
    },

    /**
     * 管理员状态对应样式类。
     * @param {Number} status 状态码
     * @returns {String}
     */
    adminStatusClass(status) {
      return Number(status) === 1 ? 'normal' : 'disabled'
    },

    /**
     * 管理员状态文案转换。
     * @param {Number} status 状态码
     * @returns {String}
     */
    adminStatusText(status) {
      const option = ADMIN_STATUS_OPTIONS.find(item => item.value === Number(status))
      return option ? option.label : '未知状态'
    },

    /**
     * 判断时段是否已经达到开始时间。
     * @param {String} dateText 日期字符串
     * @param {String} startTime 开始时间 HH:mm
     * @returns {Boolean}
     */
    isStartedTimeSlot(dateText, startTime) {
      return dateText === page.formatDate(new Date()) && startTime <= page.currentTimeText()
    },

    /**
     * 获取当前时间字符串。
     * @returns {String}
     */
    currentTimeText() {
      const now = new Date()
      const hour = `${now.getHours()}`.padStart(2, '0')
      const minute = `${now.getMinutes()}`.padStart(2, '0')
      return `${hour}:${minute}`
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
})

onLoad(() => page.onLoad())

const {
  activeTab,
  adminUserId,
  reviewLoading,
  reviewingId,
  selectedReservationIds,
  batchReviewing,
  autoReviewing,
  batchRejectReason,
  pendingReservations,
  roomLoading,
  roomList,
  equipmentOptions,
  showRoomModal,
  roomSaving,
  roomCoverUploading,
  roomForm,
  equipmentLoading,
  equipmentList,
  showEquipmentModal,
  equipmentSaving,
  equipmentForm,
  repairLoading,
  repairList,
  resolvingRepairId,
  adminLoading,
  adminList,
  showAdminModal,
  adminSaving,
  adminForm,
  statsLoading,
  stats,
  emergencySubmitting,
  emergencyForm,
  minDate,
  roomStatusLabels,
  roomStatusIndex,
  equipmentStatusLabels,
  equipmentStatusIndex,
  adminStatusLabels,
  adminStatusIndex,
  selectedReservationCount,
  reviewableReservationIds,
  allReviewReservationsSelected,
  reviewActionBusy,
  emergencyRoomOptions,
  emergencyRoomIndex,
  emergencyRoomLabel,
  goToCalendar,
  ensureAdminLogin,
  loadAllData,
  switchTab,
  refreshCurrentTab,
  loadPendingReservations,
  isReservationSelected,
  toggleReservationSelection,
  toggleAllReviewSelection,
  handleBatchReview,
  handleAutoReview,
  showBatchReviewResult,
  formatBatchReviewResult,
  handleReview,
  loadMeetingRooms,
  loadEquipmentList,
  loadRepairList,
  resolveRepair,
  loadEquipmentOptions,
  loadAdminList,
  loadStats,
  openCreateRoom,
  openEditRoom,
  closeRoomModal,
  displayCoverImage,
  chooseRoomCoverImage,
  clearRoomCoverImage,
  openCreateEquipment,
  openEditEquipment,
  closeEquipmentModal,
  openCreateAdmin,
  openEditAdmin,
  closeAdminModal,
  handleAdminStatusChange,
  submitAdminForm,
  deleteAdmin,
  handleEquipmentStatusChange,
  submitEquipmentForm,
  disableEquipment,
  toggleRoomEquipment,
  handleRoomStatusChange,
  submitRoomForm,
  disableRoom,
  handleEmergencyRoomChange,
  handleEmergencyDateChange,
  handleEmergencyStartTimeChange,
  handleEmergencyEndTimeChange,
  handleForceOverrideChange,
  submitEmergencyOccupy,
  roomStatusClass,
  equipmentStatusClass,
  repairStatusClass,
  roomStatusText,
  equipmentStatusText,
  adminStatusClass,
  adminStatusText,
  isStartedTimeSlot,
  currentTimeText,
  formatDate,
  handleLogout
} = toRefs(page)
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
  overflow-x: auto;
}

.tab-item {
  flex: 0 0 136rpx;
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

.review-toolbar {
  background-color: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 18rpx;
}

.review-toolbar-info {
  margin-bottom: 16rpx;
}

.review-toolbar-title {
  display: block;
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 6rpx;
}

.review-toolbar-tip {
  display: block;
  font-size: 23rpx;
  color: #888;
  line-height: 32rpx;
}

.review-toolbar-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin-bottom: 14rpx;
}

.toolbar-btn {
  margin: 0;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 10rpx;
  background-color: #f3f4f6;
  color: #333;
  font-size: 24rpx;
}

.toolbar-btn::after {
  border: none;
}

.toolbar-btn.approve,
.toolbar-btn.auto {
  background-color: #007aff;
  color: #fff;
}

.toolbar-btn.reject {
  background-color: #ef5350;
  color: #fff;
}

.toolbar-btn[disabled] {
  opacity: 0.7;
}

.batch-reject-input {
  width: 100%;
  height: 66rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 10rpx;
  padding: 0 18rpx;
  box-sizing: border-box;
  background-color: #fff;
  font-size: 25rpx;
  color: #333;
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

.review-title-row {
  display: flex;
  align-items: center;
  min-width: 0;
}

.select-box {
  width: 34rpx;
  height: 34rpx;
  border-radius: 8rpx;
  border: 2rpx solid #c8d0dc;
  margin-right: 12rpx;
  color: #fff;
  font-size: 24rpx;
  line-height: 34rpx;
  text-align: center;
  flex-shrink: 0;
}

.select-box.checked {
  background-color: #007aff;
  border-color: #007aff;
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  max-width: 520rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-line {
  font-size: 25rpx;
  color: #666;
  margin-top: 8rpx;
  word-break: break-all;
}

.room-cover-preview {
  width: 100%;
  height: 220rpx;
  border-radius: 10rpx;
  margin-top: 12rpx;
  background-color: #f3f4f6;
}

.room-cover-preview.form {
  margin-top: 14rpx;
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

.form-tip {
  display: block;
  margin: -8rpx 20rpx 20rpx;
  color: #888;
  font-size: 24rpx;
  line-height: 34rpx;
}

.label {
  font-size: 26rpx;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.upload-action-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.upload-btn {
  flex: 1;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: 10rpx;
  background-color: #f3f4f6;
  color: #333;
  font-size: 24rpx;
}

.upload-btn::after {
  border: none;
}

.upload-btn.primary {
  background-color: #007aff;
  color: #fff;
  margin-right: 12rpx;
}

.upload-btn[disabled] {
  opacity: 0.7;
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
