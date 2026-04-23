<template>
  <view class="login-container">
    <view class="login-header">
      <text class="title">会议室预约系统</text>
      <text class="subtitle">欢迎登录</text>
    </view>

    <view class="login-form">
      <view class="login-type-group">
        <view
          class="login-type-item"
          :class="{ active: formData.loginType === 'user' }"
          @click="switchLoginType('user')"
        >
          用户登录
        </view>
        <view
          class="login-type-item"
          :class="{ active: formData.loginType === 'admin' }"
          @click="switchLoginType('admin')"
        >
          管理员登录
        </view>
      </view>

      <view class="form-item">
        <input
          class="input"
          type="text"
          v-model="formData.username"
          placeholder="请输入用户名"
          placeholder-class="placeholder"
        />
      </view>

      <view class="form-item">
        <input
          class="input"
          type="password"
          v-model="formData.password"
          placeholder="请输入密码"
          placeholder-class="placeholder"
        />
      </view>

      <button class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <view class="register-link" v-if="formData.loginType === 'user'">
        <text>还没有账号？</text>
        <text class="link" @click="goToRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '../../utils/request'

/**
 * 表单数据。
 */
const formData = reactive({
  username: '',
  password: '',
  loginType: 'user'
})

/**
 * 提交状态。
 */
const loading = ref(false)

/**
 * 页面加载时回填用户名。
 * @param {Object} options 页面参数
 */
onLoad((options) => {
  if (options && options.username) {
    formData.username = decodeURIComponent(options.username)
  }
})

/**
 * 切换登录类型。
 * @param {String} loginType 登录类型
 */
function switchLoginType(loginType) {
  if (loading.value) {
    return
  }
  formData.loginType = loginType
}

/**
 * 处理登录逻辑。
 */
async function handleLogin() {
  const { username, password, loginType } = formData
  const finalUsername = username.trim()
  const finalPassword = password.trim()

  if (!finalUsername) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!finalPassword) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (loading.value) {
    return
  }

  loading.value = true
  try {
    const data = await request({
      url: '/api/auth/login',
      method: 'POST',
      data: {
        username: finalUsername,
        password: finalPassword,
        loginType
      }
    })

    uni.setStorageSync('token', data.token)
    uni.setStorageSync('userInfo', data.userInfo || {})
    uni.setStorageSync('loginType', loginType)

    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      if (loginType === 'admin') {
        uni.reLaunch({ url: '/pages/admin/index' })
      } else {
        uni.reLaunch({ url: '/pages/index/index' })
      }
    }, 800)
  } catch (error) {
    uni.showToast({ title: error.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到注册页面。
 */
function goToRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}
</script>

<style scoped>
.login-container {
  height: 100%;
  padding: 60rpx 40rpx;
  background-color: #f5f5f5;
  box-sizing: border-box;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.login-header .title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.login-header .subtitle {
  display: block;
  font-size: 28rpx;
  color: #999;
}

.login-form {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
}

.login-type-group {
  display: flex;
  margin-bottom: 30rpx;
  border-radius: 12rpx;
  overflow: hidden;
  background-color: #f3f4f6;
}

.login-type-item {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  text-align: center;
  font-size: 28rpx;
  color: #666;
}

.login-type-item.active {
  background-color: #007aff;
  color: #fff;
  font-weight: bold;
}

.form-item {
  margin-bottom: 30rpx;
}

.form-item .input {
  width: 100%;
  height: 90rpx;
  padding: 0 30rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 10rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.placeholder {
  color: #ccc;
}

.login-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  background-color: #007aff;
  color: #fff;
  font-size: 32rpx;
  border-radius: 10rpx;
  margin-top: 40rpx;
}

.login-btn::after {
  border: none;
}

.login-btn[disabled] {
  opacity: 0.7;
}

.register-link {
  text-align: center;
  margin-top: 40rpx;
  font-size: 26rpx;
  color: #999;
}

.register-link .link {
  color: #007aff;
  margin-left: 10rpx;
}
</style>
