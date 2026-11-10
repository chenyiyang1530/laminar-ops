<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>审批代理</h2>
          <div class="sub">在指定日期范围内，把审批任务自动转交给代理人</div>
        </div>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增代理</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="targetName" label="代理人" width="140" />
        <el-table-column prop="startDate" label="开始日期" width="130" />
        <el-table-column prop="endDate" label="结束日期" width="130" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : row.status === '2' ? 'info' : 'warning'" size="small">
              {{ row.status === '0' ? '生效中' : row.status === '2' ? '已过期' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="note">代理期间的审批任务会直接分配给代理人，代理人可在审批中心处理。</div>
    </div>

    <el-dialog v-model="dialog" title="新增审批代理" width="480px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="代理人" required>
          <el-select v-model="form.targetUserId" filterable style="width: 100%" placeholder="选择代理人">
            <el-option v-for="u in users" :key="u.value" :label="u.label" :value="u.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围" required>
          <el-date-picker
            v-model="range"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref([])
const users = ref([])
const dialog = ref(false)
const range = ref([])
const form = reactive({ targetUserId: undefined })

async function load() {
  loading.value = true
  try {
    list.value = (await request.get('/approval/delegates')).data || []
  } finally {
    loading.value = false
  }
}

async function openAdd() {
  form.targetUserId = undefined
  range.value = []
  users.value = (await request.get('/approval/transferOptions')).data || []
  dialog.value = true
}

async function submit() {
  if (!form.targetUserId || !range.value || range.value.length !== 2) {
    ElMessage.warning('请选择代理人和日期范围')
    return
  }
  await request.post('/approval/delegates', {
    targetUserId: form.targetUserId,
    startDate: range.value[0],
    endDate: range.value[1]
  })
  ElMessage.success('代理已创建')
  dialog.value = false
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除这条审批代理？', '提示', { type: 'warning' })
  await request.delete(`/approval/delegates/${row.delegateId}`)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.note { margin-top: 16px; color: #8a99ab; font-size: 13px; }
</style>
