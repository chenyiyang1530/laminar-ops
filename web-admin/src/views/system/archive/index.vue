<template>
  <div class="app-container">
    <div class="page-card archive-page">
      <div class="page-title">
        <div>
          <h2>数据归档</h2>
          <div class="sub">将历史已关闭的告警和工单移入归档表，保持业务表轻量</div>
        </div>
        <el-button :icon="Refresh" @click="loadStats">刷新</el-button>
      </div>

      <el-alert
        title="只有已消缺/已忽略的告警、已完成/已取消的工单才会归档"
        description="归档会在同一事务内复制到归档表后再从业务表删除；处理中的数据不会受影响。"
        type="warning"
        :closable="false"
        show-icon
        class="archive-alert"
      />

      <div class="toolbar">
        <span class="label">保留最近</span>
        <el-select v-model="months" style="width: 120px" @change="loadStats">
          <el-option v-for="m in monthOptions" :key="m" :label="`${m} 个月`" :value="m" />
        </el-select>
        <span class="label">之前的数据</span>
      </div>

      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">可归档告警</div>
          <div class="stat-value">{{ stats.alarmEligible ?? 0 }}</div>
          <div class="stat-desc">已消缺 / 已忽略</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">可归档工单</div>
          <div class="stat-value">{{ stats.workorderEligible ?? 0 }}</div>
          <div class="stat-desc">已完成 / 已取消</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已归档告警</div>
          <div class="stat-value">{{ stats.alarmArchived ?? 0 }}</div>
          <div class="stat-desc">归档表累计</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已归档工单</div>
          <div class="stat-value">{{ stats.workorderArchived ?? 0 }}</div>
          <div class="stat-desc">归档表累计</div>
        </div>
      </div>

      <div class="run-card">
          <div class="run-head">
            <div>
              <h3>执行归档</h3>
              <p>按保留时间归档截止：{{ stats.cutoff || '—' }}</p>
            </div>
            <div class="run-actions">
              <el-button :loading="running" @click="runNow">立即归档</el-button>
              <el-button type="primary" :loading="running" @click="run">开始归档</el-button>
            </div>
          </div>
          <div class="run-options">
            <el-checkbox v-model="alarms" :disabled="!stats.alarmClosed && !stats.alarmEligible">归档告警（当前已关闭 {{ stats.alarmClosed ?? stats.alarmEligible ?? 0 }} 条）</el-checkbox>
            <el-checkbox v-model="workorders" :disabled="!stats.workorderClosed && !stats.workorderEligible">归档工单（当前已完成 {{ stats.workorderClosed ?? stats.workorderEligible ?? 0 }} 条）</el-checkbox>
          </div>
      </div>

      <div class="archive-note">
        <div class="note-title">归档后</div>
        <ul>
          <li>告警/工单从业务列表消失，不再参与工作台统计；</li>
          <li>数据仍保留在归档表，可按需查询或恢复；</li>
          <li>建议按季度执行，保留最近 3-6 个月。</li>
        </ul>
      </div>

      <div class="archive-query">
        <div class="query-head">
          <div>
            <h3>归档数据</h3>
            <p>查询历史归档记录，可恢复到业务表</p>
          </div>
          <el-button :icon="Refresh" @click="loadArchived">刷新</el-button>
        </div>
        <el-tabs v-model="archiveTab" @tab-change="loadArchived">
          <el-tab-pane label="归档告警" name="alarms">
            <div class="query-toolbar">
              <el-input v-model="alarmKeyword" clearable placeholder="搜索告警编号或内容" style="width: 260px" @keyup.enter="loadArchived" />
              <el-button type="primary" @click="loadArchived">查询</el-button>
              <el-button :disabled="!alarmSelection.length" @click="batchRestore('alarms')">批量恢复（{{ alarmSelection.length }}）</el-button>
            </div>
            <el-table :data="alarmRecords" size="small" stripe @selection-change="alarmSelection = $event">
              <el-table-column type="selection" width="42" />
              <el-table-column prop="alarmNo" label="告警编号" width="160" />
              <el-table-column prop="content" label="告警内容" min-width="260" show-overflow-tooltip />
              <el-table-column prop="equipmentName" label="设备" width="150" show-overflow-tooltip />
              <el-table-column prop="hospitalName" label="医院" width="170" show-overflow-tooltip />
              <el-table-column prop="firstTime" label="发生时间" width="150" />
              <el-table-column prop="archivedAt" label="归档时间" width="150" />
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="restoreAlarm(row)">恢复</el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无归档告警" :image-size="44" /></template>
            </el-table>
            <el-pagination v-model:current-page="alarmQuery.pageNum" v-model:page-size="alarmQuery.pageSize" :total="alarmTotal" layout="total, prev, pager, next" class="pager" @change="loadArchived" />
          </el-tab-pane>
          <el-tab-pane label="归档工单" name="workorders">
            <div class="query-toolbar">
              <el-input v-model="workorderKeyword" clearable placeholder="搜索工单号或标题" style="width: 260px" @keyup.enter="loadArchived" />
              <el-button type="primary" @click="loadArchived">查询</el-button>
              <el-button :disabled="!workorderSelection.length" @click="batchRestore('workorders')">批量恢复（{{ workorderSelection.length }}）</el-button>
            </div>
            <el-table :data="workorderRecords" size="small" stripe @selection-change="workorderSelection = $event">
              <el-table-column type="selection" width="42" />
              <el-table-column prop="workorderNo" label="工单号" width="160" />
              <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
              <el-table-column prop="hospitalName" label="医院" width="170" show-overflow-tooltip />
              <el-table-column prop="companyName" label="服务公司" width="180" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" width="150" />
              <el-table-column prop="archivedAt" label="归档时间" width="150" />
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="restoreWorkorder(row)">恢复</el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无归档工单" :image-size="44" /></template>
            </el-table>
            <el-pagination v-model:current-page="workorderQuery.pageNum" v-model:page-size="workorderQuery.pageSize" :total="workorderTotal" layout="total, prev, pager, next" class="pager" @change="loadArchived" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()

