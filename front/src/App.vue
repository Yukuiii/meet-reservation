<script setup>
import { onHide, onLaunch, onShow } from '@dcloudio/uni-app'
import { refreshNotificationBadge } from './utils/notification'

onLaunch(() => {
  console.log('App Launch')
})

onShow(() => {
  console.log('App Show')
  const userInfo = uni.getStorageSync('userInfo')
  if (userInfo && userInfo.id) {
    refreshNotificationBadge(userInfo.id)
  }
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style>
/* 每个页面公共css */
page {
  height: 100%;
}
</style>
