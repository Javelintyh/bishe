/**
 * 登录逻辑组合式函数
 */
import { ref, type Ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { defaultHomeByRole, getRole, type RoleCode } from '@/utils/auth'

interface LoginForm {
  username: string
  password: string
}

interface UseLoginOptions {
  requiredRole: Ref<RoleCode | undefined>
  roleLabel: Ref<string>
  onAccountNotFound: (username: string) => void
}

/**
 * Notes:
 * - 登录逻辑封装
 *
 * Args:
 * - options: 依赖项
 *
 * Returns:
 * - form: 登录表单
 * - loading: 加载状态
 * - onLogin: 登录处理
 * - switchRole: 切换角色端
 * - applyExample: 应用示例账号
 */
export function useLogin(options: UseLoginOptions) {
  const { requiredRole, roleLabel, onAccountNotFound } = options

  const auth = useAuthStore()
  const router = useRouter()
  const route = useRoute()

  const loading = ref(false)
  const form = ref<LoginForm>({
    username: '',
    password: '',
  })

  function switchRole(role: RoleCode) {
    if (role === 'ADMIN') {
      router.push('/login/admin')
    } else if (role === 'WORKSHOP') {
      router.push('/login/workshop')
    } else if (role === 'WAREHOUSE') {
      router.push('/login/warehouse')
    }
  }

  function applyExample(role: RoleCode) {
    if (role === 'ADMIN') {
      switchRole('ADMIN')
      form.value.username = 'admin'
      form.value.password = 'admin123'
    } else if (role === 'WORKSHOP') {
      switchRole('WORKSHOP')
      form.value.username = 'workshop1'
      form.value.password = 'workshop123'
    } else if (role === 'WAREHOUSE') {
      switchRole('WAREHOUSE')
      form.value.username = 'warehouse1'
      form.value.password = 'warehouse123'
    }
  }

  async function onLogin() {
    loading.value = true
    try {
      await auth.login(form.value.username, form.value.password)
      const actualRole = auth.roleCode ?? getRole()
      if (requiredRole.value && actualRole !== requiredRole.value) {
        auth.logout()
        throw new Error(`当前账号不是${roleLabel.value}账号`)
      }
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string | undefined) ?? undefined
      await router.replace(redirect || defaultHomeByRole(actualRole ?? null))
    } catch (e: any) {
      const code = e?.code
      const message = e?.message as string | undefined
      if (code === 404) {
        ElMessage.error('账号不存在，已为你跳转到注册')
        onAccountNotFound(form.value.username)
        return
      }
      if (code === 400) {
        ElMessage.error('密码错误')
        return
      }
      ElMessage.error(message || '登录失败')
    } finally {
      loading.value = false
    }
  }

  return {
    form,
    loading,
    onLogin,
    switchRole,
    applyExample,
  }
}
