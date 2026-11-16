<template>
  <div class="app-container">
    <div class="page-card release-page">
      <div class="page-title">
        <div>
          <h2>版本更新</h2>
          <div class="sub">记录每次发布改了什么，方便按版本追溯</div>
        </div>
        <div class="page-actions">
          <el-button v-if="records.length" link @click="expandAll">全部展开</el-button>
          <el-button v-if="records.length" link @click="collapseAll">全部收起</el-button>
          <el-button v-if="canEdit" type="primary" :icon="Plus" @click="openAdd">发布新版本</el-button>
        </div>
      </div>

      <el-empty v-if="!records.length" description="暂无版本记录" />

      <div v-else class="release-list">
        <article v-for="(item, index) in records" :key="item.id" class="release-card" :class="{ open: isExpanded(item) }">
          <div class="release-rail">
            <span class="dot" :class="{ latest: index === 0 }" />
          </div>
          <div class="release-body">
            <div class="release-head" @click="toggle(item)">
              <div class="release-title-row">
                <el-tag effect="dark" type="primary" size="small">{{ item.version }}</el-tag>
                <el-tag v-if="index === 0" type="success" size="small">最新</el-tag>
                <el-tag v-if="item.status === '1'" type="info" size="small">草稿</el-tag>
                <h3>{{ item.title }}</h3>
              </div>
              <div class="head-right">
                <span class="expand-text">{{ isExpanded(item) ? '收起' : '展开' }}</span>
                <el-icon class="chevron" :class="{ rotated: isExpanded(item) }"><ArrowDown /></el-icon>
                <div class="release-actions" v-if="canEdit" @click.stop>
                  <el-button link type="primary" @click="openEdit(item)">编辑</el-button>
                  <el-button link type="danger" @click="remove(item)">删除</el-button>
                </div>
              </div>
            </div>
            <div class="release-date">{{ item.releaseDate }}</div>

            <div v-show="isExpanded(item)" class="release-details">
              <p v-if="item.summary" class="release-summary">{{ item.summary }}</p>

              <section v-if="lines(item.features).length" class="release-section">
                <h4><span class="mark feature" />新增功能</h4>
                <ul><li v-for="line in lines(item.features)" :key="line">{{ line }}</li></ul>
              </section>
              <section v-if="lines(item.improvements).length" class="release-section">
                <h4><span class="mark improve" />体验优化</h4>
                <ul><li v-for="line in lines(item.improvements)" :key="line">{{ line }}</li></ul>
              </section>
              <section v-if="lines(item.fixes).length" class="release-section">
                <h4><span class="mark fix" />问题修复</h4>
                <ul><li v-for="line in lines(item.fixes)" :key="line">{{ line }}</li></ul>
              </section>
            </div>
          </div>
        </article>
      </div>

      <div v-if="backendInfo" class="build-info">
        后端 {{ backendInfo.version }} · 构建 {{ backendInfo.buildTime }}
        <span v-if="backendInfo.gitCommit && backendInfo.gitCommit !== 'unknown'"> · {{ backendInfo.gitCommit }}</span>
      </div>
      <div v-else-if="backendError" class="build-info error">
        无法获取后端版本：8080 可能仍在运行旧进程，请重启后端后再试。
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑版本' : '发布新版本'" width="720px" destroy-on-close>
      <el-form :model="form" label-width="88px">
        <el-form-item label="版本号" required>
          <el-input v-model="form.version" placeholder="如 v1.6.0" style="width: 220px" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="本次版本主题" />
        </el-form-item>
        <el-form-item label="发布日期" required>
          <el-date-picker v-model="form.releaseDate" type="date" value-format="YYYY-MM-DD" style="width: 220px" />
        </el-form-item>
        <el-form-item label="一句话摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="新增功能">
          <el-input v-model="form.features" type="textarea" :rows="4" placeholder="每行一条" />
        </el-form-item>
        <el-form-item label="体验优化">
          <el-input v-model="form.improvements" type="textarea" :rows="4" placeholder="每行一条" />
        </el-form-item>
        <el-form-item label="问题修复">
          <el-input v-model="form.fixes" type="textarea" :rows="4" placeholder="每行一条" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">发布</el-radio>
            <el-radio value="1">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canEdit = computed(() => store.isSuperAdmin || store.hasPerm('system:release:edit'))
