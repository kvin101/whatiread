import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { setupApi } from '../api/setup'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/layout/AuthLayout'
import { Button } from '../components/ui/Button'
import { Input, Label } from '../components/ui/Input'
import { copy } from '../lib/copy'
import { safeAppPath } from '../lib/safeRedirect'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'
import { getApiErrorMessage } from '../lib/api'

export function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = safeAppPath(
    (location.state as { from?: { pathname: string } })?.from?.pathname,
  )
  const registrationClosed = Boolean(
    (location.state as { registrationClosed?: boolean } | null)?.registrationClosed,
  )

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const { data: setup } = useQuery({
    queryKey: QUERY_KEYS.setup.required,
    queryFn: setupApi.required,
  })

  if (setup?.setupRequired) {
    return <Navigate to={APP_ROUTES.setup} replace />
  }

  if (isAuthenticated) {
    return <Navigate to={from} replace />
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await login(email, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(getApiErrorMessage(err, 'Login failed — wrong password or gremlins.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
      <h1 className="font-display text-2xl font-bold text-ink manga-title">{copy.auth.login.title}</h1>
      <p className="mt-1 text-sm text-ink-muted">{copy.auth.login.subtitle}</p>

      {(registrationClosed || setup?.registrationEnabled === false) && (
        <div className="mt-4 space-y-2 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-ink-muted">
          <p>{copy.auth.registrationDisabled.message}</p>
          <p className="text-xs text-ink-muted/80">{copy.auth.registrationDisabled.adminHint}</p>
        </div>
      )}

      <form onSubmit={submit} className="mt-8 space-y-4">
        <div>
          <Label htmlFor="email">Email or username</Label>
          <Input
            id="email"
            type="text"
            autoComplete="username"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" className="w-full comic-cta-starburst" size="lg" disabled={loading}>
          {loading ? copy.auth.login.submitting : copy.auth.login.submit}
        </Button>
      </form>

      {setup?.registrationEnabled !== false && (
        <p className="mt-6 text-center text-sm text-ink-muted">
          {copy.auth.login.noAccount}{' '}
          <Link to={APP_ROUTES.register} className="font-medium text-accent hover:underline">
            {copy.auth.login.createAccount}
          </Link>
        </p>
      )}
    </AuthLayout>
  )
}
