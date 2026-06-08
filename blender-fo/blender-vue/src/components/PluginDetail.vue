<template>
  <div v-if="visible && plugin" class="modal-overlay" @click.self="close">
    <div class="modal-container">
      <div class="title-area">
        <input v-if="isEditing" v-model="editForm.title" class="edit-title-input" placeholder="插件名称" />
        <h2 v-else>{{ plugin.title }}</h2>
      </div>

      <div class="modal-body">
        <div class="status-row">
          <span class="status-pill" :class="statusMeta.className">{{ statusMeta.label }}</span>
          <span class="status-text">{{ statusMeta.description }}</span>
        </div>

        <div class="description-area">
          <textarea v-if="isEditing" v-model="editForm.description" rows="4" class="edit-description-input" placeholder="插件描述"></textarea>
          <p v-else class="description">{{ plugin.description || '暂无描述' }}</p>
        </div>

        <div v-if="isSyncFailedStatus && plugin.failureReason" class="failure-box">
          <strong>未通过原因</strong>
          <p>{{ plugin.failureReason }}</p>
        </div>

        <div class="info">
          <span>Blender 版本：{{ plugin.blenderVersion || '未填写' }}</span>
          <span>{{ RESOURCE_SIZE_LABEL }}：{{ formatSize(plugin.fileSize) }}</span>
        </div>

        <div v-if="!isSyncedStatus" class="panel-block">
          <strong class="section-title">本地信息</strong>
          <ul v-if="fileNames.length" class="plain-list">
            <li v-for="name in fileNames" :key="name">{{ name }}</li>
          </ul>
          <p v-else class="empty-text">本地压缩包已不可用</p>
        </div>

        <div v-if="!isEditing || isPendingLinkStatus || isSyncingStatus || isSyncedStatus" class="panel-block">
          <strong class="section-title">夸克信息</strong>
          <div class="quark-content-row">
            <div class="quark-grid">
              <div class="quark-row">
                <span class="quark-label">夸克链接</span>
                <template v-if="plugin.quarkUrl">
                  <a
                    v-if="isSyncedStatus || isSyncingStatus"
                    :href="plugin.quarkUrl"
                    target="_blank"
                    rel="noreferrer"
                    class="quark-link"
                  >
                    {{ plugin.quarkUrl }}
                  </a>
                  <span v-else class="quark-value">{{ plugin.quarkUrl }}</span>
                </template>
                <span v-else class="quark-empty">暂未填写</span>
              </div>
              <div class="quark-row">
                <span class="quark-label">提取码</span>
                <span class="quark-value">{{ plugin.extractionCode || '暂未填写' }}</span>
              </div>
            </div>
            <div v-if="isPendingLinkStatus" class="supplement-action">
              <button class="supplement-btn" @click="openQuarkSubmission">补充夸克信息</button>
            </div>
          </div>
          <p v-if="isSyncingStatus" class="waiting-note">等待管理页审核，当前插件在普通页面中只读。</p>
        </div>

        <div v-if="isEditing" class="panel-block">
          <strong class="section-title">编辑信息</strong>
          <div class="form-group">
            <label>Blender 版本</label>
            <input v-model="editForm.blenderVersion" class="text-input" placeholder="请输入 Blender 版本" />
          </div>
          <div class="form-group">
            <label>夸克链接</label>
            <input v-model="editForm.quarkUrl" class="text-input" placeholder="请输入有效的夸克分享链接" />
          </div>
          <div class="form-group">
            <label>提取码</label>
            <input v-model="editForm.extractionCode" class="text-input" placeholder="请输入提取码" />
          </div>
          <div class="edit-tip">
            当前阶段只允许修改标题、描述、版本、夸克链接和提取码；上传后的本地文件不再替换。
          </div>
        </div>
      </div>

      <div class="modal-buttons">
        <template v-if="!isEditing">
          <button class="cancel-btn" @click="close">{{ isPendingLinkStatus ? '取消' : '关闭' }}</button>
          <button v-if="canEditStatus && !isPendingLinkStatus" class="edit-btn" @click="enterEditMode">编辑信息</button>
          <button class="delete-btn" @click="confirmDelete">删除插件</button>
        </template>
        <template v-else>
          <button class="cancel-edit-btn" @click="exitEditMode">取消编辑</button>
          <button class="save-btn" @click="saveDraft">保存</button>
          <button class="submit-btn" @click="submitReviewAction">{{ primaryActionLabel }}</button>
          <button class="delete-btn" @click="confirmDelete">删除插件</button>
        </template>
      </div>
    </div>
  </div>

  <QuarkSubmissionModal
    :visible="showQuarkModal"
    :resource="plugin"
    resource-type="plugin"
    :show-backdrop="false"
    @close="showQuarkModal = false"
    @saved="handleQuarkSaved"
  />
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import axios from 'axios'
import {
  canEditResource,
  extractFileName,
  getStatusMeta,
  hasCompleteReviewInfo,
  isPendingLink,
  isSyncFailed,
  isSynced,
  isSyncing,
  splitUrls
} from '../utils/resourceReview'
import { RESOURCE_SIZE_LABEL, formatResourceSize } from '../utils/sizeFormat'
import QuarkSubmissionModal from './QuarkSubmissionModal.vue'

const props = defineProps({
  visible: Boolean,
  plugin: Object
})

const emit = defineEmits(['close', 'delete', 'update'])

const syncStatus = computed(() => props.plugin?.syncStatus)
const statusMeta = computed(() => getStatusMeta(syncStatus.value))
const fileNames = computed(() => splitUrls(props.plugin?.fileUrls).map(extractFileName))
const canEditStatus = computed(() => canEditResource(syncStatus.value))
const isPendingLinkStatus = computed(() => isPendingLink(syncStatus.value))
const isSyncFailedStatus = computed(() => isSyncFailed(syncStatus.value))
const isSyncingStatus = computed(() => isSyncing(syncStatus.value))
const isSyncedStatus = computed(() => isSynced(syncStatus.value))

