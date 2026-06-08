<template>
  <aside class="sidebar-litter" @mouseenter="showSidebar" @mouseleave="hideSidebar">
    <nav class="nav-links-litter">
      <div
        v-for="item in items"
        :key="item.to"
        class="nav-icon-item"
      >
        <svg class="nav-icon" aria-hidden="true">
          <use :xlink:href="`#icon-${item.icon}`" />
        </svg>
      </div>
    </nav>
  </aside>
</template>

<script setup>
import { useSidebar } from '../composables/useSidebar'

const { showSidebar, hideSidebar } = useSidebar()

const items = [
  { to: '/', icon: 'home' },
  { to: '/modeling', icon: 'shujujianmo' },
  { to: '/assets', icon: 'zichanguanli-zichantaizhang' },
  { to: '/plugins', icon: 'chajian1' },
  { to: '/manage', icon: 'tianchongxing-' }
]
</script>

<style scoped>
.sidebar-litter {
  position: fixed;
  left: 0;
  top: 30%;
  width: 100px;
  height: 30vh;
  background: rgba(20, 20, 30, 0);
  border-right: 1px solid rgba(255, 255, 255, 0);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 150;
}

.nav-links-litter {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-icon-item {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 50px;
  height: 50px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.3);
  transition: all 0.2s;
  cursor: default;
}

.nav-icon {
  width: 28px;
  height: 28px;
  fill: rgb(0, 0, 0);
}
</style>
