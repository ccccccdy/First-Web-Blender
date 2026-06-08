<template>
  <div class="manage-page">
    <video autoplay muted loop playsinline class="bg-video">
      <source src="../assets/video/Miku.mp4" type="video/mp4" />
    </video>

    <div class="page-mask"></div>

    <div class="content-wrapper">
      <div class="header-card">
        <div>
          <h1>管理队列</h1>
          <p>这里只处理处于 Syncing 的资源。审核通过后会清理本地压缩包，失败则退回普通页面继续修正。</p>
        </div>
        <button class="refresh-btn" @click="fetchQueue">刷新队列</button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="queue.length === 0" class="empty-state">
        当前没有等待审核的资源。
      </div>
      <div v-else class="queue-grid">
        <article
          v-for="item in queue"
          :key="`${item.resourceType}-${item.id}`"
          class="queue-card"
          @click="openReviewModal(item)"
        >
          <div class="queue-card-inner">
            <div class="card-top">
              <span class="type-chip">{{ getResourceTypeLabel(item.resourceType) }}</span>
              <span class="status-chip" :class="getStatusMeta(item.syncStatus).className">
                {{ getStatusMeta(item.syncStatus).label }}
              </span>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description || '暂无描述' }}</p>
            <div class="card-meta">
              <span>{{ formatDate(item.updatedAt || item.createdAt) }}</span>
              <span>{{ formatSize(item.fileSize) }}</span>
            </div>
          </div>
        </article>
      </div>
    </div>

    <ReviewQueueModal
      :visible="showModal"
      :resource="currentResource"
      @close="showModal = false"
      @reviewed="handleReviewed"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import axios from 'axios'
import ReviewQueueModal from '../components/ReviewQueueModal.vue'
import {
  RESOURCE_TYPES,
  getResourceTypeLabel,
  getStatusMeta,
  isSyncing
} from '../utils/resourceReview'
import { formatResourceSize } from '../utils/sizeFormat'

const loading = ref(false)
const queue = ref([])
const showModal = ref(false)
const currentResource = ref(null)

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

const formatSize = (sizeMB) => formatResourceSize(sizeMB)

const fetchQueue = async () => {
  loading.value = true

  try {
    const [artworksRes, assetsRes, pluginsRes] = await Promise.all([
      axios.get('/api/artworks'),
      axios.get('/api/assets'),
      axios.get('/api/plugins', { params: { _t: Date.now() } })
    ])

    const merged = [
      ...artworksRes.data.map((item) => ({ ...item, resourceType: RESOURCE_TYPES.ARTWORK })),
      ...assetsRes.data.map((item) => ({ ...item, resourceType: RESOURCE_TYPES.ASSET })),
      ...pluginsRes.data.map((item) => ({ ...item, resourceType: RESOURCE_TYPES.PLUGIN }))
    ]

    queue.value = merged
      .filter((item) => isSyncing(item.syncStatus))
      .sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
  } catch (error) {
    console.error(error)
    alert('加载管理队列失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const openReviewModal = (item) => {
  currentResource.value = item
  showModal.value = true
}

const handleReviewed = async () => {
  await fetchQueue()
}

onMounted(fetchQueue)
</script>

<style scoped>
.manage-page {
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
}

.page-mask {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.04), transparent 45%),
    linear-gradient(180deg, rgba(0, 0, 0, 0.18), rgba(0, 0, 0, 0.3));
  pointer-events: none;
}

.content-wrapper {
  position: relative;
  z-index: 10;
  margin: 100px 200px 80px 300px;
}

.header-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 28px 32px;
  margin-bottom: 32px;
  border-radius: 28px;
  background: rgba(0, 0, 0, 0.45);
  color: white;
}

.header-card h1 {
  margin-bottom: 10px;
}

.header-card p {
  max-width: 760px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
}

.refresh-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  cursor: pointer;
  color: white;
  background: rgba(255, 255, 255, 0.18);
}

.loading,
.empty-state {
  padding: 60px 24px;
  border-radius: 28px;
  text-align: center;
  color: white;
  background: rgba(0, 0, 0, 0.35);
}

.queue-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.queue-card {
  perspective: 1200px;
  cursor: pointer;
}

.queue-card-inner {
  height: 100%;
  padding: 22px;
  border-radius: 24px;
  background: rgba(0, 0, 0, 0.45);
  color: white;
  box-shadow: 8px 8px 2px rgba(0, 0, 0, 0.8);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.queue-card:hover .queue-card-inner {
  transform: translateY(-4px);
  box-shadow: 0 24px 36px -18px rgba(0, 0, 0, 0.7);
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.type-chip,
.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
}

.type-chip {
  background: rgba(255, 255, 255, 0.16);
}

.status-chip.is-syncing {
  background: rgba(86, 170, 255, 0.18);
  color: #8cc7ff;
}

.queue-card h3 {
  margin-bottom: 12px;
  font-size: 1.25rem;
}

.queue-card p {
  min-height: 3.2em;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.78);
}

.card-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.72);
}

@media (max-width: 1200px) {
  .content-wrapper {
    margin: 100px 60px 80px 220px;
  }

  .queue-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .content-wrapper {
    margin: 90px 24px 60px 24px;
  }

  .header-card {
    flex-direction: column;
    align-items: stretch;
  }

  .queue-grid {
    grid-template-columns: 1fr;
  }
}
</style>
