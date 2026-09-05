/**
 * risk-core: the single, authoritative implementation of risk scoring and
 * banding for VisualRiskAssessor. Every client (web, Android, future PWA)
 * must call into this module rather than reimplementing the matrix.
 *
 * Source: ADOSH-SF Technical Guideline "Process of Risk Management",
 * Version 4.0, Table 3 - Risk Rating. Score = Likelihood x Severity
 * (both 1-5), banded 1-3 / 4-6 / 8-12 / 15-25. With both factors
 * restricted to 1-5, the only products that can ever occur are
 * {1,2,3,4,5,6,8,9,10,12,15,16,20,25} - values like 7, 11, 13, 14 never
 * occur, so these bands cover every reachable score with no gaps.
 */

export type RiskBandKey = "low" | "moderate" | "high" | "extreme";

export interface RiskBand {
  key: RiskBandKey;
  label: string;
  /** Hex background colour used consistently across UI and matrix. */
  bg: string;
  /**
   * Hex text colour for use against `bg`. WCAG 2.2 AA requires 4.5:1 for
   * normal-size text; white-on-`bg` only clears that for "extreme"
   * (5.44:1) and "moderate" already used dark text. "low" (2.87:1) and
   * "high" (2.85:1) both failed against white, so they use the same dark
   * text as "moderate" instead of a new colour.
   */
  textColor: string;
  /** Same colour as `bg`, pre-split for jsPDF's setFillColor(r, g, b). */
  rgb: readonly [number, number, number];
  /** Same colour as `textColor`, pre-split for jsPDF's setTextColor(r, g, b). */
  textRgb: readonly [number, number, number];
}

export const RISK_BANDS: readonly RiskBand[] = [
  { key: "low", label: "Low Risk", bg: "#27ae60", textColor: "#212121", rgb: [39, 174, 96], textRgb: [33, 33, 33] },
  { key: "moderate", label: "Moderate Risk", bg: "#f1c40f", textColor: "#212121", rgb: [241, 196, 15], textRgb: [33, 33, 33] },
  { key: "high", label: "High Risk", bg: "#e67e22", textColor: "#212121", rgb: [230, 126, 34], textRgb: [33, 33, 33] },
  { key: "extreme", label: "Extreme Risk", bg: "#c0392b", textColor: "#ffffff", rgb: [192, 57, 43], textRgb: [255, 255, 255] },
] as const;

export const RISK_MATRIX_LEGEND =
  "Score = Likelihood (1-5) x Severity (1-5) | Low Risk: 1-3 | Moderate Risk: 4-6 | " +
  "High Risk: 8-12 | Extreme Risk: 15-25 (ADOSH-SF Technical Guideline v4.0, Table 3)";

/** Likelihood x Severity, both expected to already be validated integers 1-5. */
export function score(likelihood: number, severity: number): number {
  return likelihood * severity;
}

/** Bands a raw P x S score per the ADOSH-SF Table 3 thresholds. */
export function band(riskScore: number): RiskBand {
  if (riskScore <= 3) return RISK_BANDS[0];
  if (riskScore <= 6) return RISK_BANDS[1];
  if (riskScore <= 12) return RISK_BANDS[2];
  return RISK_BANDS[3];
}

/** Convenience: band directly from likelihood/severity. */
export function bandFor(likelihood: number, severity: number): RiskBand {
  return band(score(likelihood, severity));
}

// ---------------------------------------------------------------------------
// Confidence gating
//
// A hazard the model is not confident about must never be treated the same
// as one it is confident about. Below this threshold, the record is flagged
// for mandatory human review before it can count toward sign-off.
// ---------------------------------------------------------------------------

export const CONFIDENCE_REVIEW_THRESHOLD = 0.6;

export function needsReview(confidence: number | null | undefined): boolean {
  if (confidence === null || confidence === undefined) return true;
  if (!Number.isFinite(confidence)) return true;
  return confidence < CONFIDENCE_REVIEW_THRESHOLD;
}

