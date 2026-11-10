<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>智能派单</h2>
          <div class="sub">综合服务医院、院区位置、设备类型、技能标签、在岗状态与当前负载评分</div>
        </div>
      </div>

      <el-form inline>
        <el-form-item label="待派工单">
          <el-select v-model="workorderId" filterable placeholder="选择待派工工单" style="width: 420px" @change="load">
            <el-option v-for="w in workorders" :key="w.workorderId" :label="`${w.workorderNo} · ${w.title}（${w.hospitalName}）`" :value="w.workorderId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="MagicStick" :disabled="!workorderId" @click="load">重新推荐</el-button>
        </el-form-item>
      </el-form>

      <div v-if="result" class="wo-info">
        <el-tag type="info">{{ result.typeName }}</el-tag>
        <b>{{ result.workorderNo }}</b>
        <span>{{ result.workorderTitle }}</span>
      </div>

      <div v-loading="loading" class="cands">
        <div v-for="(c, i) in candidates" :key="c.engineer_id" class="cand" :class="{ best: i === 0 }">
          <div class="rank">{{ i + 1 }}</div>
          <div class="avatar">{{ c.engineer_name.slice(0, 1) }}</div>
          <div class="info">
            <div class="name">{{ c.engineer_name }} <el-tag v-if="c.on_duty === '1'" size="small" type="success" effect="plain">在岗</el-tag></div>
            <div class="meta">{{ c.job_title || '工程师' }} · 在手工单 {{ c.load_count }} 张</div>
            <div class="reasons">
              <el-tag v-for="r in c.reasons" :key="r" size="small" effect="plain">{{ r }}</el-tag>
            </div>
          </div>
          <div class="score">
            <div class="num">{{ c.score }}</div>
            <div class="lbl">综合得分</div>
          </div>
          <el-button type="primary" :disabled="!canDispatch" @click="dispatch(c)">派给他</el-button>
        </div>
        <div v-if="result && !candidates.length" class="empty">暂无可用工程师</div>
        <div v-if="!result" class="empty">请选择一张待派工工单</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const workorderId = ref()
const workorders = ref([])
const result = ref(null)
const candidates = ref([])
const loading = ref(false)
const canDispatch = store.hasPerm('workorder:dispatch')

async function loadWorkorders() {
  const res = await request.get('/workorder/page', { params: { pageNum: 1, pageSize: 100, status: '1' } })
  workorders.value = res.data.records
}
async function load() {
  if (!workorderId.value) return
  loading.value = true
  try {
    const res = await request.get('/dispatch/recommend/' + workorderId.value)
    result.value = res.data
    candidates.value = res.data.candidates
  } finally {
    loading.value = false
  }
}
async function dispatch(c) {
  await ElMessageBox.confirm(`确认将工单派给 ${c.engineer_name}（${c.score} 分）？`, '派工确认', { type: 'warning' })
  await request.post(`/workorder/${workorderId.value}/dispatch`, { engineerId: c.engineer_id, remark: '智能推荐派工' })
  ElMessage.success('派工成功')
  await loadWorkorders()
  workorderId.value = undefined
  result.value = null
  candidates.value = []
}
onMounted(loadWorkorders)
</script>

<style scoped>
.wo-info { margin: 12px 0; display: flex; gap: 10px; align-items: center; color: #33465c; }
.cands { margin-top: 12px; }
.cand { display: flex; align-items: center; gap: 14px; padding: 14px; border: 1px solid #eef2f7; border-radius: 10px; margin-bottom: 10px; }
.cand.best { border-color: #99f6e4; background: #f0fdfa; }
.rank { width: 24px; height: 24px; border-radius: 50%; background: #e2e8f0; color: #64748b; display: flex; align-items: center; justify-content: center; font-size: 12px; }
.cand.best .rank { background: #0f766e; color: #fff; }
.avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #ccfbf1, #99f6e4); color: #0f766e; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.info { flex: 1; min-width: 0; }
.name { font-size: 14px; font-weight: 600; display: flex; gap: 6px; align-items: center; }
.meta { font-size: 11px; color: #8494a8; margin: 3px 0 6px; }
.reasons { display: flex; gap: 6px; flex-wrap: wrap; }
.score { text-align: center; padding: 0 10px; }
.score .num { font-size: 22px; font-weight: 700; color: #0f766e; }
.score .lbl { font-size: 10px; color: #94a3b8; }
.empty { text-align: center; color: #a7b4c5; padding: 50px; }
</style>
