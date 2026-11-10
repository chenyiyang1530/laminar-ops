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
            <p>截止时间：{{ stats.cutoff || '—' }}</p>
          </div>
          <el-button type="primary" :loading="running" @click="run">开始归档</el-button>
        </div>
        <div class="run-options">
          <el-checkbox v-model="alarms" :disabled="!stats.alarmEligible">归档告警（{{ stats.alarmEligible ?? 0 }} 条）</el-checkbox>
          <el-checkbox v-model="workorders" :disabled="!stats.workorderEligible">归档工单（{{ stats.workorderEligible ?? 0 }} 条）</el-checkbox>
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
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const monthOptions = [1, 3, 6, 12]
const months = ref(1)
const alarms = ref(true)
const workorders = ref(true)
const running = ref(false)
const stats = reactive({})

async function loadStats() {
  const res = await request.get('/archive/stats', { params: { months: months.value } })
  Object.assign(stats, res.data || {})
  alarms.value = (stats.alarmEligible || 0) > 0
  workorders.value = (stats.workorderEligible || 0) > 0
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
  } finally {
    running.value = false
  }
}

onMounted(loadStats)
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
.run-options { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 14px; }
.archive-note { margin-top: 18px; font-size: 12px; color: #7b8a9d; }
.note-title { font-weight: 600; color: #5b6b7d; margin-bottom: 6px; }
.archive-note ul { margin: 0; padding-left: 18px; line-height: 1.9; }
@media (max-width: 900px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
