<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>审批配置</h2>
          <div class="sub">按请假类型设置升级天数与是否需要总区域负责人审批</div>
        </div>
        <div class="head-actions">
          <el-select v-model="regionId" style="width: 180px" @change="load">
            <el-option label="全公司" :value="0" />
            <el-option v-for="r in regions" :key="r.regionId" :label="r.regionName" :value="r.regionId" />
          </el-select>
          <el-button @click="openVersions">版本管理</el-button>
          <el-button :loading="copying" @click="openCopy">复制到区域</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rules" stripe>
        <el-table-column label="请假类型" width="160">
          <template #default="{ row }">{{ typeText(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column label="超过多少天升级" width="220">
          <template #default="{ row }">
            <el-input-number v-model="row.thresholdDays" :min="0" :max="30" controls-position="right" />
            <span class="unit">天</span>
          </template>
        </el-table-column>
        <el-table-column label="需要总区域负责人审批" width="220">
          <template #default="{ row }">
            <el-switch v-model="row.secondLevel" active-value="1" inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="260">
          <template #default="{ row }">
            <span class="hint">
              {{ row.secondLevel === '1'
                ? `超过 ${row.thresholdDays} 天需二级审批，未超过仅区域负责人审批`
                : '仅区域负责人审批，不升级' }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="note">
        配置保存后只影响新提交的请假，已提交的请假按提交时的规则执行。
      </div>

      <div class="history-head">变更历史</div>
      <el-table v-loading="loading" :data="history" stripe>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeText(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column label="原配置" width="220">
          <template #default="{ row }">
            {{ row.oldThreshold == null ? '—' : `${row.oldThreshold} 天 / ${row.oldSecond === '1' ? '二级' : '一级'}` }}
          </template>
        </el-table-column>
        <el-table-column label="新配置" width="220">
          <template #default="{ row }">
            {{ row.newThreshold == null ? '—' : `${row.newThreshold} 天 / ${row.newSecond === '1' ? '二级' : '一级'}` }}
          </template>
        </el-table-column>
        <el-table-column prop="changedBy" label="修改人" width="120" />
        <el-table-column prop="changeTime" label="修改时间" min-width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openCompare(row)">对比</el-button>
            <el-button link type="primary" :disabled="row.oldThreshold == null" @click="rollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="versionVisible" title="审批规则版本" width="760px">
      <el-table v-loading="versionLoading" :data="versions" stripe>
        <el-table-column prop="versionNo" label="版本号" width="90">
          <template #default="{ row }">V{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '已归档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="publishedTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'PUBLISHED'" link type="primary" @click="publishVersion(row)">发布</el-button>
            <el-button link type="warning" @click="rollbackVersion(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="compareVisible" title="配置版本对比" width="520px">
      <template v-if="compareRow">
        <div class="compare-head">{{ typeText(compareRow.leaveType) }}</div>
        <el-table :data="compareRows" border>
          <el-table-column prop="label" label="项目" width="150" />
          <el-table-column prop="current" label="当前配置" />
          <el-table-column prop="history" label="该历史版本" />
        </el-table>
      </template>
      <template #footer>
        <el-button @click="compareVisible = false">关闭</el-button>
        <el-button type="primary" @click="exportCompare">导出对比</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="copyVisible" title="复制配置到区域" width="460px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="目标区域" required>
          <el-select v-model="copyTarget" style="width: 100%">
            <el-option label="全公司" :value="0" />
            <el-option
              v-for="r in regions.filter((x) => x.regionId !== regionId)"
              :key="r.regionId"
              :label="r.regionName"
              :value="r.regionId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="copy-hint">将当前区域/公司的 5 类请假规则复制到目标区域，并覆盖目标区域已有规则。</div>
      <template #footer>
        <el-button @click="copyVisible = false">取消</el-button>
        <el-button type="primary" :loading="copying" @click="submitCopy">确认复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const rules = ref([])
const history = ref([])
const regions = ref([])
const regionId = ref(0)
const copyVisible = ref(false)
const copying = ref(false)
const copyTarget = ref(0)
const compareVisible = ref(false)
const compareRow = ref(null)
const compareRows = ref([])
const versionVisible = ref(false)
const versionLoading = ref(false)
const versions = ref([])

const typeText = (v) => ({
  annual: '年假',
  sick: '病假',
  personal: '事假',
  training: '培训',
  temporary: '临时离岗'
}[v] || v)

async function load() {
  loading.value = true
  try {
    const params = regionId.value ? { regionId: regionId.value } : {}
    const [r, h] = await Promise.all([
      request.get('/approval/rules', { params }),
      request.get('/approval/rules/history', { params })
    ])
    rules.value = r.data || []
    history.value = h.data || []
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const params = regionId.value ? { regionId: regionId.value } : {}
    const res = await request.put('/approval/rules', rules.value, { params })
    ElMessage.success(res.data?.versionNo ? `配置已保存并发布 V${res.data.versionNo}` : '审批配置已保存')
    await load()
  } finally {
    saving.value = false
  }
}

async function rollback(row) {
  await ElMessageBox.confirm('确认将该类型规则回滚到这条历史配置？', '回滚配置', { type: 'warning' })
  await request.post(`/approval/rules/rollback/${row.logId}`)
  ElMessage.success('已回滚')
  await load()
}

async function openVersions() {
  versionVisible.value = true
  await loadVersions()
}

async function loadVersions() {
  versionLoading.value = true
  try {
    const params = regionId.value ? { regionId: regionId.value } : {}
    versions.value = (await request.get('/approval/rules/versions', { params })).data || []
  } finally {
    versionLoading.value = false
  }
}

async function publishVersion(row) {
  await ElMessageBox.confirm(`确认发布 V${row.versionNo}？发布后当前版本将归档。`, '发布版本', { type: 'warning' })
  await request.post(`/approval/rules/versions/${row.versionId}/publish`)
  ElMessage.success('版本已发布')
  await loadVersions()
  await load()
}

async function rollbackVersion(row) {
  await ElMessageBox.confirm(`确认回滚到 V${row.versionNo}？系统会生成一个新的回滚版本。`, '回滚版本', { type: 'warning' })
  const res = await request.post(`/approval/rules/versions/${row.versionId}/rollback`)
  ElMessage.success(`已回滚，新版本 V${res.data?.versionNo || ''}`)
  await loadVersions()
  await load()
}

function openCopy() {
  copyTarget.value = 0
  copyVisible.value = true
}

function openCompare(row) {
  compareRow.value = row
  const current = rules.value.find((r) => r.leaveType === row.leaveType) || {}
  compareRows.value = [
    {
      label: '升级阈值',
      current: `${current.thresholdDays ?? '—'} 天`,
      history: row.oldThreshold == null ? '—' : `${row.oldThreshold} 天`
    },
    {
      label: '审批级别',
      current: current.secondLevel === '1' ? '二级审批' : '一级审批',
      history: row.oldSecond == null ? '—' : (row.oldSecond === '1' ? '二级审批' : '一级审批')
    }
  ]
  compareVisible.value = true
}

function exportCompare() {
  if (!compareRow.value) return
  const lines = [
    ['项目', '当前配置', '历史版本'],
    ...compareRows.value.map((r) => [r.label, r.current, r.history])
  ]
  const csv = '\uFEFF' + lines.map((r) => r.map((v) => `"${String(v).replaceAll('"', '""')}"`).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `审批规则对比-${typeText(compareRow.value.leaveType)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

async function submitCopy() {
  if ((copyTarget.value || 0) === (regionId.value || 0)) {
    ElMessage.warning('目标区域不能与当前区域相同')
    return
  }
  copying.value = true
  try {
    const params = copyTarget.value ? { regionId: copyTarget.value } : {}
    await request.put('/approval/rules', rules.value, { params })
    ElMessage.success('配置已复制')
    copyVisible.value = false
    if (copyTarget.value === regionId.value) await load()
  } finally {
    copying.value = false
  }
}

onMounted(async () => {
  regions.value = (await request.get('/customer/region/options')).data || []
  await load()
})
</script>

<style scoped>
.unit { margin-left: 8px; color: #8a99ab; }
.hint { color: #5d6b7a; font-size: 13px; }
.note { margin-top: 16px; color: #8a99ab; font-size: 13px; }
.history-head { margin: 28px 0 12px; font-size: 15px; font-weight: 600; }
.head-actions { display: flex; align-items: center; gap: 10px; }
.copy-hint { margin-top: 8px; color: #8a99ab; font-size: 13px; line-height: 1.6; }
.compare-head { margin-bottom: 12px; font-size: 15px; font-weight: 600; }
</style>