// ---------------------------------------------------------------------------
// Hazard input validation
//
// Replaces silent defaulting (e.g. `Math.round(item.likelihood)` producing
// NaN when a field is missing or malformed, or `item.confidence || 0.8`
// silently inventing a confident score). A malformed model response must be
// REJECTED, never coerced into a plausible-looking number.
// ---------------------------------------------------------------------------

export interface RawHazardCandidate {
  name?: unknown;
  key?: unknown;
  likelihood?: unknown;
  severity?: unknown;
  confidence?: unknown;
  details?: unknown;
  eliminate?: unknown;
  substitute?: unknown;
  engineering?: unknown;
  administrative?: unknown;
  ppe?: unknown;
}

export interface ValidatedHazard {
  name: string;
  key: string;
  likelihood: number;
  severity: number;
  score: number;
  band: RiskBand;
  confidence: number;
  needsReview: boolean;
  details: string;
  eliminate: string;
  substitute: string;
  engineering: string;
  administrative: string;
  ppe: string;
}

export interface ValidationRejection {
  reason: string;
  raw: RawHazardCandidate;
}

export interface ValidationResult {
  accepted: ValidatedHazard[];
  rejected: ValidationRejection[];
}

function isInt1to5(v: unknown): v is number {
  return typeof v === "number" && Number.isInteger(v) && v >= 1 && v <= 5;
}

/**
 * Validates a raw array of hazard candidates from a model response.
 * Any candidate with a missing or out-of-range likelihood/severity is
 * REJECTED (not coerced) so a malformed response never silently produces
 * a fabricated risk score. Confidence, if absent or non-numeric, is
 * clamped to 0 (never defaulted upward) which forces `needsReview: true`.
 */
export function validateHazards(raw: unknown): ValidationResult {
  const accepted: ValidatedHazard[] = [];
  const rejected: ValidationRejection[] = [];

  if (!Array.isArray(raw)) {
    return { accepted, rejected: [{ reason: "response was not an array", raw: {} }] };
  }

  for (const item of raw as RawHazardCandidate[]) {
    if (!item || typeof item !== "object") {
      rejected.push({ reason: "candidate is not an object", raw: {} });
      continue;
    }
    if (!isInt1to5(item.likelihood)) {
      rejected.push({ reason: `likelihood missing or out of range (1-5): ${String(item.likelihood)}`, raw: item });
      continue;
    }
    if (!isInt1to5(item.severity)) {
      rejected.push({ reason: `severity missing or out of range (1-5): ${String(item.severity)}`, raw: item });
      continue;
    }
    const rawConfidence = typeof item.confidence === "number" && Number.isFinite(item.confidence)
      ? item.confidence
      : 0; // never default upward — missing confidence means "unknown", not "confident"
    const confidence = Math.min(1, Math.max(0, rawConfidence));

    const likelihood = item.likelihood;
    const severity = item.severity;
    const s = score(likelihood, severity);

    accepted.push({
      name: typeof item.name === "string" && item.name.trim() ? item.name : "Unnamed hazard",
      key: typeof item.key === "string" && item.key.trim() ? item.key : "other",
      likelihood,
      severity,
      score: s,
      band: band(s),
      confidence,
      needsReview: needsReview(confidence),
      details: typeof item.details === "string" ? item.details : "",
      eliminate: typeof item.eliminate === "string" && item.eliminate ? item.eliminate : "N/A",
      substitute: typeof item.substitute === "string" && item.substitute ? item.substitute : "N/A",
      engineering: typeof item.engineering === "string" && item.engineering ? item.engineering : "N/A",
      administrative: typeof item.administrative === "string" && item.administrative ? item.administrative : "N/A",
      ppe: typeof item.ppe === "string" && item.ppe ? item.ppe : "N/A",
    });
  }

  return { accepted, rejected };
}

/** True if any accepted hazard still needs human review before sign-off may proceed. */
export function hasUnreviewedHazards(hazards: readonly { needsReview: boolean; reviewedByHuman?: boolean }[]): boolean {
  return hazards.some((h) => h.needsReview && !h.reviewedByHuman);
}

