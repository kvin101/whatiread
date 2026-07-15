#!/usr/bin/env node
/**
 * Send a STOMP chat message over SockJS (used by scripts/smoke/ws.py).
 * Usage: node scripts/smoke-ws.mjs <baseUrl> <accessToken> <conversationId> <body>
 * Run from repo root; requires frontend/node_modules.
 */
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(join(dirname(fileURLToPath(import.meta.url)), '../frontend/package.json'))
const SockJS = require('sockjs-client')
const { Client } = require('@stomp/stompjs')

const [baseUrl, accessToken, conversationId, body] = process.argv.slice(2)
if (!baseUrl || !accessToken || !conversationId || !body) {
  console.error('usage: smoke-ws.mjs <baseUrl> <accessToken> <conversationId> <body>')
  process.exit(2)
}

const wsUrl = `${baseUrl.replace(/\/$/, '')}/ws`

const client = new Client({
  webSocketFactory: () => new SockJS(wsUrl),
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 0,
  debug: () => {},
})

const timeout = setTimeout(() => {
  console.error('ws-send timeout')
  client.deactivate()
  process.exit(1)
}, 15000)

client.onConnect = () => {
  client.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({ conversationId, body, mentions: [] }),
  })
  setTimeout(() => {
    clearTimeout(timeout)
    client.deactivate()
    process.exit(0)
  }, 1500)
}

client.onStompError = (frame) => {
  clearTimeout(timeout)
  console.error('stomp error:', frame.headers['message'] || frame.body)
  client.deactivate()
  process.exit(1)
}

client.onWebSocketError = (err) => {
  clearTimeout(timeout)
  console.error('websocket error:', err.message || err)
  client.deactivate()
  process.exit(1)
}

client.activate()
