import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test('资料中心文件丢失显示状态而不是系统异常', async ({ page }) => {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_company')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })

  await page.goto('/app/resource/library')
  await expect(page.getByText('E2E文件丢失').first()).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText('文件丢失').first()).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText(/系统异常/)).toHaveCount(0)
})
