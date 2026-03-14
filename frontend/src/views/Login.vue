<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { RoleCode } from '@/utils/auth'
import { useLogin, useRegister } from '@/composables/auth'
import { LoginForm, RegisterForm, ExampleAccounts } from '@/components/auth'

const route = useRoute()

const requiredRole = computed(() => route.meta.loginRole as RoleCode | undefined)
const roleLabel = computed(() => {
  if (requiredRole.value === 'ADMIN') return '管理端'
  if (requiredRole.value === 'WORKSHOP') return '车间端'
  if (requiredRole.value === 'WAREHOUSE') return '仓库端'
  return '系统'
})

const mode = ref<'login' | 'register'>('login')

const {
  form,
  loading: loginLoading,
  onLogin,
  switchRole,
  applyExample,
} = useLogin({
  requiredRole,
  roleLabel,
  onAccountNotFound: (username) => {
    mode.value = 'register'
    setUsername(username)
  },
})

const {
  registerForm,
  registerRole,
  registerInvitationCode,
  loading: registerLoading,
  onRegister,
  setUsername,
} = useRegister({
  onRegisterSuccess: (username, password, role) => {
    switchRole(role)
    form.value.username = username
    form.value.password = password
    mode.value = 'login'
  },
})

function onApplyExample(role: RoleCode) {
  mode.value = 'login'
  applyExample(role)
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

        <LoginForm
          v-if="mode === 'login'"
          :form="form"
          :loading="loginLoading"
          :required-role="requiredRole"
          @submit="onLogin"
          @switch-role="switchRole"
          @go-to-register="mode = 'register'"
        />

        <RegisterForm
          v-else
          :register-form="registerForm"
          :register-role="registerRole"
          :register-invitation-code="registerInvitationCode"
          :loading="registerLoading"
          @update:register-role="registerRole = $event"
          @update:register-invitation-code="registerInvitationCode = $event"
          @submit="onRegister"
          @go-to-login="mode = 'login'"
        />

        <ExampleAccounts @apply-example="onApplyExample" />
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
</style>
