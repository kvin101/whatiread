import { useMutation } from '@tanstack/react-query'
import { Check, Loader2, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { booksApi } from '../../api/books'
import { shelvesApi } from '../../api/shelves'
import { ApiError } from '../../api/client'
import type { BookSearchResult } from '../../api/types'
import { formatAuthors } from '../../lib/utils'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Modal } from '../ui/Modal'
import { BookCover } from '../books/BookCover'

export function AddBooksToShelfModal({
  shelfId,
  open,
  onClose,
  onAdded,
}: {
  shelfId: string
  open: boolean
  onClose: () => void
  onAdded: () => void
}) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<BookSearchResult[]>([])
  const [addedKeys, setAddedKeys] = useState<Set<string>>(new Set())
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const bookKey = (book: BookSearchResult) =>
    book.id ?? `${book.title}-${book.externalId ?? book.isbn ?? ''}`

  const addMutation = useMutation({
    mutationFn: async (book: BookSearchResult) => {
      if (book.id) {
        return shelvesApi.addBook(shelfId, { bookId: book.id })
      }
      const created = await booksApi.createManual({
        title: book.title,
        authors: book.authors,
        pageCount: book.pageCount,
        isbn: book.isbn,
        coverUrl: book.coverUrl,
        externalId: book.externalId,
        source: book.source,
      })
      return shelvesApi.addBook(shelfId, { bookId: created.id })
    },
    onSuccess: (_data, book) => {
      setAddedKeys((prev) => new Set(prev).add(bookKey(book)))
      onAdded()
      setError(null)
    },
    onError: (e) => {
      setError(e instanceof ApiError ? e.message : 'Could not add book to shelf')
    },
  })

  const search = async () => {
    if (!query.trim()) return
    setSearching(true)
    setError(null)
    try {
      const page = await booksApi.search(query.trim())
      setResults(page.content)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Search failed')
      setResults([])
    } finally {
      setSearching(false)
    }
  }

  const handleClose = () => {
    setQuery('')
    setResults([])
    setAddedKeys(new Set())
    setError(null)
    onClose()
  }

  return (
    <Modal open={open} onClose={handleClose} title="Add books to shelf" wide>
      <p className="text-sm text-ink-muted mb-4">
        Search and add books — they are added to your library automatically if needed. You can add
        multiple books without closing this dialog.
      </p>
      <div className="flex gap-2">
        <Input
          placeholder="Search by title, author, ISBN…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && search()}
        />
        <Button onClick={search} disabled={searching}>
          {searching ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
          Search
        </Button>
      </div>
      {error && <p className="mt-3 text-sm text-danger">{error}</p>}
      <ul className="mt-6 space-y-3 max-h-80 overflow-y-auto">
        {results.length === 0 && query.trim() && !searching && (
          <li className="text-sm text-ink-muted py-4 text-center">No results — try another search.</li>
        )}
        {results.map((book) => {
          const key = bookKey(book)
          const added = addedKeys.has(key)
          return (
            <li
              key={key}
              className="flex items-center gap-3 rounded-xl border border-border p-3"
            >
              <BookCover title={book.title} coverUrl={book.coverUrl} size="sm" />
              <div className="min-w-0 flex-1">
                <p className="font-medium text-ink line-clamp-1">{book.title}</p>
                <p className="text-sm text-ink-muted">{formatAuthors(book.authors)}</p>
              </div>
              <Button
                size="sm"
                variant={added ? 'secondary' : 'primary'}
                onClick={() => !added && addMutation.mutate(book)}
                disabled={added || addMutation.isPending}
              >
                {added ? (
                  <>
                    <Check className="h-4 w-4" />
                    Added
                  </>
                ) : (
                  <>
                    <Plus className="h-4 w-4" />
                    Add
                  </>
                )}
              </Button>
            </li>
          )
        })}
      </ul>
      <div className="mt-6 flex justify-end">
        <Button variant="secondary" onClick={handleClose}>
          Done
        </Button>
      </div>
    </Modal>
  )
}
