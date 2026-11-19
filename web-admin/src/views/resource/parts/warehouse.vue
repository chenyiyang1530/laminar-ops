<template>
  <div class="warehouse-page">
    <div class="head">
      <div><h2>多仓库存与调拨</h2><p>按仓库查看库存，调拨在同一事务内完成两边流水。</p></div>
    </div>

    <el-card shadow="never" class="card">
      <template #header><b>多仓库存</b></template>
      <el-table :data="stockRows" size="small">
        <el-table-column prop="warehouse" label="仓库" width="130" />
        <el-table-column prop="partCode" label="备件编码" width="160" />
        <el-table-column prop="partName" label="备件" min-width="180" />
        <el-table-column prop="onHand" label="在库" width="90" />
        <el-table-column prop="reserved" label="预占" width="90" />
        <el-table-column prop="available" label="可用" width="90" />
      </el-table>
      <div v-if="!stockRows.length" class="empty">暂无多仓库存</div>
    </el-card>

    <el-card shadow="never" class="card">
      <template #header><b>仓库调拨</b></template>
      <el-form :model="form" label-width="90px" style="max-width: 620px">
        <el-form-item label="备件">
          <el-select v-model="form.partId" filterable style="width: 100%">
            <el-option v-for="p in parts" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="调出仓">
          <el-select v-model="form.fromWarehouse" filterable style="width: 100%">
            <el-option v-for="w in warehouses" :key="w" :label="w" :value="w" />
          </el-select>
        </el-form-item>
        <el-form-item label="调入仓">
          <el-select v-model="form.toWarehouse" filterable allow-create style="width: 100%">
            <el-option v-for="w in warehouses" :key="w" :label="w" :value="w" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" type="textarea" :rows="2" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="saving" @click="submit">提交调拨</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="card">
      <template #header><b>调拨记录</b></template>
      <el-table :data="transfers" size="small">
        <el-table-column prop="transferNo" label="调拨单号" width="190" />
        <el-table-column prop="partName" label="备件" min-width="160" />
        <el-table-column prop="fromWarehouse" label="调出仓" width="120" />
        <el-table-column prop="toWarehouse" label="调入仓" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <div v-if="!transfers.length" class="empty">暂无调拨记录</div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const stockRows = ref([])
const transfers = ref([])
const parts = ref([])
const saving = ref(false)
const form = reactive({ partId: undefined, fromWarehouse: '', toWarehouse: '', quantity: 1, reason: '' })
const warehouses = computed(() => [...new Set(stockRows.value.map((s) => s.warehouse))])

async function load() {
  const [s, t, p] = await Promise.all([
    request.get('/parts/warehouse-stock'),
    request.get('/parts/transfers/page', { params: { pageNum: 1, pageSize: 50 } }),
    request.get('/crud/part/options')
  ])
  stockRows.value = s.data || []
  transfers.value = t.data.records || []
  parts.value = p.data || []
}

async function submit() {
  if (!form.partId || !form.fromWarehouse || !form.toWarehouse || !form.quantity) {
    ElMessage.warning('请填写备件、调出仓、调入仓和数量')
    return
  }
  saving.value = true
  try {
    await request.post('/parts/transfer', { ...form })
    ElMessage.success('调拨完成')
    form.reason = ''
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head { margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.card { margin-bottom: 14px; border-radius: 12px; }
.empty { padding: 18px; text-align: center; color: #9ca3af; font-size: 12px; }
</style>
