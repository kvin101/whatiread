import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Shield, Trash2, UserPlus, UserX } from 'lucide-react'
import { useState } from 'react'
import { adminApi } from '../api/admin'
import type { AdminCreateUserRequest, AdminUser, AdminUserRole, AdminUserSuggestResult } from '../api/types'
import { UsernameAvailabilityHint } from '../components/ui/UsernameAvailabilityHint'
import { useUsernameAvailability } from '../hooks/useUsernameAvailability'
import { useAdminUserSuggest } from '../hooks/useAdminUserSuggest'
import { PageHeader } from '../components/layout/PageHeader'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { Button } from '../components/ui/Button'
import { SuggestField } from '../components/ui/SuggestField'
import { useConfirm } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { Input, Label } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Select } from '../components/ui/Select'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { displayName } from '../lib/utils'
import { getApiErrorMessage } from '../lib/api'

function StatusBadge({ user }: { user: AdminUser }) {
  if (!user.enabled) {
    return (
      <span className="inline-flex rounded-full border border-danger/30 bg-danger/15 px-2 py-0.5 text-xs font-medium text-danger">
        {copy.admin.statusBanned}
      </span>
    )
  }
  return (
    <span className="inline-flex rounded-full border border-sage/30 bg-sage/15 px-2 py-0.5 text-xs font-medium text-sage">
      {copy.admin.statusActive}
    </span>
  )
}