const monthOptions = [1, 3, 6, 12]
const months = ref(1)
const alarms = ref(true)
const workorders = ref(true)
const running = ref(false)
const stats = reactive({})
const archiveTab = ref('alarms')
const alarmKeyword = ref('')
const workorderKeyword = ref('')
const alarmRecords = ref([])
const workorderRecords = ref([])
const alarmSelection = ref([])
const workorderSelection = ref([])
const alarmTotal = ref(0)
const workorderTotal = ref(0)
const alarmQuery = reactive({ pageNum: 1, pageSize: store.pageSize || 10 })
const workorderQuery = reactive({ pageNum: 1, pageSize: store.pageSize || 10 })

async function loadStats() {
  const res = await request.get('/archive/stats', { params: { months: months.value } })
  Object.assign(stats, res.data || {})
  alarms.value = (stats.alarmClosed || stats.alarmEligible || 0) > 0
  workorders.value = (stats.workorderClosed || stats.workorderEligible || 0) > 0
}

async function loadArchived() {
  if (archiveTab.value === 'alarms') {
    const res = await request.get('/archive/alarms', {
      params: { ...alarmQuery, keyword: alarmKeyword.value }
    })
    alarmRecords.value = res.data.records || []
    alarmTotal.value = res.data.total || 0
  } else {
    const res = await request.get('/archive/workorders', {
      params: { ...workorderQuery, keyword: workorderKeyword.value }
    })
    workorderRecords.value = res.data.records || []
    workorderTotal.value = res.data.total || 0
  }
}

async function restoreAlarm(row) {
  await ElMessageBox.confirm(`确认将告警「${row.alarmNo}」恢复到业务表？`, '恢复归档', { type: 'warning' })
  await request.post(`/archive/alarms/${row.alarmId}/restore`)
  ElMessage.success('已恢复')
  await loadStats()
  await loadArchived()
}

async function restoreWorkorder(row) {
  await ElMessageBox.confirm(`确认将工单「${row.workorderNo}」恢复到业务表？`, '恢复归档', { type: 'warning' })
  await request.post(`/archive/workorders/${row.workorderId}/restore`)
  ElMessage.success('已恢复')
  await loadStats()
  await loadArchived()
}

