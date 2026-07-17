const API = `${(process.env.BASE_URL ?? process.env.SMOKE_BASE_URL ?? 'http://localhost').replace(/\/$/, '')}/api/v1`

export type ApiResult = { status: number; json: Record<string, unknown> | null }

export async function api(
  method: string,
  path: string,
  { token, body }: { token?: string; body?: unknown } = {},
): Promise<ApiResult> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  for (let attempt = 0; attempt < 4; attempt++) {
    const res = await fetch(`${API}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    })

    const text = await res.text()
    let json: Record<string, unknown> | null = null
    try {
      json = text ? (JSON.parse(text) as Record<string, unknown>) : null
    } catch {
      json = null
    }

    if (res.status !== 429 || attempt === 3) {
      return { status: res.status, json }
    }
    await new Promise((r) => setTimeout(r, 2_000 * (attempt + 1)))
  }

  return { status: 429, json: null }
}
