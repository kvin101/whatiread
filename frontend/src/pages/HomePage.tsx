import { useQuery } from '@tanstack/react-query'
import { Compass, Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { libraryApi } from '../api/library'
import { shelvesApi } from '../api/shelves'
import { conversationsApi } from '../api/conversations'
import { friendsApi } from '../api/friends'
import { recommendationsApi } from '../api/recommendations'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { BookSearchModal } from '../components/books/BookSearchModal'
import { CurrentlyReadingHero } from '../components/home/CurrentlyReadingHero'
import { FriendActivityPreview } from '../components/home/FriendActivityPreview'
import { OnboardingChecklist } from '../components/home/OnboardingChecklist'
import { ReadingGoalRing } from '../components/home/ReadingGoalRing'
import { PendingRecommendationCards } from '../components/home/PendingRecommendationCards'
import { ReadingStatsPanel } from '../components/home/ReadingStatsPanel'
import { SocialPulseCards } from '../components/home/SocialPulseCards'
import { StreakBadge } from '../components/home/StreakBadge'
import { ShelfCard } from '../components/shelves/ShelfCard'
import { CloneShelfButton } from '../components/shelves/CloneShelfDialog'
import { Button } from '../components/ui/Button'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { useAuth } from '../auth/AuthContext'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { displayName } from '../lib/utils'

function greeting(): string {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

export function HomePage() {
  const { user } = useAuth()
  const [searchOpen, setSearchOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const name = user ? displayName(user) : 'Reader'

  const { data: readingPage, refetch: refetchReading } = useQuery({
    queryKey: QUERY_KEYS.library.reading,
    queryFn: () => libraryApi.list({ status: 'READING', size: 3, sort: 'UPDATED_DESC' }),
  })

  const { data: shelves = [] } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
  })

  const { data: explorePage } = useQuery({
    queryKey: QUERY_KEYS.shelves.exploreHint,
    queryFn: () => shelvesApi.explore(0, 6),
  })

  const { data: friends = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
  })

  const { data: unreadMessages = 0 } = useQuery({
    queryKey: QUERY_KEYS.conversations.unreadCount,
    queryFn: conversationsApi.unreadCount,
  })

  const { data: pendingRecs = [] } = useQuery({
    queryKey: QUERY_KEYS.recommendations.inbox,
    queryFn: recommendationsApi.inbox,
  })

  const { data: incomingRequests = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.incoming,
    queryFn: friendsApi.listIncoming,
  })

  const { data: librarySample } = useQuery({
    queryKey: [...QUERY_KEYS.library.all, 'count-hint'],
    queryFn: () => libraryApi.list({ size: 1 }),
  })

  const readingEntries = readingPage?.content ?? []
  const readingCount = readingPage?.totalElements ?? readingEntries.length
  const discoverShelves = explorePage?.content ?? []
  const recentShelves = shelves.slice(0, 3)

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-5xl space-y-8 pb-8">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="font-display text-2xl font-bold text-ink md:text-3xl">
              {greeting()}, {name.split(' ')[0]}
            </h1>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <ReadingGoalRing />
              <StreakBadge />
              {readingCount > 0 && (
                <Link
                  to={`${APP_ROUTES.library}?status=READING`}
                  className="inline-flex items-center gap-1 rounded-full bg-sage/15 px-3 py-1 text-sm font-medium text-sage hover:bg-sage/25"
                >
                  📖 Reading ({readingCount})
                </Link>
              )}
            </div>
          </div>
          <Button onClick={() => setSearchOpen(true)}>
            <Plus className="h-4 w-4" />
            Add book
          </Button>
        </header>

        <OnboardingChecklist
          hasBooks={(librarySample?.totalElements ?? 0) > 0}
          shelfCount={shelves.length}
          friendCount={friends.length}
        />

        <CurrentlyReadingHero
          entries={readingEntries}
          onOpen={setSelectedId}
          onUpdated={() => refetchReading()}
        />

        <section>
          <h2 className="mb-3 font-display text-lg font-semibold text-ink">Needs attention</h2>
          {pendingRecs.length > 0 && (
            <div className="mb-4">
              <PendingRecommendationCards recs={pendingRecs} />
            </div>
          )}
          <SocialPulseCards
            unreadMessages={unreadMessages}
            pendingRecs={pendingRecs.length}
            pendingFriendRequests={incomingRequests.length}
          />
        </section>

        <FriendActivityPreview />

        <ReadingStatsPanel />

        {recentShelves.length > 0 && (
          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-display text-lg font-semibold text-ink">Your shelves</h2>
              <Link to={APP_ROUTES.shelves} className="text-sm font-medium text-accent hover:underline">
                See all
              </Link>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {recentShelves.map((s) => (
                <ShelfCard key={s.id} shelf={s} to={APP_ROUTES.shelf(s.id)} />
              ))}
            </div>
          </section>
        )}

        {discoverShelves.length > 0 && (
          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="font-display text-lg font-semibold text-ink">Discover shelves</h2>
              <Link
                to={APP_ROUTES.explore}
                className="inline-flex items-center gap-1 text-sm font-medium text-accent hover:underline"
              >
                <Compass className="h-4 w-4" />
                View all
              </Link>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {discoverShelves.map((s) => (
                <ShelfCard
                  key={s.id}
                  shelf={s}
                  to={APP_ROUTES.shelf(s.id)}
                  subtitle={`${s.source === 'PUBLIC' ? 'Public' : s.source === 'FRIEND' ? 'Friend' : 'Shared'} · ${s.ownerDisplayName}`}
                  actions={<CloneShelfButton shelf={s} />}
                />
              ))}
            </div>
          </section>
        )}
      </div>

      <BookSearchModal open={searchOpen} onClose={() => setSearchOpen(false)} onAdded={() => refetchReading()} />
      <BookDetailDrawer
        userBookId={selectedId}
        open={!!selectedId}
        onClose={() => setSelectedId(null)}
        onUpdated={() => refetchReading()}
      />
    </ScrollablePage>
  )
}
