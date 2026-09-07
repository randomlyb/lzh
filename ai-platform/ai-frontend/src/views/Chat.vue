<template>
  <div class="chat-page">
    <div class="chat-card">
      <div class="chat-sidebar">
        <div class="chat-sidebar-header">
          <h3>AI 对话</h3>
          <el-button type="primary" size="small" @click="newChat" class="new-chat-btn">
            <el-icon><Plus /></el-icon> 新对话
          </el-button>
        </div>
        <div class="chat-sidebar-body">
          <div class="kb-selector">
            <label>选择知识库</label>
            <el-select v-model="selectedKb" placeholder="全部知识库" clearable>
              <el-option v-for="kb in kbList" :key="kb.id" :label="kb.name" :value="kb.id" />
            </el-select>
          </div>
          <el-divider />
          <div class="conversation-list">
            <label>历史记录</label>
            <div class="conversation-items">
              <div 
                v-for="conv in conversationList" 
                :key="conv" 
                class="conversation-item"
                :class="{ active: conv === conversationId }"
                @click="switchConversation(conv)"
              >
                <el-icon><ChatLineSquare /></el-icon>
                <span class="conv-time">{{ formatTime(conv) }}</span>
                <el-icon class="delete-icon" @click.stop="deleteConversation(conv)"><Delete /></el-icon>
              </div>
              <div v-if="conversationList.length === 0" class="empty-tip">暂无历史记录</div>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-main">
        <div class="chat-messages" ref="msgRef">
          <div v-if="messages.length === 0" class="chat-welcome">
            <div class="welcome-icon">
              <el-icon :size="48"><ChatDotRound /></el-icon>
            </div>
            <h3>AI 智能助手</h3>
            <p>基于 RAG 技术，结合知识库内容为您提供精准回答</p>
            <div class="suggestions">
              <span v-for="q in suggestions" :key="q" class="suggestion" @click="inputText = q; handleSend()">{{ q }}</span>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" class="msg" :class="msg.role">
            <div class="msg-avatar">
              <el-avatar v-if="msg.role === 'user'" :size="36" :icon="UserFilled" />
              <el-avatar v-else :size="36" style="background: linear-gradient(135deg, #667eea, #764ba2)">
                <el-icon :size="20"><ChatDotRound /></el-icon>
              </el-avatar>
            </div>
            <div class="msg-body">
              <div class="msg-text" v-html="renderMd(msg.content)"></div>
              <div v-if="msg.sourceIds && msg.sourceIds.length" class="msg-sources">
                <el-tag v-for="s in msg.sourceIds" :key="s" size="small" type="info">来源: {{ s }}</el-tag>
              </div>
            </div>
          </div>

          <div v-if="loading" class="msg assistant">
            <div class="msg-avatar">
              <el-avatar :size="36" style="background: linear-gradient(135deg, #667eea, #764ba2)">
                <el-icon :size="20"><ChatDotRound /></el-icon>
              </el-avatar>
            </div>
            <div class="msg-body">
              <div class="typing-dots"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入您的问题，按 Ctrl+Enter 发送..."
            @keydown.enter.ctrl="handleSend"
            resize="none"
            :disabled="loading"
          />
          <el-button type="primary" :loading="loading" @click="handleSend" :disabled="!inputText.trim()" class="send-btn">
            <el-icon :size="18"><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { getKnowledgeBases } from '../api/knowledgeBase'
import { chat } from '../api/rag'
import { UserFilled, Delete, Promotion, Plus, ChatLineSquare } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { ElMessage, ElMessageBox } from 'element-plus'

const md = new MarkdownIt({ breaks: true, linkify: true })

const API_BASE = '/api/rag'

const suggestions = [
  '这个平台有哪些功能？',
  '如何创建知识库？',
  '请介绍一下 RAG 技术'
]

const kbList = ref([])
const selectedKb = ref(null)
const inputText = ref('')
const messages = ref([])
const loading = ref(false)
const msgRef = ref(null)
const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
const userId = ref(userInfo?.userId || userInfo?.id || '1')
const conversationId = ref(localStorage.getItem('conversationId') || null)
const conversationList = ref([])

