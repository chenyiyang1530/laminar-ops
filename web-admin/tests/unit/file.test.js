import { describe, it, expect } from 'vitest'
import { authFileUrl } from '@/utils/file'

describe('authFileUrl', () => {
  it('keeps external URLs unchanged', () => {
    expect(authFileUrl('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png')
  })

  it('converts legacy /upload path to authenticated download', () => {
    localStorage.setItem('laminar_token', 'token-123')
    const url = authFileUrl('/upload/platform/demo/a.pdf')
    expect(url).toBe('/api/common/file?path=platform%2Fdemo%2Fa.pdf&token=token-123')
  })

  it('adds token to an existing download URL', () => {
    localStorage.setItem('laminar_token', 'token-456')
    expect(authFileUrl('/api/common/file?path=a.pdf'))
      .toBe('/api/common/file?path=a.pdf&token=token-456')
  })

  it('returns empty string for empty value', () => {
    expect(authFileUrl('')).toBe('')
  })
})

describe('portal authFileUrl', () => {
  it('uses portal token', async () => {
    localStorage.setItem('portal_token', 'portal-token')
    const { authFileUrl: portalFileUrl } = await import('../../src-portal/file.js')
    expect(portalFileUrl('/upload/company/7/a.png'))
      .toBe('/api/common/file?path=company%2F7%2Fa.png&token=portal-token')
  })
})
