import { Link } from 'react-router-dom'
import { APP_ROUTES } from '../../api/paths'

export function authorSlug(name: string): string {
  if (!name.trim()) return 'item'
  const normalized = name
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
  return normalized || 'item'
}

export function AuthorLink({
  names,
  className,
}: {
  names: string[]
  className?: string
}) {
  if (!names.length) return null
  return (
    <span className={className}>
      {names.map((name, i) => (
        <span key={name}>
          {i > 0 && ', '}
          <Link
            to={APP_ROUTES.author(authorSlug(name))}
            className="text-accent hover:underline"
            onClick={(e) => e.stopPropagation()}
          >
            {name}
          </Link>
        </span>
      ))}
    </span>
  )
}
