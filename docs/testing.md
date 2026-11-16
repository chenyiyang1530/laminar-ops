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
