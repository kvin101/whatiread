import type { ReactNode } from 'react'
import { cn } from '../../lib/utils'

/** Fixed toolbar + independently scrollable list/grid body. */
export function ListPageLayout({
  toolbar,
  children,
  className,
}: {
  toolbar: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex min-h-0 flex-1 flex-col overflow-hidden', className)}>
      <div className="shrink-0 border-b border-white/8 pb-3">{toolbar}</div>
      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain pt-4 pr-1">{children}</div>
    </div>
  )
}
