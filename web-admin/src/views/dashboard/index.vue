<template>
  <div class="app-container">
    <div class="hero">
      <div>
        <h2>{{ greeting }}，{{ store.displayName }}</h2>
        <p>{{ today }} · 层流净化设备运行与运维概览</p>
      </div>
      <div class="hero-tags">
        <div class="tag"><span>在线设备</span><b>{{ stats.equipmentOnline }}/{{ stats.equipmentCount }}</b></div>
        <div class="tag"><span>未处理告警</span><b class="warn">{{ stats.alarmUnhandled }}</b></div>
        <div class="tag"><span>待处理工单</span><b>{{ stats.workorderPending }}</b></div>
      </div>
    </div>

    <el-row :gutter="14" class="kpis">
      <el-col v-for="k in kpis" :key="k.label" :xs="12" :sm="8" :md="6" :lg="3">
        <div class="kpi" :style="{ '--c': k.color }">
          <div class="kpi-icon"><el-icon><component :is="k.icon" /></el-icon></div>
          <div class="kpi-body">
            <div class="kpi-value">{{ k.value }}<small v-if="k.unit">{{ k.unit }}</small></div>
            <div class="kpi-label">{{ k.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14">
      <el-col :xs="24" :lg="16">
        <div class="page-card chart-card">
          <div class="page-title">
            <div>
              <h2>近 7 天告警与工单趋势</h2>
              <div class="sub">告警产生量与工单创建量对比</div>
            </div>
          </div>
          <EChart :option="trendOption" height="300px" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="page-card chart-card">
          <div class="page-title">
            <div>
              <h2>设备类型分布</h2>
              <div class="sub">在册设备按类型统计</div>
            </div>
          </div>
          <EChart :option="categoryOption" height="300px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14">
      <el-col :xs="24" :lg="14">
        <div class="page-card">
          <div class="page-title">
            <div>
              <h2>最新告警</h2>
              <div class="sub">按发生时间倒序</div>
            </div>
            <el-button text type="primary" @click="$router.push('/alarm/list')">查看全部</el-button>
          </div>
          <el-table :data="recentAlarms" size="small" style="width: 100%">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
            <el-table-column prop="alarmNo" label="告警编号" width="150" show-overflow-tooltip />
            <el-table-column label="级别" width="76">
              <template #default="{ row }">
                <el-tag :type="levelType(row.level)" size="small" effect="dark">{{ levelText(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="equipmentName" label="设备" min-width="150" show-overflow-tooltip />
            <el-table-column prop="hospitalName" label="医院" min-width="130" show-overflow-tooltip />
            <el-table-column prop="time" label="发生时间" width="110" />
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="page-card">
          <div class="page-title">
            <div>
              <h2>合同到期提醒</h2>
              <div class="sub">120 天内到期合同</div>
            </div>
            <el-button text type="primary" @click="$router.push('/customer/contract')">合同管理</el-button>
          </div>
          <div v-if="!expiringContracts.length" class="empty">暂无即将到期合同</div>
          <div v-for="c in expiringContracts" :key="c.contractId" class="contract-item">
            <div class="contract-main">
              <div class="contract-name">{{ c.contractName }}</div>
              <div class="contract-sub">{{ c.hospitalName }} · 到期 {{ c.endDate }}</div>
            </div>
            <el-tag :type="c.days <= 30 ? 'danger' : c.days <= 60 ? 'warning' : 'info'" size="small">
              剩 {{ c.days }} 天
            </el-tag>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14">
      <el-col :xs="24" :lg="12">
        <div class="page-card">
          <div class="page-title">
            <div>
              <h2>今日待办 / 到期任务</h2>
              <div class="sub">巡检与保养任务提醒</div>
            </div>
          </div>
          <div v-if="!dueTasks.length" class="empty">今日暂无到期任务</div>
          <div v-for="t in dueTasks" :key="t.taskNo" class="task-item">
            <el-tag size="small" :type="t.type === 'inspection' ? 'primary' : 'success'">
              {{ t.type === 'inspection' ? '巡检' : '保养' }}
            </el-tag>
            <span class="task-title">{{ t.title || t.taskNo }}</span>
            <span class="task-date">{{ String(t.planDate || '').slice(0, 10) }}</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="page-card">
          <div class="page-title">
            <div>
              <h2>医院设备在线情况</h2>
              <div class="sub">按医院统计在线设备占比</div>
            </div>
          </div>
          <EChart :option="hospitalOption" height="240px" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import EChart from '@/components/EChart.vue'

const store = useUserStore()
const data = ref({})
const stats = computed(() => data.value.stats || {})
const recentAlarms = computed(() => data.value.recentAlarms || [])
const expiringContracts = computed(() => data.value.expiringContracts || [])
const dueTasks = computed(() => data.value.dueTasks || [])

const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 6 ? '凌晨好' : h < 12 ? '上午好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
})
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const kpis = computed(() => [
  { label: '联网医院', value: stats.value.hospitalCount || 0, icon: 'FirstAidKit', color: '#1f6feb' },
  { label: '设备在线率', value: stats.value.onlineRate || 0, unit: '%', icon: 'DataLine', color: '#0ea5a4' },
  { label: '未处理告警', value: stats.value.alarmUnhandled || 0, icon: 'Warning', color: '#ef4444' },
  { label: '紧急告警', value: stats.value.alarmCritical || 0, icon: 'BellFilled', color: '#f97316' },
  { label: '待受理/派工', value: stats.value.workorderPending || 0, icon: 'Tools', color: '#8b5cf6' },
  { label: '本月工单', value: stats.value.workorderMonth || 0, icon: 'Tickets', color: '#2563eb' },
  { label: '合同到期', value: stats.value.contractExpiring || 0, icon: 'Document', color: '#d97706' },
  { label: '证书到期', value: stats.value.certExpiring || 0, icon: 'Medal', color: '#db2777' }
])

function levelText(l) { return { 1: '紧急', 2: '重要', 3: '一般', 4: '提示' }[l] || '一般' }
function levelType(l) { return { 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }[l] || 'info' }

const trendOption = computed(() => {
  const trend = data.value.alarmTrend || []
  const wo = data.value.workorderTrend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, data: ['告警', '工单'] },
    grid: { left: 36, right: 16, top: 36, bottom: 26 },
    xAxis: { type: 'category', data: trend.map((t) => t.date), axisLine: { lineStyle: { color: '#dfe6ef' } }, axisLabel: { color: '#7a8ba0' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#7a8ba0' } },
    series: [
      { name: '告警', type: 'line', smooth: true, symbolSize: 6, data: trend.map((t) => t.value), itemStyle: { color: '#ef4444' }, areaStyle: { color: 'rgba(239,68,68,.10)' } },
      { name: '工单', type: 'line', smooth: true, symbolSize: 6, data: wo.map((t) => t.value), itemStyle: { color: '#1f6feb' }, areaStyle: { color: 'rgba(31,111,235,.10)' } }
    ]
  }
})

const categoryOption = computed(() => {
  const list = data.value.equipmentByCategory || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle', textStyle: { color: '#5c6f86', fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['46%', '68%'], center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: list.map((i) => ({ name: i.name, value: i.value })),
      color: ['#1f6feb', '#0ea5a4', '#8b5cf6', '#f59e0b', '#ef4444', '#64748b']
    }]
  }
})

