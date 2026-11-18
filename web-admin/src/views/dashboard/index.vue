<template>
  <div class="dashboard app-container" v-loading="loading">
    <header class="dash-head">
      <div class="head-copy">
        <div class="eyebrow"><span class="eyebrow-dot"></span>运营总览 · OPS OVERVIEW</div>
        <h1>{{ greeting }}，{{ store.displayName }}</h1>
        <p>{{ today }} · 设备运行与运维服务概览</p>
      </div>
      <div class="head-actions">
        <span v-if="lastUpdated" class="updated">更新于 {{ lastUpdated }}</span>
        <button class="btn btn-ghost" type="button" @click="load">刷新数据</button>
        <button class="btn btn-primary" type="button" @click="$router.push('/workorder/dispatch')">处理工单</button>
      </div>
    </header>

    <section class="snapshot">
      <div class="snapshot-lead">
        <div class="snapshot-label">设备在线率</div>
        <div class="snapshot-value">{{ stats.onlineRate || 0 }}<small>%</small></div>
        <div class="snapshot-sub">{{ stats.equipmentOnline || 0 }} / {{ stats.equipmentCount || 0 }} 台设备在线</div>
        <div class="progress"><i :style="{ width: `${Math.min(100, Number(stats.onlineRate) || 0)}%` }"></i></div>
      </div>
      <div class="snapshot-grid">
        <div v-for="item in summary" :key="item.label" class="summary-item">
          <div class="summary-label">{{ item.label }}</div>
          <div class="summary-value" :class="item.tone">
            {{ item.value }}<small>{{ item.unit }}</small>
          </div>
          <div class="summary-note">{{ item.note }}</div>
        </div>
      </div>
    </section>

    <section class="minor-grid">
      <article v-for="item in minor" :key="item.label" class="minor-card">
        <div class="minor-icon"><el-icon><component :is="item.icon" /></el-icon></div>
        <div class="minor-copy">
          <div class="minor-label">{{ item.label }}</div>
          <div class="minor-value">{{ item.value }}<small>{{ item.unit }}</small></div>
          <div class="minor-note">{{ item.note }}</div>
        </div>
      </article>
    </section>

    <section class="content-grid primary-grid">
      <article class="card chart-card wide">
        <div class="card-head">
          <div>
            <h2>近 7 天告警与工单趋势</h2>
            <p>告警产生量与工单创建量对比</p>
          </div>
          <span class="head-meta">最近 7 天</span>
        </div>
        <EChart :option="trendOption" height="286px" />
      </article>
      <article class="card chart-card">
        <div class="card-head">
          <div>
            <h2>设备类型分布</h2>
            <p>在册设备按类型统计</p>
          </div>
        </div>
        <EChart :option="categoryOption" height="286px" />
      </article>
    </section>

    <section class="content-grid lower-grid">
      <div class="lower-main">
        <article class="card alarm-card">
          <div class="card-head">
            <div>
              <h2>最新告警</h2>
              <p>按发生时间倒序 · 优先处理未闭环告警</p>
            </div>
            <button class="text-action" type="button" @click="$router.push('/alarm/list')">查看全部 <span>↗</span></button>
          </div>
          <el-table :data="recentAlarms" size="small" class="alarm-table" style="width: 100%">
            <el-table-column prop="alarmNo" label="告警编号" width="150" show-overflow-tooltip />
            <el-table-column label="级别" width="78">
              <template #default="{ row }">
                <el-tag :type="levelType(row.level)" size="small" effect="plain">{{ levelText(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="equipmentName" label="设备" min-width="150" show-overflow-tooltip />
            <el-table-column prop="hospitalName" label="医院" min-width="130" show-overflow-tooltip />
            <el-table-column prop="time" label="发生时间" width="110" />
          </el-table>
        </article>

        <article class="card hospital-card">
          <div class="card-head">
            <div>
              <h2>医院设备在线情况</h2>
              <p>按医院统计在线设备占比</p>
            </div>
          </div>
          <EChart :option="hospitalOption" height="220px" />
        </article>
      </div>

      <div class="side-stack">
        <article class="card">
          <div class="card-head">
            <div>
              <h2>合同到期提醒</h2>
              <p>120 天内到期合同</p>
            </div>
            <button class="text-action" type="button" @click="$router.push('/customer/contract')">合同管理 <span>↗</span></button>
          </div>
          <div v-if="!expiringContracts.length" class="empty">暂无即将到期合同</div>
          <div v-for="c in expiringContracts" :key="c.contractId" class="list-item">
            <div class="list-main">
              <div class="list-title">{{ c.contractName }}</div>
              <div class="list-sub">{{ c.hospitalName }} · 到期 {{ c.endDate }}</div>
            </div>
            <el-tag :type="c.days <= 30 ? 'danger' : c.days <= 60 ? 'warning' : 'info'" size="small" effect="plain">
              剩 {{ c.days }} 天
            </el-tag>
          </div>
        </article>

        <article class="card">
          <div class="card-head">
            <div>
              <h2>今日待办</h2>
              <p>巡检与保养任务提醒</p>
            </div>
          </div>
          <div v-if="!dueTasks.length" class="empty">今日暂无到期任务</div>
          <div v-for="t in dueTasks" :key="t.taskNo" class="list-item">
            <el-tag size="small" :type="t.type === 'inspection' ? 'primary' : 'success'" effect="plain">
              {{ t.type === 'inspection' ? '巡检' : '保养' }}
            </el-tag>
            <div class="list-main">
              <div class="list-title">{{ t.title || t.taskNo }}</div>
              <div class="list-sub">{{ String(t.planDate || '').slice(0, 10) }}</div>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import EChart from '@/components/EChart.vue'

const store = useUserStore()
const data = ref({})
const loading = ref(false)
const lastUpdated = ref('')
const stats = computed(() => data.value.stats || {})
const recentAlarms = computed(() => data.value.recentAlarms || [])
const expiringContracts = computed(() => data.value.expiringContracts || [])
const dueTasks = computed(() => data.value.dueTasks || [])

const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 6 ? '凌晨好' : h < 12 ? '上午好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
})
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const summary = computed(() => [
  { label: '联网医院', value: stats.value.hospitalCount || 0, unit: '家', note: '已接入平台的医院', tone: '' },
  { label: '设备总数', value: stats.value.equipmentCount || 0, unit: '台', note: '在册层流设备', tone: '' },
  { label: '未处理告警', value: stats.value.alarmUnhandled || 0, unit: '条', note: '需要关注', tone: 'danger' },
  { label: '待处理工单', value: stats.value.workorderPending || 0, unit: '单', note: '待受理 / 待派工', tone: 'accent' }
])

const minor = computed(() => [
  { label: '紧急告警', value: stats.value.alarmCritical || 0, unit: '条', icon: 'Warning', note: '最高优先级' },
  { label: '本月工单', value: stats.value.workorderMonth || 0, unit: '单', icon: 'Tickets', note: '本月累计创建' },
  { label: '合同到期', value: stats.value.contractExpiring || 0, unit: '份', icon: 'Document', note: '120 天内到期' },
  { label: '证书到期', value: stats.value.certExpiring || 0, unit: '份', icon: 'Medal', note: '需要续期' }
])

function levelText(l) { return { 1: '紧急', 2: '重要', 3: '一般', 4: '提示' }[l] || '一般' }
function levelType(l) { return { 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }[l] || 'info' }

const chartText = { color: '#6b7280', fontSize: 11 }
const axisLine = { color: '#e5e7eb' }

const trendOption = computed(() => {
  const trend = data.value.alarmTrend || []
  const wo = data.value.workorderTrend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, data: ['告警', '工单'], textStyle: chartText, itemWidth: 14, itemHeight: 6 },
    grid: { left: 34, right: 14, top: 34, bottom: 24 },
    xAxis: { type: 'category', data: trend.map((t) => t.date), axisLine: { lineStyle: axisLine }, axisTick: { show: false }, axisLabel: chartText },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: chartText },
    series: [
      { name: '告警', type: 'line', smooth: true, symbol: 'none', data: trend.map((t) => t.value), lineStyle: { width: 2, color: '#ef4444' }, areaStyle: { color: 'rgba(239,68,68,.06)' } },
      { name: '工单', type: 'line', smooth: true, symbol: 'none', data: wo.map((t) => t.value), lineStyle: { width: 2, color: '#111111' }, areaStyle: { color: 'rgba(17,17,17,.04)' } }
    ]
  }
})

