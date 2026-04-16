<template>
  <div class="dashboard dashboard-compact dashboard-full">
    <section class="screen-header panel float-in">
      <div>
        <p class="sub-title">医视智行 · 数据驾驶舱</p>
        <h2>工作台</h2>
      </div>
      <div class="header-right">
        <span class="time">{{ nowText }}</span>
        <el-button type="primary" class="refresh-btn" round :loading="loading" @click="loadAll">刷新数据</el-button>
      </div>
    </section>

    <!-- 工作台主体：指标区与智能体矩阵均分剩余高度 -->
    <div class="dashboard-body">
    <!-- 第一行：3 个指标 + 情绪使用率与饼图同屏 -->
    <section class="metric-row">
      <article
        v-for="(card, i) in topMetricCards"
        :key="card.key"
        class="panel metric-card float-in metric-card-sm"
        :style="{ animationDelay: `${0.04 * i}s` }"
      >
        <div class="metric-icon-wrap" :class="card.iconClass">
          <el-icon :size="28"><component :is="card.icon" /></el-icon>
        </div>
        <p>{{ card.title }}</p>
        <h3>{{ card.value }}</h3>
        <span>{{ card.desc }}</span>
      </article>

      <article class="panel emotion-panel float-in" style="animation-delay: 0.14s">
        <div class="emotion-panel-inner">
          <div class="emotion-metric">
            <div class="metric-icon-wrap icon-d">
              <el-icon :size="28"><DataAnalysis /></el-icon>
            </div>
            <p>情绪监测使用率</p>
            <h3>{{ formatPercent(overview.emotionUsagePercent) }}</h3>
            <span>共 {{ formatNumber(overview.emotionRecordTotal) }} 条 · 有过情绪记录的用户占比</span>
          </div>
          <div class="emotion-pie-wrap">
            <div v-if="!hasEmotionData" class="pie-empty">暂无分值分布</div>
            <div v-show="hasEmotionData" class="pie-chart-host">
              <div ref="pieRef" class="pie-canvas" />
              <p class="pie-center-hint">点击环心区域放大</p>
            </div>
          </div>
        </div>
      </article>
    </section>

    <!-- 智能体矩阵：悬停上浮；头条关键词在「关键词负载」中管理 -->
    <section class="agents-section panel float-in" style="animation-delay: 0.18s">
      <div class="agents-head">
        <h4>智能体矩阵</h4>
      </div>
      <div class="agents-grid">
        <div
          v-for="(a, idx) in agentListDisplay"
          :key="a.id"
          class="agent-tile float-in"
          :class="{ 'is-disabled': a.enabled === false, 'is-toutiao': a.kind === 'toutiao' }"
          :style="{ animationDelay: `${0.02 * idx}s` }"
        >
          <div
            class="agent-tile-inner"
            :class="{
              'is-chat': a.kind === 'chat-emotion',
              'is-ml': a.kind === 'ml',
              'is-mcp': a.kind === 'mcp',
              'is-toutiao': a.kind === 'toutiao',
            }"
          >
            <div class="agent-icon" :class="'tone-' + (idx % 6)">
              <el-icon :size="28"><component :is="a.icon" /></el-icon>
            </div>
            <div class="agent-text">
              <span class="agent-name">{{ a.displayName }}</span>
              <span class="agent-role">{{ a.role }}</span>
            </div>

            <template v-if="a.kind === 'chat-emotion'">
              <div class="agent-detail-block" @click.stop>
                <div class="agent-detail-row">
                  <span class="detail-label">中文对话</span>
                  <button type="button" class="mini-text-btn" @click="openPromptEditor('chat')">提示词</button>
                </div>
                <div class="agent-detail-row">
                  <span class="detail-label">英文对话</span>
                  <button type="button" class="mini-text-btn" @click="openPromptEditor('emotion')">提示词</button>
                </div>
              </div>
            </template>

            <template v-else-if="a.kind === 'ml'">
              <div class="agent-detail-block" @click.stop>
                <div class="agent-detail-row">
                  <span class="detail-label">运动识别</span>
                  <button type="button" class="mini-text-btn" @click="openTrainImage('exercise-results')">训练结果</button>
                  <button type="button" class="mini-link-btn" @click="openDatasetLink('mlExercise')">数据集 🔗</button>
                </div>
                <div class="agent-detail-row">
                  <span class="detail-label">三餐识别</span>
                  <button type="button" class="mini-text-btn" @click="openTrainImage('food-results')">训练结果</button>
                  <button type="button" class="mini-link-btn" @click="openDatasetLink('mlFood')">数据集 🔗</button>
                </div>
              </div>
            </template>

            <template v-else-if="a.kind === 'toutiao'">
              <div class="crawler-kw-row" @click.stop>
                <el-tooltip placement="top" popper-class="kw-tooltip-dark">
                  <template #content>
                    <div v-if="!kwRows.length" class="kw-tooltip-empty">暂无关键词</div>
                    <div v-else class="kw-tooltip-inner">
                      <div v-for="row in kwRows.slice(0, 40)" :key="row.id" class="kw-tooltip-line">
                        <span :class="{ 'is-off': row.enabled === false }">{{ row.keyword }}</span>
                      </div>
                      <div v-if="kwRows.length > 40" class="kw-tooltip-more">…共 {{ kwRows.length }} 条</div>
                    </div>
                  </template>
                  <button type="button" class="kw-load-btn" @click="openCrawlerKwPanel">
                    关键词负载
                    <span class="kw-load-count">{{ kwEnabledCount }}/{{ kwRows.length }}</span>
                  </button>
                </el-tooltip>
              </div>
            </template>

            <template v-else-if="a.kind === 'mcp'">
              <div class="mcp-sub" @click.stop>
                <button type="button" class="mcp-pill disease-pill-btn" @click="openDiseasePredictDialog">疾病预测</button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </section>
    </div>

    <!-- 头条关键词：与「疾病预测」弹窗同一套视觉（蓝渐变标题区 + 高对比表体） -->
    <el-dialog
      v-model="crawlerKwPanelVisible"
      title="头条关键词"
      width="900px"
      class="dashboard-disease-dialog"
      append-to-body
      align-center
      destroy-on-close
      @open="loadCrawlKeywords"
    >
      <div class="disease-toolbar kw-toolbar-row">
        <el-button type="primary" class="refresh-btn" round size="small" :loading="kwLoading" @click="loadCrawlKeywords">
          刷新
        </el-button>
        <el-button type="success" class="refresh-btn" round size="small" @click="openKwCreate">新增</el-button>
      </div>
      <el-table
        v-loading="kwLoading"
        :data="kwRows"
        stripe
        border
        class="disease-predict-table"
        empty-text="暂无关键词"
        max-height="440"
      >
        <el-table-column type="index" label="" width="56" align="center" />
        <el-table-column prop="keyword" label="关键词" min-width="120" show-overflow-tooltip align="center" />
        <el-table-column prop="sortOrder" label="排序" width="88" align="center" />
        <el-table-column label="启用" width="100" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === true" @change="(v) => onKwToggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip align="center" />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openKwEdit(row)">编辑</el-button>
            <el-button type="danger" link @click.stop="onKwDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 关键词弹窗 -->
    <el-dialog
      v-model="kwDialogVisible"
      :title="kwDialogTitle"
      width="480px"
      class="dashboard-kw-form-dialog"
      append-to-body
      destroy-on-close
      @closed="resetKwForm"
    >
      <el-form ref="kwFormRef" :model="kwForm" :rules="kwRules" label-width="88px">
        <el-form-item label="关键词" prop="keyword">
          <el-input v-model="kwForm.keyword" maxlength="128" show-word-limit placeholder="如：高血压" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="kwForm.sortOrder" :min="0" :max="999999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="kwForm.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="kwForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="kwDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="kwSaving" @click="submitKw">保存</el-button>
      </template>
    </el-dialog>

    <!-- 训练结果图 -->
    <el-dialog
      v-model="trainImgVisible"
      title="训练结果"
      width="92%"
      class="dashboard-kw-form-dialog train-img-dialog"
      append-to-body
      align-center
      destroy-on-close
    >
      <div v-loading="trainImgLoading" class="train-img-wrap">
        <p v-if="trainImgError" class="train-img-err">{{ trainImgError }}</p>
        <img
          v-show="!trainImgError"
          :src="trainImgSrc"
          alt="训练结果"
          class="train-img"
          @load="onTrainImgLoad"
          @error="onTrainImgError"
        />
      </div>
    </el-dialog>

    <!-- 提示词编辑 -->
    <el-dialog
      v-model="promptEditorVisible"
      :title="promptEditorTitle"
      width="640px"
      class="dashboard-kw-form-dialog"
      append-to-body
      destroy-on-close
      @closed="promptText = ''"
    >
      <div v-loading="promptLoading" class="prompt-editor-wrap">
        <el-input v-model="promptText" type="textarea" :rows="18" placeholder="提示词内容" class="prompt-textarea" />
      </div>
      <template #footer>
        <el-button @click="promptEditorVisible = false">取消</el-button>
        <el-button type="primary" :loading="promptSaving" @click="savePromptToFile">保存到文件</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="diseaseDialogVisible"
      title="疾病预测"
      width="900px"
      class="dashboard-disease-dialog"
      append-to-body
      destroy-on-close
      @open="loadDiseasePredictions"
    >
      <div class="disease-toolbar">
        <el-button type="primary" class="refresh-btn" round size="small" :loading="diseaseLoading" @click="loadDiseasePredictions">
          刷新
        </el-button>
      </div>
      <el-table
        v-loading="diseaseLoading"
        :data="diseaseRows"
        stripe
        border
        class="disease-predict-table"
        max-height="440"
        empty-text="暂无预测数据"
      >
        <el-table-column prop="userId" label="用户ID" width="100" align="center" />
        <el-table-column prop="nickname" label="用户昵称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="diseaseName" label="预测疾病" min-width="220" show-overflow-tooltip />
        <el-table-column label="概率" width="100" align="center">
          <template #default="{ row }">{{ formatConfidence(row.confidence) }}</template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="100" align="center" />
        <el-table-column label="时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 情绪饼图放大 -->
    <el-dialog
      v-model="pieZoomVisible"
      title="情绪分值分布"
      width="92%"
      class="dashboard-kw-form-dialog pie-zoom-dialog"
      append-to-body
      align-center
      destroy-on-close
      @closed="onPieZoomDialogClosed"
    >
      <p class="pie-zoom-tip">与工作台数据一致 · 可悬停查看占比</p>
      <div ref="pieZoomRef" class="pie-zoom-canvas" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ChatDotRound, Cpu, DataAnalysis, Document, Promotion, Share, TrendCharts, User } from '@element-plus/icons-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAdminCrawlKeyword,
  getAdminCrawlKeywords,
  getAdminDashboardOverview,
  getAdminInferredDiseases,
  getDashboardDevImageUrl,
  getDashboardDevPrompt,
  postAdminCrawlKeyword,
  putAdminCrawlKeyword,
  putDashboardDevPrompt,
} from '@/api'

