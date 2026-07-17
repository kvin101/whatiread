import { Input, Label } from '../ui/Input'

export function SecretPinFields({
  pin,
  confirmPin,
  onPinChange,
  onConfirmPinChange,
  pinId = 'secretPin',
  confirmId = 'secretPinConfirm',
  title = 'Choose a 4-digit PIN. You will need it to open this shelf.',
  pinLabel = 'PIN',
  confirmLabel = 'Confirm PIN',
}: {
  pin: string
  confirmPin: string
  onPinChange: (value: string) => void
  onConfirmPinChange: (value: string) => void
  pinId?: string
  confirmId?: string
  title?: string
  pinLabel?: string
  confirmLabel?: string
}) {
  const pinMismatch = confirmPin.length === 4 && pin !== confirmPin

  return (
    <div className="space-y-3 rounded-xl border border-accent/25 bg-accent/5 p-4">
      <p className="text-sm text-ink">{title}</p>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <Label htmlFor={pinId}>{pinLabel}</Label>
          <Input
            id={pinId}
            type="password"
            inputMode="numeric"
            pattern="\d{4}"
            maxLength={4}
            autoComplete="off"
            value={pin}
            onChange={(e) => onPinChange(e.target.value.replace(/\D/g, '').slice(0, 4))}
            className="mt-1 tracking-[0.35em]"
            placeholder="••••"
          />
        </div>
        <div>
          <Label htmlFor={confirmId}>{confirmLabel}</Label>
          <Input
            id={confirmId}
            type="password"
            inputMode="numeric"
            pattern="\d{4}"
            maxLength={4}
            autoComplete="off"
            value={confirmPin}
            onChange={(e) => onConfirmPinChange(e.target.value.replace(/\D/g, '').slice(0, 4))}
            className="mt-1 tracking-[0.35em]"
            placeholder="••••"
          />
        </div>
      </div>
      {pinMismatch && <p className="text-sm text-danger">PINs do not match</p>}
    </div>
  )
}

export function isValidSecretPin(pin: string, confirmPin: string) {
  return /^\d{4}$/.test(pin) && pin === confirmPin
}
