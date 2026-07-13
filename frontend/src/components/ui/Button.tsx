import { cn } from '../../lib/utils'
import { useComicBurst } from './ComicBurst'
import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

const variants: Record<Variant, string> = {
  primary:
    'bg-accent text-void font-semibold hover:bg-accent-hover border border-accent/50 shadow-sm',
  secondary:
    'bg-white/5 text-ink border border-white/10 hover:bg-white/10 hover:border-white/15',
  ghost: 'text-ink-muted hover:bg-white/5 hover:text-ink border border-transparent',
  danger: 'bg-danger/15 text-danger border border-danger/25 hover:bg-danger/25',
}

const sizes: Record<Size, string> = {
  sm: 'px-3 py-1.5 text-sm rounded-lg',
  md: 'px-4 py-2 text-sm rounded-xl',
  lg: 'px-6 py-3 text-base rounded-xl',
}

export function Button({
  className,
  variant = 'primary',
  size = 'md',
  onClick,
  disabled,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant
  size?: Size
}) {
  const { ref, handleClick } = useComicBurst(onClick, { disabled })

  return (
    <button
      ref={ref}
      className={cn(
        'comic-btn inline-flex items-center justify-center gap-2 font-medium transition-all duration-200',
        'disabled:opacity-40 disabled:pointer-events-none',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/50 focus-visible:ring-offset-2 focus-visible:ring-offset-void',
        variants[variant],
        sizes[size],
        className,
      )}
      onClick={handleClick}
      disabled={disabled}
      {...props}
    />
  )
}
