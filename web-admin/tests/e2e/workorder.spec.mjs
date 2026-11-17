import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))
const companyCode = 'FJASLYLKJ'
const title = data.title

async function loginAdmin(page, username, password = data.password) {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill(username)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })
}

async function loginPortal(page, username, password = data.password) {
  await page.goto('/portal/login')
  await page.getByPlaceholder('院方账号').fill(username)
  await page.getByPlaceholder('密码').fill(password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/portal\/dashboard/, { timeout: 20_000 })
}

async function findRow(page, text) {
  const row = page.locator('tr').filter({ hasText: text }).first()
  await expect(row).toBeVisible({ timeout: 15_000 })
  return row
}

test('登录 → 受理 → 派工 → 到场 → 完工 → 院方验收', async ({ page }) => {
  // 1. 公司管理员登录并受理
  await loginAdmin(page, 'e2e_company')
  await page.goto('/app/workorder/dispatch')
  await page.getByPlaceholder('工单号 / 标题').fill(title)
  await page.getByRole('button', { name: '查询' }).click()

  let row = await findRow(page, title)
  await row.getByRole('button', { name: '受理', exact: true }).click()
  await page.getByRole('button', { name: '确定', exact: true }).click()
  await expect(page.getByText('已受理').first()).toBeVisible({ timeout: 10_000 })

  // 2. 派工给 E2E 工程师
  row = await findRow(page, title)
  await row.getByRole('button', { name: '派工', exact: true }).click()
  const dispatchDialog = page.locator('.el-dialog').filter({ hasText: '派工' }).last()
  await dispatchDialog.getByRole('combobox').click()
  await page.getByText(/E2E工程师/).last().click()
  await dispatchDialog.getByRole('button', { name: '确认派工' }).click()
  await expect(page.getByText('派工成功').first()).toBeVisible({ timeout: 10_000 })

  // 3. 工程师登录，到场并完工
  await page.evaluate(() => localStorage.clear())
  await loginAdmin(page, 'e2e_engineer')
  await page.goto('/app/workorder/my')
  await page.getByPlaceholder('工单号 / 标题').fill(title)
  await page.getByRole('button', { name: '查询' }).click()

  row = await findRow(page, title)
  await row.getByRole('button', { name: '到场', exact: true }).click()
  await expect(page.getByText('已签到').first()).toBeVisible({ timeout: 10_000 })

  row = await findRow(page, title)
  await row.getByRole('button', { name: '完工', exact: true }).click()
  const finishDialog = page.locator('.el-dialog').filter({ hasText: '完工提交' }).last()
  const textareas = finishDialog.locator('textarea')
  await textareas.nth(0).fill('E2E 测试故障原因')
  await textareas.nth(1).fill('E2E 测试处理措施')
  await textareas.nth(2).fill('E2E 测试维修报告')
  await finishDialog.getByRole('button', { name: '提交验收' }).click()
  await expect(page.getByText('已提交验收').first()).toBeVisible({ timeout: 10_000 })

  // 4. 院方登录并验收
  await page.evaluate(() => localStorage.clear())
  await loginPortal(page, 'e2e_hospital')
  await page.goto('/portal/workorders')
  await expect(page.getByText(title).first()).toBeVisible({ timeout: 15_000 })

  row = await findRow(page, title)
  await row.getByRole('button', { name: '验收', exact: true }).click()
  const acceptDialog = page.locator('.el-dialog').filter({ hasText: '工单验收' }).last()
  await acceptDialog.getByText('通过', { exact: true }).click()
  await acceptDialog.getByRole('button', { name: '确认验收' }).click()
  await expect(page.getByText('验收已提交').first()).toBeVisible({ timeout: 10_000 })

  // 验收通过后状态为待回访
  await expect(page.getByText('待回访').first()).toBeVisible({ timeout: 10_000 })
})
