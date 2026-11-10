import { reactive } from 'vue'

export const store = reactive({
  token: localStorage.getItem('portal_token') || '',
  profile: null
})
