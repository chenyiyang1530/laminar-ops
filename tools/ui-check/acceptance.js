const path = require('path');
const { chromium } = require('playwright-core');

const BASE = process.env.BASE_URL || 'http://127.0.0.1:8080';
const EXE = process.env.CHROME_PATH || 'C:\\Users\\admin\\AppData\\Local\\ms-playwright\\chromium-1248\\chrome-win64\\chrome.exe';
const shots = path.join(__dirname, 'shots');
require('fs').mkdirSync(shots, { recursive: true });

async function login(page, username) {
  await page.goto(BASE, { waitUntil: 'networkidle' });
  await page.getByText(username === 'test_ceo' ? '测试公司高管 test_ceo' : username === 'test_eng' ? '测试工程师 test_eng' : username, { exact: true }).click();
  await page.waitForTimeout(3000);
  await page.waitForLoadState('networkidle').catch(() => {});
}

(async () => {
  const browser = await chromium.launch({
    headless: process.env.HEADFUL !== '1',
    executablePath: EXE
  });
  const errors = [];
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));

  await login(page, 'test_ceo');
  await page.screenshot({ path: path.join(shots, '01-ceo-home.png'), fullPage: true });
  console.log('CEO_URL=' + page.url());
  console.log('CEO_HAS_APPROVAL_CENTER=' + await page.getByText('审批中心', { exact: true }).count());
  console.log('CEO_HAS_LEAVE_APPLY=' + await page.getByText('请假申请', { exact: true }).count());

  const approval = page.getByText('审批中心', { exact: true }).first();
  if (await approval.count()) {
    await approval.click();
    await page.waitForTimeout(1500);
    await page.screenshot({ path: path.join(shots, '02-approval-center.png'), fullPage: true });
    console.log('APPROVAL_URL=' + page.url());
    console.log('HAS_COMPANY_RECORDS_TAB=' + await page.getByText('公司审批记录', { exact: true }).count());
  }

  await context.close();

  const context2 = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page2 = await context2.newPage();
  page2.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  page2.on('pageerror', e => errors.push('pageerror: ' + e.message));
  await login(page2, 'test_eng');
  await page2.goto(BASE + '/staff/leave', { waitUntil: 'networkidle' }).catch(() => {});
  await page2.waitForTimeout(1200);
  await page2.screenshot({ path: path.join(shots, '03-leave-apply.png'), fullPage: true });
  console.log('ENGINEER_LEAVE_URL=' + page2.url());
  console.log('ENGINEER_HAS_APPLY=' + await page2.getByText('提交请假', { exact: true }).count());

  console.log('ERRORS=' + JSON.stringify(errors));
  await browser.close();
  if (errors.length) process.exit(2);
})().catch(async e => {
  console.error('ACCEPTANCE_FAILED=' + e.message);
  process.exit(1);
});
