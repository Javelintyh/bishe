export function useWarehousePageActions(options: {
  loadData: () => Promise<void>
  popupCapacityWarningsAndMaybeJump: () => Promise<void>
  incrementMessageRefresh: () => void
}) {
  const { loadData, popupCapacityWarningsAndMaybeJump, incrementMessageRefresh } = options

  async function refreshAll() {
    await loadData()
    incrementMessageRefresh()
    await popupCapacityWarningsAndMaybeJump()
  }

  async function initPage() {
    await loadData()
    await popupCapacityWarningsAndMaybeJump()
  }

  return {
    refreshAll,
    initPage,
  }
}

