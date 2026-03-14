<script setup lang="ts">
import type { RoleCode } from '@/utils/auth'

const emit = defineEmits<{
  applyExample: [role: RoleCode]
}>()
</script>

<template>
  <div class="example-block">
    <div class="example-title">示例账号</div>
    <div class="example-line">
      管理员：admin / admin123
      <el-button link type="primary" size="small" @click="emit('applyExample', 'ADMIN')">一键填入</el-button>
    </div>
    <div class="example-line">
      车间端：workshop1 / workshop123
      <el-button link type="primary" size="small" @click="emit('applyExample', 'WORKSHOP')">一键填入</el-button>
    </div>
    <div class="example-line">
      仓库端：warehouse1 / warehouse123
      <el-button link type="primary" size="small" @click="emit('applyExample', 'WAREHOUSE')">一键填入</el-button>
    </div>
  </div>
</template>

<style scoped>
.example-block {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid #eff2f8;
}

.example-title {
  margin-bottom: 6px;
  color: #6d7487;
  font-weight: 600;
}

.example-line {
  color: #98a0b2;
  font-size: 12px;
  line-height: 1.6;
}
</style>
