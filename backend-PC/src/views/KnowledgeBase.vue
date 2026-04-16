<template>
  <div class="page-kb">
    <header class="page-head panel float-in">
      <div class="head-inner">
        <p class="sub">医视智行 · 多源知识库</p>
        <h2>知识库</h2>
      </div>
    </header>

    <section class="kb-card float-in">
      <el-tabs v-model="activeTab" class="kb-tabs" @tab-change="onTabChange">
        <el-tab-pane label="推荐文献" name="articles" />
        <el-tab-pane label="就诊数据" name="qa" />
        <el-tab-pane label="癌症数据" name="liver" />
        <el-tab-pane label="案例数据" name="llama" />
        <el-tab-pane label="药品数据" name="drug" />
        <el-tab-pane label="英文文献" name="english" />
      </el-tabs>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          :placeholder="keywordPlaceholder"
          class="kw-input"
          @clear="loadPage"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" class="btn-query" round :loading="loading" @click="onSearch">查询</el-button>
        <el-button class="btn-refresh" round :loading="loading" @click="loadPage">刷新</el-button>
        <template v-if="activeTab !== 'articles'">
          <el-button type="success" class="btn-add" round @click="openCreate">新增</el-button>
          <el-button type="warning" class="btn-rebuild" round :loading="rebuildLoading" @click="onRebuildVector">
            刷新向量库
          </el-button>
        </template>
      </div>

      <div class="table-wrap">
        <!-- 爬取资讯 -->
        <el-table
          v-if="activeTab === 'articles'"
          v-loading="loading"
          :data="rows"
          stripe
          class="kb-table"
          empty-text="暂无数据（等待定时抓取或检查 Python 资讯服务）"
          border
        >
          <el-table-column type="index" label="序号" width="72" align="center" :index="indexMethod" />
          <el-table-column prop="keyword" label="关键词" min-width="110" show-overflow-tooltip />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="摘要" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-summary">{{ ellipsis(row.summary, 80) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="原文链接" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <a
                v-if="row.articleUrl"
                class="link-open"
                :href="row.articleUrl"
                target="_blank"
                rel="noopener noreferrer"
                :title="row.articleUrl"
              >
                {{ linkPreview(row.articleUrl) }}
              </a>
              <span v-else class="cell-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="入库时间" width="178" align="center">
            <template #default="{ row }">
              {{ formatTime(row.fetchedAt) }}
            </template>
          </el-table-column>
        </el-table>

        <!-- RAG：instruction / output -->
        <el-table
          v-else-if="isRagInstructionTab"
          v-loading="loading"
          :data="rows"
          stripe
          class="kb-table"
          empty-text="暂无数据（请执行 SQL 建表并导入，或检查后端接口）"
          border
        >
          <el-table-column type="index" label="序号" width="72" align="center" :index="indexMethod" />
          <el-table-column prop="instruction" label="问题 / 指令" min-width="220" show-overflow-tooltip />
          <el-table-column label="回答 / 输出" min-width="280" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-summary">{{ ellipsis(row.output, 120) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="入库时间" width="178" align="center">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="148" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="onDeleteRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- RAG：药品 -->
        <el-table
          v-else-if="activeTab === 'drug'"
          v-loading="loading"
          :data="rows"
          stripe
          class="kb-table"
          empty-text="暂无数据（请执行 SQL 建表并导入，或检查后端接口）"
          border
        >
          <el-table-column type="index" label="序号" width="72" align="center" :index="indexMethod" />
          <el-table-column prop="drugName" label="药品名" min-width="140" show-overflow-tooltip />
          <el-table-column label="适应症" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-summary">{{ ellipsis(row.indicationText, 100) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="疾病标签" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="cell-summary">{{ row.diseasesLabeled || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="入库时间" width="178" align="center">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="148" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="onDeleteRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadPage"
          @size-change="loadPage"
        />
      </div>

      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="720px"
        class="kb-dialog"
        destroy-on-close
        @closed="resetDialogForm"
      >
        <div v-if="activeTab === 'drug'" class="kb-form">
          <div class="kb-form-row">
            <span class="kb-form-label">药品名</span>
            <el-input v-model="formDrug.drugName" placeholder="必填" />
          </div>
          <div class="kb-form-row">
            <span class="kb-form-label">适应症</span>
            <el-input v-model="formDrug.indicationText" type="textarea" :rows="5" placeholder="可含 HTML，向量化时会去标签" />
          </div>
          <div class="kb-form-row">
            <span class="kb-form-label">疾病标签</span>
            <el-input v-model="formDrug.diseasesLabeled" type="textarea" :rows="2" />
          </div>
        </div>
        <div v-else-if="activeTab === 'english'" class="kb-form">
          <div class="kb-form-row">
            <span class="kb-form-label">英文问句</span>
            <el-input v-model="formEnglish.input" type="textarea" :rows="5" placeholder="input，必填" />
          </div>
          <div class="kb-form-row">
            <span class="kb-form-label">英文回答</span>
            <el-input v-model="formEnglish.output" type="textarea" :rows="10" placeholder="output" />
          </div>
        </div>
        <div v-else class="kb-form">
          <div class="kb-form-row">
            <span class="kb-form-label">问题 / 指令</span>
            <el-input v-model="formInstruction.instruction" type="textarea" :rows="5" placeholder="必填" />
          </div>
          <div class="kb-form-row">
            <span class="kb-form-label">回答 / 输出</span>
            <el-input v-model="formInstruction.output" type="textarea" :rows="10" />
          </div>
        </div>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saveLoading" @click="submitDialog">保存</el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAdminRagKbDrug,
  deleteAdminRagKbEnglish,
  deleteAdminRagKbLiverCancer,
  deleteAdminRagKbLlama,
  deleteAdminRagKbQa,
  getAdminKnowledgeArticles,
  getAdminRagKbDrugPage,
  getAdminRagKbEnglishPage,
  getAdminRagKbLiverCancerPage,
  getAdminRagKbLlamaPage,
  getAdminRagKbQaPage,
  postAdminRagKbDrug,
  postAdminRagKbEnglish,
  postAdminRagKbLiverCancer,
  postAdminRagKbLlama,
  postAdminRagKbQa,
  postAdminRagKbRebuildVector,
  putAdminRagKbDrug,
  putAdminRagKbEnglish,
  putAdminRagKbLiverCancer,
  putAdminRagKbLlama,
  putAdminRagKbQa,
} from '@/api'

