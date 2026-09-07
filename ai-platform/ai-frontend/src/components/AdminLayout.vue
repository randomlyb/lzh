<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">TechMind AI 技术知识平台</span>
          <span class="brand-sub">管理系统</span>
        </div>
      </div>

      <div class="sidebar-nav">
        <div class="nav-section">
          <span class="nav-section-title">内容</span>
          <router-link to="/admin/home" class="nav-item" :class="{ active: $route.path === '/admin/home' }">
            <el-icon :size="18"><HomeFilled /></el-icon>
            <span>首页</span>
          </router-link>
          <router-link to="/admin/articles" class="nav-item" :class="{ active: $route.path === '/admin/articles' }">
            <el-icon :size="18"><Document /></el-icon>
            <span>文章</span>
          </router-link>
        </div>

        <div class="nav-section">
          <span class="nav-section-title">AI能力</span>
          <router-link to="/admin/chat" class="nav-item" :class="{ active: $route.path === '/admin/chat' }">
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>AI对话</span>
          </router-link>
          <router-link to="/admin/knowledge-bases" class="nav-item" :class="{ active: $route.path.startsWith('/admin/knowledge-bases') }">
            <el-icon :size="18"><FolderOpened /></el-icon>
            <span>知识库</span>
          </router-link>
        </div>

        <div class="nav-section">
          <span class="nav-section-title">自动化</span>
          <router-link to="/admin/jobs" class="nav-item" :class="{ active: $route.path.startsWith('/admin/jobs') }">
            <el-icon :size="18"><Timer /></el-icon>
            <span>任务中心</span>
          </router-link>
        </div>
      </div>

      <div class="sidebar-footer">
        <div class="user-card">
          <el-avatar :size="36" :icon="UserFilled" />
          <div class="user-info">
            <span class="user-name">{{ userStore.userInfo?.username || '用户' }}</span>
            <span class="user-role">管理员</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand">
            <el-icon :size="18" class="more-icon"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-tooltip content="返回前台" placement="bottom">
            <el-button circle :icon="HomeFilled" @click="$router.push('/')" />
          </el-tooltip>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { UserFilled, HomeFilled, MoreFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentTitle = computed(() => route.meta.title || '')

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/')
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

.sidebar-brand {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.brand-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}

.brand-sub {
  color: rgba(255,255,255,0.45);
  font-size: 11px;
  font-weight: 500;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 8px;
}

.nav-section {
  margin-bottom: 20px;
}

.nav-section-title {
  display: block;
  padding: 0 12px 8px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(255,255,255,0.35);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: rgba(255,255,255,0.65);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: rgba(255,255,255,0.08);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(102,126,234,0.25), rgba(118,75,162,0.25));
  color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.user-name {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.user-role {
  color: rgba(255,255,255,0.45);
  font-size: 11px;
}

.more-icon {
  color: rgba(255,255,255,0.45);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.more-icon:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f1f5f9;
}

.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>