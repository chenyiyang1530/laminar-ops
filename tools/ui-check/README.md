# 前端页面验收工具

这个目录里的脚本用于在本机浏览器中真实打开页面、登录测试账号并截图。

## 使用

在 IDEA 终端或普通 PowerShell 中执行：

```powershell
cd D:\ideaWork\laminar-ops\tools\ui-check
$env:HEADFUL = '1'
node .\acceptance.js
```

默认访问：

```text
http://127.0.0.1:8080
```

如果页面端口不同：

```powershell
$env:BASE_URL = 'http://127.0.0.1:8080'
```

如果找不到 Chromium：

```powershell
$env:CHROME_PATH = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
```

输出截图在：

```text
tools/ui-check/shots
```

## 当前覆盖

- 登录页测试账号；
- `test_ceo` 登录；
- 公司高管菜单；
- 审批中心；
- 公司审批记录页签；
- `test_eng` 登录；
- 请假申请页。

脚本会输出浏览器控制台错误数量；有错误时退出码非 0。
