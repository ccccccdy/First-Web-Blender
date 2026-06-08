<template>
  <UploadResourceModal
    :visible="visible"
    :trigger-element="triggerElement"
    modal-title="上传资产"
    name-label="资产名称"
    name-placeholder="请输入资产名称"
    description-label="资产描述"
    description-placeholder="请输入资产描述"
    file-label="资产文件"
    add-file-label="添加资产文件"
    file-hint="至少上传 1 个资产文件"
    resource-type="asset"
    submit-url="/api/assets"
    file-field-name="assetFiles"
    @close="$emit('close')"
    @upload-success="$emit('upload-success')"
  />
</template>

<script setup>
import UploadResourceModal from './UploadResourceModal.vue'

defineProps({
  visible: Boolean,
  triggerElement: HTMLElement
})

defineEmits(['close', 'upload-success'])
</script>
