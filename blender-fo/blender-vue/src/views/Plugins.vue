<template>
  <div class="plugins-container">
    <video autoplay muted loop playsinline class="bg-video">
      <source src="../assets/video/Miku.mp4" type="video/mp4" />
    </video>

    <div class="content-wrapper">
      <div class="controls">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索插件..." 
          class="search-input"
        />
        
        <div class="custom-select" v-click-outside="closeDropdown">
          <div class="select-trigger" @click="toggleDropdown">
            <span>{{ selectedLabel }}</span>
            <svg class="arrow" :class="{ open: dropdownOpen }" viewBox="0 0 24 24" width="16" height="16">
              <polyline points="6 9 12 15 18 9" fill="none" stroke="currentColor" stroke-width="2" />
            </svg>
          </div>
          <div v-if="dropdownOpen" class="select-dropdown">
            <div 
              v-for="option in versionOptions" 
              :key="option.value"
              class="select-option"
              :class="{ active: selectedVersion === option.value }"
              @click="selectOption(option.value)"
            >
              {{ option.label }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="plugins-grid">
        <div 
          v-for="plugin in filteredPlugins" 
          :key="plugin.id" 
          class="plugin-card"
          @click="openDetailModal(plugin)"
        >
          <div class="card-inner">
            <div class="plugin-icon">
              <svg class="folder-icon" aria-hidden="true">
                <use xlink:href="#icon-wenjianjia" />
              </svg>
            </div>
            <div class="plugin-info">
              <h3>{{ plugin.title }}</h3>
              <div class="status-chip" :class="getStatusMeta(plugin.syncStatus).className">
                {{ getStatusMeta(plugin.syncStatus).label }}
              </div>
              <div class="meta">
                <span class="version">适用版本：{{ plugin.blenderVersion }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <UploadButton @upload-success="fetchPlugins" />
    <PluginDetail
      :visible="showDetailModal"
      :plugin="currentPlugin"
      @close="showDetailModal = false"
      @update="fetchPlugins"
      @delete="deletePlugin"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import UploadButton from '../components/UploadButton-Modleing.vue'
import PluginDetail from '../components/PluginDetail.vue'
import { getStatusMeta } from '../utils/resourceReview'

const plugins = ref([])
const searchQuery = ref('')
const selectedVersion = ref('')
const loading = ref(false)
const dropdownOpen = ref(false)
const versionOptions = [
  { value: '', label: '全部' },
  { value: '4.5', label: '4.5' },
  { value: '5.0', label: '5.0' },
  { value: '5.1', label: '5.1' }
]
const selectedLabel = computed(() => {
  const option = versionOptions.find(opt => opt.value === selectedVersion.value)
  return option ? option.label : '全部'
})
const toggleDropdown = () => { dropdownOpen.value = !dropdownOpen.value }
const closeDropdown = () => { dropdownOpen.value = false }
const selectOption = (value) => {
  selectedVersion.value = value
  closeDropdown()
}
const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value()
      }
    }
    document.addEventListener('click', el.clickOutsideEvent)
  },
  unmounted(el) {
    document.removeEventListener('click', el.clickOutsideEvent)
  }
}
const showDetailModal = ref(false)
const currentPlugin = ref(null)

const fetchPlugins = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/plugins', { params: { _t: Date.now() } })
    plugins.value = res.data
  } catch (err) {
    console.error('获取插件失败', err)
  } finally {
    loading.value = false
  }
}
const openDetailModal = (plugin) => {
  currentPlugin.value = plugin
  showDetailModal.value = true
}
const deletePlugin = async (id) => {
  try {
    await axios.delete(`/api/plugins/${id}`)
    await fetchPlugins()
    showDetailModal.value = false
  } catch (err) {
    console.error('删除失败', err)
    alert('删除失败，请稍后重试')
  }
}
const filteredPlugins = computed(() => {
  let result = [...plugins.value]
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(p =>
      p.title.toLowerCase().includes(q) ||
      (p.description && p.description.toLowerCase().includes(q))
    )
  }
  if (selectedVersion.value) {
    result = result.filter(p => p.blenderVersion === selectedVersion.value)
  }
  return result
})

