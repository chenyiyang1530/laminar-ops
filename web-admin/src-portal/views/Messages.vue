<template>
  <div class="page">
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div><h2>消息通知</h2><div class="sub">告警、工单、保养、合同到期等提醒</div></div>
        <el-button :icon="Check" @click="readAll">全部已读</el-button>
      </div>
      <div v-for="m in records" :key="m.msgId" class="msg" :class="{ unread: m.isRead === '0' }" @click="read(m)">
        <el-tag size="small" :type="m.level === '1' ? 'danger' : m.level === '2' ? 'warning' : 'info'" effect="plain">{{ typeText(m.msgType) }}</el-tag>
        <div class="body">
          <div class="title">{{ m.title }}</div>
          <div class="content">{{ m.content }}</div>
        </div>
        <div class="time">{{ (m.createTime || '').slice(0, 16) }}</div>
      </div>
      <div v-if="!records.length" style="text-align:center;color:#94a3b8;padding:40px">暂无消息</div>
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" class="pager" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Check } from '@element-plus/icons-vue'
import request from '@/request'

const records = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })
const typeText = (t) => ({ alarm: '告警', workorder: '工单', inspection: '巡检', maintenance: '保养', cert: '证书', contract: '合同', budget: '预算', system: '系统' }[t] || t)

async function load() {
  const res = await request.get('/portal/messages', { params: query })
  records.value = res.data.records
  total.value = res.data.total
}
async function read(m) {
  if (m.isRead === '0') { await request.put(`/portal/messages/${m.msgId}/read`); m.isRead = '1' }
}
async function readAll() { await request.put('/portal/messages/read-all'); load() }
onMounted(load)
</script>

<style scoped>
.msg { display: flex; gap: 12px; align-items: flex-start; padding: 14px 8px; border-bottom: 1px solid #f1f5f9; cursor: pointer; border-radius: 8px; }
.msg.unread { background: #f0fdfa; }
.msg:hover { background: #f8fafc; }
.body { flex: 1; min-width: 0; }
.title { font-size: 13px; font-weight: 600; color: #1f2d3d; }
.content { font-size: 12px; color: #7a8ba0; margin-top: 4px; }
.time { font-size: 11px; color: #a7b4c5; flex-shrink: 0; }
</style>
