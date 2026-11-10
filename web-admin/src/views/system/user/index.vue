<template>
  <div class="app-container">
    <div class="page-card">
      <el-form inline>
        <el-form-item label="账号"><el-input v-model="query.username" clearable style="width: 150px" @keyup.enter="search" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="query.nickName" clearable style="width: 150px" @keyup.enter="search" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 110px">
            <el-option label="正常" value="0" /><el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Plus" @click="openAdd" v-if="has('add')">新增员工</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="nickName" label="姓名" width="110" />
        <el-table-column prop="deptName" label="部门" width="140" />
        <el-table-column prop="companyName" label="公司" width="170" show-overflow-tooltip />
        <el-table-column prop="postName" label="岗位" width="120" />
        <el-table-column prop="engineerName" label="绑定工程师" width="120" />
        <el-table-column prop="roleNames" label="角色" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.status === '0'" :disabled="!has('edit') || row.userId === 1" @change="(v) => toggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column prop="loginDate" label="最后登录" width="150" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-if="has('edit')" @click="openEdit(row)">编辑</el-button>
            <el-button link v-if="has('edit')" @click="resetPwd(row)">重置密码</el-button>
            <el-button link type="danger" v-if="has('remove') && row.userId !== 1" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50, 100]" class="pager" @change="load" />
    </div>

    <el-dialog v-model="dialog" :title="form.userId ? '编辑员工' : '新增员工'" width="760px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="账号" required><el-input v-model="form.username" :disabled="!!form.userId" placeholder="登录账号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="form.nickName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="部门"><el-tree-select v-model="form.deptId" :data="deptTree" :props="{ label: 'deptName', value: 'deptId', children: 'children' }" check-strictly style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="岗位"><el-select v-model="form.postId" clearable style="width: 100%"><el-option v-for="p in posts" :key="p.value" :label="p.label" :value="p.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属公司"><el-select v-model="form.companyId" :disabled="!!store.companyId" clearable style="width: 100%" @change="onCompanyChange"><el-option v-for="c in companies" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="绑定工程师"><el-select v-model="form.engineerId" clearable filterable style="width: 100%" placeholder="不绑定" @change="onEngineerChange"><el-option v-for="e in engineers" :key="e.value" :label="e.label" :value="e.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号（登录用）"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱（登录用）"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="角色"><el-select v-model="form.roleIds" multiple style="width: 100%"><el-option v-for="r in roles" :key="r.roleId" :label="r.roleName" :value="r.roleId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">正常</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-alert v-if="!form.userId" type="info" :closable="false" title="新增员工默认密码 123456" />
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const has = (a) => store.hasPerm('system:user:' + a)
const records = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, username: '', nickName: '', status: undefined })
const dialog = ref(false)
const form = reactive({})
const deptTree = ref([])
const posts = ref([])
const roles = ref([])
const engineers = ref([])
const companies = ref([])

function buildTree(list) {
  const map = {}, roots = []
  list.forEach((d) => (map[d.deptId] = { ...d, children: [] }))
  list.forEach((d) => d.parentId && map[d.parentId] ? map[d.parentId].children.push(map[d.deptId]) : roots.push(map[d.deptId]))
  return roots
}
async function load() {
  loading.value = true
  try {
    const res = await request.get('/system/user/page', { params: query })
    records.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
const search = () => { query.pageNum = 1; load() }
function reset(obj) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, obj) }

function openAdd() {
  reset({ status: '0', gender: '0', roleIds: [], companyId: store.companyId || undefined })
  dialog.value = true
  loadEngineers()
}

async function loadEngineers() {
  if (!form.companyId && !store.companyId) {
    engineers.value = []
    return
  }
  try {
    const res = await request.get('/system/user/engineerOptions', {
      params: { companyId: form.companyId || store.companyId, userId: form.userId }
    })
    engineers.value = res.data || []
  } catch (e) {
    engineers.value = []
  }
}

function onCompanyChange() {
  form.engineerId = undefined
  loadEngineers()
}

function onEngineerChange(engineerId) {
  const engineer = engineers.value.find((e) => String(e.value) === String(engineerId))
  if (!engineer) return
  if (!form.nickName) form.nickName = engineer.label
  if (!form.phone) form.phone = engineer.phone || ''
}

async function openEdit(row) {
  const res = await request.get('/system/user/' + row.userId)
  reset({ ...res.data })
  dialog.value = true
  loadEngineers()
}
async function submit() {
  if (!form.username || !form.nickName) { ElMessage.warning('请填写账号与姓名'); return }
  saving.value = true
  try {
    if (form.userId) await request.put('/system/user', form)
    else await request.post('/system/user', form)
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally { saving.value = false }
}
async function toggle(row, v) {
  await request.put(`/system/user/${row.userId}/status?status=${v ? '0' : '1'}`)
  row.status = v ? '0' : '1'
  ElMessage.success('状态已更新')
}
async function resetPwd(row) {
  await ElMessageBox.confirm(`确认将 ${row.nickName} 的密码重置为 123456？`, '提示', { type: 'warning' })
  await request.put(`/system/user/${row.userId}/resetPwd`)
  ElMessage.success('已重置')
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除员工 ${row.nickName}？`, '提示', { type: 'warning' })
  await request.delete('/system/user/' + row.userId)
  ElMessage.success('已删除')
  load()
}
onMounted(async () => {
  const [d, p, r, c] = await Promise.all([
    request.get('/system/dept/tree'),
    request.get('/system/user/postOptions'),
    request.get('/system/user/roleOptions'),
    request.get('/crud/company/options')
  ])
  deptTree.value = buildTree(d.data)
  posts.value = p.data
  roles.value = r.data
  companies.value = c.data
  load()
})
</script>

<style scoped>
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
