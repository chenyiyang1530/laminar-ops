import vue from 'rollup-plugin-vue'
import resolve from '@rollup/plugin-node-resolve'
import commonjs from '@rollup/plugin-commonjs'
import replace from '@rollup/plugin-replace'
import alias from '@rollup/plugin-alias'
import json from '@rollup/plugin-json'
import css from 'rollup-plugin-css-only'
import path from 'node:path'

const base = (process.env.APP_BASE || '/app/').replace(/\/?$/, '/')
const html = `<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>层流净化设备智能运维平台</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🌬️</text></svg>" />
    <link rel="stylesheet" href="${base}index.css?v=20261029" />
  </head>
  <body>
    <div id="app"></div>
    <script>window.__APP_BASE__='${base}'</script>
    <script type="module" src="${base}assets/main.js?v=20261029"></script>
  </body>
</html>`

export default {
  input: 'src/main.js',
  output: {
    dir: 'dist',
    format: 'es',
    entryFileNames: 'assets/main.js',
    chunkFileNames: 'assets/[name].js',
    assetFileNames: 'assets/[name][extname]'
  },
  plugins: [
    alias({ entries: [{ find: '@', replacement: path.resolve(process.cwd(), 'src') }] }),
    json(),
    replace({
      preventAssignment: true,
      'process.env.NODE_ENV': JSON.stringify('production'),
      __VUE_OPTIONS_API__: 'true',
      __VUE_PROD_DEVTOOLS__: 'false',
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false'
    }),
    vue({ css: false }),
    resolve({ browser: true, preferBuiltins: false, extensions: ['.js', '.vue', '.json', '.mjs', '.cjs'] }),
    commonjs(),
    css({ output: 'index.css' }),
    {
      name: 'emit-html',
      generateBundle() {
        this.emitFile({ type: 'asset', fileName: 'index.html', source: html })
      }
    }
  ],
  onwarn(warning, warn) {
    if (warning.code === 'CIRCULAR_DEPENDENCY') return
    warn(warning)
  }
}
