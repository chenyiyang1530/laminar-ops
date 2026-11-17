import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

async function login(page) {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })
}

function mockPage(data, record, text) {
  return {
    code: 200,
    msg: '操作成功',
    data: { total: 1, records: [{ ...record, __text: text }] }
  }
}

test('真实表格内容固定数据截图', async ({ page }) => {
  await page.route(/\/api\/customer\/hospital\/page/, (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200, msg: '操作成功',
      data: { total: 1, records: [{ hospitalId: 9001, hospitalName: 'E2E固定医院', hospitalCode: 'E2E-FIXED', regionName: '固定区域', grade: '三甲', hospitalType: '综合医院', contactPerson: '固定联系人', phone: '13800138000', status: '0' }] }
    })
  }))
  await login(page)
  await page.goto('/app/customer/archive')
  await expect(page.getByText('E2E固定医院').first()).toBeVisible({ timeout: 15_000 })
  await expect(page).toHaveScreenshot('real-table-hospital.png', {
    fullPage: false, animations: 'disabled', caret: 'hide', maxDiffPixelRatio: 0.02, maskColor: '#f5f7fa',
    mask: [page.locator('.page-card .el-alert'), page.locator('.page-card canvas')]
  })
})

test('真实表格模式覆盖公司/设备/备件/优化页面', async ({ page }) => {
  await login(page)
  const cases = [
    {
      name: 'company', path: '/app/organization/overview', endpoint: /\/api\/crud\/company\/page/,
      record: { companyId: 9101, companyName: 'E2E固定公司', companyCode: 'E2E-COMPANY', contact: '固定联系人', phone: '13800138000', status: '0' },
      text: 'E2E固定公司'
    },
    {
      name: 'equipment', path: '/app/customer/archive?tab=equipment', endpoint: /\/api\/crud\/equipment\/page/,
      record: { equipmentId: 9201, equipmentName: 'E2E固定机组', equipmentCode: 'E2E-AHU', hospitalName: 'E2E固定医院', companyName: 'E2E固定公司', deviceTypeName: '新风机组', status: '0' },
      text: 'E2E固定机组'
    },
    {
      name: 'part', path: '/app/resource/parts', endpoint: /\/api\/crud\/part\/page/,
      record: { partId: 9301, partName: 'E2E固定滤网', partCode: 'E2E-PART', companyName: 'E2E固定公司', unit: '个', currentStock: 10, status: '0' },
      text: 'E2E固定滤网'
    },
    {
      name: 'optimization', path: '/app/system/optimization', endpoint: /\/api\/crud\/optimization\/page/,
      record: { id: 9401, title: 'E2E固定优化项', status: '0', priority: '中', createTime: '2026-11-18 00:00:00' },
      text: 'E2E固定优化项'
    }
  ]
  for (const item of cases) {
    await page.route(item.endpoint, (route) => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockPage(data, item.record, item.text))
    }))
    await page.goto(item.path)
    await expect(page.getByText(item.text).first()).toBeVisible({ timeout: 15_000 })
    await expect(page).toHaveScreenshot(`real-table-${item.name}.png`, {
      fullPage: false, animations: 'disabled', caret: 'hide', maxDiffPixelRatio: 0.02, maskColor: '#f5f7fa',
      mask: [page.locator('.page-card .el-alert'), page.locator('.page-card canvas')]
    })
  }
})
