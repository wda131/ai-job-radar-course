<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { answerInterview, getInterview, getInterviews } from '../api'
import NoticeBar from '../components/NoticeBar.vue'
import PageHeader from '../components/PageHeader.vue'
import {
  aiSourceState,
  chooseInterviewQuestionId,
  chooseInterviewSessionId,
  chooseNextInterviewQuestionId
} from '../utils/uiState'

const route = useRoute()
const sessions = ref([])
const active = ref(null)
const selectedQuestionId = ref(null)
const answer = ref('')
const notice = ref('')
const selectedQuestion = computed(() => active.value?.questions?.find(
  question => String(question.id) === String(selectedQuestionId.value)
))
const completed = computed(() => active.value?.questions?.every(question => question.answer))
const sourceState = aiUsed => aiSourceState(Boolean(aiUsed))

const load = async () => sessions.value = await getInterviews()
const setActive = session => {
  active.value = session
  selectedQuestionId.value = chooseInterviewQuestionId(session.questions)
  answer.value = ''
}
const open = async session => setActive(await getInterview(session.id))
const selectQuestion = question => {
  selectedQuestionId.value = question.id
  answer.value = ''
}

const initialize = async () => {
  await load()
  const sessionId = chooseInterviewSessionId(sessions.value, route.query.session)
  if (sessionId) setActive(await getInterview(sessionId))
}

