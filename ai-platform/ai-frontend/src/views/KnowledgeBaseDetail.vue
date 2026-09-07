<template>
  <div class="page">
    <el-page-header @back="$router.push('/admin/knowledge-bases')" class="back-header">
      <template #content>
        <span class="back-title">{{ kb?.name || '知识库' }}</span>
      </template>
    </el-page-header>

    <div class="detail-grid" v-loading="loading">
      <!-- 基本信息 -->
      <div class="card">
        <div class="card-header"><h3>基本信息</h3></div>
        <div class="card-body">
          <el-descriptions :column="1" v-if="kb" border>
            <el-descriptions-item label="ID">{{ kb.id }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ kb.name }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ kb.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="文档数">{{ kb.documentCount }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(kb.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDate(kb.updatedAt) }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <!-- 文档管理 -->
      <div class="card">
        <div class="card-header">
          <h3>文档管理</h3>
          <el-button type="primary" size="small" @click="showUploadDialog = true" class="gradient-btn">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
        </div>
        <div class="card-body">
          <el-table :data="kbDocuments" stripe size="small" v-loading="docsLoading" empty-text="暂无文档">
            <el-table-column prop="id" label="文档 ID" width="100" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="fileType" label="类型" width="100" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="danger" link @click="handleRemoveDoc(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="500px">
      <el-upload
        drag
        :http-request="customUpload"
        :before-upload="beforeUpload"
        :show-file-list="false"
        name="file"
      >
        <el-icon :size="48"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、Markdown 格式，单个文件不超过 10MB
          </div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import { getKnowledgeBase, addDocumentsToKnowledgeBase, removeDocumentFromKnowledgeBase } from '../api/knowledgeBase'

const route = useRoute()
const kbId = route.params.id
const loading = ref(false)
const kb = ref(null)
const kbDocuments = ref([])
const docsLoading = ref(false)
const showUploadDialog = ref(false)

const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${localStorage.getItem('token')}`
}))

function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '' }

function beforeUpload(file) {
  const isValid = file.size / 1024 / 1024 < 10
  if (!isValid) {
    ElMessage.error('文件大小不能超过 10MB')
  }
  return isValid
}

function handleUploadError(err) {
  console.error('上传失败', err)
  ElMessage.error('上传失败：' + (err.message || '未知错误'))
}

async function customUpload(options) {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', file.name.replace(/\.[^/.]+$/, ''))
  formData.append('knowledgeBaseId', kbId)

  try {
    const response = await fetch('/api/documents/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: formData
    })

    if (response.ok) {
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      loadData()
    } else {
      const error = await response.text()
      throw new Error(error || '上传失败')
    }
  } catch (err) {
    console.error('上传失败', err)
    ElMessage.error('上传失败：' + (err.message || '未知错误'))
  }
}

async function loadData() {
  loading.value = true
  docsLoading.value = true
  try {
    const [kbRes, docsRes] = await Promise.all([
      getKnowledgeBase(kbId),
      fetch(`/api/documents?knowledgeBaseId=${kbId}`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      }).then(res => res.json())
    ])
    kb.value = kbRes
    kbDocuments.value = docsRes || []
  } catch (e) {
    console.error('加载数据失败', e)
  } finally {
    loading.value = false
    docsLoading.value = false
  }
}

async function handleRemoveDoc(row) {
  try {
    await removeDocumentFromKnowledgeBase(kbId, row.id)
    ElMessage.success('移除成功')
    loadData()
  } catch (e) { console.error(e) }
}

onMounted(loadData)
</script>

<style scoped>
.page { max-width: 1400px; }
.back-header { margin-bottom: 24px; }
.back-title { font-size: 18px; font-weight: 600; color: #1e293b; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.card { background: #fff; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; }
.card-header { padding: 20px 24px; border-bottom: 1px solid #f1f5f9; }
.card-header h3 { font-size: 16px; font-weight: 600; color: #1e293b; }
.card-body { padding: 24px; }
.gradient-btn { background: linear-gradient(135deg, #667eea, #764ba2) !important; border: none !important; border-radius: 8px; font-weight: 600; }
@media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; } }
</style>