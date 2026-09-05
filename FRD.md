# VisualRiskAssessor — Functional Requirements Document

**Status**: Living reference document. Last updated 2026-09-05.
**Companion to**: [BRD.md](BRD.md) — read that first for the business
rationale behind each requirement below; this document is the technical
"what it does and how," not the "why it exists."

Requirement IDs are stable identifiers for future reference (in commits,
issues, or test plans), not a delivery sequence. Each maps to a
`§`-numbered BRD section where relevant, noted as **[BRD §x]**.

---

## 1. Introduction

### 1.1 Purpose
Specifies, at implementation-relevant detail, every function the app
performs: inputs, processing, outputs, and the business rules governing
each. Where the BRD says *what the product must achieve and why*, this
document says *what the software actually does, precisely*.

### 1.2 Scope
Covers the single-page application (`index.html`), the `packages/risk-core`
scoring module, the `demo-proxy` Cloudflare Worker, and the service
worker/PWA layer. Does not cover deployment/CI mechanics (see
`DEVELOPMENT.md`) or contribution process (see `CONTRIBUTING.md`).

### 1.3 Document conventions
- **FR-n.m** — a functional requirement, grouped by module (n) and
  numbered within it (m).
- "Shall" — mandatory behavior. "Should" — a documented default that a
  developer could reasonably change without violating the requirement's
  intent.
- Source citations (NEBOSH, ADOSH-SF CoP numbers, IS 14489:2018, Kinney &
  Wiruth 1976) are reproduced from the actual in-code checklist/citation
  text, not re-derived — see the cited file for the authoritative wording.

### 1.4 Definitions
See BRD.md §9 (Glossary). Additional terms used only here are defined
inline at first use.

## 2. System Overview

VisualRiskAssessor is a single HTML file (`index.html`, ~5,000+ lines:
markup, CSS, and three inline `<script>` blocks) with one build-time
dependency: `packages/risk-core`, a TypeScript module compiled via esbuild
to an IIFE (`risk-core.js`) and loaded as a plain `<script src>` — every
export becomes a property of the global `RiskCore` object. There is no
bundler, no framework, and no build step for the app shell itself.

Three script blocks share the page but not a JS scope: the main IIFE
(camera/upload/AI/scoring/review/PDF logic — the bulk of the file), a
second block (compliance checklist and IS 14489 gap-analysis rendering,
reads `window.RiskCore`/`window.escapeHtml` directly since it cannot see
the first block's closure variables), and a third, smaller block (the
emergency-preparedness table auto-population). Anything that needs to be
called across this boundary is deliberately exposed as `window.<name>`.

External runtime dependencies, all loaded from CDN with no local install:
TensorFlow.js + COCO-SSD (fallback object detection), jsPDF + jspdf-
autotable (PDF export), html2canvas (unused-but-loaded utility).

## 3. Functional Requirements

### 3.1 Image Acquisition & Input **[BRD §5.1]**

- **FR-3.1.1** — The system shall accept a photo via device camera capture
  or file upload, and shall support selecting multiple files at once for
  batch processing (each processed and saved to history independently).
- **FR-3.1.2** — The system shall support a placeholder-image path (a 1×1
  inline SVG data URI) when an assessment is created via manual hazard
  entry with no photo at all, so the rest of the pipeline (history record,
  PDF export) never has to special-case a missing image.
- **FR-3.1.3** — The system should optionally compress a photo before
  sending it to an AI provider (toggle in Settings, default on) to reduce
  token cost and avoid demo-quota rejection on very large phone-camera
  images. The full-resolution original shall always be retained for the
  on-screen record, history, and PDF/export — compression applies only to
  the copy transmitted to the AI provider.
- **FR-3.1.4** — If a photo is captured with no network connectivity, the
  system shall queue it (with metadata) in local history with
  `status: 'queued'` rather than fail the capture, and shall offer a
  "Process Queued Now" action once connectivity returns.

### 3.2 AI Hazard Detection **[BRD §5.1]**

