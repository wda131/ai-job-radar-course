<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { answerInterview, getInterview, getInterviews } from '../api'
import NoticeBar from '../components/NoticeBar.vue'
import PageHeader from '../components/PageHeader.vue'
import { chooseInterviewSessionId } from '../utils/uiState'

const route = useRoute()
const sessions = ref([])
const active = ref(null)
const answer = ref('')
const notice = ref('')
const current = computed(() => active.value?.questions?.find(question => !question.answer))

const load = async () => sessions.value = await getInterviews()
const open = async session => active.value = await getInterview(session.id)

const initialize = async () => {
  await load()
  const sessionId = chooseInterviewSessionId(sessions.value, route.query.session)
  if (sessionId) active.value = await getInterview(sessionId)
}

const submit = async () => {
  if (!answer.value.trim()) return
  try {
    await answerInterview(active.value.id, { questionId: current.value.id, answer: answer.value })
    answer.value = ''
    await open(active.value)
    await load()
    notice.value = '回答已评分'
  } catch (error) {
    notice.value = error.message
  }
  setTimeout(() => notice.value = '', 2200)
}

onMounted(initialize)
</script>

<template>
  <div>
    <NoticeBar :message="notice" />
    <PageHeader eyebrow="INTERVIEW LAB" title="模拟面试训练场" subtitle="围绕真实岗位生成问题，提交回答后即时得到评分和改进建议。">
      <RouterLink class="btn primary" to="/jobs">选择岗位练习</RouterLink>
    </PageHeader>
    <div class="interview-layout">
      <aside class="session-list">
        <h3>练习记录</h3>
        <button v-for="session in sessions" :key="session.id" :class="{ active: active?.id === session.id }" @click="open(session)">
          <span class="company-logo">{{ session.job.company.slice(0, 1) }}</span>
          <span><b>{{ session.job.title }}</b><small>{{ session.status === 'COMPLETED' ? `完成 · ${session.totalScore}分` : '练习中' }}</small></span>
          <i>›</i>
        </button>
        <div v-if="!sessions.length" class="empty">暂无练习记录</div>
      </aside>

      <section class="interview-room" v-if="active">
        <header>
          <div><span class="eyebrow">{{ active.job.company }}</span><h2>{{ active.job.title }} · 模拟面试</h2></div>
          <span class="status-chip">{{ active.status === 'COMPLETED' ? '已完成' : '进行中' }}</span>
        </header>

        <div class="question-steps">
          <i v-for="question in active.questions" :key="question.id" :class="{ done: question.answer, current: current?.id === question.id }">{{ question.questionOrder }}</i>
        </div>

        <div class="question-overview">
          <article v-for="question in active.questions" :key="question.id" :class="{ done: question.answer, current: current?.id === question.id }">
            <span>0{{ question.questionOrder }}</span>
            <p>{{ question.question }}</p>
            <b>{{ question.answer ? '已完成' : current?.id === question.id ? '当前题' : '待回答' }}</b>
          </article>
        </div>

        <div v-if="current" class="question-box">
          <small>QUESTION {{ current.questionOrder }} / {{ active.questions.length }}</small>
          <h2>{{ current.question }}</h2>
          <textarea v-model="answer" rows="7" placeholder="结合具体经历作答，尽量包含技术方法、行动过程和结果…"></textarea>
          <div><span>{{ answer.length }} 字</span><button class="btn primary" @click="submit">提交并获取反馈 →</button></div>
        </div>

        <div v-else class="result-box">
          <span>✓</span><h2>本轮练习已完成</h2><strong>{{ active.totalScore }} 分</strong><p>复盘下方各题反馈，再选择一个岗位继续练习。</p>
        </div>

        <div class="answer-history">
          <article v-for="question in active.questions.filter(item => item.answer)" :key="question.id">
            <b>{{ question.questionOrder }}. {{ question.question }}</b>
            <p>{{ question.answer.answer }}</p>
            <footer><span>评分 {{ question.answer.score }}</span>{{ question.answer.feedback }}</footer>
          </article>
        </div>
      </section>

      <section v-else class="interview-room empty-state">
        <span>◫</span><h2>还没有模拟面试</h2><p>从职位雷达选择岗位后，将自动生成并打开四道面试题。</p>
        <RouterLink class="btn primary" to="/jobs">选择岗位练习</RouterLink>
      </section>
    </div>
  </div>
</template>

<style scoped>
.question-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
  max-width: 850px;
  margin: 0 auto 24px;
}
.question-overview article {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  gap: 10px;
  align-items: center;
  min-height: 62px;
  padding: 10px 12px;
  border: 1px solid #253d53;
  border-radius: 8px;
  background: #0a1827;
}
.question-overview article.current {
  border-color: #35d2d0;
  background: #35d2d00c;
}
.question-overview article.done {
  border-color: #315e50;
  background: #53d28c0b;
}
.question-overview span {
  color: #35d2d0;
  font-size: 11px;
  font-weight: 800;
}
.question-overview p {
  margin: 0;
  color: #a9bac9;
  font-size: 10px;
  line-height: 1.55;
}
.question-overview b {
  color: #71879c;
  font-size: 8px;
  white-space: nowrap;
}
.question-overview .current b { color: #35d2d0; }
.question-overview .done b { color: #76e5bc; }
</style>
