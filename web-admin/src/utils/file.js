/**
 * 将数据库里的 /upload/... 地址转换为需要登录令牌的下载接口。
 * 旧数据仍保存 /upload/...，这里统一兼容。
 */
export function authFileUrl(url) {
  if (!url) return ''
  const token = localStorage.getItem('laminar_token') || ''
  if (url.startsWith('/api/common/file')) {
    return url + (url.includes('?') ? '&' : '?') + 'token=' + encodeURIComponent(token)
  }
  if (url.startsWith('/upload/')) {
    return `/api/common/file?path=${encodeURIComponent(url.slice('/upload/'.length))}&token=${encodeURIComponent(token)}`
  }
  return url
}
