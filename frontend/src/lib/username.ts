/** Mirrors backend username rules for client-side gating before API calls. */
export const UsernameUtils = {
  MIN_LENGTH: 3,
  MAX_LENGTH: 30,
  normalize(raw: string): string {
    return raw.trim().toLowerCase()
  },
  equals(a: string, b: string): boolean {
    return UsernameUtils.normalize(a) === UsernameUtils.normalize(b)
  },
} as const
