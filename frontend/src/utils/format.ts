/**
 * 通用格式化工具函数
 */

/**
 * Notes:
 * - 获取今日日期字符串
 *
 * Returns:
 * - (string) 格式为 YYYY-MM-DD 的日期字符串
 */
export function getTodayStr(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * Notes:
 * - 格式化日期为 YYYY-MM-DD
 *
 * Args:
 * - date (Date | string | number): 日期对象、时间戳或日期字符串
 *
 * Returns:
 * - (string) 格式化后的日期字符串
 */
export function formatDate(date: Date | string | number): string {
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * Notes:
 * - 格式化日期时间为 YYYY-MM-DD HH:mm:ss
 *
 * Args:
 * - date (Date | string | number): 日期对象、时间戳或日期字符串
 *
 * Returns:
 * - (string) 格式化后的日期时间字符串
 */
export function formatDateTime(date: Date | string | number): string {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  const second = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

/**
 * Notes:
 * - 生成采购单号
 *
 * Returns:
 * - (string) 格式为 PO + YYYYMMDDHHmmss 的单号
 */
export function genPurchaseNo(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `PO${y}${m}${day}${h}${mi}${s}`
}
