# Deploying AuraTV

This project is built with TanStack Start + Nitro, so the same codebase can ship to any host by switching the Nitro preset via the `NITRO_PRESET` environment variable.

Regardless of host, set these environment variables (from your `.env`):

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`
- `VITE_SUPABASE_PROJECT_ID`
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `SUPABASE_SERVICE_ROLE_KEY` (only if your server functions need it)
- `ADMIN_PASSWORD`
- `LOVABLE_API_KEY` (only if you use the Lovable AI Gateway)

## Lovable (default)
Click **Publish** in the Lovable editor. No config needed.

## Vercel
`vercel.json` is already included. Import the repo in Vercel — it will run `NITRO_PRESET=vercel bun run build` and serve `.vercel/output`. Add the env vars in **Project Settings → Environment Variables**.

## Netlify
`netlify.toml` is included. Import the repo in Netlify — it will run `NITRO_PRESET=netlify bun run build`. Add the env vars in **Site settings → Environment variables**.

## Cloudflare Pages / Workers
Set build command to `NITRO_PRESET=cloudflare_module bun run build` and output directory to `dist`. Add the env vars in the Pages/Workers dashboard.

## Node server (VPS, Docker, Railway, Render, Fly.io, etc.)
```
NITRO_PRESET=node-server bun run build
node .output/server/index.mjs
```
Serve behind any reverse proxy (nginx, Caddy). Set env vars in your process manager.

## Static-only
Not supported — the app has server functions (auth, admin, streaming proxy). You need a host that runs JS on the server.
