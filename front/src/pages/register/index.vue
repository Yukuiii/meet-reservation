<template>
  <view class="register-container">
    <view class="register-header">
      <text class="title">会议室预约系统</text>
      <text class="subtitle">创建新账号</text>
    </view>

    <view class="register-form">
      <!-- 用户名输入框 -->
      <view class="form-item">
        <input
          class="input"
          type="text"
          v-model="formData.username"
          placeholder="请输入用户名"
          placeholder-class="placeholder"
        />
      </view>

      <!-- 手机号输入框 -->
      <view class="form-item">
        <input
          class="input"
          type="number"
          v-model="formData.phone"
          placeholder="请输入手机号"
          placeholder-class="placeholder"
          maxlength="11"
        />
      </view>

      <!-- 密码输入框 -->
      <view class="form-item">
        <input
          class="input"
          type="password"
          v-model="formData.password"
          placeholder="请输入密码"
          placeholder-class="placeholder"
        />
      </view>

      <!-- 确认密码输入框 -->
      <view class="form-item">
        <input
          class="input"
          type="password"
          v-model="formData.confirmPassword"
          placeholder="请确认密码"
          placeholder-class="placeholder"
        />
      </view>

      <!-- 注册按钮 -->
      <button class="register-btn" @click="handleRegister">注册</button>

      <!-- 登录链接 -->
      <view class="login-link">
        <text>已有账号？</text>
        <text class="link" @click="goToLogin">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 注册页面
 * @description 用户注册功能，包含用户名、手机号、密码输入和注册验证
 */
export default {
  data() {
    return {
      // 表单数据
      formData: {
        username: '',
        phone: '',
        password: '',
        confirmPassword: ''
      }
    }
  },

  methods: {
    /**
     * 处理注册逻辑
     * @description 验证表单并提交注册请求
     */
    handleRegister() {
      const { username, phone, password, confirmPassword } = this.formData

      // 表单验证
      if (!username.trim()) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!phone.trim()) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return
      }
      // 手机号格式验证
      if (!/^1[3-9]\d{9}$/.test(phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' })
        return
      }
      if (!password.trim()) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
      if (password.length < 6) {
        uni.showToast({ title: '密码至少6位', icon: 'none' })
        return
      }
      if (password !== confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return
      }

      // TODO: 调用注册接口
      uni.showToast({ title: '注册成功', icon: 'success' })
      // 注册成功后跳转到登录页
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/login/index' })
      }, 1500)
    },

    /**
     * 跳转到登录页面
     */
    goToLogin() {
      uni.navigateTo({ url: '/pages/login/index' })
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  padding: 60rpx 40rpx;
  background-color: #f5f5f5;
}

.register-header {
  text-align: center;
  margin-bottom: 60rpx;
}

.register-header .title {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.register-header .subtitle {
  display: block;
  font-size: 28rpx;
  color: #999;
}

.register-form {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
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

.register-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  background-color: #007aff;
  color: #fff;
  font-size: 32rpx;
  border-radius: 10rpx;
  margin-top: 40rpx;
}

.register-btn::after {
  border: none;
}

.login-link {
  text-align: center;
  margin-top: 40rpx;
  font-size: 26rpx;
  color: #999;
}

.login-link .link {
  color: #007aff;
  margin-left: 10rpx;
}
</style>
