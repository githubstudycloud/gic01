import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  vus: __ENV.VUS ? parseInt(__ENV.VUS, 10) : 10,
  duration: __ENV.DURATION || '15s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
}

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')

export default function () {
  const res = http.get(`${baseUrl}/demo/ping`, {
    tags: { name: 'ping' },
    headers: {
      'X-Request-Id': `${__VU}-${__ITER}`,
    },
  })

  check(res, {
    'status=200': (r) => r.status === 200,
    'ok=true': (r) => r.status === 200 && r.json('ok') === true,
    'x-request-id present': (r) => Boolean(r.headers['X-Request-Id'] || r.headers['x-request-id']),
  })

  sleep(0.1)
}