onMounted(() => {
  fetchPlugins()
})
</script>

<style scoped>
.plugins-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow-y: auto;
}
.bg-video {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}
.content-wrapper {
  position: relative;
  margin: 100px 200px 0 300px;
  padding: 20px 0;
  background: transparent;
  z-index: 10;
  min-height: calc(100vh - 100px);
  user-select: none;
  cursor: default;
}
.controls {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  gap: 20px;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 60px;
  padding: 20px 24px;
}
.search-input {
  flex: 2;
  min-width: 200px;
  padding: 16px 20px;
  border: none;
  border-radius: 40px;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  font-size: 1rem;
  outline: none;
  transition: all 0.2s;
  box-shadow: 3px 3px 0px rgb(255, 255, 255);
  transform: translateY(-5px);
}
.search-input:focus {
  box-shadow: 0px 0px 0px rgb(255, 255, 255);
  transform: translateY(0px);
}
.search-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}
.custom-select {
  position: relative;
  min-width: 120px;
}
.select-trigger {
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 3px 3px 0px rgb(255, 255, 255);
  border-radius: 40px;
  padding: 16px 20px;
  color: white;
  font-size: 0.9rem;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  transform: translateY(-5px);
}
.arrow {
  transition: transform 0.2s;
}
.arrow.open {
  transform: rotate(180deg);
}
.select-dropdown {
  position: absolute;
  top: calc(100% + 5px);
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 20;
  overflow: hidden;
}
.select-option {
  padding: 8px 16px;
  color: white;
  cursor: pointer;
  transition: background 0.2s;
}
.select-option:hover,
.select-option.active {
  background: rgba(255, 255, 255, 0.2);
}

.plugins-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 40px;
  align-items: stretch;
}
.plugin-card {
  perspective: 1200px;
  cursor: pointer;
  display: flex;
  width: 100%;
  height: 380px;
  min-width: 0;          
}
.card-inner {
  display: flex;
  flex-direction: column;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 20px;
  overflow: hidden;
  transition: box-shadow 0.3s ease;
  box-shadow: 8px 8px 2px rgba(0, 0, 0, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  width: 100%;
  flex: 1;
  min-width: 0;           
}
.plugin-card:hover .card-inner {
  box-shadow: 0 30px 40px -20px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.2);
}
.plugin-icon {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}
.folder-icon {
  width: 68px;
  height: 68px;
  fill: #ffaa66;
}
.plugin-info {
  flex: 1;
  padding: 16px;
  color: white;
  display: flex;
  flex-direction: column;
  min-width: 0;           
}
.plugin-info h3 {
  font-size: 1.3rem;
  margin-bottom: 8px;
  font-weight: 600;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #c9c9c9;
  height: 1.8em;
}
/* 强制描述文本换行，固定高度 */
.plugin-info p {
  font-size: 0.85rem;
  line-height: 1.4;
  height: 2.8em;           
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 12px;
  overflow: hidden;
  word-break: break-word;   
  white-space: normal;
}
.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 150px;
  min-height: 42px;
  padding: 8px 18px;
  border-radius: 999px;
  margin-bottom: 16px;
  font-size: 1rem;
  font-weight: 700;
}
.status-chip.is-pending {
  background: rgba(255, 193, 7, 0.18);
  color: #ffd970;
}
.status-chip.is-syncing {
  background: rgba(86, 170, 255, 0.18);
  color: #8cc7ff;
}
.status-chip.is-synced {
  background: rgba(80, 200, 120, 0.18);
  color: #8cf1a3;
}
.status-chip.is-failed {
  background: rgba(255, 107, 107, 0.18);
  color: #ffb0b0;
}
.meta {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 10px;
}
.version {
  display: inline-block;
  background: rgba(0,0,0,0.3);
  padding: 4px 10px;
  border-radius: 20px;
}
.loading {
  text-align: center;
  color: white;
  font-size: 1.2rem;
  padding: 50px;
}
</style>