const AGENT_STORAGE_KEY = 'dashboard_agents_config_v1'

const AGENT_DEFS = [
  {
    id: 'chat-emotion',
    name: '对话智能体',
    role: '问诊对话 · 中外结合',
    defaultUrl: 'http://127.0.0.1:8000',
    icon: ChatDotRound,
    kind: 'chat-emotion',
  },
  {
    id: 'ml',
    name: '机器学习智能体',
    role: '运动识别与训练 · 饮食评估与建议',
    defaultUrl: 'http://127.0.0.1:5000',
    secondaryDefault: 'http://127.0.0.1:5000',
    icon: TrendCharts,
    kind: 'ml',
  },
  {
    id: 'toutiao',
    name: '头条智能体',
    role: '健康资讯抓取 · 关键词种子',
    defaultUrl: 'http://127.0.0.1:8095',
    icon: Promotion,
    kind: 'toutiao',
  },
  {
    id: 'mcp',
    name: '健康分析智能体',
    role: '健康风险评估 / 趋势分析',
    defaultUrl: 'http://127.0.0.1:8000',
    icon: Cpu,
    kind: 'mcp',
  },
]

const loading = ref(false)
const kwLoading = ref(false)
const kwSaving = ref(false)
const kwRows = ref([])
const kwDialogVisible = ref(false)
const kwEditingId = ref(null)
const kwFormRef = ref(null)
const kwForm = reactive({
  keyword: '',
  sortOrder: 0,
  enabled: true,
  remark: '',
})
const kwRules = {
  keyword: [{ required: true, message: '请输入关键词', trigger: 'blur' }],
}
const kwDialogTitle = computed(() => (kwEditingId.value ? '编辑关键词' : '新增关键词'))

