<template>
  <div class="home">
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-glow glow-1"></div>
        <div class="hero-glow glow-2"></div>
      </div>
      <div class="hero-content">
        <h1 class="hero-title">
          探索<span class="gradient-text"> AI 知识</span>的力量
        </h1>
        <p class="hero-desc">
          智能知识库管理平台，基于 RAG 技术实现文档检索与 AI 对话，
          让知识管理更高效、更智能
        </p>
        <div class="hero-actions">
          <router-link to="/login" class="hero-btn primary">
            <el-icon><User /></el-icon> 登录
          </router-link>
          <router-link to="/register" class="hero-btn secondary">
            <el-icon><UserFilled /></el-icon> 注册
          </router-link>
        </div>
      </div>
    </section>

    <section class="features">
      <div class="section-container">
        <h2 class="section-title">平台能力</h2>
        <div class="feature-grid">
          <div class="feature-card">
            <div class="feature-icon" style="background: linear-gradient(135deg, #667eea, #764ba2)">
              <el-icon :size="28"><FolderOpened /></el-icon>
            </div>
            <h3>知识库管理</h3>
            <p>创建和管理知识库，支持文档导入、标签分类和全文检索</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
              <el-icon :size="28"><ChatDotRound /></el-icon>
            </div>
            <h3>RAG 智能对话</h3>
            <p>基于检索增强生成技术，结合知识库内容进行精准 AI 问答</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
              <el-icon :size="28"><Search /></el-icon>
            </div>
            <h3>智能搜索</h3>
            <p>Elasticsearch 全文检索引擎，毫秒级搜索知识库内容</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon" style="background: linear-gradient(135deg, #a8edea, #fed6e3)">
              <el-icon :size="28"><Timer /></el-icon>
            </div>
            <h3>任务调度</h3>
            <p>Quartz 分布式任务调度，支持 Cron 表达式和固定频率触发</p>
          </div>
        </div>
      </div>
    </section>

    <section class="blog-showcase">
      <div class="section-container">
        <div class="blog-header">
          <h2 class="section-title" style="margin-bottom: 0">技术博客</h2>
          <p class="blog-subtitle">精选技术文章，持续更新中</p>
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
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBlogs } from '../api/job'
import { User, UserFilled, Document, ArrowRight } from '@element-plus/icons-vue'

const blogLoading = ref(false)
const blogList = ref([])

const accentColors = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a18cd1, #fbc2eb)',
]

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN')
}

onMounted(async () => {
  blogLoading.value = true
  try {
    blogList.value = await getBlogs(12)
  } catch (e) {
    console.error(e)
  } finally {
    blogLoading.value = false
  }
})
</script>

<style scoped>
.hero {
  position: relative;
  padding: 100px 24px 80px;
  text-align: center;
  overflow: hidden;
  background: #fff;
}

.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
}

.glow-1 {
  width: 400px;
  height: 400px;
  background: #667eea;
  top: -100px;
  left: -100px;
}

.glow-2 {
  width: 500px;
  height: 500px;
  background: #764ba2;
  bottom: -150px;
  right: -150px;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 700px;
  margin: 0 auto;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
  margin-bottom: 20px;
  letter-spacing: -1px;
}

.gradient-text {
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 18px;
  color: #64748b;
  line-height: 1.7;
  margin-bottom: 36px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}

.hero-btn.primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  box-shadow: 0 4px 20px rgba(102,126,234,0.4);
}

.hero-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(102,126,234,0.5);
}

.hero-btn.secondary {
  background: #fff;
  color: #1e293b;
  border: 2px solid #e2e8f0;
}

.hero-btn.secondary:hover {
  border-color: #667eea;
  color: #667eea;
}

.section-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.section-title {
  text-align: center;
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 48px;
}

.features {
  padding: 80px 0;
  background: #f8fafc;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.feature-card {
  background: #fff;
  padding: 32px 24px;
  border-radius: 16px;
  text-align: center;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
  border-color: transparent;
}

.feature-icon {
  width: 60px;
  height: 60px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin: 0 auto 20px;
}

.feature-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 10px;
}

.feature-card p {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}

.blog-showcase {
  padding: 80px 0;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
}

.blog-header {
  text-align: center;
  margin-bottom: 48px;
}

.blog-subtitle {
  font-size: 16px;
  color: #94a3b8;
  margin-top: 8px;
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.blog-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease both;
  position: relative;
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

.blog-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
  border-color: transparent;
}

.blog-card-accent {
  height: 4px;
  width: 100%;
}

.blog-card-content {
  padding: 24px;
}

.blog-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.blog-category {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: #f1f5f9;
  border-radius: 20px;
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.blog-category .el-icon {
  font-size: 14px;
}

.blog-date {
  font-size: 12px;
  color: #94a3b8;
}

.blog-title {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.blog-title a {
  color: #1e293b;
  text-decoration: none;
  transition: color 0.2s;
}

.blog-title a:hover {
  color: #667eea;
}

.blog-summary {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.blog-card-bottom {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.read-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
  text-decoration: none;
  transition: all 0.2s;
}

.read-more:hover {
  gap: 10px;
  color: #764ba2;
}

.read-more .el-icon {
  transition: transform 0.2s;
}

.read-more:hover .el-icon {
  transform: translateX(3px);
}

@media (max-width: 900px) {
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .blog-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: 32px; }
}

@media (max-width: 600px) {
  .feature-grid { grid-template-columns: 1fr; }
  .blog-grid { grid-template-columns: 1fr; }
  .hero-title { font-size: 28px; }
}
</style>