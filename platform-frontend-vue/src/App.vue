<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { api } from './api/client'
import type { components } from './api/openapi'

type FlowDto = components['schemas']['FlowDto']
type RunDto = components['schemas']['RunDto']
type ArtifactsSnapshot = components['schemas']['ArtifactsSnapshot']
type TodoItem = components['schemas']['TodoItem']

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

const flowsLoading = ref(false)
const flowsRequestId = ref<string | null>(null)
const flows = ref<FlowDto[]>([])
const flowsError = ref<string | null>(null)
const selectedFlowId = ref<string>('')

const runsLoading = ref(false)
const runsRequestId = ref<string | null>(null)
const runs = ref<RunDto[]>([])
const runsError = ref<string | null>(null)

const startInputsJson = ref<string>('{}')
const startTargetsCsv = ref<string>('')
const startLoading = ref(false)
const startRequestId = ref<string | null>(null)
const startError = ref<string | null>(null)

const artifactsLoading = ref(false)
const artifactsRequestId = ref<string | null>(null)
const artifacts = ref<ArtifactsSnapshot | null>(null)
const artifactsError = ref<string | null>(null)

async function refreshFlows() {
  flowsLoading.value = true
  flowsRequestId.value = null
  flowsError.value = null
  try {
    const { data, error, response } = await api.GET('/flows')
    flowsRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error || !data) {
      flows.value = []
      flowsError.value = JSON.stringify(error ?? 'No data', null, 2)
      return
    }
    flows.value = data
    if (!selectedFlowId.value && data.length > 0) selectedFlowId.value = data[0]!.id
  } catch (e) {
    flowsError.value = String(e)
  } finally {
    flowsLoading.value = false
  }
}

async function refreshRuns() {
  const flowId = selectedFlowId.value
  if (!flowId) return

  runsLoading.value = true
  runsRequestId.value = null
  runsError.value = null
  try {
    const { data, error, response } = await api.GET('/flows/{flowId}/runs', {
      params: {
        path: { flowId },
        query: { limit: 10 },
      },
    })
    runsRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error || !data) {
      runs.value = []
      runsError.value = JSON.stringify(error ?? 'No data', null, 2)
      return
    }
    runs.value = data
  } catch (e) {
    runsError.value = String(e)
  } finally {
    runsLoading.value = false
  }
}

function parseJsonObject(text: string): Record<string, unknown> | undefined {
  const trimmed = text.trim()
  if (!trimmed) return undefined
  const parsed = JSON.parse(trimmed) as unknown
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('inputs must be a JSON object')
  }
  return parsed as Record<string, unknown>
}

function parseTargets(text: string): string[] | undefined {
  const trimmed = text.trim()
  if (!trimmed) return undefined
  const targets = trimmed
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return targets.length ? targets : undefined
}

async function startRun() {
  const flowId = selectedFlowId.value
  if (!flowId) return

  startLoading.value = true
  startRequestId.value = null
  startError.value = null
  try {
    const inputs = parseJsonObject(startInputsJson.value)
    const targets = parseTargets(startTargetsCsv.value)
    const body = inputs || targets ? { inputs, targets } : undefined

    const { error, response } = await api.POST('/flows/{flowId}/runs', {
      params: { path: { flowId } },
      ...(body ? { body } : {}),
    })
    startRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error) {
      startError.value = JSON.stringify(error, null, 2)
      return
    }

    await refreshRuns()
  } catch (e) {
    startError.value = String(e)
  } finally {
    startLoading.value = false
  }
}

async function retryRun(runId: string) {
  const flowId = selectedFlowId.value
  if (!flowId) return

  try {
    await api.POST('/flows/{flowId}/runs/{runId}/retry', { params: { path: { flowId, runId } } })
    await refreshRuns()
  } catch {
    // best-effort retry button
  }
}

async function loadArtifacts(runId: string) {
  const flowId = selectedFlowId.value
  if (!flowId) return

  artifactsLoading.value = true
  artifactsRequestId.value = null
  artifactsError.value = null
  artifacts.value = null
  try {
    const { data, error, response } = await api.GET('/flows/{flowId}/runs/{runId}/artifacts', {
      params: { path: { flowId, runId } },
    })
    artifactsRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error || !data) {
      artifactsError.value = JSON.stringify(error ?? 'No data', null, 2)
      return
    }
    artifacts.value = data
  } catch (e) {
    artifactsError.value = String(e)
  } finally {
    artifactsLoading.value = false
  }
}

const todosLoading = ref(false)
const todosRequestId = ref<string | null>(null)
const todos = ref<TodoItem[]>([])
const todosError = ref<string | null>(null)

const todoTitle = ref('')
const todoCreateLoading = ref(false)
const todoCreateRequestId = ref<string | null>(null)
const todoCreateError = ref<string | null>(null)

async function refreshTodos() {
  todosLoading.value = true
  todosRequestId.value = null
  todosError.value = null
  try {
    const { data, error, response } = await api.GET('/crud/todos')
    todosRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error || !data) {
      todos.value = []
      todosError.value = JSON.stringify(error ?? 'No data', null, 2)
      return
    }
    todos.value = data
  } catch (e) {
    todosError.value = String(e)
  } finally {
    todosLoading.value = false
  }
}

async function createTodo() {
  const title = todoTitle.value.trim()
  if (!title) return

  todoCreateLoading.value = true
  todoCreateRequestId.value = null
  todoCreateError.value = null
  try {
    const { error, response } = await api.POST('/crud/todos', { body: { title } })
    todoCreateRequestId.value = response?.headers.get('X-Request-Id') ?? null
    if (error) {
      todoCreateError.value = JSON.stringify(error, null, 2)
      return
    }
    todoTitle.value = ''
    await refreshTodos()
  } catch (e) {
    todoCreateError.value = String(e)
  } finally {
    todoCreateLoading.value = false
  }
}

