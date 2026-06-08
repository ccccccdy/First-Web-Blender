<template>
  <UploadResourceModal
    :visible="visible"
    :trigger-element="triggerElement"
    modal-title="上传插件"
    name-label="插件名称"
    name-placeholder="请输入插件名称"
    description-label="插件描述"
    description-placeholder="请输入插件描述"
    file-label="插件文件"
    add-file-label="添加插件文件"
    file-hint="至少上传 1 个插件文件"
    resource-type="plugin"
    submit-url="/api/plugins"
    file-field-name="pluginFiles"
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
