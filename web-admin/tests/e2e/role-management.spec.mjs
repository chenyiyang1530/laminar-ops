import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))
test.setTimeout(45_000)

async function login(page, username) {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill(username)
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })
}

test('标准角色不可删除', async ({ page, request }) => {
  await login(page, 'e2e_superadmin')
  await page.goto('/app/staff/people?tab=role')
  const row = page.locator('.el-table__row').filter({ hasText: '院方子管理员' }).first()
  await expect(row).toBeVisible({ timeout: 15_000 })
  await expect(row.getByText('标准', { exact: true }).first()).toBeVisible()
  await expect(row.getByText('平台标准')).toBeVisible()
  await expect(row.getByRole('button', { name: '删除' })).toHaveCount(0)

  const token = await page.evaluate(() => localStorage.getItem('laminar_token'))
  const headers = { Authorization: `Bearer ${token}` }
  const deleted = await request.delete('/api/system/role/202', { headers })
  expect((await deleted.json()).code).not.toBe(200)
  const roles = await request.get('/api/system/role/list', { headers })
  expect((await roles.json()).data.some((item) => item.roleId === 202)).toBeTruthy()
})

test('公司管理员创建院方子管理员账号自动带出角色', async ({ request }) => {
  const suffix = Date.now().toString().slice(-6)
  const username = `e2e_auto_sub_${suffix}`
  const nickName = `E2E自动子管理${suffix}`
  const loginRes = await request.post('/api/auth/login', {
    data: { companyCode: data.companyCode, username: 'e2e_company', password: data.password }
  })
  const companyToken = (await loginRes.json()).data.token
  const companyHeaders = { Authorization: `Bearer ${companyToken}` }

  const options = await request.get('/api/system/user/roleOptions', { headers: companyHeaders })
  expect((await options.json()).data.some((role) => role.roleId === 202)).toBeTruthy()

  const created = await request.post('/api/system/user', {
    headers: companyHeaders,
    data: {
      username,
      nickName,
      accountType: 'hospital_sub_admin',
      hospitalId: data.hospitalId,
      hospitalDepartmentId: data.deptAId,
      phone: '13800138000'
    }
  })
  const createdBody = await created.json()
  expect(createdBody.code, JSON.stringify(createdBody)).toBe(200)

  const superLogin = await request.post('/api/auth/login', {
    data: { companyCode: data.companyCode, username: 'e2e_superadmin', password: data.password }
  })
  const headers = { Authorization: `Bearer ${(await superLogin.json()).data.token}` }
  const rows = await request.get(`/api/system/user/page?pageNum=1&pageSize=20&nickName=${encodeURIComponent(nickName)}`, { headers })
  const record = ((await rows.json()).data.records || []).find((row) => row.username === username)
  expect(record, '创建的院方子管理员账号').toBeTruthy()
  expect(record.roleNames).toContain('院方子管理员')
  await request.delete(`/api/system/user/${record.userId}`, { headers })
})
