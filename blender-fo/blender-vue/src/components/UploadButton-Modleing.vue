<template>
  <div>
    <div class="upload-btn-fixed" @click="toggleSubMenu">
      <div class="upload-icon-wrapper">
        <svg class="upload-icon" aria-hidden="true">
          <use :xlink:href="`#icon-shangchuan`" />
        </svg>
      </div>
      <span class="upload-text">上传资源</span>
    </div>

    <transition name="submenu-fade">
      <div v-if="showSubMenu" class="submenu-container">
        <div
          v-for="(item, index) in subItems"
          :key="item.type"
          class="submenu-item"
          :style="{ animationDelay: `${index * 0.2}s` }"
          @click="handleSubItemClick(item.type, $event)"
        >
          {{ item.label }}
        </div>
      </div>
    </transition>

    <div v-if="showSubMenu" class="submenu-mask" @click="closeSubMenu"></div>

    <transition name="modal-fly">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-container" :style="modalStyle">
          <div class="modal-scroll">
            <h2>上传新建模</h2>

            <div class="form-group title-group">
              <label>建模标题 *</label>
              <div class="title-input-wrapper">
                <input v-model="form.title" placeholder="请输入建模标题" />
                <button class="favorite-btn" @click="toggleFavorite" title="收藏">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.07 5.82 22 7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="form-group">
              <label>建模描述</label>
              <textarea v-model="form.description" rows="4" placeholder="请输入建模描述"></textarea>
            </div>

            <div class="form-group file-group">
              <label>预览图 *</label>
              <div class="file-controls">
                <button class="add-file-btn" @click="addPreviewImage">+ 添加预览图</button>
                <span class="field-hint">必须上传 1 张预览图</span>
              </div>
              <div v-if="previewImage" class="file-list">
                <div class="file-item">
                  <span>{{ previewImage.name }}</span>
                  <button class="remove-file" @click="removePreviewImage">×</button>
                </div>
              </div>
            </div>

            <div class="form-group file-group">
              <div class="label-with-icon">
                <label>附加图片/视频</label>
                <div class="info-icon-wrapper" @mouseenter="startTooltipTimer" @mouseleave="hideTooltip">
                  <svg class="question-icon" aria-hidden="true">
                    <use xlink:href="#icon-question-circle-fill" />
                  </svg>
                  <div v-if="showTooltip" class="tooltip-text">
                    这里的附加图片和视频会和模型文件一起打包成本地压缩包，预览图会单独保留。
                  </div>
                </div>
              </div>
              <div class="file-controls">
                <button class="add-file-btn" @click="addMediaFiles">+ 添加图片/视频</button>
                <span class="field-hint">可选上传</span>
              </div>
              <div v-if="mediaFiles.length" class="file-list">
                <div v-for="(file, idx) in mediaFiles" :key="`${file.name}-${idx}`" class="file-item">
                  <span>{{ file.name }}</span>
                  <button class="remove-file" @click="removeMediaFile(idx)">×</button>
                </div>
              </div>
            </div>

            <div class="form-group file-group">
              <label>模型文件 *</label>
              <div class="file-controls">
                <button class="add-file-btn" @click="addModelFile">+ 添加模型文件</button>
                <span class="field-hint">至少 1 个 .blend / .obj / .fbx 文件</span>
              </div>
              <div v-if="modelFiles.length" class="file-list">
                <div v-for="(file, idx) in modelFiles" :key="`${file.name}-${idx}`" class="file-item">
                  <span>{{ file.name }}</span>
                  <button class="remove-file" @click="removeModelFile(idx)">×</button>
                </div>
              </div>
            </div>

            <div class="status-bar is-pending">
              <span class="status-dot"></span>
              <span class="status-text">第一步：生成本地文件</span>
              <span class="status-desc">上传成功后会先创建 Pending Link，再弹出补链窗口</span>
            </div>
          </div>

          <div class="modal-buttons">
            <button class="cancel-btn" @click="closeModal">取消</button>
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

    <UploadAssetModal
      :visible="showAssetModal"
      :trigger-element="assetTrigger"
      @close="showAssetModal = false; assetTrigger = null"
      @upload-success="emit('upload-success')"
    />

    <UploadPluginModal
      :visible="showPluginModal"
      :trigger-element="pluginTrigger"
      @close="showPluginModal = false; pluginTrigger = null"
      @upload-success="emit('upload-success')"
    />

    <QuarkSubmissionModal
      :visible="showQuarkModal"
      :resource="pendingArtwork"
      resource-type="artwork"
      @close="closeQuarkModal"
      @saved="handleQuarkSaved"
    />
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import axios from 'axios'
import QuarkSubmissionModal from './QuarkSubmissionModal.vue'
import UploadAssetModal from './UploadButton-Asset.vue'
import UploadPluginModal from './UploadButton-Plugins.vue'