const loading = ref(false)
const rebuildLoading = ref(false)
const saveLoading = ref(false)
const keyword = ref('')
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
/** @type {import('vue').Ref<'articles'|'qa'|'liver'|'llama'|'drug'|'english'>} */
const activeTab = ref('articles')

const dialogVisible = ref(false)
/** @type {import('vue').Ref<'create'|'edit'>} */
const dialogMode = ref('create')
const editingId = ref(null)

const formInstruction = ref({ instruction: '', output: '' })
const formDrug = ref({ drugName: '', indicationText: '', diseasesLabeled: '' })
const formEnglish = ref({ input: '', output: '' })

const REBUILD_DATASET = {
  qa: 'qa',
  liver: 'liver',
  llama: 'llama',
  drug: 'drug',
  english: 'english',
}

const isRagInstructionTab = computed(() =>
  activeTab.value === 'qa' ||
  activeTab.value === 'liver' ||
  activeTab.value === 'llama' ||
  activeTab.value === 'english',
)

const dialogTitle = computed(() => {
  const m = dialogMode.value === 'edit' ? '编辑' : '新增'
  const map = {
    qa: 'RAG · 问答',
    liver: 'RAG · 肝癌',
    llama: 'RAG · Llama',
    drug: 'RAG · 药品',
    english: 'RAG · 英文',
  }
  return `${m} — ${map[activeTab.value] || ''}`
})

const keywordPlaceholder = computed(() => {
  if (activeTab.value === 'articles') return '按关键词筛选，如：高血压'
  if (activeTab.value === 'drug') return '药品名、适应症、疾病标签'
  if (activeTab.value === 'english') return '英文问句 / 回答关键词'
  return '问题/指令或回答内容'
})

