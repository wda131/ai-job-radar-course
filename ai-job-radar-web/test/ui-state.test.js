import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  aiSourceState,
  chooseNextInterviewQuestionId,
  chooseInterviewQuestionId,
  chooseInterviewSessionId,
  jobActionState
} from '../src/utils/uiState.js'

test('labels local model output and fallback output clearly', () => {
  assert.deepEqual(aiSourceState(true), {
    label: 'Ollama 本地大模型',
    className: 'ai'
  })
  assert.deepEqual(aiSourceState(false), {
    label: '规则引擎兜底',
    className: 'fallback'
  })
})

test('marks favorited and applied jobs', () => {
  assert.deepEqual(jobActionState(7, new Set([7]), new Set([7])), {
    favorited: true,
    applied: true,
    favoriteLabel: '已收藏',
    applyLabel: '已投递'
  })
})

test('keeps untouched jobs actionable', () => {
  const state = jobActionState(8, new Set([7]), new Set([7]))
  assert.equal(state.favoriteLabel, '收藏')
  assert.equal(state.applyLabel, '投递')
})

test('selects the requested interview or falls back to the latest', () => {
  const sessions = [{ id: 22 }, { id: 11 }]
  assert.equal(chooseInterviewSessionId(sessions, '11'), 11)
  assert.equal(chooseInterviewSessionId(sessions, '99'), 22)
  assert.equal(chooseInterviewSessionId([], null), null)
})

test('keeps snowflake interview ids precise as strings', () => {
  const sessions = [
    { id: '2076675828967198722' },
    { id: '2076675828967198721' }
  ]
  assert.equal(
    chooseInterviewSessionId(sessions, '2076675828967198721'),
    '2076675828967198721'
  )
})

test('opens any requested interview question and defaults to the first unanswered one', () => {
  const questions = [
    { id: '2077017040966352898', answer: { score: 82 } },
    { id: '2077017040966352899', answer: null },
    { id: '2077017040966352900', answer: null },
    { id: '2077017040966352901', answer: null }
  ]

  assert.equal(chooseInterviewQuestionId(questions), '2077017040966352899')
  assert.equal(
    chooseInterviewQuestionId(questions, '2077017040966352901'),
    '2077017040966352901'
  )
  assert.equal(chooseInterviewQuestionId(questions, 'missing'), '2077017040966352899')
})

test('keeps completed interview questions available for review', () => {
  const questions = [
    { id: '31', answer: { score: 80 } },
    { id: '32', answer: { score: 90 } }
  ]

  assert.equal(chooseInterviewQuestionId(questions), '31')
  assert.equal(chooseInterviewQuestionId([], null), null)
})

test('advances after the submitted question, wraps, and retains final feedback', () => {
  const questions = [
    { id: '41', answer: null },
    { id: '42', answer: { score: 81 } },
    { id: '43', answer: { score: 85 } },
    { id: '44', answer: null }
  ]

  assert.equal(chooseNextInterviewQuestionId(questions, '43'), '44')
  assert.equal(chooseNextInterviewQuestionId(questions, '44'), '41')

  const completedQuestions = questions.map(question => ({
    ...question,
    answer: question.answer || { score: 88 }
  }))
  assert.equal(chooseNextInterviewQuestionId(completedQuestions, '44'), '44')
})

test('makes every interview question selectable and keeps answered questions reviewable', () => {
  const view = readFileSync(new URL('../src/views/InterviewsView.vue', import.meta.url), 'utf8')
  const selectionHandlers = view.match(/@click="selectQuestion\(question\)"/g) || []

  assert.equal(selectionHandlers.length, 2)
  assert.match(view, /selectedQuestion\.answer/)
  assert.match(view, /本题回答与反馈/)
  assert.match(view, /:aria-current=/)
  assert.match(view, /button:not\(:last-child\) i::after/)
})

test('renders an unmistakable selected state for favorited jobs', () => {
  const card = readFileSync(new URL('../src/components/JobCard.vue', import.meta.url), 'utf8')

  assert.match(card, /favorited \? '✓ 已收藏' : '收藏'/)
  assert.match(card, /\.card-actions \.selected[\s\S]*background: linear-gradient/)
  assert.match(card, /:aria-pressed="favorited"/)
})

test('exposes the notification center through api router and navigation', () => {
  const api = readFileSync(new URL('../src/api/index.js', import.meta.url), 'utf8')
  const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')
  const app = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

  assert.match(api, /getNotifications/)
  assert.match(api, /readAllNotifications/)
  assert.match(router, /\/notifications/)
  assert.match(app, /消息中心/)
})

test('allows local model requests to finish without changing API behavior', () => {
  const request = readFileSync(new URL('../src/api/request.js', import.meta.url), 'utf8')
  assert.match(request, /timeout:\s*60000/)
})

test('binds the presentation server to an IPv4 address', () => {
  const startup = readFileSync(new URL('../../scripts/start-frontend.ps1', import.meta.url), 'utf8')
  assert.match(startup, /--host',\s*'0\.0\.0\.0'/)
})
