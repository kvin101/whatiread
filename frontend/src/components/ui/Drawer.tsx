import { X } from 'lucide-react'
import { useEffect } from 'react'
import { lockBodyScroll, unlockBodyScroll } from '../../lib/bodyScrollLock'
import { cn } from '../../lib/utils'
import { Button } from './Button'

export function Drawer({
  open,
  onClose,
  title,
  children,
}: {
  open: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
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
    <div className="fixed inset-0 z-50 flex justify-end">
      <button
        type="button"
        className="absolute inset-0 bg-black/70 backdrop-blur-sm animate-fade-in halftone-overlay"
        aria-label="Close"
        onClick={onClose}
      />
      <aside
        role="dialog"
        aria-modal
        className={cn(
          'relative z-10 flex h-full w-full max-w-md flex-col glass-strong shadow-2xl',
          'animate-slide-in-right manga-drawer-panel halftone-overlay',
        )}
      >
        <div className="glow-line w-full shrink-0" />
        <div className="flex items-center justify-between border-b border-accent/15 px-6 py-4">
          <h2 className="font-display text-xl font-bold text-ink line-clamp-2 pr-4 manga-title">{title}</h2>
          <Button variant="ghost" size="sm" onClick={onClose} aria-label="Close">
            <X className="h-5 w-5" />
          </Button>
        </div>
        <div className="flex-1 overflow-y-auto p-6 overscroll-contain panel-text">{children}</div>
      </aside>
    </div>
  )
}
