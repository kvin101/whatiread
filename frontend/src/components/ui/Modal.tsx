import { X } from 'lucide-react'
import { useEffect } from 'react'
import { lockBodyScroll, unlockBodyScroll } from '../../lib/bodyScrollLock'
import { cn } from '../../lib/utils'
import { Button } from './Button'

export function Modal({
  open,
  onClose,
  title,
  children,
  className,
  wide,
}: {
  open: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
  className?: string
  wide?: boolean
}) {
  useEffect(() => {
    if (!open) return
    lockBodyScroll()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      unlockBodyScroll()
      document.removeEventListener('keydown', onKey)
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        type="button"
        className="absolute inset-0 bg-black/75 backdrop-blur-md animate-fade-in"
        aria-label="Close"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal
        className={cn(
          'relative z-10 max-h-[90vh] w-full overflow-hidden glass-strong manga-modal-panel rounded-3xl animate-slide-up halftone-overlay',
          wide ? 'max-w-2xl' : 'max-w-lg',
          className,
        )}
      >
        <div className="glow-line w-full" />
        <div className="flex items-center justify-between border-b border-accent/15 px-6 py-4">
          <h2 className="font-display text-xl font-bold text-ink manga-title">{title}</h2>
          <Button variant="ghost" size="sm" onClick={onClose} aria-label="Close dialog">
            <X className="h-5 w-5" />
          </Button>
        </div>
        <div className="max-h-[calc(90vh-4rem)] overflow-y-auto p-6 overscroll-contain panel-text">{children}</div>
      </div>
    </div>
  )
}
