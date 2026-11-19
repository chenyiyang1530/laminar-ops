<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div><h2>备件领用</h2><div class="sub">申请 → 审批预占 → 出库扣减库存</div></div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建领用</el-button>
      </div>
      <el-radio-group v-model="status" size="small" style="margin-bottom: 10px" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="0">待审批</el-radio-button>
        <el-radio-button label="1">已批准</el-radio-button>
        <el-radio-button label="2">已出库</el-radio-button>
        <el-radio-button label="3">已驳回</el-radio-button>
      </el-radio-group>
      <el-table :data="records" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="reqNo" label="领用单号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="110" />
        <el-table-column prop="hospitalName" label="医院" min-width="170" show-overflow-tooltip />
        <el-table-column prop="workorderNo" label="关联工单" width="160" />
        <el-table-column label="金额" width="110">
        <template #default="{ row }">¥{{ row.totalAmount }}</template></el-table-column>
        <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag size="small" :type="['warning','primary','success','danger'][Number(row.status)] || 'info'">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="150" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '0'" link type="success" @click="approve(row, true)">批准</el-button>
            <el-button v-if="row.status === '0'" link type="danger" @click="approve(row, false)">驳回</el-button>
            <el-button v-if="row.status === '1'" link type="primary" @click="outbound(row)">出库</el-button>
            <el-button v-if="row.status === '2'" link type="warning" @click="openReturn(row)">退料</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" class="pager" @change="load" />
    </div>

    <el-dialog v-model="dialog" title="新建领用" width="780px">
      <el-form label-width="84px">
        <el-form-item label="关联工单"><el-input v-model="form.workorderNo" placeholder="选填，工单号" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="备件明细">
          <div style="width: 100%">
            <div v-for="(it, i) in form.items" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-select v-model="it.partId" filterable placeholder="选择备件" style="flex: 1">
                <el-option v-for="p in parts" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
              <el-select v-model="it.equipmentId" filterable clearable placeholder="使用设备（选填）" style="flex: 1">
                <el-option v-for="e in equipmentOptions" :key="e.value" :label="e.label" :value="e.value" />
              </el-select>
              <el-input-number v-model="it.quantity" :min="1" style="width: 110px" />
              <el-button link type="danger" @click="form.items.splice(i, 1)">删除</el-button>
            </div>
            <el-button size="small" @click="form.items.push({ partId: undefined, equipmentId: undefined, quantity: 1 })">+ 添加备件</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">提交申请</el-button></template>
    </el-dialog>

    <el-dialog v-model="returnDialog" title="领用退料" width="520px">
      <el-form :model="returnForm" label-width="84px">
        <el-form-item label="退料明细">
          <el-select v-model="returnForm.itemId" style="width: 100%" placeholder="选择领用明细">
            <el-option v-for="it in returnItems" :key="it.itemId"
              :label="`${it.partName}（可退 ${(it.outboundQty || 0) - (it.returnedQty || 0)}）`"
              :value="it.itemId" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="returnForm.quantity" :min="1" :precision="0" style="width: 100%" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="returnForm.reason" type="textarea" :rows="2" placeholder="请填写退料原因" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReturn">确认退料</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" size="480px" title="领用详情">
      <div v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="单号">{{ detail.requisition.req_no }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.requisition.status) }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" size="small" style="margin-top: 12px">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
          <el-table-column prop="partName" label="备件" />
          <el-table-column prop="equipmentName" label="使用设备" width="130" />
          <el-table-column prop="quantity" label="申请" width="70" />
          <el-table-column prop="reservedQty" label="预占" width="70" />
          <el-table-column prop="outboundQty" label="已出库" width="80" />
          <el-table-column prop="amount" label="金额" width="100" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(store.pageSize || 10)
const status = ref('')
const dialog = ref(false)
const form = reactive({ workorderNo: '', remark: '', requestId: '', items: [] })
const parts = ref([])
const drawer = ref(false)
const detail = ref(null)
const returnDialog = ref(false)
const returnItems = ref([])
const equipmentOptions = ref([])
const returnForm = reactive({ reqId: null, itemId: null, quantity: 1, reason: '' })
const statusText = (s) => ({ 0: '待审批', 1: '已批准', 2: '已出库', 3: '已驳回', 4: '已退回' }[s] || s)

async function load() {
  const res = await request.get('/parts/requisition/page', { params: { pageNum: pageNum.value, pageSize: pageSize.value, status: status.value } })
  records.value = res.data.records
  total.value = res.data.total
}
function openCreate() { Object.assign(form, { workorderNo: '', remark: '', requestId: crypto.randomUUID(), items: [{ partId: undefined, equipmentId: undefined, quantity: 1 }] }); dialog.value = true }
async function submit() {
  const items = form.items.filter((i) => i.partId)
  if (!items.length) { ElMessage.warning('请添加备件'); return }
  await request.post('/parts/requisition', { items, remark: form.remark, requestId: form.requestId })
  ElMessage.success('已提交'); dialog.value = false; load()
}
async function approve(row, pass) {
  await ElMessageBox.confirm(`确认${pass ? '批准' : '驳回'}该领用单？`, '提示', { type: 'warning' })
  await request.post(`/parts/requisition/${row.reqId}/${pass ? 'approve' : 'reject'}`, {})
  ElMessage.success('已处理'); load()
}
async function outbound(row) {
  await ElMessageBox.confirm('确认出库？将扣减库存', '提示', { type: 'warning' })
  await request.post(`/parts/requisition/${row.reqId}/out`)
  ElMessage.success('已出库'); load()
}
async function openDetail(row) { drawer.value = true; detail.value = (await request.get('/parts/requisition/' + row.reqId)).data }
async function openReturn(row) {
  const d = (await request.get('/parts/requisition/' + row.reqId)).data
  returnItems.value = (d.items || []).filter((it) => (it.outboundQty || 0) > (it.returnedQty || 0))
  if (!returnItems.value.length) { ElMessage.warning('没有可退数量'); return }
  Object.assign(returnForm, { reqId: row.reqId, itemId: returnItems.value[0].itemId, quantity: 1, reason: '' })
  returnDialog.value = true
}
async function submitReturn() {
  if (!returnForm.itemId || !returnForm.reason) { ElMessage.warning('请选择明细并填写原因'); return }
  await request.post(`/parts/requisition/${returnForm.reqId}/return`, {
    itemId: returnForm.itemId, quantity: returnForm.quantity, reason: returnForm.reason
  })
  ElMessage.success('退料已入库并记录审计')
  returnDialog.value = false
  load()
}
onMounted(async () => {
  parts.value = (await request.get('/crud/part/options')).data
  equipmentOptions.value = (await request.get('/crud/equipment/options')).data || []
  load()
})
</script>
<style scoped>.pager { margin-top: 14px; justify-content: flex-end; }</style>