function indexMethod(i) {
  return (pageNum.value - 1) * pageSize.value + i + 1
}

function ellipsis(s, n) {
  if (!s) return '—'
  return s.length <= n ? s : s.slice(0, n) + '…'
}

function formatTime(t) {
  if (!t) return '—'
  if (typeof t === 'string') return t.replace('T', ' ').slice(0, 19)
  return String(t)
}

/** 展示 🔗 + 域名与路径片段，完整地址放在 title / href */
function linkPreview(url) {
  if (!url || typeof url !== 'string') return '—'
  const raw = url.trim()
  try {
    const u = new URL(raw)
    const host = u.hostname || ''
    let rest = (u.pathname || '') + (u.search || '')
    if (rest === '/' || rest === '') rest = ''
    let text = host + rest
    if (text.length > 56) text = text.slice(0, 53) + '…'
    return '🔗 ' + text
  } catch {
    let s = raw.replace(/^https?:\/\//i, '')
    if (s.length > 56) s = s.slice(0, 53) + '…'
    return '🔗 ' + s
  }
}

function onSearch() {
  pageNum.value = 1
  loadPage()
}

function onTabChange() {
  pageNum.value = 1
  keyword.value = ''
  loadPage()
}

function resetDialogForm() {
  formInstruction.value = { instruction: '', output: '' }
  formDrug.value = { drugName: '', indicationText: '', diseasesLabeled: '' }
  formEnglish.value = { input: '', output: '' }
  editingId.value = null
  dialogMode.value = 'create'
}

function openCreate() {
  if (activeTab.value === 'articles') return
  dialogMode.value = 'create'
  editingId.value = null
  resetDialogForm()
  dialogVisible.value = true
}

function openEdit(row) {
  if (activeTab.value === 'articles') return
  dialogMode.value = 'edit'
  editingId.value = row.id
  if (activeTab.value === 'drug') {
    formDrug.value = {
      drugName: row.drugName || '',
      indicationText: row.indicationText || '',
      diseasesLabeled: row.diseasesLabeled || '',
    }
  } else if (activeTab.value === 'english') {
    formEnglish.value = {
      input: row.instruction || '',
      output: row.output || '',
    }
  } else {
    formInstruction.value = {
      instruction: row.instruction || '',
      output: row.output || '',
    }
  }
  dialogVisible.value = true
}

async function onDeleteRow(row) {
  if (activeTab.value === 'articles') return
  try {
    await ElMessageBox.confirm('确定删除该条记录？删除后需「刷新向量库」才会更新检索。', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  const id = row.id
  try {
    switch (activeTab.value) {
      case 'qa':
        await deleteAdminRagKbQa(id)
        break
      case 'liver':
        await deleteAdminRagKbLiverCancer(id)
        break
      case 'llama':
        await deleteAdminRagKbLlama(id)
        break
      case 'drug':
        await deleteAdminRagKbDrug(id)
        break
      case 'english':
        await deleteAdminRagKbEnglish(id)
        break
      default:
        return
    }
    ElMessage.success('已删除')
    loadPage()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

async function submitDialog() {
  const tab = activeTab.value
  if (tab === 'articles') return

  if (tab === 'drug') {
    const p = formDrug.value
    if (!p.drugName?.trim()) {
      ElMessage.warning('请填写药品名')
      return
    }
  } else if (tab === 'english') {
    const p = formEnglish.value
    if (!p.input?.trim()) {
      ElMessage.warning('请填写英文问句')
      return
    }
  } else {
    const p = formInstruction.value
    if (!p.instruction?.trim()) {
      ElMessage.warning('请填写问题/指令')
      return
    }
  }

  saveLoading.value = true
  try {
    if (tab === 'drug') {
      const p = formDrug.value
      if (dialogMode.value === 'create') {
        await postAdminRagKbDrug({
          drugName: p.drugName.trim(),
          indicationText: p.indicationText || '',
          diseasesLabeled: p.diseasesLabeled || '',
        })
      } else {
        await putAdminRagKbDrug(editingId.value, {
          drugName: p.drugName.trim(),
          indicationText: p.indicationText || '',
          diseasesLabeled: p.diseasesLabeled || '',
        })
      }
    } else if (tab === 'english') {
      const p = formEnglish.value
      if (dialogMode.value === 'create') {
        await postAdminRagKbEnglish({
          input: p.input.trim(),
          output: p.output || '',
        })
      } else {
        await putAdminRagKbEnglish(editingId.value, {
          input: p.input.trim(),
          output: p.output || '',
        })
      }
    } else {
      const p = formInstruction.value
      const body = { instruction: p.instruction.trim(), output: p.output || '' }
      if (tab === 'qa') {
        if (dialogMode.value === 'create') await postAdminRagKbQa(body)
        else await putAdminRagKbQa(editingId.value, body)
      } else if (tab === 'liver') {
        if (dialogMode.value === 'create') await postAdminRagKbLiverCancer(body)
        else await putAdminRagKbLiverCancer(editingId.value, body)
      } else if (tab === 'llama') {
        if (dialogMode.value === 'create') await postAdminRagKbLlama(body)
        else await putAdminRagKbLlama(editingId.value, body)
      }
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadPage()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

async function onRebuildVector() {
  const ds = REBUILD_DATASET[activeTab.value]
  if (!ds) return
  try {
    await ElMessageBox.confirm(
      '将从当前 MySQL 数据表重建该数据集在 Chroma 中的向量，耗时与数据量有关；请勿重复点击。是否继续？',
      '刷新向量库',
      { type: 'warning', confirmButtonText: '开始重建', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  rebuildLoading.value = true
  try {
    const res = await postAdminRagKbRebuildVector({ dataset: ds })
    ElMessage.success(res?.message || '已提交后台任务')
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    rebuildLoading.value = false
  }
}

async function loadPage() {
  loading.value = true
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    keyword: keyword.value?.trim() || undefined,
  }
  try {
    let res
    switch (activeTab.value) {
      case 'articles':
        res = await getAdminKnowledgeArticles(params)
        break
      case 'qa':
        res = await getAdminRagKbQaPage(params)
        break
      case 'liver':
        res = await getAdminRagKbLiverCancerPage(params)
        break
      case 'llama':
        res = await getAdminRagKbLlamaPage(params)
        break
      case 'drug':
        res = await getAdminRagKbDrugPage(params)
        break
      case 'english':
        res = await getAdminRagKbEnglishPage(params)
        break
      default:
        res = null
    }
    const data = res?.data ?? res
    rows.value = data?.records ?? []
    total.value = data?.total ?? 0
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPage()
})
</script>

<style scoped>
.page-kb {
  max-width: 1680px;
  margin: 0 auto;
  min-height: 100%;
}

.head-inner {
  max-width: 960px;
}

.page-head {
  padding: 18px 22px 20px;
  margin-bottom: 18px;
}

.page-head h2 {
  margin: 8px 0 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #f8fafc;
}

.sub {
  margin: 0;
  font-size: 13px;
  color: #5eead4;
  letter-spacing: 0.5px;
}

.desc {
  margin: 14px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #94a3b8;
}

.panel {
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.72) 0%, rgba(15, 23, 42, 0.85) 100%);
  border: 1px solid rgba(94, 234, 212, 0.14);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(2, 6, 23, 0.35);
}

.float-in {
  animation: float-in 0.45s ease-out both;
}

@keyframes float-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 与全页深蓝渐变背景协调的知识库主区域 */
.kb-card {
  background: linear-gradient(
    160deg,
    rgba(30, 58, 138, 0.35) 0%,
    rgba(15, 23, 42, 0.88) 42%,
    rgba(15, 118, 110, 0.14) 100%
  );
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(2, 6, 23, 0.45);
  border: 1px solid rgba(94, 234, 212, 0.18);
  padding: 20px 22px 18px;
  animation-delay: 0.06s;
}

.kb-tabs {
  margin-bottom: 16px;
}

.kb-tabs :deep(.el-tabs__header) {
  margin: 0 0 4px;
}

.kb-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(71, 85, 105, 0.45);
}

.kb-tabs :deep(.el-tabs__nav) {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

.kb-tabs :deep(.el-tabs__item) {
  flex: 1 1 0;
  text-align: center;
  color: #94a3b8;
  font-weight: 500;
}

.kb-tabs :deep(.el-tabs__item:hover) {
  color: #5eead4;
}

.kb-tabs :deep(.el-tabs__item.is-active) {
  color: #e2e8f0;
  font-weight: 600;
}

.kb-tabs :deep(.el-tabs__active-bar) {
  background-color: #5eead4;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.kw-input {
  width: 280px;
}

.kw-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.35) inset;
  background: rgba(15, 23, 42, 0.55);
  --el-input-text-color: #e2e8f0;
  --el-input-placeholder-color: rgba(148, 163, 184, 0.85);
}

.kw-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(56, 189, 248, 0.65) inset;
}

.btn-query {
  min-width: 88px;
  font-weight: 600;
  --el-button-bg-color: #1890ff;
  --el-button-border-color: #1890ff;
  --el-button-hover-bg-color: #40a9ff;
  --el-button-hover-border-color: #40a9ff;
  --el-button-active-bg-color: #096dd9;
  --el-button-active-border-color: #096dd9;
}

.btn-refresh {
  min-width: 88px;
  font-weight: 500;
  color: #e2e8f0;
  background: rgba(30, 41, 59, 0.65);
  border: 1px solid rgba(148, 163, 184, 0.35);
  --el-button-hover-text-color: #f8fafc;
  --el-button-hover-bg-color: rgba(51, 65, 85, 0.75);
  --el-button-hover-border-color: rgba(94, 234, 212, 0.35);
}

.btn-add {
  min-width: 88px;
  font-weight: 600;
}

.btn-rebuild {
  min-width: 120px;
  font-weight: 600;
}

.kb-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.kb-form-row {
  margin-bottom: 14px;
}

.kb-form-label {
  display: block;
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.table-wrap {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(71, 85, 105, 0.45);
}

.kb-table {
  width: 100%;
  --el-table-border-color: rgba(71, 85, 105, 0.45);
  --el-table-bg-color: rgba(15, 23, 42, 0.4);
  --el-table-tr-bg-color: rgba(15, 23, 42, 0.35);
  --el-table-header-bg-color: rgba(30, 58, 138, 0.35);
  --el-table-header-text-color: #e2e8f0;
  --el-table-text-color: #e2e8f0;
  --el-table-row-hover-bg-color: rgba(45, 212, 191, 0.12);
}

.kb-table :deep(.el-table__header th) {
  font-weight: 600;
  font-size: 13px;
}

.kb-table :deep(.el-table__body tr.el-table__row--striped > td.el-table__cell) {
  background-color: rgba(30, 41, 59, 0.42) !important;
}

.kb-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.kb-table :deep(.cell) {
  line-height: 1.5;
}

.cell-summary {
  color: #cbd5e1;
  font-size: 13px;
}

.cell-dash {
  color: #cbd5e1;
}

.link-open {
  display: inline-block;
  max-width: 100%;
  color: #5eead4;
  font-weight: 500;
  font-size: 13px;
  text-decoration: none;
  word-break: break-all;
  cursor: pointer;
}

.link-open:hover {
  color: #99f6e4;
  text-decoration: underline;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
  padding-top: 4px;
}

.pager :deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #1890ff !important;
  color: #fff !important;
}

.pager :deep(.el-pagination.is-background .btn-prev),
.pager :deep(.el-pagination.is-background .btn-next),
.pager :deep(.el-pagination.is-background .el-pager li) {
  background-color: rgba(30, 41, 59, 0.75);
  color: #cbd5e1;
}

.pager :deep(.el-pagination.is-background .btn-prev:hover),
.pager :deep(.el-pagination.is-background .btn-next:hover),
.pager :deep(.el-pagination.is-background .el-pager li:hover) {
  color: #5eead4;
}

.pager :deep(.el-pagination__total),
.pager :deep(.el-pagination__sizes .el-select .el-input__inner) {
  color: #94a3b8;
}
</style>
