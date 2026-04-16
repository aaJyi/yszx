import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/views/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Odometer' },
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/UserList.vue'),
        meta: { title: '用户信息', icon: 'User' },
      },
      {
        path: 'health-data',
        name: 'HealthDataList',
        component: () => import('@/views/HealthDataList.vue'),
        meta: { title: '健康数据', icon: 'Files' },
      },
      {
        path: 'family-graph',
        name: 'FamilyGraph',
        component: () => import('@/views/FamilyGraph.vue'),
        meta: { title: '家人图谱', icon: 'Share' },
      },
      {
        path: 'body-pathology',
        name: 'BodyPathologyMap',
        component: () => import('@/views/BodyPathologyMap.vue'),
        meta: { title: '病机图谱', icon: 'FirstAidKit' },
      },
      {
        path: 'knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/KnowledgeBase.vue'),
        meta: { title: '知识库', icon: 'Reading' },
      },
      {
        path: 'smart-nav-keywords',
        redirect: { name: 'Dashboard' },
      },
      {
        path: 'club-division',
        name: 'ClubDivisionGraph',
        component: () => import('@/views/ClubDivisionGraph.vue'),
        meta: { title: '社团图谱', icon: 'Share' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 医视智行` : '医视智行 · 管理后台'
  if (to.meta.noAuth) return next()
  const token = localStorage.getItem('admin_token')
  if (!token && to.path !== '/login') return next('/login')
  next()
})

export default router
