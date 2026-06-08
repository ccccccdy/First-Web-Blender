<template>
  <div class="modeling-container">
    <!-- 背景视频 -->
    <video autoplay muted loop playsinline class="bg-video">
      <source src="../assets/video/Miku.mp4" type="video/mp4" />
    </video>

    <div class="content-wrapper">
      <!-- 搜索和排序栏 -->
      <div class="controls">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索作品..." 
          class="search-input"
        />
        <div class="sort-buttons">
          <!-- 时间排序按钮 -->
          <div class="sort-btn-wrapper">
            <button 
              :class="{ active: sortField === 'date' }" 
              @click="toggleSortMenu('date')"
            >按时间排序</button>
            <div v-if="activeMenu === 'date'" class="sort-menu">
              <div @click="setSort('date', 'desc')">最近</div>
              <div @click="setSort('date', 'asc')">过去</div>
            </div>
          </div>
          <!-- 名字排序按钮 -->
          <div class="sort-btn-wrapper">
            <button 
              :class="{ active: sortField === 'name' }" 
              @click="toggleSortMenu('name')"
            >按名字排序</button>
            <div v-if="activeMenu === 'name'" class="sort-menu">
              <div @click="setSort('name', 'asc')">从 A 开始</div>
              <div @click="setSort('name', 'desc')">从 Z 返回</div>
            </div>
          </div>
          <!-- 大小排序按钮 -->
          <div class="sort-btn-wrapper">
            <button 
              :class="{ active: sortField === 'size' }" 
              @click="toggleSortMenu('size')"
            >按大小排序</button>
            <div v-if="activeMenu === 'size'" class="sort-menu">
              <div @click="setSort('size', 'desc')">从大到小</div>
              <div @click="setSort('size', 'asc')">从小到大</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 作品网格 -->
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="works-grid">
        <div 
          v-for="work in filteredAndSortedWorks" 
          :key="work.id" 
          class="work-card"
          @click="openDetailModal(work)"
          @mousemove="handleCardMouseMove($event, work.id)"
          @mouseleave="handleCardMouseLeave(work.id)"
        >
          <div class="card-inner" :ref="el => setCardInnerRef(work.id, el)">
            <div class="work-cover">
              <template v-if="coverFileUrls[work.id]">
                <img 
                  v-if="!isVideo(coverFileUrls[work.id])" 
                  :src="coverFileUrls[work.id]" 
                  :alt="work.title" 
                />
                <video 
                  v-else 
                  :src="coverFileUrls[work.id]" 
                  preload="metadata"
                  muted
                  playsinline
                  disablePictureInPicture
                  controlslist="nodownload"
                ></video>
              </template>
            </div>
            <div class="work-info">
              <h3>{{ work.title }}</h3>
              <div class="status-chip" :class="getStatusMeta(work.syncStatus).className">
                {{ getStatusMeta(work.syncStatus).label }}
              </div>
              <div class="meta">
                <span>{{ formatDate(work.createdAt) }}</span>
                <span>{{ formatSize(work.fileSize) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <UploadButton @upload-success="fetchArtworks" />
    <ArtworkDetailModal 
      :visible="showModal" 
      :artwork="currentWork" 
      @close="showModal = false" 
      @delete="deleteArtwork"
      @update="fetchArtworks"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import UploadButton from '../components/UploadButton-Modleing.vue'
import ArtworkDetailModal from '../components/ArtworkDetailModal.vue'
import { getStatusMeta } from '../utils/resourceReview'
import { formatResourceSize } from '../utils/sizeFormat'

// ---------- 数据 ----------
const artworks = ref([])
const searchQuery = ref('')
const sortField = ref('date')
const sortOrder = ref('desc')
const loading = ref(false)
const activeMenu = ref(null)

// 存储每个作品的第一个封面URL（用于显示）
const coverFileUrls = computed(() => {
  const map = {}
  artworks.value.forEach(work => {
    if (work.coverUrls) {
      map[work.id] = work.coverUrls.split(',')[0]
    }
  })
  return map
})

// 判断文件是否为视频
const isVideo = (url) => {
  if (!url) return false
  const videoExtensions = ['.mp4', '.mov', '.avi', '.mkv', '.webm', '.flv', '.wmv']
  const ext = url.substring(url.lastIndexOf('.')).toLowerCase()
  return videoExtensions.includes(ext)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${d.getMonth()+1}-${d.getDate()}`
}
const formatSize = (sizeMB) => formatResourceSize(sizeMB)

// ---------- 后端获取 ----------
const fetchArtworks = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/artworks')
    artworks.value = res.data
  } catch (err) {
    console.error('获取作品失败', err)
  } finally {
    loading.value = false
  }
}

// ---------- 排序与过滤 ----------
const toggleSortMenu = (field) => {
  activeMenu.value = activeMenu.value === field ? null : field
}
const setSort = (field, order) => {
  sortField.value = field
  sortOrder.value = order
  activeMenu.value = null
}

// 点击页面其他地方关闭菜单
const handleClickOutside = (e) => {
  if (!e.target.closest('.sort-btn-wrapper')) {
    activeMenu.value = null
  }
}
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  fetchArtworks()
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 带双向排序的计算属性
const filteredAndSortedWorks = computed(() => {
  let result = [...artworks.value]
  // 搜索过滤
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(w => 
      w.title.toLowerCase().includes(q) || 
      (w.description && w.description.toLowerCase().includes(q))
    )
  }
  // 排序
  if (sortField.value === 'date') {
    result.sort((a, b) => {
      const dateA = new Date(a.createdAt).getTime()
      const dateB = new Date(b.createdAt).getTime()
      return sortOrder.value === 'desc' ? dateB - dateA : dateA - dateB
    })
  } else if (sortField.value === 'name') {
    result.sort((a, b) => {
      const nameA = a.title.toLowerCase()
      const nameB = b.title.toLowerCase()
      return sortOrder.value === 'asc' ? nameA.localeCompare(nameB) : nameB.localeCompare(nameA)
    })
  } else if (sortField.value === 'size') {
    result.sort((a, b) => {
      const sizeA = a.fileSize || 0
      const sizeB = b.fileSize || 0
      return sortOrder.value === 'desc' ? sizeB - sizeA : sizeA - sizeB
    })
  }
  return result
})

// ---------- 3D 卡片交互 ----------
const cardInnerRefs = new Map()
const setCardInnerRef = (id, el) => {
  if (el) cardInnerRefs.set(id, el)
}
const handleCardMouseMove = (event, id) => {
  const inner = cardInnerRefs.get(id)
  if (!inner) return
  const card = event.currentTarget
  const rect = card.getBoundingClientRect()
  const mouseX = event.clientX - rect.left
  const mouseY = event.clientY - rect.top
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  const offsetXRatio = (mouseX - centerX) / centerX
  const offsetYRatio = (mouseY - centerY) / centerY
  const MAX_ROTATION = 10
  let rotateY = offsetXRatio * MAX_ROTATION
  let rotateX = -offsetYRatio * MAX_ROTATION
  inner.style.transform = `rotateX(${rotateX}deg) rotateY(${rotateY}deg)`
}
const handleCardMouseLeave = (id) => {
  const inner = cardInnerRefs.get(id)
  if (inner) inner.style.transform = 'rotateX(0deg) rotateY(0deg)'
}

// ---------- 模态框控制 ----------
const showModal = ref(false)
const currentWork = ref(null)

const openDetailModal = (work) => {
  currentWork.value = work
  showModal.value = true
}

// ---------- 删除作品 ----------
const deleteArtwork = async (id) => {
  try {
    await axios.delete(`/api/artworks/${id}`)
    await fetchArtworks()
    showModal.value = false
    // alert('作品已删除')
  } catch (err) {
    console.error('删除失败', err)
    alert('删除失败，请稍后重试')
  }
}
</script>

<style scoped>
/* 容器与背景 */
.modeling-container {
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

/* 主要内容区域 */
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

/* 搜索和排序栏 */
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
.sort-buttons {
  display: flex;
  gap: 15px;
  position: relative;
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
  backdrop-filter: blur(4px);
  background: rgba(255, 255, 255, 0.2);
  box-shadow: 3px 3px 0px rgb(255, 255, 255);
  transform: translateY(-5px);
}
.sort-buttons button:hover {
  background: rgba(255, 255, 255, 0.3);
}
.sort-buttons button.active {
  box-shadow: 0px 0px 0px rgb(255, 255, 255);
  transform: translateY(0px);
}
.sort-menu {
  position: absolute;
  top: calc(100% + 5px);
  left: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 30px;
  padding: 8px 0;
  min-width: 150px;
  z-index: 20;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.2);
}
.sort-menu div {
  padding: 10px 20px;
  color: white;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 0.9rem;
  border-radius: 30px;
}
.sort-menu div:hover {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 30px;
}

/* 作品网格 */
.works-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 50px;
}
.work-card {
  perspective: 1200px;
  cursor: pointer;
  display: flex;
  width: 100%;
  height: 380px;
  min-width: 0;
}
.work-cover video {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  transform-style: preserve-3d;
  width: 100%;
  min-width: 0;
}
.work-card:hover .card-inner {
  box-shadow: 0 30px 40px -20px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.2);
}
.work-cover {
  width: 100%;
  height: 200px;
  overflow: hidden;
}
.work-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.2, 0.9, 0.4, 1.1);
}
.work-card:hover .work-cover img {
  transform: scale(1.03);
}
.work-info {
  flex: 1;
  padding: 16px;
  color: white;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.work-info h3 {
  font-size: 1.5rem;
  margin-bottom: 8px;
  font-weight: 600;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.work-info p {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12px;
  line-height: 1.4;
  min-height: calc(1.4em * 2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
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
