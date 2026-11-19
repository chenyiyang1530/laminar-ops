<template>
  <div class="report-page">
    <div class="head"><div><h2>库存报表</h2><p>安全库存补货建议与 90 天无出库呆滞库存。</p></div></div>

    <el-card shadow="never" class="card">
      <template #header><div class="card-head"><b>安全库存 / 补货建议</b><span>当前库存 ≤ 安全库存</span></div></template>
      <el-table :data="lowStock" size="small">
        <el-table-column prop="partCode" label="备件编码" width="160" />
        <el-table-column prop="partName" label="备件" min-width="180" />
        <el-table-column prop="currentStock" label="当前库存" width="100" />
        <el-table-column prop="safetyStock" label="安全库存" width="100" />
        <el-table-column prop="suggestQty" label="建议补货" width="100" />
      </el-table>
      <div v-if="!lowStock.length" class="empty">暂无低库存备件</div>
    </el-card>

    <el-card shadow="never" class="card">
      <template #header><div class="card-head"><b>呆滞库存</b><span>90 天及以上无出库</span></div></template>
      <el-table :data="slowMoving" size="small">
        <el-table-column prop="partCode" label="备件编码" width="160" />
        <el-table-column prop="partName" label="备件" min-width="180" />
        <el-table-column prop="currentStock" label="当前库存" width="100" />
        <el-table-column prop="lastOutTime" label="最近出库" width="120" />
        <el-table-column label="无出库天数" width="110">
          <template #default="{ row }">{{ row.noOutDays ?? '从未出库' }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!slowMoving.length" class="empty">暂无呆滞库存</div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const lowStock = ref([])
const slowMoving = ref([])

async function load() {
  const res = await request.get('/parts/reports/stock')
  lowStock.value = res.data.lowStock || []
  slowMoving.value = res.data.slowMoving || []
}

onMounted(load)
</script>

<style scoped>
.head { margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.card { margin-bottom: 14px; border-radius: 12px; }
.card-head { display: flex; justify-content: space-between; font-size: 14px; }
.card-head span { color: #6b7280; font-size: 12px; }
.empty { padding: 18px; text-align: center; color: #9ca3af; font-size: 12px; }
</style>