const emit = defineEmits(['upload-success'])

const showSubMenu = ref(false)
const subItems = [
  { type: 'modeling', label: '上传建模' },
  { type: 'asset', label: '上传资产' },
  { type: 'plugin', label: '上传插件' }
]

const showModal = ref(false)
const uploading = ref(false)
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
const previewImage = ref(null)
const mediaFiles = ref([])
const modelFiles = ref([])

const showAssetModal = ref(false)
const assetTrigger = ref(null)
const showPluginModal = ref(false)
const pluginTrigger = ref(null)
const showQuarkModal = ref(false)
const pendingArtwork = ref(null)

const isFormValid = computed(() => {
  return form.value.title.trim() !== '' && Boolean(previewImage.value) && modelFiles.value.length > 0
})

const showTooltip = ref(false)
let tooltipTimer = null

const startTooltipTimer = () => {
  tooltipTimer = setTimeout(() => {
    showTooltip.value = true
  }, 100)
}

const hideTooltip = () => {
  if (tooltipTimer) {
    clearTimeout(tooltipTimer)
    tooltipTimer = null
  }
  showTooltip.value = false
}

const toggleFavorite = () => {
  alert('收藏功能开发中')
}

const selectFiles = ({ accept, multiple = false, onSelected }) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = accept
  input.multiple = multiple
  input.onchange = (event) => {
    onSelected(Array.from(event.target.files || []))
  }
  input.click()
}

const addPreviewImage = () => {
  selectFiles({
    accept: 'image/*',
    onSelected: (files) => {
      previewImage.value = files[0] || null
    }
  })
}

const removePreviewImage = () => {
  previewImage.value = null
}

const addMediaFiles = () => {
  selectFiles({
    accept: 'image/*,video/*',
    multiple: true,
    onSelected: (files) => {
      mediaFiles.value.push(...files)
    }
  })
}

const removeMediaFile = (index) => {
  mediaFiles.value.splice(index, 1)
}

const addModelFile = () => {
  selectFiles({
    accept: '.blend,.obj,.fbx',
    multiple: true,
    onSelected: (files) => {
      modelFiles.value.push(...files)
    }
  })
}

const removeModelFile = (index) => {
  modelFiles.value.splice(index, 1)
}

const resetModelingForm = () => {
  form.value = createInitialForm()
  previewImage.value = null
  mediaFiles.value = []
  modelFiles.value = []
  hideTooltip()
}

const toggleSubMenu = () => {
  showSubMenu.value = !showSubMenu.value
}

const closeSubMenu = () => {
  showSubMenu.value = false
}

const handleSubItemClick = (type, event) => {
  closeSubMenu()
  const btn = event.currentTarget

  if (type === 'modeling') {
    const rect = btn.getBoundingClientRect()
    const modalWidth = 600
    const modalHeight = 760
    const left = rect.left + (rect.width - modalWidth) / 2
    const top = rect.top + (rect.height - modalHeight) / 2
    modalStyle.transform = `translate(${left}px, ${top}px) scale(0.3)`
    modalStyle.opacity = 0
    showModal.value = true
    requestAnimationFrame(() => {
      modalStyle.transform = 'translate(-50%, -50%) scale(1)'
      modalStyle.opacity = 1
    })
    return
  }

  if (type === 'asset') {
    assetTrigger.value = btn
    showAssetModal.value = true
    return
  }

  pluginTrigger.value = btn
  showPluginModal.value = true
}

