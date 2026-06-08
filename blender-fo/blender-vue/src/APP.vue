<template>
  <!-- 独立的触发区域，始终存在 -->
  <div class="global-trigger" @mouseenter="showSidebar"></div>

  <!-- 互斥显示侧边栏 -->
  <NavBar v-if="fullVisible" />
  <NavBarLitter v-else />

  <router-view />
</template>

<script setup>
import { computed } from 'vue'
import NavBar from './components/NavBar.vue'
import NavBarLitter from './components/NavBar-litter.vue'
import { useSidebar } from './composables/useSidebar'

const { isVisible, showSidebar } = useSidebar()
const fullVisible = computed(() => isVisible.value)
</script>

<style>
/* 全局触发区域样式 */
.global-trigger {
  position: fixed;
  left: 0;
  top: 0;
  width: 10%;
  height: 100vh;
  z-index: 1000;
  background: transparent;
  cursor: default;
}

/* 其他全局样式保持不变 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background-color: #0a0a0f;
  overflow-x: hidden;
}
</style>