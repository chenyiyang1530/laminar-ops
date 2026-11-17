<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>文件巡检报告</h2>
          <div class="sub">扫描业务文件与上传目录，记录历史趋势，支持丢失文件重新上传</div>
        </div>
        <div>
          <el-button :loading="running" type="primary" :disabled="!canRun" @click="runNow">立即巡检</el-button>
          <el-button @click="exportFile('xlsx')">导出 Excel</el-button>
          <el-button @click="exportFile('pdf')">导出 PDF</el-button>
          <el-button @click="loadAll">刷新</el-button>
        </div>
      </div>

      <div class="trend-box">
        <div class="trend-head">最近 30 天趋势</div>
        <EChart :option="trendOption" height="220px" />
      </div>

      <el-tabs v-model="active" @tab-change="onTabChange">
        <el-tab-pane label="巡检记录" name="runs">
          <el-table v-loading="runLoading" :data="runs" stripe>
            <el-table-column prop="runId" label="批次" width="90" />
            <el-table-column prop="triggerType" label="触发方式" width="110">
              <template #default="{ row }">{{ row.triggerType === 'scheduled' ? '定时' : '手动' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'" size="small">
                  {{ row.status === 'success' ? '成功' : row.status === 'failed' ? '失败' : '执行中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="checked" label="检查数" width="100" />
            <el-table-column prop="missing" label="丢失数" width="100">
              <template #default="{ row }"><span :class="{ danger: row.missing > 0 }">{{ row.missing }}</span></template>
            </el-table-column>
            <el-table-column prop="recovered" label="恢复数" width="100" />
            <el-table-column prop="startTime" label="开始时间" min-width="170" />
            <el-table-column prop="endTime" label="结束时间" min-width="170" />
            <el-table-column prop="errorMsg" label="错误" min-width="180" show-overflow-tooltip />
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="runPage" :page-size="runSize" :total="runTotal" layout="total, prev, pager, next" @current-change="loadRuns" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="丢失文件" name="items">
          <el-form inline>
            <el-form-item label="状态">
              <el-select v-model="filters.status" clearable placeholder="全部" style="width: 110px" @change="searchItems">
                <el-option label="丢失" value="0" />
                <el-option label="已恢复" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="公司">
              <el-select v-model="filters.companyId" clearable filterable placeholder="全部" style="width: 170px" @change="searchItems">
                <el-option v-for="c in companies" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="医院">
              <el-select v-model="filters.hospitalId" clearable filterable placeholder="全部" style="width: 170px" @change="searchItems">
                <el-option v-for="h in hospitals" :key="h.value" :label="h.label" :value="h.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="filters.fileType" clearable placeholder="全部" style="width: 120px" @change="searchItems">
                <el-option label="文档" value="document" />
                <el-option label="图片" value="image" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="等级">
              <el-select v-model="filters.severity" clearable placeholder="全部" style="width: 100px" @change="searchItems">
                <el-option label="P1" value="P1" />
                <el-option label="P2" value="P2" />
                <el-option label="P3" value="P3" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键字">
              <el-input v-model="filters.keyword" clearable placeholder="路径 / 表名" style="width: 170px" @keyup.enter="searchItems" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchItems">查询</el-button>
              <el-button @click="resetItems">重置</el-button>
              <el-button :disabled="!selection.length || !canEdit" @click="openBatch">批量恢复</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="itemLoading" :data="items" stripe @selection-change="onSelection">
            <el-table-column type="selection" width="46" :selectable="(row) => row.status === '0'" />
            <el-table-column prop="severity" label="等级" width="74">
              <template #default="{ row }">
                <el-tag :type="row.severity === 'P1' ? 'danger' : row.severity === 'P2' ? 'warning' : 'info'" size="small">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tableName" label="表" width="180" show-overflow-tooltip />
            <el-table-column prop="recordId" label="记录ID" width="80" />
            <el-table-column prop="fileColumn" label="字段" width="110" />
            <el-table-column prop="fileType" label="类型" width="80" />
            <el-table-column prop="companyName" label="公司" min-width="140" show-overflow-tooltip />
            <el-table-column prop="hospitalName" label="医院" min-width="140" show-overflow-tooltip />
            <el-table-column prop="filePath" label="文件路径" min-width="230" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="88">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'danger' : 'success'" size="small">{{ row.status === '0' ? '丢失' : '已恢复' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="SLA" width="150">
              <template #default="{ row }">
                <span v-if="row.status === '1'">已恢复</span>
                <span v-else :class="{ danger: isOverdue(row) }">{{ row.slaDeadline ? `截止 ${String(row.slaDeadline).slice(5, 16)}` : '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === '0' && canEdit" link type="primary" @click="openRecover(row)">重新上传</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="itemPage" :page-size="itemSize" :total="itemTotal" layout="total, prev, pager, next" @current-change="loadItems" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="recoverVisible" title="重新上传文件" width="560px">
      <el-alert :title="`将替换：${recoverItem?.tableName} #${recoverItem?.recordId} ${recoverItem?.fileColumn}`" type="info" :closable="false" show-icon style="margin-bottom: 14px" />
      <FileUpload v-model="recoverFile" category="document" />
      <template #footer>
        <el-button @click="recoverVisible = false">取消</el-button>
        <el-button type="primary" :loading="recovering" :disabled="!recoverFile" @click="submitRecover">上传并恢复</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量恢复文件" width="760px">
      <el-alert v-if="batchFailures.length" type="error" :closable="false" show-icon style="margin-bottom: 10px">
        {{ batchFailures.length }} 个文件恢复失败，可重试或关闭后重新选择
      </el-alert>
      <el-table :data="batchRows" size="small">
        <el-table-column prop="severity" label="等级" width="66" />
        <el-table-column prop="tableName" label="表" width="150" />
        <el-table-column prop="recordId" label="记录" width="70" />
        <el-table-column label="新文件" min-width="300">
          <template #default="{ row }"><FileUpload v-model="row.recoverFile" category="document" /></template>
        </el-table-column>
        <el-table-column label="结果" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.batchError" type="danger" size="small">失败</el-tag>
            <el-tag v-else-if="row.batchOk" type="success" size="small">成功</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="submitBatch">
          {{ batchFailures.length ? '重试失败项' : '批量恢复' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import FileUpload from '@/components/FileUpload.vue'
import EChart from '@/components/EChart.vue'
import { useUserStore } from '@/store/user'
import { useRoute } from 'vue-router'

const store = useUserStore()
const route = useRoute()
const canRun = computed(() => store.hasPerm('system:filecheck:run'))
const canEdit = computed(() => store.hasPerm('system:filecheck:edit'))

const active = ref('runs')
const running = ref(false)
const runLoading = ref(false)
const runs = ref([])
const runPage = ref(1)
const runSize = ref(20)
const runTotal = ref(0)

const itemLoading = ref(false)
const items = ref([])
const itemPage = ref(1)
const itemSize = ref(20)
const itemTotal = ref(0)
const selection = ref([])
const companies = ref([])
const hospitals = ref([])
const filters = reactive({ status: '', companyId: undefined, hospitalId: undefined, fileType: '', severity: '', keyword: '', itemId: undefined })
const trend = ref([])

const recoverVisible = ref(false)
const recoverItem = ref(null)
const recoverFile = ref('')
const recovering = ref(false)
const batchVisible = ref(false)
const batchRows = ref([])
const batchFailures = ref([])
const batchSaving = ref(false)

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['丢失', '恢复'], right: 10 },
  grid: { left: 40, right: 20, top: 35, bottom: 28 },
  xAxis: { type: 'category', data: trend.value.map((x) => x.date.slice(5)) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    { name: '丢失', type: 'line', smooth: true, data: trend.value.map((x) => x.missing), itemStyle: { color: '#f56c6c' }, areaStyle: { opacity: 0.08 } },
    { name: '恢复', type: 'line', smooth: true, data: trend.value.map((x) => x.recovered), itemStyle: { color: '#67c23a' }, areaStyle: { opacity: 0.08 } }
  ]
}))

function isOverdue(row) {
  return row.slaDeadline && new Date(row.slaDeadline).getTime() < Date.now()
}

async function loadTrend() {
  try {
    const res = await request.get('/system/file-check/trend', {
      params: { days: 30, companyId: filters.companyId, hospitalId: filters.hospitalId }
    })
    trend.value = res.data || []
  } catch {
    trend.value = []
  }
}

async function loadRuns() {
  runLoading.value = true
  try {
    const res = await request.get('/system/file-check/runs', { params: { pageNum: runPage.value, pageSize: runSize.value } })
    runs.value = res.data.records || []
    runTotal.value = res.data.total || 0
  } finally {
    runLoading.value = false
  }
}

async function loadItems() {
  itemLoading.value = true
  try {
    const res = await request.get('/system/file-check/items', {
      params: {
        pageNum: itemPage.value,
        pageSize: itemSize.value,
        status: filters.status,
        keyword: filters.keyword,
        companyId: filters.companyId,
        hospitalId: filters.hospitalId,
        fileType: filters.fileType,
        severity: filters.severity,
        itemId: filters.itemId
      }
    })
    items.value = res.data.records || []
    itemTotal.value = res.data.total || 0
  } finally {
    itemLoading.value = false
  }
}

function searchItems() {
  itemPage.value = 1
  loadItems()
  loadTrend()
}

function resetItems() {
  filters.status = ''
  filters.companyId = undefined
  filters.hospitalId = undefined
  filters.fileType = ''
  filters.severity = ''
  filters.keyword = ''
  filters.itemId = undefined
  searchItems()
}

function onTabChange(name) {
  if (name === 'items') loadItems()
}

function onSelection(rows) {
  selection.value = rows
}

async function loadAll() {
  await Promise.all([loadTrend(), loadRuns(), loadItems()])
}

async function loadOptions() {
  try {
    const [c, h] = await Promise.all([request.get('/crud/company/options'), request.get('/crud/hospital/options')])
    companies.value = c.data || []
    hospitals.value = h.data || []
  } catch { /* 无选项权限时保留手工筛选能力 */ }
}

async function runNow() {
  running.value = true
  try {
    const res = await request.post('/system/file-check/run')
    if (res.data?.status === 'running') {
      ElMessage.warning('已有巡检任务正在执行')
      return
    }
    ElMessage.success(`巡检完成：丢失 ${res.data?.missing || 0} 个`)
    runPage.value = 1
    itemPage.value = 1
    await loadAll()
  } finally {
    running.value = false
  }
}

function openRecover(row) {
  recoverItem.value = row
  recoverFile.value = ''
  recoverVisible.value = true
}

async function submitRecover() {
  if (!recoverFile.value) return
  recovering.value = true
  try {
    await request.post(`/system/file-check/items/${recoverItem.value.itemId}/recover`, { fileUrl: recoverFile.value })
    ElMessage.success('文件已恢复，状态已刷新')
    recoverVisible.value = false
    await loadAll()
  } finally {
    recovering.value = false
  }
}

function openBatch() {
  batchRows.value = selection.value.map((row) => ({ ...row, recoverFile: '', batchError: '', batchOk: false }))
  batchFailures.value = []
  batchVisible.value = true
}

async function submitBatch() {
  const ready = batchRows.value.filter((row) => row.recoverFile && !row.batchOk)
  if (!ready.length) {
    ElMessage.warning('请至少上传一个文件')
    return
  }
  batchSaving.value = true
  try {
    let ok = 0
    const failed = []
    for (const row of ready) {
      try {
        await request.post(`/system/file-check/items/${row.itemId}/recover`, { fileUrl: row.recoverFile })
        row.batchOk = true
        row.batchError = ''
        ok++
      } catch (e) {
        row.batchOk = false
        row.batchError = e?.response?.data?.msg || e?.message || '恢复失败'
        failed.push(row)
      }
    }
    batchFailures.value = failed
    if (failed.length) {
      ElMessage.warning(`成功 ${ok} 个，失败 ${failed.length} 个，可重试失败项`)
      await loadAll()
    } else {
      ElMessage.success(`已恢复 ${ok} 个文件`)
      batchVisible.value = false
      await loadAll()
    }
  } finally {
    batchSaving.value = false
  }
}

async function exportFile(format) {
  try {
    const res = await request.get('/system/file-check/export', {
      params: {
        format,
        status: filters.status,
        keyword: filters.keyword,
        companyId: filters.companyId,
        hospitalId: filters.hospitalId,
        fileType: filters.fileType,
        severity: filters.severity
      },
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `file-check-${new Date().toISOString().slice(0, 10)}.${format}`
    link.click()
    URL.revokeObjectURL(link.href)
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(async () => {
  if (route.query.itemId) {
    filters.itemId = Number(route.query.itemId)
    active.value = 'items'
  }
  await loadOptions()
  await loadAll()
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.danger { color: #f56c6c; font-weight: 600; }
.trend-box { margin: 4px 0 12px; padding: 10px 14px 0; border: 1px solid #eef2f7; border-radius: 10px; background: #fff; }
.trend-head { font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 2px; }
</style>