const categoryOption = computed(() => {
  const list = data.value.equipmentByCategory || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 7, itemHeight: 7, textStyle: { ...chartText, fontSize: 10 } },
    series: [{
      type: 'pie', radius: ['48%', '68%'], center: ['50%', '43%'],
      itemStyle: { borderColor: '#ffffff', borderWidth: 3 },
      label: { show: false },
      data: list.map((i) => ({ name: i.name, value: i.value })),
      color: ['#111111', '#3b82f6', '#0ea5a4', '#8b5cf6', '#f59e0b', '#94a3b8']
    }]
  }
})

const hospitalOption = computed(() => {
  const list = (data.value.hospitalMap || []).slice(0, 6)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 42, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: chartText },
    yAxis: {
      type: 'category', data: list.map((h) => h.hospitalName).reverse(),
      axisLabel: { ...chartText, color: '#374151', fontSize: 11 },
      axisLine: { show: false }, axisTick: { show: false }
    },
    series: [{
      type: 'bar', barWidth: 10,
      data: list.map((h) => ({ value: h.onlineCount, total: h.equipmentCount })).reverse(),
      itemStyle: { borderRadius: [0, 4, 4, 0], color: '#111111' },
      label: { show: true, position: 'right', formatter: (p) => `${p.value}/${p.data.total}`, color: '#6b7280', fontSize: 10 }
    }]
  }
})

