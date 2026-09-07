<template>
  <div class="admin-home">
    <!-- 状态栏 -->
    <div class="status-bar" v-loading="statsLoading">
      <div class="status-item">
        <span class="status-value">{{ stats.knowledgeBaseCount }}</span>
        <span class="status-label">知识库</span>
      </div>
      <div class="status-divider"></div>
      <div class="status-item">
        <span class="status-value">{{ stats.documentCount }}</span>
        <span class="status-label">文档</span>
      </div>
      <div class="status-divider"></div>
      <div class="status-item">
        <span class="status-value">{{ stats.jobCount }}</span>
        <span class="status-label">自动化任务</span>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 技术博客 -->
      <div class="section">
        <div class="section-header">
          <h2 class="section-title">
            <el-icon><Document /></el-icon>
            技术博客
          </h2>
          <router-link to="/admin/articles" class="view-all">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="blog-grid" v-loading="blogLoading">
          <div v-for="(blog, index) in blogList" :key="blog.id" class="blog-card" :style="{ animationDelay: index * 0.05 + 's' }">
            <div class="blog-card-accent" :style="{ background: accentColors[index % accentColors.length] }"></div>
            <div class="blog-card-content">
              <div class="blog-card-top">
                <div class="blog-category">
                  <el-icon><Document /></el-icon>
                  <span>{{ blog.tags || '技术' }}</span>
                </div>
                <span class="blog-date">{{ formatDate(blog.collectedTime) }}</span>
              </div>
              <h3 class="blog-title">
                <a :href="blog.sourceUrl" target="_blank" rel="noopener">{{ blog.title }}</a>
              </h3>
              <p class="blog-summary">{{ blog.summary || '暂无摘要' }}</p>
              <div class="blog-card-bottom">
                <a :href="blog.sourceUrl" target="_blank" rel="noopener" class="read-more">
                  阅读全文
                  <el-icon><ArrowRight /></el-icon>
                </a>
              </div>
            </div>
          </div>
          <el-empty v-if="!blogLoading && !blogList.length" description="暂无博客文章" />
        </div>
      </div>

      <!-- 最近知识库 -->
      <div class="section">
        <div class="section-header">
          <h2 class="section-title">
            <el-icon><FolderOpened /></el-icon>
            最近知识库
          </h2>
          <router-link to="/admin/knowledge-bases" class="view-all">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="kb-grid" v-loading="kbLoading">
          <div v-for="kb in kbList" :key="kb.id" class="kb-card">
            <div class="kb-icon" :style="{ background: kbColors[kb.id % kbColors.length] }">
              <el-icon :size="24"><FolderOpened /></el-icon>
            </div>
            <div class="kb-info">
              <h3 class="kb-name">{{ kb.name }}</h3>
              <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
              <span class="kb-doc-count">{{ kb.documentCount || 0 }} 篇文档</span>
            </div>
            <router-link :to="`/admin/knowledge-bases/${kb.id}`" class="kb-enter">
              进入
              <el-icon><ArrowRight /></el-icon>
            </router-link>
          </div>
          <el-empty v-if="!kbLoading && !kbList.length" description="暂无知识库" />
        </div>
      </div>

      <!-- 最近任务 -->
      <div class="section">
        <div class="section-header">
          <h2 class="section-title">
            <el-icon><Timer /></el-icon>
            任务概览
          </h2>
          <router-link to="/admin/jobs" class="view-all">
            管理任务
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="job-stats" v-loading="jobLoading">
          <div class="job-stat-card" :class="'stat-' + stat.type" v-for="stat in jobStats" :key="stat.type">
            <div class="stat-icon" :style="{ background: stat.gradient }">
              <el-icon :size="24"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stat.count }}</span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
          </div>
        </div>
        <div class="job-recent-list" v-if="jobList.length > 0">
          <div class="recent-title">最近任务</div>
          <div v-for="job in jobList" :key="job.id" class="job-item">
            <div class="job-info">
              <h4 class="job-name">{{ job.jobName }}</h4>
              <span class="job-cron">{{ job.cronExpression }}</span>
            </div>
            <div class="job-status">
              <el-tag :type="getJobStatusType(job.status)" size="small" effect="light">
                {{ getJobStatusText(job.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-empty v-if="!jobLoading && !jobList.length && jobStats.every(s => s.count === 0)" description="暂无任务" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Document, FolderOpened, Timer, ArrowRight, CircleCheck, Clock, Loading, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const statsLoading = ref(false)
const blogLoading = ref(false)
const kbLoading = ref(false)
const jobLoading = ref(false)

const stats = ref({
  knowledgeBaseCount: 0,
  documentCount: 0,
  jobCount: 0
})

const blogList = ref([])
const kbList = ref([])
const jobList = ref([])

const jobStats = ref([
  { type: 'running', label: '运行中', count: 0, icon: 'Loading', gradient: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { type: 'completed', label: '已完成', count: 0, icon: 'CircleCheck', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)' },
  { type: 'pending', label: '待执行', count: 0, icon: 'Clock', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { type: 'failed', label: '失败', count: 0, icon: 'Warning', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)' }
])

const accentColors = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a8edea, #fed6e3)'
]

const kbColors = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)'
]

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function getJobStatusType(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return map[status] || 'info'
}

function getJobStatusText(status) {
  const map = { 0: '待执行', 1: '运行中', 2: '已完成', 3: '失败' }
  return map[status] || '未知'
}

async function loadStats() {
  statsLoading.value = true
  try {
    const [kbRes, docRes, jobRes] = await Promise.all([
      fetch('/api/knowledge-bases', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } }),
      fetch('/api/documents', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } }),
      fetch('/api/jobs', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } })
    ])
    const kbData = await kbRes.json()
    const docData = await docRes.json()
    const jobData = await jobRes.json()
    stats.value = {
      knowledgeBaseCount: kbData.length || 0,
      documentCount: docData.length || 0,
      jobCount: jobData.length || 0
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  } finally {
    statsLoading.value = false
  }
}

