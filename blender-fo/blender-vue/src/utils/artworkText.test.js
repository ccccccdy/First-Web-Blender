import test from 'node:test'
import assert from 'node:assert/strict'

import { ARTWORK_LOCAL_INFO_TITLE } from './artworkText.js'

test('shows preview retention note inside the local info title', () => {
  assert.equal(ARTWORK_LOCAL_INFO_TITLE, '本地信息（预览图会一直保留在本地）')
})
