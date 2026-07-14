import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import JobsView from '../views/JobsView.vue'
import MatchesView from '../views/MatchesView.vue'
import ApplicationsView from '../views/ApplicationsView.vue'
import InterviewsView from '../views/InterviewsView.vue'
import ProfileView from '../views/ProfileView.vue'
import NotificationsView from '../views/NotificationsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/', component: DashboardView },
    { path: '/jobs', component: JobsView },
    { path: '/matches', component: MatchesView },
    { path: '/applications', component: ApplicationsView },
    { path: '/interviews', component: InterviewsView },
    { path: '/notifications', component: NotificationsView },
    { path: '/profile', component: ProfileView }
  ]
})

router.beforeEach(to => {
  if (!to.meta.public && !localStorage.getItem('radar_token')) return '/login'
  if (to.path === '/login' && localStorage.getItem('radar_token')) return '/'
})

export default router
