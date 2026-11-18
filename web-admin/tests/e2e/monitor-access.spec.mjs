import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test('设备接入配置可新增网关并测试上报', async ({ page, request }) => {
  const code = `E2E-UI-GW-${Date.now().toString().slice(-6)}`
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })

  await page.goto('/app/workorder/monitor?tab=access')
  await expect(page.getByRole('heading', { name: '设备接入' })).toBeVisible({ timeout: 15_000 })
  await page.getByRole('button', { name: '新增网关' }).click()
  const dialog = page.getByRole('dialog').filter({ hasText: '新增网关' })
  await dialog.getByPlaceholder('例如 GW-AHU-01').fill(code)
  await dialog.getByPlaceholder('例如 住院楼 3F 网关').fill('E2E UI 网关')
  await dialog.getByPlaceholder('如 192.168.10.20:502').fill('192.168.10.20:502')
  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('网关已保存')).toBeVisible({ timeout: 10_000 })
  await expect(page.getByRole('dialog').filter({ hasText: '网关 Token' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByText(code).first()).toBeVisible()
  await expect(page.getByText('最近测试记录')).toBeVisible()

  await page.getByRole('button', { name: '审计' }).first().click()
  const auditDialog = page.getByRole('dialog').filter({ hasText: 'Token 审计' })
  await expect(auditDialog).toBeVisible()
  await expect(auditDialog.getByText('生成')).toBeVisible()
  await page.keyboard.press('Escape')

  await page.getByRole('button', { name: '测试上报' }).click()
  const testDialog = page.getByRole('dialog').filter({ hasText: '测试上报' })
  await testDialog.locator('.el-select').nth(0).click()
  await page.getByRole('option').filter({ hasText: 'E2E-AHU' }).first().click()
  await testDialog.locator('.el-select').nth(1).click()
  await page.getByRole('option').filter({ hasText: 'E2E-TEMP' }).first().click()
  await testDialog.getByPlaceholder('例如 23.6', { exact: true }).fill('23.6')
  await testDialog.getByRole('button', { name: '发送测试' }).click()
  await expect(testDialog.locator('.test-result')).toContainText('上报成功', { timeout: 10_000 })

  const login = await request.post('/api/auth/login', {
    data: { companyCode: data.companyCode, username: 'e2e_superadmin', password: data.password }
  })
  const headers = { Authorization: `Bearer ${(await login.json()).data.token}` }
  const list = await request.get('/api/monitor/access/gateways', { headers })
  const gateway = ((await list.json()).data || []).find((g) => g.gatewayCode === code)
  if (gateway) await request.delete(`/api/monitor/access/gateways/${gateway.gatewayId}`, { headers })
})
