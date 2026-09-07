<template>
  <div class="page">
    <el-page-header @back="$router.back()" class="back-header">
      <template #content>
        <span class="back-title">任务监控 - {{ job?.jobName || '任务' }} #{{ jobId }}</span>
      </template>
    </el-page-header>

    <div class="monitor-grid" v-if="job">
      <div class="card">
        <div class="card-header"><h3>任务信息</h3></div>
        <div class="card-body">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="任务 ID">{{ job.id }}</el-descriptions-item>
            <el-descriptions-item label="任务名称">{{ job.jobName }}</el-descriptions-item>
            <el-descriptions-item label="执行器">{{ getExecutorLabel(job.executorHandler) }}</el-descriptions-item>
            <el-descriptions-item label="触发类型">
              <el-tag :type="job.triggerType === 'CRON' ? 'primary' : 'success'" size="small">
                {{ job.triggerType === 'CRON' ? 'Cron' : '固定频率' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="触发规则">
              <code v-if="job.triggerType === 'CRON'">{{ job.cronExpression }}</code>
              <span v-else>每 {{ job.fixedRate }}ms</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="job.status === 'RUNNING' ? 'success' : 'warning'">
                {{ job.status === 'RUNNING' ? '运行中' : job.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="路由策略">{{ job.routeStrategy }}</el-descriptions-item>
            <el-descriptions-item label="最大重试">{{ job.maxRetryCount }} 次</el-descriptions-item>
            <el-descriptions-item label="最大并发">{{ job.maxConcurrent }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ formatDate(job.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <!-- 最近7天任务执行趋势 -->
      <div class="card trend-card">
        <div class="card-header">
          <h3>最近7天任务执行趋势</h3>
        </div>
        <div class="card-body">
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card stat-total">
          <div class="stat-icon">📊</div>
          <div class="stat-info">
            <span class="stat-value">{{ trendStats.total }}</span>
            <span class="stat-label">总执行</span>
          </div>
        </div>
        <div class="stat-card stat-success-rate">
          <div class="stat-icon">✅</div>
          <div class="stat-info">
            <span class="stat-value">{{ trendStats.successRate }}%</span>
            <span class="stat-label">成功率</span>
          </div>
        </div>
        <div class="stat-card stat-success">
          <div class="stat-icon">✔️</div>
          <div class="stat-info">
            <span class="stat-value">{{ trendStats.success }}</span>
            <span class="stat-label">成功次数</span>
          </div>
        </div>
        <div class="stat-card stat-failed">
          <div class="stat-icon">❌</div>
          <div class="stat-info">
            <span class="stat-value">{{ trendStats.failed }}</span>
            <span class="stat-label">失败次数</span>
          </div>
        </div>
      </div>

      <!-- AI 智能分析按钮 -->
      <div class="ai-trigger-section">
        <el-button type="primary" size="large" @click="triggerAiAnalysis" :loading="aiLoading" class="ai-analyze-btn">
          <el-icon><MagicStick /></el-icon>
          ✨ AI 智能分析
        </el-button>
      </div>

      <!-- AI 任务运行分析报告 -->
      <div class="card ai-report-card" v-if="aiReportVisible">
        <div class="card-header">
          <h3>AI 任务运行分析报告</h3>
          <el-button @click="aiReportVisible = false" size="small" text>
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="card-body ai-report-body">
          <div class="ai-messages" ref="aiMsgRef">
            <div v-for="(msg, idx) in aiMessages" :key="idx" class="ai-message" :class="msg.role">
              <div class="ai-avatar">
                <span v-if="msg.role === 'assistant'">🤖</span>
                <span v-else>👤</span>
              </div>
              <div class="ai-bubble">
                <div v-html="renderMd(msg.content)"></div>
              </div>
            </div>
            <div v-if="aiLoading" class="ai-message assistant">
              <div class="ai-avatar"><span>🤖</span></div>
              <div class="ai-bubble"><span class="typing">分析中...</span></div>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>执行日志</h3>
          <el-button @click="loadLogs" :loading="logsLoading" size="small">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
        <div class="card-body">
          <el-table 
            v-if="logList.length > 0 || !logsLoading" 
            :data="logList" 
            row-key="id"
            stripe 
            v-loading="logsLoading" 
            size="small" 
            empty-text="暂无执行日志"
            :key="logList.length"
          >
            <el-table-column prop="id" label="日志 ID" width="80" />
            <el-table-column prop="executorAddress" label="执行节点" width="150" />
            <el-table-column label="触发时间" width="170">
              <template #default="{ row }">{{ row ? formatDate(row.triggerTime) : '' }}</template>
            </el-table-column>
            <el-table-column label="开始时间" width="170">
              <template #default="{ row }">{{ row ? formatDate(row.startTime) : '' }}</template>
            </el-table-column>
            <el-table-column label="结束时间" width="170">
              <template #default="{ row }">{{ row ? formatDate(row.endTime) : '' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row" :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="retryCount" label="重试次数" width="100" />
            <el-table-column label="错误信息" min-width="180">
              <template #default="{ row }">
                <span v-if="row && row.errorMessage" class="error-msg">{{ row.errorMessage }}</span>
                <span v-else class="no-error">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><h3>执行统计</h3></div>
        <div class="card-body">
          <div class="log-stats">
            <div class="log-stat">
              <span class="log-stat-value">{{ logStats.total }}</span>
              <span class="log-stat-label">总执行次数</span>
            </div>
            <div class="log-stat success">
              <span class="log-stat-value">{{ logStats.success }}</span>
              <span class="log-stat-label">成功</span>
            </div>
            <div class="log-stat danger">
              <span class="log-stat-value">{{ logStats.failed }}</span>
              <span class="log-stat-label">失败</span>
            </div>
            <div class="log-stat warning">
              <span class="log-stat-value">{{ logStats.running }}</span>
              <span class="log-stat-label">其他</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getJob, getJobLogs, getExecutors } from '../api/job'
import * as echarts from 'echarts'
import MarkdownIt from 'markdown-it'
import { MagicStick, Close } from '@element-plus/icons-vue'

const md = new MarkdownIt({ breaks: true, linkify: true })
const route = useRoute()
const jobId = route.params.id
const job = ref(null)
const logList = ref([])
const logsLoading = ref(false)
const executors = ref([])
const trendChartRef = ref(null)
let trendChart = null
const trendData = ref([])

const logStats = computed(() => {
  const total = logList.value.length
  const success = logList.value.filter(l => l.status === 'SUCCESS').length
  const failed = logList.value.filter(l => l.status === 'FAILED').length
  const running = total - success - failed
  return { total, success, failed, running }
})

const trendStats = computed(() => {
  const total = trendData.value.reduce((sum, d) => sum + (d.success || 0) + (d.failed || 0), 0)
  const success = trendData.value.reduce((sum, d) => sum + (d.success || 0), 0)
  const failed = trendData.value.reduce((sum, d) => sum + (d.failed || 0), 0)
  const successRate = total > 0 ? ((success / total) * 100).toFixed(1) : 0
  return { total, success, failed, successRate }
})

// AI 分析相关
const aiReportVisible = ref(false)
const aiMessages = ref([])
const aiLoading = ref(false)
const aiMsgRef = ref(null)
const aiConversationId = ref(localStorage.getItem('job_ai_conversationId') || String(Date.now()))

// 从 localStorage 获取 userId
const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
const userId = ref(userInfo?.userId || userInfo?.id || 1)

function renderMd(text) { return md.render(text || '') }

function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '' }

function getExecutorLabel(value) {
  const ex = executors.value.find(e => e.value === value)
  return ex ? ex.label : value
}

async function loadJob() {
  try { job.value = await getJob(jobId) } catch (e) { console.error(e) }
}

async function loadLogs() {
  if (logsLoading.value) return
  logsLoading.value = true
  try {
    const response = await getJobLogs(jobId)
    if (Array.isArray(response)) {
      logList.value = response.filter(item => item && typeof item === 'object')
    } else {
      logList.value = []
    }
  } catch (e) {
    console.error('加载日志失败:', e)
    logList.value = []
  } finally {
    logsLoading.value = false
  }
}

async function loadExecutors() {
  try { executors.value = await getExecutors() } catch (e) { console.error(e) }
}

async function loadTrend() {
  try {
    const res = await fetch(`/api/jobs/trend?days=7`)
    const data = await res.json()
    trendData.value = data
    renderTrendChart(data)
  } catch (e) {
    console.error('加载趋势数据失败:', e)
  }
}

function renderTrendChart(data) {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  
  const dates = data.map(d => d.date)
  const success = data.map(d => d.success)
  const failed = data.map(d => d.failed)

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成功', '失败'], bottom: 0 },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [
      { 
        name: '成功', 
        type: 'line', 
        data: success, 
        smooth: true, 
        itemStyle: { color: '#43e97b' }, 
        areaStyle: { 
          color: { 
            type: 'linear', 
            x: 0, y: 0, x2: 0, y2: 1, 
            colorStops: [
              { offset: 0, color: 'rgba(67,233,123,0.3)' }, 
              { offset: 1, color: 'rgba(67,233,123,0.05)' }
            ] 
          } 
        } 
      },
      { 
        name: '失败', 
        type: 'line', 
        data: failed, 
        smooth: true, 
        itemStyle: { color: '#f5576c' }, 
        areaStyle: { 
          color: { 
            type: 'linear', 
            x: 0, y: 0, x2: 0, y2: 1, 
            colorStops: [
              { offset: 0, color: 'rgba(245,87,108,0.3)' }, 
              { offset: 1, color: 'rgba(245,87,108,0.05)' }
            ] 
          } 
        } 
      }
    ]
  })
}

async function triggerAiAnalysis() {
  aiReportVisible.value = true
  aiLoading.value = true
  
  // 初始化欢迎消息
  if (aiMessages.value.length === 0) {
    aiMessages.value = [
      { role: 'assistant', content: '你好！我是任务分析助手，正在为你分析任务执行情况...' }
    ]
  }
  
  // 自动发送分析请求
  const question = '请分析这个任务的执行情况，包括整体执行数据、异常时间节点、性能瓶颈、失败原因分析和优化建议'
  
  aiMessages.value.push({ role: 'user', content: question })
  scrollToAiBottom()

  localStorage.setItem('job_ai_conversationId', aiConversationId.value)

  try {
    const res = await fetch('/api/agent/job/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        question: question,
        userId: String(userId.value),
        conversationId: aiConversationId.value
      })
    })
    const data = await res.json()
    console.log('AI分析响应:', data)
    aiMessages.value.push({ role: 'assistant', content: data.answer || '分析完成' })
  } catch (e) {
    aiMessages.value.push({ role: 'assistant', content: '分析失败：' + (e.message || '未知错误') })
  } finally {
    aiLoading.value = false
    scrollToAiBottom()
  }
}