- **FR-3.2.1** — The system shall support at least three AI vision
  providers, user-selectable, each requiring the user's own API key:
  Groq (`qwen/qwen3.6-27b`), Anthropic Claude (`claude-sonnet-4-5`), and
  Google Gemini (`gemini-3.6-flash`). Each provider adapter shall accept
  the same `(prompt, dataUrl, apiKey, signal)` shape and return the
  model's raw text response; all downstream parsing/validation is
  provider-agnostic.
- **FR-3.2.2** — The system shall offer a no-key demo mode routed through
  a shared, rate-limited Cloudflare Worker proxy (always Groq-backed,
  regardless of which provider is selected in Settings), explicitly
  labelled for evaluation only, not for real assessments.
- **FR-3.2.3** — The AI prompt shall present hazard categories as
  explicit, named checklists (a bullet list of specific things to look
  for per category) rather than one generic "find hazards" instruction.
  As of this writing the categories are: Fire Safety, Working at Height,
  Excavation, Workplace Transport, Struck-by/Machinery Guarding,
  Electrical, Confined Space, Manual Handling, Housekeeping, Chemical/
  Hazardous Materials Storage, PPE Compliance (Head/Eye/Hand/Foot/
  Hearing/Respiratory/damaged-PPE), Ladders (condition/tagging), and
  Scaffold Tags (Red/Green/Yellow tag system) — see Appendix A for the
  full current list with sources.
- **FR-3.2.4** — An optional "Security Patrol" mode shall add
  wildlife/security/lighting checklist items to the prompt, additive to
  the standard categories and only included when that context is
  explicitly selected (never shown to a routine electrical/general
  inspection).
- **FR-3.2.5** — Every hazard candidate the AI returns shall include:
  `name`, `key` (one of a fixed, enumerated valid-keys list — currently
  `electrical, fire, machinery, slip, height, chemical, ppe, struck,
  confined, ergonomic, heat, unclear, other`, plus `wildlife, security,
  lighting` when Security Patrol is active), `likelihood` (1–5 integer),
  `severity` (1–5 integer), `confidence` (0–1), `details`, and five
  Hierarchy-of-Controls fields (`eliminate`, `substitute`, `engineering`,
  `administrative`, `ppe`).
- **FR-3.2.6** — The system shall include a standing "Unidentified
  object" instruction: when something looks potentially hazardous but
  cannot be clearly identified (angle, distance, occlusion, low light),
  the model shall report it with `key: "unclear"`, explain why in
  `details`, and shall never assign it high confidence — this exists so
  uncertain findings are routed to a human rather than silently dropped
  or given an invented identity.
- **FR-3.2.7** — The prompt shall end with a self-verification pass
  instructing the model to re-examine the image specifically for missed
  hazards in corners/edges, the background, the floor/ground, and
  partially obscured items, before finalizing its answer.
- **FR-3.2.8** — `RiskCore.validateHazards()` shall reject (not coerce) any
  candidate with a missing or out-of-range `likelihood`/`severity`, and
  shall clamp a missing/non-numeric `confidence` to `0` (never default it
  upward) — a malformed model response shall never silently produce a
  fabricated score or an artificially confident finding.
- **FR-3.2.9** — Fallback chain: if no AI key is configured (and demo mode
  is off), the system shall attempt COCO-SSD object detection, and if that
  model is unavailable, shall fall back to rule-based keyword matching
  against the image filename. A genuine empty result from a real AI call
  (the model looked and found nothing) is a valid outcome and shall
  **not** trigger a fallback to a weaker detector — only the absence of a
  usable AI result does.
- **FR-3.2.10** — The active detection mode (`ai` / `coco` / `rule-based` /
  `manual` / `baseline`) shall always be visibly displayed to the user,
  including which provider actually produced an AI result — a COCO or
  rule-based result shall never be visually indistinguishable from a full
  AI assessment.

### 3.3 Risk Scoring **[BRD §5.1]**

- **FR-3.3.1** — `packages/risk-core` shall be the single source of truth
  for scoring/banding; no other module shall reimplement the matrix.
