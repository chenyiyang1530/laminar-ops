import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test('真实表格内容固定数据截图', async ({ page }) => {
  await page.route(/\/api\/customer\/hospital\/page/, (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200,
      msg: '操作成功',
      data: {
        total: 1,
        records: [{
          hospitalId: 9001,
          hospitalName: 'E2E固定医院',
          hospitalCode: 'E2E-FIXED',
          regionName: '固定区域',
          grade: '三甲',
          hospitalType: '综合医院',
          contactPerson: '固定联系人',
          phone: '13800138000',
          status: '0'
        }]
      }
    })
  }))

  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })

  await page.goto('/app/customer/archive')
  await expect(page.getByText('E2E固定医院').first()).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText('E2E-FIXED').first()).toBeVisible()
  await expect(page).toHaveScreenshot('real-table-hospital.png', {
    fullPage: false,
    animations: 'disabled',
    caret: 'hide',
    maxDiffPixelRatio: 0.02,
    maskColor: '#f5f7fa',
    mask: [page.locator('.page-card .el-alert'), page.locator('.page-card canvas')]
  })
})

