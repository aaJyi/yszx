<template>
  <div class="app-shell">
    <div class="bg-gradient" aria-hidden="true" />
    <div class="bg-grid" aria-hidden="true" />
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
:root {
  --ys-primary: #0d9488;
  --ys-primary-light: #14b8a6;
  --ys-bg-deep: #0f172a;
  --ys-text: #e2e8f0;
}

#app {
  font-family: 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
}

html,
body {
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background: #0b1220;
}

.app-shell {
  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
}

.bg-gradient {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: linear-gradient(165deg, #0b1220 0%, #0f172a 38%, #134e4a 100%);
  animation: bg-shift 14s ease-in-out infinite alternate;
}

@keyframes bg-shift {
  0% {
    filter: hue-rotate(0deg) saturate(1);
  }
  100% {
    filter: hue-rotate(-8deg) saturate(1.08);
  }
}

.bg-grid {
  position: fixed;
  inset: 0;
  z-index: 0;
  opacity: 0.22;
  background-image:
    linear-gradient(rgba(94, 234, 212, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(94, 234, 212, 0.07) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 20%, black 20%, transparent 75%);
  pointer-events: none;
  animation: grid-drift 22s linear infinite;
}

@keyframes grid-drift {
  from {
    transform: translate(0, 0);
  }
  to {
    transform: translate(48px, 48px);
  }
}

.app-shell > *:not(.bg-gradient):not(.bg-grid) {
  position: relative;
  z-index: 1;
}
</style>
