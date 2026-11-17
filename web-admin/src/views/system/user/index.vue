<template>
  <div class="app-container">
    <div class="page-card">
      <el-form inline>
        <el-form-item label="姓名"><el-input v-model="query.nickName" clearable style="width: 120px" placeholder="姓名" @keyup.enter="search" /></el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="query.postId" clearable style="width: 150px" placeholder="全部岗位">
            <el-option v-for="p in posts" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="query.phone" clearable style="width: 140px" placeholder="手机号" @keyup.enter="search" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 100px">
            <el-option label="正常" value="0" /><el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!store.companyId" label="公司">
          <el-select v-model="query.companyId" clearable filterable style="width: 190px">
            <el-option v-for="c in companies" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="query.deptId" :data="deptTree" :props="{ label: 'deptName', value: 'deptId', children: 'children' }" check-strictly clearable style="width: 160px" placeholder="全部部门" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.userType" clearable style="width: 130px" placeholder="全部类型">
            <el-option label="内部员工" value="1" />
            <el-option label="院方账号" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" clearable style="width: 130px" placeholder="全部角色">
            <el-option v-for="r in roles" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button :icon="Plus" @click="openAdd" v-if="has('add')">新增员工</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.userType === '2' ? 'success' : 'info'" effect="plain">
              {{ row.userType === '2' ? '院方' : '内部' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nickName" label="姓名" width="110" />
        <el-table-column prop="deptName" label="部门" width="140" />
        <el-table-column label="所属机构" width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.userType === '2' ? (row.hospitalName || '—') : (row.companyName || '—') }}</template>
        </el-table-column>
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
      <el-alert
        v-if="store.companyId"
        title="院方账号由平台管理员统一创建，公司管理员只能创建公司员工或工程师。"
        type="info"
        :closable="false"
        show-icon
        class="hospital-account-tip"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="账号类型">
          <el-radio-group v-model="accountType" :disabled="!!form.userId" class="account-type-group" @change="onAccountTypeChange">
            <el-radio-button v-for="type in accountTypes" :key="type.value" :value="type.value">{{ type.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="账号" prop="username" required><el-input v-model="form.username" :disabled="!!form.userId" maxlength="32" placeholder="3-32位字母/数字/下划线" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名" prop="nickName" required><el-input v-model="form.nickName" maxlength="30" /></el-form-item></el-col>
          <el-col v-if="!isHospitalType" :span="12"><el-form-item label="部门"><el-tree-select v-model="form.deptId" :data="deptTree" :props="{ label: 'deptName', value: 'deptId', children: 'children' }" check-strictly :disabled="!!form.engineerId" style="width: 100%" @change="onDeptChange" /></el-form-item></el-col>
          <el-col v-if="!isHospitalType" :span="12"><el-form-item label="岗位"><el-select v-model="form.postId" clearable style="width: 100%"><el-option v-for="p in posts" :key="p.value" :label="p.label" :value="p.value" /></el-select></el-form-item></el-col>
          <el-col v-if="!isHospitalType" :span="12"><el-form-item label="所属公司"><el-select v-model="form.companyId" :disabled="!!store.companyId" clearable style="width: 100%" @change="onCompanyChange"><el-option v-for="c in companies" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item></el-col>
          <el-col v-if="isHospitalType" :span="12"><el-form-item label="院方医院" required><el-select v-model="form.hospitalId" filterable style="width: 100%" placeholder="选择院方所属医院" @change="onHospitalChange"><el-option v-for="h in hospitals" :key="h.value" :label="h.label" :value="h.value" /></el-select></el-form-item></el-col>
          <el-col v-if="accountType === 'hospital_sub_admin'" :span="12"><el-form-item label="院方科室" required><el-select v-model="form.hospitalDepartmentId" filterable style="width: 100%" placeholder="选择所属科室"><el-option v-for="d in hospitalDepartments" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item></el-col>
          <el-col v-if="accountType === 'engineer'" :span="12"><el-form-item label="绑定工程师"><el-select v-model="form.engineerId" clearable filterable :disabled="!canBindEngineer" style="width: 100%" :placeholder="canBindEngineer ? '选择工程师档案' : '请先选择维保部或下级部门'" @change="onEngineerChange"><el-option v-for="e in engineers" :key="e.value" :label="e.label" :value="e.value" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号（登录用）" prop="phone"><el-input v-model="form.phone" maxlength="11" inputmode="numeric" placeholder="11位手机号" @input="onPhoneInput" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱（登录用）" prop="email"><el-input v-model="form.email" maxlength="100" placeholder="name@example.com" /></el-form-item></el-col>
          <el-col v-if="!isHospitalType" :span="12"><el-form-item label="角色"><el-select v-model="form.roleIds" multiple style="width: 100%" @change="onRoleChange"><el-option v-for="r in roles" :key="r.roleId" :label="r.roleName" :value="r.roleId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">正常</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-alert v-if="!form.userId" type="info" :closable="false" title="新增员工使用系统参数中的账号初始密码，首次登录需修改" />
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
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
const accountType = ref('company')
const accountTypes = computed(() => {
  const types = [
    { value: 'company', label: '公司员工' },
    { value: 'engineer', label: '工程师' }
  ]
  if (!store.companyId) {
    types.push(
      { value: 'hospital_admin', label: '院方管理员' },
      { value: 'hospital_sub_admin', label: '院方子管理员' },
      { value: 'hospital_dept', label: '院方科室用户' }
    )
  }
  return types
})
const isHospitalType = computed(() => ['hospital_admin', 'hospital_sub_admin', 'hospital_dept'].includes(accountType.value))
const hospitalDepartments = ref([])
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, nickName: '', phone: '', status: undefined, companyId: undefined, deptId: undefined, postId: undefined, roleId: undefined, userType: undefined })
const dialog = ref(false)
const form = reactive({})
const formRef = ref()
const rules = {
  username: [
    { required: true, message: '请填写登录账号', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]{2,31}$/, message: '账号需以字母开头，3-32位字母、数字或下划线', trigger: 'blur' }
  ],
  nickName: [
    { required: true, message: '请填写姓名', trigger: 'blur' },
    { max: 30, message: '姓名不能超过30个字符', trigger: 'blur' }
  ],
  phone: [{
    validator: (_rule, value, callback) => {
      if (!value || /^1[3-9]\d{9}$/.test(value)) callback()
      else callback(new Error('请输入11位手机号'))
    },
    trigger: 'blur'
  }],
  email: [{
    validator: (_rule, value, callback) => {
      if (!value || /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value)) callback()
      else callback(new Error('邮箱格式不正确'))
    },
    trigger: 'blur'
  }]
}
const deptTree = ref([])
const posts = ref([])
const roles = ref([])
const engineers = ref([])
const companies = ref([])
const hospitals = ref([])