- **FR-3.3.2** — The default method shall be ADOSH-SF: `score = Likelihood
  × Severity` (both 1–5 integers), banded Low (1–3) / Moderate (4–6) /
  High (8–12) / Extreme (15–25), per ADOSH-SF Technical Guideline
  "Process of Risk Management" v4.0, Table 3.
- **FR-3.3.3** — The system shall offer two additional, user-selectable
  methods without altering the AI's native 1–5 output scale:
  - **NEBOSH/HSG65** (3×3): the app's 1–5 scale is compressed to NEBOSH's
    1–3 scale as `{1,2}→1, {3,4}→2, {5}→3`; score = compressed L × S;
    banded Low (1–2) / Medium (3–4) / High (6–9).
  - **Fine-Kinney** (Kinney & Wiruth, 1976): Likelihood and Consequence
    are derived from the app's 1–5 scale via a fixed lookup table onto
    Kinney's native scales; **Exposure** has no 1–5 analog and is a new,
    directly-entered field (default: 3, "Occasional/weekly"). Score =
    Likelihood × Exposure × Consequence; banded Slight (<20) / Possible
    (20–<70) / Substantial (70–<160) / High (160–<320) / Very High (≥320),
    boundaries lower-bound-inclusive.
  - Every bucket-mapping function's doc comment, and the on-screen
    bridging-note tooltip next to the method selector, shall state
    explicitly that the mapping is this app's own choice, not part of
    either cited standard.
- **FR-3.3.4** — Switching the selected method shall recompute the risk
  register, both matrix representations, and the hero score live, with no
  page reload. Fine-Kinney, having no 2D grid (Exposure varies per hazard,
  not per grid cell), shall swap the Risk Matrix tab to a per-hazard
  Likelihood × Exposure × Consequence = Score breakdown list instead of
  drawing a grid.
- **FR-3.3.5** — Switching the selected method shall never alter a
  hazard's own stored native Likelihood/Severity, nor the ADOSH-SF
  registry entry's output versus the original `score()`/`band()`
  functions (regression-pinned by an exhaustive 25-pair test in
  `packages/risk-core/test/index.test.ts`).
- **FR-3.3.6** — The KPI/Risk-Summary dashboard (on-screen and PDF) is
  explicitly **out of scope** for method-awareness at this time — it
  retains its own independent hard-coded thresholds unrelated to
  `RiskCore.band()`. Persisting which method scored a given saved history
  record is likewise not yet implemented. Both are named follow-ups in
  BRD §7.

### 3.4 Human Review & Sign-off **[BRD §5.1]**

- **FR-3.4.1** — Every AI-sourced hazard shall be individually editable in
  the Hazard Register (Table A): a reviewer may open inline edit mode,
  change Likelihood/Severity via dropdown, and save — this mutates the
  hazard's own stored `L`/`S`/`risk` values directly and marks it
  `manualOverride: true`.
- **FR-3.4.2** — Independently, the HSE Officer Review tab shall let a
  reviewer override a hazard's Likelihood/Severity (and, for Fine-Kinney,
  Exposure) without touching its stored native values, recorded per-hazard
  in a separate `reviewData` structure (`override`, `reviewL`, `reviewS`,
  `reviewExposure`, `reviewControls`, `verified`). This is a distinct
  precedence layer from FR-3.4.1, resolved by `getEffectiveRiskData()` /
  `getEffectiveRiskResult()`: the review-tab's value takes precedence only
  when both `verified` and `override` are true for that hazard; otherwise
  the hazard's own (possibly Table-A-edited) values apply.
- **FR-3.4.3** — Any AI-sourced hazard with confidence below the
  configured threshold (`RiskCore.CONFIDENCE_REVIEW_THRESHOLD`, currently
  0.6), or with `key: "unclear"` regardless of its reported confidence,
  shall require the reviewer to explicitly check "Override" on that row in
  the HSE Review tab before sign-off can proceed. This gate is
  centralized in a single function (`hazardNeedsReview()`) — every
  enforcement point (the on-screen ⚠ Review badge and the actual sign-off
  gate) shall call it, never re-derive the condition independently.
