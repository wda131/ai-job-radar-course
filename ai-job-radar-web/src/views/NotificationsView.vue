<script setup>
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/PageHeader.vue'
import NoticeBar from '../components/NoticeBar.vue'
import { getNotifications, readAllNotifications, readNotification } from '../api'

const notifications = ref([])
const loading = ref(true)
const notice = ref('')
const unreadCount = computed(() => notifications.value.filter(item => !item.readStatus).length)

const load = async () => {
  loading.value = true
  try { notifications.value = await getNotifications() } finally { loading.value = false }
}
const markRead = async item => {
  if (item.readStatus) return
  await readNotification(item.id)
  item.readStatus = true
}
const markAll = async () => {
  if (!unreadCount.value) return
  await readAllNotifications()
  notifications.value.forEach(item => { item.readStatus = true })
  notice.value = '全部消息已标记为已读'
  setTimeout(() => { notice.value = '' }, 2200)
}
const formatTime = value => value ? value.replace('T', ' ').slice(0, 16) : ''

onMounted(load)
</script>

<template>
  <div>
    <NoticeBar :message="notice" />
    <PageHeader eyebrow="ASYNC NOTIFICATION" title="消息中心" subtitle="RabbitMQ 异步消费投递事件，及时记录每一次求职进展。">
      <button class="btn ghost" :disabled="!unreadCount" @click="markAll">全部已读 · {{ unreadCount }}</button>
    </PageHeader>
    <section class="notification-panel panel">
      <div v-if="loading" class="empty">正在同步消息...</div>
      <button v-for="item in notifications" v-else :key="item.id" class="notification-item"
              :class="{ unread: !item.readStatus }" @click="markRead(item)">
        <span class="notification-dot"></span><span class="notification-icon">↗</span>
        <span class="notification-copy">
          <b>{{ item.title }}</b><p>{{ item.content }}</p>
          <small>{{ formatTime(item.createdAt) }} · {{ item.readStatus ? '已读' : '点击标记已读' }}</small>
        </span>
      </button>
      <div v-if="!loading && !notifications.length" class="empty-state">
        <span>✦</span><h2>暂时没有消息</h2><p>完成一次职位投递后，这里会收到异步进度通知。</p>
        <RouterLink class="btn primary" to="/jobs">去发现职位</RouterLink>
      </div>
    </section>
  </div>
</template>