const isHospitalRole = computed(() => (form.roleIds || []).some((id) => {
  const role = roles.value.find((r) => String(r.roleId) === String(id))
  return role && String(role.roleName || '').startsWith('院方')
}))

function onAccountTypeChange(type) {
  form.hospitalId = undefined
  form.hospitalDepartmentId = undefined
  form.engineerId = undefined
  if (type === 'hospital_admin' || type === 'hospital_dept' || type === 'hospital_sub_admin') {
    const roleName = type === 'hospital_admin' ? '院方管理员'
      : type === 'hospital_sub_admin' ? '院方子管理员' : '院方科室用户'
    const role = roles.value.find((r) => r.roleName === roleName)
    form.roleIds = role ? [role.roleId] : []
  } else if (type === 'engineer') {
    const role = roles.value.find((r) => r.roleName === '维保工程师')
    form.roleIds = role ? [role.roleId] : []
  } else {
    form.roleIds = []
  }
}

async function onHospitalChange(hospitalId) {
  form.hospitalDepartmentId = undefined
  hospitalDepartments.value = []
  if (!hospitalId) { form.companyId = undefined; return }
  try {
    const res = await request.get('/crud/hospital/' + hospitalId)
    form.companyId = res.data?.companyId
  } catch (e) { /* ignore */ }
  if (accountType.value === 'hospital_sub_admin') await loadHospitalDepartments(hospitalId)
}