// ---------------------------------------------------------------------------
// Selectable scoring methodologies
//
// The AI always returns Likelihood/Severity on this app's own 1-5 integer
// scale (see isInt1to5 above) - that never changes, regardless of which
// scoring method is selected for on-screen/PDF display. NEBOSH's and
// Fine-Kinney's native scales are DERIVED from that 1-5 scale via the
// bucket mappings below, never re-collected from the AI or re-entered by
// hand. Those mappings are THIS APP'S OWN BRIDGING CHOICE, not part of
// either published standard - every mapping function says so below, and
// the app surfaces the same note next to the on-screen method selector, so
// nothing invented gets misattributed to NEBOSH or to Kinney & Wiruth.
//
// ADOSH-SF stays the default and is a pure adapter over score()/band()
// above - zero new logic - which is how "switching methods never changes
// ADOSH-SF's own numbers" is guaranteed, not just asserted.
// ---------------------------------------------------------------------------

export type ScoringMethodId = "adosh-sf" | "nebosh-hsg65" | "fine-kinney";

/**
 * Same shape as RiskBand, but not RiskBand itself - Fine-Kinney has five
 * bands (Slight/Possible/Substantial/High/Very High), one more than
 * RiskBandKey's four, so it needs its own key space. Kept structurally
 * identical so UI code can render any method's bands the same way.
 */
export interface MethodBand {
  key: string;
  label: string;
  bg: string;
  textColor: string;
  rgb: readonly [number, number, number];
  /** Same colour as `textColor`, pre-split for jsPDF's setTextColor(r, g, b). */
  textRgb: readonly [number, number, number];
}

export interface ScoringMethod {
  id: ScoringMethodId;
  label: string;
  citation: string;
  legend: string;
  bands: readonly MethodBand[];
  /** True only for Fine-Kinney - the one method with a dimension that isn't derived from the app's existing 1-5 scale. */
  usesExposure: boolean;
  /** Null for ADOSH-SF (nothing bridged - it's the app's native scale). Shown next to the method selector for the other two. */
  bridgingNote: string | null;
  axisLabels: { likelihood: string; severity: string; exposure?: string };
  scoreFromAppScale(likelihood: number, severity: number, exposure: number): number;
  bandForScore(riskScore: number): MethodBand;
}

// --- NEBOSH / HSG65-style 3x3 ------------------------------------------------

/** App's 1-5 Likelihood/Severity compressed to NEBOSH's 1-3 scale: {1,2}->1, {3,4}->2, {5}->3. This bucketing is this app's own choice, not part of the NEBOSH syllabus. */
export function mapAppLikelihoodToNebosh(appLikelihood: number): number {
  if (appLikelihood <= 2) return 1;
  if (appLikelihood <= 4) return 2;
  return 3;
}

/** Same bucketing as mapAppLikelihoodToNebosh, kept as a separate named function per axis for readable call sites. */
export function mapAppSeverityToNebosh(appSeverity: number): number {
  return mapAppLikelihoodToNebosh(appSeverity);
}

export function neboshScore(neboshLikelihood: number, neboshSeverity: number): number {
  return neboshLikelihood * neboshSeverity;
}

/** Reuses the exact ADOSH-SF band objects (same colours/labels) for NEBOSH's Low/Medium/High - NEBOSH has no fourth "Extreme" band. */
export const NEBOSH_BANDS: readonly MethodBand[] = [RISK_BANDS[0], RISK_BANDS[1], RISK_BANDS[2]];

/** Reachable NEBOSH S x L products (L,S each 1-3) are {1,2,3,4,6,9} - 5, 7 and 8 never occur. */
export function neboshBand(neboshScoreValue: number): MethodBand {
  if (neboshScoreValue <= 2) return NEBOSH_BANDS[0];
  if (neboshScoreValue <= 4) return NEBOSH_BANDS[1];
  return NEBOSH_BANDS[2];
}

export const NEBOSH_LEGEND =
  "Score = Likelihood (1-3) x Severity (1-3), derived from this app's 1-5 scale | Low: 1-2 | Medium: 3-4 | High: 6-9 (5, 7, 8 unreachable)";

