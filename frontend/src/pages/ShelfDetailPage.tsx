import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Link2, Pencil, Plus, Trash2, Users } from 'lucide-react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { shelvesApi } from '../api/shelves'
import type { ShelfBook, ShelfVisibility } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { BookCard } from '../components/books/BookCard'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { AddBooksToShelfModal } from '../components/shelves/AddBooksToShelfModal'
import { ShelfActivityFeed } from '../components/shelves/ShelfActivityFeed'
import { CloneShelfDialog } from '../components/shelves/CloneShelfDialog'
import { EditShelfModal } from '../components/shelves/EditShelfModal'
import { ShelfSharingPanel } from '../components/shelves/ShelfSharingPanel'
import { ShelfVisibilityBadge } from '../components/shelves/ShelfCard'
import { ShelfIcon } from '../components/shelves/ShelfIcon'
import { VisibilityPicker } from '../components/shelves/VisibilityPicker'
import { CommentThread } from '../components/comments/CommentThread'
import { Button } from '../components/ui/Button'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { Drawer } from '../components/ui/Drawer'
import { copy } from '../lib/copy'
import { cn, initials } from '../lib/utils'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { ScrollablePage } from '../components/layout/ScrollablePage'

const ROLE_LABELS: Record<string, string> = {
  OWNER: 'Owner',
  ADMIN: 'Admin',
  EDITOR: 'Editor',
  VIEWER: 'Viewer',
}

type Tab = 'books' | 'updates' | 'sharing'

