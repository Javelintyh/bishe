<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import type { RoleCode } from '@/utils/auth'

interface RegisterFormData {
  username: string
  password: string
  confirmPassword: string
}

const props = defineProps<{
  registerForm: RegisterFormData
  registerRole: RoleCode
  registerInvitationCode: string
  loading: boolean
}>()

const emit = defineEmits<{
  'update:registerRole': [role: RoleCode]
  'update:registerInvitationCode': [code: string]
  submit: []
  goToLogin: []
}>()
</script>

<template>
  <el-form @submit.prevent class="login-form" autocomplete="off">
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
      <el-radio-group
        :model-value="registerRole"
        class="role-radio-group"
        @update:model-value="emit('update:registerRole', $event as RoleCode)"
      >
        <el-radio-button label="WORKSHOP">车间端</el-radio-button>
        <el-radio-button label="WAREHOUSE">仓库端</el-radio-button>
        <el-radio-button label="ADMIN">管理员端</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item v-if="registerRole === 'ADMIN'">
      <el-input
        :model-value="registerInvitationCode"
        placeholder="请输入管理员邀请码"
        class="login-input"
        @update:model-value="emit('update:registerInvitationCode', $event)"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :loading="loading" class="submit-btn" @click="emit('submit')">注册</el-button>
    </el-form-item>
    <el-form-item class="register-tip">
      <span class="example-link">
        已有账号？
        <a class="link" @click="emit('goToLogin')">返回登录</a>
      </span>
    </el-form-item>
  </el-form>
</template>

<style scoped>
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
