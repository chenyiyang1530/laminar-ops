import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test('院方子管理员只能看到本科室工单', async ({ page }) => {
  await page.goto('/portal/login')
  await page.getByPlaceholder('院方账号').fill(data.subadminUsername)
  await page.getByPlaceholder('密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/portal\/dashboard/, { timeout: 20_000 })

  await page.goto('/portal/workorders')
  await expect(page.getByText(data.deptTitleA).first()).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText(data.deptTitleB)).toHaveCount(0)
})
