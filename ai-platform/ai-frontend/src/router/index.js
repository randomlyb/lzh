import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('../components/PublicLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: 'TechMind AI 技术知识平台' }
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { title: '登录' }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('../views/Register.vue'),
        meta: { title: '注册' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('../components/AdminLayout.vue'),
    redirect: '/admin/home',
    children: [
      {
        path: 'home',
        name: 'AdminHome',
        component: () => import('../views/AdminHome.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      {
        path: 'articles',
        name: 'Articles',
        component: () => import('../views/Articles.vue'),
        meta: { title: '文章', requiresAuth: true }
      },
      {
        path: 'knowledge-bases',
        name: 'KnowledgeBases',
        component: () => import('../views/KnowledgeBases.vue'),
        meta: { title: '知识库', requiresAuth: true }
      },
      {
        path: 'knowledge-bases/:id',
        name: 'KnowledgeBaseDetail',
        component: () => import('../views/KnowledgeBaseDetail.vue'),
        meta: { title: '知识库详情', requiresAuth: true }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue'),
        meta: { title: 'AI对话', requiresAuth: true }
      },
      {
        path: 'jobs',
        name: 'Jobs',
        component: () => import('../views/Jobs.vue'),
        meta: { title: '任务中心', requiresAuth: true }
      },
      {
        path: 'jobs/:id/logs',
        name: 'JobLogs',
        component: () => import('../views/JobLogs.vue'),
        meta: { title: '任务监控', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router