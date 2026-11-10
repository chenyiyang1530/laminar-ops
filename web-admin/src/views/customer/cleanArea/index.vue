<template>
  <CrudPage resource="cleanArea" :readonly="!!store.companyId" />
</template>

<script setup>
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
</script>
