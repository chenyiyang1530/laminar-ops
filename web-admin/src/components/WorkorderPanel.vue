<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>{{ mine ? '我的工单' : '工单管理' }}</h2>
          <div class="sub">报修 → 受理 → 派工 → 到场 → 完工 → 验收 → 回访 全流程闭环</div>
        </div>
        <el-button v-if="!mine && can('add')" type="primary" :icon="Plus" @click="openCreate">新建工单</el-button>
      </div>

      <div class="status-tabs">
        <el-radio-group v-model="query.status" size="small" @change="search">
          <el-radio-button label="open">进行中</el-radio-button>
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="0">待受理</el-radio-button>
          <el-radio-button label="1">待派工</el-radio-button>
          <el-radio-button label="2">处理中</el-radio-button>
          <el-radio-button label="3">待验收</el-radio-button>
          <el-radio-button label="4">待回访</el-radio-button>
          <el-radio-button label="5">已完成</el-radio-button>
        </el-radio-group>
      </div>

      <el-form inline class="search-form">
        <el-form-item label="等级">
          <el-select v-model="query.level" clearable placeholder="全部" style="width: 120px" @change="search">
            <el-option v-for="d in dicts.level" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="医院" v-if="!mine">
          <el-select v-model="query.hospitalId" clearable placeholder="全部" style="width: 200px" @change="search">
            <el-option v-for="h in hospitals" :key="h.value" :label="h.label" :value="h.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" clearable placeholder="工单号 / 标题" style="width: 190px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="list-tools-bar">
        <ListTools
          v-model="visibleCols"
          :columns="workorderColumns"
          :default-keys="workorderDefaultKeys"
          :exporting="exporting"
          @reset="resetCols"
          @export="exportCsv"
        />
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column v-if="isVisible('workorderNo')" prop="workorderNo" label="工单号" min-width="160" />
        <el-table-column v-if="isVisible('title')" prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="!mine && isVisible('hospitalName')" prop="hospitalName" label="医院" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="isVisible('companyName')" prop="companyName" label="承接公司" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="isVisible('equipmentName')" prop="equipmentName" label="设备" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="isVisible('faultLevel')" label="等级" min-width="80">
          <template #default="{ row }">
            <el-tag :type="levelType(row.faultLevel)" size="small" effect="dark">{{ levelText(row.faultLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isVisible('status')" label="状态" min-width="96">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isVisible('engineerName')" prop="engineerName" label="工程师" min-width="100">
          <template #default="{ row }">{{ row.engineerName || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="isVisible('createTime')" prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '0' && can('receive')" link type="primary" @click="receive(row)">受理</el-button>
            <el-button v-if="row.status === '1' && can('dispatch')" link type="primary" @click="openDispatch(row)">派工</el-button>
            <el-button v-if="['2'].includes(row.status) && can('dispatch')" link @click="openReassign(row)">改派</el-button>
            <el-button v-if="row.status === '2' && can('arrive') && isAssignedEngineer(row) && !row.arriveTime" link type="warning" @click="arrive(row)">到场</el-button>
            <el-button v-if="row.status === '2' && can('finish') && isAssignedEngineer(row) && row.arriveTime" link type="success" @click="openFinish(row)">完工</el-button>
            <el-button v-if="row.status === '3' && can('accept') && store.isHospitalUser" link type="success" @click="openAccept(row)">验收</el-button>
            <el-button v-if="row.status === '4' && can('callback')" link type="primary" @click="openCallback(row)">回访</el-button>
            <el-button link @click="openDetail(row)">详情</el-button>
            <el-button v-if="['0','1'].includes(row.status) && can('cancel')" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @change="load"
      />
    </div>

    <el-dialog v-model="createDialog" title="新建工单" width="640px">
      <el-form :model="createForm" label-width="92px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="医院" required>
              <el-select v-model="createForm.hospitalId" filterable :disabled="!!store.hospitalId" style="width: 100%" @change="onHospitalChange">
                <el-option v-for="h in hospitals" :key="h.value" :label="h.label" :value="h.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备">
              <el-select
                v-model="createForm.equipmentId"
                clearable
                filterable
                :disabled="!createForm.hospitalId"
                :placeholder="createForm.hospitalId ? '请选择设备' : '请先选择医院'"
                style="width: 100%"
              >
                <el-option v-for="e in equipments" :key="e.value" :label="e.label" :value="e.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标题" required><el-input v-model="createForm.title" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="故障等级">
              <el-select v-model="createForm.faultLevel" style="width: 100%">
                <el-option v-for="d in dicts.level" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="故障描述"><el-input v-model="createForm.faultDesc" type="textarea" :rows="3" /></el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="createForm.reportContact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="createForm.reportPhone" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dispatchDialog" :title="dispatchMode === 'reassign' ? '改派' : '派工'" width="620px">
      <el-form label-width="80px">
        <el-form-item label="工程师" required>
          <el-select v-model="dispatchForm.engineerId" filterable style="width: 100%">
            <el-option v-for="g in engineers" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
        <div v-if="selectedCandidate" class="candidate-score">
          <div class="candidate-head">
            <b>{{ selectedCandidate.engineer_name }}</b>
            <span>综合得分 <strong>{{ selectedCandidate.score }}</strong></span>
          </div>
          <div class="dimension-grid">
            <div v-for="d in selectedCandidate.dimensions" :key="d.key" class="dimension">
              <span class="dim-label">{{ d.label }}</span>
              <span class="dim-score">{{ d.score }}</span>
              <span class="dim-weight">权重 {{ d.weight }}%</span>
            </div>
          </div>
          <div class="dimension-reasons">
            <span v-for="d in selectedCandidate.dimensions" :key="d.key">{{ d.label }}：{{ d.reason }}</span>
          </div>
        </div>
        <el-form-item label="说明"><el-input v-model="dispatchForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchDialog = false">取消</el-button>
        <el-button type="primary" @click="submitDispatch">{{ dispatchMode === 'reassign' ? '确认改派' : '确认派工' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishDialog" title="完工提交" width="640px">
      <el-alert
        v-if="current && !current.arriveTime"
        title="请先完成到场签到，再提交完工"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 14px"
      />
      <el-form label-width="100px">
        <el-form-item label="到场时间">
          <span class="finish-arrive">{{ current?.arriveTime || '未签到' }}</span>
        </el-form-item>
        <el-form-item label="故障原因" required>
          <el-input v-model="finishForm.rootCause" type="textarea" :rows="2" placeholder="如：初效过滤器堵塞、风机轴承磨损、传感器漂移" />
        </el-form-item>
        <el-form-item label="处理措施" required>
          <el-input v-model="finishForm.solution" type="textarea" :rows="2" placeholder="如：更换初效过滤器，复测压差并恢复运行" />
        </el-form-item>
        <el-form-item label="维修报告" required>
          <el-input v-model="finishForm.report" type="textarea" :rows="4" placeholder="记录排查过程、处理结果、复测数据" />
        </el-form-item>
        <el-form-item label="更换备件">
          <el-switch v-model="finishForm.partsReplaced" active-text="已更换" inactive-text="未更换" />
        </el-form-item>
        <el-form-item v-if="finishForm.partsReplaced" label="备件明细">
          <el-input v-model="finishForm.partsUsed" placeholder="备件名称、规格、数量" />
        </el-form-item>
        <el-form-item v-if="finishForm.partsReplaced" label="使用备件">
          <div style="width: 100%">
            <div v-for="(p, i) in finishForm.parts" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-select v-model="p.partId" filterable placeholder="选择备件" style="flex: 1">
                <el-option v-for="it in partOptions" :key="it.value" :label="it.label" :value="it.value" />
              </el-select>
              <el-input-number v-model="p.quantity" :min="1" style="width: 110px" />
              <el-button link type="danger" @click="finishForm.parts.splice(i, 1)">删除</el-button>
            </div>
            <el-button size="small" @click="finishForm.parts.push({ partId: undefined, quantity: 1 })">+ 添加实际使用备件</el-button>
          </div>
        </el-form-item>
        <el-form-item label="时间指标">
          <span class="finish-arrive">响应时间：告警 → 到场；停机/维修时间：到场 → 完工，提交后自动计算</span>
        </el-form-item>
        <el-form-item label="现场照片">
          <FileUpload v-model="finishForm.imageUrl" category="workorder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFinish">提交验收</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="acceptDialog" title="工单验收" width="680px">
      <div v-loading="acceptLoading" class="accept-detail">
        <el-descriptions v-if="acceptDetail" :column="2" border size="small">
          <el-descriptions-item label="工单号">{{ acceptDetail.workorder_no }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ acceptDetail.equipment_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="到场时间">{{ acceptDetail.arrive_time || '—' }}</el-descriptions-item>
          <el-descriptions-item label="完工时间">{{ acceptDetail.finish_time || '—' }}</el-descriptions-item>
          <el-descriptions-item label="故障原因" :span="2">{{ acceptDetail.finish_root_cause || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处理措施" :span="2">{{ acceptDetail.finish_solution || '—' }}</el-descriptions-item>
          <el-descriptions-item label="维修报告" :span="2">{{ acceptDetail.repair_report || '—' }}</el-descriptions-item>
          <el-descriptions-item label="更换备件">{{ acceptDetail.finish_parts || '未更换' }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">{{ acceptDetail.response_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="停机/维修时间">{{ acceptDetail.downtime_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="现场照片" :span="2">
            <SecureFileLink v-if="acceptDetail.finish_image" :path="acceptDetail.finish_image" text="查看现场照片" />
            <span v-else>—</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else-if="!acceptLoading" :image-size="42" description="暂无完工详情" />
      </div>
      <el-form label-width="80px" style="margin-top: 16px">
        <el-form-item label="验收结果">
          <el-radio-group v-model="acceptForm.result">
            <el-radio value="1">通过</el-radio>
            <el-radio value="0">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验收意见"><el-input v-model="acceptForm.remark" type="textarea" :rows="2" placeholder="通过可不填；驳回请说明原因" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAccept">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="callbackDialog" title="服务回访" width="460px">
      <el-form label-width="80px">
        <el-form-item label="满意度">
          <el-rate v-model="callbackForm.score" />
        </el-form-item>
        <el-form-item label="回访内容"><el-input v-model="callbackForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="callbackDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCallback">完成回访</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" size="640px" :title="detail?.workorder?.workorder_no || '工单详情'">
      <div v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="标题" :span="2">{{ detail.workorder.title }}</el-descriptions-item>
          <el-descriptions-item label="医院">{{ detail.workorder.hospital_name }}</el-descriptions-item>
          <el-descriptions-item label="承接公司">{{ detail.workorder.company_name || '未匹配合同' }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ detail.workorder.equipment_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="位置">{{ detail.workorder.campus_name }} / {{ detail.workorder.department_name }}</el-descriptions-item>
          <el-descriptions-item label="工程师">{{ detail.workorder.engineer_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.workorder.status) }}</el-descriptions-item>
          <el-descriptions-item label="等级">{{ levelText(detail.workorder.fault_level) }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ detail.workorder.fault_desc || '—' }}</el-descriptions-item>
          <el-descriptions-item label="维修报告" :span="2">{{ detail.workorder.repair_report || '—' }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">{{ detail.workorder.response_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="停机/维修时间">{{ detail.workorder.downtime_minutes ?? 0 }} 分钟</el-descriptions-item>
          <el-descriptions-item label="总费用">{{ detail.workorder.total_cost ?? '—' }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="sec">费用明细</h4>
        <el-table :data="detail.costs" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
          <el-table-column prop="costName" label="项目" />
          <el-table-column prop="costType" label="类型" width="90" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column prop="amount" label="金额" width="90" />
        </el-table>

        <h4 class="sec">流转记录</h4>
        <el-timeline>
          <el-timeline-item v-for="(l, i) in detail.logs" :key="i" :timestamp="l.createTime">
            <div class="log-t">{{ actionText(l.action) }} · {{ l.operatorName }}</div>
            <div class="log-c">{{ l.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import ListTools from '@/components/ListTools.vue'
import FileUpload from '@/components/FileUpload.vue'
import SecureFileLink from '@/components/SecureFileLink.vue'
import { useColumnPrefs } from '@/composables/useColumnPrefs'
import { downloadCsv } from '@/utils/csv'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const props = defineProps({ mine: { type: Boolean, default: false } })
const workorderColumns = [
  { key: 'workorderNo', label: '工单号' },
  { key: 'title', label: '标题' },
  { key: 'hospitalName', label: '医院' },
  { key: 'companyName', label: '承接公司' },
  { key: 'equipmentName', label: '设备' },
  { key: 'faultLevel', label: '等级' },
  { key: 'status', label: '状态' },
  { key: 'engineerName', label: '工程师' },
  { key: 'createTime', label: '创建时间' }
]
const workorderDefaultKeys = workorderColumns.map((c) => c.key)
const { visible: visibleCols, reset: resetCols } = useColumnPrefs(
  `laminar_cols_workorder_${props.mine ? 'mine' : 'all'}`,
  workorderDefaultKeys
)
const exporting = ref(false)
const isVisible = (key) => visibleCols.value.includes(key)

const store = useUserStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const hospitals = ref([])
const equipments = ref([])
const engineers = ref([])
const dispatchCandidates = ref([])
const selectedCandidate = computed(() =>
  dispatchCandidates.value.find((c) => String(c.engineer_id) === String(dispatchForm.engineerId))
)
const dicts = reactive({ level: [] })
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, status: 'open', level: undefined, hospitalId: undefined, keyword: undefined })
const detailDrawer = ref(false)
const detail = ref(null)
const createDialog = ref(false)
const createForm = reactive({ hospitalId: undefined, equipmentId: undefined, title: '', faultLevel: '2', faultDesc: '', reportContact: '', reportPhone: '' })
const dispatchDialog = ref(false)
const dispatchMode = ref('dispatch')
const dispatchForm = reactive({ engineerId: undefined, remark: '' })
const finishDialog = ref(false)
const finishForm = reactive({
  report: '',
  rootCause: '',
  solution: '',
  partsReplaced: false,
  partsUsed: '',
  parts: [],
  imageUrl: ''
})
const partOptions = ref([])
const acceptDialog = ref(false)
const acceptDetail = ref(null)
const acceptLoading = ref(false)
const acceptForm = reactive({ result: '1', remark: '' })
const callbackDialog = ref(false)
const callbackForm = reactive({ score: 5, content: '' })
const current = ref(null)

const can = (p) => store.hasPerm('workorder:' + p)
function isAssignedEngineer(row) {
  return store.engineerId !== null && store.engineerId !== undefined
    && row.engineerId !== null && row.engineerId !== undefined
    && String(row.engineerId) === String(store.engineerId)
}
function statusText(s) { return { 0: '待受理', 1: '待派工', 2: '处理中', 3: '待验收', 4: '待回访', 5: '已完成', 6: '已取消' }[s] || s }
function statusType(s) { return { 0: 'danger', 1: 'warning', 2: 'primary', 3: 'warning', 4: 'info', 5: 'success', 6: 'info' }[s] || 'info' }
function levelText(l) { return { 0: 'P0 紧急', 1: 'P1 高', 2: 'P2 普通' }[l] || l }
function levelType(l) { return { 0: 'danger', 1: 'warning', 2: 'primary' }[l] || 'info' }
function actionText(a) { return { create: '创建', receive: '受理', dispatch: '派工', reassign: '改派', arrive: '到场', finish: '完工', accept: '验收', reject: '驳回', callback: '回访', cancel: '取消' }[a] || a }

async function load() {
  loading.value = true
  try {
    const res = await request.get(props.mine ? '/workorder/my' : '/workorder/page', { params: query })
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
function search() { query.pageNum = 1; load() }

async function exportCsv() {
  exporting.value = true
  try {
    const res = await request.get(props.mine ? '/workorder/my' : '/workorder/page', {
      params: { ...query, pageNum: 1, pageSize: 5000 }
    })
    const rows = res.data?.records || []
    if (!rows.length) {
      ElMessage.warning('当前筛选条件下没有数据')
      return
    }
    const cols = workorderColumns.filter((c) => isVisible(c.key) && (c.key !== 'hospitalName' || !props.mine))
    const headers = cols.map((c) => c.label)
    const data = rows.map((row) => cols.map((c) => {
      if (c.key === 'faultLevel') return levelText(row.faultLevel)
      if (c.key === 'status') return statusText(row.status)
      return row[c.key] ?? ''
    }))
    downloadCsv(`工单-${new Date().toISOString().slice(0, 10)}.csv`, headers, data)
  } finally {
    exporting.value = false
  }
}
function reset() {
  Object.assign(query, { pageNum: 1, status: 'open', level: undefined, hospitalId: undefined, keyword: undefined })
  load()
}

function openCreate() {
  Object.assign(createForm, {
    hospitalId: store.hospitalId || undefined,
    equipmentId: undefined,
    title: '',
    faultLevel: '2',
    faultDesc: '',
    reportContact: '',
    reportPhone: ''
  })
  loadEquipments()
  createDialog.value = true
}

async function loadEquipments() {
  if (!createForm.hospitalId) {
    equipments.value = []
    return
  }
  const res = await request.get('/crud/equipment/options', { params: { hospitalId: createForm.hospitalId } })
  equipments.value = res.data
}

function onHospitalChange() {
  createForm.equipmentId = undefined
  loadEquipments()
}
async function submitCreate() {
  if (!createForm.hospitalId || !createForm.title) { ElMessage.warning('请填写医院与标题'); return }
  await request.post('/workorder', createForm)
  ElMessage.success('工单已创建')
  createDialog.value = false
  load()
}

async function receive(row) {
  await ElMessageBox.confirm(`受理工单「${row.workorderNo}」？`, '提示', { type: 'warning' })
  await request.post(`/workorder/${row.workorderId}/receive`)
  ElMessage.success('已受理')
  load()
}

async function openDispatch(row) {
  current.value = row
  dispatchMode.value = 'dispatch'
  dispatchForm.engineerId = undefined
  dispatchForm.remark = ''
  dispatchDialog.value = true
  await loadDispatchCandidates(row)
}

async function openReassign(row) {
  current.value = row
  dispatchMode.value = 'reassign'
  dispatchForm.engineerId = undefined
  dispatchForm.remark = ''
  dispatchDialog.value = true
  await loadDispatchCandidates(row)
}

async function loadDispatchCandidates(row) {
  engineers.value = []
  dispatchCandidates.value = []
  try {
    const res = await request.get(`/dispatch/recommend/${row.workorderId}`)
    dispatchCandidates.value = res.data?.candidates || []
    engineers.value = dispatchCandidates.value.map((c) => ({
      value: c.engineer_id,
      label: `${c.engineer_name} · ${c.score}分`
    }))
    if (!engineers.value.length) ElMessage.warning('该工单所在区域暂无可用工程师')
  } catch (e) {
    ElMessage.warning('暂无可用工程师')
  }
}

async function submitDispatch() {
  if (!dispatchForm.engineerId) { ElMessage.warning('请选择工程师'); return }
  const url = dispatchMode.value === 'reassign'
    ? `/workorder/${current.value.workorderId}/reassign`
    : `/workorder/${current.value.workorderId}/dispatch`
  await request.post(url, dispatchForm)
  ElMessage.success(dispatchMode.value === 'reassign' ? '改派成功' : '派工成功')
  dispatchDialog.value = false
  load()
}

async function arrive(row) {
  await request.post(`/workorder/${row.workorderId}/arrive`)
  ElMessage.success('已签到')
  load()
}

function openFinish(row) {
  if (!row.arriveTime) { ElMessage.warning('请先到场签到，再提交完工'); return }
  current.value = row
  Object.assign(finishForm, {
    report: '',
    rootCause: '',
    solution: '',
    partsReplaced: false,
    partsUsed: '',
    parts: [],
    imageUrl: ''
  })
  finishDialog.value = true
}
async function submitFinish() {
  if (!finishForm.rootCause.trim()) { ElMessage.warning('请填写故障原因'); return }
  if (!finishForm.solution.trim()) { ElMessage.warning('请填写处理措施'); return }
  if (!finishForm.report.trim()) { ElMessage.warning('请填写维修报告'); return }
  if (finishForm.partsReplaced && !finishForm.partsUsed.trim()) { ElMessage.warning('请填写备件明细'); return }
  await request.post(`/workorder/${current.value.workorderId}/finish`, {
    ...finishForm,
    parts: finishForm.partsReplaced ? finishForm.parts.filter((p) => p.partId) : []
  })
  ElMessage.success('已提交验收')
  finishDialog.value = false
  load()
}

async function openAccept(row) {
  current.value = row
  acceptForm.result = '1'
  acceptForm.remark = ''
  acceptDetail.value = null
  acceptLoading.value = true
  acceptDialog.value = true
  try {
    const res = await request.get('/workorder/' + row.workorderId)
    acceptDetail.value = res.data?.workorder || res.data
  } finally {
    acceptLoading.value = false
  }
}
async function submitAccept() {
  await request.post(`/workorder/${current.value.workorderId}/accept`, acceptForm)
  ElMessage.success('验收已提交')
  acceptDialog.value = false
  load()
}

function openCallback(row) { current.value = row; callbackForm.score = 5; callbackForm.content = ''; callbackDialog.value = true }
async function submitCallback() {
  await request.post(`/workorder/${current.value.workorderId}/callback`, callbackForm)
  ElMessage.success('回访完成')
  callbackDialog.value = false
  load()
}

async function cancel(row) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消工单', { inputType: 'textarea' })
  await request.post(`/workorder/${row.workorderId}/cancel`, { reason: value })
  ElMessage.success('已取消')
  load()
}

async function openDetail(row) {
  detailDrawer.value = true
  const res = await request.get('/workorder/' + row.workorderId)
  detail.value = res.data
}

onMounted(async () => {
  const [h, d, p] = await Promise.all([
    request.get('/crud/hospital/options'),
    request.get('/system/dict/type/fault_level'),
    request.get('/crud/part/options')
  ])
  hospitals.value = h.data
  dicts.level = d.data
  partOptions.value = p.data || []
  load()
})
</script>

<style scoped>
.list-tools-bar { display: flex; justify-content: flex-end; margin: 6px 0 8px; }
.status-tabs { margin-bottom: 12px; }
.search-form { margin-bottom: 4px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.sec { margin: 20px 0 10px; font-size: 14px; color: #33465c; }
.candidate-score { margin: -2px 0 14px; padding: 12px; border: 1px solid #e6ebf2; border-radius: 8px; background: #f8fbff; }
.candidate-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; color: #33465c; }
.candidate-head strong { font-size: 20px; color: #0f766e; }
.dimension-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.dimension { display: flex; flex-direction: column; padding: 8px; border-radius: 6px; background: #fff; }
.dim-label { font-size: 11px; color: #8494a8; }
.dim-score { font-size: 18px; font-weight: 700; color: #1f2d3d; }
.dim-weight { font-size: 10px; color: #a7b4c5; }
.dimension-reasons { display: flex; flex-direction: column; gap: 3px; margin-top: 9px; font-size: 11px; color: #5b6b7d; line-height: 1.5; }
.finish-arrive { color: #5b6b7d; font-size: 13px; }
.accept-detail { min-height: 80px; }
.log-t { font-size: 13px; color: #2b3b50; }
.log-c { font-size: 12px; color: #7a8ba0; margin-top: 3px; }
</style>
