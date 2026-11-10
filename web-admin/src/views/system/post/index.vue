<template>
  <CrudPage resource="post">
    <template #intro>
      <el-alert
        title="岗位是员工职务，不是部门；部门请到「组织与权限 → 组织架构」维护。区域审批请到「地理区域」维护。"
        type="info"
        :closable="false"
        show-icon
        class="intro-tip"
      />
    </template>
  </CrudPage>
</template>

<script setup>
import CrudPage from '@/components/CrudPage.vue'
</script>

<style scoped>
.intro-tip { margin-bottom: 12px; }
</style>
