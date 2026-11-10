<template>
  <CrudPage
    resource="region"
    :hidden-fields="isCompanyUser ? ['companyId'] : []"
    :form-defaults="isCompanyUser ? { companyId: store.companyId } : {}"
  >
    <template #intro>
      <el-alert
        title="地理区域用于请假审批链、区域数据范围和区域负责人；它和「组织架构 → 部门」是两套树，部门可在编辑时关联对应区域。"
        type="info"
        :closable="false"
        show-icon
        class="intro-tip"
      />
    </template>
  </CrudPage>
</template>

<script setup>
import { computed } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const isCompanyUser = computed(() => !!store.companyId)
</script>

<style scoped>
.intro-tip { margin-bottom: 12px; }
</style>
