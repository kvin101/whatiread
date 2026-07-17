import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { setupApi } from '../api/setup'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/layout/AuthLayout'
import { Button } from '../components/ui/Button'
import { Input, Label } from '../components/ui/Input'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { UsernameAvailabilityHint } from '../components/ui/UsernameAvailabilityHint'
import { useUsernameAvailability } from '../hooks/useUsernameAvailability'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { getApiErrorMessage } from '../lib/api'

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    email: '',
    username: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const usernameCheck = useUsernameAvailability(form.username)

  const { data: setup, isLoading: setupLoading } = useQuery({
    queryKey: QUERY_KEYS.setup.required,
    queryFn: setupApi.required,
    staleTime: 0,
  })

  if (setupLoading) {
    return <BookLoaderCenter className="min-h-screen" />
  }

  if (setup?.setupRequired) {
    return <Navigate to={APP_ROUTES.setup} replace />
  }

  if (setup && !setup.registrationEnabled) {
    return <Navigate to={APP_ROUTES.login} replace state={{ registrationClosed: true }} />
  }

  if (isAuthenticated) {
    return <Navigate to={APP_ROUTES.home} replace />
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await register({
        email: form.email,
        username: form.username,
        password: form.password,
        firstName: form.firstName,
        lastName: form.lastName || undefined,
        phoneNumber: form.phoneNumber || undefined,
      })
      navigate(APP_ROUTES.home, { replace: true })
    } catch (err) {
      setError(getApiErrorMessage(err, 'Registration failed — try a stronger password.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      quote="Join a bookshelf that won't sell your reading habits to advertisers. Revolutionary, we know."
      centered
    >
      <h1 className="font-display text-2xl font-bold text-ink text-center manga-title">{copy.auth.register.title}</h1>
      <p className="mt-1 text-sm text-ink-muted text-center">{copy.auth.register.subtitle}</p>

      <form onSubmit={submit} className="mt-8 space-y-4">
        <div>
          <Label htmlFor="firstName">First name</Label>
          <Input
            id="firstName"
            required
            value={form.firstName}
            onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
          />
        </div>
        <div>
          <Label htmlFor="lastName">Last name</Label>
          <Input
            id="lastName"
            value={form.lastName}
            onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
          />
        </div>
        <div>
          <Label htmlFor="phone">Phone (optional)</Label>
          <Input
            id="phone"
            type="tel"
            value={form.phoneNumber}
            onChange={(e) => setForm((f) => ({ ...f, phoneNumber: e.target.value }))}
          />
        </div>
        <div>
          <Label htmlFor="username">Username</Label>
          <Input
            id="username"
            required
            minLength={3}
            maxLength={30}
            pattern="[a-zA-Z][a-zA-Z0-9_]{2,29}"
            title="3–30 characters; start with a letter; letters, numbers, underscores only"
            autoComplete="username"
            value={form.username}
            onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
          />
          <UsernameAvailabilityHint value={form.username} check={usernameCheck} />
        </div>
        <div>
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            required
            value={form.email}
            onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
          />
        </div>
        <div>
          <Label htmlFor="password">Password (8+ chars — make it spicy)</Label>
          <Input
            id="password"
            type="password"
            required
            minLength={8}
            value={form.password}
            onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
          />
        </div>
        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" className="w-full" size="lg" disabled={loading || usernameCheck.data?.available === false}>
          {loading ? copy.auth.register.submitting : copy.auth.register.submit}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-ink-muted">
        {copy.auth.register.hasAccount}{' '}
        <Link to={APP_ROUTES.login} className="font-medium text-accent hover:underline">
          {copy.auth.register.signIn}
        </Link>
      </p>
    </AuthLayout>
  )
}
