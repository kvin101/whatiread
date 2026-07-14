import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Shield, User } from 'lucide-react'
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
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

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
    <div className="mx-auto max-w-3xl space-y-12">
      <header className="animate-slide-up">
        <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-accent">You</p>
        <h1 className="font-display text-3xl font-bold text-ink md:text-4xl">{copy.settings.title}</h1>
        <p className="mt-2 text-base text-ink-muted">{copy.settings.description}</p>
      </header>

      <section className="manga-panel halftone-overlay space-y-8 rounded-2xl p-8">
        <div className="flex items-center gap-3">
          <User className="h-5 w-5 text-accent" />
          <h2 className="section-header-manga">Your profile</h2>
        </div>

        <ProfileAvatarUpload />

        <div className="space-y-1 border-t border-border/60 pt-6">
          <p className="text-sm text-ink-muted">{user?.email}</p>
          {user?.username && <p className="text-sm text-ink-muted">@{user.username}</p>}
        </div>

        <div className="grid gap-6 sm:grid-cols-2">
          <div className="space-y-2">
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
          <div className="space-y-2">
            <Label htmlFor="firstName">First name</Label>
            <Input
              id="firstName"
              value={profileForm.firstName}
              onChange={(e) => setProfileForm((f) => ({ ...f, firstName: e.target.value }))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="lastName">Last name</Label>
            <Input
              id="lastName"
              value={profileForm.lastName}
              onChange={(e) => setProfileForm((f) => ({ ...f, lastName: e.target.value }))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="phone">Phone</Label>
            <Input
              id="phone"
              type="tel"
              value={profileForm.phoneNumber}
              onChange={(e) => setProfileForm((f) => ({ ...f, phoneNumber: e.target.value }))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="city">City</Label>
            <Input
              id="city"
              value={profileForm.city}
              onChange={(e) => setProfileForm((f) => ({ ...f, city: e.target.value }))}
            />
          </div>
          <div className="space-y-2 sm:col-span-2">
            <Label htmlFor="country">Country</Label>
            <Input
              id="country"
              value={profileForm.country}
              onChange={(e) => setProfileForm((f) => ({ ...f, country: e.target.value }))}
            />
          </div>
          <div className="space-y-2 sm:col-span-2">
            <Label htmlFor="bio">Bio (optional)</Label>
            <Textarea
              id="bio"
              rows={4}
              value={profileForm.writerBio}
              onChange={(e) => setProfileForm((f) => ({ ...f, writerBio: e.target.value }))}
            />
          </div>
        </div>

        <label className="flex items-center gap-3 text-sm text-ink">
          <input
            type="checkbox"
            checked={acceptRecommendations}
            onChange={(e) => setAcceptRecommendations(e.target.checked)}
          />
          Accept recommendations from friends
        </label>

        <div className="flex items-center gap-3 pt-2">
          <Button onClick={() => profileMutation.mutate()} disabled={profileMutation.isPending}>
            Save profile
          </Button>
          {profileSaved && <span className="text-sm text-sage">Profile saved</span>}
        </div>
      </section>

      {user?.admin && (
        <section className="manga-panel halftone-overlay space-y-5 rounded-2xl p-8">
          <div className="flex items-center gap-3">
            <Shield className="h-5 w-5 text-accent" />
            <h2 className="section-header-manga">{copy.settings.admin.title}</h2>
          </div>
          <p className="text-sm leading-relaxed text-ink-muted">{copy.settings.admin.description}</p>
          <p className="text-sm leading-relaxed text-ink-muted">
            {registrationEnabled
              ? copy.settings.admin.registrationOn
              : copy.settings.admin.registrationOff}
          </p>
          <label className="flex items-center gap-3 text-sm text-ink">
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
    </div>
  )
}
