<template>
  <CrudPage resource="department" detail :detail-tabs="tabs" :readonly="!!store.companyId" />
</template>

<script setup>
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()

const tabs = [
  {
    key: 'equipment',
    label: '设备',
    resource: 'equipment',
    param: 'departmentId',
    columns: [
      { prop: 'equipmentCode', label: '设备编号', width: 150 },
      { prop: 'equipmentName', label: '设备名称' },
      { prop: 'model', label: '型号', width: 140 },
      { prop: 'runStatus', label: '运行状态', width: 110 }
    ]
  },
  {
    key: 'contact',
    label: '对接人',
    resource: 'contact',
    param: 'departmentId',
    columns: [
      { prop: 'contactName', label: '姓名', width: 110 },
      { prop: 'position', label: '职务', width: 130 },
      { prop: 'phone', label: '联系电话', width: 140 }
    ]
  }
]
</script>
