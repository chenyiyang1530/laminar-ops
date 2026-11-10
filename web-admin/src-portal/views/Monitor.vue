<template>
  <div class="page">
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div><h2>本院设备实时监测</h2><div class="sub">每 30 秒采集，15 秒自动刷新</div></div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
      <div class="grid">
        <div v-for="m in list" :key="m.equipment_id" class="eq" :class="{ off: m.monitor_status !== '1' }" @click="open(m)">
          <div class="head"><span class="dot" :class="m.monitor_status === '1' ? 'on' : 'off'"></span>{{ m.equipment_name }}</div>
          <div class="loc">{{ m.campus_name }} · {{ m.area_name }}</div>
          <div class="metrics">
            <div v-for="mt in (m.metrics || []).slice(0, 3)" :key="mt.pointId" class="m">
              <div class="mv">{{ mt.value ?? '--' }}<small>{{ mt.unit }}</small></div>
              <div class="ml">{{ metricName(mt.metric) }}</div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!list.length" style="text-align:center;color:#94a3b8;padding:40px">暂无设备</div>
    </div>

    <el-drawer v-model="drawer" size="520px" :title="detail?.equipment?.equipment_name || '设备详情'">
      <div v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="位置">{{ detail.equipment.campus_name }} / {{ detail.equipment.area_name }}</el-descriptions-item>
          <el-descriptions-item label="编号">{{ detail.equipment.equipment_code }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.equipment.run_status === '2' ? '故障' : '运行' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.points" size="small" style="margin-top:12px">
        <el-table-column type="index" label="序号" width="62" align="center" />
          <el-table-column prop="point_name" label="点位" />
          <el-table-column label="实时值" width="110">
        <template #default="{ row }">{{ row.latestValue ?? '--' }} {{ row.unit }}</template></el-table-column>
          <el-table-column label="告警范围" width="140">
        <template #default="{ row }">{{ row.alarm_lower ?? '-' }} ~ {{ row.alarm_upper ?? '-' }}</template></el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/request'

const list = ref([])
const drawer = ref(false)
const detail = ref(null)
let timer = null
const metricMap = { temperature: '温度', humidity: '湿度', pressure: '压差', wind_speed: '风速', particle: '粒子', oxygen_pressure: '氧气', vacuum: '真空度', run_state: '状态' }
const metricName = (m) => metricMap[m] || m

async function load() { list.value = (await request.get('/portal/monitor')).data.list }
async function open(m) {
  drawer.value = true
  detail.value = (await request.get('/portal/monitor/equipment/' + m.equipment_id)).data
}
onMounted(() => { load(); timer = setInterval(load, 15000) })
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; margin-top: 14px; }
.eq { border: 1px solid #eef2f7; border-radius: 10px; padding: 12px 14px; cursor: pointer; }
.eq:hover { border-color: #99f6e4; box-shadow: 0 6px 18px rgba(13,148,136,.08); }
.eq.off { opacity: .65; }
.head { font-weight: 600; display: flex; align-items: center; gap: 8px; }
.dot { width: 8px; height: 8px; border-radius: 50%; } .dot.on { background: #14b8a6; } .dot.off { background: #94a3b8; }
.loc { font-size: 11px; color: #94a3b8; margin: 5px 0 10px; }
.metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.m { background: #f6fbfa; border-radius: 8px; padding: 8px 4px; text-align: center; }
.mv { font-size: 16px; font-weight: 700; color: #0f766e; } .mv small { font-size: 10px; color: #94a3b8; }
.ml { font-size: 10px; color: #8494a8; }
</style>
