<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div><h2>部门管理</h2><div class="sub">公司内部组织架构</div></div>
        <div class="page-actions">
          <el-select
            v-if="isSuper"
            v-model="selectedCompanyId"
            placeholder="选择公司"
            style="width: 220px"
            @change="load"
          >
            <el-option v-for="c in companies" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
          <el-button v-if="canAdd" type="primary" :icon="Plus" @click="openAdd(0)">新增部门</el-button>
        </div>
      </div>
      <el-alert
        title="部门用于组织架构；只有关联了地理区域的部门，才会与请假审批区域对应。未关联的部门不参与审批链。"
        type="info"
        :closable="false"
        show-icon
        class="dept-tip"
      />
      <el-table :data="tree" row-key="deptId" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column v-if="isSuper" prop="companyName" label="公司" width="190" show-overflow-tooltip />
        <el-table-column prop="deptName" label="部门名称" min-width="220" />
        <el-table-column prop="regionName" label="地理区域" min-width="150" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="canAdd" link type="primary" @click="openAdd(row.deptId)">新增下级</el-button>
            <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canRemove" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.deptId ? '编辑部门' : '新增部门'" width="680px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="treeOptions" :props="{ label: 'deptName', value: 'deptId', children: 'children' }" check-strictly style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门名称" required><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="关联区域">
          <el-select v-model="form.regionId" clearable filterable style="width: 100%" placeholder="不关联地理区域">
            <el-option v-for="r in regions" :key="r.regionId" :label="r.regionName" :value="r.regionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" :disabled="!!form.regionId" :placeholder="form.regionId ? '保存后按区域主管同步' : ''" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const isSuper = computed(() => store.isSuperAdmin)
const canAdd = computed(() => store.hasPerm('system:dept:add'))
const canEdit = computed(() => store.hasPerm('system:dept:edit'))
const canRemove = computed(() => store.hasPerm('system:dept:remove'))
const companies = ref([])
const regions = ref([])
const selectedCompanyId = ref(null)
const flat = ref([])
const tree = ref([])
const treeOptions = ref([])
const dialog = ref(false)
const form = reactive({})

function build(list) {
  const map = {}, roots = []
  list.forEach((d) => (map[d.deptId] = { ...d, children: [] }))
  list.forEach((d) => d.parentId && map[d.parentId] ? map[d.parentId].children.push(map[d.deptId]) : roots.push(map[d.deptId]))
  return roots
}
async function load() {
  const params = selectedCompanyId.value ? { companyId: selectedCompanyId.value } : {}
  flat.value = (await request.get('/system/dept/tree', { params })).data
  tree.value = build(flat.value)
  treeOptions.value = [{ deptId: 0, deptName: '顶级部门', children: build(flat.value) }]
  const allRegions = (await request.get('/customer/region/options')).data || []
  regions.value = selectedCompanyId.value
    ? allRegions.filter((r) => String(r.companyId) === String(selectedCompanyId.value))
    : allRegions
}
function openAdd(parentId) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { parentId, sort: 0, companyId: selectedCompanyId.value || store.companyId || undefined }); dialog.value = true }
function openEdit(row) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row); dialog.value = true }
async function submit() {
  if (!form.deptName) { ElMessage.warning('请填写部门名称'); return }
  if (form.deptId) await request.put('/system/dept', form)
  else await request.post('/system/dept', form)
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除部门「${row.deptName}」？`, '提示', { type: 'warning' })
  await request.delete('/system/dept/' + row.deptId); ElMessage.success('已删除'); load()
}
onMounted(async () => {
  if (isSuper.value) {
    companies.value = (await request.get('/crud/company/options')).data || []
    if (companies.value.length) selectedCompanyId.value = companies.value[0].value
  }
  await load()
})
</script>

<style scoped>
.page-actions { display: flex; align-items: center; gap: 10px; }
.dept-tip { margin-bottom: 12px; }
</style>
