export function generateRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID().replace(/-/g, '')
  }

  // Fallback: keep it short and URL-safe.
  const rnd = Math.random().toString(16).slice(2)
  const ts = Date.now().toString(16)
  return `${ts}${rnd}`
}
