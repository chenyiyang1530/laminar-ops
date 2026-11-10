<template>
  <div class="not-found">
    <div class="code">404</div>
    <p>页面不存在或暂无访问权限</p>
    <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
  </div>
</template>

<style scoped>
.not-found { height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background: #f4f6fa; }
.code { font-size: 92px; font-weight: 800; color: #dbe4ef; letter-spacing: 4px; }
p { color: #8494a8; }
</style>
