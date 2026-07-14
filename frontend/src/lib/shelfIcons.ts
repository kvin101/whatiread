import type { LucideIcon } from 'lucide-react'
import {
  Book,
  BookMarked,
  BookOpen,
  Bookmark,
  Brain,
  Coffee,
  Drama,
  Flame,
  Flower2,
  Globe,
  Heart,
  Landmark,
  Leaf,
  Lightbulb,
  Moon,
  PenLine,
  Rocket,
  Sparkles,
  Star,
  Target,
} from 'lucide-react'

export const DEFAULT_SHELF_ICON = 'books'

export const SHELF_ICON_OPTIONS: { id: string; label: string; Icon: LucideIcon }[] = [
  { id: 'books', label: 'Books', Icon: BookMarked },
  { id: 'book-open', label: 'Open book', Icon: BookOpen },
  { id: 'book', label: 'Single book', Icon: Book },
  { id: 'star', label: 'Star', Icon: Star },
  { id: 'sparkles', label: 'Sparkles', Icon: Sparkles },
  { id: 'moon', label: 'Moon', Icon: Moon },
  { id: 'flame', label: 'Flame', Icon: Flame },
  { id: 'lightbulb', label: 'Ideas', Icon: Lightbulb },
  { id: 'target', label: 'Target', Icon: Target },
  { id: 'leaf', label: 'Nature', Icon: Leaf },
  { id: 'rocket', label: 'Rocket', Icon: Rocket },
  { id: 'heart', label: 'Heart', Icon: Heart },
  { id: 'pen', label: 'Writing', Icon: PenLine },
  { id: 'landmark', label: 'Classics', Icon: Landmark },
  { id: 'drama', label: 'Drama', Icon: Drama },
  { id: 'globe', label: 'World', Icon: Globe },
  { id: 'bookmark', label: 'Bookmark', Icon: Bookmark },
  { id: 'brain', label: 'Mind', Icon: Brain },
  { id: 'coffee', label: 'Cozy', Icon: Coffee },
  { id: 'flower', label: 'Bloom', Icon: Flower2 },
]

const ICON_BY_ID = new Map(SHELF_ICON_OPTIONS.map((o) => [o.id, o.Icon]))

/** Legacy emoji values stored before Lucide icon keys. */
const LEGACY_EMOJI_TO_ID: Record<string, string> = {
  '📚': 'books',
  '📖': 'book-open',
  '✨': 'sparkles',
  '🌙': 'moon',
  '🔥': 'flame',
  '💡': 'lightbulb',
  '🎯': 'target',
  '🌿': 'leaf',
  '🚀': 'rocket',
  '💜': 'heart',
  '📝': 'pen',
  '🏛️': 'landmark',
  '🎭': 'drama',
  '🌍': 'globe',
  '⭐': 'star',
  '📕': 'book',
  '🧠': 'brain',
  '☕': 'coffee',
  '🦋': 'flower',
  '🔖': 'bookmark',
}

export function resolveShelfIconId(icon?: string | null): string {
  if (!icon) return DEFAULT_SHELF_ICON
  if (ICON_BY_ID.has(icon)) return icon
  return LEGACY_EMOJI_TO_ID[icon] ?? DEFAULT_SHELF_ICON
}

export function shelfIconComponent(icon?: string | null): LucideIcon {
  const id = resolveShelfIconId(icon)
  return ICON_BY_ID.get(id) ?? BookMarked
}
