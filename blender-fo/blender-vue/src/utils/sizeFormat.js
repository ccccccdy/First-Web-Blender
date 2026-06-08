export const RESOURCE_SIZE_LABEL = '文件大小'

export function formatResourceSize(sizeMB) {
  if (sizeMB == null || Number.isNaN(sizeMB)) {
    return '未知'
  }

  if (sizeMB <= 0) {
    return '0 KB'
  }

  if (sizeMB < 0.1) {
    return `${Math.max(1, Math.round(sizeMB * 1024))} KB`
  }

  return `${sizeMB.toFixed(1)} MB`
}