// --- Fine-Kinney --------------------------------------------------------------
//
// Kinney, G.F. & Wiruth, A.D. (1976), "Practical Risk Analysis for Safety
// Management". Band cutoffs per safetydojo.org/en/risk-assessment/kinney-method.php
// - published cutoffs vary slightly by textbook, this specific source is
// the one cited here. The app-scale-to-Kinney-scale mappings below are
// this app's own bridging choice, not part of the Kinney & Wiruth method.

/** Index 0 = app Likelihood 1 ... index 4 = app Likelihood 5, mapped onto Kinney's native Likelihood scale. */
const KINNEY_LIKELIHOOD_BY_APP_LEVEL: readonly number[] = [0.2, 1, 3, 6, 10];

/** Index 0 = app Severity 1 ... index 4 = app Severity 5. Kinney's 100 ("catastrophe, numerous fatalities") is never reached from this app's 1-5 severity scale, by design - the same "some published values are unreachable" pattern as ADOSH-SF's own bands (7, 11, 13, 14). */
const KINNEY_CONSEQUENCE_BY_APP_LEVEL: readonly number[] = [1, 3, 7, 15, 40];

function clampAppLevel(appLevel: number): number {
  return Math.min(5, Math.max(1, Math.round(appLevel)));
}

export function mapAppLikelihoodToKinney(appLikelihood: number): number {
  return KINNEY_LIKELIHOOD_BY_APP_LEVEL[clampAppLevel(appLikelihood) - 1];
}

export function mapAppSeverityToKinneyConsequence(appSeverity: number): number {
  return KINNEY_CONSEQUENCE_BY_APP_LEVEL[clampAppLevel(appSeverity) - 1];
}

/** Exposure has no 1-5 analog in this app - it's a new field the reviewer sets directly when Fine-Kinney is selected. Values are Kinney's own native exposure scale. */
export const FINE_KINNEY_EXPOSURE_OPTIONS: readonly { value: number; label: string }[] = [
  { value: 0.5, label: "Very rare (a few times a year)" },
  { value: 1, label: "Rare (annually)" },
  { value: 2, label: "Unusual (monthly)" },
  { value: 3, label: "Occasional (weekly)" },
  { value: 6, label: "Frequent (daily)" },
  { value: 10, label: "Continuous" },
];

export const FINE_KINNEY_DEFAULT_EXPOSURE = 3; // "Occasional (weekly)"

export function fineKinneyScore(kinneyLikelihood: number, exposure: number, consequence: number): number {
  return kinneyLikelihood * exposure * consequence;
}

// Same WCAG AA contrast fix as RISK_BANDS above: "slight" and "substantial"
// share ADOSH-SF's green/orange, which fail 4.5:1 against white (2.87:1 and
// 2.85:1) and so get the same dark text. "high" (5.44:1) and "very-high"
// (9.95:1, despite looking like the riskiest colour) both already pass
// against white and are left alone.
export const FINE_KINNEY_BANDS: readonly MethodBand[] = [
  { key: "slight", label: "Slight Risk", bg: "#27ae60", textColor: "#212121", rgb: [39, 174, 96], textRgb: [33, 33, 33] },
  { key: "possible", label: "Possible Risk", bg: "#f1c40f", textColor: "#212121", rgb: [241, 196, 15], textRgb: [33, 33, 33] },
  { key: "substantial", label: "Substantial Risk", bg: "#e67e22", textColor: "#212121", rgb: [230, 126, 34], textRgb: [33, 33, 33] },
  { key: "high", label: "High Risk", bg: "#c0392b", textColor: "#ffffff", rgb: [192, 57, 43], textRgb: [255, 255, 255] },
  { key: "very-high", label: "Very High Risk", bg: "#7b241c", textColor: "#ffffff", rgb: [123, 36, 28], textRgb: [255, 255, 255] },
] as const;

