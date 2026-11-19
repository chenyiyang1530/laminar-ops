<template>
  <div class="stocktake-page">
    <div class="head">
      <div><h2>备件盘点</h2><p>按仓库生成盘点单，审批后按实盘数量生成盘盈盘亏流水。</p></div>
      <el-button type="primary" :disabled="!canStock" @click="openCreate">新建盘点</el-button>
    </div>

    <el-radio-group v-model="status" size="small" style="margin-bottom: 10px" @change="load">
      <el-radio-button label="">全部</el-radio-button>
      <el-radio-button label="0">待审批</el-radio-button>
      <el-radio-button label="1">已通过</el-radio-button>
      <el-radio-button label="2">已驳回</el-radio-button>
    </el-radio-group>

    <el-table :data="records" size="small">
      <el-table-column prop="stocktakeNo" label="盘点单号" width="190" />
      <el-table-column prop="warehouse" label="仓库" width="130" />
      <el-table-column prop="applicantName" label="申请人" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === '0' ? 'warning' : row.status === '1' ? 'success' : 'danger'">
            {{ row.status === '0' ? '待审批' : row.status === '1' ? '已通过' : '已驳回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approveBy" label="审批人" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link @click="openDetail(row)">详情</el-button>
          <template v-if="row.status === '0' && canApprove">
            <el-button link type="success" @click="approve(row, true)">通过</el-button>
            <el-button link type="danger" @click="approve(row, false)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="!records.length" class="empty">暂无盘点单</div>

    <el-dialog v-model="dialog" title="新建盘点" width="820px">
      <el-form label-width="90px">
        <el-form-item label="盘点仓库">
          <el-select v-model="form.warehouse" style="width: 100%" @change="loadSnapshot">
            <el-option v-for="w in warehouses" :key="w" :label="w" :value="w" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点说明"><el-input v-model="form.reason" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <el-table :data="form.items" size="small" max-height="360">
        <el-table-column prop="partName" label="备件" min-width="180" />
        <el-table-column prop="bookQty" label="账面" width="90" />
        <el-table-column label="实盘" width="130">
          <template #default="{ row }"><el-input-number v-model="row.actualQty" :min="0" :precision="0" size="small" /></template>
        </el-table-column>
        <el-table-column label="差异" width="90">
          <template #default="{ row }">{{ (row.actualQty || 0) - row.bookQty }}</template>
        </el-table-column>
        <el-table-column label="原因" min-width="150">
          <template #default="{ row }"><el-input v-model="row.reason" size="small" /></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交盘点</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="盘点详情" width="720px">
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="盘点单号">{{ detail.stocktake.stocktake_no }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.stocktake.warehouse }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.stocktake.status === '0' ? '待审批' : detail.stocktake.status === '1' ? '已通过' : '已驳回' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.stocktake.approve_by || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="detail" :data="detail.items" size="small" style="margin-top: 12px">
        <el-table-column prop="partName" label="备件" />
        <el-table-column prop="bookQty" label="账面" width="90" />
        <el-table-column prop="actualQty" label="实盘" width="90" />
        <el-table-column prop="difference" label="差异" width="90" />
        <el-table-column prop="reason" label="原因" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canStock = store.hasPerm('resource:part:stock')
const canApprove = store.hasPerm('resource:requisition:approve')
const records = ref([])
const status = ref('')
const dialog = ref(false)
const detailDialog = ref(false)
const detail = ref(null)
const saving = ref(false)
const stockRows = ref([])
const form = reactive({ warehouse: '', reason: '', items: [] })
const warehouses = computed(() => [...new Set(stockRows.value.map((s) => s.warehouse))])

async function load() {
  const [s, w] = await Promise.all([
    request.get('/parts/stocktake/page', { params: { pageNum: 1, pageSize: 50, status: status.value } }),
    request.get('/parts/warehouse-stock')
  ])
  records.value = s.data.records || []
  stockRows.value = w.data || []
}

async function loadSnapshot() {
  form.items = stockRows.value.filter((s) => s.warehouse === form.warehouse)
    .map((s) => ({ partId: s.partId, partName: s.partName, bookQty: s.onHand, actualQty: s.onHand, reason: '' }))
}

function openCreate() {
  form.warehouse = ''
  form.reason = ''
  form.items = []
  dialog.value = true
}

async function submit() {
  if (!form.warehouse || !form.items.length) { ElMessage.warning('请选择有库存的盘点仓库'); return }
  saving.value = true
  try {
    await request.post('/parts/stocktake', { ...form })
    ElMessage.success('盘点单已提交，等待审批')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function approve(row, pass) {
  await ElMessageBox.confirm(`确认${pass ? '通过并执行盘点差异' : '驳回'}盘点单「${row.stocktakeNo}」？`, '提示', { type: 'warning' })
  await request.post(`/parts/stocktake/${row.stocktakeId}/${pass ? 'approve' : 'reject'}`, {})
  ElMessage.success('盘点单已处理')
  await load()
}

async function openDetail(row) {
  detail.value = (await request.get(`/parts/stocktake/${row.stocktakeId}`)).data
  detailDialog.value = true
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.empty { padding: 18px; text-align: center; color: #9ca3af; font-size: 12px; }
</style>
