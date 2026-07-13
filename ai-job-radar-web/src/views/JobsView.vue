<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  addFavorite,
  createApplication,
  createInterview,
  getApplications,
  getFavorites,
  getJobs,
  matchJob,
  removeFavorite
} from '../api'
import JobCard from '../components/JobCard.vue'
import NoticeBar from '../components/NoticeBar.vue'
import PageHeader from '../components/PageHeader.vue'
import { jobActionState } from '../utils/uiState'

const router = useRouter()
const filters = reactive({ keyword: '', city: '', minSalary: null, page: 1, size: 8 })
const page = ref({ records: [], total: 0 })
const favoriteIds = ref(new Set())
const applicationJobIds = ref(new Set())
const loading = ref(false)
const notice = ref('')

const flash = message => {
  notice.value = message
  setTimeout(() => notice.value = '', 2400)
}

const load = async () => {
  loading.value = true
  try {
    const [jobPage, favorites, applications] = await Promise.all([
      getJobs(filters),
      getFavorites(),
      getApplications()
    ])
    page.value = jobPage
    favoriteIds.value = new Set(favorites.map(item => item.job.id))
    applicationJobIds.value = new Set(applications.map(item => item.job.id))
  } finally {
    loading.value = false
  }
}

const stateFor = job => jobActionState(job.id, favoriteIds.value, applicationJobIds.value)

const toggleFavorite = async job => {
  const ids = new Set(favoriteIds.value)
  if (ids.has(job.id)) {
    await removeFavorite(job.id)
    ids.delete(job.id)
    flash('已取消收藏')
  } else {
    await addFavorite(job.id)
    ids.add(job.id)
    flash('收藏成功')
  }
  favoriteIds.value = ids
}

const apply = async job => {
  if (applicationJobIds.value.has(job.id)) return
  await createApplication({ jobId: job.id, progressNote: '已通过职位雷达投递' })
  applicationJobIds.value = new Set([...applicationJobIds.value, job.id])
  flash('投递成功')
}

const act = async (type, job) => {
  try {
    if (type === 'favorite') return await toggleFavorite(job)
    if (type === 'apply') return await apply(job)
    if (type === 'interview') {
      const session = await createInterview(job.id)
      await router.push({ path: '/interviews', query: { session: session.id } })
      return
    }
    const result = await matchJob(job.id)
    flash(`匹配完成：${result.score}分 · ${result.summary}`)
  } catch (error) {
    flash(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <NoticeBar :message="notice" />
    <PageHeader eyebrow="JOB RADAR" title="职位雷达" subtitle="按目标、城市与薪资扫描岗位，让选择建立在数据之上。">
      <span class="result-count">扫描到 <b>{{ page.total }}</b> 个机会</span>
    </PageHeader>
    <form class="filter-bar" @submit.prevent="filters.page = 1; load()">
      <label class="search-input">⌕<input v-model="filters.keyword" placeholder="搜索职位或公司" /></label>
      <select v-model="filters.city"><option value="">全部城市</option><option>北京</option><option>上海</option><option>深圳</option><option>杭州</option><option>济南</option></select>
      <select v-model="filters.minSalary"><option :value="null">不限薪资</option><option :value="10000">10K 以上</option><option :value="15000">15K 以上</option><option :value="20000">20K 以上</option></select>
      <button class="btn primary">开始扫描</button>
    </form>
    <div v-if="loading" class="loading-grid"><i v-for="n in 4" :key="n"></i></div>
    <div v-else class="job-grid">
      <JobCard
        v-for="job in page.records"
        :key="job.id"
        :job="job"
        :favorited="stateFor(job).favorited"
        :applied="stateFor(job).applied"
        @match="act('match', $event)"
        @favorite="act('favorite', $event)"
        @apply="act('apply', $event)"
        @interview="act('interview', $event)"
      />
    </div>
    <div class="pagination" v-if="page.total > filters.size">
      <button :disabled="filters.page === 1" @click="filters.page--; load()">上一页</button>
      <span>{{ filters.page }} / {{ Math.ceil(page.total / filters.size) }}</span>
      <button :disabled="filters.page >= Math.ceil(page.total / filters.size)" @click="filters.page++; load()">下一页</button>
    </div>
  </div>
</template>
