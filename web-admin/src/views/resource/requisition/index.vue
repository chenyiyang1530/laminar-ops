<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div><h2>备件领用</h2><div class="sub">申请 → 审批 → 出库扣减库存</div></div>
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
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" class="pager" @change="load" />
    </div>

    <el-dialog v-model="dialog" title="新建领用" width="620px">
      <el-form label-width="84px">
        <el-form-item label="关联工单"><el-input v-model="form.workorderNo" placeholder="选填，工单号" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="备件明细">
          <div style="width: 100%">
            <div v-for="(it, i) in form.items" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-select v-model="it.partId" filterable placeholder="选择备件" style="flex: 1">
                <el-option v-for="p in parts" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
              <el-input-number v-model="it.quantity" :min="1" style="width: 120px" />
              <el-button link type="danger" @click="form.items.splice(i, 1)">删除</el-button>
            </div>
            <el-button size="small" @click="form.items.push({ partId: undefined, quantity: 1 })">+ 添加备件</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">提交申请</el-button></template>
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
          <el-table-column prop="quantity" label="数量" width="80" />
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
const form = reactive({ workorderNo: '', remark: '', items: [] })
const parts = ref([])
const drawer = ref(false)
const detail = ref(null)
const statusText = (s) => ({ 0: '待审批', 1: '已批准', 2: '已出库', 3: '已驳回', 4: '已退回' }[s] || s)

async function load() {
  const res = await request.get('/parts/requisition/page', { params: { pageNum: pageNum.value, pageSize: pageSize.value, status: status.value } })
  records.value = res.data.records
  total.value = res.data.total
}
function openCreate() { Object.assign(form, { workorderNo: '', remark: '', items: [{ partId: undefined, quantity: 1 }] }); dialog.value = true }
async function submit() {
  const items = form.items.filter((i) => i.partId)
  if (!items.length) { ElMessage.warning('请添加备件'); return }
  await request.post('/parts/requisition', { items, remark: form.remark })
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
onMounted(async () => { parts.value = (await request.get('/crud/part/options')).data; load() })
</script>
<style scoped>.pager { margin-top: 14px; justify-content: flex-end; }</style>
