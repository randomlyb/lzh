<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>任务监控</h2>
        <p>实时监控任务执行情况，AI 智能分析任务运行状态</p>
      </div>
      <el-button type="primary" size="large" @click="handleCreate" class="gradient-btn">
        <el-icon><Plus /></el-icon> 创建任务
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card stat-total">
        <div class="stat-icon"><el-icon><List /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ jobStats.total || 0 }}</span>
          <span class="stat-label">总执行次数</span>
        </div>
      </div>
      <div class="stat-card stat-success">
        <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ jobStats.success || 0 }}</span>
          <span class="stat-label">成功次数</span>
        </div>
      </div>
      <div class="stat-card stat-failed">
        <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ jobStats.failed || 0 }}</span>
          <span class="stat-label">失败次数</span>
        </div>
      </div>
      <div class="stat-card stat-rate">
        <div class="stat-icon"><el-icon><TrendCharts /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ jobStats.successRate || 0 }}%</span>
          <span class="stat-label">成功率</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <div class="chart-card">
        <div class="chart-header">
          <h3>任务执行趋势</h3>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="14">近14天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3>成功失败占比</h3>
        </div>
        <div ref="pieChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- 任务耗时排行 -->
    <div class="chart-card duration-card">
      <div class="chart-header">
        <h3>任务平均耗时排行</h3>
      </div>
      <div ref="durationChartRef" class="chart-container" style="height: 300px;"></div>
    </div>

    <!-- 任务列表 -->
    <div class="section-header">
      <h3>任务列表</h3>
    </div>
    <div class="job-grid" v-loading="loading">
      <div v-for="job in jobList" :key="job.id" class="job-card" :class="'status-' + job.status.toLowerCase()">
        <div class="job-card-header">
          <div class="job-type-icon" :style="{ background: getJobGradient(job.executorHandler) }">
            <el-icon :size="24"><component :is="getJobIcon(job.executorHandler)" /></el-icon>
          </div>
          <div class="job-header-info">
            <h3 class="job-name">{{ job.jobName }}</h3>
            <span class="job-executor">{{ getExecutorLabel(job.executorHandler) }}</span>
          </div>
          <el-tag :type="getStatusType(job.status)" size="small" effect="dark" class="job-status-tag">
            {{ getStatusText(job.status) }}
          </el-tag>
        </div>
        
        <div class="job-card-body">
          <div class="job-detail">
            <span class="detail-label">触发方式</span>
            <span class="detail-value">
              <el-tag v-if="job.triggerType === 'CRON'" type="primary" size="small" effect="light">Cron</el-tag>
              <el-tag v-else type="success" size="small" effect="light">固定频率</el-tag>
            </span>
          </div>
          <div class="job-detail">
            <span class="detail-label">触发规则</span>
            <code class="detail-value cron-code">{{ job.triggerType === 'CRON' ? job.cronExpression : `每 ${job.fixedRate}ms` }}</code>
          </div>
        </div>

        <div class="job-card-footer">
          <el-button type="primary" link @click="handleViewLogs(job)" class="action-btn">
            <el-icon><Document /></el-icon> 日志
          </el-button>
          <el-button v-if="job.status === 'RUNNING'" type="warning" link @click="handlePause(job)" class="action-btn">
            <el-icon><VideoPause /></el-icon> 暂停
          </el-button>
          <el-button v-if="job.status === 'PAUSED'" type="success" link @click="handleResume(job)" class="action-btn">
            <el-icon><VideoPlay /></el-icon> 恢复
          </el-button>
          <el-button type="primary" link @click="handleEdit(job)" class="action-btn">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button type="danger" link @click="handleDelete(job)" class="action-btn">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
      
      <el-empty v-if="!loading && !jobList.length" description="暂无任务" />
    </div>

    <!-- AI 分析浮动按钮 -->
    <div class="ai-analysis-fab" @click="showAiAnalysis = true">
      <el-icon :size="28"><ChatDotRound /></el-icon>
      <span>AI 分析</span>
    </div>

    <!-- AI 分析对话框 -->
    <el-dialog v-model="showAiAnalysis" title="AI 任务分析" width="600px" class="ai-dialog">
      <div class="ai-chat-area">
        <div class="ai-messages" ref="aiMsgRef">
          <div v-for="(msg, idx) in aiMessages" :key="idx" class="ai-message" :class="msg.role">
            <div class="ai-avatar">
              <el-icon v-if="msg.role === 'assistant'" :size="20"><ChatDotRound /></el-icon>
              <el-icon v-else :size="20"><User /></el-icon>
            </div>
            <div class="ai-bubble">
              <div v-html="renderMd(msg.content)"></div>
            </div>
          </div>
          <div v-if="aiLoading" class="ai-message assistant">
            <div class="ai-avatar"><el-icon :size="20"><ChatDotRound /></el-icon></div>
            <div class="ai-bubble"><span class="typing">分析中...</span></div>
          </div>
        </div>
        <div class="ai-input-area">
          <el-input
            v-model="aiInput"
            placeholder="例如：分析最近一周任务执行情况"
            @keyup.enter="sendAiMessage"
            :disabled="aiLoading"
            size="large"
          >
            <template #append>
              <el-button @click="sendAiMessage" :loading="aiLoading" type="primary">
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
          <div class="ai-suggestions">
            <el-tag size="small" @click="aiInput = '分析最近一周任务执行情况'; sendAiMessage()" class="suggestion-tag">分析最近一周任务</el-tag>
            <el-tag size="small" @click="aiInput = '为什么有任务失败？'; sendAiMessage()" class="suggestion-tag">失败原因分析</el-tag>
            <el-tag size="small" @click="aiInput = '哪个任务执行时间最长？'; sendAiMessage()" class="suggestion-tag">耗时最长任务</el-tag>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 创建/编辑任务对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :close-on-click-modal="false" class="job-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="例如：博客采集任务" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行器" prop="executorHandler">
              <el-select v-model="form.executorHandler" placeholder="选择执行器" style="width: 100%">
                <el-option v-for="ex in executors" :key="ex.value" :label="ex.label" :value="ex.value">
                  <div class="executor-option">
                    <span class="executor-label">{{ ex.label }}</span>
                    <span class="executor-desc">{{ ex.desc }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="触发类型" prop="triggerType">
          <el-radio-group v-model="form.triggerType">
            <el-radio value="CRON">Cron 表达式</el-radio>
            <el-radio value="FIXED_RATE">固定频率</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.triggerType === 'CRON'" label="Cron 表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="0 0/5 * * * ?">
            <template #append>
              <el-tooltip content="秒 分 时 日 月 周" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="form.triggerType === 'FIXED_RATE'" label="间隔(毫秒)" prop="fixedRate">
          <el-input-number v-model="form.fixedRate" :min="1000" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="执行参数">
              <el-input v-model="form.executorParams" placeholder="JSON 参数" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路由策略" prop="routeStrategy">
              <el-select v-model="form.routeStrategy" placeholder="选择策略" style="width: 100%">
                <el-option label="轮询" value="ROUND_ROBIN" />
                <el-option label="一致性哈希" value="CONSISTENT_HASH" />
                <el-option label="广播" value="BROADCAST" />
                <el-option label="随机" value="RANDOM" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大重试次数">
              <el-input-number v-model="form.maxRetryCount" :min="0" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重试间隔(ms)">
              <el-input-number v-model="form.retryDelay" :min="0" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="最大并发数">
          <el-input-number v-model="form.maxConcurrent" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleSubmit" size="large" class="gradient-btn" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  VideoPlay, VideoPause, CircleClose, CircleCheck, List, Document, Edit, Delete, 
  Plus, QuestionFilled, Collection, MagicStick, Link, Timer, TrendCharts,
  ChatDotRound, User, Promotion
} from '@element-plus/icons-vue'
import { getJobs, createJob, updateJob, deleteJob, pauseJob, resumeJob, getExecutors } from '../api/job'
import * as echarts from 'echarts'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ breaks: true, linkify: true })
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const jobList = ref([])
const executors = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const jobStats = ref({})
const trendDays = ref(7)
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const durationChartRef = ref(null)
let trendChart = null
let pieChart = null
let durationChart = null

