<template>
  <div class="app-container">
    <div class="hero">
      <div>
        <h2>洁净区实时监测</h2>
        <p>层流机组 / 净化空调 / 新风 / 除湿 / 供氧 / 负压 · 每 30 秒采集一次</p>
      </div>
      <div class="hero-stats">
        <div class="hs"><span>在线设备</span><b class="ok">{{ summary.total || 0 }}</b></div>
        <div class="hs"><span>离线/未接入</span><b>{{ summary.offline || 0 }}</b></div>
        <div class="hs"><span>活动告警</span><b class="warn">{{ summary.alarmOpen || 0 }}</b></div>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="hospitalId" clearable placeholder="全部医院" style="width: 190px" @change="onHospitalChange">
        <el-option v-for="h in hospitals" :key="h.value" :label="h.label" :value="h.value" />
      </el-select>
      <el-select v-model="campusId" clearable placeholder="全部院区" style="width: 150px; margin-left: 10px" :disabled="!hospitalId" @change="onCampusChange">
        <el-option v-for="c in campuses" :key="c.value" :label="c.label" :value="c.value" />
      </el-select>
      <el-select v-model="departmentId" clearable placeholder="全部科室" style="width: 150px; margin-left: 10px" :disabled="!campusId" @change="load">
        <el-option v-for="d in departments" :key="d.value" :label="d.label" :value="d.value" />
      </el-select>
      <el-input v-model="keyword" clearable placeholder="搜索名称 / 编号 / 类型 / 区域" style="width: 220px; margin-left: 10px" />
      <el-switch v-model="auto" active-text="自动刷新" style="margin-left: 16px" />
      <el-button :icon="Refresh" style="margin-left: 10px" @click="load">刷新</el-button>
      <span class="updated" v-if="updatedAt">最近更新 {{ updatedAt }}</span>
    </div>

    <div v-loading="loading" class="grid">
      <div
        v-for="m in filtered"
        :key="m.equipment_id"
        class="eq-card"
        :class="{ offline: m.monitor_status !== '1', fault: m.run_status === '2' }"
        @click="open(m)"
      >
        <div class="eq-head">
          <span class="dot" :class="m.monitor_status === '1' ? 'on' : 'off'"></span>
          <span class="name">{{ m.equipment_name }}</span>
          <el-tag size="small" effect="plain" class="type">{{ m.type_name || '设备' }}</el-tag>
        </div>
        <div class="loc">{{ m.hospital_name }} · {{ m.campus_name }} · {{ m.department_name }}</div>
        <div class="metrics">
          <div v-for="slot in metricSlots(m)" :key="slot.metric" class="metric">
            <div class="mv" :class="{ muted: slot.value == null }">
              {{ slot.value == null ? '--' : slot.value }}<small>{{ slot.unit }}</small>
            </div>
            <div class="ml">{{ slot.label }}</div>
          </div>
        </div>
        <div class="eq-foot">
          <span>{{ m.run_status === '2' ? '故障' : m.run_status === '1' ? '停机' : m.monitor_status === '1' ? '运行中' : '离线' }}</span>
          <span class="tm">{{ (m.lastTime || '').slice(11) }}</span>
        </div>
      </div>
      <div v-if="!filtered.length && !loading" class="empty">暂无匹配设备</div>
    </div>

    <el-drawer v-model="drawer" size="720px" :title="detail?.equipment?.equipment_name || '设备详情'">
      <div v-if="detail" class="detail">
        <div class="d-head">
          <div>
            <div class="d-loc">{{ detail.equipment.hospital_name }} · {{ detail.equipment.campus_name }} · {{ detail.equipment.department_name }}</div>
            <div class="d-sub">编号 {{ detail.equipment.equipment_code }} · 型号 {{ detail.equipment.model || '—' }} · {{ detail.equipment.type_name }}</div>
          </div>
          <el-tag :type="detail.equipment.run_status === '2' ? 'danger' : 'success'">
            {{ detail.equipment.run_status === '2' ? '故障' : '运行' }}
          </el-tag>
        </div>

        <el-select v-model="activePoint" style="width: 260px; margin: 14px 0" @change="renderChart">
          <el-option v-for="p in detail.points" :key="p.point_id" :label="p.point_name + '（' + metricName(p.metric) + '）'" :value="p.point_id" />
        </el-select>
        <EChart :option="chartOption" height="260px" />

        <el-table :data="detail.points" size="small" style="margin-top: 10px">
        <el-table-column type="index" label="序号" width="62" align="center" />
          <el-table-column prop="point_name" label="点位" />
          <el-table-column label="指标" width="100">
        <template #default="{ row }">{{ metricName(row.metric) }}</template></el-table-column>
          <el-table-column label="实时值" width="120">
            <template #default="{ row }"><b>{{ row.latestValue ?? '--' }}</b> <span class="unit">{{ row.unit }}</span></template>
          </el-table-column>
          <el-table-column label="告警范围" width="150">
            <template #default="{ row }">{{ row.alarm_lower ?? '-' }} ~ {{ row.alarm_upper ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="latestTime" label="更新时间" width="150" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import EChart from '@/components/EChart.vue'

const loading = ref(false)
const list = ref([])
const summary = ref({})
const hospitals = ref([])
const hospitalId = ref()
const campuses = ref([])
const campusId = ref()
const departments = ref([])
const departmentId = ref()
const keyword = ref('')
const auto = ref(true)
const updatedAt = ref('')
const drawer = ref(false)
const detail = ref(null)
const activePoint = ref(null)
const chartOption = ref({})
let timer = null

