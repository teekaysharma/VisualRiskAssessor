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
  /** Hex text colour for use against `bg`. */
  textColor: string;
  /** Same colour as `bg`, pre-split for jsPDF's setFillColor(r, g, b). */
  rgb: readonly [number, number, number];
}

export const RISK_BANDS: readonly RiskBand[] = [
  { key: "low", label: "Low Risk", bg: "#27ae60", textColor: "#ffffff", rgb: [39, 174, 96] },
  { key: "moderate", label: "Moderate Risk", bg: "#f1c40f", textColor: "#212121", rgb: [241, 196, 15] },
  { key: "high", label: "High Risk", bg: "#e67e22", textColor: "#ffffff", rgb: [230, 126, 34] },
  { key: "extreme", label: "Extreme Risk", bg: "#c0392b", textColor: "#ffffff", rgb: [192, 57, 43] },
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