const isEditing = ref(false)
const showQuarkModal = ref(false)
const editForm = reactive({
  title: '',
  description: '',
  blenderVersion: '',
  quarkUrl: '',
  extractionCode: ''
})

const primaryActionLabel = computed(() => (isSyncFailedStatus.value ? '重新提交' : '提交审核'))

const formatSize = (sizeMB) => formatResourceSize(sizeMB)

const close = () => {
  if (isEditing.value) {
    exitEditMode()
  }
  emit('close')
}

const openQuarkSubmission = () => {
  showQuarkModal.value = true
}

const handleQuarkSaved = () => {
  showQuarkModal.value = false
  emit('update')
  close()
}

const enterEditMode = () => {
  isEditing.value = true
  editForm.title = props.plugin.title
  editForm.description = props.plugin.description || ''
  editForm.blenderVersion = props.plugin.blenderVersion || ''
  editForm.quarkUrl = props.plugin.quarkUrl || ''
  editForm.extractionCode = props.plugin.extractionCode || ''
}

const exitEditMode = () => {
  isEditing.value = false
  editForm.title = ''
  editForm.description = ''
  editForm.blenderVersion = ''
  editForm.quarkUrl = ''
  editForm.extractionCode = ''
}

const buildPayload = (action) => {
  const formData = new FormData()
  formData.append('id', props.plugin.id)
  formData.append('title', editForm.title)
  formData.append('description', editForm.description)
  formData.append('blenderVersion', editForm.blenderVersion)
  formData.append('quarkUrl', editForm.quarkUrl)
  formData.append('extractionCode', editForm.extractionCode)
  formData.append('action', action)
  return formData
}

const saveDraft = async () => {
  try {
    await axios.put('/api/plugins', buildPayload('save'), {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    alert('保存成功')
    emit('update')
    close()
  } catch (error) {
    console.error(error)
    alert('保存失败，请稍后重试')
  }
}

const submitReviewAction = async () => {
  if (!hasCompleteReviewInfo(editForm.quarkUrl, editForm.extractionCode)) {
    alert('提交审核前请填写有效的夸克链接和提取码')
    return
  }

  const action = isSyncFailedStatus.value ? 'resubmit' : 'submitReview'

  try {
    await axios.put('/api/plugins', buildPayload(action), {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    alert(action === 'resubmit' ? '已重新提交审核' : '已提交审核')
    emit('update')
    close()
  } catch (error) {
    console.error(error)
    alert('提交失败，请稍后重试')
  }
}

const confirmDelete = () => {
  if (confirm(`确定要删除插件“${props.plugin.title}”吗？此操作不可恢复。`)) {
    emit('delete', props.plugin.id)
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  user-select: none;
}

.modal-container {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.6), rgba(0, 0, 0, 0.6));
  backdrop-filter: blur(20px);
  border-radius: 34px;
  width: 860px;
  max-width: 92%;
  max-height: 90vh;
  overflow-y: auto;
  padding: 30px;
  color: white;
  box-shadow: 5px 5px 10px 2px rgb(151, 156, 153);
}

.modal-container h2 {
  margin-top: 0;
  text-align: center;
  font-size: 1.8rem;
}

.modal-body {
  margin: 20px 0;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
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

.status-text {
  color: rgba(255, 255, 255, 0.9);
}

.description-area {
  margin: 16px 0;
}

.description {
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.9);
}

.failure-box,
.panel-block {
  margin-top: 18px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.28);
}

.failure-box strong,
.section-title {
  display: block;
  margin-bottom: 8px;
}

.info {
  display: flex;
  justify-content: space-between;
  margin: 16px 0;
  font-size: 0.85rem;
  color: #fff;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 12px;
}

.plain-list {
  margin: 0;
  padding-left: 20px;
}

.plain-list li {
  margin: 6px 0;
}

.empty-text {
  color: rgba(255, 255, 255, 0.72);
  margin: 0;
}

.quark-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quark-content-row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 18px;
}

.supplement-action {
  display: flex;
  justify-content: flex-end;
}

.quark-row {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 12px;
  align-items: start;
}

.quark-label {
  color: rgba(255, 255, 255, 0.72);
}

.quark-value,
.quark-empty {
  word-break: break-all;
}

.quark-link {
  color: #fff066;
  word-break: break-all;
}

.waiting-note,
.edit-tip {
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 0.86rem;
}

.edit-title-input,
.edit-description-input,
.text-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  padding: 12px 16px;
  color: white;
  font-size: 1rem;
  resize: vertical;
  font-family: inherit;
}

.edit-title-input {
  font-size: 1.2rem;
  font-weight: bold;
}

.form-group {
  margin-bottom: 12px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 20px;
}

.modal-buttons button {
  padding: 8px 20px;
  border-radius: 40px;
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: 0.2s;
}

.cancel-btn,
.cancel-edit-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.delete-btn {
  background: #e74c3c;
  color: white;
}

.edit-btn {
  background: #2196f3;
  color: white;
}

.save-btn {
  background: #3f6fd5;
  color: white;
}

.submit-btn {
  background: #4caf50;
  color: white;
}

.supplement-btn {
  border: none;
  border-radius: 999px;
  padding: 12px 16px;
  background: #2196f3;
  color: white;
  cursor: pointer;
  font-weight: 700;
  transform: translateY(-10px);
}

.modal-buttons button:hover {
  transform: scale(1.02);
}
</style>
