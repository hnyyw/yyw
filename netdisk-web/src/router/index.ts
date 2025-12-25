import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import DriveView from '@/views/DriveView.vue'
import ShareView from '@/views/ShareView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/drive' },
    { path: '/login', component: LoginView },
    { path: '/drive', component: DriveView },
    { path: '/share/:shareId', component: ShareView },
  ],
})

