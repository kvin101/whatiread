import { useQuery } from '@tanstack/react-query'
import { NavLink, useLocation } from 'react-router-dom'
import {
  BookMarked,
  Compass,
  LayoutGrid,
  LogOut,
  MessageCircle,
  MoreHorizontal,
  Settings,
  Shield,
  ThumbsUp,
  Users,
  Zap,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { conversationsApi } from '../../api/conversations'
import { useChatConnectionState } from '../../chat/ChatProvider'
import { friendsApi } from '../../api/friends'
import { recommendationsApi } from '../../api/recommendations'
import { APP_ROUTES } from '../../api/paths'
import { useAuth } from '../../auth/AuthContext'
import { copy } from '../../lib/copy'
import { QUERY_KEYS } from '../../lib/constants'
import { cn, displayName } from '../../lib/utils'
import { UserAvatar } from '../ui/UserAvatar'
import { Button } from '../ui/Button'
import { triggerComicBurst, useComicBurst } from '../ui/ComicBurst'
import { PageTransition } from './PageTransition'

const primaryNav = [
  { to: APP_ROUTES.library, label: copy.nav.library, icon: BookMarked, short: 'Books' },
  { to: APP_ROUTES.shelves, label: copy.nav.shelves, icon: LayoutGrid, short: 'Shelves' },
  { to: APP_ROUTES.explore, label: copy.nav.explore, icon: Compass, short: 'Explore' },
  { to: APP_ROUTES.messages, label: copy.nav.messages, icon: MessageCircle, short: 'Chat' },
] as const

const moreNav = [
  { to: APP_ROUTES.friends, label: copy.nav.friends, icon: Users },
  { to: APP_ROUTES.recommendations, label: copy.nav.recommendations, icon: ThumbsUp },
  { to: APP_ROUTES.settings, label: copy.nav.settings, icon: Settings },
] as const

const adminNav = { to: APP_ROUTES.adminUsers, label: copy.nav.administration, icon: Shield } as const

function NavItem({
  to,
  label,
  icon: Icon,
  badge = 0,
  compact,
}: {
  to: string
  label: string
  icon: React.ComponentType<{ className?: string; strokeWidth?: number }>
  badge?: number
  compact?: boolean
}) {
  return (
    <NavLink
      to={to}
      onClick={(e) => compact && triggerComicBurst(e.currentTarget)}
      className={({ isActive }) =>
        cn(
          'group nav-link-glow relative flex items-center gap-3 rounded-xl font-medium transition-all duration-300',
          compact && 'comic-btn',
          compact ? 'flex-col gap-1 px-2 py-2 text-[10px]' : 'px-4 py-2.5 text-sm',
          isActive
            ? 'bg-white/[0.06] text-accent border-l-2 border-l-accent'
            : 'text-ink-muted hover:bg-white/5 hover:text-ink border-l-2 border-l-transparent',
        )
      }
    >
      {({ isActive }) => (
        <>
          {isActive && !compact && (
            <span className="sr-only">Current page</span>
          )}
          <span className="relative">
            <Icon className={cn(compact ? 'h-5 w-5' : 'h-5 w-5 shrink-0')} strokeWidth={isActive ? 2.25 : 1.75} />
            {badge > 0 && (
              <span className="absolute -right-1.5 -top-1.5 z-10 flex h-4 min-w-4 items-center justify-center rounded-full border border-void/50 bg-accent px-1 text-[10px] font-bold text-void">
                {badge > 9 ? '9+' : badge}
              </span>
            )}
          </span>
          <span className={cn(compact ? 'truncate max-w-[4rem]' : '')}>{compact ? label.split(' ')[0] : label}</span>
        </>
      )}
    </NavLink>
  )
}

export function AppShell() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [moreOpen, setMoreOpen] = useState(false)
  const chatConnected = useChatConnectionState()
  const name = user ? displayName(user) : ''

  const { data: unreadMessages = 0 } = useQuery({
    queryKey: QUERY_KEYS.conversations.unreadCount,
    queryFn: conversationsApi.unreadCount,
    enabled: !!user,
    refetchInterval: chatConnected ? false : 30_000,
    staleTime: 5_000,
  })

  const { data: pendingRecs = [] } = useQuery({
    queryKey: QUERY_KEYS.recommendations.inbox,
    queryFn: recommendationsApi.inbox,
    enabled: !!user,
    refetchInterval: chatConnected ? false : 20_000,
    staleTime: 5_000,
  })
  const unreadRecs = pendingRecs.length

  const { data: incomingFriendRequests = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.incoming,
    queryFn: friendsApi.listIncoming,
    enabled: !!user,
    refetchInterval: chatConnected ? false : 30_000,
    staleTime: 5_000,
  })
  const pendingFriendRequests = incomingFriendRequests.length

  useEffect(() => {
    const base = copy.brand.name
    const total = unreadMessages + unreadRecs + pendingFriendRequests
    document.title = total > 0 ? `(${total}) ${base}` : base
    return () => {
      document.title = base
    }
  }, [unreadMessages, unreadRecs, pendingFriendRequests])

  const badgeFor = (to: string) => {
    if (to === APP_ROUTES.messages) return unreadMessages
    if (to === APP_ROUTES.recommendations) return unreadRecs
    if (to === APP_ROUTES.friends) return pendingFriendRequests
    return 0
  }

  const moreActive = moreNav.some((n) => location.pathname.startsWith(n.to))
    || (user?.admin && location.pathname.startsWith(adminNav.to))

  const { ref: moreRef, handleClick: handleMoreClick } = useComicBurst(
    () => setMoreOpen((o) => !o),
  )

  return (
    <div className="flex h-[100dvh] overflow-hidden">
      <aside className="hidden h-full w-72 shrink-0 flex-col overflow-hidden border-r border-white/8 glass-strong halftone-overlay halftone-subtle lg:flex">
        <div className="shrink-0 border-b border-white/8 px-4 py-4">
          <Link
            to={APP_ROUTES.library}
            className="flex items-center gap-2.5 rounded-xl transition-opacity hover:opacity-90"
          >
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent-dim border border-accent/20">
              <Zap className="h-5 w-5 text-accent" fill="currentColor" />
            </div>
            <div className="min-w-0">
              <span className="font-display text-lg font-bold tracking-tight text-ink manga-title">{copy.brand.name}</span>
              <p className="text-[11px] text-ink-muted leading-tight mt-0.5 truncate">{copy.brand.tagline}</p>
            </div>
          </Link>
        </div>

        <nav className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-3 py-4">
          <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-widest text-ink-muted/70">Reading</p>
          <div className="space-y-0.5">
            {primaryNav.map(({ to, label, icon }) => (
              <NavItem key={to} to={to} label={label} icon={icon} badge={badgeFor(to)} />
            ))}
          </div>
          <p className="mt-5 px-3 pb-2 text-[10px] font-semibold uppercase tracking-widest text-ink-muted/70">Social</p>
          <div className="space-y-0.5">
            {moreNav.map(({ to, label, icon }) => (
              <NavItem key={to} to={to} label={label} icon={icon} badge={badgeFor(to)} />
            ))}
            {user?.admin && (
              <NavItem to={adminNav.to} label={adminNav.label} icon={adminNav.icon} />
            )}
          </div>
        </nav>

        <div className="shrink-0 border-t border-white/8 p-3">
          <Link
            to={APP_ROUTES.userProfile(user?.id ?? '')}
            className="flex items-center gap-3 rounded-xl px-3 py-2.5 transition-colors hover:bg-white/5"
          >
            <UserAvatar name={name} avatarUrl={user?.avatarUrl} size="sm" className="h-10 w-10 text-sm" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-ink">{name}</p>
              <p className="truncate text-xs text-ink-muted">
                {user?.username ? `@${user.username}` : copy.nav.profile}
              </p>
            </div>
          </Link>
          <Button variant="ghost" size="sm" className="mt-1 w-full justify-start text-ink-muted" onClick={() => logout()}>
            <LogOut className="h-4 w-4" />
            {copy.nav.signOut}
          </Button>
        </div>
      </aside>

      <div className="flex h-full min-w-0 flex-1 flex-col overflow-hidden pb-[calc(4.5rem+env(safe-area-inset-bottom))] lg:pb-0">
        <header className="shrink-0 flex items-center justify-between glass border-b border-accent/10 px-4 py-3 lg:hidden">
          <Link to={APP_ROUTES.library} className="flex items-center gap-2 hover:opacity-90">
            <Zap className="h-5 w-5 text-accent" fill="currentColor" />
            <span className="font-display text-lg font-bold tracking-tight manga-title">{copy.brand.name}</span>
          </Link>
          <div className="flex items-center gap-1">
            <Link
              to={APP_ROUTES.userProfile(user?.id ?? '')}
              className="rounded-xl p-1 transition-colors hover:bg-white/5"
              aria-label={copy.nav.profile}
            >
              <UserAvatar name={name} avatarUrl={user?.avatarUrl} size="sm" className="h-9 w-9 text-xs" />
            </Link>
            <Button variant="ghost" size="sm" onClick={() => logout()} aria-label={copy.nav.signOut}>
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </header>

        <main className="flex min-h-0 flex-1 flex-col overflow-hidden">
          <div className="mx-auto flex h-full min-h-0 w-full max-w-7xl flex-col p-3 md:p-5">
            <PageTransition />
          </div>
        </main>

        <nav className="fixed bottom-0 inset-x-0 z-40 glass-strong border-t border-white/10 pb-[env(safe-area-inset-bottom)] lg:hidden">
          <div className="flex items-stretch justify-around px-1 pt-1">
            {primaryNav.map(({ to, icon, short }) => (
              <NavItem
                key={to}
                to={to}
                label={short}
                icon={icon}
                badge={badgeFor(to)}
                compact
              />
            ))}
            <button
              ref={moreRef}
              type="button"
              onClick={handleMoreClick}
              className={cn(
                'flex flex-col items-center gap-1 rounded-xl px-3 py-2 text-[10px] font-medium transition-all comic-btn',
                moreActive || moreOpen ? 'text-accent bg-white/[0.06] border-l-2 border-l-accent' : 'text-ink-muted border-l-2 border-l-transparent',
              )}
            >
              <MoreHorizontal className="h-5 w-5" strokeWidth={moreActive ? 2.25 : 1.75} />
              More
            </button>
          </div>
        </nav>

        {moreOpen && (
          <>
            <button
              type="button"
              className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden"
              aria-label="Close menu"
              onClick={() => setMoreOpen(false)}
            />
            <div className="fixed bottom-[calc(4.5rem+env(safe-area-inset-bottom))] inset-x-3 z-50 glass-strong manga-modal-panel rounded-2xl border border-white/10 p-2 shadow-2xl animate-slide-up lg:hidden">
              {[...moreNav, ...(user?.admin ? [adminNav] : [])].map(({ to, label, icon: Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  onClick={(e) => {
                    triggerComicBurst(e.currentTarget)
                    setMoreOpen(false)
                  }}
                  className={({ isActive }) =>
                    cn(
                      'comic-btn relative flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-colors nav-link-glow',
                      isActive ? 'bg-white/[0.06] text-accent border-l-2 border-l-accent nav-link-active' : 'text-ink-muted hover:bg-white/5 border-l-2 border-l-transparent',
                    )
                  }
                >
                  <Icon className="h-5 w-5" />
                  {label}
                  {badgeFor(to) > 0 && (
                    <span className="relative z-10 ml-auto flex h-5 min-w-5 items-center justify-center rounded-full border border-void/50 bg-accent px-1.5 text-[10px] font-bold text-void">
                      {badgeFor(to) > 9 ? '9+' : badgeFor(to)}
                    </span>
                  )}
                </NavLink>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
