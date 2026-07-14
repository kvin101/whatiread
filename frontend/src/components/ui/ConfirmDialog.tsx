import { AlertTriangle, HelpCircle } from 'lucide-react'
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
        <div className="space-y-8">
          <div className="flex gap-4">
            <div
              className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl ${
                opts.variant === 'danger' ? 'bg-danger/15 text-danger' : 'bg-accent-dim text-accent'
              }`}
            >
              {opts.variant === 'danger' ? (
                <AlertTriangle className="h-6 w-6" />
              ) : (
                <HelpCircle className="h-6 w-6" />
              )}
            </div>
            {opts.description && (
              <p className="pt-1 text-base leading-relaxed text-ink-muted">{opts.description}</p>
            )}
          </div>
          <div className="flex flex-col-reverse gap-3 border-t border-border/60 pt-6 sm:flex-row sm:justify-end">
            <Button variant="secondary" className="sm:min-w-[7rem]" onClick={() => close(false)}>
              {opts.cancelLabel ?? copy.confirm.cancel}
            </Button>
            <Button
              variant={opts.variant === 'danger' ? 'danger' : 'primary'}
              className="sm:min-w-[7rem]"
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
