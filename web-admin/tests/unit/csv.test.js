import { describe, it, expect } from 'vitest'
import { csvCell } from '@/utils/csv'

describe('csvCell', () => {
  it('escapes quotes and wraps values', () => {
    expect(csvCell('正常')).toBe('"正常"')
    expect(csvCell('a"b')).toBe('"a""b"')
  })

  it('treats null and undefined as empty', () => {
    expect(csvCell(null)).toBe('""')
    expect(csvCell(undefined)).toBe('""')
  })
})