- **FR-3.4.4** — Sign-off shall require a name, designation, and date, and
  an explicit checkbox confirmation referencing ISO 45001:2018 and
  applicable UAE OH&S legislation, linked to the standalone legal
  disclaimer (`disclaimer.html`). Sign-off shall be blocked while any
  hazard still fails FR-3.4.3.

### 3.5 Compliance & Gap-Analysis Reporting **[BRD §5.1]**

- **FR-3.5.1** — A Hierarchy of Controls table shall be generated per
  hazard (Eliminate / Substitute / Engineering / Administrative / PPE),
  sourced from the AI's own per-hazard recommendation fields, falling back
  to the matching `hazardDB` category's generic recommendations when the
  AI provides none.
- **FR-3.5.2** — A Compliance Coverage Status checklist shall report, per
  ISO 45001/ADOSH-SF clause, whether the current assessment satisfies it
  (e.g. hazard identification completed, risk scoring applied — now
  citing whichever method is selected per FR-3.3.3, Hierarchy of Controls
  recorded, competent-person sign-off, emergency preparedness recorded).
- **FR-3.5.3** — An IS 14489:2018 Photo-Assessable Gap Analysis panel
  shall report, per included Annex C clause (Work at Height, Confined
  Space, Physical Hazard/Housekeeping, Electrical, Chemical Storage,
  Fire/Explosion — the clauses a photograph can actually speak to), either
  "non-conformity observed" (cited against an actual detected hazard by
  key) or "no issue observed in this photo." It shall **never** report a
  clause as "compliant," and shall **never** list a clause that cannot be
  photo-assessed (management systems, training records, PHA methodology,
  and similar) — omission, not a false "N/A," is the honest representation
  for those.

### 3.6 Reporting & Export **[BRD §5.1]**

- **FR-3.6.1** — PDF export shall include, at minimum: assessment
  metadata (date, site, analysis method, **scoring method and its
  citation**, overall score/level), the assessed image, a KPI/risk summary
  (see FR-3.3.6 scope note), the full risk register (with method-aware
  score/rating per FR-3.3.4), Hierarchy of Controls, a risk matrix page
  (grid, or the Fine-Kinney breakdown list per FR-3.3.4), a legal
  disclaimer, a signature block, and — when review data exists — an HSE
  Officer Review summary (reviewer info, and, when any hazard is
  overridden or verified, a Hazard Review & Override Summary table
  showing AI vs. reviewed L/S, the resolved Score/Rating via
  `getEffectiveRiskResult()`, source, and verified status).
- **FR-3.6.2** — A plain-text export shall summarize the same assessment
  (site, date, scoring method, overall level/score, and one line per
  hazard with source label, L/S, method-aware risk score, residual risk,
  owner, and status) for contexts where a PDF viewer isn't convenient.
- **FR-3.6.3** — All AI-derived free-text fields (`name`, `details`, and
  the five Hierarchy-of-Controls fields) shall be HTML-escaped before
  being interpolated into any `innerHTML` rendering path, on-screen or in
  generated report sections — a crafted or compromised model response
  shall never be able to inject markup/script into the reviewer's browser.

### 3.7 History & Local Storage **[BRD §5.2]**

- **FR-3.7.1** — Every completed or queued assessment shall be persisted
  to a local IndexedDB database (`vra-history`, object store
  `assessments`, keyed by a generated `id`) — no server-side storage,
  consistent with BRD §5 (no-backend-by-default).
- **FR-3.7.2** — A saved record shall include: id, timestamp, site
  context (industry/task/location/site name/GPS), analysis mode, overall
  score, the full hazard arrays (baseline/AI/manual), sign-off data, image
  metadata, a thumbnail, and (once the AI Trust Signals feature is
  merged) a `trustSignals` aggregation object.
- **FR-3.7.3** — History shall support export to a file and import from
  one, with a user choice of including embedded photos or data only, for
  backup/portability without any server round-trip.