const submit = async () => {
  if (!selectedQuestion.value || selectedQuestion.value.answer || !answer.value.trim()) return
  try {
    const submittedQuestionId = selectedQuestion.value.id
    await answerInterview(active.value.id, {
      questionId: submittedQuestionId,
      answer: answer.value
    })
    answer.value = ''
    const refreshed = await getInterview(active.value.id)
    active.value = refreshed
    selectedQuestionId.value = chooseNextInterviewQuestionId(
      refreshed.questions,
      submittedQuestionId
    )
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

        <div class="question-steps" aria-label="面试题目导航">
          <button v-for="question in active.questions" :key="question.id" type="button" :aria-label="`打开第${question.questionOrder}题，${question.answer ? '已完成' : '待回答'}`" :aria-current="selectedQuestion?.id === question.id ? 'step' : undefined" @click="selectQuestion(question)">
            <i :class="{ done: question.answer, current: selectedQuestion?.id === question.id }">{{ question.questionOrder }}</i>
          </button>
        </div>

        <div class="question-overview">
          <button v-for="question in active.questions" :key="question.id" type="button" class="question-card" :class="{ done: question.answer, current: selectedQuestion?.id === question.id }" :aria-current="selectedQuestion?.id === question.id ? 'step' : undefined" @click="selectQuestion(question)">
            <span>0{{ question.questionOrder }}</span>
            <p>{{ question.question }}</p>
            <b>{{ question.answer ? selectedQuestion?.id === question.id ? '查看反馈' : '已完成' : selectedQuestion?.id === question.id ? '当前题' : '待回答' }}</b>
          </button>
        </div>

        <div v-if="completed" class="result-box compact-result">
          <span>✓</span><h2>本轮练习已完成</h2><strong>{{ active.totalScore }} 分</strong><p>点击上方任意题目，可查看回答、评分和改进建议。</p>
        </div>

        <div v-if="selectedQuestion && !selectedQuestion.answer" class="question-box">
          <small>QUESTION {{ selectedQuestion.questionOrder }} / {{ active.questions.length }}</small>
          <h2>{{ selectedQuestion.question }}</h2>
          <textarea v-model="answer" rows="7" placeholder="结合具体经历作答，尽量包含技术方法、行动过程和结果…"></textarea>
          <div><span>{{ answer.length }} 字</span><button class="btn primary" @click="submit">提交并获取反馈 →</button></div>
        </div>

        <div v-else-if="selectedQuestion?.answer" class="question-review">
          <small>本题回答与反馈 · QUESTION {{ selectedQuestion.questionOrder }}</small>
          <h2>{{ selectedQuestion.question }}</h2>
          <div class="review-answer"><b>你的回答</b><p>{{ selectedQuestion.answer.answer }}</p></div>
          <footer class="review-feedback">
            <div class="feedback-heading">
              <span>评分 {{ selectedQuestion.answer.score }}</span>
              <i class="ai-source" :class="sourceState(selectedQuestion.answer.aiUsed).className">
                {{ sourceState(selectedQuestion.answer.aiUsed).label }}
              </i>
            </div>
            <p>{{ selectedQuestion.answer.feedback }}</p>
            <div v-if="selectedQuestion.answer.aiUsed" class="feedback-grid">
              <section><b>回答优势</b><ul><li v-for="item in selectedQuestion.answer.strengths" :key="item">{{ item }}</li></ul></section>
              <section><b>可改进点</b><ul><li v-for="item in selectedQuestion.answer.weaknesses" :key="item">{{ item }}</li></ul></section>
              <section><b>优化建议</b><p>{{ selectedQuestion.answer.suggestion }}</p></section>
            </div>
            <div v-else class="fallback-feedback">本题使用课程关键词规则评分；启动 Ollama 后会自动切换为本地大模型反馈。</div>
          </footer>
        </div>

        <div class="answer-history">
          <article v-for="question in active.questions.filter(item => item.answer)" :key="question.id">
            <b>{{ question.questionOrder }}. {{ question.question }}</b>
            <p>{{ question.answer.answer }}</p>
            <footer><span>评分 {{ question.answer.score }}</span><i class="history-source">{{ sourceState(question.answer.aiUsed).label }}</i>{{ question.answer.feedback }}</footer>
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
.question-overview .question-card {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  gap: 10px;
  align-items: center;
  min-height: 62px;
  padding: 10px 12px;
  border: 1px solid #253d53;
  border-radius: 8px;
  background: #0a1827;
  color: inherit;
  text-align: left;
  cursor: pointer;
}
.question-overview .question-card:hover {
  border-color: #39718b;
  transform: translateY(-1px);
}
.question-overview .question-card.current {
  border-color: #35d2d0;
  background: #35d2d00c;
  box-shadow: inset 3px 0 #35d2d0;
}
.question-overview .question-card.done {
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
.question-steps button {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
}
.question-steps button:hover i { border-color: #35d2d0; color: #35d2d0; }
.question-steps button:not(:last-child) i::after {
  content: '';
  position: absolute;
  left: 24px;
  width: 34px;
  border-top: 1px solid #294157;
}
.compact-result { padding: 12px 20px 24px; }
.compact-result > span { width: 36px; height: 36px; font-size: 17px; }
.compact-result h2 { margin: 8px 0 3px; }
.compact-result strong { font-size: 26px; }
.question-review {
  max-width: 750px;
  margin: 0 auto;
}
.question-review > small { color: #53d28c; font-size: 9px; letter-spacing: 1px; }
.question-review > h2 { font-size: 19px; line-height: 1.6; margin: 8px 0 18px; }
.review-answer,
.question-review > footer {
  padding: 16px;
  border: 1px solid #263e54;
  border-radius: 8px;
  background: #091725;
}
.review-answer b,
.question-review > footer span { color: #8ca2b7; font-size: 10px; }
.review-answer p,
.question-review > footer p { margin: 8px 0 0; color: #c1d0dd; font-size: 11px; line-height: 1.7; }
.question-review > footer { margin-top: 10px; border-color: #315e50; background: #53d28c0b; }
.question-review > footer span { color: #76e5bc; font-weight: 800; }
.feedback-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.feedback-heading .ai-source { font-style: normal; }
.feedback-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-top: 14px; }
.feedback-grid section { padding: 11px; border: 1px solid #29475b; border-radius: 7px; background: #07131f; }
.feedback-grid b { color: #8fd9d2; font-size: 10px; }
.feedback-grid ul { margin: 7px 0 0; padding-left: 15px; color: #aebdcc; font-size: 10px; line-height: 1.7; }
.feedback-grid section > p { color: #aebdcc; font-size: 10px; line-height: 1.7; }
.fallback-feedback { margin-top: 12px; padding: 10px; border-radius: 6px; background: #f3b85b0d; color: #d9b877; font-size: 9px; }
.history-source { margin-right: 8px; color: #6f879c; font-size: 8px; font-style: normal; }
</style>
