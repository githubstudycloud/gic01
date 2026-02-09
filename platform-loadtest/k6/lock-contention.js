import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter } from 'k6/metrics'

export const options = {
  vus: __ENV.VUS ? parseInt(__ENV.VUS, 10) : 20,
  duration: __ENV.DURATION || '15s',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800'],
  },
}

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const lockName = __ENV.LOCK_NAME || 'k6'
const holdMillis = __ENV.HOLD_MILLIS ? parseInt(__ENV.HOLD_MILLIS, 10) : 300

const acquired = new Counter('lock_acquired')
const rejected = new Counter('lock_rejected')

export default function () {
  const url =
    `${baseUrl}/demo/lock?` +
    `name=${encodeURIComponent(lockName)}` +
    `&ttlSeconds=5` +
    `&holdMillis=${holdMillis}`

  const res = http.get(url, {
    tags: { name: 'lock' },
    headers: { 'X-Request-Id': `${__VU}-${__ITER}` },
  })

  let ok = false
  let acquiredVal = false
  try {
    acquiredVal = res.json('acquired') === true
    ok = true
  } catch (_) {
    ok = false
  }

  if (acquiredVal) acquired.add(1)
  else rejected.add(1)

  check(res, {
    'status=200': (r) => r.status === 200,
    'json parse ok': () => ok,
    'x-request-id present': (r) => Boolean(r.headers['X-Request-Id'] || r.headers['x-request-id']),
  })

  sleep(0.05)
}

