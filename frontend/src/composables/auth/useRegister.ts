/**
 * 注册逻辑组合式函数
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import type { RoleCode } from '@/utils/auth'

interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
}

interface UseRegisterOptions {
  onRegisterSuccess: (username: string, password: string, role: RoleCode) => void
}

/**
 * Notes:
 * - 注册逻辑封装
 *
 * Args:
 * - options: 依赖项
 *
 * Returns:
 * - registerForm: 注册表单
 * - registerRole: 注册角色
 * - registerInvitationCode: 邀请码
 * - loading: 加载状态
 * - onRegister: 注册处理
 * - resetRegisterForm: 重置表单
 */
export function useRegister(options: UseRegisterOptions) {
  const { onRegisterSuccess } = options

  const loading = ref(false)
  const registerForm = ref<RegisterForm>({
    username: '',
    password: '',
    confirmPassword: '',
  })
  const registerRole = ref<RoleCode>('WORKSHOP')
  const registerInvitationCode = ref('')

  function resetRegisterForm() {
    registerForm.value.username = ''
    registerForm.value.password = ''
    registerForm.value.confirmPassword = ''
    registerInvitationCode.value = ''
  }

  function setUsername(username: string) {
    registerForm.value.username = username
    registerForm.value.password = ''
    registerForm.value.confirmPassword = ''
  }

  async function onRegister() {
    if (!registerForm.value.username || !registerForm.value.password) {
      ElMessage.error('请输入用户名和密码')
      return
    }
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    if (registerRole.value === 'ADMIN' && !registerInvitationCode.value) {
      ElMessage.error('注册管理员账号需要邀请码')
      return
    }
    loading.value = true
    try {
      await register({
        username: registerForm.value.username,
        password: registerForm.value.password,
        roleCode: registerRole.value,
        invitationCode: registerRole.value === 'ADMIN' ? registerInvitationCode.value : undefined,
      })
      const roleText =
        registerRole.value === 'ADMIN' ? '管理员端' : registerRole.value === 'WAREHOUSE' ? '仓库端' : '车间端'
      ElMessage.success(`注册成功，请使用新账号登录（${roleText}）`)
      onRegisterSuccess(registerForm.value.username, registerForm.value.password, registerRole.value)
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '注册失败')
    } finally {
      loading.value = false
    }
  }

  return {
    registerForm,
    registerRole,
    registerInvitationCode,
    loading,
    onRegister,
    resetRegisterForm,
    setUsername,
  }
}
