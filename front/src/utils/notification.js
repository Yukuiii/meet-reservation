import { request } from './request'

/**
 * 刷新消息 TabBar 角标（消息 Tab 索引为 2）。
 * @param {number|string} userId
 */
export async function refreshNotificationBadge(userId) {
  if (!userId) return
  try {
    const result = await request({
      url: `/api/notifications/unread-count?userId=${userId}`,
      method: 'GET'
    })
    const count = result?.count || 0
    if (count > 0) {
      uni.setTabBarBadge({ index: 2, text: count > 99 ? '99+' : String(count) })
    } else {
      uni.removeTabBarBadge({ index: 2 })
    }
  } catch (e) {
    // 角标失败不影响页面
  }
}