/** Boundaries are lower-bound-inclusive (the published cutoffs are ambiguous at the boundary): <20 Slight, 20-<70 Possible, 70-<160 Substantial, 160-<320 High, >=320 Very High. */
export function fineKinneyBand(kinneyScoreValue: number): MethodBand {
  if (kinneyScoreValue < 20) return FINE_KINNEY_BANDS[0];
  if (kinneyScoreValue < 70) return FINE_KINNEY_BANDS[1];
  if (kinneyScoreValue < 160) return FINE_KINNEY_BANDS[2];
  if (kinneyScoreValue < 320) return FINE_KINNEY_BANDS[3];
  return FINE_KINNEY_BANDS[4];
}

export const FINE_KINNEY_LEGEND =
  "Score = Likelihood x Exposure x Consequence | <20 Slight | 20-<70 Possible | 70-<160 Substantial | 160-<320 High | >=320 Very High (Kinney & Wiruth 1976)";

// --- Registry ------------------------------------------------------------------

export const DEFAULT_SCORING_METHOD_ID: ScoringMethodId = "adosh-sf";

export const SCORING_METHODS: Readonly<Record<ScoringMethodId, ScoringMethod>> = {
  "adosh-sf": {
    id: "adosh-sf",
    label: "ADOSH-SF (5×5, default)",
    citation: 'ADOSH-SF Technical Guideline "Process of Risk Management", v4.0, Table 3',
    legend: RISK_MATRIX_LEGEND,
    bands: RISK_BANDS,
    usesExposure: false,
    bridgingNote: null,
    axisLabels: { likelihood: "Likelihood (1-5)", severity: "Severity (1-5)" },
    scoreFromAppScale: (likelihood, severity) => score(likelihood, severity),
    bandForScore: (riskScore) => band(riskScore),
  },
  "nebosh-hsg65": {
    id: "nebosh-hsg65",
    label: "NEBOSH / HSG65 (3×3)",
    citation: "NEBOSH National General Certificate handbook & NEBOSH IGC1",
    legend: NEBOSH_LEGEND,
    bands: NEBOSH_BANDS,
    usesExposure: false,
    bridgingNote:
      "Likelihood and Severity are compressed from this app's 1-5 scale into NEBOSH's 1-3 scale " +
      "({1,2}→1, {3,4}→2, {5}→3) - this bucketing is this app's own bridging choice, not part of the NEBOSH syllabus.",
    axisLabels: { likelihood: "Likelihood (1-3, derived)", severity: "Severity (1-3, derived)" },
    scoreFromAppScale: (likelihood, severity) =>
      neboshScore(mapAppLikelihoodToNebosh(likelihood), mapAppSeverityToNebosh(severity)),
    bandForScore: (riskScore) => neboshBand(riskScore),
  },
  "fine-kinney": {
    id: "fine-kinney",
    label: "Fine-Kinney (L×E×C)",
    citation: "Kinney, G.F. & Wiruth, A.D. (1976); cutoffs per safetydojo.org/en/risk-assessment/kinney-method.php",
    legend: FINE_KINNEY_LEGEND,
    bands: FINE_KINNEY_BANDS,
    usesExposure: true,
    bridgingNote:
      "Likelihood and Consequence are derived from this app's 1-5 Likelihood/Severity scale via a fixed lookup " +
      "table; Exposure has no 1-5 analog and is entered directly. This mapping is this app's own bridging choice, " +
      "not part of the Kinney & Wiruth method itself.",
    axisLabels: { likelihood: "Likelihood (derived)", severity: "Consequence (derived)", exposure: "Exposure" },
    scoreFromAppScale: (likelihood, severity, exposure) =>
      fineKinneyScore(mapAppLikelihoodToKinney(likelihood), exposure, mapAppSeverityToKinneyConsequence(severity)),
    bandForScore: (riskScore) => fineKinneyBand(riskScore),
  },
};

/** Unknown/undefined ids (e.g. an older saved record, or a future method removed later) fall back to ADOSH-SF rather than throwing. */
export function getScoringMethod(id: string | null | undefined): ScoringMethod {
  if (id && Object.prototype.hasOwnProperty.call(SCORING_METHODS, id)) {
    return SCORING_METHODS[id as ScoringMethodId];
  }
  return SCORING_METHODS[DEFAULT_SCORING_METHOD_ID];
}