// 格式化时间戳为可读时间
function formatTime(timestamp) {
  if (!timestamp) return '未知时间'
  
  const ts = parseInt(timestamp)
  if (isNaN(ts) || ts <= 0) return '未知时间'
  
  const date = new Date(ts)
  if (isNaN(date.getTime())) return '未知时间'
  
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// 加载对话列表
async function loadConversationList() {
  try {
    const response = await fetch(`${API_BASE}/conversations?userId=${userId.value}`)
    if (response.ok) {
      conversationList.value = await response.json()
    }
  } catch (e) {
    console.error('加载对话列表失败', e)
  }
}

// 切换对话
async function switchConversation(convId) {
  conversationId.value = convId
  localStorage.setItem('conversationId', convId)
  await loadChatHistory()
}

// 删除对话
async function deleteConversation(convId) {
  try {
    await ElMessageBox.confirm('确定要删除这条对话记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await fetch(`${API_BASE}/history?userId=${userId.value}&conversationId=${convId}`, {
      method: 'DELETE'
    })
    
    // 如果删除的是当前对话，清空消息
    if (convId === conversationId.value) {
      messages.value = []
      conversationId.value = null
      localStorage.removeItem('conversationId')
    }
    
    // 重新加载对话列表
    await loadConversationList()
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除对话失败', e)
      ElMessage.error('删除失败')
    }
  }
}

// 从 Redis 加载对话历史
async function loadChatHistory() {
  if (!conversationId.value) return
  try {
    const response = await fetch(`${API_BASE}/history?userId=${userId.value}&conversationId=${conversationId.value}`)
    if (response.ok) {
      const history = await response.json()
      messages.value = history.map(msg => ({
        role: msg.role,
        content: msg.content,
        sourceIds: msg.sourceIds || []
      }))
    }
  } catch (e) {
    console.error('加载对话历史失败', e)
  }
}

// 保存 conversationId 到 localStorage
watch(conversationId, (newId) => {
  if (newId) {
    localStorage.setItem('conversationId', newId)
  } else {
    localStorage.removeItem('conversationId')
  }
})

// 保存对话历史到 Redis（每次发送消息后自动保存）
async function saveChatHistory() {
  // 后端在 chat 接口中已经自动保存，这里不需要额外处理
}

function renderMd(text) { return md.render(text || '') }

function scrollToBottom() {
  nextTick(() => {
    if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
  })
}