// 从 localStorage 获取 userId
const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
const userId = ref(userInfo?.userId || userInfo?.id || 1)

// AI 分析
const showAiAnalysis = ref(false)
const aiConversationId = ref(localStorage.getItem('job_ai_conversationId') || String(Date.now()))
const aiMessages = ref([])
const aiInput = ref('')
const aiLoading = ref(false)
const aiMsgRef = ref(null)

// 加载历史对话
async function loadAiHistory() {
  try {
    const res = await fetch(`/api/agent/job/history?userId=${userId.value}&conversationId=${aiConversationId.value}`)
    if (res.ok) {
      const history = await res.json()
      if (history && history.length > 0) {
        aiMessages.value = history.map(msg => ({
          role: msg.role,
          content: msg.content
        }))
        return
      }
    }
  } catch (e) {
    console.error('加载AI历史失败:', e)
  }
  // 如果没有历史，显示欢迎消息
  aiMessages.value = [
    { role: 'assistant', content: '你好！我是任务分析助手，可以帮你分析任务执行情况。试试问我：\n- 分析最近一周任务执行情况\n- 为什么有任务失败？\n- 哪个任务执行时间最长？' }
  ]
}

const form = ref({
  id: null,
  jobName: '',
  triggerType: 'CRON',
  cronExpression: '0 0/5 * * * ?',
  fixedRate: 60000,
  executorHandler: '',
  executorParams: '',
  routeStrategy: 'ROUND_ROBIN',
  maxRetryCount: 3,
  retryDelay: 5000,
  maxConcurrent: 1
})
const rules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  executorHandler: [{ required: true, message: '请选择执行器', trigger: 'change' }],
  triggerType: [{ required: true, message: '请选择触发类型', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }],
  routeStrategy: [{ required: true, message: '请选择路由策略', trigger: 'change' }]
}
const dialogTitle = computed(() => isEdit.value ? '编辑任务' : '创建任务')