watch(selectedFlowId, async () => {
  artifacts.value = null
  artifactsError.value = null
  await refreshRuns()
})

onMounted(async () => {
  await refreshFlows()
  await refreshTodos()
})
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

      <article class="card">
        <div class="cardHead">
          <h2>Flows</h2>
          <div class="muted">GET /flows</div>
        </div>

        <div class="actions">
          <button class="btn" :disabled="flowsLoading" @click="refreshFlows">
            {{ flowsLoading ? 'Refreshing...' : 'Refresh Flows' }}
          </button>
          <button class="btn" :disabled="runsLoading || !selectedFlowId" @click="refreshRuns">
            {{ runsLoading ? 'Loading...' : 'Refresh Runs' }}
          </button>
        </div>

        <div v-if="flowsRequestId" class="kv">
          <span class="k">X-Request-Id</span>
          <code class="v">{{ flowsRequestId }}</code>
        </div>
        <pre v-if="flowsError" class="err">{{ flowsError }}</pre>

        <label v-if="flows.length > 0" style="display: block; margin-top: 0.5rem">
          <div class="label">Flow</div>
          <select v-model="selectedFlowId">
            <option v-for="f in flows" :key="f.id" :value="f.id">
              {{ f.id }}
            </option>
          </select>
        </label>

        <div v-if="selectedFlowId" class="form" style="margin-top: 0.75rem">
          <label style="grid-column: 1 / -1">
            <div class="label">inputs (JSON object)</div>
            <textarea v-model="startInputsJson" rows="4" spellcheck="false" />
          </label>
          <label style="grid-column: 1 / -1">
            <div class="label">targets (comma separated, optional)</div>
            <input v-model="startTargetsCsv" placeholder="e.g. measure.summary" />
          </label>
        </div>

        <div class="actions">
          <button class="btn" :disabled="startLoading || !selectedFlowId" @click="startRun">
            {{ startLoading ? 'Starting...' : 'Start Run' }}
          </button>
        </div>
        <div v-if="startRequestId" class="kv">
          <span class="k">X-Request-Id</span>
          <code class="v">{{ startRequestId }}</code>
        </div>
        <pre v-if="startError" class="err">{{ startError }}</pre>

        <div v-if="runsRequestId" class="kv">
          <span class="k">Runs X-Request-Id</span>
          <code class="v">{{ runsRequestId }}</code>
        </div>
        <pre v-if="runsError" class="err">{{ runsError }}</pre>

        <div v-if="runs.length > 0" class="runs">
          <div v-for="r in runs" :key="r.runId" class="runRow">
            <div class="runMeta">
              <code class="runId">{{ r.runId.slice(0, 10) }}</code>
              <span class="status" :data-s="r.status">{{ r.status }}</span>
            </div>
            <div class="runActions">
              <button class="btn mini" @click="loadArtifacts(r.runId)">Artifacts</button>
              <button class="btn mini" @click="retryRun(r.runId)">Retry</button>
            </div>
          </div>
        </div>

        <div v-if="artifactsRequestId" class="kv">
          <span class="k">Artifacts X-Request-Id</span>
          <code class="v">{{ artifactsRequestId }}</code>
        </div>
        <pre v-if="artifactsError" class="err">{{ artifactsError }}</pre>
        <pre v-if="artifacts" class="out">{{ toJson(artifacts) }}</pre>
      </article>

      <article class="card">
        <div class="cardHead">
          <h2>Todos</h2>
          <div class="muted">/crud/todos</div>
        </div>

        <div class="form">
          <label style="grid-column: 1 / -1">
            <div class="label">Title</div>
            <input v-model="todoTitle" placeholder="new todo" />
          </label>
        </div>

        <div class="actions">
          <button class="btn" :disabled="todoCreateLoading || !todoTitle.trim()" @click="createTodo">
            {{ todoCreateLoading ? 'Creating...' : 'Create' }}
          </button>
          <button class="btn" :disabled="todosLoading" @click="refreshTodos">
            {{ todosLoading ? 'Loading...' : 'Refresh' }}
          </button>
        </div>

        <div v-if="todoCreateRequestId" class="kv">
          <span class="k">Create X-Request-Id</span>
          <code class="v">{{ todoCreateRequestId }}</code>
        </div>
        <pre v-if="todoCreateError" class="err">{{ todoCreateError }}</pre>

        <div v-if="todosRequestId" class="kv">
          <span class="k">List X-Request-Id</span>
          <code class="v">{{ todosRequestId }}</code>
        </div>
        <pre v-if="todosError" class="err">{{ todosError }}</pre>
        <pre v-if="todos.length" class="out">{{ toJson(todos) }}</pre>
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

select,
textarea {
  width: 100%;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.6);
  padding: 0.55rem 0.65rem;
  border-radius: 12px;
  outline: none;
  color: var(--ink);
}

textarea {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 0.85rem;
  resize: vertical;
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

.runs {
  margin-top: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.runRow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.55rem 0.65rem;
  background: rgba(255, 255, 255, 0.55);
  border-top: 1px solid var(--border);
}

.runRow:first-child {
  border-top: none;
}

.runMeta {
  display: flex;
  align-items: baseline;
  gap: 0.6rem;
  min-width: 0;
}

.runId {
  font-size: 0.85rem;
  padding: 0.05rem 0.4rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
}

.status {
  font-size: 0.8rem;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.status[data-s='SUCCEEDED'] {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.25);
}

.status[data-s='FAILED'] {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.25);
}

.runActions {
  display: flex;
  gap: 0.5rem;
}

.btn.mini {
  padding: 0.45rem 0.65rem;
  border-radius: 10px;
  font-size: 0.85rem;
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
