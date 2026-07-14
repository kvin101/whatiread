import { apiUrl } from '../../api/client'
import { cn, initials } from '../../lib/utils'

type AvatarVariant = 'default' | 'danger' | 'pending'

const variantStyles: Record<AvatarVariant, string> = {
  default: 'bg-sage/15 text-sage ring-sage/20',
  danger: 'bg-danger/15 text-danger ring-danger/20',
  pending: 'bg-accent-dim text-accent ring-accent/20',
}

const sizeStyles = {
  sm: 'h-9 w-9 text-xs',
  md: 'h-11 w-11 text-sm',
  lg: 'h-20 w-20 text-xl',
  xl: 'h-24 w-24 text-2xl',
} as const

export function resolveAvatarUrl(avatarUrl?: string | null): string | undefined {
  if (!avatarUrl) return undefined
  if (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://')) return avatarUrl
  return apiUrl(avatarUrl)
}

export function UserAvatar({
  name,
  avatarUrl,
  size = 'md',
  variant = 'default',
  className,
}: {
  name: string
  avatarUrl?: string | null
  size?: keyof typeof sizeStyles
  variant?: AvatarVariant
  className?: string
}) {
  const src = resolveAvatarUrl(avatarUrl)
  const base = cn(
    'flex shrink-0 items-center justify-center overflow-hidden rounded-full font-semibold ring-2',
    sizeStyles[size],
    variantStyles[variant],
    className,
  )

  if (src) {
    return <img src={src} alt="" className={cn(base, 'object-cover')} />
  }

  return <div className={base}>{initials(name)}</div>
}
