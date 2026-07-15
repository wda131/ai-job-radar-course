<script setup>
import { onMounted, ref } from 'vue'
import { getMatches } from '../api'
import PageHeader from '../components/PageHeader.vue'
import { salaryRange } from '../utils/format'
import { aiSourceState } from '../utils/uiState'

const matches = ref([])
const sourceState = aiUsed => aiSourceState(Boolean(aiUsed))

onMounted(async () => matches.value = await getMatches())
</script>

<template>
  <div>
    <PageHeader eyebrow="AI MATCHING" title="智能匹配报告" subtitle="规则评分与本地语义模型协同分析，每一分都有依据。">
      <RouterLink class="btn primary" to="/jobs">生成新报告</RouterLink>
    </PageHeader>

    <div v-if="matches.length" class="match-list">
      <article v-for="m in matches" :key="m.id" class="match-card">
        <div class="score-column">
          <div class="score-ring" :style="{ '--score': m.score }">
            <span><b>{{ m.score }}</b><small>综合分</small></span>
          </div>
          <span class="ai-source" :class="sourceState(m.aiUsed).className">
            {{ sourceState(m.aiUsed).label }}
          </span>
        </div>

        <div class="match-main">
          <span class="eyebrow">{{ m.job.company }} · {{ m.job.city }}</span>
          <h2>{{ m.job.title }}</h2>
          <div class="score-breakdown">
            <span>规则分 <b>{{ m.ruleScore ?? m.score }}</b></span>
            <span>语义分 <b>{{ m.semanticScore ?? '—' }}</b></span>
            <small>综合分 = 规则分 × 70% + 语义分 × 30%</small>
          </div>
          <p>{{ m.summary }}</p>

          <div class="skill-line">
            <strong>匹配技能</strong>
            <span v-for="s in (m.matchedSkills || '待补充').split(',')" :key="s" class="tag good">{{ s }}</span>
          </div>
          <div class="skill-line">
            <strong>缺失技能</strong>
            <span v-for="s in (m.missingSkills || '暂无明显短板').split(',')" :key="s" class="tag warn">{{ s }}</span>
          </div>

          <div v-if="m.aiUsed" class="ai-evidence">
            <section>
              <b>模型识别优势</b>
              <ul><li v-for="item in m.strengths" :key="item">{{ item }}</li></ul>
            </section>
            <section>
              <b>能力差距</b>
              <ul><li v-for="item in m.gaps" :key="item">{{ item }}</li></ul>
            </section>
            <section>
              <b>行动建议</b>
              <ul><li v-for="item in m.suggestions" :key="item">{{ item }}</li></ul>
            </section>
          </div>
          <div v-else class="fallback-note">本地模型不可用，本报告已自动使用原课程规则算法，核心功能不受影响。</div>
        </div>

        <div class="match-meta">
          <b>{{ salaryRange(m.job) }}</b>
          <small>{{ m.job.experienceYears }}年 · {{ m.job.education }}</small>
        </div>
      </article>
    </div>

    <div v-else class="empty-state">
      <span>◎</span><h2>还没有匹配报告</h2>
      <p>从职位雷达选择感兴趣的岗位，系统将给出可解释评分。</p>
      <RouterLink class="btn primary" to="/jobs">去选择职位</RouterLink>
    </div>
  </div>
</template>
