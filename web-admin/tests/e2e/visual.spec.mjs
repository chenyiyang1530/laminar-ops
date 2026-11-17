import { test, expect } from '@playwright/test'


test('登录页视觉基线', async ({ page }) => {
  await page.goto('/app/login')
  await expect(page.locator('.form-box')).toBeVisible()
  await expect(page).toHaveScreenshot('login-page.png', {
    animations: 'disabled',
    caret: 'hide',
    maxDiffPixelRatio: 0.05
  })
})

