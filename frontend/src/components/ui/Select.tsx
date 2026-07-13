import { cn } from '../../lib/utils'
import type { SelectHTMLAttributes } from 'react'

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-ink',
        'focus:border-accent/40 focus:outline-none focus:ring-2 focus:ring-accent/20',
        'disabled:opacity-50 transition-all',
        className,
      )}
      {...props}
    >
      {children}
    </select>
  )
}
