import { useMutation } from '@tanstack/react-query'
import { Loader2, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { booksApi } from '../../api/books'
import { libraryApi } from '../../api/library'
import { ApiError } from '../../api/client'
import type { BookSearchResult } from '../../api/types'
import { formatAuthors } from '../../lib/utils'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Modal } from '../ui/Modal'
import { BookCover } from './BookCover'

export function BookSearchModal({
  open,
  onClose,
  onAdded,
}: {
  open: boolean
  onClose: () => void
  onAdded: () => void
}) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<BookSearchResult[]>([])
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const addMutation = useMutation({
    mutationFn: async (book: BookSearchResult) => {
      let bookId = book.id
      if (!bookId) {
        const created = await booksApi.createManual({
          title: book.title,
          authors: book.authors,
          pageCount: book.pageCount,
          isbn: book.isbn,
          coverUrl: book.coverUrl,
          externalId: book.externalId,
          source: book.source,
        })
        bookId = created.id
      }
      return libraryApi.add({ bookId })
    },
    onSuccess: () => {
      onAdded()
      onClose()
    },
    onError: (e) => {
      setError(e instanceof ApiError ? e.message : 'Could not add book')
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

  return (
    <Modal open={open} onClose={onClose} title="Add a book" wide>
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
        {results.map((book) => (
          <li
            key={book.id ?? `${book.title}-${book.externalId}`}
            className="flex items-center gap-3 rounded-xl border border-border p-3"
          >
            <BookCover title={book.title} coverUrl={book.coverUrl} size="sm" />
            <div className="min-w-0 flex-1">
              <p className="font-medium text-ink line-clamp-1">{book.title}</p>
              <p className="text-sm text-ink-muted">{formatAuthors(book.authors)}</p>
            </div>
            <Button
              size="sm"
              onClick={() => addMutation.mutate(book)}
              disabled={addMutation.isPending}
            >
              <Plus className="h-4 w-4" />
              Add
            </Button>
          </li>
        ))}
        {!searching && results.length === 0 && query && (
          <p className="text-center text-sm text-ink-muted py-8">No results. Try another query.</p>
        )}
      </ul>
    </Modal>
  )
}