function scrollToAiBottom() {
  nextTick(() => {
    if (aiMsgRef.value) aiMsgRef.value.scrollTop = aiMsgRef.value.scrollHeight
  })
}

onMounted(() => {
  loadJob()
  loadLogs()
  loadExecutors()
  loadTrend()

  window.addEventListener('resize', () => {
    trendChart?.resize()
  })
})
</script>

<style scoped>
.page { max-width: 1400px; }
.back-header { margin-bottom: 24px; }
.back-title { font-size: 18px; font-weight: 600; color: #1e293b; }
.monitor-grid { display: flex; flex-direction: column; gap: 20px; }
.card { background: #fff; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; }
.card-header { padding: 20px 24px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; justify-content: space-between; }
.card-header h3 { font-size: 16px; font-weight: 600; color: #1e293b; }
.card-body { padding: 24px; }

/* 趋势图 */
.trend-card .card-body { padding: 16px; }
.trend-chart { width: 100%; height: 320px; }

/* 统计卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: #f8fafc;
}

.stat-total .stat-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-success-rate .stat-icon { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-success .stat-icon { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-failed .stat-icon { background: linear-gradient(135deg, #f5576c, #fa709a); }

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

/* AI 分析按钮区域 */
.ai-trigger-section {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.ai-analyze-btn {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;
  border-radius: 12px !important;
  padding: 16px 40px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3) !important;
  transition: all 0.3s !important;
}

.ai-analyze-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.4) !important;
}

/* AI 报告卡片 */
.ai-report-card .card-body {
  padding: 0;
}

.ai-report-body {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-message {
  display: flex;
  gap: 12px;
  max-width: 90%;
}

.ai-message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
  background: #f1f5f9;
}

.ai-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.ai-message.assistant .ai-bubble {
  background: #f8fafc;
  color: #334155;
}

.ai-message.user .ai-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.typing {
  color: #94a3b8;
  font-style: italic;
}

.log-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.log-stat { text-align: center; padding: 20px; border-radius: 12px; background: #f8fafc; }
.log-stat.success { background: #f0fdf4; }
.log-stat.danger { background: #fef2f2; }
.log-stat.warning { background: #fffbeb; }
.log-stat-value { font-size: 28px; font-weight: 800; color: #1e293b; display: block; line-height: 1; }
.log-stat-label { font-size: 13px; color: #94a3b8; margin-top: 6px; display: block; }

.error-msg { color: #f56c6c; font-size: 13px; }
.no-error { color: #c0c4cc; }

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>