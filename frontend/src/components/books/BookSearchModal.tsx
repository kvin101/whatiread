import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { booksApi } from '../../api/books'
import { libraryApi } from '../../api/library'
import type { BookSearchResult } from '../../api/types'
import { getApiErrorMessage } from '../../lib/api'
import { useBookSearch } from '../../hooks/useBookSearch'
import { BookSearchField } from './BookSearchField'
import {
  bookSearchResultKey,
  BookSearchAddButton,
  BookSearchResultsList,
} from './BookSearchResultsList'
import { Button } from '../ui/Button'
import { Modal } from '../ui/Modal'

export function BookSearchModal({
  open,
  onClose,
  onAdded,
}: {
  open: boolean
  onClose: () => void
  onAdded: () => void
}) {
  const [addedKeys, setAddedKeys] = useState<Set<string>>(new Set())
  const { query, setQuery, results, searching, error, setError, search, reset } =
    useBookSearch('Search failed')

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
    onSuccess: (_data, book) => {
      setAddedKeys((prev) => new Set(prev).add(bookSearchResultKey(book)))
      onAdded()
      setError(null)
    },
    onError: (e) => {
      setError(getApiErrorMessage(e, 'Could not add book'))
    },
  })

  const handleClose = () => {
    reset()
    setAddedKeys(new Set())
    onClose()
  }

  return (
    <Modal open={open} onClose={handleClose} title="Add a book" wide>
      <BookSearchField
        query={query}
        onQueryChange={setQuery}
        onSearch={search}
        searching={searching}
        enabled={open}
        autoFocus
      />
      {error && <p className="mt-4 text-base text-danger">{error}</p>}
      <BookSearchResultsList
        results={results}
        searching={searching}
        query={query}
        emptyMessage="No Open Library results. Try another query or press Search."
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