const closeModal = () => {
  modalStyle.transform = 'scale(0.3)'
  modalStyle.opacity = 0
  setTimeout(() => {
    showModal.value = false
    resetModelingForm()
    modalStyle.transform = 'scale(0.3)'
    modalStyle.opacity = 0
  }, 300)
}

const submitUpload = async () => {
  if (!isFormValid.value) return

  uploading.value = true

  const formData = new FormData()
  formData.append('title', form.value.title)
  formData.append('description', form.value.description || '')

  if (previewImage.value) {
    formData.append('coverFiles', previewImage.value)
  }

  mediaFiles.value.forEach((file) => formData.append('coverFiles', file))
  modelFiles.value.forEach((file) => formData.append('modelFiles', file))

  try {
    const response = await axios.post('/api/artworks', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    pendingArtwork.value = response.data
    emit('upload-success')
    showQuarkModal.value = true
    closeModal()
  } catch (error) {
    console.error(error)
    alert('上传失败，请检查后端服务')
  } finally {
    uploading.value = false
  }
}

const closeQuarkModal = () => {
  showQuarkModal.value = false
  pendingArtwork.value = null
}

const handleQuarkSaved = () => {
  emit('upload-success')
}
</script>

<style scoped>
.upload-btn-fixed {
  position: fixed;
  right: 40px;
  top: 80%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  width: 100px;
  padding: 15px 0;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 25px;
  box-shadow: 8px 8px 2px rgba(0, 0, 0, 0.8);
  cursor: pointer;
  transition: all 0.2s;
  z-index: 200;
  color: white;
  user-select: none;
}

.upload-btn-fixed:hover {
  background: rgba(181, 181, 181, 0.852);
  transform: translateY(-50%) scale(1.1);
  color: rgb(16, 16, 16);
}

.upload-icon-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
}

.upload-icon {
  width: 50px;
  height: 50px;
  stroke: white;
}

.upload-text {
  font-size: 14px;
  font-weight: bold;
  text-align: center;
}

.submenu-container {
  position: fixed;
  right: -50px;
  top: 30%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 50px;
  z-index: 201;
  background: transparent;
  user-select: none;
}

.submenu-item {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(20px);
  border-radius: 60px;
  width: 600px;
  padding: 40px 24px;
  text-align: center;
  font-size: 30px;
  font-weight: 1000;
  color: rgb(212, 212, 212);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(255, 255, 255, 0.2);
  opacity: 0;
  transform: translateY(-10px);
  animation: slideIn 0.3s ease forwards;
  box-shadow: 3px 3px 0 rgb(255, 255, 255);
}

.submenu-item:hover {
  background: rgba(0, 132, 189, 0.9);
  transform: scale(1.05);
}

@keyframes slideIn {
  0% {
    opacity: 0;
    transform: translateY(-10px);
  }

  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.submenu-fade-enter-active,
.submenu-fade-leave-active {
  transition: opacity 0.2s ease;
}

.submenu-fade-enter-from,
.submenu-fade-leave-to {
  opacity: 0;
}

.submenu-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 199;
}

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
  transition: all 0.2s;
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

.label-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
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

.info-icon-wrapper {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.question-icon {
  width: 18px;
  height: 18px;
  fill: currentColor;
  color: rgba(255, 255, 255, 0.7);
  transform: translateY(-3px);
  transition: color 0.2s;
}

.question-icon:hover {
  color: #ffaa66;
}

.tooltip-text {
  position: absolute;
  bottom: 150%;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  color: white;
  font-size: 0.75rem;
  padding: 6px 12px;
  border-radius: 8px;
  z-index: 100;
  pointer-events: none;
  white-space: normal;
  width: 220px;
  text-align: center;
}
</style>
