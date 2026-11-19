<template>
  <div class="stock-page">
    <div class="stock-head">
      <div>
        <h2>库存调整</h2>
        <p>库存变动必须形成流水，不允许直接修改当前库存。</p>
      </div>
    </div>

    <div class="stock-form">
      <el-form :model="form" label-width="84px">
        <el-form-item label="备件">
          <el-select v-model="form.partId" filterable placeholder="选择备件" style="width: 100%" @change="loadTxn">
            <el-option v-for="p in parts" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button label="in">入库</el-radio-button>
            <el-radio-button label="out">出库</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.quantity" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="例如：采购入库、盘盈、报废、历史修正" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" :disabled="!canStock" @click="submit">提交库存变动</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="txn-card">
      <div class="txn-head">
        <h3>最近库存流水</h3>
      </div>
      <el-table :data="transactions" size="small">
        <el-table-column prop="txnTime" label="时间" width="160" />
        <el-table-column prop="txnType" label="类型" width="120" />
        <el-table-column label="变动" width="100">
          <template #default="{ row }">
            <span :class="row.quantity > 0 ? 'in' : 'out'">{{ row.quantity > 0 ? '+' : '' }}{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="beforeOnHand" label="变动前" width="90" />
        <el-table-column prop="afterOnHand" label="变动后" width="90" />
        <el-table-column prop="beforeReserved" label="预占前" width="90" />
        <el-table-column prop="afterReserved" label="预占后" width="90" />
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
      </el-table>
      <div v-if="!transactions.length" class="empty">暂无库存流水</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canStock = store.hasPerm('resource:part:stock')
const parts = ref([])
const saving = ref(false)
const transactions = ref([])
const form = reactive({ partId: undefined, type: 'in', quantity: 1, remark: '' })

async function loadParts() {
  parts.value = (await request.get('/crud/part/options')).data || []
}

async function loadTxn() {
  transactions.value = []
  if (!form.partId) return
  const res = await request.get(`/parts/${form.partId}/transactions`, { params: { limit: 20 } })
  transactions.value = res.data || []
}

async function submit() {
  if (!canStock) return
  if (!form.partId || !form.quantity || !form.remark) {
    ElMessage.warning('请填写备件、数量和原因')
    return
  }
  saving.value = true
  try {
    await request.post('/parts/stock', { ...form })
    ElMessage.success('库存流水已记录')
    form.remark = ''
    await loadTxn()
    await loadParts()
  } finally {
    saving.value = false
  }
}

onMounted(loadParts)
</script>

<style scoped>
.stock-page { padding: 2px 0; }
.stock-head { margin-bottom: 16px; }
.stock-head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.stock-head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.stock-form { max-width: 560px; padding: 18px 20px; border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }
.txn-card { margin-top: 16px; padding: 16px 18px; border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }
.txn-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.txn-head h3 { margin: 0; font-size: 15px; font-weight: 600; }
.txn-head span { color: #6b7280; font-size: 12px; }
.in { color: #047857; font-weight: 600; }
.out { color: #b91c1c; font-weight: 600; }
.empty { padding: 18px; color: #9ca3af; text-align: center; font-size: 12px; }
</style>
