export function csvCell(value) {
  return `"${String(value ?? '').replace(/"/g, '""')}"`
}

export function downloadCsv(filename, headers, rows) {
  const lines = [headers.map(csvCell).join(',')]
  rows.forEach((row) => lines.push(row.map(csvCell).join(',')))
  const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
