import { cn } from '../../lib/utils'
import type { InputHTMLAttributes } from 'react'

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-ink',
        'placeholder:text-ink-muted/50',
        'focus:border-accent/40 focus:outline-none focus:ring-2 focus:ring-accent/20 focus:bg-white/[0.07]',
        'transition-all duration-200',
        className,
      )}
      {...props}
    />
  )
}

export function Label({
  children,
  className,
  ...props
}: React.LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label
      className={cn('mb-1.5 block text-sm font-medium text-ink-muted', className)}
      {...props}
    >
      {children}
    </label>
  )
}
