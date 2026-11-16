/**
 * 关键接口集成冒烟测试。
 * 使用前启动后端，按需设置环境变量：
 *   API_BASE=http://127.0.0.1:8080
 *   TEST_USERNAME=testco2admin
 *   TEST_PASSWORD=123456
 *   TEST_COMPANY_CODE=TESTCO2
 */
const base = (process.env.API_BASE || 'http://127.0.0.1:8080').replace(/\/$/, '')
const username = process.env.TEST_USERNAME
const password = process.env.TEST_PASSWORD
const companyCode = process.env.TEST_COMPANY_CODE || ''
let failed = 0

if (!username || !password) {
  console.error('请设置 TEST_USERNAME、TEST_PASSWORD 后再运行集成测试')
  process.exit(1)
}

async function request(path, options = {}) {
  const response = await fetch(base + path, options)
  const text = await response.text()
  let body
  try {
    body = JSON.parse(text)
  } catch {
    body = text
  }
  return { status: response.status, body }
}

function check(name, condition, detail = '') {
  if (condition) {
    console.log(`PASS  ${name}`)
  } else {
    failed++
    console.error(`FAIL  ${name}${detail ? '：' + detail : ''}`)
  }
}

async function main() {
  const health = await request('/actuator/health')
  check('健康检查', health.status === 200 && health.body?.status === 'UP', JSON.stringify(health.body))

  const features = await request('/api/public/features')
  check('公开配置接口', features.status === 200 && features.body?.code === 200, JSON.stringify(features.body))

  const login = await request('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ companyCode, username, password })
  })
  check('登录', login.body?.code === 200 && !!login.body?.data?.token, JSON.stringify(login.body))
  if (login.body?.code !== 200) return printResult()

  const token = login.body.data.token
  const headers = { Authorization: 'Bearer ' + token }

  const info = await request('/api/auth/info', { headers })
  check('获取当前用户信息', info.body?.code === 200 && !!info.body?.data?.user, JSON.stringify(info.body))

  const workorders = await request('/api/workorder/page?pageNum=1&pageSize=10', { headers })
  check('工单分页查询', workorders.body?.code === 200 && Array.isArray(workorders.body?.data?.records), JSON.stringify(workorders.body))

  const alarms = await request('/api/alarm/page?pageNum=1&pageSize=10', { headers })
  check('告警分页查询', alarms.body?.code === 200 && Array.isArray(alarms.body?.data?.records), JSON.stringify(alarms.body))

  printResult()
}

function printResult() {
  console.log('')
  if (failed > 0) {
    console.error(`集成测试失败：${failed} 项`)
    process.exitCode = 1
  } else {
    console.log('集成测试通过')
  }
}

main().catch((error) => {
  console.error('集成测试异常：', error)
  process.exitCode = 1
})
