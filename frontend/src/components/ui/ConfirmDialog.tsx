import { useCallback, useState } from 'react'
import { Button } from './Button'
import { Modal } from './Modal'
import { copy } from '../../lib/copy'

type ConfirmOptions = {
  title: string
  description?: string
  confirmLabel?: string
  cancelLabel?: string
  variant?: 'danger' | 'primary'
}

export function useConfirm() {
  const [open, setOpen] = useState(false)
  const [opts, setOpts] = useState<ConfirmOptions | null>(null)
  const [resolver, setResolver] = useState<((v: boolean) => void) | null>(null)

  const confirm = useCallback((options: ConfirmOptions) => {
    return new Promise<boolean>((resolve) => {
      setOpts(options)
      setResolver(() => resolve)
      setOpen(true)
    })
  }, [])

  const close = (result: boolean) => {
    setOpen(false)
    resolver?.(result)
    setResolver(null)
    setOpts(null)
  }

  const dialog =
    opts && open ? (
      <Modal open={open} onClose={() => close(false)} title={opts.title}>
        <div className="space-y-6">
          {opts.description && <p className="text-sm text-ink-muted leading-relaxed">{opts.description}</p>}
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" onClick={() => close(false)}>
              {opts.cancelLabel ?? copy.confirm.cancel}
            </Button>
            <Button
              variant={opts.variant === 'danger' ? 'danger' : 'primary'}
              onClick={() => close(true)}
            >
              {opts.confirmLabel ?? copy.confirm.delete}
            </Button>
          </div>
        </div>
      </Modal>
    ) : null

  return { confirm, dialog }
}
