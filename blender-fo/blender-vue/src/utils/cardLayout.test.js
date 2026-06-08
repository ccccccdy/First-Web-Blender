import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const cardPages = [
  {
    path: new URL('../views/Modeling.vue', import.meta.url),
    gridClass: 'works-grid',
    cardClass: 'work-card',
    descriptionField: 'work.description',
    titleSelector: 'work-info h3'
  },
  {
    path: new URL('../views/Assets.vue', import.meta.url),
    gridClass: 'assets-grid',
    cardClass: 'asset-card',
    descriptionField: 'asset.description',
    titleSelector: 'asset-info h3'
  },
  {
    path: new URL('../views/Plugins.vue', import.meta.url),
    gridClass: 'plugins-grid',
    cardClass: 'plugin-card',
    descriptionField: 'plugin.description',
    titleSelector: 'plugin-info h3'
  }
]

const readPage = (path) => readFileSync(path, 'utf8')

const getRule = (source, selector) => {
  const match = source.match(new RegExp(`\\.${selector}\\s*\\{([^}]*)\\}`, 'm'))
  return match?.[1] ?? ''
}

test('resource cards stay in four fixed columns and truncate long titles', () => {
  const cardHeights = []

  for (const page of cardPages) {
    const source = readPage(page.path)
    const gridRule = getRule(source, page.gridClass)
    const cardRule = getRule(source, page.cardClass)
    const statusRule = getRule(source, 'status-chip')
    const titleRule = getRule(source, page.titleSelector)
    const heightMatch = cardRule.match(/height:\s*(\d+px)/)

    assert.equal(source.includes(`<p>{{ ${page.descriptionField}`), false)
    assert.match(gridRule, /grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/)
    assert.ok(heightMatch)
    cardHeights.push(heightMatch[1])
    assert.match(cardRule, /min-width:\s*0/)
    assert.match(statusRule, /font-size:\s*1rem/)
    assert.match(titleRule, /white-space:\s*nowrap/)
    assert.match(titleRule, /overflow:\s*hidden/)
    assert.match(titleRule, /text-overflow:\s*ellipsis/)
  }

  assert.deepEqual([...new Set(cardHeights)], ['380px'])
})

test('plugin cards use the same preview-panel format as asset cards', () => {
  const assetSource = readPage(new URL('../views/Assets.vue', import.meta.url))
  const pluginSource = readPage(new URL('../views/Plugins.vue', import.meta.url))
  const assetPreviewRule = getRule(assetSource, 'asset-preview')
  const pluginPreviewRule = getRule(pluginSource, 'plugin-icon')
  const pluginInnerRule = getRule(pluginSource, 'card-inner')
  const pluginInfoRule = getRule(pluginSource, 'plugin-info')

  assert.match(pluginPreviewRule, /height:\s*200px/)
  assert.match(pluginPreviewRule, /overflow:\s*hidden/)
  assert.match(pluginPreviewRule, /background:\s*rgba\(0,\s*0,\s*0,\s*0\.35\)/)
  assert.equal(pluginInnerRule.includes('padding: 20px'), false)
  assert.equal(pluginInnerRule.includes('text-align: center'), false)
  assert.match(pluginInfoRule, /padding:\s*16px/)

  assert.match(assetPreviewRule, /height:\s*200px/)
})
