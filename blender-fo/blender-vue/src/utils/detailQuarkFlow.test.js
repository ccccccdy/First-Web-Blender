import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const detailModals = [
  {
    path: new URL('../components/ArtworkDetailModal.vue', import.meta.url),
    name: 'artwork',
    resourceType: 'artwork',
    deleteLabel: '删除作品'
  },
  {
    path: new URL('../components/AssetDetailModal.vue', import.meta.url),
    name: 'asset',
    resourceType: 'asset',
    deleteLabel: '删除资源'
  },
  {
    path: new URL('../components/PluginDetail.vue', import.meta.url),
    name: 'plugin',
    resourceType: 'plugin',
    deleteLabel: '删除插件'
  }
]

const readPage = (path) => readFileSync(path, 'utf8')

const getRule = (source, selector) => {
  const match = source.match(new RegExp(`\\.${selector}\\s*\\{([^}]*)\\}`, 'm'))
  return match?.[1] ?? ''
}

test('pending-link detail modals open a dedicated Quark supplement modal instead of edit mode', () => {
  for (const modal of detailModals) {
    const source = readPage(modal.path)

    assert.match(source, /<QuarkSubmissionModal/)
    assert.match(source, new RegExp(`resource-type="${modal.resourceType}"|:resource-type="'${modal.resourceType}'"`))
    assert.match(source, /:show-backdrop="false"/)
    assert.match(source, /@click="openQuarkSubmission"/)
    assert.match(source, /补充夸克信息/)
    assert.match(source, /<div class="quark-content-row">/)
    assert.match(source, /<div v-if="isPendingLinkStatus" class="supplement-action">/)
    assert.match(getRule(source, 'quark-content-row'), /grid-template-columns:\s*1fr\s+auto/)
    assert.match(getRule(source, 'quark-content-row'), /align-items:\s*center/)
    assert.match(getRule(source, 'supplement-action'), /justify-content:\s*flex-end/)
    assert.match(source, /v-if="canEditStatus && !isPendingLinkStatus"/)
    assert.match(source, /\{\{ isPendingLinkStatus \? '取消' : '关闭' \}\}/)
    assert.match(source, new RegExp(`<button class="delete-btn"[^>]*>${modal.deleteLabel}</button>`))
  }
})

test('Quark supplement modal can render without adding another backdrop', () => {
  const source = readPage(new URL('../components/QuarkSubmissionModal.vue', import.meta.url))

  assert.match(source, /showBackdrop/)
  assert.match(source, /without-backdrop/)
})
