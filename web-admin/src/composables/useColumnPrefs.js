import { ref, watch } from 'vue'

export function useColumnPrefs(storageKey, defaults) {
  let initial = defaults
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey) || 'null')
    if (Array.isArray(saved) && saved.length) initial = saved
  } catch (e) { /* ignore */ }
  const visible = ref([...initial])
  watch(visible, (value) => {
    try { localStorage.setItem(storageKey, JSON.stringify(value)) } catch (e) { /* ignore */ }
  }, { deep: true })
  function reset() {
    visible.value = [...defaults]
    try { localStorage.removeItem(storageKey) } catch (e) { /* ignore */ }
  }
  return { visible, reset }
}
