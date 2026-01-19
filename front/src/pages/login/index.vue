<template>
  <view class="login-container">
    <view class="login-header">
      <text class="title">会议室预约系统</text>
      <text class="subtitle">欢迎登录</text>
    </view>

    <view class="login-form">
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

      <!-- 登录按钮 -->
      <button class="login-btn" @click="handleLogin">登录</button>

      <!-- 注册链接 -->
      <view class="register-link">
        <text>还没有账号？</text>
        <text class="link" @click="goToRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 登录页面
 * @description 用户登录功能，包含用户名密码输入和登录验证
 */
export default {
  data() {
    return {
      // 表单数据
      formData: {
        username: '',
        password: ''
      }
    }
  },

  methods: {
    /**
     * 处理登录逻辑
     * @description 验证表单并提交登录请求
     */
    handleLogin() {
      const { username, password } = this.formData

      // 表单验证
      if (!username.trim()) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!password.trim()) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }

      // TODO: 调用登录接口
      uni.showToast({ title: '登录成功', icon: 'success' })
    },

    /**
     * 跳转到注册页面
     */
    goToRegister() {
      uni.navigateTo({ url: '/pages/register/index' })
    }
  }
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
