/**
 * 分页组合式函数
 */
import { computed, ref, type Ref, type ComputedRef } from 'vue'

interface UsePaginationOptions<T> {
  /** 数据源（可为数组或计算属性） */
  data: Ref<T[]> | ComputedRef<T[]>
  /** 每页大小 */
  pageSize?: number
}

interface UsePaginationReturn<T> {
  /** 当前页码（从1开始） */
  currentPage: Ref<number>
  /** 分页后的数据 */
  pagedData: ComputedRef<T[]>
  /** 总条数 */
  total: ComputedRef<number>
  /** 是否需要显示分页器 */
  showPagination: ComputedRef<boolean>
  /** 重置到第一页 */
  resetPage: () => void
}

/**
 * Notes:
 * - 通用分页逻辑封装
 *
 * Args:
 * - options (UsePaginationOptions<T>): 分页配置
 *   - data: 数据源
 *   - pageSize: 每页大小，默认5
 *
 * Returns:
 * - (UsePaginationReturn<T>)
 *   - currentPage: 当前页码
 *   - pagedData: 分页后数据
 *   - total: 总条数
 *   - showPagination: 是否显示分页
 *   - resetPage: 重置页码
 */
export function usePagination<T>(options: UsePaginationOptions<T>): UsePaginationReturn<T> {
  const { data, pageSize = 5 } = options

  const currentPage = ref(1)

  const total = computed(() => data.value.length)

  const showPagination = computed(() => total.value > pageSize)

  const pagedData = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    return data.value.slice(start, start + pageSize)
  })

  const resetPage = () => {
    currentPage.value = 1
  }

  return {
    currentPage,
    pagedData,
    total,
    showPagination,
    resetPage,
  }
}
