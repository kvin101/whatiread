import type { ReactNode } from 'react'

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string
  title: string
  description?: ReactNode
  action?: ReactNode
}) {
  return (
    <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between animate-slide-up mb-2 speed-lines rounded-2xl p-1">
      <div>
        {eyebrow && (
          <p className="section-header-manga mb-2">{eyebrow}</p>
        )}
        <h1 className="manga-title text-3xl md:text-4xl text-ink tracking-tight leading-tight">
          <span className="text-gradient">{title}</span>
        </h1>
        {description && (
          <div className="mt-3 text-ink-muted max-w-2xl panel-text prose-readable">{description}</div>
        )}
      </div>
      {action}
    </header>
  )
}
