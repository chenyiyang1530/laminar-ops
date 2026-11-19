<template>
  <div class="reconcile-page">
    <div class="head">
      <div>
        <h2>库存对账</h2>
        <p>系统每日自动校验账面库存和库存余额汇总，差异需人工核销。</p>
      </div>
      <el-button type="primary" :disabled="!canRun" :loading="running" @click="runReconcile">立即对账</el-button>
    </div>

    <el-radio-group v-model="resolved" size="small" style="margin-bottom: 10px" @change="load">
      <el-radio-button label="">全部</el-radio-button>
      <el-radio-button label="0">待核销</el-radio-button>
      <el-radio-button label="1">已核销</el-radio-button>
    </el-radio-group>

    <el-table :data="records" size="small">
      <el-table-column prop="checkedAt" label="检查时间" width="170" />
      <el-table-column prop="partName" label="备件" min-width="150" />
      <el-table-column prop="warehouse" label="仓库" width="110" />
      <el-table-column prop="bookStock" label="账面" width="80" />
      <el-table-column prop="stockSum" label="余额汇总" width="100" />
      <el-table-column label="差异" width="80">
        <template #default="{ row }"><span class="diff">{{ row.difference }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.resolved === '1' ? 'success' : 'warning'">{{ row.resolved === '1' ? '已核销' : '待核销' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resolvedBy" label="核销人" width="100" />
      <el-table-column prop="resolveNote" label="核销说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="row.resolved !== '1' && canResolve" link type="primary" @click="resolve(row)">核销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="!records.length" class="empty">暂无对账差异</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canRun = store.hasPerm('resource:part:stock')
const canResolve = store.hasPerm('resource:requisition:approve')
const resolved = ref('')
const records = ref([])
const running = ref(false)

async function load() {
  const res = await request.get('/parts/reconcile/page', { params: { pageNum: 1, pageSize: 50, resolved: resolved.value } })
  records.value = res.data.records || []
}

async function runReconcile() {
  running.value = true
  try {
    const res = await request.post('/parts/reconcile')
    ElMessage.success(`对账完成，发现 ${res.data ?? 0} 个差异`)
    await load()
  } finally {
    running.value = false
  }
}

async function resolve(row) {
  const { value } = await ElMessageBox.prompt('请填写核销说明', '核销对账差异', { inputValue: '' })
  if (!value) { ElMessage.warning('核销说明不能为空'); return }
  await request.post(`/parts/reconcile/${row.id}/resolve`, { note: value })
  ElMessage.success('已核销')
  await load()
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.diff { color: #b91c1c; font-weight: 600; }
.empty { padding: 20px; text-align: center; color: #9ca3af; font-size: 12px; }
</style>
