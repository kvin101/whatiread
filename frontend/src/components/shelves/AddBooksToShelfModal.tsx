import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { booksApi } from '../../api/books'
import { shelvesApi } from '../../api/shelves'
import type { BookSearchResult } from '../../api/types'
import { getApiErrorMessage } from '../../lib/api'
import { useBookSearch } from '../../hooks/useBookSearch'
import { BookSearchField } from '../books/BookSearchField'
import {
  bookSearchResultKey,
  BookSearchAddButton,
  BookSearchResultsList,
} from '../books/BookSearchResultsList'
import { Button } from '../ui/Button'
import { Modal } from '../ui/Modal'

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
  const [addedKeys, setAddedKeys] = useState<Set<string>>(new Set())
  const { query, setQuery, results, searching, error, setError, search, reset } =
    useBookSearch('Search failed')

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
      setAddedKeys((prev) => new Set(prev).add(bookSearchResultKey(book)))
      onAdded()
      setError(null)
    },
    onError: (e) => {
      setError(getApiErrorMessage(e, 'Could not add book to shelf'))
    },
  })

  const handleClose = () => {
    reset()
    setAddedKeys(new Set())
    onClose()
  }

  return (
    <Modal open={open} onClose={handleClose} title="Add books to shelf" wide>
      <p className="text-sm text-ink-muted mb-4">
        Type for suggestions from our catalog, then press Search for Open Library results. Books are
        added to your library automatically if needed.
      </p>
      <BookSearchField
        query={query}
        onQueryChange={setQuery}
        onSearch={search}
        searching={searching}
        enabled={open}
      />
      {error && <p className="mt-3 text-sm text-danger">{error}</p>}
      <BookSearchResultsList
        results={results}
        searching={searching}
        query={query}
        emptyMessage="No Open Library results — try another search."
        renderAction={(book) => (
          <BookSearchAddButton
            onAdd={() => addMutation.mutate(book)}
            pending={addMutation.isPending}
            added={addedKeys.has(bookSearchResultKey(book))}
          />
        )}
      />
      <div className="mt-6 flex justify-end">
        <Button variant="secondary" onClick={handleClose}>
          Done
        </Button>
      </div>
    </Modal>
  )
}