export function ShelfDetailPage() {
  const { shelfId } = useParams<{ shelfId: string }>()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [addOpen, setAddOpen] = useState(false)
  const [selectedBook, setSelectedBook] = useState<ShelfBook | null>(null)
  const [tab, setTab] = useState<Tab>('books')
  const [cloneOpen, setCloneOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [membersOpen, setMembersOpen] = useState(false)

  const { data: shelf } = useQuery({
    queryKey: QUERY_KEYS.shelves.detail(shelfId!),
    queryFn: () => shelvesApi.get(shelfId!),
    enabled: !!shelfId,
  })

  const { data: shelfBooks = [], refetch: refetchBooks, isLoading: booksLoading } = useQuery({
    queryKey: QUERY_KEYS.shelves.books(shelfId!),
    queryFn: () => shelvesApi.listBooks(shelfId!),
    enabled: !!shelfId,
  })

  const { data: members = [] } = useQuery({
    queryKey: QUERY_KEYS.shelves.members(shelfId!),
    queryFn: () => shelvesApi.listMembers(shelfId!),
    enabled: !!shelfId && !!shelf,
  })

  useEffect(() => {
    if (searchParams.get('addBooks') === '1') {
      setAddOpen(true)
      searchParams.delete('addBooks')
      setSearchParams(searchParams, { replace: true })
    }
  }, [searchParams, setSearchParams])

  const isOwner = shelf?.ownerId === user?.id
  const role = shelf?.currentUserRole
  const canEdit =
    isOwner || role === 'OWNER' || role === 'ADMIN' || role === 'EDITOR'
  const canManageMembers = isOwner || role === 'ADMIN'
  const canViewActivity = isOwner || role === 'ADMIN' || role === 'EDITOR'
  const readOnlyBookView = !!shelf && user?.id !== shelf.ownerId
  const showSharingTab =
    canManageMembers ||
    (shelf && (shelf.visibility === 'PRIVATE' || shelf.visibility === 'SECRET'))

  const maintainers = useMemo(() => {
    const ownerName = shelf?.ownerDisplayName ?? 'Owner'
    const ownerEntry = {
      id: shelf?.ownerId ?? '',
      name: ownerName,
      role: 'OWNER' as const,
    }
    const others = members
      .filter((m) => m.userId !== shelf?.ownerId)
      .map((m) => ({
        id: m.userId,
        name: m.displayName ?? 'Member',
        role: m.role,
      }))
    return [ownerEntry, ...others]
  }, [members, shelf?.ownerDisplayName, shelf?.ownerId])

  const updateVisibilityMutation = useMutation({
    mutationFn: (visibility: ShelfVisibility) =>
      shelvesApi.update(shelfId!, { visibility }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.detail(shelfId!) })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
    },
  })

  const removeBookMutation = useMutation({
    mutationFn: (userBookId: string) => shelvesApi.removeBook(shelfId!, userBookId),
    onSuccess: () => {
      refetchBooks()
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
    },
  })

  const backTo =
    shelf && !isOwner && shelf.ownerId ? APP_ROUTES.userProfile(shelf.ownerId) : APP_ROUTES.shelves

  if (!shelfId) return null

  const tabs: { id: Tab; label: string; show?: boolean }[] = [
    { id: 'books', label: 'Books' },
    { id: 'updates', label: 'Updates', show: canViewActivity },
    { id: 'sharing', label: 'Sharing', show: showSharingTab },
  ]

  return (
    <ScrollablePage>
    <div>
      <Link
        to={backTo}
        className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-accent mb-4"
      >
        <ArrowLeft className="h-4 w-4" />
        {isOwner ? 'All shelves' : 'Back to profile'}
      </Link>

      {shelf && (
        <header className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex gap-4">
            <ShelfIcon icon={shelf.icon} size="lg" className="rounded-2xl" />
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="font-display text-3xl font-bold text-ink">{shelf.name}</h1>
                <ShelfVisibilityBadge visibility={shelf.visibility} />
              </div>
              {shelf.description && (
                <p className="mt-2 text-ink-muted max-w-xl">{shelf.description}</p>
              )}
              <p className="mt-2 text-sm text-ink-muted">
                Created by{' '}
                <Link
                  to={APP_ROUTES.userProfile(shelf.ownerId)}
                  className="font-medium text-accent hover:underline"
                >
                  {shelf.ownerDisplayName ?? 'Reader'}
                </Link>
                {' · '}
                {shelfBooks.length} books
              </p>
              {maintainers.length > 0 && (
                <div className="mt-3 flex flex-wrap items-center gap-3">
                  <div className="flex -space-x-2" aria-hidden>
                    {maintainers.slice(0, 3).map((m) => (
                      <div
                        key={m.id}
                        title={m.name}
                        className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-paper bg-sage/15 text-xs font-semibold text-sage"
                      >
                        {initials(m.name)}
                      </div>
                    ))}
                    {maintainers.length > 3 && (
                      <div className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-paper bg-paper-elevated text-[10px] font-semibold text-ink-muted">
                        +{maintainers.length - 3}
                      </div>
                    )}
                  </div>
                  <span className="rounded-full bg-paper-elevated px-2.5 py-0.5 text-xs font-medium text-ink-muted">
                    {maintainers.length === 1 ? '1 person' : `${maintainers.length} people`}
                  </span>
                  <Button size="sm" variant="secondary" onClick={() => setMembersOpen(true)}>
                    <Users className="h-4 w-4" />
                    {canManageMembers ? 'Manage access' : 'Members'}
                  </Button>
                </div>
              )}
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {canManageMembers && (
              <Button size="sm" variant="secondary" onClick={() => setTab('sharing')}>
                <Link2 className="h-4 w-4" />
                Share
              </Button>
            )}
            {!isOwner && shelf && (
              <Button size="sm" variant="secondary" onClick={() => setCloneOpen(true)}>
                Clone to my shelves
              </Button>
            )}
            {canEdit && (
              <>
                {isOwner && (
                  <Button size="sm" variant="secondary" onClick={() => setEditOpen(true)}>
                    <Pencil className="h-4 w-4" />
                    Edit shelf
                  </Button>
                )}
                {isOwner && (
                  <VisibilityPicker
                    compact
                    value={shelf.visibility}
                    onChange={(visibility) => updateVisibilityMutation.mutate(visibility)}
                  />
                )}
                <Button onClick={() => setAddOpen(true)}>
                  <Plus className="h-4 w-4" />
                  Add books
                </Button>
              </>
            )}
          </div>
        </header>
      )}

      <div className="mt-6 flex gap-1 border-b border-border">
        {tabs
          .filter((t) => t.show !== false)
          .map((t) => (
            <button
              key={t.id}
              type="button"
              onClick={() => setTab(t.id)}
              className={cn(
                'px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors',
                tab === t.id
                  ? 'border-accent text-accent'
                  : 'border-transparent text-ink-muted hover:text-ink',
              )}
            >
              {t.label}
            </button>
          ))}
      </div>

      {tab === 'books' && (
        <>
          {booksLoading && <BookSkeletonGrid className="mt-8" />}
          {!booksLoading && (
          <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {shelfBooks.map((sb) => (
              <div key={sb.userBookId} className="relative group">
                <BookCard entry={sb.userBook} onClick={() => setSelectedBook(sb)} />
                {canEdit && (
                  <button
                    type="button"
                    className="absolute top-2 right-2 rounded-lg bg-paper-elevated/90 p-2 opacity-0 group-hover:opacity-100 transition-opacity shadow border border-border"
                    onClick={async (e) => {
                      e.stopPropagation()
                      const ok = await confirm({
                        title: 'Remove from shelf?',
                        description: 'Remove this book from the shelf?',
                        confirmLabel: copy.confirm.remove,
                        variant: 'danger',
                      })
                      if (ok) removeBookMutation.mutate(sb.userBookId)
                    }}
                    aria-label="Remove from shelf"
                  >
                    <Trash2 className="h-4 w-4 text-danger" />
                  </button>
                )}
              </div>
            ))}
          </div>
          )}

          {!booksLoading && shelfBooks.length === 0 && canEdit && (
            <div className="mt-8 rounded-2xl border border-dashed border-border p-8 text-center">
              <p className="text-ink-muted">This shelf has no books yet.</p>
              <Button className="mt-4" onClick={() => setAddOpen(true)}>
                <Plus className="h-4 w-4" />
                Add your first books
              </Button>
            </div>
          )}
        </>
      )}

      {tab === 'updates' && (
        <div className="mt-8">
          <ShelfActivityFeed shelfId={shelfId} />
        </div>
      )}

      {tab === 'sharing' && shelf && (
        <div className="mt-8">
          <ShelfSharingPanel
            shelfId={shelfId}
            ownerId={shelf.ownerId}
            canManage={canManageMembers}
          />
        </div>
      )}

      {shelf && tab === 'books' && (
        <div className="mt-12 max-w-xl">
          <h2 className="font-display text-lg font-semibold text-ink mb-4">Discussion</h2>
          <CommentThread targetType="SHELF" targetId={shelf.id} />
        </div>
      )}

      {canEdit && shelfId && (
        <AddBooksToShelfModal
          shelfId={shelfId}
          open={addOpen}
          onClose={() => setAddOpen(false)}
          onAdded={() => {
            refetchBooks()
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
            queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.events(shelfId!) })
          }}
        />
      )}

      {shelf && (
        <>
          <CloneShelfDialog shelf={shelf} open={cloneOpen} onClose={() => setCloneOpen(false)} />
          {isOwner && (
            <EditShelfModal
              shelf={shelf}
              open={editOpen}
              onClose={() => setEditOpen(false)}
              onDeleted={() => navigate(APP_ROUTES.shelves)}
            />
          )}
        </>
      )}

      <BookDetailDrawer
        userBookId={selectedBook?.userBook.id ?? null}
        viewEntry={readOnlyBookView ? selectedBook?.userBook : undefined}
        open={!!selectedBook}
        onClose={() => setSelectedBook(null)}
        onUpdated={() => refetchBooks()}
      />
      <Drawer
        open={membersOpen}
        onClose={() => setMembersOpen(false)}
        title={canManageMembers ? 'Manage access' : 'Shelf members'}
      >
        <ul className="space-y-2">
          {maintainers.map((m) => (
            <li
              key={m.id}
              className="flex items-center justify-between gap-3 rounded-2xl border border-border bg-paper-elevated px-3 py-2.5"
            >
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-sage/15 text-xs font-semibold text-sage">
                  {initials(m.name)}
                </div>
                {m.id ? (
                  <Link
                    to={APP_ROUTES.userProfile(m.id)}
                    className="truncate text-sm font-medium text-ink hover:text-accent"
                    onClick={() => setMembersOpen(false)}
                  >
                    {m.name}
                  </Link>
                ) : (
                  <span className="truncate text-sm font-medium text-ink">{m.name}</span>
                )}
              </div>
              <span className="shrink-0 rounded-full bg-paper px-2 py-0.5 text-xs font-medium text-ink-muted">
                {ROLE_LABELS[m.role] ?? m.role}
              </span>
            </li>
          ))}
        </ul>
        {canManageMembers && (
          <Button
            className="mt-4 w-full"
            onClick={() => {
              setMembersOpen(false)
              setTab('sharing')
            }}
          >
            Invite &amp; manage roles
          </Button>
        )}
      </Drawer>

      {dialog}
    </div>
    </ScrollablePage>
  )
}
