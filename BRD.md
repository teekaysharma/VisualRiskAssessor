# VisualRiskAssessor — Business Requirements Document

**Status**: Living reference document. Last updated 2026-09-05.
**Owner**: TKSharma (UAE-based HSE / GHG verification consultant).
**Repository**: [teekaysharma/VisualRiskAssessor](https://github.com/teekaysharma/VisualRiskAssessor)
(Apache License 2.0), live at
[teekaysharma.github.io/VisualRiskAssessor](https://teekaysharma.github.io/VisualRiskAssessor/).

This document is the standing reference for what VisualRiskAssessor is, why
it exists, what it currently does, and where it's headed. It is meant to be
read and edited over time as the product changes — not a one-time snapshot.

---

## 1. Executive Summary

VisualRiskAssessor is a photo-based occupational health and safety (HSE)
risk assessment tool. A user photographs a workplace, an AI vision model
identifies hazards against sourced, category-specific checklists, and the
tool produces an ISO 45001 / ADOSH-SF-aligned 5×5 risk score. Every AI
finding is reviewable and editable by a human before it counts toward a
signed-off assessment — the tool assists a competent HSE professional, it
never replaces their judgement or issues a finding with independent
regulatory standing.

It ships as a single-file, installable, offline-first Progressive Web App
(PWA) with no required backend, no mandatory account, and no server-side
storage of assessment data by default. This is a deliberate, load-bearing
design choice, not an accident of scope — see §5 (Non-Functional
Requirements) and §7 (Risks).

## 2. Problem Statement / Business Objectives

**Problem**: Site-level HSE risk assessment is traditionally a slow, paper-
or spreadsheet-driven process. A competent person walks a site, manually
identifies hazards against memorised or looked-up checklists, scores each
one, and writes up a report — often hours after the walk, from memory and
notes. Findings are inconsistent across assessors, checklist coverage
depends on what the individual remembers to check, and there is no
structured record of which hazard categories an assessor's own judgement
tends to miss.

**Objective**: Compress that process to "point a camera, review the AI's
findings, sign off" — without removing the human's authority or judgement,
and without fabricating confidence the tool doesn't have. Every design
decision in §4-§5 traces back to one of these:
- Reduce the time from site walk to a defensible, standards-aligned written
  assessment.
- Never let an AI finding masquerade as more certain than it is — a
  low-confidence or unidentifiable finding must be forced into human review,
  not silently dropped or silently accepted.
- Keep the assessor's photographic evidence and site data under their own
  control by default (offline-first, local storage, no server dependency)
  given the personal-data sensitivity of workplace photography.
- Ground every checklist item and scoring formula in a citable, real
  standard (NEBOSH, ADOSH-SF, ISO 45001, IS 14489:2018, Fine-Kinney/Kinney &
  Wiruth 1976) — never invent hazard criteria or thresholds.

## 3. Scope

**In scope**:
- Single-photo hazard identification and risk scoring for site-level HSE
  assessments, primarily construction/industrial/general-workplace contexts
  in a UAE regulatory setting (ADOSH-SF as the default standard).
- Human-in-the-loop review, override, and formal competent-person sign-off.
- PDF and plain-text export of a finished assessment.
- Local, on-device history of past assessments, with trend/reporting views
  across repeat visits to the same named site.
- Multiple AI vision providers (bring-your-own-key), plus a shared
  no-key demo mode for evaluation only.

**Out of scope** (by design, not oversight — revisit only with explicit
justification):
- Permit-to-work workflow, contractor prequalification, LMS/training
  content delivery, ERP-style asset management. These are real features of
  larger commercial/open-source EHS platforms (SafetyCulture/iAuditor,
  Autonomous-EHS-Management, HSE-Digital-Toolkit — see the competitive scan
  referenced in §6) but would turn this into a different product than the
  one built and positioned here.
- Continuous/video-based CCTV hazard monitoring (a different product
  category — Construction-Hazard-Detection, securade/hub — this tool is
  single-photo, point-in-time assessment).
- Any assessment claim of site-wide "compliance." The tool's own IS
  14489:2018 gap-analysis panel explicitly reports "no issue observed in
  this photo," never "compliant" — a single photo cannot prove site-wide
  conformance, and the product does not claim otherwise anywhere.

## 4. Stakeholders

| Stakeholder | Interest |
|---|---|
| Owner/maintainer (TKSharma) | Primary user (own HSE/GHG consulting practice) and sole developer/product owner. |
| Site HSE officers / competent persons (end users) | Need fast, defensible, standards-cited assessments they can sign their name to. |
| Site workers photographed | Subjects of photo-derived personal data — see PDPL/DIFC note in §5. |
| Anyone forking/using the public repo | Apache 2.0 licensed — any organisation can self-host or adapt it. |

## 5. Functional Requirements

### 5.1 Core assessment loop (MVP, shipped)
- Photo capture or upload (single or batch), or manual hazard entry when no
  photo is available.
- AI hazard detection via a shared prompt routed to the selected provider
  (Groq, Anthropic Claude, or Google Gemini — bring-your-own-key), or a
  shared rate-limited no-key demo mode (Cloudflare Worker proxy).
- Explicit, sourced, per-category checklists rather than one generic
  instruction — currently: Fire, Working at Height, Excavation, Workplace
  Transport, Struck-by/Machinery Guarding, Electrical, Confined Space,
  Manual Handling, Housekeeping, Chemical/Hazardous Materials Storage, PPE
  Compliance (all seven standard PPE categories — Head/Eye/Hand/Foot/
  Hearing/Respiratory/Other), Ladders (condition/tagging), and Scaffold
  Tags (Red/Green/Yellow inspection-tag system). Sourced against the NEBOSH
  National General Certificate handbook, ADOSH-SF Codes of Practice, and
  (for the two newest categories) the Safety Professionals Reference
  (David Yates, 4th ed.).
- A rule-based/keyword fallback and a COCO-SSD object-detection fallback
  when no AI key/connectivity is available — always visibly labelled as a
  weaker result, never presented as if it were a full AI assessment.
- Mandatory human review gate: any AI finding below the confidence
  threshold, or an "unclear" (unidentifiable object) finding, blocks
  sign-off until a human has individually reviewed it.
- Selectable risk-scoring methodology: ADOSH-SF 5×5 (default), NEBOSH/HSG65
  3×3, or Fine-Kinney (Likelihood × Exposure × Consequence) — switching
  recomputes the risk register, matrix, and hero score live, before export.
  `packages/risk-core` is the single source of truth; the ADOSH-SF path is a
  byte-identical adapter over the original scoring code (pinned by test).
- Hierarchy of Controls (Eliminate/Substitute/Engineering/Administrative/
  PPE) generated per hazard.
- Competent-person sign-off: name, designation, date, and an explicit
  confirmation statement referencing ISO 45001:2018 and applicable UAE
  OH&S legislation, linked to a standalone legal disclaimer.
- PDF export (risk register, corrective-action tracker, Hierarchy of
  Controls, risk matrix or — for Fine-Kinney — a per-hazard L×E×C
  breakdown, IS 14489 photo-assessable gap analysis, HSE officer review
  summary) and a plain-text export, both scoring-method-aware.
- IS 14489:2018 Photo-Assessable Gap Analysis: reports only the clauses a
  photograph can actually speak to, as "non-conformity observed" (cited
  against a real detected hazard) or "no issue observed in this photo" —
  never "compliant," and never lists the ~80% of the standard's audit scope
  (management systems, training records, PHA methodology) that no photo
  could ever assess.

### 5.2 Data, history, and reporting (MVP, shipped)
- Local-only history (IndexedDB) — export/import for backup, no
  server-side storage.
- Offline-first PWA: service worker caching, and photos taken offline are
  queued and analyzed once connectivity returns.
- Site name + coarse GPS tagging (best-effort, not survey-grade).
- Site Trends: groups saved assessments by site name, shows risk trend
  across repeat visits (score delta since first visit) and flags hazard
  categories recurring across 2+ separate visits — a persistent finding,
  not a one-off.
- AI Trust Signals: per-hazard-category tally of how often the AI's
  findings needed a human correction (overridden, flagged for mandatory
  review, or missed entirely and added by hand), aggregated across all
  saved history — surfaces which checklist categories most need
  strengthening. Not seen in any comparable tool researched to date (see
  §6) — a genuine differentiator, not parity work.

### 5.3 Implementation status note

Everything in §5.1–5.2 has been built and individually verified live in a
browser. As of this writing, several pieces exist as open, tested, but not
yet merged pull requests against `main` (PPE/Ladders/Scaffold-tags
checklists, AI Trust Signals, Site Trends, selectable scoring methodology) —
this section describes the product's designed and implemented functional
scope, not a claim that every listed capability is already live in the
public GitHub Pages deployment at any given moment. Check `git log main` for
current merge state.

## 6. Non-Functional Requirements

- **Offline-first architecture**: the app must remain usable with no
  network connection beyond the initial load; a queued photo taken offline
  must never be silently lost.
- **No-backend-by-default**: no assessment data leaves the user's device
  unless they explicitly export it or choose to use a bring-your-own-key AI
  provider (in which case only the photo and prompt go to that provider,
  never to any server this project controls, with the sole exception of the
  optional shared demo-mode proxy).
- **UAE PDPL / DIFC / ADGM considerations for photo-derived personal
  data**: workplace photographs can capture identifiable individuals.
  Sourced research (retained in project memory) confirms photos are
  personal data under UAE PDPL, no "legitimate interests" processing basis
  currently exists in the PDPL executive regulations (still not issued as
  of last check), and DIFC Data Protection Regulation 10 specifically
  governs AI processing for DIFC-registered entities. This shapes the
  "local-first, no mandatory server, user controls their own export"
  design — it is a compliance-driven constraint, not an incidental
  architecture preference.
- **Apache 2.0 licensing**: chosen over MIT specifically for its clearer,
  more thorough patent and contribution terms; applied consistently across
  LICENSE, CONTRIBUTING.md, and in-app footer links.
- **Human-accountable sign-off as a legal/liability design constraint**:
  the competent-person sign-off step and its linked legal disclaimer exist
  because the tool's AI output has no independent regulatory standing —
  every scoring-method change, every new checklist, and the mandatory
  review gate are all designed to keep that accountability chain intact,
  never to quietly weaken it for convenience.
- **Every checklist item and scoring formula must be sourced**: no
  invented hazard criteria, no fabricated thresholds. Where this project's
  own bridging logic is used (e.g. mapping the app's native 1–5 scale into
  NEBOSH's 1–3 scale, or into Fine-Kinney's native scales), the code and
  the UI both say explicitly that the bridging is this app's own choice,
  not part of the cited standard.

## 7. Growth Roadmap Beyond MVP

Organised by how settled each item is, not by delivery order.

**Committed, scoped, ready to build:**
- Ergonomics checklist broadening (static/awkward-posture items beyond
  lifting/carrying, reusing the existing `ergonomic` hazard key) — sourced
  from the Handbook of Human Factors' confirmation that posture-based
  ergonomic assessment is designed to work from photographs.

**Explicitly flagged, not yet scoped (real open questions, not decisions
deferred out of laziness):**
- A REBA/RULA-style posture-scoring layer, analogous to how Fine-Kinney was
  added as a scoring method. Open questions before this becomes a real
  plan: can a vision model reliably estimate posture angles from a single
  2D photo well enough for such a score to mean anything (versus producing
  a plausible-looking fabricated number), and what does a numeric posture
  score add over the honest "ergonomic hazard flagged, human review
  required" behaviour the app already has today. If pursued, the natural
  home is `packages/risk-core`'s existing `SCORING_METHODS` registry, and
  the open-source `@smartqhse/hse-calculators` library (MIT) already has
  REBA/RULA implementations worth reusing rather than rebuilding.
- KPI dashboard (on-screen and PDF) currently has its own independent
  hard-coded thresholds, unrelated to the selectable-scoring-methodology
  work — needs its own equivalent-bands design per method.
- Persisting which scoring method was used per saved history record, and
  what that means for Site Trends comparing scores across visits
  potentially scored under different methods.
- Corrective-action tracking with real persistence (status/assignee/due
  date across visits, not just a one-time PDF snapshot table).
- Draft/resume mid-assessment (insurance against connectivity drops or
  interruptions during a site walk).
- Multi-language UI (at minimum Arabic, given the explicit UAE market;
  Hindi/Urdu given the labour demographic) — real evidence of need
  (Construction-Hazard-Detection ships 8 languages), but a genuinely large
  effort for a single-file app with no i18n infrastructure today.
- WCAG 2.2 AA as an explicit, tracked accessibility target.
- Photo annotation/markup (drawing a marker at the hazard location on the
  image itself, carried into the PDF) — offered by the closest commercial
  analog (FindRisk) and most commercial inspection apps generally.
- A Help section plus an opt-in, user-initiated, anonymized usage-insights
  feedback mechanism (GitHub issue or email, person reviews the exact
  payload before anything sends) — discussed but not yet built. Payload
  would reuse the AI Trust Signals aggregation and explicitly exclude
  photos, GPS, site names, and any free-text fields that could echo
  something identifying from a photo.

**Explicitly not recommended** (see §3, Out of scope): permit-to-work
workflow, contractor prequalification, LMS/training content,
ERP-style asset management, continuous/video CCTV monitoring.

## 8. Risks

| Risk | Mitigation already in place |
|---|---|
| AI hallucination / fabricated findings presented as fact | Mandatory human review gate for low-confidence/unclear findings; `RiskCore.validateHazards()` rejects malformed model output rather than coercing it into a plausible number; competent-person sign-off and legal disclaimer make clear the AI does not carry independent regulatory authority. |
| Third-party AI provider dependency (API changes, deprecation, downtime) | Multi-provider support (Groq/Anthropic/Gemini) plus rule-based and COCO-SSD fallbacks, always visibly labelled as weaker results. |
| PDPL/DIFC exposure from photo-derived personal data | Local-only storage by default, no mandatory server, user controls export; any future feedback mechanism (see §7) is opt-in and excludes photos/GPS/free text by design, not by after-the-fact redaction. |
| Misattributing this project's own bridging/interpretation as part of a cited standard | Every bridging function and UI note says explicitly what is this app's own choice versus what is sourced directly from NEBOSH/ADOSH-SF/ISO 45001/Kinney & Wiruth. |
| Scope creep into a full EHS suite, diluting the tool's actual value proposition | Explicit "out of scope" list in §3, revisited and reaffirmed at each roadmap review rather than drifting by accretion. |

## 9. Glossary

- **ADOSH-SF** — Abu Dhabi Occupational Safety and Health System Framework; source of this app's default 5×5 risk matrix and most-cited Codes of Practice.
- **NEBOSH** — National Examination Board in Occupational Safety and Health (UK); source of the National General Certificate handbook used to ground several checklists, and the 3×3 HSG65-style scoring method.
- **Fine-Kinney** — Kinney & Wiruth's (1976) Likelihood × Exposure × Consequence risk-scoring method, offered as a selectable alternative.
- **IS 14489:2018** — Indian Standard, "Occupational Health and Safety Audit — Code of Practice"; source of the Photo-Assessable Gap Analysis panel's Annex C clause references.
- **HOC** — Hierarchy of Controls (Eliminate, Substitute, Engineering, Administrative, PPE).
- **PWA** — Progressive Web App; an installable, offline-capable web app with no app-store distribution required.
- **PDPL** — UAE Federal Decree-Law on the Protection of Personal Data.
- **AI Trust Signals** — this project's own term for the per-category tally of how often AI findings required human correction (see §5.2).
