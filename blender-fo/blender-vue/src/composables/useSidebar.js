// src/composables/useSidebar.js
import { ref } from 'vue'

const isVisible = ref(false)
let hideTimeout = null
let showTimeout = null

export function useSidebar() {
  const showSidebar = () => {
    if (showTimeout) clearTimeout(showTimeout)
    if (hideTimeout) clearTimeout(hideTimeout)
    showTimeout = setTimeout(() => {
      isVisible.value = true
      showTimeout = null
    }, 50)
  }

  const hideSidebar = () => {
    if (showTimeout) {
      clearTimeout(showTimeout)
      showTimeout = null
    }
    if (isVisible.value) {
      if (hideTimeout) clearTimeout(hideTimeout)
      hideTimeout = setTimeout(() => {
        isVisible.value = false
        hideTimeout = null
      }, 300)
    }
  }

  return {
    isVisible,
    showSidebar,
    hideSidebar
  }
}