const metricMap = {
  temperature: '温度',
  humidity: '湿度',
  pressure: '压差',
  wind_speed: '风速',
  particle: '粒子',
  oxygen_pressure: '氧气压力',
  vacuum: '真空度',
  run_state: '运行状态'
}
function metricName(m) { return metricMap[m] || m }
const displayMetrics = [
  { metric: 'temperature', label: '温度', unit: '℃' },
  { metric: 'humidity', label: '湿度', unit: '%RH' },
  { metric: 'pressure', label: '压差', unit: 'Pa' },
  { metric: 'wind_speed', label: '风速', unit: 'm/s' }
]
function metricSlots(m) {
  const list = m.metrics || []
  return displayMetrics.map((def) => {
    const point = list.find((p) => p.metric === def.metric)
    return {
      ...def,
      unit: point?.unit || def.unit,
      value: point?.value
    }
  })
}

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter((m) => [
    m.equipment_code,
    m.equipment_name,
    m.department_name,
    m.hospital_name,
    m.campus_name,
    m.type_name,
    m.install_location
  ].filter(Boolean).join(' ').toLowerCase().includes(kw))
})

async function load() {
  loading.value = true
  try {
    const res = await request.get('/monitor/overview', {
      params: { hospitalId: hospitalId.value, campusId: campusId.value, departmentId: departmentId.value }
    })
    list.value = res.data.list
    summary.value = res.data.summary
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } finally {
    loading.value = false
  }
}

async function onHospitalChange() {
  campusId.value = undefined
  departmentId.value = undefined
  departments.value = []
  campuses.value = hospitalId.value
    ? ((await request.get('/crud/campus/options', { params: { hospitalId: hospitalId.value } })).data || [])
    : []
  await load()
}

async function onCampusChange() {
  departmentId.value = undefined
  departments.value = campusId.value
    ? ((await request.get('/crud/department/options', { params: { campusId: campusId.value } })).data || [])
    : []
  await load()
}

async function open(row) {
  drawer.value = true
  detail.value = null
  const res = await request.get('/monitor/equipment/' + row.equipment_id)
  detail.value = res.data
  activePoint.value = res.data.points[0]?.point_id
  renderChart()
}

function renderChart() {
  const p = detail.value?.points.find((x) => x.point_id === activePoint.value)
  if (!p) return
  chartOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 20, bottom: 26 },
    xAxis: { type: 'category', data: (p.history || []).map((h) => h.t), axisLabel: { color: '#7a8ba0', fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#7a8ba0' } },
    series: [{
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: (p.history || []).map((h) => h.value),
      lineStyle: { color: '#1f6feb', width: 2 },
      areaStyle: { color: 'rgba(31,111,235,.12)' }
    }]
  }
}

onMounted(async () => {
  const h = await request.get('/crud/hospital/options')
  hospitals.value = h.data
  await load()
  timer = setInterval(() => { if (auto.value) load() }, 10000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.hero {
  display: flex; justify-content: space-between; align-items: center; padding: 18px 22px; border-radius: 12px; color: #fff;
  background: radial-gradient(600px 200px at 0% 0%, rgba(56,189,248,.32), transparent 60%), linear-gradient(120deg, #10233d, #123a5c 60%, #0f5e6b);
  margin-bottom: 14px;
}
.hero h2 { margin: 0 0 6px; font-size: 20px; }
.hero p { margin: 0; color: #9fb8cf; font-size: 12px; }
.hero-stats { display: flex; gap: 32px; }
.hs { display: flex; flex-direction: column; gap: 3px; }
.hs span { color: #9fb8cf; font-size: 11px; }
.hs b { font-size: 22px; }
.hs b.ok { color: #5eead4; }
.hs b.warn { color: #fca5a5; }
.filter-bar { display: flex; align-items: center; background: #fff; padding: 12px 16px; border-radius: 10px; margin-bottom: 14px; }
.updated { margin-left: auto; color: #94a3b8; font-size: 12px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
.eq-card {
  background: #fff; border-radius: 12px; padding: 14px 16px; cursor: pointer; border: 1px solid #eef2f7;
  transition: all .18s ease; box-shadow: 0 1px 2px rgba(16,28,46,.03);
}
.eq-card:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(16,28,46,.08); border-color: #d6e4f5; }
.eq-card.fault { border-color: #fecaca; background: linear-gradient(180deg, #fff, #fff7f7); }
.eq-card.offline { opacity: .72; }
.eq-head { display: flex; align-items: center; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.on { background: #10b981; box-shadow: 0 0 0 3px rgba(16,185,129,.15); }
.dot.off { background: #94a3b8; }
.name { font-weight: 600; font-size: 14px; color: #1f2d3d; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.type { transform: scale(.9); }
.loc { font-size: 11px; color: #8ea0b5; margin: 6px 0 12px; }
.metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.metric { background: #f7f9fc; border-radius: 8px; padding: 8px 6px; text-align: center; }
.mv { font-size: 17px; font-weight: 700; color: #1f6feb; }
.mv small { font-size: 10px; color: #94a3b8; margin-left: 1px; font-weight: 400; }
.mv.muted { color: #b9c8da; }
.ml { font-size: 10px; color: #8494a8; margin-top: 2px; }
.eq-foot { display: flex; justify-content: space-between; font-size: 11px; color: #94a3b8; margin-top: 12px; }
.empty { grid-column: 1/-1; text-align: center; padding: 60px; color: #a7b4c5; }
.d-head { display: flex; justify-content: space-between; align-items: flex-start; }
.d-loc { font-size: 14px; color: #33465c; font-weight: 600; }
.d-sub { font-size: 11px; color: #94a3b8; margin-top: 5px; }
.unit { color: #94a3b8; font-size: 11px; }
</style>
