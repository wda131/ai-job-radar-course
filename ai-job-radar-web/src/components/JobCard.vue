<script setup>
import { salaryRange } from '../utils/format'
defineProps({ job: Object, score: Number, compact: Boolean })
defineEmits(['match', 'favorite', 'apply', 'interview'])
</script>
<template>
  <article class="job-card" :class="{ compact }">
    <div class="job-top"><div class="company-logo">{{ job.company?.slice(0, 1) }}</div><div><h3>{{ job.title }}</h3><p>{{ job.company }} · {{ job.city }}</p></div><span v-if="score != null" class="score-pill">{{ score }}% 匹配</span></div>
    <strong class="salary">{{ salaryRange(job) }}</strong>
    <div class="tags"><span>{{ job.experienceYears }}年经验</span><span>{{ job.education }}</span><span v-for="tag in (job.welfareTags || '').split(',').slice(0, 2)" :key="tag">{{ tag }}</span></div>
    <p class="job-desc">{{ job.description }}</p>
    <div class="card-actions"><button class="btn primary" @click="$emit('match', job)">AI 匹配</button><button class="btn ghost" @click="$emit('favorite', job)">收藏</button><button class="btn ghost" @click="$emit('apply', job)">记录投递</button><button class="icon-btn" title="模拟面试" @click="$emit('interview', job)">◫</button></div>
  </article>
</template>
