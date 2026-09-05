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
    DEFAULT_SCORING_METHOD_ID: () => DEFAULT_SCORING_METHOD_ID,
    FINE_KINNEY_BANDS: () => FINE_KINNEY_BANDS,
    FINE_KINNEY_DEFAULT_EXPOSURE: () => FINE_KINNEY_DEFAULT_EXPOSURE,
    FINE_KINNEY_EXPOSURE_OPTIONS: () => FINE_KINNEY_EXPOSURE_OPTIONS,
    FINE_KINNEY_LEGEND: () => FINE_KINNEY_LEGEND,
    NEBOSH_BANDS: () => NEBOSH_BANDS,
    NEBOSH_LEGEND: () => NEBOSH_LEGEND,
    RISK_BANDS: () => RISK_BANDS,
    RISK_MATRIX_LEGEND: () => RISK_MATRIX_LEGEND,
    SCORING_METHODS: () => SCORING_METHODS,
    band: () => band,
    bandFor: () => bandFor,
    fineKinneyBand: () => fineKinneyBand,
    fineKinneyScore: () => fineKinneyScore,
    getScoringMethod: () => getScoringMethod,
    hasUnreviewedHazards: () => hasUnreviewedHazards,
    mapAppLikelihoodToKinney: () => mapAppLikelihoodToKinney,
    mapAppLikelihoodToNebosh: () => mapAppLikelihoodToNebosh,
    mapAppSeverityToKinneyConsequence: () => mapAppSeverityToKinneyConsequence,
    mapAppSeverityToNebosh: () => mapAppSeverityToNebosh,
    neboshBand: () => neboshBand,
    neboshScore: () => neboshScore,
    needsReview: () => needsReview,
    score: () => score,
    validateHazards: () => validateHazards
  });
  var RISK_BANDS = [
    { key: "low", label: "Low Risk", bg: "#27ae60", textColor: "#212121", rgb: [39, 174, 96], textRgb: [33, 33, 33] },
    { key: "moderate", label: "Moderate Risk", bg: "#f1c40f", textColor: "#212121", rgb: [241, 196, 15], textRgb: [33, 33, 33] },
    { key: "high", label: "High Risk", bg: "#e67e22", textColor: "#212121", rgb: [230, 126, 34], textRgb: [33, 33, 33] },
    { key: "extreme", label: "Extreme Risk", bg: "#c0392b", textColor: "#ffffff", rgb: [192, 57, 43], textRgb: [255, 255, 255] }
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
  function mapAppLikelihoodToNebosh(appLikelihood) {
    if (appLikelihood <= 2) return 1;
    if (appLikelihood <= 4) return 2;
    return 3;
  }
  function mapAppSeverityToNebosh(appSeverity) {
    return mapAppLikelihoodToNebosh(appSeverity);
  }
  function neboshScore(neboshLikelihood, neboshSeverity) {
    return neboshLikelihood * neboshSeverity;
  }
  var NEBOSH_BANDS = [RISK_BANDS[0], RISK_BANDS[1], RISK_BANDS[2]];
  function neboshBand(neboshScoreValue) {
    if (neboshScoreValue <= 2) return NEBOSH_BANDS[0];
    if (neboshScoreValue <= 4) return NEBOSH_BANDS[1];
    return NEBOSH_BANDS[2];
  }
  var NEBOSH_LEGEND = "Score = Likelihood (1-3) x Severity (1-3), derived from this app's 1-5 scale | Low: 1-2 | Medium: 3-4 | High: 6-9 (5, 7, 8 unreachable)";
  var KINNEY_LIKELIHOOD_BY_APP_LEVEL = [0.2, 1, 3, 6, 10];
  var KINNEY_CONSEQUENCE_BY_APP_LEVEL = [1, 3, 7, 15, 40];
  function clampAppLevel(appLevel) {
    return Math.min(5, Math.max(1, Math.round(appLevel)));
  }
  function mapAppLikelihoodToKinney(appLikelihood) {
    return KINNEY_LIKELIHOOD_BY_APP_LEVEL[clampAppLevel(appLikelihood) - 1];
  }
  function mapAppSeverityToKinneyConsequence(appSeverity) {
    return KINNEY_CONSEQUENCE_BY_APP_LEVEL[clampAppLevel(appSeverity) - 1];
  }
  var FINE_KINNEY_EXPOSURE_OPTIONS = [
    { value: 0.5, label: "Very rare (a few times a year)" },
    { value: 1, label: "Rare (annually)" },
    { value: 2, label: "Unusual (monthly)" },
    { value: 3, label: "Occasional (weekly)" },
    { value: 6, label: "Frequent (daily)" },
    { value: 10, label: "Continuous" }
  ];
  var FINE_KINNEY_DEFAULT_EXPOSURE = 3;
  function fineKinneyScore(kinneyLikelihood, exposure, consequence) {
    return kinneyLikelihood * exposure * consequence;
  }
  var FINE_KINNEY_BANDS = [
    { key: "slight", label: "Slight Risk", bg: "#27ae60", textColor: "#212121", rgb: [39, 174, 96], textRgb: [33, 33, 33] },
    { key: "possible", label: "Possible Risk", bg: "#f1c40f", textColor: "#212121", rgb: [241, 196, 15], textRgb: [33, 33, 33] },
    { key: "substantial", label: "Substantial Risk", bg: "#e67e22", textColor: "#212121", rgb: [230, 126, 34], textRgb: [33, 33, 33] },
    { key: "high", label: "High Risk", bg: "#c0392b", textColor: "#ffffff", rgb: [192, 57, 43], textRgb: [255, 255, 255] },
    { key: "very-high", label: "Very High Risk", bg: "#7b241c", textColor: "#ffffff", rgb: [123, 36, 28], textRgb: [255, 255, 255] }
  ];
  function fineKinneyBand(kinneyScoreValue) {
    if (kinneyScoreValue < 20) return FINE_KINNEY_BANDS[0];
    if (kinneyScoreValue < 70) return FINE_KINNEY_BANDS[1];
    if (kinneyScoreValue < 160) return FINE_KINNEY_BANDS[2];
    if (kinneyScoreValue < 320) return FINE_KINNEY_BANDS[3];
    return FINE_KINNEY_BANDS[4];
  }
  var FINE_KINNEY_LEGEND = "Score = Likelihood x Exposure x Consequence | <20 Slight | 20-<70 Possible | 70-<160 Substantial | 160-<320 High | >=320 Very High (Kinney & Wiruth 1976)";
  var DEFAULT_SCORING_METHOD_ID = "adosh-sf";
  var SCORING_METHODS = {
    "adosh-sf": {
      id: "adosh-sf",
      label: "ADOSH-SF (5\xD75, default)",
      citation: 'ADOSH-SF Technical Guideline "Process of Risk Management", v4.0, Table 3',
      legend: RISK_MATRIX_LEGEND,
      bands: RISK_BANDS,
      usesExposure: false,
      bridgingNote: null,
      axisLabels: { likelihood: "Likelihood (1-5)", severity: "Severity (1-5)" },
      scoreFromAppScale: (likelihood, severity) => score(likelihood, severity),
      bandForScore: (riskScore) => band(riskScore)
    },
    "nebosh-hsg65": {
      id: "nebosh-hsg65",
      label: "NEBOSH / HSG65 (3\xD73)",
      citation: "NEBOSH National General Certificate handbook & NEBOSH IGC1",
      legend: NEBOSH_LEGEND,
      bands: NEBOSH_BANDS,
      usesExposure: false,
      bridgingNote: "Likelihood and Severity are compressed from this app's 1-5 scale into NEBOSH's 1-3 scale ({1,2}\u21921, {3,4}\u21922, {5}\u21923) - this bucketing is this app's own bridging choice, not part of the NEBOSH syllabus.",
      axisLabels: { likelihood: "Likelihood (1-3, derived)", severity: "Severity (1-3, derived)" },
      scoreFromAppScale: (likelihood, severity) => neboshScore(mapAppLikelihoodToNebosh(likelihood), mapAppSeverityToNebosh(severity)),
      bandForScore: (riskScore) => neboshBand(riskScore)
    },
    "fine-kinney": {
      id: "fine-kinney",
      label: "Fine-Kinney (L\xD7E\xD7C)",
      citation: "Kinney, G.F. & Wiruth, A.D. (1976); cutoffs per safetydojo.org/en/risk-assessment/kinney-method.php",
      legend: FINE_KINNEY_LEGEND,
      bands: FINE_KINNEY_BANDS,
      usesExposure: true,
      bridgingNote: "Likelihood and Consequence are derived from this app's 1-5 Likelihood/Severity scale via a fixed lookup table; Exposure has no 1-5 analog and is entered directly. This mapping is this app's own bridging choice, not part of the Kinney & Wiruth method itself.",
      axisLabels: { likelihood: "Likelihood (derived)", severity: "Consequence (derived)", exposure: "Exposure" },
      scoreFromAppScale: (likelihood, severity, exposure) => fineKinneyScore(mapAppLikelihoodToKinney(likelihood), exposure, mapAppSeverityToKinneyConsequence(severity)),
      bandForScore: (riskScore) => fineKinneyBand(riskScore)
    }
  };
  function getScoringMethod(id) {
    if (id && Object.prototype.hasOwnProperty.call(SCORING_METHODS, id)) {
      return SCORING_METHODS[id];
    }
    return SCORING_METHODS[DEFAULT_SCORING_METHOD_ID];
  }
  return __toCommonJS(index_exports);
})();
