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
          <el-button @click="loadAll">刷新</el-button>
        </div>
      </div>

      <el-tabs v-model="active">
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
              <el-select v-model="itemStatus" clearable placeholder="全部" style="width: 130px" @change="searchItems">
                <el-option label="丢失" value="0" />
                <el-option label="已恢复" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键字">
              <el-input v-model="keyword" clearable placeholder="路径 / 表名" style="width: 230px" @keyup.enter="searchItems" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchItems">查询</el-button>
              <el-button @click="resetItems">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="itemLoading" :data="items" stripe>
            <el-table-column prop="tableName" label="表" width="190" />
            <el-table-column prop="recordId" label="记录ID" width="90" />
            <el-table-column prop="fileColumn" label="字段" width="120" />
            <el-table-column prop="companyName" label="公司" min-width="150" show-overflow-tooltip />
            <el-table-column prop="hospitalName" label="医院" min-width="150" show-overflow-tooltip />
            <el-table-column prop="filePath" label="文件路径" min-width="260" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'danger' : 'success'" size="small">{{ row.status === '0' ? '丢失' : '已恢复' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastSeen" label="最近发现" min-width="170" />
            <el-table-column label="操作" width="130" fixed="right">
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import FileUpload from '@/components/FileUpload.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
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
const itemStatus = ref('')
const keyword = ref('')

const recoverVisible = ref(false)
const recoverItem = ref(null)
const recoverFile = ref('')
const recovering = ref(false)

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
      params: { pageNum: itemPage.value, pageSize: itemSize.value, status: itemStatus.value, keyword: keyword.value }
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
}

function resetItems() {
  itemStatus.value = ''
  keyword.value = ''
  searchItems()
}

async function loadAll() {
  await Promise.all([loadRuns(), loadItems()])
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

onMounted(loadAll)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.danger { color: #f56c6c; font-weight: 600; }
</style>