async function loadBlogs() {
  blogLoading.value = true
  try {
    const res = await fetch('/api/blogs?limit=6', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } })
    const data = await res.json()
    blogList.value = data.slice(0, 6)
  } catch (e) {
    console.error('加载博客失败', e)
  } finally {
    blogLoading.value = false
  }
}

async function loadKnowledgeBases() {
  kbLoading.value = true
  try {
    const res = await fetch('/api/knowledge-bases', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } })
    const data = await res.json()
    kbList.value = data.slice(0, 4)
  } catch (e) {
    console.error('加载知识库失败', e)
  } finally {
    kbLoading.value = false
  }
}

async function loadJobs() {
  jobLoading.value = true
  try {
    const res = await fetch('/api/jobs', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } })
    const data = await res.json()
    jobList.value = data.slice(0, 5)
    
    // 计算任务统计
    const running = data.filter(j => j.status === 1).length
    const completed = data.filter(j => j.status === 2).length
    const pending = data.filter(j => j.status === 0).length
    const failed = data.filter(j => j.status === 3).length
    
    jobStats.value[0].count = running
    jobStats.value[1].count = completed
    jobStats.value[2].count = pending
    jobStats.value[3].count = failed
  } catch (e) {
    console.error('加载任务失败', e)
  } finally {
    jobLoading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadBlogs()
  loadKnowledgeBases()
  loadJobs()
})
</script>

<style scoped>
.admin-home {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100%;
}

.status-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.status-value {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
}

.status-label {
  font-size: 13px;
  color: #64748b;
}

.status-divider {
  width: 1px;
  height: 32px;
  background: #e2e8f0;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.view-all {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667eea;
  font-size: 14px;
  text-decoration: none;
  transition: opacity 0.2s;
}

.view-all:hover {
  opacity: 0.8;
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.blog-card {
  position: relative;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
  animation: fadeInUp 0.5s ease-out both;
}

.blog-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.blog-card-accent {
  height: 4px;
}

.blog-card-content {
  padding: 16px;
}

.blog-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.blog-category {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
}

.blog-date {
  font-size: 12px;
  color: #94a3b8;
}

.blog-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
  line-height: 1.4;
}

.blog-title a {
  color: #1e293b;
  text-decoration: none;
}

.blog-title a:hover {
  color: #667eea;
}

.blog-summary {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.blog-card-bottom {
  display: flex;
  justify-content: flex-end;
}

.read-more {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667eea;
  font-size: 14px;
  text-decoration: none;
}

.read-more:hover {
  opacity: 0.8;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.kb-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  transition: all 0.2s;
}

.kb-card:hover {
  background: #f1f5f9;
}

.kb-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.kb-info {
  flex: 1;
  min-width: 0;
}

.kb-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px;
}

.kb-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-doc-count {
  font-size: 12px;
  color: #94a3b8;
}

.kb-enter {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667eea;
  font-size: 14px;
  text-decoration: none;
  flex-shrink: 0;
}

.job-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.job-stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.job-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.job-recent-list {
  margin-top: 8px;
}

.recent-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
  padding-left: 4px;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.job-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  transition: background 0.2s;
}

.job-item:hover {
  background: #f1f5f9;
}

.job-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.job-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin: 0;
}

.job-cron {
  font-size: 12px;
  color: #94a3b8;
  font-family: monospace;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>