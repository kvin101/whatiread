import { Component, type ReactNode } from 'react'
import { Button } from './Button'

type ErrorBoundaryProps = {
  children: ReactNode
}

type ErrorBoundaryState = {
  hasError: boolean
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false }

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center p-6">
          <div className="max-w-md rounded-3xl border border-border bg-paper-elevated p-6 text-center shadow-sm">
            <h1 className="text-xl font-semibold text-ink">Something went wrong</h1>
            <p className="mt-2 text-sm text-ink-muted">
              Refresh the page or try again in a moment.
            </p>
            <Button className="mt-4" onClick={() => window.location.reload()}>
              Reload
            </Button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
