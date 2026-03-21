import type { Ref } from 'vue'
import type { TabsPaneContext } from 'element-plus'
import type { OrderVO } from '@/api/order'
import type { ProductionWorkOrder } from '@/api/production'
import type { InventoryStock } from '@/api/inventory'

type UseAdminPageActionsOptions = {
  refreshCurrentTab: (tabName: string) => Promise<void>
  onExportReport: (orders: OrderVO[], workOrders: ProductionWorkOrder[], stocks: InventoryStock[]) => void
  state: {
    orders: OrderVO[]
    workOrders: ProductionWorkOrder[]
    stocks: InventoryStock[]
  }
  loadAllData: () => Promise<void>
}

export function useAdminPageActions(options: UseAdminPageActionsOptions) {
  const { refreshCurrentTab, onExportReport, state, loadAllData } = options

  function handleTabClick(pane: TabsPaneContext) {
    if (typeof pane.paneName === 'string') {
      refreshCurrentTab(pane.paneName)
    }
  }

  function handleExportReport() {
    onExportReport(state.orders, state.workOrders, state.stocks)
  }

  async function initPage() {
    await loadAllData()
  }

  return {
    handleTabClick,
    handleExportReport,
    initPage,
  }
}

