import test from 'node:test'
import assert from 'node:assert/strict'

import { NO_EXTRACTION_CODE, hasCompleteReviewInfo } from './resourceReview.js'

const validQuarkUrl = 'https://pan.quark.cn/s/abc123'

test('accepts resources that explicitly have no extraction code', () => {
  assert.equal(hasCompleteReviewInfo(validQuarkUrl, NO_EXTRACTION_CODE), true)
})

test('still rejects blank extraction code when not marked as none', () => {
  assert.equal(hasCompleteReviewInfo(validQuarkUrl, ''), false)
})
