<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import type { RoleCode } from '@/utils/auth'

interface LoginFormData {
  username: string
  password: string
}

const props = defineProps<{
  form: LoginFormData
  loading: boolean
  requiredRole: RoleCode | undefined
}>()

const emit = defineEmits<{
  submit: []
  switchRole: [role: RoleCode]
  goToRegister: []
}>()
</script>

<template>
  <div>
    <div class="role-switch">
      <el-button
        class="role-btn"
        :type="requiredRole === 'WORKSHOP' ? 'primary' : 'default'"
        size="small"
        @click="emit('switchRole', 'WORKSHOP')"
      >
        车间端
      </el-button>
      <el-button
        class="role-btn"
        :type="requiredRole === 'WAREHOUSE' ? 'primary' : 'default'"
        size="small"
        @click="emit('switchRole', 'WAREHOUSE')"
      >
        仓库端
      </el-button>
      <el-button
        class="role-btn"
        :type="requiredRole === 'ADMIN' ? 'primary' : 'default'"
        size="small"
        @click="emit('switchRole', 'ADMIN')"
      >
        管理端
      </el-button>
    </div>

    <el-form @submit.prevent class="login-form" autocomplete="off">
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
        <el-button type="primary" :loading="loading" class="submit-btn" @click="emit('submit')">登录</el-button>
      </el-form-item>
      <el-form-item class="register-tip">
        <span class="example-link">
          没有账号？
          <a class="link" @click="emit('goToRegister')">立即注册账号</a>
        </span>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
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
</style>
