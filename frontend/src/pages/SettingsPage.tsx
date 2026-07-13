import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Download, Shield, Target, Upload, User } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { accountApi } from '../api/account'
import { adminApi } from '../api/admin'
import { goalsApi } from '../api/goals'
import { importExportApi, type GoodreadsImportResult } from '../api/importExport'
import { setupApi } from '../api/setup'
import { useAuth } from '../auth/AuthContext'
import { saveStoredAuth, loadStoredAuth } from '../auth/storage'
import { Button } from '../components/ui/Button'
import { Input, Label } from '../components/ui/Input'
import { Textarea } from '../components/ui/Textarea'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

const CURRENT_YEAR = new Date().getFullYear()

export function SettingsPage() {
  const queryClient = useQueryClient()
  const { user, refreshUser } = useAuth()
  const [target, setTarget] = useState('')
  const [importResult, setImportResult] = useState<GoodreadsImportResult | null>(null)
  const [importError, setImportError] = useState<string | null>(null)
  const [profileForm, setProfileForm] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
    city: '',
    country: '',
    writerBio: '',
  })
  const [acceptRecommendations, setAcceptRecommendations] = useState(true)
  const [profileSaved, setProfileSaved] = useState(false)
  const [registrationEnabled, setRegistrationEnabled] = useState(true)
  const [registrationError, setRegistrationError] = useState<string | null>(null)

  const { data: setup } = useQuery({
    queryKey: QUERY_KEYS.setup.required,
    queryFn: setupApi.required,
    enabled: !!user?.admin,
  })

  useEffect(() => {
    if (setup) setRegistrationEnabled(setup.registrationEnabled)
  }, [setup])

  useEffect(() => {
    if (user) {
      setProfileForm({
        firstName: user.firstName ?? '',
        lastName: user.lastName ?? '',
        phoneNumber: user.phoneNumber ?? '',
        city: user.city ?? '',
        country: user.country ?? '',
        writerBio: user.writerBio ?? '',
      })
      setAcceptRecommendations(user.acceptRecommendations !== false)
    }
  }, [user])

  const { data: goal } = useQuery({
    queryKey: QUERY_KEYS.goals(CURRENT_YEAR),
    queryFn: () => goalsApi.get(CURRENT_YEAR),
    retry: false,
  })

  useEffect(() => {
    if (goal) setTarget(String(goal.targetBooks))
  }, [goal])

  const goalMutation = useMutation({
    mutationFn: () => goalsApi.upsert(CURRENT_YEAR, parseInt(target, 10)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.goals(CURRENT_YEAR) }),
  })

  const profileMutation = useMutation({
    mutationFn: () =>
      accountApi.updateProfile({
        firstName: profileForm.firstName.trim(),
        lastName: profileForm.lastName.trim() || undefined,
        phoneNumber: profileForm.phoneNumber.trim() || undefined,
        city: profileForm.city.trim() || undefined,
        country: profileForm.country.trim() || undefined,
        writerBio: profileForm.writerBio.trim() || undefined,
        writer: profileForm.writerBio.trim().length > 0,
        acceptRecommendations,
      }),
    onSuccess: async (updated) => {
      const stored = loadStoredAuth()
      if (stored) {
        saveStoredAuth({ ...stored, user: updated })
      }
      await refreshUser()
      setProfileSaved(true)
      setTimeout(() => setProfileSaved(false), 3000)
    },
  })

  const registrationMutation = useMutation({
    mutationFn: (enabled: boolean) => adminApi.setRegistrationEnabled(enabled),
    onSuccess: (_, enabled) => {
      setRegistrationError(null)
      queryClient.setQueryData(QUERY_KEYS.setup.required, (prev: typeof setup) =>
        prev ? { ...prev, registrationEnabled: enabled } : prev,
      )
    },
    onError: (e: Error) => setRegistrationError(e.message),
  })

  const importMutation = useMutation({
    mutationFn: (file: File) => importExportApi.importGoodreads(file),
    onSuccess: (result) => {
      setImportResult(result)
      setImportError(null)
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.library.all })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.shelves.all })
    },
    onError: (e: Error) => {
      setImportError(e.message)
      setImportResult(null)
    },
  })

  return (
    <div className="max-w-2xl space-y-10">
      <header className="animate-slide-up">
        <p className="text-xs font-semibold uppercase tracking-widest text-accent mb-1">You</p>
        <h1 className="font-display text-3xl font-bold text-ink">{copy.settings.title}</h1>
        <p className="mt-1 text-ink-muted">{copy.settings.description}</p>
      </header>

      <section className="manga-panel halftone-overlay rounded-2xl p-6 space-y-4">
        <div className="flex items-center gap-2">
          <User className="h-5 w-5 text-accent" />
          <h2 className="section-header-manga">Your profile</h2>
        </div>
        <p className="text-sm text-ink-muted">{user?.email}</p>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label htmlFor="firstName">First name</Label>
            <Input
              id="firstName"
              value={profileForm.firstName}
              onChange={(e) => setProfileForm((f) => ({ ...f, firstName: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="lastName">Last name</Label>
            <Input
              id="lastName"
              value={profileForm.lastName}
              onChange={(e) => setProfileForm((f) => ({ ...f, lastName: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="phone">Phone</Label>
            <Input
              id="phone"
              type="tel"
              value={profileForm.phoneNumber}
              onChange={(e) => setProfileForm((f) => ({ ...f, phoneNumber: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="city">City</Label>
            <Input
              id="city"
              value={profileForm.city}
              onChange={(e) => setProfileForm((f) => ({ ...f, city: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div className="sm:col-span-2">
            <Label htmlFor="country">Country</Label>
            <Input
              id="country"
              value={profileForm.country}
              onChange={(e) => setProfileForm((f) => ({ ...f, country: e.target.value }))}
              className="mt-1"
            />
          </div>
          <div className="sm:col-span-2">
            <Label htmlFor="bio">Bio (optional)</Label>
            <Textarea
              id="bio"
              rows={2}
              value={profileForm.writerBio}
              onChange={(e) => setProfileForm((f) => ({ ...f, writerBio: e.target.value }))}
              className="mt-1"
            />
          </div>
        </div>
        <label className="flex items-center gap-2 text-sm text-ink">
          <input
            type="checkbox"
            checked={acceptRecommendations}
            onChange={(e) => setAcceptRecommendations(e.target.checked)}
          />
          Accept recommendations from friends
        </label>
        <div className="flex items-center gap-3">
          <Button onClick={() => profileMutation.mutate()} disabled={profileMutation.isPending}>
            Save profile
          </Button>
          {profileSaved && <span className="text-sm text-sage">Profile saved</span>}
        </div>
      </section>

      <section className="manga-panel halftone-overlay rounded-2xl p-6 space-y-4">
        <div className="flex items-center gap-2">
          <Target className="h-5 w-5 text-accent" />
          <h2 className="section-header-manga">{CURRENT_YEAR} reading goal</h2>
        </div>
        {goal && (
          <p className="text-sm text-ink-muted">
            {goal.booksRead} of {goal.targetBooks} books read this year
          </p>
        )}
        <div className="flex gap-2 items-end">
          <div className="flex-1">
            <Label>Target books</Label>
            <Input
              type="number"
              min={1}
              max={1000}
              value={target || (goal ? String(goal.targetBooks) : '')}
              onChange={(e) => setTarget(e.target.value)}
            />
          </div>
          <Button
            onClick={() => goalMutation.mutate()}
            disabled={goalMutation.isPending || !target}
          >
            Save goal
          </Button>
        </div>
      </section>

      {user?.admin && (
        <section className="manga-panel halftone-overlay rounded-2xl p-6 space-y-4">
          <div className="flex items-center gap-2">
            <Shield className="h-5 w-5 text-accent" />
            <h2 className="section-header-manga">{copy.settings.admin.title}</h2>
          </div>
          <p className="text-sm text-ink-muted">{copy.settings.admin.description}</p>
          <p className="text-sm text-ink-muted">
            {registrationEnabled
              ? copy.settings.admin.registrationOn
              : copy.settings.admin.registrationOff}
          </p>
          <label className="flex items-center gap-2 text-sm text-ink">
            <input
              type="checkbox"
              checked={registrationEnabled}
              disabled={registrationMutation.isPending}
              onChange={(e) => {
                const enabled = e.target.checked
                setRegistrationEnabled(enabled)
                registrationMutation.mutate(enabled)
              }}
            />
            {copy.settings.admin.registrationLabel}
          </label>
          {registrationError && <p className="text-sm text-danger">{registrationError}</p>}
          <Link to={APP_ROUTES.adminUsers}>
            <Button variant="secondary">{copy.settings.admin.manageUsers}</Button>
          </Link>
        </section>
      )}

      <section className="manga-panel halftone-overlay rounded-2xl p-6 space-y-4">
        <div className="flex items-center gap-2">
          <Upload className="h-5 w-5 text-accent" />
          <h2 className="section-header-manga">Import from Goodreads</h2>
        </div>
        <p className="text-sm text-ink-muted">
          Upload your Goodreads library export CSV. Books are added to your library; custom shelves
          become private shelves.
        </p>
        <input
          type="file"
          accept=".csv,text/csv"
          className="text-sm"
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) importMutation.mutate(file)
          }}
        />
        {importError && <p className="text-sm text-danger">{importError}</p>}
        {importResult && (
          <p className="text-sm text-sage">
            Imported {importResult.booksImported} books ({importResult.shelvesCreated} new shelves
            {importResult.duplicatesSkipped > 0
              ? `, ${importResult.duplicatesSkipped} duplicate rows skipped`
              : ''}
            , {importResult.errors} errors, {importResult.skipped} empty skipped).
          </p>
        )}
      </section>

      <section className="manga-panel halftone-overlay rounded-2xl p-6 space-y-4">
        <div className="flex items-center gap-2">
          <Download className="h-5 w-5 text-accent" />
          <h2 className="section-header-manga">Export library</h2>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={() => importExportApi.downloadLibrary('csv')}>
            Download CSV
          </Button>
          <Button variant="secondary" onClick={() => importExportApi.downloadLibrary('json')}>
            Download JSON
          </Button>
        </div>
      </section>
    </div>
  )
}
