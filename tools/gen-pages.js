const fs = require('fs')
const path = require('path')

const base = path.join(process.cwd(), 'web-admin', 'src', 'views')
const pages = {
  'customer/campus/index': 'campus',
  'customer/department/index': 'department',
  'customer/cleanArea/index': 'cleanArea',
  'customer/contact/index': 'contact',
  'customer/contract/index': 'contract',
  'device/type/index': 'deviceType',
  'device/equipment/index': 'equipment',
  'device/point/index': 'monitorPoint',
  'device/rule/index': 'alarmRule',
  'staff/engineer/index': 'engineer',
  'staff/skill/index': 'skill',
  'staff/certificate/index': 'certificate',
  'resource/part/index': 'part',
  'resource/document/index': 'document',
  'resource/knowledge/index': 'knowledge',
  'budget/index': 'budget',
  'workorder/inspectionPlan/index': 'inspectionPlan',
  'workorder/maintenancePlan/index': 'maintenancePlan'
}

for (const [file, resource] of Object.entries(pages)) {
  const full = path.join(base, file + '.vue')
  fs.mkdirSync(path.dirname(full), { recursive: true })
  fs.writeFileSync(full, `<template>
  <CrudPage resource="${resource}" />
</template>

<script setup>
import CrudPage from '@/components/CrudPage.vue'
</script>
`)
}
console.log('crud pages:', Object.keys(pages).length)
