/** In-memory only — cleared when leaving the shelf or locking. */
const unlockTokens = new Map<string, string>()

export function getSecretShelfUnlockToken(shelfId: string): string | undefined {
  return unlockTokens.get(shelfId)
}

export function setSecretShelfUnlockToken(shelfId: string, token: string) {
  unlockTokens.set(shelfId, token)
}

export function clearSecretShelfUnlockToken(shelfId: string) {
  unlockTokens.delete(shelfId)
}

export const SHELF_UNLOCK_HEADER = 'X-Shelf-Unlock'
