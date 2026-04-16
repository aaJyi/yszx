<template>
  <el-container class="layout" direction="vertical">
    <el-header class="top-header" height="64px">
      <div class="brand" @click="router.push('/dashboard')">
        <div class="brand-mark">
          <img class="brand-img" :src="APP_LOGO_URL" alt="" />
        </div>
        <div class="brand-text">
          <span class="brand-name">医视智行</span>
<!--          <span class="brand-sub">机器视觉</span>-->
<!--          <span class="brand-sub">医疗问诊康复辅助诊疗系统</span>-->
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        router
        :ellipsis="false"
        class="top-menu"
        background-color="transparent"
        text-color="rgba(226, 232, 240, 0.92)"
        active-text-color="#5eead4"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户信息</span>
        </el-menu-item>
        <el-menu-item index="/health-data">
          <el-icon><Files /></el-icon>
          <span>健康数据</span>
        </el-menu-item>
        <el-menu-item index="/family-graph">
          <el-icon><Share /></el-icon>
          <span>家人图谱</span>
        </el-menu-item>
        <el-menu-item index="/body-pathology">
          <el-icon><FirstAidKit /></el-icon>
          <span>病机图谱</span>
        </el-menu-item>
        <el-menu-item index="/knowledge-base">
          <el-icon><Reading /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/club-division">
          <el-icon><Histogram /></el-icon>
          <span>社团图谱</span>
        </el-menu-item>
      </el-menu>

      <div class="header-actions">
        <span class="clock">{{ clockText }}</span>
        <el-divider direction="vertical" class="header-divider" />
        <span class="user-name">{{ userStore.nickname || '管理员' }}</span>
        <el-button type="primary" link class="logout-btn" @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-main class="main-area">
      <div class="main-inner animate-fade-in">
        <router-view v-slot="{ Component }">
          <transition name="page-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { Files, FirstAidKit, Histogram, Reading, Share, User } from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { APP_LOGO_URL } from '@/constants/brand'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const clockText = ref('')

let clockTimer = null

function tickClock() {
  clockText.value = new Date().toLocaleString('zh-CN', {
    hour12: false,
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function logout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) window.clearInterval(clockTimer)
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: transparent;
}

.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 0 16px;
  flex-shrink: 0;
  z-index: 50;
  background: linear-gradient(
    105deg,
    rgba(15, 23, 42, 0.97) 0%,
    rgba(30, 41, 59, 0.96) 45%,
    rgba(15, 118, 110, 0.35) 100%
  );
  border-bottom: 1px solid rgba(94, 234, 212, 0.18);
  box-shadow: 0 4px 24px rgba(2, 6, 23, 0.35);
  backdrop-filter: blur(12px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
  padding-right: 12px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #fff;
  display: grid;
  place-items: center;
  overflow: hidden;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12), 0 8px 20px rgba(13, 148, 136, 0.35);
  animation: brand-glow 3s ease-in-out infinite alternate;
}

.brand-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

@keyframes brand-glow {
  from {
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12), 0 8px 20px rgba(13, 148, 136, 0.35);
  }
  to {
    box-shadow: 0 0 0 1px rgba(94, 234, 212, 0.35), 0 10px 28px rgba(45, 212, 191, 0.45);
  }
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name {
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 3px;
  color: #f8fafc;
  line-height: 1.2;
}

.brand-sub {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
  letter-spacing: 1px;
}

.top-menu {
  flex: 1;
  justify-content: flex-end;
  max-width: 900px;
  border-bottom: none !important;
  --el-menu-hover-bg-color: rgba(45, 212, 191, 0.12);
  --el-menu-active-color: #5eead4;
}

.top-menu :deep(.el-menu-item) {
  border-bottom: 2px solid transparent !important;
  border-radius: 8px 8px 0 0;
  margin: 0 4px;
  font-weight: 500;
}

.top-menu :deep(.el-menu-item.is-active) {
  border-bottom-color: #5eead4 !important;
  background: rgba(45, 212, 191, 0.08) !important;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 12px;
}

.clock {
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  color: #94a3b8;
}

.header-divider {
  border-color: rgba(148, 163, 184, 0.35);
  margin: 0 4px;
}

.user-name {
  font-size: 14px;
  color: #e2e8f0;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  color: #f87171 !important;
  font-weight: 500;
}

.main-area {
  padding: 0;
  overflow: auto;
  flex: 1;
}

.main-inner {
  padding: 22px 24px 32px;
  min-height: calc(100vh - 64px);
}

.animate-fade-in {
  animation: fade-in 0.45s ease-out;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-slide-enter-active,
.page-slide-leave-active {
  transition: all 0.28s ease;
}
.page-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
