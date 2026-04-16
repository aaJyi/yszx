<template>
  <div class="login-page">
    <div class="login-bg-decoration" aria-hidden="true" />
    <div class="login-card float-up">
      <div class="logo">
        <img class="logo-badge" :src="APP_LOGO_URL" alt="" />
        <h1>医视智行</h1>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="form" @submit.prevent="onSubmit">
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            size="large"
            maxlength="11"
            clearable
          >
            <template #prefix>
              <el-icon><Iphone /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            clearable
            @keyup.enter="onSubmit"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="onSubmit">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api'
import { useUserStore } from '@/stores/user'
import { APP_LOGO_URL } from '@/constants/brand'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  phone: '',
  password: '',
})
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { len: 11, message: '手机号为11位', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await login({
      phone: form.phone,
      password: form.password,
    })
    const info = res.data || res
    userStore.setLogin({
      token: info.token,
      userId: info.userId,
      nickname: info.nickname || form.phone,
    })
    ElMessage.success('登录成功')
    router.replace('/')
  } catch (e) {
    let msg = '登录失败'
    if (!e.response) {
      msg =
        '网络异常，请检查后端服务是否启动、地址是否正确（当前请求：' +
        (import.meta.env.VITE_API_BASE_URL || '/api') +
        '）'
    } else if (e.response?.data?.message) {
      msg = e.response.data.message
    } else if (e.message && e.message !== '请求失败') {
      msg = e.message
    }
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: radial-gradient(ellipse 120% 80% at 50% -20%, rgba(45, 212, 191, 0.25), transparent),
    linear-gradient(165deg, #0b1220 0%, #0f172a 45%, #134e4a 100%);
}

.login-bg-decoration {
  position: absolute;
  inset: -40%;
  background: radial-gradient(circle at 30% 40%, rgba(99, 102, 241, 0.15), transparent 45%),
    radial-gradient(circle at 70% 60%, rgba(13, 148, 136, 0.2), transparent 40%);
  animation: login-pulse 8s ease-in-out infinite alternate;
  pointer-events: none;
}

@keyframes login-pulse {
  from {
    transform: scale(1) rotate(0deg);
    opacity: 0.9;
  }
  to {
    transform: scale(1.05) rotate(2deg);
    opacity: 1;
  }
}

.login-card {
  width: min(420px, 100%);
  position: relative;
  z-index: 1;
  background: rgba(30, 41, 59, 0.72);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(94, 234, 212, 0.18);
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  padding: 32px 28px 28px;
  overflow: hidden;
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #14b8a6, #6366f1, #a855f7);
  opacity: 0.95;
}

.login-card::after {
  content: '';
  position: absolute;
  top: -30%;
  left: -50%;
  width: 45%;
  height: 160%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  transform: skewX(-14deg);
  animation: card-shimmer 5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes card-shimmer {
  0% {
    transform: translateX(-40%) skewX(-14deg);
    opacity: 0;
  }
  25% {
    opacity: 1;
  }
  100% {
    transform: translateX(220%) skewX(-14deg);
    opacity: 0;
  }
}

.float-up {
  animation: float-up 0.6s ease-out;
}

@keyframes float-up {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.logo-badge {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: #fff;
  object-fit: contain;
  display: block;
  box-shadow: 0 10px 28px rgba(13, 148, 136, 0.45);
  margin-bottom: 4px;
}

.logo h1 {
  margin: 0;
  font-size: 24px;
  letter-spacing: 6px;
  color: #f8fafc;
  font-weight: 800;
}

.logo p {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 0.5px;
  text-align: center;
  line-height: 1.5;
}

.form :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.6);
  box-shadow: 0 0 0 1px rgba(71, 85, 105, 0.5);
}

.form :deep(.el-input__inner) {
  color: #e2e8f0;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #0d9488, #14b8a6) !important;
  border: none !important;
  box-shadow: 0 8px 24px rgba(13, 148, 136, 0.4);
}

.submit-btn:hover {
  filter: brightness(1.06);
}
</style>
