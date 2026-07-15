import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import './assets/pages.css'
import './assets/ai-enhancements.css'
import './assets/notifications.css'

createApp(App).use(router).mount('#app')
