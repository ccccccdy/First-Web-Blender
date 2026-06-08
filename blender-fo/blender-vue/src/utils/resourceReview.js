export const REVIEW_STATUS = Object.freeze({
  PENDING_LINK: 'PENDING_LINK',
  SYNCING: 'SYNCING',
  SYNCED: 'SYNCED',
  SYNC_FAILED: 'SYNC_FAILED'
})

export const RESOURCE_TYPES = Object.freeze({
  ARTWORK: 'artwork',
  ASSET: 'asset',
  PLUGIN: 'plugin'
})

export const NO_EXTRACTION_CODE = '无'

const STATUS_META = {
  [REVIEW_STATUS.PENDING_LINK]: {
    label: 'Pending Link',
    className: 'is-pending',
    description: '等待补充夸克链接和提取码'
  },
  [REVIEW_STATUS.SYNCING]: {
    label: 'Syncing',
    className: 'is-syncing',
    description: '等待管理页人工审核'
  },
  [REVIEW_STATUS.SYNCED]: {
    label: 'Synced',
    className: 'is-synced',
    description: '已通过审核，可直接使用网盘信息'
  },
  [REVIEW_STATUS.SYNC_FAILED]: {
    label: 'Sync Failed',
    className: 'is-failed',
    description: '审核未通过，需要修正后重新提交'
  }
}

const QUARK_HOSTS = new Set(['pan.quark.cn', 'drive.quark.cn'])

export function normalizeSyncStatus(status) {
  return status || REVIEW_STATUS.PENDING_LINK
}

export function getStatusMeta(status) {
  return STATUS_META[normalizeSyncStatus(status)] || STATUS_META[REVIEW_STATUS.PENDING_LINK]
}

export function isPendingLink(status) {
  return normalizeSyncStatus(status) === REVIEW_STATUS.PENDING_LINK
}

export function isSyncing(status) {
  return normalizeSyncStatus(status) === REVIEW_STATUS.SYNCING
}

export function isSynced(status) {
  return normalizeSyncStatus(status) === REVIEW_STATUS.SYNCED
}

export function isSyncFailed(status) {
  return normalizeSyncStatus(status) === REVIEW_STATUS.SYNC_FAILED
}

export function canEditResource(status) {
  return isPendingLink(status) || isSyncFailed(status)
}

export function canSubmitReview(status) {
  return isPendingLink(status) || isSyncFailed(status)
}

export function isValidQuarkUrl(url) {
  if (!url || !url.trim()) {
    return false
  }

  try {
    const parsed = new URL(url.trim())
    return QUARK_HOSTS.has(parsed.hostname)
  } catch {
    return false
  }
}

export function hasCompleteReviewInfo(quarkUrl, extractionCode) {
  return isValidQuarkUrl(quarkUrl) && Boolean(extractionCode && extractionCode.trim())
}

export function splitUrls(value) {
  if (!value) {
    return []
  }
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

export function extractFileName(url) {
  if (!url) {
    return ''
  }
  return url.substring(url.lastIndexOf('/') + 1)
}

export function getResourceTypeLabel(type) {
  switch (type) {
    case RESOURCE_TYPES.ARTWORK:
      return '建模'
    case RESOURCE_TYPES.ASSET:
      return '资产'
    case RESOURCE_TYPES.PLUGIN:
      return '插件'
    default:
      return '资源'
  }
}

export function getResourceEndpoint(type) {
  switch (type) {
    case RESOURCE_TYPES.ARTWORK:
      return '/api/artworks'
    case RESOURCE_TYPES.ASSET:
      return '/api/assets'
    case RESOURCE_TYPES.PLUGIN:
      return '/api/plugins'
    default:
      throw new Error(`Unsupported resource type: ${type}`)
  }
}
