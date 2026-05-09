import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

function isPublicLoginName(name: unknown): name is 'loginUser' | 'loginAdmin' {
  return name === 'loginUser' || name === 'loginAdmin'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'portal',
      component: () => import('@/views/PortalHomeView.vue'),
      meta: { public: true, title: '系统入口' },
    },
    {
      path: '/login/user',
      name: 'loginUser',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '读者端登录' },
    },
    {
      path: '/login/admin',
      name: 'loginAdmin',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '管理端登录' },
    },
    { path: '/login', redirect: '/' },
    {
      path: '/u',
      component: () => import('@/layouts/UserLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'home',
          name: 'userHome',
          component: () => import('@/views/UserHomeView.vue'),
          meta: { title: '读者服务' },
        },
        {
          path: 'borrow',
          name: 'userBorrow',
          component: () => import('@/views/BorrowView.vue'),
          meta: { title: '借书申请' },
        },
        {
          path: 'return',
          name: 'userReturn',
          component: () => import('@/views/ReturnView.vue'),
          meta: { title: '还书' },
        },
        {
          path: 'pickup',
          name: 'userPickup',
          component: () => import('@/views/PickupView.vue'),
          meta: { title: '取书确认' },
        },
        {
          path: 'borrow-history',
          name: 'userBorrowHistory',
          component: () => import('@/views/BorrowHistoryView.vue'),
          meta: { title: '我的借阅记录' },
        },
        {
          path: 'transfer',
          name: 'userTransfer',
          component: () => import('@/views/TransferUserView.vue'),
          meta: { title: '我的调拨与进度' },
        },
        {
          path: 'seat',
          name: 'userSeat',
          component: () => import('@/views/SeatView.vue'),
          meta: { title: '座位预约' },
        },
        {
          path: 'fault',
          name: 'userFault',
          component: () => import('@/views/FaultReportView.vue'),
          meta: { title: '故障报修' },
        },
        { path: '', redirect: { name: 'userHome' } },
      ],
    },
    {
      path: '/a',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'home',
          name: 'adminHome',
          component: () => import('@/views/AdminHomeView.vue'),
          meta: { title: '管理概览' },
        },
        {
          path: 'transfer',
          name: 'adminTransfer',
          component: () => import('@/views/TransferAdminView.vue'),
          meta: { title: '调拨与库存' },
        },
        {
          path: 'fault',
          name: 'adminFault',
          component: () => import('@/views/FaultManageView.vue'),
          meta: { title: '故障工单运维' },
        },
        {
          path: 'equipment',
          name: 'adminEquipment',
          component: () => import('@/views/EquipmentView.vue'),
          meta: { title: '设备状态' },
        },
        {
          path: 'dev-tasks',
          name: 'adminDevTasks',
          component: () => import('@/views/DevTasksView.vue'),
          meta: { title: '定时任务（调试）' },
        },
        { path: '', redirect: { name: 'adminHome' } },
      ],
    },
    { path: '/borrow', redirect: '/u/borrow' },
    { path: '/return', redirect: '/u/return' },
    { path: '/pickup', redirect: '/u/pickup' },
    { path: '/transfer', redirect: '/u/transfer' },
    { path: '/seat', redirect: '/u/seat' },
    { path: '/fault', redirect: '/u/fault' },
    { path: '/equipment', redirect: '/a/equipment' },
    { path: '/dev-tasks', redirect: '/a/dev-tasks' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  auth.hydrate()

  if (to.meta.public) {
    if (auth.isAuthenticated && to.name === 'portal') {
      return { path: auth.portalMode === 'admin' ? '/a/home' : '/u/home' }
    }
    if (auth.isAuthenticated && isPublicLoginName(to.name)) {
      return { path: auth.portalMode === 'admin' ? '/a/home' : '/u/home' }
    }
    return true
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    const wantsAdmin = to.path.startsWith('/a')
    return {
      name: wantsAdmin ? 'loginAdmin' : 'loginUser',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.requiresAuth && auth.isAuthenticated && auth.portalMode) {
    const onAdminPath = to.path.startsWith('/a')
    const onUserPath = to.path.startsWith('/u')
    if (onAdminPath && auth.portalMode !== 'admin') {
      return { path: '/u/home' }
    }
    if (onUserPath && auth.portalMode !== 'user') {
      return { path: '/a/home' }
    }
  }

  return true
})

export default router
