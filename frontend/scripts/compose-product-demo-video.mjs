#!/usr/bin/env node
/**
 * Stitch one README-ready product demo from selected Playwright chapter clips.
 *
 * One clip per major journey (not all 29 tests). Order matches the user story:
 * welcome → tour → add book → library → author → shelves → explore → friends →
 * messages → recommendations → profile → sign out.
 */
import { readFile, writeFile, access } from 'node:fs/promises'
import { constants } from 'node:fs'
import { join, dirname, isAbsolute } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const RESULTS_JSON = join(ROOT, 'playwright-results.json')
const OUTPUT = join(ROOT, 'product-demo.webm')

/** Exact Playwright test titles — one representative clip per chapter. */
const DEMO_STORY = [
  'Visitor opens Sign in, then hops to Create account',
  'Priya visits every section from the sidebar',
  'Priya searches Open Library from Add book and closes the modal',
  'Priya reviews notes, comments, rating, and status in the book drawer',
  "Priya opens Arundhati Roy's author page from the drawer",
  'Priya manages Monsoon Reading List — tabs, edit, add books, members',
  'Priya browses public shelves and tries clone',
  'Priya searches friends, messages Arjun, and checks sent requests',
  'Priya starts a chat with Arjun and sends a note about The White Tiger',
  "Priya accepts Arjun's recommendation of The White Tiger",
  'Priya updates her writer bio on the account page',
  'Priya signs out and returns to the login page',
]

function collectTests(node, out = []) {
  if (!node || typeof node !== 'object') return out
  if (Array.isArray(node.specs)) {
    for (const spec of node.specs) {
      if (spec.title && spec.tests) {
        for (const test of spec.tests) {
          out.push({ title: spec.title, test })
        }
      }
    }
  }
  if (Array.isArray(node.suites)) {
    for (const suite of node.suites) collectTests(suite, out)
  }
  return out
}

function videoPathForTest(test) {
  for (const result of test.results ?? []) {
    for (const attachment of result.attachments ?? []) {
      if (attachment.name === 'video' && attachment.path) {
        return attachment.path
      }
    }
  }
  return null
}

async function resolveClipPaths() {
  await access(RESULTS_JSON, constants.R_OK)
  const report = JSON.parse(await readFile(RESULTS_JSON, 'utf8'))
  const indexed = new Map()
  for (const { title, test } of collectTests(report)) {
    const video = videoPathForTest(test)
    if (video) indexed.set(title, video)
  }

  const clipPaths = []
  console.log('Composing product demo from chapter clips:')
  for (let i = 0; i < DEMO_STORY.length; i++) {
    const title = DEMO_STORY[i]
    const relative = indexed.get(title)
    if (!relative) {
      throw new Error(`No video found for test: "${title}"`)
    }
    const absolute = isAbsolute(relative) ? relative : join(ROOT, relative)
    await access(absolute, constants.R_OK)
    clipPaths.push(absolute)
    console.log(`  ${String(i + 1).padStart(2, '0')}  ${title}`)
  }
  return clipPaths
}

function runFfmpeg(listFile) {
  const result = spawnSync(
    'ffmpeg',
    [
      '-y',
      '-f',
      'concat',
      '-safe',
      '0',
      '-i',
      listFile,
      '-c:v',
      'libvpx-vp9',
      '-crf',
      '32',
      '-b:v',
      '0',
      '-an',
      OUTPUT,
    ],
    { stdio: 'inherit' },
  )
  if (result.status !== 0) {
    throw new Error('ffmpeg failed to compose product-demo.webm')
  }
}

async function main() {
  const clipPaths = await resolveClipPaths()
  const listFile = join(ROOT, '.product-demo-concat.txt')
  const listBody = clipPaths.map((p) => `file '${p.replace(/'/g, "'\\''")}'`).join('\n')
  await writeFile(listFile, listBody, 'utf8')
  runFfmpeg(listFile)
  console.log(`\nWrote ${OUTPUT} (${clipPaths.length} chapters)`)
}

main().catch((err) => {
  console.error(err.message ?? err)
  process.exit(1)
})
