<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { defaultHomeByRole, getRole } from '@/utils/auth'
import { getCurrentInvitation, updateInvitation } from '@/api/adminInvitation'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

const role = computed(() => auth.roleCode ?? getRole())
const home = computed(() => defaultHomeByRole(role.value ?? null))
const roleName = computed(() => {
  if (role.value === 'WORKSHOP') return '车间端'
  if (role.value === 'WAREHOUSE') return '仓库端'
  return '管理端'
})
const greetingText = computed(() => `您好，${roleName.value} ${auth.username ?? ''}`)
const headerThemeClass = computed(() => {
  if (role.value === 'WORKSHOP') return 'main-header--workshop'
  if (role.value === 'WAREHOUSE') return 'main-header--warehouse'
  return 'main-header--admin'
})

const currentInvitation = ref<{ code: string; used: boolean } | null>(null)

async function loadInvitation() {
  if (auth.username === 'admin' && role.value === 'ADMIN') {
    try {
      const resp = await getCurrentInvitation()
      currentInvitation.value = resp.data ?? null
    } catch (e: any) {
      ElMessage.error(e?.message || '加载邀请码失败')
    }
  } else {
    currentInvitation.value = null
  }
}

async function onUpdateInvitation() {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的管理员邀请码', '更新邀请码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '邀请码不能为空',
    })
    const resp = await updateInvitation(value)
    currentInvitation.value = resp.data
    ElMessage.success('邀请码已更新')
  } catch {
    // ignore
  }
}

function onLogout() {
  auth.logout()
  if (role.value === 'WORKSHOP') {
    router.push('/login/workshop')
  } else if (role.value === 'WAREHOUSE') {
    router.push('/login/warehouse')
  } else {
    router.push('/login/admin')
  }
}

onMounted(() => {
  loadInvitation()
})

watch(
  () => [auth.username, role.value],
  () => {
    loadInvitation()
  },
)
</script>

<template>
  <el-container class="main-layout">
    <el-header class="main-header" :class="headerThemeClass">
      <div class="brand">
        <div class="brand-dot" />
        <span>轻量级生产管理平台</span>
      </div>
      <div class="header-right">
        <span v-if="auth.username === 'admin' && role === 'ADMIN' && currentInvitation" class="invite-tag">
          管理员邀请码：{{ currentInvitation.code }}
          <span v-if="currentInvitation.used" class="invite-status">(已使用，需更新)</span>
          <span v-else class="invite-status">(可用)</span>
          <a class="invite-link" @click="onUpdateInvitation">更新</a>
        </span>
        <span v-if="auth.username" class="user-text">{{ greetingText }}</span>
        <el-button size="small" class="logout-btn" @click="onLogout">退出登录</el-button>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.main-layout {
  height: 100vh;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 62px;
  padding: 0 22px;
}

.main-header--admin {
  background: linear-gradient(90deg, #4f8dff 0%, #6a65ff 55%, #7a52ff 100%);
  box-shadow: 0 10px 24px rgba(66, 103, 214, 0.28);
}

.main-header--workshop {
  background: linear-gradient(90deg, #27a6c2 0%, #2c90d8 52%, #4478e8 100%);
  box-shadow: 0 10px 24px rgba(47, 138, 165, 0.26);
}

.main-header--warehouse {
  background: linear-gradient(90deg, #f2aa4e 0%, #e78f39 50%, #d7772f 100%);
  box-shadow: 0 10px 24px rgba(173, 116, 50, 0.28);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: #fff;
}

.brand-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0 6px rgba(255, 255, 255, 0.2);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.invite-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.96);
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  gap: 6px;
}

.invite-status {
  opacity: 0.9;
}

.invite-link {
  text-decoration: underline;
  cursor: pointer;
}

.user-text {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: rgba(255, 255, 255, 0.96);
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  backdrop-filter: blur(2px);
}

.logout-btn {
  border: 0;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
}

.logout-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.28);
}

.main-content {
  background: linear-gradient(180deg, #eaf2ff 0%, #f3f7ff 100%);
  padding: 16px;
}
</style>

