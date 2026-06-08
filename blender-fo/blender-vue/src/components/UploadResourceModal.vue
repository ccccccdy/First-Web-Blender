<template>
  <transition name="modal-fly">
    <div v-if="visible" class="modal-overlay" @click.self="close">
      <div class="modal-container" :style="modalStyle">
        <div class="modal-scroll">
          <h2>{{ modalTitle }}</h2>

          <div class="form-group title-group">
            <label>{{ nameLabel }} *</label>
            <div class="title-input-wrapper">
              <input v-model="form.title" :placeholder="namePlaceholder" />
              <button class="favorite-btn" @click="toggleFavorite" title="收藏">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.07 5.82 22 7 14.14 2 9.27l6.91-1.01L12 2z" />
                </svg>
              </button>
            </div>
          </div>

          <div class="form-group">
            <label>{{ descriptionLabel }}</label>
            <textarea v-model="form.description" rows="4" :placeholder="descriptionPlaceholder"></textarea>
          </div>

          <div class="form-group file-group">
            <label>{{ fileLabel }} *</label>
            <div class="file-controls">
              <button class="add-file-btn" @click="addFiles">+ {{ addFileLabel }}</button>
              <span class="field-hint">{{ fileHint }}</span>
            </div>
            <div v-if="files.length" class="file-list">
              <div v-for="(file, idx) in files" :key="`${file.name}-${idx}`" class="file-item">
                <span>{{ file.name }}</span>
                <button class="remove-file" @click="removeFile(idx)">×</button>
              </div>
            </div>
          </div>

          <div class="status-bar is-pending">
            <span class="status-dot"></span>
            <span class="status-text">第一步：生成本地压缩包</span>
            <span class="status-desc">上传成功后会进入 Pending Link，并弹出第二步补链窗口</span>
          </div>
        </div>

        <div class="modal-buttons">
          <button class="cancel-btn" @click="close">取消</button>
          <button
            class="submit-btn"
            :class="{ 'btn-active': isFormValid }"
            :disabled="!isFormValid || uploading"
            @click="submitUpload"
          >
            {{ uploading ? '上传中...' : '确认上传' }}
          </button>
        </div>
      </div>
    </div>
  </transition>

  <QuarkSubmissionModal
    :visible="showQuarkModal"
    :resource="pendingResource"
    :resource-type="resourceType"
    @close="closeQuarkModal"
    @saved="handleQuarkSaved"
  />
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import axios from 'axios'
import QuarkSubmissionModal from './QuarkSubmissionModal.vue'

const props = defineProps({
  visible: Boolean,
  triggerElement: HTMLElement,
  modalTitle: {
    type: String,
    required: true
  },
  nameLabel: {
    type: String,
    required: true
  },
  namePlaceholder: {
    type: String,
    required: true
  },
  descriptionLabel: {
    type: String,
    required: true
  },
  descriptionPlaceholder: {
    type: String,
    required: true
  },
  fileLabel: {
    type: String,
    required: true
  },
  addFileLabel: {
    type: String,
    required: true
  },
  fileHint: {
    type: String,
    required: true
  },
  submitUrl: {
    type: String,
    required: true
  },
  fileFieldName: {
    type: String,
    required: true
  },
  resourceType: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['close', 'upload-success'])

const modalStyle = reactive({
  transform: 'scale(0.3)',
  opacity: 0,
  transition: 'all 0.3s cubic-bezier(0.42,0,0.58,1)'
})

const createInitialForm = () => ({
  title: '',
  description: ''
})

const form = ref(createInitialForm())
const files = ref([])
const uploading = ref(false)
const showQuarkModal = ref(false)
const pendingResource = ref(null)

const isFormValid = computed(() => {
  return form.value.title.trim() !== '' && files.value.length > 0
})

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      animateIn()
    }
  }
)

const toggleFavorite = () => {
  alert('收藏功能开发中')
}

const addFiles = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '*'
  input.multiple = true
  input.onchange = (event) => {
    files.value.push(...Array.from(event.target.files || []))
  }
  input.click()
}

const removeFile = (index) => {
  files.value.splice(index, 1)
}

const resetForm = () => {
  form.value = createInitialForm()
  files.value = []
}

const animateIn = () => {
  if (props.triggerElement) {
    const rect = props.triggerElement.getBoundingClientRect()
    const modalWidth = 600
    const modalHeight = 620
    const left = rect.left + (rect.width - modalWidth) / 2
    const top = rect.top + (rect.height - modalHeight) / 2
    modalStyle.transform = `translate(${left}px, ${top}px) scale(0.3)`
    modalStyle.opacity = 0
    requestAnimationFrame(() => {
      modalStyle.transform = 'translate(-50%, -50%) scale(1)'
      modalStyle.opacity = 1
    })
    return
  }

  modalStyle.transform = 'translate(-50%, -50%) scale(1)'
  modalStyle.opacity = 1
}