async function loadHospitalDepartments(hospitalId) {
  if (!hospitalId) { hospitalDepartments.value = []; return }
  try {
    const dept = await request.get('/crud/department/options', { params: { hospitalId } })
    hospitalDepartments.value = dept.data || []
  } catch (e) { hospitalDepartments.value = [] }
}

function onRoleChange() {
  if (!isHospitalRole.value) form.hospitalId = undefined
  else if (!hospitals.value.length) loadHospitals()
}

async function loadHospitals() {
  try {
    const res = await request.get('/crud/hospital/options')
    hospitals.value = res.data || []
  } catch (e) {
    hospitals.value = []
  }
}

function buildTree(list) {
  const map = {}, roots = []
  list.forEach((d) => (map[d.deptId] = { ...d, children: [] }))
  list.forEach((d) => d.parentId && map[d.parentId] ? map[d.parentId].children.push(map[d.deptId]) : roots.push(map[d.deptId]))
  return roots
}

function deptPath(id, nodes = deptTree.value, path = []) {
  for (const node of nodes) {
    const next = [...path, node]
    if (String(node.deptId) === String(id)) return next
    const found = deptPath(id, node.children || [], next)
    if (found.length) return found
  }
  return []
}

const canBindEngineer = computed(() => {
  const path = deptPath(form.deptId)
  return path.some((d) => ['维保部', '维保中心', '维保事业部'].includes(d.deptName))
})

function onDeptChange() {
  if (!canBindEngineer.value) form.engineerId = undefined
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
function resetSearch() {
  Object.assign(query, { pageNum: 1, nickName: '', phone: '', status: undefined, companyId: undefined, deptId: undefined, postId: undefined, roleId: undefined, userType: undefined })
  load()
}
function reset(obj) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, obj) }

function openAdd() {
  accountType.value = 'company'
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
  if (!form.companyId && engineer.companyId) form.companyId = engineer.companyId
  if (engineer.deptId) form.deptId = engineer.deptId
  if (!form.roleIds?.length) {
    const engineerRole = roles.value.find((r) => r.roleName === '维保工程师')
    if (engineerRole) form.roleIds = [engineerRole.roleId]
  }
}

function onPhoneInput(value) {
  form.phone = String(value || '').replace(/\D/g, '').slice(0, 11)
}

async function openEdit(row) {
  const res = await request.get('/system/user/' + row.userId)
  reset({ ...res.data })
  if (form.hospitalId) {
    const roleNames = (form.roleIds || []).map((id) => roles.value.find((r) => String(r.roleId) === String(id))?.roleName || '')
    accountType.value = roleNames.includes('院方子管理员') ? 'hospital_sub_admin'
      : roleNames.includes('院方科室用户') ? 'hospital_dept' : 'hospital_admin'
  } else if (form.engineerId) {
    accountType.value = 'engineer'
  } else {
    accountType.value = 'company'
  }
  if (accountType.value === 'hospital_sub_admin' && form.hospitalId) await loadHospitalDepartments(form.hospitalId)
  dialog.value = true
  loadEngineers()
}
async function submit() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  if (!form.username || !form.nickName) { ElMessage.warning('请填写账号与姓名'); return }
  if (isHospitalType.value && !form.hospitalId) { ElMessage.warning('院方类型请选择所属医院'); return }
  if (accountType.value === 'hospital_sub_admin' && !form.hospitalDepartmentId) { ElMessage.warning('院方子管理员请选择所属科室'); return }
  saving.value = true
  try {
    const payload = { ...form, accountType: accountType.value }
    if (form.userId) await request.put('/system/user', payload)
    else await request.post('/system/user', payload)
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
  await ElMessageBox.confirm(`确认将 ${row.nickName} 的密码重置为系统初始密码？`, '提示', { type: 'warning' })
  await request.put(`/system/user/${row.userId}/resetPwd`)
  ElMessage.success('已重置为系统初始密码')
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
  await loadHospitals()
  load()
})
</script>

<style scoped>
.pager { margin-top: 14px; justify-content: flex-end; }
.hospital-account-tip { margin-bottom: 12px; }
.account-type-group { display: flex; flex-wrap: nowrap; }
.account-type-group :deep(.el-radio-button__inner) {
  padding: 8px 14px;
  white-space: nowrap;
}
</style>


