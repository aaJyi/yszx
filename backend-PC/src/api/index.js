import request from './request'

// 登录（与小程序共用后端）
export function login(data) {
  return request.post('/t-user/login', data)
}

// 注册
export function register(data) {
  return request.post('/t-user/register', data)
}

/** 管理端控制台：四表汇总 + 情绪区间占比 */
export function getAdminDashboardOverview() {
  return request.get('/admin/dashboard/overview')
}

/** 管理端：用户疾病预测（来自 user_inferred_disease） */
export function getAdminInferredDiseases(params) {
  return request.get('/admin/dashboard/inferred-diseases', { params })
}

/** 管理端：用户分页列表 */
export function getAdminUserPage(params) {
  return request.get('/admin/users/page', { params })
}

/** 管理端：更新用户资料（昵称、手机、性别、状态等） */
export function putAdminUser(userId, data) {
  return request.put(`/admin/users/${userId}`, data)
}

/** 管理端：健康数据汇总（各类型最近一条） */
export function getAdminHealthOverview(params) {
  return request.get('/admin/health-data/overview', { params })
}

/** 管理端：某用户某类型的全部健康原始记录（元信息） */
export function getAdminHealthRecords(params) {
  return request.get('/admin/health-data/records', { params })
}

/** 管理端：家人关系图谱（力导向图节点与边） */
export function getAdminFamilyGraph() {
  return request.get('/admin/family-graph')
}

/**
 * 病机图谱：按器官 key 查询疾病详述与治疗措施（后端 organ_disease_detail）
 * @param {string} organKey 如 brain、liver、lymph_nodes
 */
export function getAdminOrganDiseases(organKey) {
  return request.get('/admin/organ-diseases', { params: { organKey } })
}

/** 管理端：健康资讯知识库（定时爬虫入库，分页） */
export function getAdminKnowledgeArticles(params) {
  return request.get('/admin/knowledge-articles/page', { params })
}

/** 管理端：RAG 向量库对应数据集（与 ai-doctor-rag 分表一致） */
export function getAdminRagKbQaPage(params) {
  return request.get('/admin/rag-kb/qa/page', { params })
}

export function getAdminRagKbLiverCancerPage(params) {
  return request.get('/admin/rag-kb/liver-cancer/page', { params })
}

export function getAdminRagKbLlamaPage(params) {
  return request.get('/admin/rag-kb/llama/page', { params })
}

export function getAdminRagKbDrugPage(params) {
  return request.get('/admin/rag-kb/drug/page', { params })
}

export function getAdminRagKbEnglishPage(params) {
  return request.get('/admin/rag-kb/english/page', { params })
}

/** 管理端：从 MySQL 重建当前数据集对应的 Chroma 向量（异步） */
export function postAdminRagKbRebuildVector(data) {
  return request.post('/admin/rag-kb/rebuild-vector', data)
}

export function postAdminRagKbQa(data) {
  return request.post('/admin/rag-kb/qa', data)
}
export function putAdminRagKbQa(id, data) {
  return request.put(`/admin/rag-kb/qa/${id}`, data)
}
export function deleteAdminRagKbQa(id) {
  return request.delete(`/admin/rag-kb/qa/${id}`)
}

export function postAdminRagKbLiverCancer(data) {
  return request.post('/admin/rag-kb/liver-cancer', data)
}
export function putAdminRagKbLiverCancer(id, data) {
  return request.put(`/admin/rag-kb/liver-cancer/${id}`, data)
}
export function deleteAdminRagKbLiverCancer(id) {
  return request.delete(`/admin/rag-kb/liver-cancer/${id}`)
}

export function postAdminRagKbLlama(data) {
  return request.post('/admin/rag-kb/llama', data)
}
export function putAdminRagKbLlama(id, data) {
  return request.put(`/admin/rag-kb/llama/${id}`, data)
}
export function deleteAdminRagKbLlama(id) {
  return request.delete(`/admin/rag-kb/llama/${id}`)
}

export function postAdminRagKbDrug(data) {
  return request.post('/admin/rag-kb/drug', data)
}
export function putAdminRagKbDrug(id, data) {
  return request.put(`/admin/rag-kb/drug/${id}`, data)
}
export function deleteAdminRagKbDrug(id) {
  return request.delete(`/admin/rag-kb/drug/${id}`)
}

export function postAdminRagKbEnglish(data) {
  return request.post('/admin/rag-kb/english', data)
}
export function putAdminRagKbEnglish(id, data) {
  return request.put(`/admin/rag-kb/english/${id}`, data)
}
export function deleteAdminRagKbEnglish(id) {
  return request.delete(`/admin/rag-kb/english/${id}`)
}

/** 智能导航：爬取种子关键词 CRUD */
export function getAdminCrawlKeywords() {
  return request.get('/admin/crawl-keywords')
}

export function postAdminCrawlKeyword(data) {
  return request.post('/admin/crawl-keywords', data)
}

export function putAdminCrawlKeyword(id, data) {
  return request.put(`/admin/crawl-keywords/${id}`, data)
}

export function deleteAdminCrawlKeyword(id) {
  return request.delete(`/admin/crawl-keywords/${id}`)
}

/** 工作台：训练结果图（后端映射本地 PNG） */
export function getDashboardDevImageUrl(name) {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${base}/admin/dashboard/dev/images/${name}`
}

/** 工作台：提示词读写（chat / emotion） */
export function getDashboardDevPrompt(slot) {
  return request.get(`/admin/dashboard/dev/prompts/${slot}`).then((body) => body?.data ?? '')
}

export function putDashboardDevPrompt(slot, text) {
  return request.put(`/admin/dashboard/dev/prompts/${slot}`, text ?? '', {
    headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
  })
}

/** NCSS 社团图谱：最新快照 + 力导向图数据 */
export function getAdminClubDivisionLatest() {
  return request.get('/admin/club-division/latest')
}

/** 社团成员明细（默认最新快照） */
export function getAdminClubDivisionMembers(params) {
  return request.get('/admin/club-division/members', { params })
}

/** 触发 NCSS 重算（可能较慢） */
export function postAdminClubDivisionRecompute(body) {
  // 大样本（如 600+ 用户）计算与图片导出可能超过 3 分钟
  return request.post('/admin/club-division/recompute', body || {}, { timeout: 900000 })
}

/** NCSS matplotlib 导出图片清单（dashboard + expansion 帧） */
export function getAdminClubDivisionImgManifest() {
  return request.get('/admin/club-division/img-manifest')
}

/** 社团推荐预览（管理端） */
export function getAdminClubDivisionRecommendations(params) {
  return request.get('/admin/club-division/recommendations', { params })
}
