import { apiFetch, apiUrl, getAccessToken } from './client'
import { API_PATHS, AUTH_HEADERS } from './paths'

export interface GoodreadsImportResult {
  rowsProcessed: number
  booksImported: number
  shelvesCreated: number
  skipped: number
  duplicatesSkipped: number
  errors: number
}

export const importExportApi = {
  importGoodreads(file: File) {
    const form = new FormData()
    form.append('file', file)
    return apiFetch<GoodreadsImportResult>(API_PATHS.import.goodreads, {
      method: 'POST',
      body: form,
    })
  },

  downloadLibrary(format: 'csv' | 'json') {
    const token = getAccessToken()
    const url = apiUrl(API_PATHS.export.library(format))
    return fetch(url, {
      headers: token ? { Authorization: AUTH_HEADERS.bearer(token) } : {},
    }).then(async (res) => {
      if (!res.ok) throw new Error('Export failed')
      const blob = await res.blob()
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = `library.${format}`
      a.click()
      URL.revokeObjectURL(a.href)
    })
  },
}
