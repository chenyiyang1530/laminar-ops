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
        <el-table-column prop="companyName" label="服务公司" min-width="170" show-overflow-tooltip />
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
        <el-form-item label="报修对象">
          <el-radio-group v-model="report.repairType">
            <el-radio value="equipment">设备故障</el-radio>
            <el-radio value="facility">其他设施</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="report.repairType === 'equipment'" label="设备" required>
          <el-select v-model="report.equipmentId" filterable style="width: 100%">
            <el-option v-for="e in equipments" :key="e.value" :label="e.label" :value="e.value" />
          </el-select>
        </el-form-item>
        <template v-else>
          <el-form-item v-if="deptScoped" label="报修科室">
            <el-input :model-value="store.profile?.departmentName || '本科室'" disabled />
          </el-form-item>
          <template v-else>
            <el-form-item label="院区" required>
              <el-select v-model="report.campusId" style="width: 100%" placeholder="选择院区" @change="loadDepartments">
                <el-option v-for="c in campuses" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="科室" required>
              <el-select v-model="report.departmentId" filterable style="width: 100%" placeholder="选择科室">
                <el-option v-for="d in departments" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </template>
        </template>
        <el-form-item label="标题" required><el-input v-model="report.title" placeholder="例如：3号手术室空调出风温度异常 / 手术室操作台损坏" /></el-form-item>
        <el-form-item label="紧急程度">
          <el-radio-group v-model="report.faultLevel">
            <el-radio value="1">紧急</el-radio>
            <el-radio value="2">一般</el-radio>
            <el-radio value="3">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故障描述" required><el-input v-model="report.faultDesc" type="textarea" :rows="3" placeholder="请描述故障现象、开始时间、是否影响使用" /></el-form-item>
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

    <el-dialog v-model="acceptDialog" title="工单验收" width="640px">
      <div v-loading="acceptLoading">
        <el-descriptions v-if="acceptDetail" :column="2" border size="small">
          <el-descriptions-item label="工单号">{{ acceptDetail.workorder_no }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ acceptDetail.equipment_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="服务公司">{{ acceptDetail.company_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="工程师">{{ acceptDetail.engineer_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到场时间">{{ acceptDetail.arrive_time || '—' }}</el-descriptions-item>
          <el-descriptions-item label="完工时间">{{ acceptDetail.finish_time || '—' }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">{{ acceptDetail.response_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="停机/维修时间">{{ acceptDetail.downtime_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="故障原因" :span="2">{{ acceptDetail.finish_root_cause || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处理措施" :span="2">{{ acceptDetail.finish_solution || '—' }}</el-descriptions-item>
          <el-descriptions-item label="维修报告" :span="2">{{ acceptDetail.repair_report || '—' }}</el-descriptions-item>
          <el-descriptions-item label="更换备件">{{ acceptDetail.finish_parts || '未更换' }}</el-descriptions-item>
          <el-descriptions-item label="现场照片">
            <SecureFileLink v-if="acceptDetail.finish_image" :path="acceptDetail.finish_image" text="查看照片" />
            <span v-else>—</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else-if="!acceptLoading" :image-size="42" description="暂无完工详情" />
      </div>
      <el-form label-width="80px" style="margin-top: 16px">
        <el-form-item label="结果">
          <el-radio-group v-model="accept.result"><el-radio value="1">通过</el-radio><el-radio value="0">驳回</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="意见"><el-input v-model="accept.remark" type="textarea" :rows="2" placeholder="驳回请说明原因" /></el-form-item>
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
          <el-descriptions-item label="位置">{{ detail.workorder.campus_name || '—' }} / {{ detail.workorder.department_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.workorder.status) }}</el-descriptions-item>
          <el-descriptions-item label="服务公司">{{ detail.workorder.company_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="报修时间">{{ detail.workorder.create_time || '—' }}</el-descriptions-item>
          <el-descriptions-item label="工程师">{{ detail.workorder.engineer_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="故障描述">{{ detail.workorder.fault_desc || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到场时间">{{ detail.workorder.arrive_time || '—' }}</el-descriptions-item>
          <el-descriptions-item label="完工时间">{{ detail.workorder.finish_time || '—' }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">{{ detail.workorder.response_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="停机/维修时间">{{ detail.workorder.downtime_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="故障原因">{{ detail.workorder.finish_root_cause || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处理措施">{{ detail.workorder.finish_solution || '—' }}</el-descriptions-item>
          <el-descriptions-item label="维修报告">{{ detail.workorder.repair_report || '—' }}</el-descriptions-item>
          <el-descriptions-item label="更换备件">{{ detail.workorder.finish_parts || '未更换' }}</el-descriptions-item>
          <el-descriptions-item label="现场照片">
            <SecureFileLink v-if="detail.workorder.finish_image" :path="detail.workorder.finish_image" text="查看照片" />
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="验收结果">{{ detail.workorder.accept_result === '1' ? '通过' : detail.workorder.accept_result === '0' ? '驳回' : '待验收' }}</el-descriptions-item>
          <el-descriptions-item label="验收意见">{{ detail.workorder.accept_remark || '—' }}</el-descriptions-item>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/request'
import { store } from '@/store'
import SecureFileLink from '@/components/SecureFileLink.vue'

const records = ref([])
const total = ref(0)
const equipments = ref([])
const campuses = ref([])
const departments = ref([])
const deptScoped = computed(() => !!store.profile?.departmentId)
const query = reactive({ pageNum: 1, pageSize: store.profile?.pageSize || 10, status: undefined })
const reportDialog = ref(false)
const report = reactive({ repairType: 'equipment', equipmentId: undefined, campusId: undefined, departmentId: undefined, title: '', faultLevel: '2', faultDesc: '', reportContact: '', reportPhone: '' })
const acceptDialog = ref(false)
const acceptLoading = ref(false)
const acceptDetail = ref(null)
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

async function openReport() {
  Object.assign(report, {
    repairType: 'equipment',
    equipmentId: undefined,
    campusId: undefined,
    departmentId: deptScoped.value ? store.profile?.departmentId : undefined,
    title: '',
    faultLevel: '2',
    faultDesc: '',
    reportContact: store.profile?.nickName || '',
    reportPhone: ''
  })
  reportDialog.value = true
  if (!deptScoped.value) {
    try {
      campuses.value = (await request.get('/portal/campus-options')).data || []
    } catch (e) { campuses.value = [] }
  }
}

async function loadDepartments() {
  departments.value = []
  report.departmentId = undefined
  if (!report.campusId) return
  try {
    departments.value = (await request.get('/portal/department-options', { params: { campusId: report.campusId } })).data || []
  } catch (e) { departments.value = [] }
}

async function submitReport() {
  if (report.repairType === 'equipment' && !report.equipmentId) {
    ElMessage.warning('请选择报修设备')
    return
  }
  if (report.repairType === 'facility' && !deptScoped.value && (!report.campusId || !report.departmentId)) {
    ElMessage.warning('请选择报修院区和科室')
    return
  }
  if (!report.title || !report.faultDesc) {
    ElMessage.warning('请填写标题和故障描述')
    return
  }
  const payload = { ...report }
  if (report.repairType === 'equipment') {
    payload.campusId = undefined
    payload.departmentId = undefined
  } else if (deptScoped.value) {
    payload.campusId = undefined
    payload.departmentId = store.profile?.departmentId
  }
  await request.post('/portal/workorders', payload)
  ElMessage.success('报修已提交')
  reportDialog.value = false
  load()
}
async function openAccept(row) {
  current.value = row
  accept.result = '1'
  accept.remark = ''
  acceptDetail.value = null
  acceptLoading.value = true
  acceptDialog.value = true
  try {
    detail.value = (await request.get('/portal/workorders/' + row.workorderId)).data
    acceptDetail.value = detail.value?.workorder || null
  } finally {
    acceptLoading.value = false
  }
}
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
