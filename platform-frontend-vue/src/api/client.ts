import createClient from 'openapi-fetch'

import type { paths } from './openapi'
import { generateRequestId } from './requestId'

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

export const api = createClient<paths>({
  baseUrl,
  fetch: async (req: Request): Promise<Response> => {
    const headers = new Headers(req.headers)
    if (!headers.has('X-Request-Id')) headers.set('X-Request-Id', generateRequestId())

    // Recreate the Request so the header change is applied (Request headers are immutable).
    const out = new Request(req, { headers })
    return fetch(out)
  },
})
