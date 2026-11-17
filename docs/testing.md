# 测试说明

## 后端单元测试

```bash
cd backend
mvn test
```

当前覆盖：

```text
数据权限隔离
合同范围优先级
公司编码
JWT
密码
客户端信息
参数读取
```

## 前端单元测试

```bash
cd web-admin
npm run test:unit
```

当前覆盖：

```text
文件下载地址转换
CSV 转义
列显示偏好持久化
```

## 关键接口集成测试

先启动后端，再执行：

```bash
cd web-admin
npm run test:integration
```

默认不会内置测试账号密码；运行前必须设置：

```text
API_BASE=http://127.0.0.1:8080
TEST_USERNAME=有权限的测试账号
TEST_PASSWORD=测试账号密码
TEST_COMPANY_CODE=公司编码
```

示例：

```powershell
$env:TEST_USERNAME="你的测试账号"
$env:TEST_PASSWORD="测试账号密码"
$env:TEST_COMPANY_CODE="公司编码"
npm run test:integration
```

测试账号和测试工单已清理，不再默认使用 `testco2admin`。

覆盖：

```text
健康检查
公开配置接口
登录
当前用户信息
工单分页
告警分页
```

## 注意

集成测试只做只读查询，不创建、不删除、不归档业务数据。

前端 Vitest 依赖 esbuild 子进程；如果运行环境沙箱禁止 spawn，需要允许子进程或使用普通终端执行。
## 前端 E2E（登录到验收）

前置：

```text
后端 8080
MySQL/Redis
Edge 浏览器
E2E 测试数据会自动创建，不依赖现有公司/医院/设备
```

执行：

```bash
cd web-admin
npm install
npm run test:e2e
```

覆盖：

```text
公司管理员登录
受理
派工给 E2E 工程师
工程师登录、到场、完工
院方账号登录、验收驳回
工程师重新处理
院方再次验收通过
验收后状态为待回访
现场照片上传与签名下载
院方子管理员科室隔离
院方端移动尺寸
登录页截图基线（本地）
```

E2E 会自动创建两个科室工单和院方子管理员，用于验证：

```text
科室A子管理员只看到科室A工单
看不到科室B工单
```

视觉回归使用：

```text
web-admin/tests/e2e/__screenshots__/visual.spec.mjs/login-page.png
```

Windows 和 Linux 字体渲染不同，CI 中跳过视觉基线；需要单独生成 Linux 基线后再开启。


## CI 中的 E2E

`.github/workflows/ci.yml` 的 `e2e` job 会：

```text
启动 MySQL/Redis 服务
构建后端和前端
安装 Playwright Chromium
启动后端
自动创建 E2E 公司和测试数据
执行登录到验收链路
清理 E2E 数据
上传 Playwright 报告
```

测试数据不再写死 `company_id=7`，会自动创建：

```text
E2E_TEST 公司
E2E-HOSP 医院
E2E-AHU 设备
```


