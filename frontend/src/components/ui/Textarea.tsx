import { forwardRef } from 'react'
import { cn } from '../../lib/utils'
import type { TextareaHTMLAttributes } from 'react'

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, ...props }, ref) {
  return (
    <textarea
      ref={ref}
      className={cn(
        'w-full resize-y rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-ink min-h-[88px]',
        'placeholder:text-ink-muted/50',
        'focus:border-accent/40 focus:outline-none focus:ring-2 focus:ring-accent/20',
        'transition-all duration-200',
        className,
      )}
      {...props}
    />
  )
},
)
