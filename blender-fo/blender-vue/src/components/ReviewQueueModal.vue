<template>
  <div v-if="visible && resource" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <div class="modal-body">
        <div class="header-row">
          <div>
            <h2>{{ resource.title }}</h2>
            <p class="type-text">{{ resourceTypeLabel }}</p>
          </div>
          <span class="status-pill" :class="statusMeta.className">{{ statusMeta.label }}</span>
        </div>

        <div v-if="resource.description" class="description-box">
          {{ resource.description }}
        </div>

        <div v-if="previewImageUrl" class="preview-box">
          <img :src="previewImageUrl" :alt="resource.title" class="preview-image" />
        </div>

        <div class="info-grid">
          <div class="info-card">
            <strong>本地信息摘要</strong>
            <p v-if="resource.fileSize">{{ RESOURCE_SIZE_LABEL }}：{{ formatSize(resource.fileSize) }}</p>
            <ul v-if="fileNames.length" class="plain-list">
              <li v-for="name in fileNames" :key="name">{{ name }}</li>
            </ul>
            <p v-else class="empty-text">没有可展示的本地文件名</p>
          </div>

          <div class="info-card">
            <strong>夸克信息</strong>
            <a :href="resource.quarkUrl" target="_blank" rel="noreferrer" class="quark-link">
              {{ resource.quarkUrl }}
            </a>
            <p>提取码：{{ resource.extractionCode }}</p>
            <button class="open-link-btn" @click="openLink">打开夸克链接</button>
          </div>
        </div>

        <div class="review-panel">
          <label>未通过原因</label>
          <textarea
            v-model="failureReason"
            rows="3"
            placeholder="如果点未通过，这里必须填写简短原因"
          ></textarea>
        </div>
      </div>

      <div class="modal-buttons">
        <button class="cancel-btn" @click="$emit('close')">关闭</button>
        <button class="reject-btn" :disabled="submitting" @click="rejectResource">
          {{ submitting ? '处理中...' : '未通过' }}
        </button>
        <button class="approve-btn" :disabled="submitting" @click="approveResource">
          {{ submitting ? '处理中...' : '通过' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import axios from 'axios'
import {
  extractFileName,
  getResourceEndpoint,
  getResourceTypeLabel,
  getStatusMeta,
  splitUrls
} from '../utils/resourceReview'
import { RESOURCE_SIZE_LABEL, formatResourceSize } from '../utils/sizeFormat'

const props = defineProps({
  visible: Boolean,
  resource: Object
})

const emit = defineEmits(['close', 'reviewed'])

const failureReason = ref('')
const submitting = ref(false)

watch(
  () => props.resource,
  () => {
    failureReason.value = ''
    submitting.value = false
  }
)

const statusMeta = computed(() => getStatusMeta(props.resource?.syncStatus))
const resourceTypeLabel = computed(() => getResourceTypeLabel(props.resource?.resourceType))
const previewImageUrl = computed(() => splitUrls(props.resource?.coverUrls)[0] || '')
const fileNames = computed(() => {
  const urls = props.resource?.resourceType === 'artwork'
    ? splitUrls(props.resource?.modelFileUrls)
    : splitUrls(props.resource?.fileUrls)
  return urls.map(extractFileName)
})

const formatSize = (sizeMB) => formatResourceSize(sizeMB)

const openLink = () => {
  window.open(props.resource.quarkUrl, '_blank', 'noopener,noreferrer')
}

const reviewResource = async (decision) => {
  submitting.value = true

  const formData = new FormData()
  formData.append('decision', decision)
  if (decision === 'reject') {
    if (!failureReason.value.trim()) {
      alert('未通过时必须填写失败原因')
      submitting.value = false
      return
    }
    formData.append('failureReason', failureReason.value)
  }

  try {
    const endpoint = `${getResourceEndpoint(props.resource.resourceType)}/${props.resource.id}/review`
    await axios.post(endpoint, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    emit('reviewed')
    emit('close')
  } catch (error) {
    console.error(error)
    alert('审核失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const approveResource = () => reviewResource('approve')
const rejectResource = () => reviewResource('reject')
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1100;
}

.modal-container {
  width: 960px;
  max-width: 92%;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  color: white;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.55), rgba(0, 0, 0, 0.75));
  backdrop-filter: blur(20px);
}

.modal-body {
  padding: 28px;
  overflow-y: auto;
}

.header-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: start;
  margin-bottom: 18px;
}

.header-row h2 {
  margin: 0 0 8px;
}

.type-text {
  color: rgba(255, 255, 255, 0.72);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 700;
}

.status-pill.is-syncing {
  background: rgba(86, 170, 255, 0.18);
  color: #8cc7ff;
}

.description-box,
.info-card,
.review-panel {
  padding: 16px;
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.26);
}

.description-box {
  margin-bottom: 18px;
  line-height: 1.6;
}

.preview-box {
  margin-bottom: 18px;
  border-radius: 18px;
  overflow: hidden;
  background: #000;
}

.preview-image {
  display: block;
  width: 100%;
  max-height: 380px;
  object-fit: contain;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.info-card strong {
  display: block;
  margin-bottom: 10px;
}

.plain-list {
  margin: 8px 0 0;
  padding-left: 20px;
}

.plain-list li {
  margin: 6px 0;
}

.quark-link {
  display: inline-block;
  margin: 6px 0 10px;
  color: #fff066;
  word-break: break-all;
}

.open-link-btn {
  margin-top: 10px;
  border: none;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.18);
  color: white;
}

.empty-text {
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
}

.review-panel label {
  display: block;
  margin-bottom: 8px;
}

.review-panel textarea {
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 12px 14px;
  resize: vertical;
  color: white;
  background: rgba(255, 255, 255, 0.14);
}

.review-panel textarea:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.2);
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 28px 24px;
  background: linear-gradient(0deg, rgba(0, 0, 0, 0.85), rgba(0, 0, 0, 0.56));
}

.modal-buttons button {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  color: white;
  cursor: pointer;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.18);
}

.reject-btn {
  background: #c05656;
}

.approve-btn {
  background: #38a169;
}

.modal-buttons button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 820px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