- **FR-3.7.4** — A record captured offline shall be saved with
  `status: 'queued'` and rendered distinctly in the History list (dashed
  border, "not yet analyzed" label) rather than appearing as if it were a
  completed assessment.

### 3.8 Site Trends **[BRD §5.2]**

- **FR-3.8.1** — The system shall group saved (non-queued) history records
  by site name (trimmed, case-insensitive exact match against the "What
  are we analyzing?" field) and shall only display a trend for a site with
  2 or more saved visits — a single visit has nothing to trend against.
- **FR-3.8.2** — For each qualifying site, the system shall display a
  per-visit table (date, method-banded risk score, hazard count, signed-
  off status) sorted oldest-to-newest, and the net change in overall score
  from the first visit to the latest (worded as "Improved by n" /
  "Worsened by n" / "No change").
  - GPS-based fuzzy proximity matching is explicitly out of scope — only
    an exact typed-name match is attempted.
- **FR-3.8.3** — The system shall flag any hazard category (`key`) that
  appears in 2 or more separate visits to the same site as "recurring,"
  distinct from a one-off finding — this is the primary actionable signal
  the view is designed to surface.

### 3.9 AI Trust Signals **[BRD §5.2]**

- **FR-3.9.1** — At save-to-history time, the system shall tally, per
  hazard category (`key`): how many `"ai"`-sourced findings in that
  category were accepted as-is, how many were overridden by a human
  review (`reviewData[i].override === true`), how many were flagged for
  mandatory review per FR-3.4.3, and how many hazards in that category
  were missed by the AI and added manually (`source: "manual"`).
  `"baseline"`-sourced (pre-loaded fixture) hazards and rule-based/COCO
  fallback hazards carry no AI signal and are excluded from this tally by
  design.
- **FR-3.9.2** — An on-demand aggregate view shall sum these tallies
  across every saved history record and present them sorted by total
  correction activity (overridden + missed) descending — the categories
  most in need of checklist strengthening surface first, not an
  alphabetical or chronological list.

### 3.10 Settings & Configuration **[BRD §5.1]**

- **FR-3.10.1** — The AI provider, and the API key for it, shall be
  configurable in Settings; the key shall be held in the browser only
  (never persisted to IndexedDB or transmitted anywhere but the selected
  provider's own API endpoint, or the demo proxy in demo mode).
- **FR-3.10.2** — The scoring-method selector (FR-3.3.3) shall be visible
  regardless of which results tab (Hazard Register / Risk Matrix /
  Compliance / Emergency Plan / HSE Review) is active, since it drives all
  of them simultaneously.

## 4. Data Requirements

### 4.1 Hazard object (in-memory, per finding)
`name, key, L, S, risk, exposure, conf, recs[], source ('ai'|'baseline'|
'manual'|'rule-based'), needsReview, details, eliminate, substitute,
engineering, administrative, ppe, manualOverride, verified, isEditing`.
`exposure` defaults to `RiskCore.FINE_KINNEY_DEFAULT_EXPOSURE` (3) at the
point of use for any hazard that doesn't carry its own value, rather than
being force-written onto every hazard-creation site — see BRD §5.1's
selectable-scoring-methodology entry.

### 4.2 Review data (per hazard index, HSE Review tab only)
`override, reviewL, reviewS, reviewExposure, reviewControls, verified` —
keyed by the hazard's position in `[...baselineHazards, ...hazards,
...manualHazards]`, **not persisted** to history today (a known gap: a
reopened history record cannot show what was overridden in its review
tab, only the hazard's own final values).

### 4.3 History record (IndexedDB, `vra-history`/`assessments`)
See FR-3.7.2. Schema-less store (`keyPath: 'id'` only) — new fields (e.g.
`trustSignals`) can be added without a version migration; a record saved
before a field existed simply lacks it, and reading code must treat that
as "no data," never as an error.

## 5. External Interface Requirements

- **AI provider APIs** (Groq/Anthropic/Gemini): one image + one text
  prompt per request, JSON hazard array expected in the response text
  (fenced or bare), parsed defensively (`<think>` tags and code fences
  stripped before `JSON.parse`).
- **Demo proxy** (`demo-proxy/worker.js`, Cloudflare Worker): stateless
  forwarder to Groq holding a shared key server-side; no database of any
  kind, confirmed by direct read.
- **Browser APIs relied on**: `MediaDevices.getUserMedia` (camera),
  `Geolocation` (site GPS, best-effort), `IndexedDB` (history),
  `ServiceWorker` (offline caching), `File`/`Blob` (upload, export/import,
  PDF/image generation).

## 6. Non-Functional Requirements (function-linked)

See BRD §6 for the full list; the ones with direct functional
consequences here:
- No assessment may be silently lost offline (FR-3.1.4, FR-3.7.4).
- No AI output may be trusted at face value into a scored, signed-off
  assessment without passing FR-3.2.8 (validation) and, where applicable,
  FR-3.4.3 (mandatory review gate).
- No AI-derived text may reach `innerHTML` unescaped (FR-3.6.3).

## 7. Assumptions & Constraints

- The AI vision model can identify workplace objects/conditions well
  enough for a checklist-guided prompt to be useful; it cannot verify
  facts not visible in the frame (e.g. whether a hazard was previously
  reported, or a permit's actual paperwork status beyond a visible tag).
- A single photo represents a single point in time and cannot establish
  site-wide compliance — enforced explicitly in FR-3.5.3, and this
  constraint should be treated as binding on any future reporting feature
  too.
- `packages/risk-core`'s scoring/banding logic and the AI prompt/
  validation schema are treated as a stable contract; changes to either
  require the exhaustive regression tests noted in FR-3.3.5 to keep
  passing unchanged.

## 8. Traceability to BRD

| FRD section | BRD reference |
|---|---|
| 3.1–3.2 (Input, AI Detection) | §5.1 Core assessment loop |
| 3.3 (Risk Scoring) | §5.1; roadmap items in §7 |
| 3.4 (Review & Sign-off) | §5.1; §6 human-accountable sign-off |
| 3.5 (Compliance/Gap Analysis) | §5.1; §3 scope (never claims compliance) |
| 3.6 (Reporting/Export) | §5.1 |
| 3.7 (History/Storage) | §5.2; §6 no-backend-by-default |
| 3.8 (Site Trends) | §5.2 |
| 3.9 (AI Trust Signals) | §5.2 |
| 3.10 (Settings) | §5.1; §6 UAE PDPL/DIFC note |

## Appendix A — Current AI-Prompt Checklist Categories

| Category | Hazard key | Primary source |
|---|---|---|
| Fire Safety | `fire` | ADOSH-SF CoP; NEBOSH handbook |
| Working at Height | `height` | NEBOSH handbook; ADOSH-SF CoP 23.0 |
| Excavation | `other` | ADOSH-SF CoP 29.0 |
| Workplace Transport | `struck` | NEBOSH handbook |
| Struck-by / Machinery Guarding | `machinery` / `struck` | ADOSH-SF CoP 47.0 |
| Electrical | `electrical` | ADOSH-SF CoP 15.0 |
| Confined Space | `confined` | ADOSH-SF CoP 27.0 |
| Manual Handling | `ergonomic` | NEBOSH handbook |
| Housekeeping | `slip` | NEBOSH handbook |
| Chemical / Hazardous Materials Storage | `chemical` | ADOSH-SF CoP |
| PPE Compliance (incl. Hearing, Respiratory) | `ppe` | ADOSH-SF CoP 2.0/3.0; Safety Professionals Reference (Yates, 4th ed.) |
| Ladders (condition & tagging) | `height` | ADOSH-SF CoP 37.0 |
| Scaffold Tags | `height` | Petrojet HSE manual; ADOSH-SF CoP 26.0; NEBOSH handbook |

Not yet in the prompt (identified, deferred — see BRD §7): Lifting
Operations/Rigging, Lockout/Tagout, broader Ergonomics beyond manual
handling.
