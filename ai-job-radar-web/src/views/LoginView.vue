<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter(); const loading = ref(false); const error = ref('')
const form = reactive({ username: 'student', password: '123456' })
const submit = async () => {
  loading.value = true; error.value = ''
  try { const data = await login(form); localStorage.setItem('radar_token', data.token); localStorage.setItem('radar_profile', JSON.stringify(data.profile)); router.push('/') }
  catch (e) { error.value = e.message } finally { loading.value = false }
}
</script>
<template>
  <div class="login-page"><div class="login-orbit orbit-a"></div><div class="login-orbit orbit-b"></div>
    <section class="login-story"><div class="brand large"><span class="brand-mark">⌁</span><div><b>AI 求职雷达</b><small>CAREER INTELLIGENCE</small></div></div><span class="eyebrow">从海量机会中锁定你的方向</span><h1>让每一次投递<br><em>更有依据。</em></h1><p>连接个人能力画像、职位数据与面试反馈，用可解释的匹配结果管理你的完整求职旅程。</p><div class="login-metrics"><div><b>12+</b><span>精选岗位</span></div><div><b>4 维</b><span>智能匹配</span></div><div><b>全程</b><span>进度追踪</span></div></div></section>
    <form class="login-panel" @submit.prevent="submit"><span class="eyebrow">WELCOME BACK</span><h2>登录求职工作台</h2><p>课程演示账号已为你预填</p><label>账号<input v-model="form.username" autocomplete="username" /></label><label>密码<input v-model="form.password" type="password" autocomplete="current-password" /></label><div v-if="error" class="form-error">{{ error }}</div><button class="btn primary submit" :disabled="loading">{{ loading ? '正在连接…' : '进入工作台 →' }}</button><small class="demo-tip">演示账号 student / 123456</small></form>
  </div>
</template>
