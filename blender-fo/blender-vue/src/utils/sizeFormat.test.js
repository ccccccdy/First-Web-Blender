import test from 'node:test'
import assert from 'node:assert/strict'

import { RESOURCE_SIZE_LABEL, formatResourceSize } from './sizeFormat.js'

test('uses file size wording for packaged resource sizes', () => {
  assert.equal(RESOURCE_SIZE_LABEL, '文件大小')
})

test('formats tiny non-zero resource sizes as KB instead of 0.0 MB', () => {
  assert.equal(formatResourceSize(0.04), '41 KB')
})

test('formats normal resource sizes as MB', () => {
  assert.equal(formatResourceSize(12.34), '12.3 MB')
})

test('falls back to unknown for missing size', () => {
  assert.equal(formatResourceSize(null), '未知')
})
