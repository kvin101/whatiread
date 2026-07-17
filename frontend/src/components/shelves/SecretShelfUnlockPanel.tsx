import { useState } from 'react'
import { Lock } from 'lucide-react'
import { shelvesApi } from '../../api/shelves'
import { setSecretShelfUnlockToken } from '../../lib/secretShelfUnlock'
import { Button } from '../ui/Button'
import { Input, Label } from '../ui/Input'
import { getApiErrorMessage } from '../../lib/api'

export function SecretShelfUnlockPanel({
  shelfName,
  shelfId,
  onUnlocked,
}: {
  shelfName: string
  shelfId: string
  onUnlocked: () => void
}) {
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    if (!/^\d{4}$/.test(pin)) {
      setError('Enter your 4-digit PIN')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const { unlockToken } = await shelvesApi.unlock(shelfId, pin)
      setSecretShelfUnlockToken(shelfId, unlockToken)
      onUnlocked()
    } catch (e) {
      setError(getApiErrorMessage(e, 'Incorrect PIN'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-md rounded-2xl border border-white/10 bg-paper-elevated/50 p-8 text-center">
      <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-accent/10 text-accent">
        <Lock className="h-7 w-7" />
      </div>
      <h2 className="font-display text-lg font-semibold text-ink">{shelfName}</h2>
      <p className="mt-2 text-sm text-ink-muted">This secret shelf is locked. Enter your PIN to view the books.</p>
      <div className="mt-6 text-left">
        <Label htmlFor="unlockPin">4-digit PIN</Label>
        <Input
          id="unlockPin"
          type="password"
          inputMode="numeric"
          maxLength={4}
          autoComplete="off"
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
          className="mt-1 tracking-[0.35em]"
          placeholder="••••"
          onKeyDown={(e) => {
            if (e.key === 'Enter') void submit()
          }}
        />
      </div>
      {error && <p className="mt-3 text-sm text-danger">{error}</p>}
      <Button className="mt-5 w-full" disabled={loading || pin.length !== 4} onClick={() => void submit()}>
        Unlock shelf
      </Button>
    </div>
  )
}
