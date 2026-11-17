import { test, expect } from '@playwright/test'

// Linux 与 Windows 的字体渲染不同，CI 使用 Linux 时应先生成对应基线。
test.skip(process.env.CI === 'true', 'CI Linux 需要单独生成视觉基线')

test('登录页视觉基线', async ({ page }) => {
  await page.goto('/app/login')
  await expect(page.locator('.form-box')).toBeVisible()
  await expect(page).toHaveScreenshot('login-page.png', {
    animations: 'disabled',
    caret: 'hide',
    maxDiffPixelRatio: 0.05
  })
})
