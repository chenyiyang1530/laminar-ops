import { describe, it, expect } from 'vitest'
import { effectScope, nextTick } from 'vue'
import { useColumnPrefs } from '@/composables/useColumnPrefs'

describe('useColumnPrefs', () => {
  it('uses defaults when storage is empty', () => {
    const scope = effectScope()
    const { visible } = scope.run(() => useColumnPrefs('test-columns', ['a', 'b']))
    expect(visible.value).toEqual(['a', 'b'])
    scope.stop()
  })

  it('restores saved preferences', () => {
    localStorage.setItem('test-columns-saved', JSON.stringify(['b']))
    const scope = effectScope()
    const { visible } = scope.run(() => useColumnPrefs('test-columns-saved', ['a', 'b']))
    expect(visible.value).toEqual(['b'])
    scope.stop()
  })

  it('persists changes and resets', async () => {
    const scope = effectScope()
    const { visible, reset } = scope.run(() => useColumnPrefs('test-columns-reset', ['a', 'b']))
    visible.value = ['b', 'a']
    await nextTick()
    expect(JSON.parse(localStorage.getItem('test-columns-reset'))).toEqual(['b', 'a'])
    reset()
    await nextTick()
    expect(visible.value).toEqual(['a', 'b'])
    expect(JSON.parse(localStorage.getItem('test-columns-reset'))).toEqual(['a', 'b'])
    scope.stop()
  })
})
