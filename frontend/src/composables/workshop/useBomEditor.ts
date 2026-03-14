/**
 * BOM 编辑器组合式函数
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as baseApi from '@/api/base'
import type { BomEditLine } from '@/types/workshop'

interface UseBomEditorOptions {
  loadData: () => Promise<void>
  getBomMap: () => Record<number, baseApi.BomLine[]>
}

/**
 * Notes:
 * - BOM 配方编辑逻辑封装
 *
 * Args:
 * - options (UseBomEditorOptions): 依赖项
 *
 * Returns:
 * - selectedProductId: 选中的成品ID
 * - bomLines: BOM 编辑行
 * - bomSubmitting: 提交状态
 * - onSelectProduct: 选择成品回调
 * - addBomLine: 添加BOM行
 * - removeBomLine: 删除BOM行
 * - saveBom: 保存BOM配方
 */
export function useBomEditor(options: UseBomEditorOptions) {
  const { loadData, getBomMap } = options

  const selectedProductId = ref<number | null>(null)
  const bomLines = ref<BomEditLine[]>([])
  const bomSubmitting = ref(false)

  function onSelectProduct(productId: number) {
    selectedProductId.value = productId
    const existingBom = getBomMap()[productId] ?? []
    if (existingBom.length > 0) {
      bomLines.value = existingBom.map((line) => ({
        materialId: line.materialId,
        qty: line.qty,
      }))
    } else {
      bomLines.value = [{ materialId: null, qty: 1 }]
    }
  }

  function addBomLine() {
    bomLines.value.push({ materialId: null, qty: 1 })
  }

  function removeBomLine(index: number) {
    bomLines.value.splice(index, 1)
  }

  async function saveBom() {
    if (!selectedProductId.value) {
      ElMessage.warning('请选择成品')
      return
    }
    const validLines = bomLines.value.filter(
      (line) => line.materialId !== null && line.qty > 0,
    )
    if (validLines.length === 0) {
      ElMessage.warning('请至少添加一条BOM配方')
      return
    }

    bomSubmitting.value = true
    try {
      await baseApi.bulkSetBom({
        productMaterialId: selectedProductId.value,
        lines: validLines.map((line) => ({
          materialId: line.materialId!,
          qty: line.qty,
        })),
      })
      ElMessage.success('BOM配方保存成功')
      await loadData()
    } catch (e: any) {
      ElMessage.error(e.response?.data?.message || '保存失败')
    } finally {
      bomSubmitting.value = false
    }
  }

  function resetBomEditor() {
    selectedProductId.value = null
    bomLines.value = []
  }

  return {
    selectedProductId,
    bomLines,
    bomSubmitting,
    onSelectProduct,
    addBomLine,
    removeBomLine,
    saveBom,
    resetBomEditor,
  }
}
