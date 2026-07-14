import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Copy, LogIn } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { shelvesApi } from '../api/shelves'
import type { ShelfBook } from '../api/types'
import { APP_ROUTES } from '../api/paths'
import { useAuth } from '../auth/AuthContext'
import { BookCard } from '../components/books/BookCard'
import { BookDetailDrawer } from '../components/books/BookDetailDrawer'
import { ShelfVisibilityBadge } from '../components/shelves/ShelfCard'
import { ShelfIcon } from '../components/shelves/ShelfIcon'
import { Button } from '../components/ui/Button'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { QUERY_KEYS } from '../lib/constants'

export function SharedShelfPage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const { isAuthenticated, user } = useAuth()
  const queryClient = useQueryClient()
  const [selectedBook, setSelectedBook] = useState<ShelfBook | null>(null)
  const [cloneName, setCloneName] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: QUERY_KEYS.shelves.shared(token!),
    queryFn: () => shelvesApi.getShared(token!),
    enabled: !!token,
    retry: false,
  })

  const cloneMutation = useMutation({
    mutationFn: () =>
      shelvesApi.cloneFromShare(token!, {
        name: cloneName.trim() || `Copy of ${data?.shelf.name ?? 'shelf'}`,
        includeBooks: true,
        visibility: 'PRIVATE',
      }),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
      navigate(APP_ROUTES.shelf(created.id))
    },
  })

  if (!token) return null

  if (isLoading) {
    return (
      <div className="min-h-screen bg-paper px-4 py-10">
        <div className="mx-auto max-w-5xl">
          <BookSkeletonGrid />
        </div>
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper px-4">
        <div className="max-w-md text-center">
          <h1 className="font-display text-2xl font-bold text-ink">Link unavailable</h1>
          <p className="mt-2 text-ink-muted">
            This share link may have expired or been revoked.
          </p>
          <Button className="mt-6" onClick={() => navigate(APP_ROUTES.login)}>
            Sign in
          </Button>
        </div>
      </div>
    )
  }

  const { shelf, books } = data
  const isOwner = user?.id === shelf.ownerId
  const defaultCloneName = `Copy of ${shelf.name}`

  return (
    <div className="min-h-screen bg-paper">
      <header className="border-b border-border bg-paper-elevated">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-4">
          <Link to={APP_ROUTES.login} className="font-display text-lg font-bold text-accent">
            WhatIRead
          </Link>
          {!isAuthenticated ? (
            <Link
              to={APP_ROUTES.login}
              state={{ from: { pathname: APP_ROUTES.sharedShelf(token) } }}
              className="inline-flex items-center gap-2 rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-sm text-ink hover:bg-white/10"
            >
              <LogIn className="h-4 w-4" />
              Sign in
            </Link>
          ) : (
            <Link
              to={APP_ROUTES.library}
              className="inline-flex items-center gap-2 rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-sm text-ink hover:bg-white/10"
            >
              My library
            </Link>
          )}
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
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
                Shared by {shelf.ownerDisplayName ?? 'a reader'} · {books.length} books
              </p>
            </div>
          </div>

          {!isOwner && (
            <div className="rounded-2xl border border-border bg-paper-elevated p-4 space-y-3 w-full lg:w-80">
              <p className="text-sm font-medium text-ink">Clone this shelf</p>
              {isAuthenticated ? (
                <>
                  <input
                    type="text"
                    className="w-full rounded-xl border border-border bg-paper px-3 py-2 text-sm"
                    value={cloneName || defaultCloneName}
                    onChange={(e) => setCloneName(e.target.value)}
                    placeholder={defaultCloneName}
                  />
                  <Button
                    className="w-full"
                    onClick={() => cloneMutation.mutate()}
                    disabled={cloneMutation.isPending}
                  >
                    <Copy className="h-4 w-4" />
                    Clone to my shelves
                  </Button>
                  {cloneMutation.error && (
                    <p className="text-sm text-danger">{(cloneMutation.error as Error).message}</p>
                  )}
                </>
              ) : (
                <Link
                  to={APP_ROUTES.login}
                  state={{ from: { pathname: APP_ROUTES.sharedShelf(token) } }}
                  className="comic-btn inline-flex w-full items-center justify-center gap-2 rounded-xl bg-white/5 px-4 py-2 text-sm font-medium text-ink border border-white/10 hover:bg-white/10"
                >
                  <LogIn className="h-4 w-4" />
                  Sign in to clone
                </Link>
              )}
            </div>
          )}
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {books.map((sb) => (
            <BookCard key={sb.userBookId} entry={sb.userBook} onClick={() => setSelectedBook(sb)} />
          ))}
        </div>

        {books.length === 0 && (
          <p className="mt-8 text-center text-ink-muted">This shelf has no visible books yet.</p>
        )}
      </main>

      <BookDetailDrawer
        userBookId={selectedBook?.userBook.id ?? null}
        viewEntry={selectedBook?.userBook}
        open={!!selectedBook}
        onClose={() => setSelectedBook(null)}
      />
    </div>
  )
}
