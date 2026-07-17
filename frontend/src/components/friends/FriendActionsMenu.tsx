import { useEffect, useRef, useState } from 'react'
import { MoreHorizontal } from 'lucide-react'
import { cn } from '../../lib/utils'

export function FriendActionsMenu({
  items,
  align = 'right',
}: {
  items: Array<{
    label: string
    onClick: () => void
    danger?: boolean
    disabled?: boolean
  }>
  align?: 'left' | 'right'
}) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onPointerDown = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [open])

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-label="More actions"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
        className="flex h-9 w-9 items-center justify-center rounded-full text-ink-muted transition-colors hover:bg-white/8 hover:text-ink"
      >
        <MoreHorizontal className="h-5 w-5" />
      </button>
      {open && (
        <div
          className={cn(
            'absolute top-full z-20 mt-1 min-w-[10rem] overflow-hidden rounded-xl border border-border bg-paper-elevated py-1 shadow-lg',
            align === 'right' ? 'right-0' : 'left-0',
          )}
        >
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              disabled={item.disabled}
              onClick={() => {
                setOpen(false)
                item.onClick()
              }}
              className={cn(
                'block w-full px-4 py-2.5 text-left text-sm transition-colors hover:bg-white/5 disabled:opacity-50',
                item.danger ? 'text-danger' : 'text-ink',
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
