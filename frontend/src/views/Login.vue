<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { defaultHomeByRole, getRole, type RoleCode } from '@/utils/auth'
import { register } from '@/api/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const requiredRole = computed(() => route.meta.loginRole as RoleCode | undefined)

const roleLabel = computed(() => {
  if (requiredRole.value === 'ADMIN') return '管理端'
  if (requiredRole.value === 'WORKSHOP') return '车间端'
  if (requiredRole.value === 'WAREHOUSE') return '仓库端'
  return '系统'
})

const loading = ref(false)
const mode = ref<'login' | 'register'>('login')
const form = ref({
  username: '',
  password: '',
})
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
})
const registerRole = ref<RoleCode>('WORKSHOP')
const registerInvitationCode = ref('')

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
  mode.value = 'login'
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

async function onSubmit() {
  if (mode.value === 'register') {
    await onRegister()
    return
  }
  loading.value = true
  try {
    const resp = await auth.login(form.value.username, form.value.password)
    const actualRole = auth.roleCode ?? getRole()
    if (requiredRole.value && actualRole !== requiredRole.value) {
      auth.logout()
      throw new Error(`当前账号不是${roleLabel.value}账号`)
    }
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string | undefined) ?? undefined
    await router.replace(redirect || defaultHomeByRole(actualRole ?? null))
  } catch (e: any) {
    // 来自 auth.store 的错误为完整响应对象
    const code = e?.code
    const message = e?.message as string | undefined
    if (code === 404) {
      ElMessage.error('账号不存在，已为你跳转到注册')
      mode.value = 'register'
      registerForm.value.username = form.value.username
      registerForm.value.password = ''
      registerForm.value.confirmPassword = ''
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
    // 注册成功后切换到对应端登录，并自动填入刚注册的账号密码
    switchRole(registerRole.value)
    form.value.username = registerForm.value.username
    form.value.password = registerForm.value.password
    mode.value = 'login'
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-shell">
      <div class="cover">
        <div class="cover-badge" />
      </div>

      <div class="panel">
        <h1 class="title">轻量级生产管理平台</h1>
        <p class="subtitle">
          欢迎{{ mode === 'login' ? '登录' : '注册' }}{{ roleLabel }}
        </p>

        <div v-if="mode === 'login'" class="role-switch">
          <el-button
            class="role-btn"
            :type="requiredRole === 'WORKSHOP' ? 'primary' : 'default'"
            size="small"
            @click="switchRole('WORKSHOP')"
          >
            车间端
          </el-button>
          <el-button
            class="role-btn"
            :type="requiredRole === 'WAREHOUSE' ? 'primary' : 'default'"
            size="small"
            @click="switchRole('WAREHOUSE')"
          >
            仓库端
          </el-button>
          <el-button
            class="role-btn"
            :type="requiredRole === 'ADMIN' ? 'primary' : 'default'"
            size="small"
            @click="switchRole('ADMIN')"
          >
            管理端
          </el-button>
        </div>

        <el-form v-if="mode === 'login'" @submit.prevent class="login-form" autocomplete="off">
          <el-form-item>
            <el-input
              v-model="form.username"
              autocomplete="off"
              placeholder="请输入用户名"
              :prefix-icon="User"
              class="login-input"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              autocomplete="new-password"
              show-password
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              class="login-input"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="onSubmit">登录</el-button>
          </el-form-item>
          <el-form-item class="register-tip">
            <span class="example-link">
              没有账号？
              <a class="link" @click="mode = 'register'">立即注册账号</a>
            </span>
          </el-form-item>
        </el-form>

        <el-form v-else @submit.prevent class="login-form" autocomplete="off">
          <el-form-item>
            <el-input
              v-model="registerForm.username"
              autocomplete="off"
              placeholder="请输入用户名（用于登录）"
              :prefix-icon="User"
              class="login-input"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="registerForm.password"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
              class="login-input"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              class="login-input"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item>
            <el-radio-group v-model="registerRole" class="role-radio-group">
              <el-radio-button label="WORKSHOP">车间端</el-radio-button>
              <el-radio-button label="WAREHOUSE">仓库端</el-radio-button>
              <el-radio-button label="ADMIN">管理员端</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="registerRole === 'ADMIN'">
            <el-input
              v-model="registerInvitationCode"
              placeholder="请输入管理员邀请码"
              class="login-input"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="onSubmit">注册</el-button>
          </el-form-item>
          <el-form-item class="register-tip">
            <span class="example-link">
              已有账号？
              <a class="link" @click="mode = 'login'">返回登录</a>
            </span>
          </el-form-item>
        </el-form>

        <div class="example-block">
          <div class="example-title">示例账号</div>
          <div class="example-line">
            管理员：admin / admin123
            <el-button link type="primary" size="small" @click="applyExample('ADMIN')">一键填入</el-button>
          </div>
          <div class="example-line">
            车间端：workshop1 / workshop123
            <el-button link type="primary" size="small" @click="applyExample('WORKSHOP')">一键填入</el-button>
          </div>
          <div class="example-line">
            仓库端：warehouse1 / warehouse123
            <el-button link type="primary" size="small" @click="applyExample('WAREHOUSE')">一键填入</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px 12px;
  background: linear-gradient(180deg, #eaf2ff 0%, #f3f7ff 100%);
}

.login-shell {
  width: 100%;
  max-width: 420px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 16px 38px rgba(87, 111, 192, 0.22);
  background: #fff;
}

.cover {
  position: relative;
  height: 160px;
  background: linear-gradient(135deg, #5c8bff 0%, #7d4dff 100%);
}

.cover::before,
.cover::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
}

.cover::before {
  width: 14px;
  height: 14px;
  right: 24px;
  top: 26px;
}

.cover::after {
  width: 10px;
  height: 10px;
  right: 56px;
  top: 58px;
}

.cover-badge {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 68px;
  height: 68px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle at 34% 34%, #76d5ff 0%, #2988ff 55%, #2f63ff 100%);
  box-shadow: 0 8px 18px rgba(13, 55, 194, 0.35);
}

.panel {
  padding: 26px 22px 20px;
}

.title {
  margin: 0;
  text-align: center;
  font-size: 30px;
  font-weight: 700;
  color: #1f2430;
}

.subtitle {
  margin: 6px 0 14px;
  text-align: center;
  color: #8a93a7;
  font-size: 14px;
}

.role-switch {
  display: flex;
  gap: 6px;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 16px;
  background: #f5f7ff;
}

.role-btn {
  flex: 1;
  margin-left: 0 !important;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.submit-btn {
  width: 100%;
  height: 40px;
  border: 0;
  border-radius: 10px;
  font-size: 15px;
  background: linear-gradient(90deg, #4f8dff 0%, #7b52ff 100%);
}

.example-block {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid #eff2f8;
}

.example-title {
  margin-bottom: 6px;
  color: #6d7487;
  font-weight: 600;
}

.example-line {
  color: #98a0b2;
  font-size: 12px;
  line-height: 1.6;
}

.register-tip {
  margin-top: -4px;
}

.example-link {
  font-size: 12px;
  color: #8a93a7;
}

.example-link .link {
  color: #4f8dff;
  cursor: pointer;
}

.role-radio-group {
  width: 100%;
  display: flex;
  justify-content: space-between;
}
</style>

