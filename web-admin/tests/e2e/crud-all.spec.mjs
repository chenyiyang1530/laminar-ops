import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))
const RESOURCES = [
  'company', 'companyQualification', 'region', 'hospital', 'campus', 'department',
  'contact', 'contract', 'deviceType', 'equipment', 'monitorPoint', 'engineer',
  'certificate', 'skill', 'part', 'document', 'knowledge', 'alarmRule',
  'inspectionPlan', 'maintenancePlan', 'optimization', 'post', 'dictType', 'dictData'
]

test('24 个 CRUD 资源新增、编辑、删除', async ({ request }) => {
  const login = await request.post('/api/auth/login', {
    data: { companyCode: data.companyCode, username: 'e2e_superadmin', password: data.password }
  })
  expect(login.ok()).toBeTruthy()
  const token = (await login.json()).data.token
  const headers = { Authorization: `Bearer ${token}` }
  const stamp = Date.now()
  const optionCache = new Map()
  const failures = []

  async function options(resource) {
    if (optionCache.has(resource)) return optionCache.get(resource)
    const res = await request.get(`/api/crud/${resource}/options`, { headers })
    const list = res.ok() ? (await res.json()).data || [] : []
    optionCache.set(resource, list)
    return list
  }

  function specialValue(field, res, stamp) {
    const name = field.name.toLowerCase()
    if (name.includes('code') || name.includes('no') || name.includes('type') || name.includes('value')) {
      return `E2E${res}${stamp}`.slice(0, 45)
    }
    if (name.includes('phone')) return '13800138000'
    if (name.includes('email')) return `e2e_${stamp}@example.com`
    if (name.includes('date') || field.type === 'date') return '2026-11-17'
    if (field.type === 'number') return 1
    return `E2E_${res}_${stamp}`
  }

  async function build(resource, meta, nameField) {
    const payload = {}
    for (const field of meta.fields || []) {
      if (!field.form || !field.required || field.virtual || field.readOnly) continue
      let value = field.defaultValue ?? null
      if (field.type === 'select') {
        const list = field.options ? await options(field.options) : []
        value = list[0]?.value ?? null
      } else if (field.type === 'dict') {
        value = field.dictType === 'monitor_metric' || field.name === 'metric' ? 'temperature' : (field.defaultValue ?? '0')
      } else if (field.type === 'number') {
        value = 1
      } else if (field.type === 'date') {
        value = '2026-11-17'
      } else if (field.type === 'datetime') {
        value = '2026-11-17 12:00:00'
      } else if (field.name === nameField) {
        value = `E2E_${resource}_${stamp}`
      } else {
        value = specialValue(field, resource, stamp)
      }
      if (value === null || value === '') {
        failures.push(`${resource}: 必填字段 ${field.name} 无可用值`)
      } else {
        payload[field.name] = value
      }
    }
    return payload
  }

  for (const resource of RESOURCES) {
    try {
      const metaRes = await request.get(`/api/crud/${resource}/meta`, { headers })
      if (!metaRes.ok()) throw new Error(`meta ${metaRes.status()}`)
      const meta = (await metaRes.json()).data
      const nameField = (meta.fields || []).find((f) => f.column === meta.nameColumn)?.name
      if (!nameField) throw new Error('找不到名称字段')

      const createPayload = await build(resource, meta, nameField)
      const created = await request.post(`/api/crud/${resource}`, { headers, data: createPayload })
      if (!created.ok()) throw new Error(`create ${created.status()} ${await created.text()}`)
      const createdBody = await created.json()
      if (createdBody.code !== 200 || !createdBody.data) throw new Error(`create code ${createdBody.code}`)
      const id = createdBody.data

      const updateValue = `${createPayload[nameField] || 'E2E'}_EDIT`
      const updated = await request.put(`/api/crud/${resource}`, {
        headers, data: { [meta.pk]: id, [nameField]: updateValue }
      })
      if (!updated.ok() || (await updated.json()).code !== 200) throw new Error(`update ${updated.status()}`)

      const detail = await request.get(`/api/crud/${resource}/${id}`, { headers })
      if (!detail.ok() || (await detail.json()).code !== 200) throw new Error(`get ${detail.status()}`)

      const deleted = await request.delete(`/api/crud/${resource}/${id}`, { headers })
      if (!deleted.ok() || (await deleted.json()).code !== 200) throw new Error(`delete ${deleted.status()}`)
    } catch (e) {
      failures.push(`${resource}: ${e.message}`)
    }
  }

  expect(failures, `CRUD 失败：\n${failures.join('\n')}`).toHaveLength(0)
})
