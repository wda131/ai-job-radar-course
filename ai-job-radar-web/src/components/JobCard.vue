<script setup>
import { salaryRange } from '../utils/format'

defineProps({
  job: Object,
  score: Number,
  compact: Boolean,
  favorited: Boolean,
  applied: Boolean
})
defineEmits(['match', 'favorite', 'apply', 'interview'])
</script>

<template>
  <article class="job-card" :class="{ compact }">
    <div class="job-top"><div class="company-logo">{{ job.company?.slice(0, 1) }}</div><div><h3>{{ job.title }}</h3><p>{{ job.company }} · {{ job.city }} <span class="source-pill">{{ job.source === 'BOSS' ? 'BOSS' : '本地数据' }}</span></p></div><span v-if="score != null" class="score-pill">{{ score }}% 匹配</span></div>
    <strong class="salary">{{ salaryRange(job) }}</strong>
    <div class="tags"><span>{{ job.experienceYears }}年经验</span><span>{{ job.education }}</span><span v-for="tag in (job.welfareTags || '').split(',').slice(0, 2)" :key="tag">{{ tag }}</span></div>
    <p class="job-desc">{{ job.description }}</p>
    <div class="card-actions">
      <button type="button" class="btn primary" @click="$emit('match', job)">AI 匹配</button>
      <button type="button" class="btn ghost" :class="{ selected: favorited }" :aria-pressed="favorited" @click="$emit('favorite', job)">{{ favorited ? '✓ 已收藏' : '收藏' }}</button>
      <button type="button" class="btn ghost" :class="{ completed: applied }" :disabled="applied" @click="$emit('apply', job)">{{ applied ? '已投递' : '投递' }}</button>
      <button type="button" class="icon-btn" title="模拟面试" @click="$emit('interview', job)">◫</button>
    </div>
  </article>
</template>

<style scoped>
.source-pill {
  display: inline-flex;
  align-items: center;
  margin-left: 7px;
  padding: 1px 7px;
  color: #75e8df;
  font-size: 11px;
  line-height: 18px;
  border: 1px solid #2d7775;
  border-radius: 999px;
  background: #123534;
  vertical-align: middle;
}
.card-actions .selected {
  color: #061810;
  border-color: #74e3b3;
  background: linear-gradient(135deg, #53d28c, #76e5bc);
  box-shadow: 0 7px 18px #53d28c30;
  font-weight: 800;
}
.card-actions .completed {
  color: #8ff0c7;
  border-color: #315e50;
  background: #15352d;
  opacity: 1;
}
.card-actions .btn:disabled {
  cursor: default;
  transform: none;
}
</style>
