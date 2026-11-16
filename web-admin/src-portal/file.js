export function authFileUrl(url) {
  if (!url) return ''
  const token = localStorage.getItem('portal_token') || ''
  if (url.startsWith('/api/common/file')) {
    return url + (url.includes('?') ? '&' : '?') + 'token=' + encodeURIComponent(token)
  }
  if (url.startsWith('/upload/')) {
    return `/api/common/file?path=${encodeURIComponent(url.slice('/upload/'.length))}&token=${encodeURIComponent(token)}`
  }
  return url
}
