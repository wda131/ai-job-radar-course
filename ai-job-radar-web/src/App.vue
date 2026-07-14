<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const profile = computed(() => {
  void route.path
  return JSON.parse(localStorage.getItem('radar_profile') || '{}')
})
const nav = [
  ['/', '⌂', '总览'], ['/jobs', '⌕', '职位雷达'], ['/matches', '◇', '智能匹配'],
  ['/applications', '▦', '求职进度'], ['/interviews', '◉', '模拟面试'],
  ['/notifications', '✦', '消息中心'], ['/profile', '●', '我的档案']
]
const logout = () => {
  localStorage.removeItem('radar_token')
  localStorage.removeItem('radar_profile')
  router.push('/login')
}
</script>

<template>
  <RouterView v-if="route.path === '/login'" />
  <div v-else class="shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">⌁</span><div><b>AI 求职雷达</b><small>CAREER INTELLIGENCE</small></div></div>
      <nav>
        <RouterLink v-for="item in nav" :key="item[0]" :to="item[0]" :class="{ active: route.path === item[0] }">
          <span class="nav-icon">{{ item[1] }}</span>{{ item[2] }}
        </RouterLink>
      </nav>
      <div class="sidebar-card">
        <span class="pulse"></span><small>求职档案完成度</small>
        <strong>{{ profile.skills ? 86 : 42 }}%</strong>
        <div class="progress"><i :style="{ width: (profile.skills ? 86 : 42) + '%' }"></i></div>
        <RouterLink to="/profile">完善个人画像 →</RouterLink>
      </div>
      <button class="user-block" @click="logout">
        <span class="avatar">{{ (profile.name || profile.username || '同').slice(0, 1) }}</span>
        <span><b>{{ profile.name || profile.username || '同学' }}</b><small>{{ profile.targetRole || '求职探索中' }}</small></span><i>→</i>
      </button>
    </aside>
    <main class="main"><RouterView /></main>
  </div>
</template>