const crawlerKwPanelVisible = ref(false)

const trainImgVisible = ref(false)
const trainImgName = ref('exercise-results')
const trainImgBust = ref(0)
const trainImgLoading = ref(false)
const trainImgError = ref('')
const trainImgSrc = computed(() => `${getDashboardDevImageUrl(trainImgName.value)}?t=${trainImgBust.value}`)

const promptEditorVisible = ref(false)
const promptSlot = ref('chat')
const promptText = ref('')
const promptLoading = ref(false)
const promptSaving = ref(false)
const promptEditorTitle = computed(() =>
  promptSlot.value === 'emotion' ? '英文对话 · 提示词' : '中文对话 · 提示词'
)

const DATASET_URLS = {
  mlExercise: 'https://app.roboflow.com/aajyis-workspace/exercise-ekbld-kjve3/browse?queryText=&pageSize=50&startingIndex=0&browseQuery=true',
  mlFood: 'https://www.kaggle.com/datasets/gillesokhin/nutrition5k-dataset',
}

const diseaseDialogVisible = ref(false)
const diseaseLoading = ref(false)
const diseaseRows = ref([])

const kwEnabledCount = computed(() => kwRows.value.filter((r) => r.enabled === true).length)

function openCrawlerKwPanel() {
  crawlerKwPanelVisible.value = true
}

function openTrainImage(name) {
  trainImgName.value = name
  trainImgBust.value = Date.now()
  trainImgError.value = ''
  trainImgLoading.value = true
  trainImgVisible.value = true
}

function onTrainImgLoad() {
  trainImgLoading.value = false
  trainImgError.value = ''
}

function onTrainImgError() {
  trainImgLoading.value = false
  trainImgError.value =
    '无法加载图片：请确认后端 application.yml 中 dashboard.dev-assets 路径正确且 PNG 文件存在。'
}

async function openPromptEditor(slot) {
  promptSlot.value = slot
  promptEditorVisible.value = true
  promptLoading.value = true
  promptText.value = ''
  try {
    promptText.value = await getDashboardDevPrompt(slot)
  } catch (e) {
    ElMessage.error(e?.message || '加载提示词失败')
  } finally {
    promptLoading.value = false
  }
}

async function savePromptToFile() {
  promptSaving.value = true
  try {
    await putDashboardDevPrompt(promptSlot.value, promptText.value)
    ElMessage.success('已保存到服务器文件')
    promptEditorVisible.value = false
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    promptSaving.value = false
  }
}

function openDatasetLink(key) {
  const u = DATASET_URLS[key] || ''
  if (!u) {
    ElMessage.warning('未配置数据集链接')
    return
  }
  window.open(u, '_blank', 'noopener')
}

function openDiseasePredictDialog() {
  diseaseDialogVisible.value = true
}

function formatDateTime(v) {
  if (!v) return '—'
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ').slice(0, 19) : s.slice(0, 19)
}

function formatConfidence(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '—'
  return `${(n * 100).toFixed(1)}%`
}

async function loadDiseasePredictions() {
  diseaseLoading.value = true
  try {
    const res = await getAdminInferredDiseases({ limit: 400 })
    const data = res?.data ?? res
    diseaseRows.value = Array.isArray(data) ? data : []
  } catch (e) {
    // 兼容后端尚未发布该接口：404 时按空列表处理，不弹错误
    const status = e?.response?.status
    if (status !== 404) {
      ElMessage.error(e?.message || '加载疾病预测失败')
    }
    diseaseRows.value = []
  } finally {
    diseaseLoading.value = false
  }
}

const nowText = ref('')
const overview = ref({
  userTotal: 0,
  rawHealthDataTotal: 0,
  familyMemberTotal: 0,
  familyPenetrationPercent: 0,
  emotionRecordTotal: 0,
  emotionUsagePercent: 0,
  emotionLevelShares: [],
})

const pieRef = ref(null)
let pieChart = null
const pieZoomVisible = ref(false)
const pieZoomRef = ref(null)
let pieZoomChart = null
let timer = null

