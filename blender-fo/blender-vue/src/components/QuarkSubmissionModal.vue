<template>
  <transition name="modal-fly">
    <div v-if="visible && resource" class="modal-overlay" :class="{ 'without-backdrop': !showBackdrop }" @click.self="close">
      <div class="modal-container">
        <div class="modal-scroll">
          <h2>{{ heading }}</h2>

          <div class="summary-card">
            <div class="summary-row">
              <span class="summary-label">标题</span>
              <span class="summary-value">{{ resource.title }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">状态</span>
              <span class="status-pill" :class="statusMeta.className">{{ statusMeta.label }}</span>
            </div>
            <p class="summary-desc">{{ statusMeta.description }}</p>
          </div>

          <div class="form-group">
            <label>夸克链接</label>
            <input
              v-model="form.quarkUrl"
              class="text-input"
              placeholder="请输入 pan.quark.cn 或 drive.quark.cn 链接"
              @blur="touched = true"
            />
            <p v-if="quarkUrlError" class="field-error">{{ quarkUrlError }}</p>
          </div>

          <div class="form-group">
            <label>提取码</label>
            <div class="radio-row">
              <label class="radio-option">
                <input v-model="extractionMode" type="radio" value="none" />
                <span>无</span>
              </label>
              <label class="radio-option">
                <input v-model="extractionMode" type="radio" value="has" />
                <span>有</span>
              </label>
            </div>
            <input
              v-if="extractionMode === 'has'"
              v-model="form.extractionCode"
              class="text-input"
              placeholder="请输入提取码"
              @blur="touched = true"
            />
            <p v-if="submitAttempted && !hasExtractionCode" class="field-error">请选择无提取码，或填写提取码</p>
          </div>

          <div class="status-bar" :class="statusBarClass">
            <span class="status-dot"></span>
            <span class="status-text">{{ statusBarLabel }}</span>
            <span class="status-desc">{{ statusBarDescription }}</span>
          </div>
        </div>

        <div class="modal-buttons">
          <button class="cancel-btn" @click="close">稍后填写</button>
          <button class="save-btn" :disabled="saving" @click="saveDraft">
            {{ saving ? '保存中...' : '保存草稿' }}
          </button>
          <button class="submit-btn" :disabled="submitting" @click="submitReview">
            {{ submitting ? '提交中...' : submitLabel }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, reactive, watch, ref } from 'vue'
import axios from 'axios'
import {
  getResourceEndpoint,
  getStatusMeta,
  hasCompleteReviewInfo,
  isSyncFailed,
  isValidQuarkUrl,
  NO_EXTRACTION_CODE
} from '../utils/resourceReview'

const props = defineProps({
  visible: Boolean,
  resource: Object,
  resourceType: {
    type: String,
    required: true
  },
  showBackdrop: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['close', 'saved'])

const form = reactive({
  quarkUrl: '',
  extractionCode: ''
})

const touched = ref(false)
const submitAttempted = ref(false)
const saving = ref(false)
const submitting = ref(false)
const extractionMode = ref('has')

watch(
  () => props.resource,
  (resource) => {
    form.quarkUrl = resource?.quarkUrl || ''
    form.extractionCode = resource?.extractionCode === NO_EXTRACTION_CODE ? '' : resource?.extractionCode || ''
    extractionMode.value = resource?.extractionCode === NO_EXTRACTION_CODE ? 'none' : 'has'
    touched.value = false
    submitAttempted.value = false
  },
  { immediate: true }
)

const heading = computed(() => (isSyncFailed(props.resource?.syncStatus) ? '重新提交审核' : '补充夸克信息'))
const submitLabel = computed(() => (isSyncFailed(props.resource?.syncStatus) ? '重新提交' : '提交审核'))
const statusMeta = computed(() => getStatusMeta(props.resource?.syncStatus))
const submittedExtractionCode = computed(() => (extractionMode.value === 'none' ? NO_EXTRACTION_CODE : form.extractionCode.trim()))
const hasExtractionCode = computed(() => extractionMode.value === 'none' || Boolean(form.extractionCode.trim()))

const quarkUrlError = computed(() => {
  if (!form.quarkUrl.trim()) {
    return ''
  }
  if (isValidQuarkUrl(form.quarkUrl)) {
    return ''
  }
  return touched.value || submitAttempted.value ? '请输入有效的夸克链接' : ''
})

const statusBarClass = computed(() => {
  if (hasCompleteReviewInfo(form.quarkUrl, submittedExtractionCode.value)) {
    return 'is-ready'
  }
  if (form.quarkUrl.trim() && quarkUrlError.value) {
    return 'is-failed'
  }
  return 'is-pending'
})

const statusBarLabel = computed(() => {
  if (statusBarClass.value === 'is-ready') {
    return '可提交审核'
  }
  if (statusBarClass.value === 'is-failed') {
    return '链接格式有误'
  }
  return '可先保存草稿'
})

const statusBarDescription = computed(() => {
  if (statusBarClass.value === 'is-ready') {
    return extractionMode.value === 'none' ? '夸克链接已准备完成，无需提取码' : '夸克链接和提取码都已准备完成'
  }
  if (statusBarClass.value === 'is-failed') {
    return '当前链接不是有效的夸克分享地址'
  }
  return '你可以稍后继续补全夸克信息'
})

const close = () => {
  emit('close')
}

const buildPayload = (action) => {
  const formData = new FormData()
  formData.append('id', props.resource.id)
  formData.append('title', props.resource.title || '')
  formData.append('description', props.resource.description || '')
  if (props.resourceType === 'plugin') {
    formData.append('blenderVersion', props.resource.blenderVersion || '')
  }
  formData.append('quarkUrl', form.quarkUrl)
  formData.append('extractionCode', submittedExtractionCode.value)
  formData.append('action', action)
  return formData
}

const saveDraft = async () => {
  saving.value = true
  touched.value = true

  try {
    await axios.put(getResourceEndpoint(props.resourceType), buildPayload('save'), {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    emit('saved')
    close()
  } catch (error) {
    console.error(error)
    alert('保存草稿失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const submitReview = async () => {
  submitAttempted.value = true
  touched.value = true

  if (!hasCompleteReviewInfo(form.quarkUrl, submittedExtractionCode.value)) {
    return
  }

  submitting.value = true
  const action = isSyncFailed(props.resource?.syncStatus) ? 'resubmit' : 'submitReview'

  try {
    await axios.put(getResourceEndpoint(props.resourceType), buildPayload(action), {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    emit('saved')
    close()
  } catch (error) {
    console.error(error)
    alert('提交审核失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(3px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1200;
}

.modal-overlay.without-backdrop {
  background: transparent;
  backdrop-filter: none;
}

.modal-container {
  width: 560px;
  max-width: 90%;
  max-height: calc(100vh - 160px);
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  color: white;
  background: linear-gradient(120deg, rgba(0, 0, 0, 0.88), rgba(255, 255, 255, 0.4));
  backdrop-filter: blur(20px);
  overflow: hidden;
}

.modal-scroll {
  padding: 32px 32px 20px;
  overflow-y: auto;
}

.modal-container h2 {
  margin-bottom: 24px;
  text-align: center;
  font-size: 1.6rem;
}

.summary-card {
  margin-bottom: 24px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.summary-label {
  color: rgba(255, 255, 255, 0.72);
}

.summary-value {
  text-align: right;
}

.summary-desc {
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.88rem;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.88);
}

.radio-row {
  display: flex;
  gap: 14px;
  margin-bottom: 12px;
}

.radio-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  cursor: pointer;
}

.radio-option input {
  accent-color: #38a169;
}

.text-input {
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 12px 14px;
  color: white;
  background: rgba(255, 255, 255, 0.14);
}

.text-input:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.22);
}

.field-error {
  margin-top: 8px;
  color: #ffb3b3;
  font-size: 0.82rem;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 0.82rem;
}

.status-pill.is-pending {
  background: rgba(255, 193, 7, 0.18);
  color: #ffd970;
}

.status-pill.is-syncing {
  background: rgba(86, 170, 255, 0.18);
  color: #8cc7ff;
}

.status-pill.is-synced {
  background: rgba(80, 200, 120, 0.18);
  color: #8cf1a3;
}

.status-pill.is-failed {
  background: rgba(255, 107, 107, 0.18);
  color: #ffb0b0;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.status-text {
  font-weight: 700;
}

.status-desc {
  font-size: 0.84rem;
  color: rgba(255, 255, 255, 0.78);
}

.status-bar.is-ready .status-dot {
  background: #39d98a;
  box-shadow: 0 0 12px rgba(57, 217, 138, 0.8);
}

.status-bar.is-pending .status-dot {
  background: #f3c64d;
  box-shadow: 0 0 12px rgba(243, 198, 77, 0.8);
}

.status-bar.is-failed .status-dot {
  background: #ff6b6b;
  box-shadow: 0 0 12px rgba(255, 107, 107, 0.8);
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 32px 24px;
  background: linear-gradient(0deg, rgba(0, 0, 0, 0.85), rgba(0, 0, 0, 0.56));
}

.modal-buttons button {
  border: none;
  border-radius: 999px;
  padding: 10px 18px;
  color: white;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.modal-buttons button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-buttons button:hover:not(:disabled) {
  transform: scale(1.02);
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.18);
}

.save-btn {
  background: #3f6fd5;
}

.submit-btn {
  background: #38a169;
}
</style>
