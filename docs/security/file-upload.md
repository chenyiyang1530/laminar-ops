# 上传与文件下载

## 上传校验

接口：

```text
POST /api/common/upload
```

必须登录。

校验顺序：

```text
文件非空
文件大小不超过平台参数 platform.upload.maxSizeMb
扩展名在 platform.upload.allowedTypes 内
Apache Tika 检测真实文件类型
真实类型必须与扩展名一致
```

示例：

```text
把文本文件改名为 .pdf
→ 400 文件内容与扩展名不匹配
```

支持类型：

```text
jpg / jpeg / png / webp / pdf / doc / docx / xls / xlsx
```

## 文件访问

上传后返回：

```json
{
  "url": "/upload/company/9/xxx.png",
  "path": "company/9/xxx.png"
}
```

前端不再把登录 Token 拼在下载地址里，而是调用：

```text
GET /api/common/file/sign?path=...
```

返回短期签名 URL：

```text
/api/common/signed-file?path=...&expires=...&signature=...
```

签名参数：

```text
HMAC-SHA256
默认有效期 1800 秒
签名内容：path + 换行 + expires
```

验证失败或过期返回：

```json
{
  "code": 403,
  "msg": "文件下载链接无效或已过期"
}
```

签名 URL 可以公开访问，但只在有效期内有效，不需要携带 JWT。

## 配置

```yaml
laminar:
  file:
    sign-secret: ${FILE_SIGN_SECRET:}
    sign-ttl-seconds: ${FILE_SIGN_TTL_SECONDS:1800}
```

`FILE_SIGN_SECRET` 为空时复用 `CONFIG_SECRET`。

## 安全注意

```text
签名 URL 不要长期保存
不要把 JWT 放进 URL
上传目录不要直接暴露为静态资源
生产环境建议接入对象存储和病毒扫描
```

## 病毒扫描

配置：

```yaml
laminar:
  upload:
    virus-scan-enabled: true
    clamav-command: clamdscan
```

开启后文件会写入临时文件并调用：

```text
clamdscan --no-summary <temp-file>
```

退出码：

```text
0 = 通过
1 = 发现病毒，拒绝上传
其他 = 扫描服务异常，拒绝上传
```

未安装 ClamAV 时请保持关闭，否则上传会被拒绝。

## 历史文件清理

接口：

```text
POST /api/system/upload/cleanup?dryRun=true
```

默认只预演，不删除。

引用来源包括：

```text
证书文件
公司资质
合同附件
文档中心
巡检图片
工单现场照片
归档工单照片
平台 Logo
```

超过保留天数且没有被引用的文件才会被删除。

生产建议先 dryRun 审核，再关闭：

```yaml
laminar:
  upload:
    cleanup-enabled: false
    cleanup-retention-days: 180
```

