# platform-frontend-vue

Vue 3 + Vite + TypeScript frontend for the `platform-*` foundation.

This app demonstrates:
- typed API calls generated from OpenAPI (`openapi-typescript` + `openapi-fetch`)
- request correlation via `X-Request-Id`
- local dev proxy to the sample backend (no CORS headache)

## Prereqs

- Node 20+
- Backend (sample): run from repo root:

```bash
mvn -q -pl platform-sample-app spring-boot:run
```

## First run

```bash
cd platform-frontend-vue
npm install

# Generate src/api/openapi.ts from the sample contract file
npm run gen:api

npm run dev
```

Open: `http://localhost:5173`

## Backend URL

Default dev mode uses Vite proxy to `http://localhost:8080` (see `vite.config.ts`).

For explicit base URL, copy `.env.example` to `.env` and set:

```bash
VITE_API_BASE_URL=https://your-backend.example.com
```