const close = () => {
  modalStyle.transform = 'scale(0.3)'
  modalStyle.opacity = 0
  setTimeout(() => {
    emit('close')
    resetForm()
  }, 300)
}

const submitUpload = async () => {
  if (!isFormValid.value) {
    return
  }

  uploading.value = true

  const formData = new FormData()
  formData.append('title', form.value.title)
  formData.append('description', form.value.description || '')
  files.value.forEach((file) => formData.append(props.fileFieldName, file))

  try {
    const response = await axios.post(props.submitUrl, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    pendingResource.value = response.data
    emit('upload-success')
    showQuarkModal.value = true
    close()
  } catch (error) {
    console.error(error)
    alert('上传失败，请检查后端服务')
  } finally {
    uploading.value = false
  }
}

const closeQuarkModal = () => {
  showQuarkModal.value = false
  pendingResource.value = null
}

const handleQuarkSaved = () => {
  emit('upload-success')
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
  z-index: 1000;
  user-select: none;
}

.modal-container {
  background: linear-gradient(120deg, rgba(0, 0, 0, 0.85), rgba(255, 255, 255, 0.6));
  backdrop-filter: blur(20px);
  border-radius: 28px;
  padding: 40px 40px 0;
  width: 600px;
  max-width: 90%;
  max-height: calc(100vh - 200px);
  color: white;
  position: absolute;
  left: 50%;
  top: 50%;
  display: flex;
  flex-direction: column;
  overflow: visible;
  transform: translate(-50%, -50%);
  transition: transform 0.3s cubic-bezier(0.2, 0.9, 0.4, 1.1), opacity 0.3s;
}

.modal-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  width: calc(100% + 34px);
  margin-right: -34px;
  padding-right: 34px;
  scrollbar-gutter: stable;
}

.modal-scroll::-webkit-scrollbar {
  width: 10px;
}

.modal-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 999px;
}

.modal-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.45);
  border-radius: 999px;
}

.modal-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.62);
}

.modal-container h2 {
  margin-bottom: 30px;
  font-size: 1.8rem;
  text-align: center;
  font-weight: bold;
}

.form-group {
  margin-bottom: 26px;
}

.file-group {
  margin-bottom: 30px;
}

.title-input-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.title-group input,
.form-group textarea {
  width: 100%;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
  color: white;
  font-size: 1rem;
}

.title-group input:focus,
.form-group textarea:focus {
  background: rgba(255, 255, 255, 0.25);
  outline: none;
}

.title-group input::placeholder,
.form-group textarea::placeholder {
  color: rgba(255, 255, 255, 0.8);
}

.favorite-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 215, 0, 0.8);
  transition: transform 0.2s;
}

.favorite-btn:hover {
  transform: scale(1.1);
  color: gold;
}

.form-group textarea {
  resize: vertical;
  font-family: inherit;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.85);
}

.file-controls {
  display: flex;
  gap: 15px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.field-hint {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.72);
}

.add-file-btn {
  background: #2196f3;
  border: none;
  padding: 8px 15px;
  border-radius: 30px;
  color: white;
  cursor: pointer;
  transition: 0.2s;
}

.add-file-btn:hover {
  background: #0b7dda;
  transform: scale(1.02);
}

.file-list {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 16px;
  padding: 10px;
  max-height: 150px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 0.85rem;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item span {
  word-break: break-all;
  margin-right: 10px;
}

.remove-file {
  background: none;
  border: none;
  color: #ff7e5e;
  font-size: 18px;
  cursor: pointer;
  padding: 0 5px;
}

.remove-file:hover {
  color: #ff4d2e;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 12px 16px 20px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  flex-shrink: 0;
  background: #f3c64d;
  box-shadow: 0 0 12px rgba(243, 198, 77, 0.8);
}

.status-text {
  font-weight: 700;
}

.status-desc {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.8);
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  z-index: 5;
  margin: 0 -40px;
  padding: 18px 40px 24px;
  background: linear-gradient(0deg, rgba(0, 0, 0, 0.85), rgba(0, 0, 0, 0.6));
  backdrop-filter: blur(40px);
  border-radius: 0 0 28px 28px;
}

.cancel-btn,
.submit-btn {
  padding: 8px 20px;
  border-radius: 40px;
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: 0.2s;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.submit-btn {
  background: #ff7e5e;
  color: white;
}

.submit-btn.btn-active {
  background: #4caf50;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn:hover,
.submit-btn:hover:not(:disabled) {
  transform: scale(1.02);
}
</style>
