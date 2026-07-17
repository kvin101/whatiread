import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, Circle } from 'lucide-react'
import { APP_ROUTES } from '../../api/paths'
import { Button } from '../ui/Button'

const STORAGE_KEY = 'whatiread.onboarding.v1'

type ChecklistState = {
  addedBook: boolean
  createdShelf: boolean
  addedFriend: boolean
  exploredShelf: boolean
  dismissed: boolean
}

const DEFAULT: ChecklistState = {
  addedBook: false,
  createdShelf: false,
  addedFriend: false,
  exploredShelf: false,
  dismissed: false,
}

function loadState(): ChecklistState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return DEFAULT
    return { ...DEFAULT, ...JSON.parse(raw) }
  } catch {
    return DEFAULT
  }
}

export function OnboardingChecklist({
  hasBooks,
  shelfCount,
  friendCount,
}: {
  hasBooks: boolean
  shelfCount: number
  friendCount: number
}) {
  const [state, setState] = useState(loadState)

  useEffect(() => {
    setState((s) => {
      const next = {
        ...s,
        addedBook: s.addedBook || hasBooks,
        createdShelf: s.createdShelf || shelfCount > 0,
        addedFriend: s.addedFriend || friendCount > 0,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      return next
    })
  }, [hasBooks, shelfCount, friendCount])

  const steps = [
    { key: 'addedBook' as const, label: 'Add your first book', to: APP_ROUTES.library },
    { key: 'createdShelf' as const, label: 'Create a shelf', to: APP_ROUTES.shelves },
    { key: 'addedFriend' as const, label: 'Add a friend', to: APP_ROUTES.friends },
    { key: 'exploredShelf' as const, label: 'Explore a public shelf', to: APP_ROUTES.explore },
  ]

  const doneCount = steps.filter((s) => state[s.key]).length
  if (state.dismissed || doneCount >= steps.length) return null

  const dismiss = () => {
    const next = { ...state, dismissed: true }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setState(next)
  }

  const markExplored = () => {
    const next = { ...state, exploredShelf: true }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setState(next)
  }

  return (
    <section className="rounded-2xl border border-accent/20 bg-accent/5 p-4">
      <div className="flex items-start justify-between gap-2">
        <div>
          <h2 className="font-display font-semibold text-ink">Get started</h2>
          <p className="text-sm text-ink-muted mt-0.5">
            {doneCount}/{steps.length} complete
          </p>
        </div>
        <Button type="button" variant="ghost" size="sm" onClick={dismiss}>
          Dismiss
        </Button>
      </div>
      <ul className="mt-3 space-y-2">
        {steps.map((step) => {
          const done = state[step.key]
          return (
            <li key={step.key}>
              <Link
                to={step.to}
                onClick={step.key === 'exploredShelf' ? markExplored : undefined}
                className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-white/5"
              >
                {done ? (
                  <Check className="h-4 w-4 text-sage shrink-0" />
                ) : (
                  <Circle className="h-4 w-4 text-ink-muted shrink-0" />
                )}
                <span className={done ? 'text-ink-muted line-through' : 'text-ink'}>{step.label}</span>
              </Link>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
