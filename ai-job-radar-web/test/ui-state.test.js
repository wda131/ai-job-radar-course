import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { chooseInterviewSessionId, jobActionState } from '../src/utils/uiState.js'

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

test('exposes the notification center through api router and navigation', () => {
  const api = readFileSync(new URL('../src/api/index.js', import.meta.url), 'utf8')
  const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')
  const app = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

  assert.match(api, /getNotifications/)
  assert.match(api, /readAllNotifications/)
  assert.match(router, /\/notifications/)
  assert.match(app, /消息中心/)
})
