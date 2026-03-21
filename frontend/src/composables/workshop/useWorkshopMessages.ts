import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as messageApi from '@/api/message'

export function useWorkshopMessages() {
  const workshopMessages = ref<messageApi.MessageNotice[]>([])
  const messagesLoading = ref(false)
  const workshopMessageCount = computed(() => workshopMessages.value.length)

  async function loadWorkshopMessages() {
    messagesLoading.value = true
    try {
      const resp = await messageApi.listMessages(true)
      if (resp.code !== 0) {
        ElMessage.error(resp.message || '加载消息失败')
        return
      }
      workshopMessages.value = (resp.data ?? []).filter((m) => m.noticeType === 'WS_PRODUCE_URGE')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '加载消息失败')
    } finally {
      messagesLoading.value = false
    }
  }

  async function handleWorkshopMessage(msg: messageApi.MessageNotice) {
    try {
      await messageApi.markRead(msg.id)
      ElMessage.success('已知晓催促信息')
      await loadWorkshopMessages()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
    }
  }

  return {
    workshopMessages,
    messagesLoading,
    workshopMessageCount,
    loadWorkshopMessages,
    handleWorkshopMessage,
  }
}