function renderMd(text) { return md.render(text || '') }

function formatDate(d) { 
  if (!d) return ''
  const date = new Date(d)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function getExecutorLabel(value) {
  const ex = executors.value.find(e => e.value === value)
  return ex ? ex.label : value
}

function getStatusType(status) {
  const map = { RUNNING: 'success', PAUSED: 'warning', STOPPED: 'info' }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = { RUNNING: '运行中', PAUSED: '已暂停', STOPPED: '已停止' }
  return map[status] || status
}

function getJobIcon(handler) {
  if (handler?.includes('blog') || handler?.includes('Blog')) return Collection
  if (handler?.includes('summary') || handler?.includes('Summary') || handler?.includes('摘要')) return MagicStick
  if (handler?.includes('github') || handler?.includes('GitHub')) return Link
  return Timer
}

function getJobGradient(handler) {
  if (handler?.includes('blog') || handler?.includes('Blog')) return 'linear-gradient(135deg, #667eea, #764ba2)'
  if (handler?.includes('summary') || handler?.includes('Summary') || handler?.includes('摘要')) return 'linear-gradient(135deg, #f093fb, #f5576c)'
  if (handler?.includes('github') || handler?.includes('GitHub')) return 'linear-gradient(135deg, #4facfe, #00f2fe)'
  return 'linear-gradient(135deg, #43e97b, #38f9d7)'
}

async function loadData() {
  loading.value = true
  try { jobList.value = await getJobs() } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadExecutors() {
  try {
    const allExecutors = await getExecutors()
    const internalExecutors = ['pdfParseJobExecutor', 'chunkJobExecutor']
    executors.value = allExecutors.filter(ex => !internalExecutors.includes(ex.value))
  } catch (e) { console.error(e) }
}

async function loadStats() {
  try {
    const res = await fetch('/api/jobs/statistics')
    jobStats.value = await res.json()
  } catch (e) { console.error(e) }
}

async function loadTrend() {
  try {
    const res = await fetch(`/api/jobs/trend?days=${trendDays.value}`)
    const data = await res.json()
    renderTrendChart(data)
  } catch (e) { console.error(e) }
}

async function loadPieData() {
  try {
    const res = await fetch('/api/jobs/statistics')
    const data = await res.json()
    renderPieChart(data)
  } catch (e) { console.error(e) }
}

async function loadDuration() {
  try {
    const res = await fetch('/api/jobs/duration')
    const data = await res.json()
    renderDurationChart(data)
  } catch (e) { console.error(e) }
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
      { name: '成功', type: 'line', data: success, smooth: true, itemStyle: { color: '#43e97b' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(67,233,123,0.3)' }, { offset: 1, color: 'rgba(67,233,123,0.05)' }] } } },
      { name: '失败', type: 'line', data: failed, smooth: true, itemStyle: { color: '#f5576c' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(245,87,108,0.3)' }, { offset: 1, color: 'rgba(245,87,108,0.05)' }] } } }
    ]
  })
}

function renderPieChart(data) {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c} ({d}%)' },
      data: [
        { value: data.success || 0, name: '成功', itemStyle: { color: '#43e97b' } },
        { value: data.failed || 0, name: '失败', itemStyle: { color: '#f5576c' } }
      ]
    }]
  })
}

function renderDurationChart(data) {
  if (!durationChartRef.value) return
  if (!durationChart) durationChart = echarts.init(durationChartRef.value)

  const names = data.map(d => d.jobName).reverse()
  const durations = data.map(d => d.avgDuration).reverse()

  durationChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c}ms' },
    grid: { left: 120, right: 40, top: 10, bottom: 30 },
    xAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    yAxis: { type: 'category', data: names, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    series: [{
      type: 'bar',
      data: durations,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      },
      label: { show: true, position: 'right', formatter: '{c}ms' }
    }]
  })
}

