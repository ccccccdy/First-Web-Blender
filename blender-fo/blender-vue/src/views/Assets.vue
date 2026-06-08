<template>
  <div class="assets-container">
    <video autoplay muted loop playsinline class="bg-video">
      <source src="../assets/video/Miku.mp4" type="video/mp4" />
    </video>

    <div class="content-wrapper">
      <div class="controls">
        <input
          type="text"
          v-model="searchQuery"
          placeholder="搜索资产..."
          class="search-input"
        />
        <div class="sort-buttons">
          <div class="sort-btn-wrapper">
            <button :class="{ active: sortField === 'date' }" @click="toggleSortMenu('date')">按时间排序</button>
            <div v-if="activeMenu === 'date'" class="sort-menu">
              <div @click="setSort('date', 'desc')">最新</div>
              <div @click="setSort('date', 'asc')">最早</div>
            </div>
          </div>
          <div class="sort-btn-wrapper">
            <button :class="{ active: sortField === 'name' }" @click="toggleSortMenu('name')">按名称排序</button>
            <div v-if="activeMenu === 'name'" class="sort-menu">
              <div @click="setSort('name', 'asc')">A 到 Z</div>
              <div @click="setSort('name', 'desc')">Z 到 A</div>
            </div>
          </div>
          <div class="sort-btn-wrapper">
            <button :class="{ active: sortField === 'size' }" @click="toggleSortMenu('size')">按大小排序</button>
            <div v-if="activeMenu === 'size'" class="sort-menu">
              <div @click="setSort('size', 'desc')">从大到小</div>
              <div @click="setSort('size', 'asc')">从小到大</div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="assets-grid">
        <div
          v-for="asset in filteredAndSortedAssets"
          :key="asset.id"
          class="asset-card"
          @click="openDetailModal(asset)"
          @mousemove="handleCardMouseMove($event, asset.id)"
          @mouseleave="handleCardMouseLeave(asset.id)"
        >
          <div class="card-inner" :ref="el => setCardInnerRef(asset.id, el)">
            <div class="asset-preview">
              <div class="file-icon-wrap">
                <svg class="file-icon" aria-hidden="true"><use xlink:href="#icon-wenjianjia" /></svg>
              </div>
            </div>
            <div class="asset-info">
              <h3>{{ asset.title }}</h3>
              <div class="status-chip" :class="getStatusMeta(asset.syncStatus).className">
                {{ getStatusMeta(asset.syncStatus).label }}
              </div>
              <div class="meta">
                <span>{{ formatDate(asset.createdAt) }}</span>
                <span>{{ formatSize(asset.fileSize) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <UploadButton @upload-success="fetchAssets" />
    <AssetDetailModal
      :visible="showModal"
      :asset="currentAsset"
      @close="showModal = false"
      @delete="deleteAsset"
      @update="fetchAssets"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import UploadButton from '../components/UploadButton-Modleing.vue'
import AssetDetailModal from '../components/AssetDetailModal.vue'
import { getStatusMeta } from '../utils/resourceReview'
import { formatResourceSize } from '../utils/sizeFormat'

const assets = ref([])
const searchQuery = ref('')
const sortField = ref('date')
const sortOrder = ref('desc')
const loading = ref(false)
const activeMenu = ref(null)
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}
const formatSize = (sizeMB) => formatResourceSize(sizeMB)

const fetchAssets = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/assets')
    assets.value = res.data
  } catch (err) {
    console.error('获取资产失败', err)
  } finally {
    loading.value = false
  }
}

const toggleSortMenu = (field) => {
  activeMenu.value = activeMenu.value === field ? null : field
}
const setSort = (field, order) => {
  sortField.value = field
  sortOrder.value = order
  activeMenu.value = null
}
const handleClickOutside = (e) => {
  if (!e.target.closest('.sort-btn-wrapper')) {
    activeMenu.value = null
  }
}

