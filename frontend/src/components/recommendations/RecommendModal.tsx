import { BookOpen, Layers, Search, UserPlus, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { Book, FriendSummary, RecommendationTargetType, Shelf } from '../../api/types'
import { copy } from '../../lib/copy'
import { cn, displayName, formatAuthors, initials } from '../../lib/utils'
import { BookCover } from '../books/BookCover'
import { Button } from '../ui/Button'
import { EmptyState } from '../ui/EmptyState'
import { Input, Label } from '../ui/Input'
import { Modal } from '../ui/Modal'
import { Textarea } from '../ui/Textarea'

const PAGE_SIZE = 50

export function RecommendModal({
  open,
  onClose,
  friends,
  libraryBooks,
  libraryLoading,
  libraryHasMore,
  onLoadMoreBooks,
  onBookSearchChange,
  bookSearch,
  shelves,
  shelvesLoading,
  onSubmit,
  pending,
  error,
}: {
  open: boolean
  onClose: () => void
  friends: FriendSummary[]
  libraryBooks: Book[]
  libraryLoading?: boolean
  libraryHasMore?: boolean
  onLoadMoreBooks?: () => void
  onBookSearchChange: (q: string) => void
  bookSearch: string
  shelves: Shelf[]
  shelvesLoading?: boolean
  onSubmit: (payload: {
    friendId: string
    targetType: RecommendationTargetType
    bookIds?: string[]
    shelfIds?: string[]
    message: string
  }) => void
  pending?: boolean
  error?: string | null
}) {
  const [friendSearch, setFriendSearch] = useState('')
  const [visibleFriends, setVisibleFriends] = useState(PAGE_SIZE)
  const [selectedFriend, setSelectedFriend] = useState<FriendSummary | null>(null)
  const [targetType, setTargetType] = useState<RecommendationTargetType>('BOOK')
  const [selectedBooks, setSelectedBooks] = useState<Book[]>([])
  const [selectedShelves, setSelectedShelves] = useState<Shelf[]>([])
  const [shelfSearch, setShelfSearch] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!open) {
      setFriendSearch('')
      setVisibleFriends(PAGE_SIZE)
      setSelectedFriend(null)
      setTargetType('BOOK')
      setSelectedBooks([])
      setSelectedShelves([])
      setShelfSearch('')
      setMessage('')
    }
  }, [open])

  const filteredFriends = useMemo(() => {
    const q = friendSearch.trim().toLowerCase()
    const sorted = [...friends].sort((a, b) => displayName(a).localeCompare(displayName(b)))
    if (!q) return sorted
    return sorted.filter((f) => {
      const name = displayName(f).toLowerCase()
      return name.includes(q) || f.email.toLowerCase().includes(q)
    })
  }, [friends, friendSearch])

  const visibleFriendList = filteredFriends.slice(0, visibleFriends)
  const friendsHasMore = visibleFriends < filteredFriends.length
  const selectedBookIds = useMemo(() => new Set(selectedBooks.map((b) => b.id)), [selectedBooks])
  const selectedShelfIds = useMemo(() => new Set(selectedShelves.map((s) => s.id)), [selectedShelves])

  const availableBooks = useMemo(
    () => libraryBooks.filter((b) => !selectedBookIds.has(b.id)),
    [libraryBooks, selectedBookIds],
  )

  const filteredShelves = useMemo(() => {
    const q = shelfSearch.trim().toLowerCase()
    const sorted = [...shelves].sort((a, b) => a.name.localeCompare(b.name))
    if (!q) return sorted
    return sorted.filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        (s.description?.toLowerCase().includes(q) ?? false),
    )
  }, [shelves, shelfSearch])

  const availableShelves = useMemo(
    () => filteredShelves.filter((s) => !selectedShelfIds.has(s.id)),
    [filteredShelves, selectedShelfIds],
  )

  const addBook = (book: Book) => {
    if (selectedBookIds.has(book.id)) return
    setSelectedBooks((prev) => [...prev, book])
  }

  const removeBook = (bookId: string) => {
    setSelectedBooks((prev) => prev.filter((b) => b.id !== bookId))
  }

  const addShelf = (shelf: Shelf) => {
    if (selectedShelfIds.has(shelf.id)) return
    setSelectedShelves((prev) => [...prev, shelf])
  }

  const removeShelf = (shelfId: string) => {
    setSelectedShelves((prev) => prev.filter((s) => s.id !== shelfId))
  }

  const selectionCount = targetType === 'BOOK' ? selectedBooks.length : selectedShelves.length
  const canSubmit = !!selectedFriend && selectionCount > 0 && !pending

  return (
    <Modal open={open} onClose={onClose} title={copy.recommendations.modal.title}>
      <div className="space-y-5">
        <div>
          <Label>Who gets peer-pressured?</Label>
          {selectedFriend ? (
            <div className="mt-2 flex items-center gap-3 rounded-2xl border border-accent/30 bg-accent-dim px-4 py-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-sage/15 text-sm font-semibold text-sage">
                {initials(displayName(selectedFriend))}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium text-ink">{displayName(selectedFriend)}</p>
                <p className="truncate text-xs text-ink-muted">{selectedFriend.email}</p>
              </div>
              <Button
                type="button"
                size="sm"
                variant="secondary"
                disabled={pending}
                onClick={() => setSelectedFriend(null)}
              >
                Change
              </Button>
            </div>
          ) : friends.length === 0 ? (
            <EmptyState
              icon={UserPlus}
              title="No friends yet"
              description="Add friends first — then you can peer-pressure them about books."
              className="mt-3 py-8"
            />
          ) : (
            <div className="mt-2">
              <div className="relative mb-3">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
                <Input
                  type="search"
                  placeholder="Search friends by name or email…"
                  value={friendSearch}
                  onChange={(e) => {
                    setFriendSearch(e.target.value)
                    setVisibleFriends(PAGE_SIZE)
                  }}
                  className="pl-9"
                  autoFocus
                />
              </div>
              {filteredFriends.length === 0 ? (
                <EmptyState
                  icon={Search}
                  title="No matches"
                  description="Try a different name or email."
                  className="py-8"
                />
              ) : (
                <>
                  <ul className="max-h-40 space-y-2 overflow-y-auto">
                    {visibleFriendList.map((f) => {
                      const name = displayName(f)
                      return (
                        <li key={f.id}>
                          <button
                            type="button"
                            disabled={pending}
                            onClick={() => setSelectedFriend(f)}
                            className="flex w-full items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3 text-left transition-colors hover:border-accent/30 hover:bg-accent-dim disabled:opacity-50"
                          >
                            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-sage/15 text-xs font-semibold text-sage">
                              {initials(name)}
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-medium text-ink">{name}</p>
                              <p className="truncate text-xs text-ink-muted">{f.email}</p>
                            </div>
                          </button>
                        </li>
                      )
                    })}
                  </ul>
                  {friendsHasMore && (
                    <Button
                      variant="secondary"
                      size="sm"
                      className="mt-2 w-full"
                      onClick={() => setVisibleFriends((n) => n + PAGE_SIZE)}
                    >
                      Load more ({filteredFriends.length - visibleFriends} remaining)
                    </Button>
                  )}
                </>
              )}
            </div>
          )}
        </div>

        {selectedFriend && (
          <>
            <div className="flex gap-2 rounded-xl border border-white/10 bg-white/[0.03] p-1">
              <button
                type="button"
                disabled={pending}
                onClick={() => setTargetType('BOOK')}
                className={cn(
                  'flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  targetType === 'BOOK'
                    ? 'bg-accent-dim text-accent'
                    : 'text-ink-muted hover:text-ink',
                )}
              >
                <BookOpen className="h-4 w-4" />
                Book
              </button>
              <button
                type="button"
                disabled={pending}
                onClick={() => setTargetType('SHELF')}
                className={cn(
                  'flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  targetType === 'SHELF'
                    ? 'bg-accent-dim text-accent'
                    : 'text-ink-muted hover:text-ink',
                )}
              >
                <Layers className="h-4 w-4" />
                Shelf
              </button>
            </div>

            {targetType === 'BOOK' ? (
              <div>
                <Label>Which books from your pile?</Label>
                <p className="mt-1 text-xs text-ink-muted">
                  Pick one or more — same pitch goes to all of them.
                </p>

                {selectedBooks.length > 0 && (
                  <ul className="mt-3 flex flex-wrap gap-2">
                    {selectedBooks.map((book) => (
                      <li key={book.id}>
                        <span className="inline-flex max-w-full items-center gap-2 rounded-full border border-accent/30 bg-accent-dim py-1 pl-1 pr-2 text-sm text-ink">
                          <BookCover title={book.title} coverUrl={book.coverUrl} size="sm" className="!h-8 !w-6 rounded" />
                          <span className="truncate max-w-[10rem] font-medium">{book.title}</span>
                          <button
                            type="button"
                            disabled={pending}
                            onClick={() => removeBook(book.id)}
                            className="rounded-full p-0.5 text-ink-muted transition-colors hover:bg-white/10 hover:text-ink"
                            aria-label={`Remove ${book.title}`}
                          >
                            <X className="h-3.5 w-3.5" />
                          </button>
                        </span>
                      </li>
                    ))}
                  </ul>
                )}

                <div className="relative mt-3">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
                  <Input
                    type="search"
                    placeholder="Search your library by title or author…"
                    value={bookSearch}
                    onChange={(e) => onBookSearchChange(e.target.value)}
                    className="pl-9"
                  />
                </div>

                {libraryLoading && availableBooks.length === 0 ? (
                  <p className="mt-3 text-sm text-ink-muted">Loading your pile…</p>
                ) : availableBooks.length === 0 ? (
                  <EmptyState
                    icon={BookOpen}
                    title={bookSearch.trim() ? 'No matches' : 'No more books to add'}
                    description={
                      bookSearch.trim()
                        ? 'Try a different title or author.'
                        : selectedBooks.length > 0
                          ? 'You have selected every book in this view.'
                          : 'Add books to your library first.'
                    }
                    className="py-8"
                  />
                ) : (
                  <>
                    <ul className="mt-3 max-h-48 space-y-2 overflow-y-auto">
                      {availableBooks.map((book) => (
                        <li key={book.id}>
                          <button
                            type="button"
                            disabled={pending}
                            onClick={() => addBook(book)}
                            className="flex w-full items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.03] px-3 py-2.5 text-left transition-colors hover:border-accent/30 hover:bg-accent-dim disabled:opacity-50"
                          >
                            <BookCover title={book.title} coverUrl={book.coverUrl} size="sm" />
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-medium text-ink">{book.title}</p>
                              <p className="truncate text-xs text-ink-muted">{formatAuthors(book.authors)}</p>
                            </div>
                          </button>
                        </li>
                      ))}
                    </ul>
                    {libraryHasMore && onLoadMoreBooks && (
                      <Button
                        variant="secondary"
                        size="sm"
                        className="mt-2 w-full"
                        disabled={libraryLoading}
                        onClick={onLoadMoreBooks}
                      >
                        {libraryLoading ? 'Loading…' : 'Load more books'}
                      </Button>
                    )}
                  </>
                )}
              </div>
            ) : (
              <div>
                <Label>Which shelf are you sharing?</Label>
                <p className="mt-1 text-xs text-ink-muted">
                  Pick one or more of your shelves — they can clone them into their library.
                </p>

                {selectedShelves.length > 0 && (
                  <ul className="mt-3 flex flex-wrap gap-2">
                    {selectedShelves.map((shelf) => (
                      <li key={shelf.id}>
                        <span className="inline-flex max-w-full items-center gap-2 rounded-full border border-accent/30 bg-accent-dim py-1 pl-2 pr-2 text-sm text-ink">
                          <span className="text-base">{shelf.icon ?? '📚'}</span>
                          <span className="truncate max-w-[10rem] font-medium">{shelf.name}</span>
                          <button
                            type="button"
                            disabled={pending}
                            onClick={() => removeShelf(shelf.id)}
                            className="rounded-full p-0.5 text-ink-muted transition-colors hover:bg-white/10 hover:text-ink"
                            aria-label={`Remove ${shelf.name}`}
                          >
                            <X className="h-3.5 w-3.5" />
                          </button>
                        </span>
                      </li>
                    ))}
                  </ul>
                )}

                <div className="relative mt-3">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
                  <Input
                    type="search"
                    placeholder="Search your shelves…"
                    value={shelfSearch}
                    onChange={(e) => setShelfSearch(e.target.value)}
                    className="pl-9"
                  />
                </div>

                {shelvesLoading ? (
                  <p className="mt-3 text-sm text-ink-muted">Loading your shelves…</p>
                ) : availableShelves.length === 0 ? (
                  <EmptyState
                    icon={Layers}
                    title={shelfSearch.trim() ? 'No matches' : 'No more shelves to add'}
                    description={
                      shelfSearch.trim()
                        ? 'Try a different shelf name.'
                        : selectedShelves.length > 0
                          ? 'You have selected every shelf in this view.'
                          : 'Create a shelf first.'
                    }
                    className="py-8"
                  />
                ) : (
                  <ul className="mt-3 max-h-48 space-y-2 overflow-y-auto">
                    {availableShelves.map((shelf) => (
                      <li key={shelf.id}>
                        <button
                          type="button"
                          disabled={pending}
                          onClick={() => addShelf(shelf)}
                          className="flex w-full items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.03] px-3 py-2.5 text-left transition-colors hover:border-accent/30 hover:bg-accent-dim disabled:opacity-50"
                        >
                          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-accent-dim text-xl">
                            {shelf.icon ?? '📚'}
                          </span>
                          <div className="min-w-0 flex-1">
                            <p className="truncate font-medium text-ink">{shelf.name}</p>
                            <p className="truncate text-xs text-ink-muted">
                              {shelf.bookCount} {shelf.bookCount === 1 ? 'book' : 'books'}
                            </p>
                          </div>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </>
        )}

        {selectedFriend && selectionCount > 0 && (
          <div>
            <Label>Your pitch (optional but helps)</Label>
            <Textarea
              className="mt-1"
              rows={2}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder={copy.recommendations.modal.messagePlaceholder}
            />
          </div>
        )}

        {error && <p className="text-sm text-danger">{error}</p>}

        <div className="flex gap-2">
          <Button variant="secondary" className="flex-1" onClick={onClose} disabled={pending}>
            Cancel
          </Button>
          <Button
            className="flex-1"
            disabled={!canSubmit}
            onClick={() =>
              selectedFriend &&
              onSubmit({
                friendId: selectedFriend.id,
                targetType,
                bookIds: targetType === 'BOOK' ? selectedBooks.map((b) => b.id) : undefined,
                shelfIds: targetType === 'SHELF' ? selectedShelves.map((s) => s.id) : undefined,
                message,
              })
            }
          >
            {pending
              ? 'Sending…'
              : selectionCount > 1
                ? `Send ${selectionCount} recs`
                : copy.recommendations.modal.submit}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
