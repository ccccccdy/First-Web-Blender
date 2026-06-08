<template>
  <transition name="slide">
    <aside v-if="isVisible" class="sidebar" @mouseleave="hideSidebar">
      <nav class="nav-links">
        <router-link
          v-for="(item, i) in items"
          :key="item.to"
          :to="item.to"
          @click="hideSidebar"
          class="nav-item"
          :style="{ '--i': i }"
        >
          <svg class="nav-icon" aria-hidden="true">
            <use :xlink:href="`#icon-${item.icon}`" />
          </svg>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>
  </transition>

  <transition name="fade">
    <div v-if="isVisible" class="overlay" @click="hideSidebar"></div>
  </transition>
</template>

<script setup>
import { useSidebar } from '../composables/useSidebar'

const { isVisible, hideSidebar } = useSidebar()

const items = [
  { to: '/', label: '首页', icon: 'home' },
  { to: '/modeling', label: '建模', icon: 'shujujianmo' },
  { to: '/assets', label: '资产', icon: 'zichanguanli-zichantaizhang' },
  { to: '/plugins', label: '插件', icon: 'chajian1' },
  { to: '/manage', label: '管理', icon: 'tianchongxing-' }
]
</script>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  width: 600px;
  height: 100vh;
  border-right: 1px solid rgba(255, 255, 255, 0.4);
  display: flex;
  flex-direction: column;
  padding: 2rem 1rem;
  z-index: 300;
  background: rgba(0, 0, 0, 0.7);
  justify-content: center;
}

.slide-enter-active, .slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from, .slide-leave-to {
  transform: translateX(-100%);
}

.slide-enter-to, .slide-leave-from {
  transform: translateX(0);
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.fade-enter-to, .fade-leave-from {
  opacity: 1;
}

.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  background: rgba(0, 0, 0, 0.2);
  z-index: 250;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: 50px;
}

.nav-item {
  color: rgb(255, 255, 255);
  text-decoration: none;
  font-size: 50px;
  font-weight: 500;
  padding: 30px 1.5rem;
  border-radius: 70px;
  background: rgba(255, 255, 255, 0.1);
  text-align: center;
  transition: all 0.2s;
  white-space: nowrap;
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
  animation: pop 0.6s cubic-bezier(0.2, 1, 0.2, 1) forwards;
  animation-delay: calc(100ms + var(--i) * 0.2s);
}

.nav-icon {
  flex-shrink: 0;
  transition: transform 0.2s;
  width: 50px;
  height: 50px;
  transform: translate(-24px, 8px);
}


.nav-item:hover,
.router-link-active {
  background: rgba(255, 255, 255, 0.4);
  box-shadow:
    3px 3px 0 2px rgba(255, 255, 255, 0.7),
    0 8px 20px rgba(0, 0, 0, 0.3);
  transform: translateX(4px);
}

.nav-item:active {
  transform: translateX(4px) scale(0.97);
  box-shadow:
    inset 0 2px 5px rgba(0, 0, 0, 0.2),
    0 1px 2px rgba(255, 255, 255, 0.4);
  transition: all 0.05s ease;
}

@keyframes pop {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.nav-item {
  user-select: none;
  cursor: default;
}
</style>
