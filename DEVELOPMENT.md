# Development Guide

## Getting started

No build step for the app itself — `index.html` is plain HTML/CSS/JS.

```bash
git clone https://github.com/teekaysharma/VisualRiskAssessor.git
cd VisualRiskAssessor
python3 -m http.server 8000
```

Open `http://localhost:8000`. Camera access and the service worker both
require `localhost` or `https://` — they won't work over `file://`.

Every code change should be verified live in a browser before it's
considered done, not just read back. When testing AI-provider behavior,
clear the service worker and caches first (`navigator.serviceWorker
.getRegistrations()` → unregister, `caches.keys()` → delete) — otherwise
stale cached JS gets served silently and edits appear not to take effect.

## Project layout

- `index.html` — the entire app. Single file by design; there's no bundler
  or module system to fight with.
- `packages/risk-core/` — the one place risk scoring/banding logic lives
  (TypeScript, with tests). Build it and copy the output to the repo root:
  ```bash
  cd packages/risk-core
  npm install
  npm run build
  cp dist/risk-core.js ../../risk-core.js
  ```
  `index.html` loads the root-level `risk-core.js`, not the `dist/` copy —
  don't forget the copy step after a change.
- `demo-proxy/worker.js` — Cloudflare Worker for the no-key demo mode. Holds
  a shared, rate-limited Groq key server-side. Deploying/rotating this is
  outside the scope of the client app.
- `sw.js` — service worker (cache-first with background revalidation).
- `disclaimer.html` — standalone legal page, no shared JS with the app.

## AI hazard detection

`geminiHazards()` in `index.html` builds one prompt and routes it to
whichever provider is selected (`AI_PROVIDERS` config: Groq, Anthropic,
Gemini). The prompt uses explicit per-category checklists (fire, height,
excavation, transport, machinery, electrical, confined space, manual
handling) rather than a single generic instruction — checklists measurably
outperform generic instructions for VLM attention. If you're adding a new
hazard category, follow that pattern rather than adding another line to the
generic instruction, and cite a real source (NEBOSH, ADOSH-SF, ISO 45001)
for the checklist content rather than inventing it.

A response with zero hazards found is a *valid* AI result, not a failure —
don't reintroduce logic that falls back to COCO/rule-based detection just
because the count is 0. See `analyze()`'s handling of `aiResult !== null`.

## Testing

There's no test suite for `index.html` itself yet — verify changes live in
a browser (see above). `packages/risk-core` has its own test suite:

```bash
cd packages/risk-core
npm test
```

## Commit messages

Conventional commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`) —
see git log for the established style.
