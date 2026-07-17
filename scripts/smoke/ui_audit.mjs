#!/usr/bin/env node
/**
 * Convenience launcher for browser E2E tests.
 * Prefer: cd frontend && npm run test:e2e
 */
import { spawnSync } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDir = join(dirname(fileURLToPath(import.meta.url)), '../../frontend')
const npmCmd = process.platform === 'win32' ? 'npm.cmd' : 'npm'

const args = process.argv.slice(2)
const headed = args.includes('--headed')
const npmArgs = ['run', headed ? 'test:e2e:headed' : 'test:e2e']

const result = spawnSync(npmCmd, npmArgs, {
  cwd: frontendDir,
  stdio: 'inherit',
  env: { ...process.env, ...(headed ? { E2E_VISIBLE: '1' } : {}) },
})

process.exit(result.status ?? 1)
