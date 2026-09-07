<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>知识库</h2>
        <p>管理知识库，支持文档导入、AI对话和全文检索</p>
      </div>
      <el-button type="primary" size="large" @click="handleCreate" class="gradient-btn">
        <el-icon><Plus /></el-icon> 新建知识库
      </el-button>
    </div>

    <div class="kb-grid" v-loading="loading">
      <div v-for="kb in kbList" :key="kb.id" class="kb-card">
        <div class="kb-icon" :style="{ background: kbColors[kb.id % kbColors.length] }">
          <el-icon :size="28"><FolderOpened /></el-icon>
        </div>
        <div class="kb-info">
          <h3 class="kb-name">{{ kb.name }}</h3>
          <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
          <div class="kb-meta">
            <span class="kb-doc-count">{{ kb.documentCount || 0 }} 篇文档</span>
            <span class="kb-date">创建于 {{ formatDate(kb.createdAt) }}</span>
          </div>
        </div>
        <div class="kb-actions">
          <router-link :to="`/admin/knowledge-bases/${kb.id}`" class="kb-action primary">
            <el-icon><Document /></el-icon>
            进入
          </router-link>
          <el-button type="primary" link @click="handleEdit(kb)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button type="danger" link @click="handleDelete(kb)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
      <el-empty v-if="!loading && !kbList.length" description="暂无知识库，点击新建开始使用" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="top">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入描述信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleSubmit" size="large" class="gradient-btn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, Document, Edit, Delete } from '@element-plus/icons-vue'
import { getKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase } from '../api/knowledgeBase'

const router = useRouter()
const loading = ref(false)
const kbList = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const form = ref({ id: null, name: '', description: '' })
const rules = { name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }] }
const dialogTitle = computed(() => isEdit.value ? '编辑知识库' : '新建知识库')

const kbColors = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)'
]

function formatDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN') : '' }

async function loadData() {
  loading.value = true
  try { kbList.value = await getKnowledgeBases() } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function handleCreate() {
  isEdit.value = false
  form.value = { id: null, name: '', description: '' }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { id: row.id, name: row.name, description: row.description }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  try {
    if (isEdit.value) {
      await updateKnowledgeBase(form.value.id, { name: form.value.name, description: form.value.description })
      ElMessage.success('更新成功')
    } else {
      await createKnowledgeBase({ name: form.value.name, description: form.value.description })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { console.error(e) }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除知识库"${row.name}"吗？`, '提示', { type: 'warning' })
  try { await deleteKnowledgeBase(row.id); ElMessage.success('删除成功'); loadData() } catch (e) { console.error(e) }
}

onMounted(loadData)
</script>

<style scoped>
.page { max-width: 1400px; }
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}
.page-header h2 { font-size: 22px; font-weight: 700; color: #1e293b; margin-bottom: 4px; }
.page-header p { font-size: 14px; color: #94a3b8; }

.gradient-btn {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;
  border-radius: 10px;
  font-weight: 600;
}
.gradient-btn:hover { opacity: 0.9; }

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.kb-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.3s;
}

.kb-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.kb-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.kb-info {
  flex: 1;
}

.kb-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.kb-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 12px;
  line-height: 1.5;
}

.kb-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kb-doc-count {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
}

.kb-date {
  font-size: 12px;
  color: #94a3b8;
}

.kb-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.kb-action {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;
}

.kb-action.primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.kb-action.primary:hover {
  opacity: 0.9;
}
</style>