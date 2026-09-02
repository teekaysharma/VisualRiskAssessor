# VisualRiskAssessor

A photo-based HSE (Health, Safety, Environment) risk assessment tool. Point a
camera at a workplace, get AI-assisted hazard detection and an ISO 45001 /
ADOSH-SF aligned 5×5 risk score back, then review, override, and sign off as
the competent person of record before it counts as a finished assessment.

Runs entirely as a single-page, installable web app (PWA) — no build step,
no backend required for the app itself. Live at
[teekaysharma.github.io/VisualRiskAssessor](https://teekaysharma.github.io/VisualRiskAssessor/).

## Features

- **AI-assisted hazard detection** — bring your own API key for Groq,
  Anthropic Claude, or Google Gemini vision models, or try the shared demo
  quota with no key needed. Explicit per-category checklists (fire, working
  at height, excavation, workplace transport, machinery/struck-by,
  electrical, confined space, manual handling) sourced against the NEBOSH
  National General Certificate handbook and ADOSH-SF Codes of Practice.
- **Human-in-the-loop by design** — every AI finding is reviewable and
  editable. Low-confidence or unidentified findings are flagged for
  mandatory human review and block sign-off until resolved.
- **5×5 risk matrix** — Likelihood × Severity scoring with a shared,
  single-source-of-truth scoring module (`packages/risk-core`) used by both
  the on-screen matrix and PDF export, so they can never disagree.
- **Hierarchy of Controls** — Eliminate / Substitute / Engineering /
  Administrative / PPE recommendations for every hazard.
- **PDF export** — a formatted report including the risk register,
  corrective action tracker, hierarchy of controls, and risk matrix.
- **Local history** — assessments are saved in the browser (IndexedDB), with
  export/import for backup, and no server-side storage.
- **Offline-first** — a service worker caches the app for offline use, and
  photos taken with no connection are queued and analyzed once you're back
  online.
- **Site name + coarse GPS tagging** — auto-detects device location (best
  effort, not survey-grade) and lets you name what's being assessed.
- **Competent-person sign-off** — a formal review/override/sign-off step
  before an assessment is considered final, with a linked
  [legal disclaimer](disclaimer.html).

## Running locally

This is a static single-page app — any static file server works:

```bash
python3 -m http.server 8000
# or
npx http-server -p 8000
```

Then open `http://localhost:8000`. Camera access requires `localhost` or
`https://` (not `file://`).

## Project structure

- `index.html` — the entire app (HTML/CSS/JS), single file by design.
- `packages/risk-core/` — TypeScript module for risk scoring/banding, built
  to `risk-core.js` and vendored at the repo root for the app to load.
- `demo-proxy/` — Cloudflare Worker backing the no-key demo mode (holds a
  shared, rate-limited Groq key server-side; not required to run the app
  with your own key).
- `sw.js` / `manifest.json` — service worker and PWA manifest.
- `disclaimer.html` — standalone legal disclaimer page.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE). This tool assists trained HSE
personnel; it does not replace their judgement or carry any legal/regulatory
authority of its own — see the [disclaimer](disclaimer.html) for full terms.