export function AdminUsersPage() {
  const queryClient = useQueryClient()
  const { confirm, dialog } = useConfirm()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [resetUser, setResetUser] = useState<AdminUser | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [createForm, setCreateForm] = useState({
    email: '',
    username: '',
    password: '',
    firstName: '',
    lastName: '',
    role: 'USER' as AdminUserRole,
  })
  const [resetPassword, setResetPassword] = useState('')
  const usernameCheck = useUsernameAvailability(createForm.username)

  const {
    data: adminSuggestions = [],
    isFetching: adminSuggestFetching,
    isError: adminSuggestError,
  } = useAdminUserSuggest(searchInput)

  const { data, isLoading } = useQuery({
    queryKey: QUERY_KEYS.admin.users(page, search),
    queryFn: () => adminApi.listUsers(page, 20, search),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })

  const createMutation = useMutation({
    mutationFn: (body: AdminCreateUserRequest) => adminApi.createUser(body),
    onSuccess: () => {
      invalidate()
      setCreateOpen(false)
      setCreateForm({ email: '', username: '', password: '', firstName: '', lastName: '', role: 'USER' })
      setError(null)
    },
    onError: (e: Error) => setError(getApiErrorMessage(e, 'Could not create user')),
  })

  const resetMutation = useMutation({
    mutationFn: ({ userId, password }: { userId: string; password: string }) =>
      adminApi.resetPassword(userId, password),
    onSuccess: () => {
      invalidate()
      setResetUser(null)
      setResetPassword('')
      setError(null)
    },
    onError: (e: Error) => setError(getApiErrorMessage(e, 'Could not reset password')),
  })

  const enabledMutation = useMutation({
    mutationFn: ({ userId, enabled }: { userId: string; enabled: boolean }) =>
      adminApi.setEnabled(userId, enabled),
    onSuccess: () => invalidate(),
  })

  const deleteMutation = useMutation({
    mutationFn: (userId: string) => adminApi.deleteUser(userId),
    onSuccess: () => invalidate(),
  })

  const users = data?.content ?? []

  const handleBanToggle = async (user: AdminUser) => {
    const name = displayName(user)
    if (user.enabled) {
      const ok = await confirm({
        title: copy.admin.banConfirm.title,
        description: copy.admin.banConfirm.description(name),
        confirmLabel: copy.admin.banConfirm.confirm,
        variant: 'danger',
      })
      if (ok) enabledMutation.mutate({ userId: user.id, enabled: false })
    } else {
      const ok = await confirm({
        title: copy.admin.unbanConfirm.title,
        description: copy.admin.unbanConfirm.description(name),
        confirmLabel: copy.admin.unbanConfirm.confirm,
      })
      if (ok) enabledMutation.mutate({ userId: user.id, enabled: true })
    }
  }

  const handleDelete = async (user: AdminUser) => {
    const ok = await confirm({
      title: copy.admin.deleteConfirm.title,
      description: copy.admin.deleteConfirm.description(displayName(user)),
      confirmLabel: copy.admin.deleteConfirm.confirm,
      variant: 'danger',
    })
    if (ok) deleteMutation.mutate(user.id)
  }

  return (
    <ScrollablePage>
    <div>
      <PageHeader
        title={copy.admin.title}
        description={copy.admin.description}
        action={
          <Button onClick={() => setCreateOpen(true)}>
            <UserPlus className="h-4 w-4" />
            {copy.admin.createUser}
          </Button>
        }
      />

      <form
        className="mt-4 flex flex-wrap gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          setPage(0)
          setSearch(searchInput.trim())
        }}
      >
        <SuggestField<AdminUserSuggestResult>
          value={searchInput}
          onValueChange={setSearchInput}
          suggestions={adminSuggestions}
          isFetching={adminSuggestFetching}
          isError={adminSuggestError}
          placeholder={copy.admin.searchPlaceholder}
          loadingLabel="Finding users…"
          emptyLabel="No matching users"
          getKey={(user) => user.id}
          onSelect={(user) => {
            setSearchInput(user.displayName)
            setPage(0)
            setSearch(user.displayName)
          }}
          renderItem={(user) => (
            <>
              <div className="min-w-0 flex-1">
                <p className="truncate text-base font-medium text-ink">{user.displayName}</p>
                <p className="truncate text-sm text-ink-muted">
                  @{user.username} · {user.email}
                </p>
              </div>
            </>
          )}
        />
        <Button type="submit" variant="secondary">
          Search
        </Button>
      </form>

      {isLoading && <BookLoaderCenter className="mt-12" />}

      {!isLoading && users.length === 0 && (
        <EmptyState
          className="mt-8"
          icon={Shield}
          title={copy.admin.empty.title}
          description={copy.admin.empty.description}
        />
      )}

      {!isLoading && users.length > 0 && (
        <div className="mt-8 overflow-x-auto rounded-2xl border border-border manga-panel">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-ink-muted">
                <th className="px-4 py-3 font-semibold">{copy.admin.columns.user}</th>
                <th className="px-4 py-3 font-semibold">{copy.admin.columns.role}</th>
                <th className="px-4 py-3 font-semibold">{copy.admin.columns.status}</th>
                <th className="px-4 py-3 font-semibold text-right">{copy.admin.columns.actions}</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} className="border-b border-border/60 last:border-0">
                  <td className="px-4 py-3">
                    <p className="font-medium text-ink">{displayName(user)}</p>
                    <p className="text-xs text-ink-muted">@{user.username} · {user.email}</p>
                  </td>
                  <td className="px-4 py-3 text-ink-muted">
                    {user.admin ? copy.admin.roleAdmin : copy.admin.roleUser}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge user={user} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap justify-end gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => {
                          setResetUser(user)
                          setResetPassword('')
                          setError(null)
                        }}
                      >
                        <KeyRound className="h-3.5 w-3.5" />
                        {copy.admin.resetPassword}
                      </Button>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => handleBanToggle(user)}
                        disabled={user.admin}
                      >
                        {user.enabled ? (
                          <>
                            <UserX className="h-3.5 w-3.5" />
                            {copy.admin.ban}
                          </>
                        ) : (
                          copy.admin.unban
                        )}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-danger hover:text-danger"
                        onClick={() => handleDelete(user)}
                        disabled={user.admin}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        {copy.admin.delete}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-ink-muted">
          <span>
            Page {data.number + 1} of {data.totalPages} ({data.totalElements} users)
          </span>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="secondary"
              disabled={data.first ?? page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="secondary"
              disabled={data.last ?? page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title={copy.admin.create.title}>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate({
              email: createForm.email.trim(),
              username: createForm.username.trim(),
              password: createForm.password,
              firstName: createForm.firstName.trim(),
              lastName: createForm.lastName.trim() || undefined,
              role: createForm.role,
            })
          }}
        >
          <div>
            <Label htmlFor="create-first">First name</Label>
            <Input
              id="create-first"
              required
              value={createForm.firstName}
              onChange={(e) => setCreateForm((f) => ({ ...f, firstName: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="create-last">Last name</Label>
            <Input
              id="create-last"
              value={createForm.lastName}
              onChange={(e) => setCreateForm((f) => ({ ...f, lastName: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="create-username">Username</Label>
            <Input
              id="create-username"
              required
              minLength={3}
              maxLength={30}
              pattern="[a-zA-Z][a-zA-Z0-9_]{2,29}"
              value={createForm.username}
              onChange={(e) => setCreateForm((f) => ({ ...f, username: e.target.value }))}
              className="mt-1"
            />
            <UsernameAvailabilityHint value={createForm.username} check={usernameCheck} />
          </div>
          <div>
            <Label htmlFor="create-email">Email</Label>
            <Input
              id="create-email"
              type="email"
              required
              value={createForm.email}
              onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="create-password">Password</Label>
            <Input
              id="create-password"
              type="password"
              required
              minLength={8}
              value={createForm.password}
              onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="create-role">Role</Label>
            <Select
              id="create-role"
              value={createForm.role}
              onChange={(e) => setCreateForm((f) => ({ ...f, role: e.target.value as AdminUserRole }))}
              className="mt-1"
            >
              <option value="USER">{copy.admin.roleUser}</option>
              <option value="ADMIN">{copy.admin.roleAdmin}</option>
            </Select>
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" disabled={createMutation.isPending || usernameCheck.data?.available === false}>
            {copy.admin.create.submit}
          </Button>
        </form>
      </Modal>

      <Modal
        open={!!resetUser}
        onClose={() => setResetUser(null)}
        title={copy.admin.reset.title}
      >
        {resetUser && (
          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              resetMutation.mutate({ userId: resetUser.id, password: resetPassword })
            }}
          >
            <p className="text-sm text-ink-muted">
              {copy.admin.reset.description(resetUser.email)}
            </p>
            <div>
              <Label htmlFor="reset-password">New password</Label>
              <Input
                id="reset-password"
                type="password"
                required
                minLength={8}
                value={resetPassword}
                onChange={(e) => setResetPassword(e.target.value)}
                className="mt-1"
              />
            </div>
            {error && <p className="text-sm text-danger">{error}</p>}
            <Button type="submit" disabled={resetMutation.isPending}>
              {copy.admin.reset.submit}
            </Button>
          </form>
        )}
      </Modal>

      {dialog}
    </div>
    </ScrollablePage>
  )
}