const hasEmotionData = computed(() => (overview.value.emotionLevelShares || []).length > 0)

const topMetricCards = computed(() => {
  const o = overview.value
  return [
    {
      key: 'user',
      title: '注册用户',
      value: formatNumber(o.userTotal),
      desc: '当前使用产品的用户总数',
      icon: User,
      iconClass: 'icon-a',
    },
    {
      key: 'raw',
      title: '原始健康数据',
      value: formatNumber(o.rawHealthDataTotal),
      desc: '上传的原始健康数据条数',
      icon: Document,
      iconClass: 'icon-b',
    },
    {
      key: 'family',
      title: '家人管理普及率',
      value: formatPercent(o.familyPenetrationPercent),
      desc: `共 ${formatNumber(o.familyMemberTotal)} 条家人档案`,
      icon: Share,
      iconClass: 'icon-c',
    },
  ]
})

const agentOverrides = ref({})

function loadAgentOverrides() {
  try {
    const raw = localStorage.getItem(AGENT_STORAGE_KEY)
    let o = raw ? JSON.parse(raw) : {}
    if (!o['chat-emotion'] && (o.chat || o.emotion)) {
      const c = o.chat || {}
      const e = o.emotion || {}
      o['chat-emotion'] = {
        displayName: c.displayName || e.displayName,
        baseUrl: c.baseUrl || e.baseUrl,
        note: [c.note, e.note].filter(Boolean).join('；') || '',
        enabled: c.enabled !== false && e.enabled !== false,
      }
    }
    if (!o.ml && (o.exercise || o.food)) {
      const x = o.exercise || {}
      const f = o.food || {}
      o.ml = {
        displayName: x.displayName || f.displayName,
        baseUrl: x.baseUrl || f.baseUrl,
        secondaryUrl: f.baseUrl || x.baseUrl,
        note: [x.note, f.note].filter(Boolean).join('；') || '',
        enabled: x.enabled !== false && f.enabled !== false,
      }
    }
    if (!o.mcp && o['smart-asst']) {
      o.mcp = { ...o['smart-asst'] }
    }
    if (!o.toutiao && o.crawler) {
      o.toutiao = { ...o.crawler }
    }
    agentOverrides.value = o
  } catch {
    agentOverrides.value = {}
  }
}

const agentListDisplay = computed(() => {
  return AGENT_DEFS.map((def) => {
    const ov = agentOverrides.value[def.id] || {}
    const enabled = ov.enabled !== false
    const secondaryDefault = def.secondaryDefault || def.defaultUrl
    return {
      ...def,
      displayName: ov.displayName || def.name,
      effectiveUrl: (ov.baseUrl || def.defaultUrl || '').trim() || def.defaultUrl,
      effectiveSecondaryUrl: (ov.secondaryUrl || secondaryDefault || '').trim() || secondaryDefault,
      note: ov.note || '',
      enabled,
    }
  })
})

function formatNumber(val) {
  return Number(val || 0).toLocaleString('zh-CN')
}

function formatPercent(val) {
  if (val == null || val === '') return '0%'
  const n = Number(val)
  if (Number.isNaN(n)) return '0%'
  return `${n.toFixed(1)}%`
}

function resetKwForm() {
  kwForm.keyword = ''
  kwForm.sortOrder = 0
  kwForm.enabled = true
  kwForm.remark = ''
  kwEditingId.value = null
}

async function loadCrawlKeywords() {
  kwLoading.value = true
  try {
    const res = await getAdminCrawlKeywords()
    const data = res?.data ?? res
    kwRows.value = Array.isArray(data) ? data : []
  } catch (e) {
    ElMessage.error(e?.message || '关键词加载失败')
    kwRows.value = []
  } finally {
    kwLoading.value = false
  }
}

function openKwCreate() {
  resetKwForm()
  kwDialogVisible.value = true
}

function openKwEdit(row) {
  kwEditingId.value = row.id
  kwForm.keyword = row.keyword || ''
  kwForm.sortOrder = row.sortOrder ?? 0
  kwForm.enabled = row.enabled !== false
  kwForm.remark = row.remark || ''
  kwDialogVisible.value = true
}