const hospitalOption = computed(() => {
  const list = (data.value.hospitalMap || []).slice(0, 6)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 30, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#7a8ba0' } },
    yAxis: { type: 'category', data: list.map((h) => h.hospitalName).reverse(), axisLabel: { color: '#5c6f86', fontSize: 11 }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{
      type: 'bar', barWidth: 12,
      data: list.map((h) => ({ value: h.onlineCount, total: h.equipmentCount })).reverse(),
      itemStyle: { borderRadius: [0, 6, 6, 0], color: '#0ea5a4' },
      label: { show: true, position: 'right', formatter: (p) => `${p.value}/${p.data.total}`, color: '#5c6f86', fontSize: 11 }
    }]
  }
})

async function load() {
  const res = await request.get('/dashboard/overview')
  data.value = res.data
}

onMounted(load)
</script>

<style scoped>
.hero {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  padding: 20px 24px; border-radius: 12px; color: #fff;
  background: radial-gradient(700px 200px at 0% 0%, rgba(56,189,248,.35), transparent 60%), linear-gradient(120deg, #10233d, #123a5c 55%, #0f5e6b);
}
.hero h2 { margin: 0 0 6px; font-size: 21px; }
.hero p { margin: 0; color: #9fb8cf; font-size: 12px; }
.hero-tags { display: flex; gap: 28px; }
.tag { display: flex; flex-direction: column; gap: 4px; }
.tag span { font-size: 11px; color: #9fb8cf; }
.tag b { font-size: 20px; }
.tag b.warn { color: #fca5a5; }

.kpis { margin-bottom: 2px; }
.kpi {
  background: #fff; border-radius: 10px; padding: 14px; display: flex; align-items: center; gap: 12px;
  margin-bottom: 14px; border: 1px solid #eef2f7; box-shadow: 0 1px 2px rgba(16,28,46,.03);
}
.kpi-icon {
  width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
  font-size: 19px; color: var(--c); background: color-mix(in srgb, var(--c) 12%, white);
}
.kpi-value { font-size: 22px; font-weight: 700; color: #1f2d3d; line-height: 1.1; }
.kpi-value small { font-size: 12px; margin-left: 2px; color: #7a8ba0; }
.kpi-label { font-size: 11px; color: #8494a8; margin-top: 3px; }

.chart-card { margin-bottom: 14px; }
.contract-item, .task-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 0; border-bottom: 1px dashed #edf1f6;
}
.contract-item:last-child, .task-item:last-child { border-bottom: none; }
.contract-name { font-size: 13px; color: #2b3b50; }
.contract-sub { font-size: 11px; color: #94a3b8; margin-top: 3px; }
.task-title { flex: 1; font-size: 13px; color: #33465c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-date { font-size: 11px; color: #94a3b8; }
.empty { color: #a7b4c5; font-size: 12px; padding: 18px 0; text-align: center; }
</style>
