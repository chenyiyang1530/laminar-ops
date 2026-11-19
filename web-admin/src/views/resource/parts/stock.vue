<template>
  <div class="stock-page">
    <div class="stock-head">
      <div>
        <h2>库存调整</h2>
        <p>库存调整必须提交审批，审批通过后才生成库存流水；不允许直接修改当前库存。</p>
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
        <el-form-item label="原因类型">
          <el-select v-model="form.reasonType" style="width: 100%">
            <el-option label="采购入库" value="purchase" />
            <el-option label="盘点差异" value="stocktake" />
            <el-option label="盘盈" value="gain" />
            <el-option label="盘亏" value="loss" />
            <el-option label="报废" value="scrap" />
            <el-option label="历史修正" value="correction" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="必须填写调整原因" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" :disabled="!canStock" @click="submit">提交调整申请</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="txn-card">
      <div class="txn-head">
        <h3>调整审批记录</h3>
        <el-radio-group v-model="status" size="small" @change="loadAdjustments">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="0">待审批</el-radio-button>
          <el-radio-button label="1">已通过</el-radio-button>
          <el-radio-button label="2">已驳回</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="adjustments" size="small">
        <el-table-column prop="adjustmentNo" label="调整单号" width="180" />
        <el-table-column prop="partName" label="备件" min-width="140" />
        <el-table-column label="变动" width="90">
          <template #default="{ row }">{{ row.adjustType === 'in' ? '+' : '-' }}{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column prop="reasonType" label="原因类型" width="110" />
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === '0' ? 'warning' : row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '0' ? '待审批' : row.status === '1' ? '已通过' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="createTime" label="申请时间" width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '0' && canApprove">
              <el-button link type="success" @click="approve(row, true)">通过</el-button>
              <el-button link type="danger" @click="approve(row, false)">驳回</el-button>
            </template>
            <span v-else class="muted">{{ row.approveBy || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!adjustments.length" class="empty">暂无调整单</div>
    </div>

    <div class="txn-card">
      <div class="txn-head"><h3>最近库存流水</h3></div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canStock = store.hasPerm('resource:part:stock')
const canApprove = store.hasPerm('resource:requisition:approve')
const parts = ref([])
const saving = ref(false)
const status = ref('')
const adjustments = ref([])
const transactions = ref([])
const form = reactive({ partId: undefined, type: 'in', quantity: 1, reasonType: 'purchase', reason: '' })

async function loadParts() {
  parts.value = (await request.get('/crud/part/options')).data || []
}

async function loadAdjustments() {
  const res = await request.get('/parts/adjustment/page', { params: { pageNum: 1, pageSize: 50, status: status.value } })
  adjustments.value = res.data.records || []
}

async function loadTxn() {
  transactions.value = []
  if (!form.partId) return
  const res = await request.get(`/parts/${form.partId}/transactions`, { params: { limit: 20 } })
  transactions.value = res.data || []
}

async function submit() {
  if (!canStock) return
  if (!form.partId || !form.quantity || !form.reason) {
    ElMessage.warning('请填写备件、数量和原因')
    return
  }
  saving.value = true
  try {
    await request.post('/parts/stock', { ...form, remark: form.reason })
    ElMessage.success('调整申请已提交，等待审批')
    form.reason = ''
    await loadAdjustments()
  } finally {
    saving.value = false
  }
}

async function approve(row, pass) {
  await ElMessageBox.confirm(`确认${pass ? '通过' : '驳回'}调整单「${row.adjustmentNo}」？`, '提示', { type: 'warning' })
  await request.post(`/parts/adjustment/${row.adjustmentId}/${pass ? 'approve' : 'reject'}`, {})
  ElMessage.success('审批完成')
  await loadAdjustments()
  await loadTxn()
}

onMounted(async () => {
  await loadParts()
  await loadAdjustments()
})
</script>

<style scoped>
.stock-page { padding: 2px 0; }
.stock-head { margin-bottom: 16px; }
.stock-head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.stock-head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.stock-form { max-width: 560px; padding: 18px 20px; border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }
.txn-card { margin-top: 16px; padding: 16px 18px; border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }
.txn-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 8px; }
.txn-head h3 { margin: 0; font-size: 15px; font-weight: 600; }
.in { color: #047857; font-weight: 600; }
.out { color: #b91c1c; font-weight: 600; }
.muted { color: #9ca3af; font-size: 11px; }
.empty { padding: 18px; color: #9ca3af; text-align: center; font-size: 12px; }
</style>
