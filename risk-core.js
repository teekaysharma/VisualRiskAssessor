"use strict";
var RiskCore = (() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

  // src/index.ts
  var index_exports = {};
  __export(index_exports, {
    CONFIDENCE_REVIEW_THRESHOLD: () => CONFIDENCE_REVIEW_THRESHOLD,
    RISK_BANDS: () => RISK_BANDS,
    RISK_MATRIX_LEGEND: () => RISK_MATRIX_LEGEND,
    band: () => band,
    bandFor: () => bandFor,
    hasUnreviewedHazards: () => hasUnreviewedHazards,
    needsReview: () => needsReview,
    score: () => score,
    validateHazards: () => validateHazards
  });
  var RISK_BANDS = [
    { key: "low", label: "Low Risk", bg: "#27ae60", textColor: "#ffffff", rgb: [39, 174, 96] },
    { key: "moderate", label: "Moderate Risk", bg: "#f1c40f", textColor: "#212121", rgb: [241, 196, 15] },
    { key: "high", label: "High Risk", bg: "#e67e22", textColor: "#ffffff", rgb: [230, 126, 34] },
    { key: "extreme", label: "Extreme Risk", bg: "#c0392b", textColor: "#ffffff", rgb: [192, 57, 43] }
  ];
  var RISK_MATRIX_LEGEND = "Score = Likelihood (1-5) x Severity (1-5) | Low Risk: 1-3 | Moderate Risk: 4-6 | High Risk: 8-12 | Extreme Risk: 15-25 (ADOSH-SF Technical Guideline v4.0, Table 3)";
  function score(likelihood, severity) {
    return likelihood * severity;
  }
  function band(riskScore) {
    if (riskScore <= 3) return RISK_BANDS[0];
    if (riskScore <= 6) return RISK_BANDS[1];
    if (riskScore <= 12) return RISK_BANDS[2];
    return RISK_BANDS[3];
  }
  function bandFor(likelihood, severity) {
    return band(score(likelihood, severity));
  }
  var CONFIDENCE_REVIEW_THRESHOLD = 0.6;
  function needsReview(confidence) {
    if (confidence === null || confidence === void 0) return true;
    if (!Number.isFinite(confidence)) return true;
    return confidence < CONFIDENCE_REVIEW_THRESHOLD;
  }
  function isInt1to5(v) {
    return typeof v === "number" && Number.isInteger(v) && v >= 1 && v <= 5;
  }
  function validateHazards(raw) {
    const accepted = [];
    const rejected = [];
    if (!Array.isArray(raw)) {
      return { accepted, rejected: [{ reason: "response was not an array", raw: {} }] };
    }
    for (const item of raw) {
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
      const rawConfidence = typeof item.confidence === "number" && Number.isFinite(item.confidence) ? item.confidence : 0;
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
        ppe: typeof item.ppe === "string" && item.ppe ? item.ppe : "N/A"
      });
    }
    return { accepted, rejected };
  }
  function hasUnreviewedHazards(hazards) {
    return hazards.some((h) => h.needsReview && !h.reviewedByHuman);
  }
  return __toCommonJS(index_exports);
})();
