<template>
  <router-view />
</template>

<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import request from '@/request'

let sessionTimer = null

onMounted(() => {
  sessionTimer = setInterval(() => {
    if (localStorage.getItem('portal_token')) {
      request.get('/auth/session/status').catch(() => {})
    }
  }, 3000)
})

onBeforeUnmount(() => {
  if (sessionTimer) clearInterval(sessionTimer)
})
</script>

