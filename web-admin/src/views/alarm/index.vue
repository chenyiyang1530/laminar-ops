<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>告警中心</h2>
          <div class="sub">分级告警 · 确认 · 处理 · 消缺 · 全链路追溯</div>
        </div>
        <div class="tabs">
          <el-radio-group v-model="query.status" size="small" @change="search">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="open">活动告警</el-radio-button>
            <el-radio-button label="0">未确认</el-radio-button>
            <el-radio-button label="1">已确认</el-radio-button>
            <el-radio-button label="2">处理中</el-radio-button>
            <el-radio-button label="3">已消缺</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <el-form inline class="search-form">
        <el-form-item label="级别">
          <el-select v-model="query.level" clearable placeholder="全部" style="width: 120px" @change="search">
            <el-option v-for="d in dicts.level" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="医院">
          <el-select v-model="query.hospitalId" clearable placeholder="全部" style="width: 200px" @change="search">
            <el-option v-for="h in hospitals" :key="h.value" :label="h.label" :value="h.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" clearable placeholder="告警编号 / 内容" style="width: 200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="list-tools-bar">
        <ListTools
          v-model="visibleCols"
          :columns="alarmColumns"
          :default-keys="alarmDefaultKeys"
          :exporting="exporting"
          @reset="resetCols"
          @export="exportCsv"
        />
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column v-if="isVisible('alarmNo')" prop="alarmNo" label="告警编号" min-width="170" />
        <el-table-column v-if="isVisible('level')" label="级别" min-width="80">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" effect="dark" size="small">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isVisible('equipmentName')" prop="equipmentName" label="设备" min-width="170" show-overflow-tooltip />
        <el-table-column v-if="isVisible('hospitalName')" prop="hospitalName" label="医院" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="isVisible('content')" prop="content" label="告警内容" min-width="260" show-overflow-tooltip />
        <el-table-column v-if="isVisible('status')" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isVisible('firstTime')" prop="firstTime" label="发生时间" min-width="160" />
        <el-table-column v-if="isVisible('duration')" label="持续时间" min-width="110">
          <template #default="{ row }">{{ fmtDuration(row.duration) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '0' && can('confirm')" link type="primary" @click="confirm(row)">确认</el-button>
            <el-button v-if="['0','1'].includes(row.status) && can('handle')" link type="warning" @click="openHandle(row)">处理</el-button>
            <el-button v-if="['0','1','2'].includes(row.status) && can('close')" link type="success" @click="openClose(row)">消缺</el-button>
            <el-button v-if="['0','1','2'].includes(row.status) && can('ignore')" link type="info" @click="openIgnore(row)">忽略</el-button>
            <el-button link @click="openDetail(row)">追溯</el-button>
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

    <el-dialog v-model="closeDialog" title="告警消缺" width="560px">
      <el-form label-width="90px">
        <el-form-item label="根本原因" required>
          <el-input v-model="closeForm.rootCause" type="textarea" :rows="2" placeholder="如：初效过滤器堵塞导致风量不足" />
        </el-form-item>
        <el-form-item label="处理措施">
          <el-input v-model="closeForm.solution" type="textarea" :rows="2" placeholder="更换过滤器并复测压差" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitClose">确认消缺</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleDialog" title="开始处理" width="480px">
      <el-input v-model="handleRemark" type="textarea" :rows="2" placeholder="处理说明（可选）" />
      <template #footer>
        <el-button @click="handleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ignoreDialog" title="忽略告警" width="480px">
      <el-input v-model="ignoreReason" type="textarea" :rows="2" placeholder="忽略原因（必填）" />
      <template #footer>
        <el-button @click="ignoreDialog = false">取消</el-button>
        <el-button type="primary" @click="submitIgnore">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" size="560px" title="告警追溯">
      <div v-if="detail" class="detail">
        <div class="d-title">
          <el-tag :type="levelType(detail.alarm.level)" effect="dark">{{ levelText(detail.alarm.level) }}</el-tag>
          <span>{{ detail.alarm.alarm_no }}</span>
          <el-tag :type="statusType(detail.alarm.status)" effect="plain" size="small">{{ statusText(detail.alarm.status) }}</el-tag>
        </div>
        <div v-if="store.hasPerm('workorder:add')" class="d-actions">
          <el-button size="small" type="primary" plain :disabled="!!detail.alarm.workorder_id" @click="toWorkorder">
            {{ detail.alarm.workorder_id ? '已转工单 #' + detail.alarm.workorder_id : '一键转工单' }}
          </el-button>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="设备">{{ detail.alarm.equipment_name }}</el-descriptions-item>
          <el-descriptions-item label="位置">{{ detail.alarm.hospital_name }} / {{ detail.alarm.campus_name }} / {{ detail.alarm.area_name }}</el-descriptions-item>
          <el-descriptions-item label="内容">{{ detail.alarm.content }}</el-descriptions-item>
          <el-descriptions-item label="首次发生">{{ detail.alarm.first_time }}</el-descriptions-item>
          <el-descriptions-item label="确认">{{ detail.alarm.confirm_by || '—' }} {{ detail.alarm.confirm_time || '' }}</el-descriptions-item>
          <el-descriptions-item label="消缺">{{ detail.alarm.close_by || '—' }} {{ detail.alarm.close_time || '' }}</el-descriptions-item>
          <el-descriptions-item label="根本原因">{{ detail.alarm.root_cause || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处理措施">{{ detail.alarm.solution || '—' }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="timeline-title">处理时间线</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(l, i) in detail.logs"
            :key="i"
            :timestamp="l.createTime"
            :type="l.toStatus === '3' ? 'success' : l.toStatus === '4' ? 'info' : 'primary'"
          >
            <div class="log-title">{{ actionText(l.action) }} · {{ l.operatorName }}</div>
            <div class="log-content">{{ l.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import ListTools from '@/components/ListTools.vue'
import { useColumnPrefs } from '@/composables/useColumnPrefs'
import { downloadCsv } from '@/utils/csv'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const alarmColumns = [
  { key: 'alarmNo', label: '告警编号' },
  { key: 'level', label: '级别' },
  { key: 'equipmentName', label: '设备' },
  { key: 'hospitalName', label: '医院' },
  { key: 'content', label: '告警内容' },
  { key: 'status', label: '状态' },
  { key: 'firstTime', label: '发生时间' },
  { key: 'duration', label: '持续时间' }
]
const alarmDefaultKeys = alarmColumns.map((c) => c.key)
const { visible: visibleCols, reset: resetCols } = useColumnPrefs('laminar_cols_alarm', alarmDefaultKeys)
const exporting = ref(false)
const isVisible = (key) => visibleCols.value.includes(key)

const store = useUserStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const hospitals = ref([])
const dicts = reactive({ level: [] })
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, level: undefined, status: 'open', hospitalId: undefined, keyword: undefined })
const detailDrawer = ref(false)
const detail = ref(null)
const closeDialog = ref(false)
const closeForm = reactive({ rootCause: '', solution: '' })
const current = ref(null)
const handleDialog = ref(false)
const handleRemark = ref('')
const ignoreDialog = ref(false)
const ignoreReason = ref('')

const can = (a) => store.hasPerm('alarm:' + a)

function levelText(l) { return { 1: '紧急', 2: '重要', 3: '一般', 4: '提示' }[l] || '一般' }
function levelType(l) { return { 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }[l] || 'info' }
function statusText(s) { return { 0: '未确认', 1: '已确认', 2: '处理中', 3: '已消缺', 4: '已忽略' }[s] || s }
function statusType(s) { return { 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }[s] || 'info' }
function actionText(a) { return { create: '生成告警', confirm: '确认', handle: '开始处理', close: '消缺', ignore: '忽略', dispatch: '派单' }[a] || a }
function fmtDuration(sec) {
  if (!sec) return '—'
  if (sec < 60) return sec + ' 秒'
  if (sec < 3600) return Math.floor(sec / 60) + ' 分'
  return Math.floor(sec / 3600) + ' 时 ' + Math.floor((sec % 3600) / 60) + ' 分'
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/alarm/page', { params: query })
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
    const res = await request.get('/alarm/page', { params: { ...query, pageNum: 1, pageSize: 5000 } })
    const rows = res.data?.records || []
    if (!rows.length) {
      ElMessage.warning('当前筛选条件下没有数据')
      return
    }
    const cols = alarmColumns.filter((c) => isVisible(c.key))
    const headers = cols.map((c) => c.label)
    const data = rows.map((row) => cols.map((c) => {
      if (c.key === 'level') return levelText(row.level)
      if (c.key === 'status') return statusText(row.status)
      if (c.key === 'duration') return fmtDuration(row.duration)
      return row[c.key] ?? ''
    }))
    downloadCsv(`告警-${new Date().toISOString().slice(0, 10)}.csv`, headers, data)
  } finally {
    exporting.value = false
  }
}
function reset() {
  Object.assign(query, { pageNum: 1, level: undefined, status: 'open', hospitalId: undefined, keyword: undefined })
  load()
}

async function confirm(row) {
  await ElMessageBox.confirm(`确认告警「${row.alarmNo}」？`, '确认', { type: 'warning' })
  await request.post(`/alarm/${row.alarmId}/confirm`, {})
  ElMessage.success('已确认')
  load()
}

function openHandle(row) { current.value = row; handleRemark.value = ''; handleDialog.value = true }
async function submitHandle() {
  await request.post(`/alarm/${current.value.alarmId}/handle`, { remark: handleRemark.value })
  ElMessage.success('已开始处理')
  handleDialog.value = false
  load()
}

function openClose(row) { current.value = row; closeForm.rootCause = ''; closeForm.solution = ''; closeDialog.value = true }
async function submitClose() {
  if (!closeForm.rootCause.trim()) { ElMessage.warning('请填写根本原因'); return }
  await request.post(`/alarm/${current.value.alarmId}/close`, closeForm)
  ElMessage.success('已消缺')
  closeDialog.value = false
  load()
}

function openIgnore(row) { current.value = row; ignoreReason.value = ''; ignoreDialog.value = true }
async function submitIgnore() {
  if (!ignoreReason.value.trim()) { ElMessage.warning('请填写忽略原因'); return }
  await request.post(`/alarm/${current.value.alarmId}/ignore`, { reason: ignoreReason.value })
  ElMessage.success('已忽略')
  ignoreDialog.value = false
  load()
}

async function openDetail(row) {
  detailDrawer.value = true
  const res = await request.get('/alarm/' + row.alarmId)
  detail.value = res.data
}

async function toWorkorder() {
  const res = await request.post('/workorder/from-alarm/' + detail.value.alarm.alarm_id)
  ElMessage.success('已生成工单：' + res.data)
  detail.value.alarm.workorder_id = res.data
}

onMounted(async () => {
  const [h, d] = await Promise.all([request.get('/crud/hospital/options'), request.get('/system/dict/type/alarm_level')])
  hospitals.value = h.data
  dicts.level = d.data
  load()
})
</script>

<style scoped>
.list-tools-bar { display: flex; justify-content: flex-end; margin: 6px 0 8px; }
.tabs { display: flex; align-items: center; }
.search-form { margin-bottom: 4px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.d-title { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; font-weight: 600; }
.d-actions { margin-bottom: 12px; }
.timeline-title { margin: 22px 0 12px; font-size: 14px; color: #33465c; }
.log-title { font-size: 13px; color: #2b3b50; }
.log-content { font-size: 12px; color: #7a8ba0; margin-top: 3px; }
</style>