async function handleSend() {
  if (!inputText.value.trim() || loading.value) return
  const question = inputText.value.trim()
  
  // 如果是新对话，生成新的 conversationId
  if (!conversationId.value) {
    conversationId.value = String(Date.now())
  }
  
  messages.value.push({ role: 'user', content: question })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await chat({
      question,
      conversationId: conversationId.value,
      userId: userId.value
    })
    const answer = res?.answer || res?.content || '暂无回复'
    messages.value.push({
      role: 'assistant',
      content: answer,
      sourceIds: res?.sourceIds || []
    })
    // 后端已自动保存对话历史到 Redis
    // 重新加载对话列表，确保新对话出现在列表中
    await loadConversationList()
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，请求失败：' + (e.message || '未知错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function clearChat() {
  messages.value = []
  if (conversationId.value) {
    try {
      await fetch(`${API_BASE}/history?userId=${userId.value}&conversationId=${conversationId.value}`, {
        method: 'DELETE'
      })
    } catch (e) {
      console.error('清空对话历史失败', e)
    }
  }
  conversationId.value = null
}

function newChat() {
  messages.value = []
  conversationId.value = null
  localStorage.removeItem('conversationId')
  // 重新加载对话列表
  loadConversationList()
}

onMounted(async () => {
  try { kbList.value = await getKnowledgeBases() } catch (e) { console.error(e) }
  // 加载对话列表
  await loadConversationList()
  // 加载对话历史
  await loadChatHistory()
})

// 监听 conversationId 变化，自动加载历史
watch(conversationId, async (newId) => {
  if (newId) {
    await loadChatHistory()
  }
})
</script>

<style scoped>
.chat-page { height: calc(100vh - 140px); }
.chat-card {
  display: flex; height: 100%; border-radius: 16px; overflow: hidden;
  border: 1px solid #e2e8f0; background: #fff;
}

.chat-sidebar {
  width: 240px; border-right: 1px solid #e2e8f0; background: #f8fafc;
  display: flex; flex-direction: column; flex-shrink: 0;
}
.chat-sidebar-header {
  padding: 20px; border-bottom: 1px solid #e2e8f0;
}
.chat-sidebar-header h3 { font-size: 16px; font-weight: 600; color: #1e293b; }
.chat-sidebar-body { padding: 20px; flex: 1; overflow-y: auto; }
.kb-selector label { display: block; font-size: 13px; color: #64748b; margin-bottom: 8px; font-weight: 500; }
.conversation-list label { display: block; font-size: 13px; color: #64748b; margin-bottom: 8px; font-weight: 500; }
.conversation-items { display: flex; flex-direction: column; gap: 4px; max-height: 300px; overflow-y: auto; }
.conversation-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 8px;
  cursor: pointer; transition: all 0.2s; font-size: 13px; color: #64748b;
}
.conversation-item:hover { background: #e2e8f0; }
.conversation-item.active { background: #e0e7ff; color: #667eea; font-weight: 500; }
.conv-time { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.delete-icon { opacity: 0; transition: opacity 0.2s; color: #ef4444; }
.conversation-item:hover .delete-icon { opacity: 1; }
.empty-tip { text-align: center; color: #94a3b8; font-size: 12px; padding: 16px 0; }

.chat-main { flex: 1; display: flex; flex-direction: column; }
.chat-messages { flex: 1; overflow-y: auto; padding: 24px; background: #fafbfc; }

.chat-welcome { text-align: center; padding: 60px 20px; }
.welcome-icon { width: 80px; height: 80px; border-radius: 20px; background: linear-gradient(135deg, #eef2ff, #e0e7ff); display: flex; align-items: center; justify-content: center; color: #667eea; margin: 0 auto 20px; }
.chat-welcome h3 { font-size: 22px; font-weight: 700; color: #1e293b; margin-bottom: 8px; }
.chat-welcome p { color: #94a3b8; font-size: 14px; margin-bottom: 24px; }

.suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.suggestion {
  padding: 8px 16px; background: #fff; border: 1px solid #e2e8f0; border-radius: 20px;
  font-size: 13px; color: #64748b; cursor: pointer; transition: all 0.2s;
}
.suggestion:hover { border-color: #667eea; color: #667eea; }

.msg { display: flex; gap: 12px; margin-bottom: 24px; }
.msg.user { flex-direction: row-reverse; }
.msg-avatar { flex-shrink: 0; }
.msg-body { max-width: 75%; }
.msg-text {
  padding: 14px 18px; border-radius: 14px; line-height: 1.7; font-size: 14px;
  word-break: break-word;
}
.msg.user .msg-text { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border-bottom-right-radius: 4px; }
.msg.assistant .msg-text { background: #fff; border: 1px solid #e2e8f0; border-bottom-left-radius: 4px; color: #1e293b; }
.msg-text :deep(p) { margin: 0 0 8px; }
.msg-text :deep(p:last-child) { margin: 0; }
.msg-text :deep(code) { background: rgba(0,0,0,0.06); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.msg-text :deep(pre) { background: #1e293b; color: #e2e8f0; padding: 16px; border-radius: 8px; overflow-x: auto; margin: 8px 0; }
.msg-text :deep(pre code) { background: none; padding: 0; }
.msg-sources { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }

.typing-dots { display: flex; gap: 4px; padding: 14px 18px; background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; border-bottom-left-radius: 4px; }
.typing-dots span { width: 8px; height: 8px; background: #94a3b8; border-radius: 50%; animation: bounce 1.4s infinite; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%,60%,100% { transform: translateY(0); } 30% { transform: translateY(-8px); } }

.chat-input-area {
  padding: 16px 20px; border-top: 1px solid #e2e8f0; background: #fff;
  display: flex; gap: 12px; align-items: flex-end;
}
.send-btn { width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2) !important; border: none !important; }
.send-btn:hover { opacity: 0.9; }
</style>