const records = ref([])
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})
const expandedIds = ref(new Set())
const backendInfo = ref(null)
const backendError = ref(false)

const lines = (text) => (text || '').split(/\r?\n/).map((s) => s.trim()).filter(Boolean)
const isExpanded = (item) => expandedIds.value.has(item.id)

function toggle(item) {
  const next = new Set(expandedIds.value)
  if (next.has(item.id)) next.delete(item.id)
  else next.add(item.id)
  expandedIds.value = next
}

function expandAll() {
  expandedIds.value = new Set(records.value.map((item) => item.id))
}

function collapseAll() {
  expandedIds.value = new Set()
}

async function load() {
  records.value = (await request.get('/release/list')).data || []
  expandedIds.value = new Set(records.value.length ? [records.value[0].id] : [])
}

function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  const today = new Date()
  const local = new Date(today.getTime() - today.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
  Object.assign(form, { version: '', title: '', releaseDate: local, status: '0' })
  dialog.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialog.value = true
}

async function submit() {
  if (!form.version || !form.title || !form.releaseDate) {
    ElMessage.warning('请填写版本号、标题和发布日期')
    return
  }
  saving.value = true
  try {
    if (form.id) await request.put('/release', form)
    else await request.post('/release', form)
    ElMessage.success('已保存')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除版本 ${row.version}？`, '提示', { type: 'warning' })
  await request.delete(`/release/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await load()
  try { await store.markReleaseSeen() } catch (e) { /* ignore */ }
  try {
    backendInfo.value = (await request.get('/public/version')).data
  } catch (e) {
    backendError.value = true
  }
})
</script>

<style scoped>
.release-page { max-width: 1040px; }
.page-actions { display: flex; align-items: center; gap: 4px; }
.release-list { margin-top: 8px; }
.release-card { display: flex; gap: 16px; }
.release-rail { width: 16px; display: flex; flex-direction: column; align-items: center; flex: 0 0 16px; }
.release-rail .dot { width: 9px; height: 9px; border-radius: 50%; background: #c7d3e2; margin-top: 8px; position: relative; }
.release-rail .dot.latest { background: var(--brand); box-shadow: 0 0 0 4px rgba(45, 107, 255, 0.12); }
.release-card:not(:last-child) .release-rail::after {
  content: ''; width: 1px; flex: 1; background: #e6ebf2; margin-top: 8px;
}
.release-body { flex: 1; padding: 0 0 28px; }
.release-card:not(.open) .release-body { padding-bottom: 14px; }
.release-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; cursor: pointer; user-select: none; }
.release-title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.release-title-row h3 { margin: 0; font-size: 16px; font-weight: 600; color: #1f2d3d; }
.head-right { display: flex; align-items: center; gap: 8px; flex: 0 0 auto; }
.expand-text { font-size: 12px; color: #93a1b5; }
.chevron { color: #93a1b5; transition: transform .18s ease; }
.chevron.rotated { transform: rotate(180deg); }
.release-date { font-size: 12px; color: #93a1b5; margin-top: 7px; }
.release-details { padding-top: 4px; }
.release-summary { margin: 10px 0 4px; font-size: 13px; color: #5b6b7d; line-height: 1.7; }
.release-section { margin-top: 14px; }
.release-section h4 { display: flex; align-items: center; gap: 7px; margin: 0 0 7px; font-size: 13px; font-weight: 600; color: #33465c; }
.mark { width: 7px; height: 7px; border-radius: 2px; display: inline-block; }
.mark.feature { background: #2d6bff; }
.mark.improve { background: #13a36f; }
.mark.fix { background: #e6a23c; }
.release-section ul { margin: 0; padding-left: 18px; }
.release-section li { font-size: 13px; color: #5b6b7d; line-height: 1.9; }
.release-actions { flex: 0 0 auto; }
.build-info { margin-top: 18px; padding-top: 12px; border-top: 1px solid #eef2f7; font-size: 11px; color: #93a1b5; }
.build-info.error { color: #e6a23c; }
</style>

