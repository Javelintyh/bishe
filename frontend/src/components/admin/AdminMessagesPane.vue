<script setup lang="ts">
import { computed, ref } from 'vue'
import type * as messageApi from '@/api/message'
import { levelLabel, noticeTypeLabel } from '@/constants/admin'

const PAGE_SIZE = 5
const currentPage = ref(1)

const props = defineProps<{
  messages: messageApi.MessageNotice[]
  onScanMessages?: () => void
  onMarkMessageRead?: (msg: messageApi.MessageNotice) => void
  onPurchaseInbound?: (msg: messageApi.MessageNotice) => void
  onCreatePurchaseFromRequest?: (msg: messageApi.MessageNotice) => void
  onCreatePurchaseFromStockLow?: (msg: messageApi.MessageNotice) => void
  stockLowActionLabel?: (msg: messageApi.MessageNotice) => string
  onWorkOrderRemind?: (msg: messageApi.MessageNotice) => void
}>()

const pagedMessages = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return props.messages.slice(start, start + PAGE_SIZE)
})
</script>

<template>
  <div>
    <div class="action-bar">
      <el-button
        v-if="props.onScanMessages"
        size="small"
        type="primary"
        @click="props.onScanMessages && props.onScanMessages()"
      >
        扫描超期/库存预警
      </el-button>
    </div>
    <el-table :data="pagedMessages" style="width: 100%" size="small">
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          {{ noticeTypeLabel(row.noticeType) }}
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" />
      <el-table-column label="对象" width="220">
        <template #default="{ row }">
          <span v-if="row.noticeType === 'STOCK_LOW'">
            {{ (row.content || '').split('\n')[0] || row.relatedId }}
          </span>
          <span v-else-if="row.noticeType === 'WO_OVERDUE'">
            {{ (row.content || '').split('\n')[0] || row.relatedId }}
          </span>
          <span v-else-if="row.noticeType === 'WO_DUE_SOON'">
            {{ (row.content || '').split('\n')[0] || row.relatedId }}
          </span>
          <span v-else-if="row.noticeType === 'CAPACITY_WARN_PRODUCT'">
            成品总仓
          </span>
          <span v-else-if="row.noticeType === 'CAPACITY_WARN_RAW'">
            {{ (row.content || '').split('\n')[0] || row.relatedId }}
          </span>
          <span v-else-if="row.noticeType === 'PURCHASE_ARRIVED'">
            采购单 {{ row.relatedId }}
          </span>
          <span v-else-if="row.noticeType === 'PURCHASE_REQUEST'">
            物料 {{ row.relatedId }}
          </span>
          <span v-else>
            {{ row.relatedId }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="详情">
        <template #default="{ row }">
          <div v-for="(line, idx) in (row.content || '').split('\n').slice(1)" :key="idx">
            {{ line }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="90">
        <template #default="{ row }">
          {{ levelLabel(row.level) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column v-if="props.onMarkMessageRead" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.read ? 'info' : 'danger'">{{ row.read ? '已读' : '未读' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-if="row.noticeType === 'PURCHASE_ARRIVED' && props.onPurchaseInbound"
            link
            type="primary"
            size="small"
            @click="props.onPurchaseInbound(row)"
          >
            入库
          </el-button>
          <el-button
            v-else-if="row.noticeType === 'PURCHASE_REQUEST' && props.onCreatePurchaseFromRequest"
            link
            type="primary"
            size="small"
            @click="props.onCreatePurchaseFromRequest(row)"
          >
            创建采购单
          </el-button>
          <el-button
            v-else-if="row.noticeType === 'STOCK_LOW' && props.onCreatePurchaseFromStockLow"
            link
            type="primary"
            size="small"
            :disabled="row.read"
            @click="props.onCreatePurchaseFromStockLow(row)"
          >
            {{ props.stockLowActionLabel ? props.stockLowActionLabel(row) : '处理预警' }}
          </el-button>
          <el-button
            v-else-if="(row.noticeType === 'WO_DUE_SOON' || row.noticeType === 'WO_OVERDUE') && row.relatedType === 'WORK_ORDER' && props.onWorkOrderRemind"
            link
            type="primary"
            size="small"
            @click="props.onWorkOrderRemind(row)"
          >
            提醒仓库
          </el-button>
          <el-button
            v-if="!row.read && props.onMarkMessageRead"
            link
            type="primary"
            size="small"
            @click="props.onMarkMessageRead(row)"
          >
            设为已读
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="props.messages.length > PAGE_SIZE"
      v-model:current-page="currentPage"
      :page-size="PAGE_SIZE"
      :total="props.messages.length"
      layout="prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
    />
  </div>
</template>

