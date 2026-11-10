# 在 IntelliJ IDEA 中运行 Laminar Ops

## 1. 打开项目

- IDEA → `Open` → 选择 `D:\ideaWork\laminar-ops\backend`（含 `pom.xml` 的目录）
- 等待 Maven 依赖解析完成

## 2. 选择 JDK 17

- `File → Project Structure → Project`
  - SDK：17（没有就 `Add SDK → JDK`，指向 `D:\software\jdk17`）
  - Language level：17
- `Settings → Build, Execution, Deployment → Build Tools → Maven → Runner`：JRE 选 17

## 3. Lombok

- 确认 `Lombok` 插件启用
- `Settings → Build, Execution, Deployment → Compiler → Annotation Processors`：勾选 `Enable annotation processing`

## 4. 依赖与数据库

- 本机 Maven 本地仓库通过 `backend/.mvn/maven.config` 指向 `../.m2`（即 `D:\ideaWork\.m2`），无需重新下载
- 数据库已初始化（`laminar_ops`，51 张表）；如需重建，按顺序执行 `sql/01~05`

## 5. 启动

- 已内置运行配置 `LaminarOpsApplication`，右上角选择后点击绿色 ▶
- 或右键 `com.laminar.ops.LaminarOpsApplication` → `Run`

启动成功后：

| 端 | 地址 | 账号 |
| --- | --- | --- |
| 运维后台 | http://127.0.0.1:8080/app/login | admin / 123456 |
| 医院前台 | http://127.0.0.1:8080/portal/login | hospital1 / 123456 |
| 健康检查 | http://127.0.0.1:8080/actuator/health | — |

## 6. 前端修改后

```powershell
cd D:\ideaWork\laminar-ops\web-admin
npm install        # 首次
npm run build      # 运维后台
npm run build:portal
```

刷新浏览器即可，后端无需重启。

## 7. 常见问题

- **8080 被占用**：结束旧 java 进程，或修改 `application.yml` 的 `server.port`
- **Redis 连接失败**：启动 `D:\software\redis\redis-server.exe`
- **页面 404**：确认 `web-admin/dist`、`dist-portal` 存在且 `application.yml` 中路径正确
- **Maven 下载失败**：删除 `backend/.mvn/maven.config` 使用默认仓库，或配置国内镜像

## 前后端分离开发（推荐）

后端只跑 API（8080），两个前端用 Vite 独立启动（热更新）：

```powershell
# 终端 1：后端（或直接 IDEA Run LaminarOpsApplication）
cd D:\ideaWork\laminar-ops\backend
mvn spring-boot:run

# 终端 2：运维后台
cd D:\ideaWork\laminar-ops\web-admin
npm install
npm run dev          # http://localhost:5173/login

# 终端 3：医院前台
npm run dev:portal   # http://localhost:5174/login
```

- Vite `server.proxy` 已把 `/api`、`/upload` 代理到 `http://127.0.0.1:8080`；
- 开发阶段请访问 5173 / 5174，**不要**访问 8080 的 `/app`、`/portal`（那是后端托管的生产模式）；
- 一键启动三个窗口：右键 `start-dev.ps1` → 使用 PowerShell 运行。

若 `npm run dev` 报 `spawn EPERM`（极少见，通常是安全软件拦截）：

```powershell
cd D:\ideaWork\laminar-ops\web-admin
npm rebuild esbuild
```

## IDEA 一键启动（已内置运行配置）

在 IDEA 打开 `backend` 项目后，**重启一次 IDEA**（或 File → Reload All from Disk），右上角运行配置会出现：

| 配置 | 作用 |
| --- | --- |
| `LaminarOpsApplication` | 后端 8080 |
| `前端-运维后台 (5173)` | Vite 开发服务器 5173 |
| `前端-院方端 (5174)` | Vite 开发服务器 5174 |
| `启动全部 (后端+双前端)` | Compound，一键启动以上三个 |

使用前确认：

- `Settings → Languages & Frameworks → Node.js` 已选择 Node（路径 `D:\software\node\node.exe`）；
- 若无 Node.js 配置项，安装/启用 IDEA 的 Node.js 插件，或在该页面手动指定 Node 路径；
- 首次运行前端配置时，IDEA 会在 `web-admin` 下执行 `npm run dev` / `npm run dev:portal`。

若前端配置显示红色/找不到 package.json，可改为在 IDEA 里单独打开 `D:\ideaWork\laminar-ops\web-admin` 作为第二个项目，再用终端执行 npm 命令。

## 已生成的运行配置（.idea/runConfigurations）

| 配置 | 用途 |
| --- | --- |
| 后端 (JAR 8080) | 直接运行 `backend/target/laminar-ops-1.0.0.jar`，无需 Maven 模块导入 |
| 后端 (Spring Boot 8080) | 源码调试运行，需先完成 Maven 导入（模块名 `laminar-ops`） |
| 前端-运维后台 (5173) | `npm run dev` |
| 前端-院方端 (5174) | `npm run dev:portal` |
| 启动全部 (JAR+双前端) | 后端 JAR + 两个前端 |
| 启动全部 (源码+双前端) | Spring Boot + 两个前端 |

首次打开项目后：

1. 右侧 Maven 面板点 **Reload All Maven Projects**；
2. 若没有 `laminar-ops` 模块：右键 `backend/pom.xml` → **Add as Maven Project**；
3. `Settings → Languages & Frameworks → Node.js` 选择 `D:\software\node\node.exe`；
4. 想立即运行：选 **后端 (JAR 8080)**；想断点调试：选 **后端 (Spring Boot 8080)**。

> JAR 模式在代码修改后需要重新 `mvn -q -DskipTests package`；源码模式由 IDEA 自动编译。
