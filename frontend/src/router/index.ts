import { createRouter, createWebHistory } from 'vue-router'
import { defaultHomeByRole, getRole, getToken, type RoleCode } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      redirect: '/login/admin',
    },
    {
      path: '/login/admin',
      name: 'login-admin',
      meta: { loginRole: 'ADMIN' satisfies RoleCode },
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/login/workshop',
      name: 'login-workshop',
      meta: { loginRole: 'WORKSHOP' satisfies RoleCode },
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/login/warehouse',
      name: 'login-warehouse',
      meta: { loginRole: 'WAREHOUSE' satisfies RoleCode },
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        {
          path: '',
          redirect: () => defaultHomeByRole(getRole()),
        },
        {
          path: 'admin',
          name: 'admin',
          meta: { roles: ['ADMIN'] satisfies RoleCode[] },
          component: () => import('@/views/admin/AdminDashboard.vue'),
        },
        {
          path: 'workshop',
          name: 'workshop',
          meta: { roles: ['WORKSHOP'] satisfies RoleCode[] },
          component: () => import('@/views/workshop/WorkshopTodo.vue'),
        },
        {
          path: 'warehouse',
          name: 'warehouse',
          meta: { roles: ['WAREHOUSE'] satisfies RoleCode[] },
          component: () => import('@/views/warehouse/WarehouseDashboard.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = getToken()

  // 未登录：只允许访问各自登录页
  if (!token) {
    if (to.path.startsWith('/login')) {
      return
    }
    if (to.path.startsWith('/admin')) {
      return { path: '/login/admin', query: { redirect: to.fullPath } }
    }
    if (to.path.startsWith('/workshop')) {
      return { path: '/login/workshop', query: { redirect: to.fullPath } }
    }
    if (to.path.startsWith('/warehouse')) {
      return { path: '/login/warehouse', query: { redirect: to.fullPath } }
    }
    return { path: '/login/admin', query: { redirect: to.fullPath } }
  }

  // 已登录访问登录页：直接按角色跳转各自首页
  if (to.path.startsWith('/login')) {
    return defaultHomeByRole(getRole())
  }

  const roles = (to.meta.roles as RoleCode[] | undefined) ?? undefined
  if (roles && roles.length > 0) {
    const role = getRole()
    if (!role || !roles.includes(role)) {
      return defaultHomeByRole(role)
    }
  }
})

export default router
