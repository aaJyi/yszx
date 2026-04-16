<template>
  <div class="page-data">
    <header class="page-head panel float-in">
      <div>
        <p class="sub">医视智行 · 用户中心</p>
        <h2>用户信息</h2>
      </div>
    </header>

    <section class="panel content-panel float-in">
      <div class="toolbar">
        <el-button type="primary" round :loading="loading" @click="loadUsers">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe class="data-table" empty-text="暂无用户数据">
        <el-table-column type="index" label="序号" width="64" :index="indexMethod" />
        <el-table-column label="openid" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="openid">{{ row.openid || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="昵称" min-width="120">
          <template #default="{ row }">
            <span>{{ row.nickname || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="头像" width="100">
          <template #default="{ row }">
            <el-avatar v-if="row.avatarUrl" :size="40" :src="row.avatarUrl" fit="cover" />
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="密码" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="pwd-hint">{{ row.passwordHint }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="genderText" label="性别" width="88" />
        <el-table-column prop="statusText" label="状态" width="100" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="success" link @click="goHealthData(row)">健康数据</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadUsers"
          @size-change="loadUsers"
        />
      </div>
    </section>

    <el-dialog
      v-model="editVisible"
      title="编辑用户"
      width="480px"
      class="user-edit-dialog"
      append-to-body
      destroy-on-close
      @closed="editRow = null"
    >
      <el-form v-if="editForm" :model="editForm" label-width="88px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="64" show-word-limit placeholder="昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="editForm.gender" placeholder="选择" style="width: 100%">
            <el-option :value="0" label="未知" />
            <el-option :value="1" label="男" />
            <el-option :value="2" label="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminUserPage, putAdminUser } from '@/api'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const editVisible = ref(false)
const editRow = ref(null)
const editForm = ref(null)
const saving = ref(false)

function indexMethod(index) {
  return (pageNum.value - 1) * pageSize.value + index + 1
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getAdminUserPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    const data = res?.data ?? res
    rows.value = data?.records ?? []
    total.value = data?.total ?? 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  editRow.value = row
  editForm.value = {
    nickname: row.nickname || '',
    phone: row.phone || '',
    gender: row.gender != null ? Number(row.gender) : 0,
    status: row.status != null ? Number(row.status) : 1,
  }
  editVisible.value = true
}

async function submitEdit() {
  const row = editRow.value
  if (!row?.userId || !editForm.value) return
  saving.value = true
  try {
    await putAdminUser(row.userId, {
      nickname: editForm.value.nickname,
      phone: editForm.value.phone,
      gender: editForm.value.gender,
      status: editForm.value.status,
    })
    ElMessage.success('已保存')
    editVisible.value = false
    await loadUsers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function goHealthData(row) {
  if (!row?.userId) return
  router.push({ path: '/health-data', query: { userId: String(row.userId) } })
}

onMounted(() => loadUsers())
</script>

<style scoped>
.page-data {
  max-width: 1600px;
  margin: 0 auto;
}

.page-head {
  padding: 18px 20px;
  margin-bottom: 18px;
}

.page-head h2 {
  margin: 6px 0 0;
  color: #f8fafc;
  font-size: 22px;
  letter-spacing: 1px;
}

.sub {
  margin: 0;
  color: #5eead4;
  font-size: 13px;
}

.desc {
  margin: 12px 0 0;
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.65;
  max-width: 900px;
}

.desc code {
  font-size: 12px;
  color: #99f6e4;
  background: rgba(15, 23, 42, 0.6);
  padding: 2px 6px;
  border-radius: 4px;
}

.panel {
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.88) 100%);
  border: 1px solid rgba(94, 234, 212, 0.14);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(2, 6, 23, 0.35);
}

.content-panel {
  padding: 16px 18px 20px;
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

.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.data-table {
  --el-table-bg-color: rgba(15, 23, 42, 0.35);
  --el-table-tr-bg-color: rgba(15, 23, 42, 0.25);
  --el-table-header-bg-color: rgba(13, 148, 136, 0.2);
  --el-table-border-color: rgba(71, 85, 105, 0.45);
  --el-table-text-color: #e2e8f0;
  --el-table-header-text-color: #f1f5f9;
  /* 默认 hover 为浅色，深色主题下会看不清文字 */
  --el-table-row-hover-bg-color: rgba(45, 212, 191, 0.12);
  --el-table-current-row-bg-color: rgba(45, 212, 191, 0.18);
}

.data-table :deep(.el-table__body tr.el-table__row--striped > td.el-table__cell) {
  background-color: rgba(30, 41, 59, 0.45);
}

.data-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.openid {
  font-size: 12px;
  color: #bae6fd;
  word-break: break-all;
}

.pwd-hint {
  font-size: 12px;
  color: #a5b4fc;
}

.muted {
  color: #64748b;
}
</style>

<style lang="scss">
.user-edit-dialog.el-dialog {
  background: linear-gradient(165deg, #1d4ed8 0%, #2563eb 42%, #3b82f6 100%);
  border: 1px solid rgba(147, 197, 253, 0.55);
  border-radius: 14px;
  box-shadow: 0 22px 55px rgba(37, 99, 235, 0.4);
}
.user-edit-dialog .el-dialog__header {
  padding: 16px 20px 12px;
  margin-right: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.22);
}
.user-edit-dialog .el-dialog__title {
  color: #f8fafc;
  font-weight: 600;
  font-size: 17px;
}
.user-edit-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #e0e7ff;
}
.user-edit-dialog .el-dialog__body {
  padding: 18px 20px 8px;
  background: rgba(15, 23, 42, 0.12);
}
.user-edit-dialog .el-dialog__footer {
  padding: 12px 20px 18px;
  background: rgba(15, 23, 42, 0.12);
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}
.user-edit-dialog .el-form-item__label {
  color: #e0e7ff;
}
.user-edit-dialog .el-input__wrapper,
.user-edit-dialog .el-select .el-input__wrapper {
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.35) inset;
}
.user-edit-dialog .el-radio {
  color: #f1f5f9;
}
.user-edit-dialog .el-radio__label {
  color: #f1f5f9;
}
</style>
