/**
 * 车间端常量配置
 */
import type { BadReason } from '@/types/workshop'

// 导出通用常量
export * from '../common'

/** 默认不良原因列表 */
export const DEFAULT_BAD_REASONS: BadReason[] = [
  { code: 'MATERIAL', text: '原材料问题' },
  { code: 'EQUIPMENT', text: '设备故障' },
  { code: 'HUMAN', text: '人为失误' },
  { code: 'OTHER', text: '其他原因' },
]

/** 看板饼图颜色配置 */
export const KANBAN_CHART_COLORS = ['#ff9800', '#4caf50', '#2196f3', '#9c27b0', '#67c23a']
