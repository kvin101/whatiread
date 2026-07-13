import { Zap } from 'lucide-react'
import type { ReactNode } from 'react'
import { copy } from '../../lib/copy'

export function AuthLayout({
  quote,
  children,
  centered = false,
}: {
  quote?: string
  children: ReactNode
  centered?: boolean
}) {
  return (
    <div className="flex min-h-screen min-h-[100dvh]">
      <div className="hidden flex-1 flex-col justify-between p-12 lg:flex relative overflow-hidden speed-lines">
        <div className="absolute inset-0 bg-gradient-to-br from-accent/25 via-transparent to-sage/10" />
        <div className="absolute inset-0 halftone-overlay opacity-40" />
        <div className="absolute inset-0 opacity-30">
          <div className="absolute top-1/4 left-1/4 h-64 w-64 rounded-full bg-accent/30 blur-[100px] animate-glow-pulse" />
          <div className="absolute bottom-1/4 right-1/4 h-48 w-48 rounded-full bg-sage/20 blur-[80px] animate-glow-pulse" />
        </div>
        <div className="relative flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent-dim border border-accent/20">
            <Zap className="h-6 w-6 text-accent" fill="currentColor" />
          </div>
          <span className="font-display text-2xl font-bold text-ink manga-title">{copy.brand.name}</span>
        </div>
        <blockquote className="relative max-w-lg">
          <p className="manga-title text-4xl leading-tight text-ink">
            {quote ?? copy.auth.login.quote}
          </p>
        </blockquote>
        <p className="relative text-xs text-ink-muted">{copy.brand.footnote}</p>
      </div>

      <div
        className={
          centered
            ? 'flex flex-1 items-center justify-center px-6 py-12 sm:px-12 speed-lines'
            : 'flex flex-1 flex-col justify-center px-6 py-12 sm:px-12 speed-lines'
        }
      >
        <div className="mx-auto w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <Zap className="h-6 w-6 text-accent" fill="currentColor" />
            <span className="font-display text-xl font-bold text-ink manga-title">{copy.brand.name}</span>
          </div>
          <div className="glass-strong manga-modal-panel rounded-2xl p-8 border border-white/10 lg:border-0 lg:bg-transparent lg:p-0 lg:backdrop-blur-none lg:shadow-none">
            {children}
          </div>
        </div>
      </div>
    </div>
  )
}
