<template>
  <div class="page">
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div><h2>报修工单</h2><div class="sub">本院报修、进度跟踪与验收确认</div></div>
        <el-button type="primary" :icon="Plus" @click="openReport">我要报修</el-button>
      </div>
      <el-radio-group v-model="query.status" size="small" style="margin-top:12px" @change="search">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="0">待受理</el-radio-button>
        <el-radio-button label="2">处理中</el-radio-button>
        <el-radio-button label="3">待验收</el-radio-button>
        <el-radio-button label="5">已完成</el-radio-button>
      </el-radio-group>
      <el-table :data="records" size="small" style="margin-top:12px">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column prop="workorderNo" label="工单号" width="155" />
        <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
        <el-table-column prop="equipmentName" label="设备" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === '5' ? 'success' : row.status === '3' ? 'warning' : 'info'">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="engineerName" label="工程师" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="145" />
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '3'" link type="success" @click="openAccept(row)">验收</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" class="pager" @change="load" />
    </div>

    <el-dialog v-model="reportDialog" title="我要报修" width="560px">
      <el-form :model="report" label-width="88px">
        <el-form-item label="设备" required>
          <el-select v-model="report.equipmentId" filterable style="width: 100%">
            <el-option v-for="e in equipments" :key="e.value" :label="e.label" :value="e.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required><el-input v-model="report.title" /></el-form-item>
        <el-form-item label="故障描述"><el-input v-model="report.faultDesc" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="report.reportContact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="电话"><el-input v-model="report.reportPhone" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交报修</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="acceptDialog" title="工单验收" width="460px">
      <el-form label-width="80px">
        <el-form-item label="结果">
          <el-radio-group v-model="accept.result"><el-radio value="1">通过</el-radio><el-radio value="0">驳回</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="意见"><el-input v-model="accept.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAccept">确认验收</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" size="540px" :title="detail?.workorder?.workorder_no || '工单详情'">
      <div v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="标题">{{ detail.workorder.title }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ detail.workorder.equipment_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.workorder.status) }}</el-descriptions-item>
          <el-descriptions-item label="工程师">{{ detail.workorder.engineer_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="维修报告">{{ detail.workorder.repair_report || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:18px 0 10px">流转记录</h4>
        <el-timeline>
          <el-timeline-item v-for="(l, i) in detail.logs" :key="i" :timestamp="l.createTime">
            <div style="font-size:13px">{{ l.operatorName }} · {{ l.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/request'
import { store } from '@/store'

const records = ref([])
const total = ref(0)
const equipments = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, status: undefined })
const reportDialog = ref(false)
const report = reactive({ equipmentId: undefined, title: '', faultDesc: '', reportContact: '', reportPhone: '' })
const acceptDialog = ref(false)
const accept = reactive({ result: '1', remark: '' })
const current = ref(null)
const drawer = ref(false)
const detail = ref(null)

const statusText = (s) => ({ 0: '待受理', 1: '待派工', 2: '处理中', 3: '待验收', 4: '待回访', 5: '已完成', 6: '已取消' }[s] || s)

async function load() {
  const res = await request.get('/portal/workorders', { params: query })
  records.value = res.data.records
  total.value = res.data.total
}
function search() { query.pageNum = 1; load() }

function openReport() {
  Object.assign(report, { equipmentId: undefined, title: '', faultDesc: '', reportContact: store.profile?.nickName || '', reportPhone: '' })
  reportDialog.value = true
}
async function submitReport() {
  if (!report.equipmentId || !report.title) { ElMessage.warning('请选择设备并填写标题'); return }
  await request.post('/portal/workorders', report)
  ElMessage.success('报修已提交')
  reportDialog.value = false
  load()
}
function openAccept(row) { current.value = row; accept.result = '1'; accept.remark = ''; acceptDialog.value = true }
async function submitAccept() {
  await request.post(`/portal/workorders/${current.value.workorderId}/accept`, accept)
  ElMessage.success('验收已提交')
  acceptDialog.value = false
  load()
}
async function openDetail(row) { drawer.value = true; detail.value = (await request.get('/portal/workorders/' + row.workorderId)).data }

onMounted(async () => {
  equipments.value = (await request.get('/portal/equipment-options')).data
  load()
})
</script>
