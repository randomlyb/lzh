<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>文章</h2>
        <p>浏览和搜索技术博客文章</p>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar card">
      <div class="card-body">
        <el-form :inline="true" @submit.prevent="handleSearch" size="large">
          <el-form-item>
            <el-input v-model="query" placeholder="搜索文章关键词..." style="width: 420px" clearable>
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleSearch" class="gradient-btn">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div class="card" style="margin-top: 20px" v-if="hasSearched" v-loading="loading">
      <div class="card-header">
        <h3>搜索结果</h3>
        <span v-if="totalHits > 0" class="result-count">共 {{ totalHits }} 条结果</span>
      </div>
      <div class="card-body">
        <div v-if="results.length" class="result-list">
          <div v-for="(item, idx) in results" :key="idx" class="result-item">
            <div class="result-rank">#{{ idx + 1 }}</div>
            <div class="result-main">
              <h4 class="result-title">
                {{ item.title || item.documentId || '无标题' }}
                <span class="result-score">相关度 {{ (item.score * 100).toFixed(0) }}%</span>
              </h4>
              <p class="result-content">{{ item.content?.substring(0, 300) || '暂无内容' }}</p>
              <div class="result-meta">
                <el-tag size="small" type="info">文档 ID: {{ item.documentId }}</el-tag>
                <a v-if="item.url" :href="item.url" target="_blank" class="result-link">查看原文</a>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="未找到相关文章" />
      </div>
    </div>

    <!-- 文章列表 -->
    <div class="card" style="margin-top: 20px" v-loading="blogLoading">
      <div class="card-header">
        <h3>全部文章</h3>
      </div>
      <div class="card-body">
        <div class="blog-grid">
          <div v-for="(blog, index) in blogList" :key="blog.id" class="blog-card">
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
          <el-empty v-if="!blogLoading && !blogList.length" description="暂无文章" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Document, ArrowRight } from '@element-plus/icons-vue'
import { search } from '../api/rag'

const query = ref('')
const loading = ref(false)
const results = ref([])
const totalHits = ref(0)
const hasSearched = ref(false)

const blogLoading = ref(false)
const blogList = ref([])

const accentColors = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a8edea, #fed6e3)'
]

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function handleSearch() {
  if (!query.value.trim()) return
  hasSearched.value = true
  loading.value = true
  try {
    const res = await search({ query: query.value, page: 1, size: 20 })
    results.value = res?.items || []
    totalHits.value = res?.totalHits || 0
  } catch (e) {
    console.error(e)
    results.value = []
    totalHits.value = 0
  } finally {
    loading.value = false
  }
}

async function loadBlogs() {
  blogLoading.value = true
  try {
    const res = await fetch('/api/blogs', {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    const data = await res.json()
    blogList.value = data || []
  } catch (e) {
    console.error('加载文章失败', e)
  } finally {
    blogLoading.value = false
  }
}

onMounted(() => {
  loadBlogs()
})
</script>

<style scoped>
.page { max-width: 1200px; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1e293b; margin-bottom: 4px; }
.page-header p { font-size: 14px; color: #94a3b8; }

.card { background: #fff; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; }
.card-header { padding: 20px 24px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; justify-content: space-between; }
.card-header h3 { font-size: 16px; font-weight: 600; color: #1e293b; }
.card-body { padding: 24px; }
.result-count { font-size: 13px; color: #94a3b8; }
.gradient-btn { background: linear-gradient(135deg, #667eea, #764ba2) !important; border: none !important; border-radius: 10px; font-weight: 600; }

.result-list { display: flex; flex-direction: column; gap: 16px; }
.result-item { display: flex; gap: 16px; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px; transition: all 0.2s; }
.result-item:hover { border-color: #667eea; box-shadow: 0 4px 16px rgba(102,126,234,0.08); }
.result-rank { font-size: 24px; font-weight: 800; color: #e2e8f0; width: 40px; flex-shrink: 0; text-align: center; line-height: 1; }
.result-main { flex: 1; }
.result-title { font-size: 16px; font-weight: 600; color: #1e293b; margin-bottom: 8px; display: flex; align-items: center; gap: 12px; }
.result-score { font-size: 12px; font-weight: 500; color: #667eea; background: #eef2ff; padding: 2px 8px; border-radius: 20px; }
.result-content { font-size: 14px; color: #64748b; line-height: 1.6; margin-bottom: 12px; }
.result-meta { display: flex; align-items: center; gap: 12px; }
.result-link { font-size: 13px; color: #667eea; text-decoration: none; }
.result-link:hover { text-decoration: underline; }

.blog-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.blog-card { position: relative; background: white; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; transition: all 0.3s; }
.blog-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08); }
.blog-card-accent { height: 4px; }
.blog-card-content { padding: 16px; }
.blog-card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.blog-category { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #64748b; background: #f1f5f9; padding: 4px 8px; border-radius: 4px; }
.blog-date { font-size: 12px; color: #94a3b8; }
.blog-title { font-size: 16px; font-weight: 600; margin: 0 0 8px; line-height: 1.4; }
.blog-title a { color: #1e293b; text-decoration: none; }
.blog-title a:hover { color: #667eea; }
.blog-summary { font-size: 14px; color: #64748b; line-height: 1.5; margin: 0 0 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.blog-card-bottom { display: flex; justify-content: flex-end; }
.read-more { display: flex; align-items: center; gap: 4px; color: #667eea; font-size: 14px; text-decoration: none; }
</style>