async function run() {
  if (!alarms.value && !workorders.value) {
    ElMessage.warning('请至少选择一种数据类型')
    return
  }
  await ElMessageBox.confirm(
    `确认归档 ${months.value} 个月前的数据？\n告警 ${stats.alarmEligible || 0} 条，工单 ${stats.workorderEligible || 0} 条。`,
    '确认归档',
    { type: 'warning' }
  )
  running.value = true
  try {
    const res = await request.post('/archive/run', {
      months: months.value,
      alarms: alarms.value,
      workorders: workorders.value
    })
    ElMessage.success(`归档完成：告警 ${res.data.alarmArchived || 0} 条，工单 ${res.data.workorderArchived || 0} 条`)
    await loadStats()
    await loadArchived()
  } finally {
    running.value = false
  }
}

async function runNow() {
  if (!alarms.value && !workorders.value) {
    ElMessage.warning('请至少选择一种数据类型')
    return
  }
  await ElMessageBox.confirm(
    `确认立即归档所有已关闭数据？\n告警 ${stats.alarmClosed || 0} 条，工单 ${stats.workorderClosed || 0} 条。`,
    '立即归档',
    { type: 'warning' }
  )
  running.value = true
  try {
    const res = await request.post('/archive/run-now', {
      alarms: alarms.value,
      workorders: workorders.value
    })
    ElMessage.success(`立即归档完成：告警 ${res.data.alarmArchived || 0} 条，工单 ${res.data.workorderArchived || 0} 条`)
    await loadStats()
    await loadArchived()
  } finally {
    running.value = false
  }
}

async function batchRestore(type) {
  const rows = type === 'alarms' ? alarmSelection.value : workorderSelection.value
  const ids = rows.map((row) => type === 'alarms' ? row.alarmId : row.workorderId)
  if (!ids.length) return
  await ElMessageBox.confirm(`确认恢复选中的 ${ids.length} 条归档数据？`, '批量恢复', { type: 'warning' })
  const url = type === 'alarms' ? '/archive/alarms/restore-batch' : '/archive/workorders/restore-batch'
  const res = await request.post(url, { ids })
  if (res.data.failed) {
    ElMessage.warning(`恢复完成：成功 ${res.data.success} 条，失败 ${res.data.failed} 条`)
  } else {
    ElMessage.success(`已恢复 ${res.data.success} 条`)
  }
  if (type === 'alarms') alarmSelection.value = []
  else workorderSelection.value = []
  await loadStats()
  await loadArchived()
}

onMounted(async () => {
  await loadStats()
  await loadArchived()
})
</script>

<style scoped>
.archive-page { max-width: 980px; }
.archive-alert { margin-bottom: 16px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.label { font-size: 13px; color: #5b6b7d; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat-card { border: 1px solid #e6ebf2; border-radius: 10px; padding: 14px 16px; background: #fff; }
.stat-label { font-size: 12px; color: #8a99ab; }
.stat-value { font-size: 26px; font-weight: 600; color: #1f2d3d; margin: 7px 0 3px; }
.stat-desc { font-size: 11px; color: #a2afbf; }
.run-card { margin-top: 16px; border: 1px solid #e6ebf2; border-radius: 10px; padding: 16px; background: #fbfdff; }
.run-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.run-head h3 { margin: 0; font-size: 14px; color: #1f2d3d; }
.run-head p { margin: 5px 0 0; font-size: 12px; color: #93a1b5; }
.run-actions { display: flex; gap: 8px; }
.run-options { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 14px; }
.archive-note { margin-top: 18px; font-size: 12px; color: #7b8a9d; }
.note-title { font-weight: 600; color: #5b6b7d; margin-bottom: 6px; }
.archive-note ul { margin: 0; padding-left: 18px; line-height: 1.9; }
.archive-query { margin-top: 24px; padding-top: 18px; border-top: 1px solid #eef2f7; }
.query-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 8px; }
.query-head h3 { margin: 0; font-size: 15px; color: #1f2d3d; }
.query-head p { margin: 5px 0 0; font-size: 12px; color: #93a1b5; }
.query-toolbar { display: flex; gap: 10px; margin: 4px 0 12px; }
.pager { margin-top: 12px; justify-content: flex-end; }
@media (max-width: 900px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