async function load() {
  loading.value = true
  try {
    const res = await request.get('/dashboard/overview')
    data.value = res.data
    lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dashboard {
  --d-ink: #111111;
  --d-body: #374151;
  --d-mute: #6b7280;
  --d-soft: #f8f9fa;
  --d-surface: #f5f5f5;
  --d-line: #e5e7eb;
  --d-accent: #3b82f6;
  --d-danger: #ef4444;
  --d-radius: 12px;
  padding: 8px 4px 34px;
  color: var(--d-ink);
}

.dash-head {
  display: flex; justify-content: space-between; align-items: flex-end; gap: 24px;
  padding: 10px 2px 22px; margin-bottom: 18px; border-bottom: 1px solid var(--d-line);
}
.eyebrow { display: flex; align-items: center; gap: 8px; color: var(--d-mute); font-size: 11px; font-weight: 600; letter-spacing: .12em; text-transform: uppercase; }
.eyebrow-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--d-ink); display: inline-block; }
.dash-head h1 { margin: 10px 0 7px; font-size: clamp(28px, 3.3vw, 38px); font-weight: 600; letter-spacing: -.035em; line-height: 1.08; }
.dash-head p { margin: 0; color: var(--d-mute); font-size: 13px; }
.head-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; padding-bottom: 3px; }
.updated { color: #9ca3af; font-size: 11px; margin-right: 4px; white-space: nowrap; }
.btn { height: 38px; padding: 0 15px; border-radius: 8px; border: 1px solid var(--d-line); font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; transition: none; }
.btn-ghost { background: #ffffff; color: var(--d-ink); }
.btn-ghost:hover { background: var(--d-soft); }
.btn-primary { background: var(--d-ink); color: #ffffff; border-color: var(--d-ink); }
.btn-primary:hover { background: #242424; }

.snapshot {
  display: grid; grid-template-columns: minmax(250px, .9fr) 2fr;
  background: var(--d-soft); border: 1px solid var(--d-line); border-radius: 16px;
  overflow: hidden; margin-bottom: 14px;
}
.snapshot-lead { padding: 22px 24px; border-right: 1px solid var(--d-line); display: flex; flex-direction: column; justify-content: center; }
.snapshot-label { color: var(--d-mute); font-size: 12px; font-weight: 500; }
.snapshot-value { margin-top: 7px; font-size: 44px; line-height: 1; letter-spacing: -.055em; font-weight: 600; }
.snapshot-value small { margin-left: 3px; font-size: 17px; font-weight: 500; color: var(--d-mute); letter-spacing: 0; }
.snapshot-sub { margin-top: 10px; color: var(--d-mute); font-size: 12px; }
.progress { height: 4px; margin-top: 16px; border-radius: 999px; background: #e5e7eb; overflow: hidden; }
.progress i { display: block; height: 100%; border-radius: 999px; background: var(--d-ink); transition: width .3s ease; }
.snapshot-grid { display: grid; grid-template-columns: 1fr 1fr; }
.summary-item { padding: 18px 22px; border-right: 1px solid var(--d-line); border-bottom: 1px solid var(--d-line); }
.summary-item:nth-child(2n) { border-right: none; }
.summary-item:nth-last-child(-n+2) { border-bottom: none; }
.summary-label { color: var(--d-mute); font-size: 12px; }
.summary-value { margin-top: 5px; font-size: 25px; line-height: 1.15; font-weight: 600; letter-spacing: -.02em; }
.summary-value small { margin-left: 4px; color: var(--d-mute); font-size: 11px; font-weight: 500; letter-spacing: 0; }
.summary-value.danger { color: var(--d-danger); }
.summary-value.accent { color: var(--d-accent); }
.summary-note { margin-top: 5px; color: #9ca3af; font-size: 11px; }

.minor-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 14px; }
.minor-card { display: flex; align-items: flex-start; gap: 12px; padding: 16px 17px; background: #ffffff; border: 1px solid var(--d-line); border-radius: var(--d-radius); }
.minor-icon { width: 30px; height: 30px; flex: 0 0 30px; border-radius: 8px; background: var(--d-soft); display: flex; align-items: center; justify-content: center; color: var(--d-ink); font-size: 15px; }
.minor-copy { min-width: 0; }
.minor-label { color: var(--d-mute); font-size: 11px; }
.minor-value { margin-top: 3px; font-size: 21px; line-height: 1.2; font-weight: 600; letter-spacing: -.02em; }
.minor-value small { margin-left: 3px; color: var(--d-mute); font-size: 10px; font-weight: 500; }
.minor-note { margin-top: 2px; color: #9ca3af; font-size: 10px; }

.content-grid { display: grid; gap: 14px; margin-bottom: 14px; }
.primary-grid { grid-template-columns: minmax(0, 1.7fr) minmax(300px, 1fr); }
.lower-grid { grid-template-columns: minmax(0, 1.55fr) minmax(300px, 1fr); margin-bottom: 0; align-items: start; }
.lower-main { display: grid; gap: 14px; align-content: start; min-width: 0; }
.card { background: #ffffff; border: 1px solid var(--d-line); border-radius: var(--d-radius); padding: 18px 20px; min-width: 0; }
.card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.card-head h2 { margin: 0; font-size: 15px; font-weight: 600; letter-spacing: -.01em; }
.card-head p { margin: 4px 0 0; color: var(--d-mute); font-size: 11px; }
.head-meta { color: #9ca3af; font-size: 11px; white-space: nowrap; }
.text-action { border: 0; background: transparent; color: var(--d-mute); font-size: 11px; font-family: inherit; cursor: pointer; padding: 2px 0; white-space: nowrap; }
.text-action:hover { color: var(--d-ink); }
.text-action span { margin-left: 3px; }
.chart-card { padding-bottom: 8px; }
.side-stack { display: grid; gap: 14px; align-content: start; }
.side-stack .card { padding-bottom: 14px; }
.list-item { display: flex; align-items: center; gap: 10px; padding: 9px 0; border-bottom: 1px solid #f3f4f6; }
.list-item:last-child { border-bottom: none; }
.list-main { min-width: 0; flex: 1; }
.list-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--d-body); font-size: 12px; }
.list-sub { margin-top: 3px; color: #9ca3af; font-size: 10px; }
.empty { padding: 20px 0; text-align: center; color: #9ca3af; font-size: 11px; }

.alarm-table :deep(.el-table__header th) { background: transparent; color: var(--d-mute); font-size: 11px; font-weight: 500; }
.alarm-table :deep(.el-table__row td) { color: var(--d-body); font-size: 12px; border-bottom-color: #f3f4f6; }
.alarm-table :deep(.el-table__inner-wrapper::before) { display: none; }
.alarm-table :deep(.el-table__row) { height: 42px; }

@media (max-width: 1100px) {
  .primary-grid, .lower-grid { grid-template-columns: 1fr; }
  .minor-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 760px) {
  .dashboard { padding: 4px 0 24px; }
  .dash-head { align-items: flex-start; flex-direction: column; gap: 14px; }
  .dash-head h1 { font-size: 28px; }
  .head-actions { width: 100%; justify-content: flex-end; }
  .updated { margin-right: auto; }
  .snapshot { grid-template-columns: 1fr; }
  .snapshot-lead { border-right: none; border-bottom: 1px solid var(--d-line); }
  .summary-item { padding: 14px 16px; }
  .minor-grid { grid-template-columns: 1fr 1fr; }
  .card { padding: 15px; }
  .alarm-card { overflow-x: auto; }
}
</style>