async function submitKw() {
  try {
    await kwFormRef.value?.validate?.()
  } catch {
    return
  }
  kwSaving.value = true
  try {
    const payload = {
      keyword: kwForm.keyword.trim(),
      sortOrder: kwForm.sortOrder ?? 0,
      enabled: kwForm.enabled,
      remark: kwForm.remark?.trim() || null,
    }
    if (kwEditingId.value) {
      await putAdminCrawlKeyword(kwEditingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await postAdminCrawlKeyword(payload)
      ElMessage.success('已新增')
    }
    kwDialogVisible.value = false
    await loadCrawlKeywords()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    kwSaving.value = false
  }
}

async function onKwToggle(row, enabled) {
  try {
    await putAdminCrawlKeyword(row.id, {
      keyword: row.keyword,
      sortOrder: row.sortOrder,
      enabled,
      remark: row.remark,
    })
    row.enabled = enabled
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch (e) {
    ElMessage.error(e?.message || '更新失败')
    await loadCrawlKeywords()
  }
}

function onKwDelete(row) {
  ElMessageBox.confirm(`确定删除关键词「${row.keyword}」？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      await deleteAdminCrawlKeyword(row.id)
      ElMessage.success('已删除')
      await loadCrawlKeywords()
    })
    .catch(() => {})
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadOverviewInner(), loadCrawlKeywords()])
  } finally {
    loading.value = false
  }
}

async function loadOverviewInner() {
  try {
    const res = await getAdminDashboardOverview()
    const data = res?.data ?? res
    overview.value = {
      userTotal: data?.userTotal ?? 0,
      rawHealthDataTotal: data?.rawHealthDataTotal ?? 0,
      familyMemberTotal: data?.familyMemberTotal ?? 0,
      familyPenetrationPercent: data?.familyPenetrationPercent ?? 0,
      emotionRecordTotal: data?.emotionRecordTotal ?? 0,
      emotionUsagePercent: data?.emotionUsagePercent ?? 0,
      emotionLevelShares: data?.emotionLevelShares ?? [],
    }
    await nextTick()
    renderPieOnly()
  } catch {
    overview.value = {
      userTotal: 0,
      rawHealthDataTotal: 0,
      familyMemberTotal: 0,
      familyPenetrationPercent: 0,
      emotionRecordTotal: 0,
      emotionUsagePercent: 0,
      emotionLevelShares: [],
    }
    disposePie()
  }
}

function updateNowText() {
  nowText.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

function disposePie() {
  if (pieChart) {
    pieChart.getZr().off('click', onPieZrClick)
    pieChart.dispose()
    pieChart = null
  }
}

function resizePie() {
  pieChart?.resize()
}

function buildEmotionPieOption(large) {
  const rows = overview.value.emotionLevelShares || []
  const pieData = rows.map((r) => ({
    name: `${r.rangeLabel} ${r.name}`,
    value: Number(r.percent) || 0,
  }))
  const colors = ['#34d399', '#2dd4bf', '#38bdf8', '#a78bfa', '#fb7185']
  const fs = large ? 16 : 14
  const legFs = large ? 15 : 14
  return {
    backgroundColor: 'transparent',
    color: colors,
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>占比：{c}%',
    },
    legend: {
      show: true,
      type: 'scroll',
      orient: 'horizontal',
      bottom: large ? 12 : 0,
      textStyle: { color: '#94a3b8', fontSize: legFs },
      itemWidth: large ? 10 : 8,
      itemHeight: large ? 10 : 8,
    },
    series: [
      {
        type: 'pie',
        radius: large ? ['36%', '64%'] : ['32%', '58%'],
        center: ['50%', large ? '46%' : '44%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: 'rgba(15,23,42,0.9)',
          borderWidth: 1,
        },
        label: { color: '#e2e8f0', fontSize: fs, formatter: '{d}%' },
        emphasis: {
          scale: true,
          itemStyle: { shadowBlur: 12, shadowColor: 'rgba(45,212,191,0.35)' },
        },
        data: pieData,
      },
    ],
  }
}

/** 环心点击：内半径与 series 中 32% 一致（相对 min 边长的一半） */
function onPieZrClick(e) {
  if (!pieChart) return
  const w = pieChart.getWidth()
  const h = pieChart.getHeight()
  const half = Math.min(w, h) / 2
  const rInner = half * 0.32 + 6
  const cx = w * 0.5
  const cy = h * 0.44
  const dx = e.offsetX - cx
  const dy = e.offsetY - cy
  if (Math.sqrt(dx * dx + dy * dy) <= rInner) {
    pieZoomVisible.value = true
    nextTick(() => {
      renderPieZoomChart()
      window.addEventListener('resize', resizePieZoom)
    })
  }
}

function bindPieCenterClick() {
  if (!pieChart) return
  const zr = pieChart.getZr()
  zr.off('click', onPieZrClick)
  zr.on('click', onPieZrClick)
}

function resizePieZoom() {
  pieZoomChart?.resize()
}

function renderPieZoomChart() {
  const rows = overview.value.emotionLevelShares || []
  if (!rows.length || !pieZoomRef.value) return
  if (!pieZoomChart) pieZoomChart = echarts.init(pieZoomRef.value)
  pieZoomChart.setOption(buildEmotionPieOption(true))
  pieZoomChart.resize()
}

function onPieZoomDialogClosed() {
  window.removeEventListener('resize', resizePieZoom)
  pieZoomChart?.dispose()
  pieZoomChart = null
}

function renderPieOnly() {
  const rows = overview.value.emotionLevelShares || []
  if (!rows.length || !pieRef.value) {
    disposePie()
    return
  }

  nextTick(() => {
    if (!pieRef.value) return
    if (!pieChart) pieChart = echarts.init(pieRef.value)
    pieChart.setOption(buildEmotionPieOption(false))
    resizePie()
    bindPieCenterClick()
  })
}

watch(
  () => overview.value.emotionLevelShares,
  () => {
    if ((overview.value.emotionLevelShares || []).length) {
      nextTick(() => renderPieOnly())
    } else {
      disposePie()
    }
  },
  { deep: true }
)

onMounted(async () => {
  loadAgentOverrides()
  updateNowText()
  timer = window.setInterval(updateNowText, 1000)
  window.addEventListener('resize', resizePie)
  await loadAll()
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  window.removeEventListener('resize', resizePie)
  window.removeEventListener('resize', resizePieZoom)
  disposePie()
  pieZoomChart?.dispose()
  pieZoomChart = null
})
</script>

<style scoped>
.dashboard-compact.dashboard-full {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0 4px 12px;
  color: #e2e8f0;
  min-height: calc(100vh - 64px - 48px);
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-sizing: border-box;
}

.dashboard-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel {
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.88) 100%);
  border: 1px solid rgba(94, 234, 212, 0.14);
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(2, 6, 23, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(10px);
}

.float-in {
  animation: float-in 0.5s ease-out both;
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

.screen-header {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.screen-header h2 {
  margin: 4px 0 0;
  color: #f8fafc;
  font-size: 30px;
  letter-spacing: 1px;
  font-weight: 700;
}

.sub-title {
  margin: 0;
  color: #5eead4;
  font-size: 16px;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time {
  color: #94a3b8;
  font-size: 16px;
  font-variant-numeric: tabular-nums;
}

.refresh-btn {
  background: linear-gradient(135deg, #0d9488, #14b8a6) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(13, 148, 136, 0.35);
}

/* 指标区：与下方智能体区等高 */
.metric-row {
  margin-top: 0;
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) minmax(280px, 1.35fr);
  gap: 16px;
  align-items: stretch;
}

.metric-row > .metric-card-sm,
.metric-row > .emotion-panel {
  height: 100%;
  min-height: 0;
}

.metric-card-sm {
  padding: 16px 18px 14px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.metric-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  margin-bottom: 10px;
  color: #fff;
}

.icon-a {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}
.icon-b {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.35);
}
.icon-c {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  box-shadow: 0 4px 12px rgba(168, 85, 247, 0.35);
}
.icon-d {
  background: linear-gradient(135deg, #14b8a6, #0d9488);
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.4);
}

.metric-card-sm p {
  margin: 0;
  color: #94a3b8;
  font-size: 16px;
}
.metric-card-sm h3 {
  margin: 8px 0 6px;
  color: #f1f5f9;
  font-size: 32px;
  font-weight: 700;
}
.metric-card-sm span {
  color: #64748b;
  font-size: 15px;
  line-height: 1.45;
  display: block;
}

.emotion-panel {
  padding: 16px 18px 14px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.emotion-panel-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: start;
  flex: 1;
  min-height: 0;
}

.emotion-metric {
  align-self: start;
  text-align: left;
}

.emotion-metric p {
  margin: 0;
  color: #94a3b8;
  font-size: 16px;
}
.emotion-metric h3 {
  margin: 8px 0 6px;
  color: #f1f5f9;
  font-size: 32px;
  font-weight: 700;
}
.emotion-metric span {
  color: #64748b;
  font-size: 15px;
  line-height: 1.45;
}

.emotion-pie-wrap {
  position: relative;
  min-height: 0;
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
}
.pie-chart-host {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: clamp(120px, 20vh, 220px);
  justify-content: flex-start;
}
.pie-canvas {
  width: 100%;
  height: clamp(120px, 20vh, 220px);
  min-height: 120px;
}
.pie-center-hint {
  margin: 4px 0 0;
  text-align: center;
  font-size: 12px;
  color: #64748b;
  line-height: 1.2;
  pointer-events: none;
  flex-shrink: 0;
}
.pie-empty {
  min-height: clamp(130px, 18vh, 200px);
  display: grid;
  place-items: center;
  color: #64748b;
  font-size: 15px;
  background: rgba(15, 23, 42, 0.35);
  border-radius: 10px;
}

/* 智能体矩阵：与指标区同高、单行四列 */
.agents-section {
  margin-top: 0;
  padding: 16px 18px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.agents-head {
  margin-bottom: 14px;
  flex-shrink: 0;
}
.agents-head h4 {
  margin: 0 0 6px;
  color: #f1f5f9;
  font-size: 22px;
  font-weight: 600;
}
.agents-sub {
  font-size: 15px;
  color: #64748b;
  line-height: 1.45;
}

.agents-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  flex: 1;
  min-height: 0;
  align-items: stretch;
  align-content: stretch;
}

.agent-tile {
  border-radius: 14px;
  cursor: default;
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease;
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.55) 0%, rgba(15, 23, 42, 0.72) 100%);
  border: 1px solid rgba(94, 234, 212, 0.14);
  box-shadow: 0 8px 28px rgba(2, 6, 23, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.03);
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.agent-tile:hover {
  transform: translateY(-5px);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.65), 0 0 0 1px rgba(94, 234, 212, 0.22);
  z-index: 2;
}
.agent-tile.is-disabled {
  opacity: 0.55;
}

.agent-tile-inner {
  padding: 14px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-height: 0;
}

.agent-tile-inner.has-keywords {
  min-height: 0;
}

.agent-tile-inner.is-ml {
  min-height: 0;
}

.agent-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  align-self: flex-start;
  flex-shrink: 0;
}
.tone-0 {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}
.tone-1 {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
}
.tone-2 {
  background: linear-gradient(135deg, #f97316, #ea580c);
}
.tone-3 {
  background: linear-gradient(135deg, #14b8a6, #0d9488);
}
.tone-4 {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
}
.tone-5 {
  background: linear-gradient(135deg, #ec4899, #db2777);
}

.agent-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-self: stretch;
  text-align: left;
}
.agent-name {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  line-height: 1.3;
}
.agent-role {
  font-size: 15px;
  color: #94a3b8;
  line-height: 1.35;
}
.agent-url-hint {
  font-size: 13px;
  color: #64748b;
  font-family: ui-monospace, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 6px;
  margin-top: 4px;
  max-height: 52px;
  overflow: hidden;
}
.kw-chip {
  font-size: 12px;
  line-height: 1.25;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(94, 234, 212, 0.12);
  color: #99f6e4;
  border: 1px solid rgba(94, 234, 212, 0.22);
}
.kw-empty {
  font-size: 12px;
  color: #64748b;
  line-height: 1.35;
}

.crawler-kw-row {
  margin-top: 6px;
  padding-top: 8px;
  border-top: 1px solid rgba(94, 234, 212, 0.1);
}
.kw-load-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  justify-content: center;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.4);
  background: linear-gradient(145deg, rgba(30, 64, 175, 0.55), rgba(15, 23, 42, 0.88));
  color: #e2e8f0;
  font-size: 15px;
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s,
    transform 0.15s;
}
.kw-load-btn:hover {
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.45), rgba(15, 23, 42, 0.92));
  border-color: rgba(96, 165, 250, 0.5);
}
.kw-load-count {
  font-family: ui-monospace, monospace;
  color: #5eead4;
  font-weight: 600;
}

.agent-detail-block {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.agent-detail-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  font-size: 14px;
  line-height: 1.4;
}
.detail-label {
  color: #cbd5e1;
  font-weight: 600;
  min-width: 5em;
  flex-shrink: 0;
}
.mini-text-btn {
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background: #2563eb;
  color: #eff6ff;
  font-size: 13px;
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s;
}
.mini-text-btn:hover {
  background: #1d4ed8;
  border-color: #1d4ed8;
}
.mini-link-btn {
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: #38bdf8;
  font-size: 13px;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.mini-link-btn:hover {
  color: #7dd3fc;
}

.mcp-sub {
  margin-top: auto;
  padding-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.mcp-pill {
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  background: rgba(99, 102, 241, 0.2);
  border: 1px solid rgba(129, 140, 248, 0.45);
  color: #c7d2fe;
}
.disease-pill-btn {
  cursor: pointer;
  width: 100%;
  text-align: center;
}
.mcp-pill.is-muted {
  opacity: 0.75;
  background: rgba(15, 23, 42, 0.6);
  border-color: rgba(94, 234, 212, 0.15);
  color: #94a3b8;
  font-weight: 500;
}

.agent-tile-inner.is-chat,
.agent-tile-inner.is-ml {
  justify-content: flex-start;
}
.agent-tile-inner.is-mcp {
  justify-content: flex-start;
}
.agent-tile-inner.is-toutiao {
  justify-content: flex-start;
}

/* 头条：关键词区沉底，标题与图标仍在左上 */
.agent-tile-inner.is-toutiao .crawler-kw-row {
  margin-top: auto;
  padding-top: 10px;
}

.prompt-editor-wrap {
  min-height: 120px;
}
.prompt-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
}
.prompt-textarea :deep(.el-textarea__inner) {
  font-family: ui-monospace, system-ui, sans-serif;
  font-size: 13px;
  line-height: 1.55;
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.65) 0%, rgba(15, 23, 42, 0.95) 100%);
  color: #e2e8f0;
  border-color: rgba(94, 234, 212, 0.22);
}
.prompt-textarea :deep(.el-textarea__inner::placeholder) {
  color: #64748b;
}

.train-img-wrap {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.train-img {
  max-width: 100%;
  max-height: 72vh;
  border-radius: 10px;
  border: 1px solid rgba(94, 234, 212, 0.15);
}
.train-img-err {
  color: #fb923c;
  font-size: 14px;
  text-align: center;
  padding: 24px;
  line-height: 1.5;
}

/* 弹窗内表格（深色） */
.sn-table.sn-table-dark :deep(.el-table),
.sn-table-dark :deep(.el-table) {
  --el-table-border-color: rgba(94, 234, 212, 0.14);
  --el-table-header-bg-color: rgba(15, 23, 42, 0.95);
  --el-table-bg-color: rgba(15, 23, 42, 0.55);
  --el-table-tr-bg-color: rgba(30, 41, 59, 0.45);
  --el-table-row-hover-bg-color: rgba(45, 212, 191, 0.08);
  color: #e2e8f0;
  font-size: 14px;
}
.sn-table-dark :deep(.el-table th.el-table__cell) {
  color: #cbd5e1;
  font-weight: 600;
}
.sn-table-dark :deep(.el-table td.el-table__cell) {
  color: #e2e8f0;
  border-color: rgba(94, 234, 212, 0.1);
}
.sn-table-dark :deep(.el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(15, 23, 42, 0.35) !important;
}
.sn-table-dark :deep(.el-table__empty-block) {
  background: transparent;
}
.sn-table-dark :deep(.el-loading-mask) {
  background: rgba(15, 23, 42, 0.65);
}

@media (max-width: 1100px) {
  .metric-row {
    grid-template-columns: 1fr 1fr;
  }
  .emotion-panel {
    grid-column: 1 / -1;
  }
  .agents-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metric-row {
    grid-template-columns: 1fr;
  }
  .agents-grid {
    grid-template-columns: 1fr;
  }
  .emotion-panel-inner {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* append-to-body 弹窗需非 scoped */
.dashboard-kw-crud-dialog,
.dashboard-kw-form-dialog {
  max-width: 720px;
}
.dashboard-kw-crud-dialog.el-dialog,
.dashboard-kw-form-dialog.el-dialog {
  background: linear-gradient(160deg, rgba(30, 41, 59, 0.98) 0%, rgba(15, 23, 42, 0.99) 100%);
  border: 1px solid rgba(94, 234, 212, 0.18);
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(2, 6, 23, 0.75);
}
.dashboard-kw-crud-dialog .el-dialog__header,
.dashboard-kw-form-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(94, 234, 212, 0.12);
  margin-right: 0;
  padding-bottom: 14px;
}
.dashboard-kw-crud-dialog .el-dialog__title,
.dashboard-kw-form-dialog .el-dialog__title {
  color: #f1f5f9;
  font-size: 18px;
  font-weight: 600;
}
.dashboard-kw-crud-dialog .el-dialog__body,
.dashboard-kw-form-dialog .el-dialog__body {
  color: #e2e8f0;
  padding-top: 8px;
}
.dashboard-kw-form-dialog .el-dialog__footer {
  border-top: 1px solid rgba(94, 234, 212, 0.1);
}

/* 提示词 / 表单内多行框：深蓝底，与驾驶舱一致 */
.dashboard-kw-form-dialog .el-textarea__inner {
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.65) 0%, rgba(15, 23, 42, 0.95) 100%) !important;
  color: #e2e8f0 !important;
  border: 1px solid rgba(94, 234, 212, 0.22) !important;
  border-radius: 10px !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
.dashboard-kw-form-dialog .el-textarea__inner:hover {
  border-color: rgba(94, 234, 212, 0.35) !important;
}
.dashboard-kw-form-dialog .el-textarea__inner:focus {
  border-color: rgba(45, 212, 191, 0.5) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 0 0 1px rgba(45, 212, 191, 0.12);
}
.dashboard-kw-form-dialog .el-textarea__inner::placeholder {
  color: #64748b !important;
}

/* 新增关键词弹窗：输入区改蓝色底 */
.dashboard-kw-form-dialog .el-input__wrapper,
.dashboard-kw-form-dialog .el-input-number .el-input__wrapper,
.dashboard-kw-form-dialog .el-input-number__decrease,
.dashboard-kw-form-dialog .el-input-number__increase {
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.28) 0%, rgba(30, 64, 175, 0.45) 100%) !important;
  border-color: rgba(96, 165, 250, 0.45) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}
.dashboard-kw-form-dialog .el-input__inner,
.dashboard-kw-form-dialog .el-input-number .el-input__inner {
  color: #eaf2ff !important;
}
.dashboard-kw-form-dialog .el-input__inner::placeholder {
  color: rgba(191, 219, 254, 0.88) !important;
}

.kw-panel-tip {
  margin: 0 0 12px;
  font-size: 14px;
  color: #94a3b8;
  line-height: 1.45;
}
.kw-tooltip-dark.el-popper.is-dark,
.kw-tooltip-dark.el-popper {
  max-width: 300px !important;
  background: rgba(15, 23, 42, 0.96) !important;
  border: 1px solid rgba(94, 234, 212, 0.22) !important;
  color: #e2e8f0 !important;
  box-shadow: 0 12px 40px rgba(2, 6, 23, 0.55);
}
.kw-tooltip-dark .el-popper__arrow::before {
  background: rgba(15, 23, 42, 0.96) !important;
  border: 1px solid rgba(94, 234, 212, 0.22) !important;
}
.kw-tooltip-empty {
  font-size: 12px;
  color: #94a3b8;
}
.kw-tooltip-inner {
  max-height: 220px;
  overflow-y: auto;
  font-size: 12px;
  line-height: 1.5;
}
.kw-tooltip-line .is-off {
  opacity: 0.45;
  text-decoration: line-through;
}
.kw-tooltip-more {
  margin-top: 6px;
  color: #64748b;
  font-size: 11px;
}

.train-img-dialog.el-dialog {
  max-width: 960px;
}

.dashboard-disease-dialog.el-dialog {
  max-width: 980px;
  background: linear-gradient(165deg, #1e3a5f 0%, #1d4ed8 35%, #172554 100%);
  border: 1px solid rgba(147, 197, 253, 0.45);
}
.dashboard-disease-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding-bottom: 12px;
}
.dashboard-disease-dialog .el-dialog__title {
  color: #f8fafc;
  font-weight: 600;
  font-size: 17px;
}
.dashboard-disease-dialog .el-dialog__body {
  background: rgba(248, 250, 252, 0.08);
  padding-top: 12px;
}
.dashboard-disease-dialog .disease-predict-table {
  --el-table-border-color: rgba(30, 64, 175, 0.35);
  --el-table-bg-color: #eff6ff;
  --el-table-tr-bg-color: #ffffff;
  --el-table-header-bg-color: #1d4ed8;
}
.dashboard-disease-dialog .disease-predict-table.el-table,
.dashboard-disease-dialog .disease-predict-table .el-table__expanded-cell,
.dashboard-disease-dialog .disease-predict-table .el-table__body-wrapper,
.dashboard-disease-dialog .disease-predict-table .el-scrollbar__view {
  background-color: #f8fafc !important;
}
.dashboard-disease-dialog .disease-predict-table tr,
.dashboard-disease-dialog .disease-predict-table td.el-table__cell {
  background-color: #ffffff !important;
}
.dashboard-disease-dialog .disease-predict-table .el-table th.el-table__cell {
  background: linear-gradient(180deg, #2563eb 0%, #1d4ed8 100%) !important;
  color: #f8fafc !important;
  font-weight: 600;
  border-color: rgba(255, 255, 255, 0.25) !important;
}
.dashboard-disease-dialog .disease-predict-table .el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background-color: #e0f2fe !important;
}
.dashboard-disease-dialog .disease-predict-table .el-table td.el-table__cell,
.dashboard-disease-dialog .disease-predict-table .el-table .cell,
.dashboard-disease-dialog .disease-predict-table .el-table__empty-text {
  color: #0f172a !important;
  font-size: 13px;
  font-weight: 500;
}
.dashboard-disease-dialog .disease-predict-table .el-table__body tr:hover > td.el-table__cell {
  background-color: #bfdbfe !important;
}
.disease-toolbar {
  margin-bottom: 10px;
}
.dashboard-disease-dialog .kw-toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pie-zoom-dialog.el-dialog {
  max-width: 720px;
}
.pie-zoom-tip {
  margin: 0 0 10px;
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.45;
}
.pie-zoom-canvas {
  width: 100%;
  height: min(52vh, 440px);
  min-height: 300px;
}

/* 头条关键词弹窗已复用 .dashboard-disease-dialog + .disease-predict-table，不再单独写深色表样式 */
</style>
