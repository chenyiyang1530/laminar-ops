import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test('文件巡检报告页面、趋势和收件人入口', async ({ page }) => {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })

  await page.goto('/app/system/file-check')
  await expect(page.getByText('文件巡检报告').first()).toBeVisible({ timeout: 15_000 })
  await expect(page.getByRole('button', { name: '导出 Excel' })).toBeVisible()
  await expect(page.getByRole('button', { name: '导出 PDF' })).toBeVisible()
  await expect(page.getByRole('tab', { name: '邮件收件人' })).toBeVisible()
  await page.getByRole('tab', { name: '邮件收件人' }).click()
  await expect(page.getByRole('button', { name: '新增收件人' })).toBeVisible()
})
