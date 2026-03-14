/**
 * 用户操作组合式函数
 */
import { ElMessage, ElMessageBox } from 'element-plus'
import * as sysApi from '@/api/sys'

interface UseUserOperationsOptions {
  loadUsers: () => Promise<void>
}

/**
 * Notes:
 * - 用户管理操作逻辑封装
 *
 * Args:
 * - options: 依赖项
 *
 * Returns:
 * - onToggleUserEnabled: 切换用户启用状态
 * - onResetPassword: 重置用户密码
 * - onDeleteUser: 删除用户
 */
export function useUserOperations(options: UseUserOperationsOptions) {
  const { loadUsers } = options

  async function onToggleUserEnabled(row: sysApi.UserVO) {
    try {
      const nextEnabled = !row.enabled
      await sysApi.updateUser(row.id, { roleCode: row.roleCode, enabled: nextEnabled })
      row.enabled = nextEnabled
      ElMessage.success('已更新用户状态')
    } catch (e: any) {
      ElMessage.error(e?.message || '更新失败')
    }
  }

  async function onResetPassword(row: sysApi.UserVO) {
    try {
      await ElMessageBox.confirm(`确定将用户 ${row.username} 的密码重置为 123456 吗？`, '提示', {
        type: 'warning',
      })
      await sysApi.resetPassword(row.id)
      ElMessage.success('已重置密码为 123456')
    } catch {
      // ignore cancel
    }
  }

  async function onDeleteUser(row: sysApi.UserVO) {
    try {
      await ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？此操作不可恢复！`, '警告', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
      })
      await sysApi.deleteUser(row.id)
      ElMessage.success('已删除用户')
      await loadUsers()
    } catch {
      // ignore cancel
    }
  }

  return {
    onToggleUserEnabled,
    onResetPassword,
    onDeleteUser,
  }
}
