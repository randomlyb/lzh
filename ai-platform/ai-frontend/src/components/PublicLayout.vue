<template>
  <div class="public-layout">
    <header class="public-header">
      <div class="header-inner">
        <a href="/" class="logo">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <span class="logo-text">TechMind AI 技术知识平台</span>
        </a>
        <nav class="nav-links">
          <a href="/" class="nav-link">首页</a>
          <a href="/login" class="nav-link">登录</a>
          <a href="/admin/home" class="btn-primary">进入后台</a>
        </nav>
      </div>
    </header>
    <main class="public-main">
      <router-view />
    </main>
    <footer class="public-footer">
      <div class="footer-inner">
        <span>TechMind AI 技术知识平台 &copy; 2026</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}
.public-header {
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #1e293b;
}
.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.logo-icon svg {
  width: 20px;
  height: 20px;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 24px;
}
.nav-link {
  color: #64748b;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s;
}
.nav-link:hover {
  color: #1e293b;
}
.btn-primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;
}
.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(102,126,234,0.4);
}
.public-main {
  flex: 1;
}
.public-footer {
  background: #fff;
  border-top: 1px solid #e2e8f0;
  padding: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
</style>