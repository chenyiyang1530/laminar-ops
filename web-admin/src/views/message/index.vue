<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>消息中心</h2>
          <div class="sub">告警、工单、保养、证书、合同到期等统一提醒</div>
        </div>
        <el-button type="primary" plain :icon="Check" @click="readAll">全部已读</el-button>
      </div>

      <el-form inline class="search-form">
        <el-form-item label="类型">
          <el-select v-model="query.msgType" clearable placeholder="全部" style="width: 150px" @change="search">
            <el-option v-for="d in dicts.type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isRead" clearable placeholder="全部" style="width: 120px" @change="search">
            <el-option label="未读" value="0" />
            <el-option label="已读" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </el-form-item>
      </el-form>

      <div v-loading="loading">
        <div v-if="!records.length" class="empty">暂无消息</div>
        <div v-for="m in records" :key="m.msgId" class="msg-item" :class="{ unread: m.isRead === '0' }" @click="openMsg(m)">
          <div class="msg-icon" :class="'lv' + (m.level || 3)">
            <el-icon><Warning v-if="m.level === '1' || m.level === '2'" /><Bell v-else /></el-icon>
          </div>
          <div class="msg-main">
            <div class="msg-title">
              <span class="dot" v-if="m.isRead === '0'"></span>{{ m.title }}
              <el-tag size="small" effect="plain" class="type-tag">{{ typeLabel(m.msgType) }}</el-tag>
            </div>
            <div class="msg-content">{{ m.content }}</div>
          </div>
          <div class="msg-time">{{ (m.createTime || '').slice(0, 16) }}</div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        class="pager"
        @change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh, Warning, Bell } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const store = useUserStore()
const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const dicts = reactive({ type: [] })
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, msgType: undefined, isRead: '0' })

function typeLabel(v) {
  return dicts.type.find((d) => d.dictValue === v)?.dictLabel || v
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/message/page', { params: query })
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

async function openMsg(m) {
  if (m.isRead === '0') {
    await request.put('/message/read/' + m.msgId)
    m.isRead = '1'
  }
  if (m.bizType === 'file_check' && m.bizId) {
    router.push({ path: '/system/file-check', query: { itemId: m.bizId } })
  }
}

async function readAll() {
  await request.put('/message/read-all')
  ElMessage.success('已全部标记为已读')
  load()
}

onMounted(async () => {
  const d = await request.get('/system/dict/type/message_type')
  dicts.type = d.data
  load()
})
</script>

<style scoped>
.search-form { margin-bottom: 4px; }
.msg-item { display: flex; gap: 14px; align-items: flex-start; padding: 14px 10px; border-bottom: 1px solid #f0f3f8; cursor: pointer; border-radius: 8px; }
.msg-item:hover { background: #f8fafd; }
.msg-item.unread { background: #f6f9ff; }
.msg-icon { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: #eef2f8; color: #64748b; }
.msg-icon.lv1 { background: #fee2e2; color: #dc2626; }
.msg-icon.lv2 { background: #fef3c7; color: #d97706; }
.msg-main { flex: 1; min-width: 0; }
.msg-title { font-size: 14px; color: #1f2d3d; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.dot { width: 7px; height: 7px; border-radius: 50%; background: #ef4444; display: inline-block; }
.type-tag { margin-left: auto; transform: scale(.92); }
.msg-content { font-size: 12px; color: #7a8ba0; margin-top: 5px; }
.msg-time { font-size: 11px; color: #a7b4c5; flex-shrink: 0; }
.empty { text-align: center; color: #a7b4c5; padding: 50px 0; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
