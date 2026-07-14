import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Compass, Plus } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { shelvesApi } from '../api/shelves'
import type { ShelfVisibility } from '../api/types'
import { IconPicker } from '../components/shelves/IconPicker'
import { ShelfCard } from '../components/shelves/ShelfCard'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { FilterChips } from '../components/ui/FilterChips'
import { BookSkeletonGrid } from '../components/ui/BookLoader'
import { ListPageLayout } from '../components/layout/ListPageLayout'
import { PageHeader } from '../components/layout/PageHeader'
import { Input, Label } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Textarea } from '../components/ui/Textarea'
import { VISIBILITY_LABELS } from '../lib/constants'
import { copy } from '../lib/copy'
import { LayoutGrid } from 'lucide-react'
import { QUERY_KEYS } from '../lib/constants'
import { DEFAULT_SHELF_ICON } from '../lib/shelfIcons'
import { APP_ROUTES } from '../api/paths'
import { VisibilityPicker } from '../components/shelves/VisibilityPicker'
import { getApiErrorMessage } from '../lib/api'

type VisibilityFilter = 'ALL' | ShelfVisibility

const FILTER_TABS: VisibilityFilter[] = ['ALL', 'PUBLIC', 'FRIENDS', 'PRIVATE', 'SECRET']

export function ShelvesPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<VisibilityFilter>('ALL')
  const [createOpen, setCreateOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [icon, setIcon] = useState(DEFAULT_SHELF_ICON)
  const [visibility, setVisibility] = useState<ShelfVisibility>('PRIVATE')
  const [error, setError] = useState<string | null>(null)

  const { data: shelves = [], isLoading } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
  })

  const filtered = useMemo(() => {
    if (filter === 'ALL') return shelves
    return shelves.filter((s) => s.visibility === filter)
  }, [shelves, filter])

  const filterOptions = FILTER_TABS.map((tab) => ({
    value: tab,
    label: tab === 'ALL' ? 'All' : VISIBILITY_LABELS[tab],
  }))

  const createMutation = useMutation({
    mutationFn: () =>
      shelvesApi.create({
        name,
        description: description.trim() || undefined,
        icon,
        visibility,
      }),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
      setCreateOpen(false)
      setName('')
      setDescription('')
      setIcon(DEFAULT_SHELF_ICON)
      setVisibility('PRIVATE')
      setError(null)
      navigate(`${APP_ROUTES.shelf(created.id)}?addBooks=1`)
    },
    onError: (e) => setError(getApiErrorMessage(e, 'Shelf refused to be born.')),
  })

  return (
    <>
      <ListPageLayout
        toolbar={
          <>
            <PageHeader
              title={copy.shelves.title}
              action={
                <div className="flex flex-wrap gap-2">
                  <Link
                    to={APP_ROUTES.explore}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-border bg-paper-elevated px-4 py-2 text-sm font-medium text-ink shadow-sm hover:border-ink/20 transition-colors"
                  >
                    <Compass className="h-4 w-4" />
                    {copy.shelves.explore}
                  </Link>
                  <Button onClick={() => setCreateOpen(true)}>
                    <Plus className="h-4 w-4" />
                    {copy.shelves.newShelf}
                  </Button>
                </div>
              }
            />
            <div className="mt-4">
              <FilterChips options={filterOptions} value={filter} onChange={setFilter} label="Visibility" />
            </div>
          </>
        }
      >
        {isLoading && <BookSkeletonGrid count={6} />}

        {!isLoading && filtered.length === 0 && (
          <EmptyState
            icon={LayoutGrid}
            title={filter === 'ALL' ? copy.shelves.empty.title : `No ${VISIBILITY_LABELS[filter].toLowerCase()} shelves`}
            description={copy.shelves.empty.description}
            action={
              <Button onClick={() => setCreateOpen(true)}>
                <Plus className="h-4 w-4" />
                {copy.shelves.empty.cta}
              </Button>
            }
          />
        )}

        {!isLoading && filtered.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((shelf) => (
              <ShelfCard
                key={shelf.id}
                shelf={shelf}
                to={APP_ROUTES.shelf(shelf.id)}
                subtitle={VISIBILITY_LABELS[shelf.visibility]}
              />
            ))}
          </div>
        )}
      </ListPageLayout>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title={copy.shelves.create.title} wide>
        <div className="space-y-5">
          <div>
            <Label htmlFor="shelfName">Name</Label>
            <Input
              id="shelfName"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={copy.shelves.create.namePlaceholder}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="shelfDesc">Short note</Label>
            <Textarea
              id="shelfDesc"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={copy.shelves.create.descPlaceholder}
              className="mt-1"
            />
          </div>
          <div>
            <Label>Icon</Label>
            <div className="mt-2">
              <IconPicker value={icon} onChange={setIcon} />
            </div>
          </div>
          <div>
            <VisibilityPicker value={visibility} onChange={setVisibility} />
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button
            className="w-full"
            disabled={!name.trim() || createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            Create shelf
          </Button>
        </div>
      </Modal>
    </>
  )
}
