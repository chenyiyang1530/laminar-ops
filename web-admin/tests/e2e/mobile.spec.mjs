import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test.use({
  viewport: { width: 390, height: 844 },
  isMobile: true,
  hasTouch: true,
  deviceScaleFactor: 3
})

test('院方端移动尺寸可登录并查看工单', async ({ page }) => {
  await page.goto('/portal/login')
  await page.getByPlaceholder('院方账号').fill('e2e_hospital')
  await page.getByPlaceholder('密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/portal\/dashboard/, { timeout: 20_000 })

  await page.goto('/portal/workorders')
  await expect(page.getByText('报修工单').first()).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText(data.title).first()).toBeVisible({ timeout: 15_000 })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(overflow).toBeLessThanOrEqual(2)
})
