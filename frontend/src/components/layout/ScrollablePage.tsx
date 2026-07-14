import type { ReactNode } from 'react'
import { cn } from '../../lib/utils'

/** Standard page wrapper — scrolls when content exceeds the viewport. */
export function ScrollablePage({
  children,
  className,
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <div className={cn('min-h-0 flex-1 overflow-y-auto overscroll-contain pr-1', className)}>
      {children}
    </div>
  )
}