async function sendAiMessage() {
  if (!aiInput.value.trim() || aiLoading.value) return
  const question = aiInput.value.trim()
  aiMessages.value.push({ role: 'user', content: question })
  aiInput.value = ''
  aiLoading.value = true
  scrollToAiBottom()

  // 保存 conversationId 到 localStorage
  localStorage.setItem('job_ai_conversationId', aiConversationId.value)

  try {
    // 调用任务分析 Agent，通过 MCP 工具获取数据并分析
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

function handleCreate() {
  isEdit.value = false
  form.value = {
    id: null, jobName: '', triggerType: 'CRON', cronExpression: '0 0/5 * * * ?',
    fixedRate: 60000, executorHandler: '', executorParams: '', routeStrategy: 'ROUND_ROBIN',
    maxRetryCount: 3, retryDelay: 5000, maxConcurrent: 1
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateJob(form.value)
      ElMessage.success('更新成功')
    } else {
      await createJob(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

async function handlePause(row) {
  try { await pauseJob(row.id); ElMessage.success('已暂停'); loadData() } catch (e) { console.error(e) }
}

async function handleResume(row) {
  try { await resumeJob(row.id); ElMessage.success('已恢复'); loadData() } catch (e) { console.error(e) }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除任务"${row.jobName}"吗？`, '提示', { type: 'warning' })
  try { await deleteJob(row.id); ElMessage.success('删除成功'); loadData() } catch (e) { console.error(e) }
}

function handleViewLogs(row) {
  router.push(`/admin/jobs/${row.id}/logs`)
}

onMounted(() => {
  loadData()
  loadExecutors()
  loadStats()
  loadTrend()
  loadPieData()
  loadDuration()
  loadAiHistory()

  window.addEventListener('resize', () => {
    trendChart?.resize()
    pieChart?.resize()
    durationChart?.resize()
  })
})
</script>

<style scoped>
.page { max-width: 1400px; }

.page-header {
  display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px;
}
.page-header h2 { font-size: 22px; font-weight: 700; color: #1e293b; margin-bottom: 4px; }
.page-header p { font-size: 14px; color: #94a3b8; }

.gradient-btn { 
  background: linear-gradient(135deg, #667eea, #764ba2) !important; 
  border: none !important; 
  border-radius: 10px; 
  font-weight: 600; 
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
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
  color: white;
  font-size: 24px;
}

.stat-total .stat-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-success .stat-icon { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-failed .stat-icon { background: linear-gradient(135deg, #f5576c, #fa709a); }
.stat-rate .stat-icon { background: linear-gradient(135deg, #4facfe, #00f2fe); }

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

/* 图表区域 */
.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 20px;
}

.duration-card {
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.chart-container {
  width: 100%;
  height: 320px;
}

/* 任务卡片 */
.section-header {
  margin-bottom: 16px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.job-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
  margin-bottom: 80px;
}

.job-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  transition: all 0.3s;
}

.job-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.job-card.status-running { border-left: 4px solid #43e97b; }
.job-card.status-paused { border-left: 4px solid #fa709a; }
.job-card.status-stopped { border-left: 4px solid #cbd5e1; }

.job-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.job-type-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.job-header-info {
  flex: 1;
  min-width: 0;
}

.job-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.job-executor {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
}

.job-status-tag {
  flex-shrink: 0;
}

.job-card-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.job-detail {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-label {
  font-size: 13px;
  color: #94a3b8;
}

.detail-value {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

.cron-code {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Monaco', monospace;
  font-size: 12px;
  color: #667eea;
}

.job-card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.action-btn {
  font-size: 13px;
  padding: 4px 8px;
}

.action-btn .el-icon {
  margin-right: 4px;
}

.executor-option { display: flex; flex-direction: column; }
.executor-label { font-weight: 600; color: #1e293b; }
.executor-desc { font-size: 12px; color: #94a3b8; }

/* AI 分析浮动按钮 */
.ai-analysis-fab {
  position: fixed;
  bottom: 32px;
  right: 32px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 14px 24px;
  border-radius: 50px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
  font-weight: 600;
  z-index: 100;
}

.ai-analysis-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.5);
}

/* AI 对话框 */
.ai-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.ai-chat-area {
  display: flex;
  flex-direction: column;
  height: 500px;
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
  max-width: 85%;
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
}

.ai-message.assistant .ai-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.ai-message.user .ai-avatar {
  background: #f1f5f9;
  color: #64748b;
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

.ai-input-area {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background: #fafbfc;
}

.ai-suggestions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.suggestion-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-tag:hover {
  background: #667eea !important;
  color: white !important;
  border-color: #667eea !important;
}

.typing {
  color: #94a3b8;
  font-style: italic;
}

.job-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.job-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.job-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
  .job-grid { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); }
}

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .job-grid { grid-template-columns: 1fr; }
}
</style>