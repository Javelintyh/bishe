export function useWorkshopPageActions(options: {
  loadData: () => Promise<void> | void
  loadWorkshopMessages: () => Promise<void> | void
}) {
  const { loadData, loadWorkshopMessages } = options

  async function refreshAll() {
    await loadData()
    await loadWorkshopMessages()
  }

  async function initPage() {
    await refreshAll()
  }

  return {
    refreshAll,
    initPage,
  }
}

