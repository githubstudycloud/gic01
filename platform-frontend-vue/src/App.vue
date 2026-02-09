<script setup lang="ts">
import { computed, ref } from 'vue'

import { api } from './api/client'

const backend = computed(() => import.meta.env.VITE_API_BASE_URL ?? '(vite proxy -> http://localhost:8080)')

const pingLoading = ref(false)
const pingRequestId = ref<string | null>(null)
const pingData = ref<unknown>(null)
const pingError = ref<string | null>(null)

async function ping() {
  pingLoading.value = true
  pingRequestId.value = null
  pingData.value = null
  pingError.value = null

  try {
    const { data, error, response } = await api.GET('/demo/ping')
    pingRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error) {
      pingError.value = JSON.stringify(error, null, 2)
    } else {
      pingData.value = data
    }
  } catch (e) {
    pingError.value = String(e)
  } finally {
    pingLoading.value = false
  }
}

const lockName = ref('demo')
const lockTtlSeconds = ref(5)
const lockLoading = ref(false)
const lockRequestId = ref<string | null>(null)
const lockData = ref<unknown>(null)
const lockError = ref<string | null>(null)

async function tryLock() {
  lockLoading.value = true
  lockRequestId.value = null
  lockData.value = null
  lockError.value = null

  try {
    const { data, error, response } = await api.GET('/demo/lock', {
      params: {
        query: {
          name: lockName.value,
          ttlSeconds: lockTtlSeconds.value,
        },
      },
    })
    lockRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error) {
      lockError.value = JSON.stringify(error, null, 2)
    } else {
      lockData.value = data
    }
  } catch (e) {
    lockError.value = String(e)
  } finally {
    lockLoading.value = false
  }
}

function toJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}
</script>

<template>
  <main class="page">
    <header class="hero">
      <div class="badge">platform-frontend-vue</div>
      <h1>API Contract Playground</h1>
      <p class="sub">
        Typed calls generated from OpenAPI. Default dev mode proxies to the sample backend.
      </p>

      <div class="meta">
        <div class="kv">
          <span class="k">Backend</span>
          <code class="v">{{ backend }}</code>
        </div>
        <div class="kv">
          <span class="k">Correlation</span>
          <code class="v">X-Request-Id</code>
        </div>
      </div>
    </header>

    <section class="grid">
      <article class="card">
        <div class="cardHead">
          <h2>Ping</h2>
          <div class="muted">GET /demo/ping</div>
        </div>
        <div class="actions">
          <button class="btn" :disabled="pingLoading" @click="ping">
            {{ pingLoading ? 'Pinging...' : 'Ping' }}
          </button>
        </div>
        <div v-if="pingRequestId" class="kv">
          <span class="k">X-Request-Id</span>
          <code class="v">{{ pingRequestId }}</code>
        </div>
        <pre v-if="pingData" class="out">{{ toJson(pingData) }}</pre>
        <pre v-if="pingError" class="err">{{ pingError }}</pre>
      </article>

      <article class="card">
        <div class="cardHead">
          <h2>Try Lock</h2>
          <div class="muted">GET /demo/lock</div>
        </div>

        <div class="form">
          <label>
            <div class="label">Name</div>
            <input v-model="lockName" placeholder="lock name" />
          </label>
          <label>
            <div class="label">TTL (s)</div>
            <input v-model.number="lockTtlSeconds" type="number" min="1" max="60" />
          </label>
        </div>

        <div class="actions">
          <button class="btn" :disabled="lockLoading" @click="tryLock">
            {{ lockLoading ? 'Requesting...' : 'Try Lock' }}
          </button>
        </div>

        <div v-if="lockRequestId" class="kv">
          <span class="k">X-Request-Id</span>
          <code class="v">{{ lockRequestId }}</code>
        </div>
        <pre v-if="lockData" class="out">{{ toJson(lockData) }}</pre>
        <pre v-if="lockError" class="err">{{ lockError }}</pre>
      </article>
    </section>

    <footer class="foot">
      <div>
        Backend:
        <code>mvn -q -pl platform-sample-app spring-boot:run</code>
      </div>
      <div>
        Generate types:
        <code>npm run gen:api</code>
      </div>
    </footer>
  </main>
</template>

<style scoped>
.page {
  width: min(1080px, calc(100% - 2rem));
  margin: 2rem auto;
  animation: enter 420ms cubic-bezier(0.2, 0.9, 0.2, 1) both;
}

.hero {
  padding: 1.25rem 1.25rem 1.5rem;
}

.badge {
  display: inline-flex;
  border: 1px solid var(--border);
  background: color-mix(in oklab, var(--card), transparent 30%);
  padding: 0.25rem 0.5rem;
  border-radius: 999px;
  font-size: 0.8rem;
  letter-spacing: 0.03em;
}

h1 {
  margin: 0.6rem 0 0.25rem;
  font-size: clamp(2rem, 2vw + 1.4rem, 3rem);
}

.sub {
  margin: 0;
  max-width: 64ch;
  color: var(--muted);
}

.meta {
  margin-top: 1rem;
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

@media (max-width: 860px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

.card {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: color-mix(in oklab, var(--card), transparent 10%);
  backdrop-filter: blur(10px);
  padding: 1rem;
  box-shadow: 0 18px 60px rgba(15, 23, 42, 0.08);
}

.cardHead {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.muted {
  color: var(--muted);
  font-size: 0.9rem;
}

.actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 0.75rem;
}

.btn {
  appearance: none;
  border: 1px solid color-mix(in oklab, var(--border), transparent 40%);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0.35));
  color: var(--ink);
  padding: 0.6rem 0.9rem;
  border-radius: 12px;
  cursor: pointer;
  transition:
    transform 120ms ease,
    box-shadow 120ms ease,
    border-color 120ms ease;
}

.btn:hover:enabled {
  transform: translateY(-1px);
  border-color: color-mix(in oklab, var(--accent), var(--border) 60%);
  box-shadow: 0 12px 40px rgba(14, 165, 233, 0.15);
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

@media (max-width: 520px) {
  .form {
    grid-template-columns: 1fr;
  }
}

.label {
  font-size: 0.85rem;
  color: var(--muted);
  margin-bottom: 0.25rem;
}

input {
  width: 100%;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.6);
  padding: 0.55rem 0.65rem;
  border-radius: 12px;
  outline: none;
  color: var(--ink);
}

input:focus {
  border-color: color-mix(in oklab, var(--accent), var(--border) 45%);
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.15);
}

.kv {
  display: flex;
  gap: 0.6rem;
  align-items: baseline;
  margin-top: 0.5rem;
}

.k {
  color: var(--muted);
  font-size: 0.85rem;
  min-width: 7rem;
}

.v {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 0.85rem;
  color: var(--ink);
  background: rgba(15, 23, 42, 0.06);
  padding: 0.1rem 0.4rem;
  border-radius: 8px;
}

.out,
.err {
  margin-top: 0.75rem;
  white-space: pre-wrap;
  border-radius: 12px;
  padding: 0.75rem;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.6);
  max-height: 320px;
  overflow: auto;
}

.err {
  border-color: rgba(239, 68, 68, 0.35);
  background: rgba(254, 226, 226, 0.6);
}

.foot {
  margin: 1rem 1.25rem 2rem;
  color: var(--muted);
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

@keyframes enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
