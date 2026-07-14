import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ExternalLink, Shield, SlidersHorizontal } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { accountApi } from '../api/account'
import { adminApi } from '../api/admin'
import { setupApi } from '../api/setup'
import { useAuth } from '../auth/AuthContext'
import { saveStoredAuth, loadStoredAuth } from '../auth/storage'
import { ProfileAvatarUpload } from '../components/profile/ProfileAvatarUpload'
import { UsernameAvailabilityHint } from '../components/ui/UsernameAvailabilityHint'
import { useUsernameAvailability } from '../hooks/useUsernameAvailability'
import { Button } from '../components/ui/Button'
import { Input, Label } from '../components/ui/Input'
import { Textarea } from '../components/ui/Textarea'
import { PageHeader } from '../components/layout/PageHeader'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { ScrollablePage } from '../components/layout/ScrollablePage'
import { displayName } from '../lib/utils'

function FieldGroup({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-ink-muted">{title}</h3>
      {children}
    </div>
  )
}

export function SettingsPage() {
  const queryClient = useQueryClient()
  const { user, refreshUser } = useAuth()
  const [profileForm, setProfileForm] = useState({
    firstName: '',
    lastName: '',
    username: '',
    phoneNumber: '',
    city: '',
    country: '',
    writerBio: '',
  })
  const [acceptRecommendations, setAcceptRecommendations] = useState(true)
  const [profileSaved, setProfileSaved] = useState(false)
  const [registrationEnabled, setRegistrationEnabled] = useState(true)
  const [registrationError, setRegistrationError] = useState<string | null>(null)
  const usernameCheck = useUsernameAvailability(profileForm.username, { currentUser: true })
  const name = user ? displayName(user) : ''

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
        username: user.username ?? '',
        phoneNumber: user.phoneNumber ?? '',
        city: user.city ?? '',
        country: user.country ?? '',
        writerBio: user.writerBio ?? '',
      })
      setAcceptRecommendations(user.acceptRecommendations !== false)
    }
  }, [user])

  const profileMutation = useMutation({
    mutationFn: () =>
      accountApi.updateProfile({
        firstName: profileForm.firstName.trim(),
        lastName: profileForm.lastName.trim() || undefined,
        username: profileForm.username.trim() || undefined,
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

  return (
    <ScrollablePage>
      <div className="mx-auto max-w-2xl space-y-5">
        <PageHeader
          title={copy.settings.title}
          action={
            user?.id ? (
              <Link
                to={APP_ROUTES.userProfile(user.id)}
                className="inline-flex items-center gap-1.5 text-sm font-medium text-accent hover:underline"
              >
                <ExternalLink className="h-3.5 w-3.5" />
                Public profile
              </Link>
            ) : undefined
          }
        />

        <section className="rounded-xl border border-white/10 bg-paper-elevated/40 p-4 md:p-5 space-y-5">
          <div className="flex items-center gap-4 border-b border-white/8 pb-4">
            <ProfileAvatarUpload compact />
            <div className="min-w-0">
              <p className="font-semibold text-ink truncate">{name}</p>
              {user?.username && <p className="text-sm text-ink-muted">@{user.username}</p>}
              <p className="text-sm text-ink-muted truncate">{user?.email}</p>
            </div>
          </div>

          <FieldGroup title="Profile">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  value={profileForm.username}
                  onChange={(e) => setProfileForm((f) => ({ ...f, username: e.target.value }))}
                  minLength={3}
                  maxLength={30}
                  pattern="[a-zA-Z][a-zA-Z0-9_]{2,29}"
                />
                <UsernameAvailabilityHint value={profileForm.username} check={usernameCheck} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="firstName">First name</Label>
                <Input
                  id="firstName"
                  value={profileForm.firstName}
                  onChange={(e) => setProfileForm((f) => ({ ...f, firstName: e.target.value }))}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="lastName">Last name</Label>
                <Input
                  id="lastName"
                  value={profileForm.lastName}
                  onChange={(e) => setProfileForm((f) => ({ ...f, lastName: e.target.value }))}
                />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="bio">Bio</Label>
                <Textarea
                  id="bio"
                  rows={3}
                  placeholder="About your reading…"
                  value={profileForm.writerBio}
                  onChange={(e) => setProfileForm((f) => ({ ...f, writerBio: e.target.value }))}
                />
              </div>
            </div>
          </FieldGroup>

          <FieldGroup title="Location & contact">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="city">City</Label>
                <Input
                  id="city"
                  value={profileForm.city}
                  onChange={(e) => setProfileForm((f) => ({ ...f, city: e.target.value }))}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="country">Country</Label>
                <Input
                  id="country"
                  value={profileForm.country}
                  onChange={(e) => setProfileForm((f) => ({ ...f, country: e.target.value }))}
                />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="phone">Phone</Label>
                <Input
                  id="phone"
                  type="tel"
                  value={profileForm.phoneNumber}
                  onChange={(e) => setProfileForm((f) => ({ ...f, phoneNumber: e.target.value }))}
                />
                <p className="text-xs text-ink-muted">Not shown on your public profile.</p>
              </div>
            </div>
          </FieldGroup>

          <FieldGroup title="Preferences">
            <label className="flex items-start gap-2.5 text-sm text-ink">
              <input
                type="checkbox"
                className="mt-0.5"
                checked={acceptRecommendations}
                onChange={(e) => setAcceptRecommendations(e.target.checked)}
              />
              <span>Accept recommendations from friends</span>
            </label>
          </FieldGroup>

          <div className="flex items-center gap-3 pt-1">
            <Button size="sm" onClick={() => profileMutation.mutate()} disabled={profileMutation.isPending}>
              <SlidersHorizontal className="h-4 w-4" />
              Save changes
            </Button>
            {profileSaved && <span className="text-sm text-sage">Saved</span>}
          </div>
        </section>

        {user?.admin && (
          <section className="rounded-xl border border-white/10 bg-paper-elevated/40 p-4 md:p-5 space-y-4">
            <h2 className="font-display text-base font-semibold text-ink">{copy.settings.admin.title}</h2>
            <p className="text-sm text-ink-muted">
              {registrationEnabled
                ? copy.settings.admin.registrationOn
                : copy.settings.admin.registrationOff}
            </p>
            <label className="flex items-center gap-2.5 text-sm text-ink">
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
              <Button size="sm" variant="secondary">
                <Shield className="h-4 w-4" />
                {copy.settings.admin.manageUsers}
              </Button>
            </Link>
          </section>
        )}
      </div>
    </ScrollablePage>
  )
}
