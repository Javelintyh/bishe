/**
 * 报工表单组合式函数
 */
import { computed, reactive, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as productionApi from '@/api/production'
import type { ReportForm } from '@/types/workshop'

interface UseReportFormOptions {
  loadData: () => Promise<void>
  getWorkOrders: () => productionApi.ProductionWorkOrder[]
  getMaterialName: (id: number) => string
  activeTab: Ref<string>
}

/**
 * Notes:
 * - 报工表单逻辑封装
 *
 * Args:
 * - options (UseReportFormOptions): 依赖项
 *
 * Returns:
 * - reportForm: 报工表单数据
 * - selectedProductName: 选中产品名称
 * - onSelectReportWorkOrder: 选择工单回调
 * - goToReport: 跳转到报工页面
 * - onReport: 提交报工
 * - resetReportForm: 重置表单
 */
export function useReportForm(options: UseReportFormOptions) {
  const { loadData, getWorkOrders, getMaterialName, activeTab } = options

  const reportForm = reactive<ReportForm>({
    workOrderNo: '',
    processName: '',
    goodQty: 1,
    badQty: 0,
    badReasonCode: '',
    badReasonText: '',
  })

  const selectedProductName = computed(() => {
    const wo = getWorkOrders().find((w) => w.workOrderNo === reportForm.workOrderNo)
    if (!wo) return ''
    return getMaterialName(wo.productMaterialId)
  })

  function resetReportForm() {
    reportForm.workOrderNo = ''
    reportForm.processName = ''
    reportForm.goodQty = 1
    reportForm.badQty = 0
    reportForm.badReasonCode = ''
    reportForm.badReasonText = ''
  }

  function onSelectReportWorkOrder(workOrderNo: string) {
    const wo = getWorkOrders().find((w) => w.workOrderNo === workOrderNo)
    if (wo) {
      reportForm.goodQty = wo.qty
      reportForm.processName = getMaterialName(wo.productMaterialId)
    }
  }

  function goToReport(wo: productionApi.ProductionWorkOrder) {
    activeTab.value = 'report'
    reportForm.workOrderNo = wo.workOrderNo
    reportForm.processName = getMaterialName(wo.productMaterialId)
    reportForm.goodQty = wo.qty
  }

  async function onReport() {
    if (!reportForm.workOrderNo) {
      ElMessage.warning('请选择工单')
      return
    }
    try {
      await productionApi.createReport({
        workOrderNo: reportForm.workOrderNo,
        processName: reportForm.processName,
        goodQty: reportForm.goodQty,
        badQty: reportForm.badQty,
        badReasonCode: reportForm.badReasonCode || undefined,
        badReasonText: reportForm.badReasonText || undefined,
      })

      const wo = getWorkOrders().find((w) => w.workOrderNo === reportForm.workOrderNo)
      if (wo) {
        await productionApi.updateWorkOrderStatus(wo.id, 'DONE')
      }

      ElMessage.success('报工成功')
      resetReportForm()
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '报工失败')
    }
  }

  return {
    reportForm,
    selectedProductName,
    onSelectReportWorkOrder,
    goToReport,
    onReport,
    resetReportForm,
  }
}
