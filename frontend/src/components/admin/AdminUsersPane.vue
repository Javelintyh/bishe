<script setup lang="ts">
import { computed, ref } from 'vue'
import type * as sysApi from '@/api/sys'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  users: sysApi.UserVO[]
  onToggleUserEnabled: (user: sysApi.UserVO) => void
  onResetPassword: (user: sysApi.UserVO) => void
  onDeleteUser: (user: sysApi.UserVO) => void
  currentUsername: string | null
}>()

const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.users.slice(start, start + PAGE_SIZE)
})
</script>

<template>
  <div>
    <p v-if="props.currentUsername !== 'admin'" style="margin-bottom: 8px; font-size: 12px; color: #909399">
      说明：普通管理员仅可管理车间端和仓库端账号，高级管理员账号由主管理员统一维护。
    </p>
    <el-table :data="pagedUsers" style="width: 100%" size="small">
      <el-table-column prop="username" label="用户名" />
      <el-table-column label="角色">
        <template #default="{ row }">
          {{
            row.roleCode === 'ADMIN'
              ? '管理员'
              : row.roleCode === 'WORKSHOP'
                ? '车间员'
                : row.roleCode === 'WAREHOUSE'
                  ? '仓管员'
                  : row.roleCode
          }}
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="启用">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            :disabled="row.username === 'admin'"
            @change="() => props.onToggleUserEnabled(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <template v-if="row.username !== 'admin'">
            <el-button
              link
              type="primary"
              size="small"
              @click="props.onResetPassword(row)"
            >
              重置密码
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              @click="props.onDeleteUser(row)"
            >
              删除
            </el-button>
          </template>
          <span v-else style="font-size: 12px; color: #909399">主管理员账号由系统保护</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="props.users.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.users.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </div>
</template>