const filteredAndSortedAssets = computed(() => {
  let result = [...assets.value]
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(asset =>
      asset.title.toLowerCase().includes(q) ||
      (asset.description && asset.description.toLowerCase().includes(q))
    )
  }
  if (sortField.value === 'date') {
    result.sort((a, b) => sortOrder.value === 'desc'
      ? new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      : new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
  } else if (sortField.value === 'name') {
    result.sort((a, b) => sortOrder.value === 'asc'
      ? a.title.toLowerCase().localeCompare(b.title.toLowerCase())
      : b.title.toLowerCase().localeCompare(a.title.toLowerCase()))
  } else if (sortField.value === 'size') {
    result.sort((a, b) => sortOrder.value === 'desc'
      ? (b.fileSize || 0) - (a.fileSize || 0)
      : (a.fileSize || 0) - (b.fileSize || 0))
  }
  return result
})

const cardInnerRefs = new Map()
const setCardInnerRef = (id, el) => {
  if (el) cardInnerRefs.set(id, el)
}
const handleCardMouseMove = (event, id) => {
  const inner = cardInnerRefs.get(id)
  if (!inner) return
  const rect = event.currentTarget.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const rotateY = ((x - rect.width / 2) / (rect.width / 2)) * 10
  const rotateX = -((y - rect.height / 2) / (rect.height / 2)) * 10
  inner.style.transform = `rotateX(${rotateX}deg) rotateY(${rotateY}deg)`
}
const handleCardMouseLeave = (id) => {
  const inner = cardInnerRefs.get(id)
  if (inner) inner.style.transform = 'rotateX(0deg) rotateY(0deg)'
}

const showModal = ref(false)
const currentAsset = ref(null)
const openDetailModal = (asset) => {
  currentAsset.value = asset
  showModal.value = true
}
const deleteAsset = async (id) => {
  try {
    await axios.delete(`/api/assets/${id}`)
    await fetchAssets()
    showModal.value = false
  } catch (err) {
    console.error('删除失败', err)
    alert('删除失败，请稍后重试')
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  fetchAssets()
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.assets-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow-y: auto;
}
.bg-video {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}
.content-wrapper {
  position: relative;
  margin: 100px 200px 0 300px;
  padding: 20px 0;
  z-index: 10;
  min-height: calc(100vh - 100px);
  user-select: none;
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
  box-shadow: 3px 3px 0 rgb(255, 255, 255);
  transform: translateY(-5px);
}
.search-input:focus {
  box-shadow: 0 0 0 rgb(255, 255, 255);
  transform: translateY(0);
}
.search-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}
.sort-buttons {
  display: flex;
  gap: 15px;
}
.sort-btn-wrapper {
  position: relative;
}
.sort-buttons button {
  border: none;
  padding: 14px 24px;
  border-radius: 40px;
  color: white;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.2);
  box-shadow: 3px 3px 0 rgb(255, 255, 255);
  transform: translateY(-5px);
}
.sort-buttons button.active {
  box-shadow: 0 0 0 rgb(255, 255, 255);
  transform: translateY(0);
}
.sort-menu {
  position: absolute;
  top: calc(100% + 5px);
  left: 0;
  min-width: 150px;
  z-index: 20;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 30px;
  padding: 8px 0;
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.sort-menu div {
  padding: 10px 20px;
  color: white;
  cursor: pointer;
  font-size: 0.9rem;
  border-radius: 30px;
}
.sort-menu div:hover {
  background: rgba(255, 255, 255, 0.2);
}
.assets-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 50px;
}
.asset-card {
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
  width: 100%;
  min-width: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 20px;
  overflow: hidden;
  transition: box-shadow 0.3s ease;
  box-shadow: 8px 8px 2px rgba(0, 0, 0, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transform-style: preserve-3d;
}
.asset-card:hover .card-inner {
  box-shadow: 0 30px 40px -20px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.2);
}
.asset-preview {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.35);
}
.asset-preview img,
.asset-preview video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.file-icon-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.file-icon {
  width: 68px;
  height: 68px;
  fill: #ffaa66;
}
.asset-info {
  flex: 1;
  padding: 16px;
  color: white;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.asset-info h3 {
  font-size: 1.5rem;
  margin-bottom: 8px;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.asset-info p {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12px;
  line-height: 1.4;
  min-height: calc(1.4em * 2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
.loading {
  text-align: center;
  color: white;
  font-size: 1.2rem;
  padding: 50px;
}
</style>
