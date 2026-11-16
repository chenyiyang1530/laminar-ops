# CI/CD

## 触发

```text
push main/master
pull request main/master
手动 workflow_dispatch
```

## CI 流水线

```text
.github/workflows/ci.yml
```

后端：

```text
JDK 17
mvn test
mvn package
上传 jar 构建产物
```

前端：

```text
Node 20
npm ci
npm run test:unit
npm run build
npm run build:portal
上传 dist / dist-portal
```

## 集成测试

CI 的 `integration` job 会启动：

```text
MySQL 8 服务容器
Redis 7 服务容器
后端 jar
```

执行：

```text
node tools/api-smoke-test.mjs
```

测试账号来自新库种子 `admin / Laminar@2026`，CI 会先关闭首次改密标记。

## 发布流水线

```text
.github/workflows/release.yml
```

触发：

```text
推送 v* 标签
```

产出：

```text
laminar-ops.jar
laminar-ops-admin.zip
laminar-ops-portal.zip
```

并创建 GitHub Release。

## 自动部署

```text
.github/workflows/deploy.yml
```

手动选择目标 host 和 GitHub Environment，使用：

```text
SSH_PRIVATE_KEY
SSH_USER
```

服务器执行：

```text
tools/deploy-linux.sh
```

健康检查失败自动切回上一个 release。

## 说明

当前 CD 不做服务器自动部署（除非配置 deploy workflow 的 Secrets 和服务器）。

