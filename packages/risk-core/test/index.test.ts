import { describe, it, expect } from "vitest";
import {
  score,
  band,
  bandFor,
  needsReview,
  validateHazards,
  hasUnreviewedHazards,
  CONFIDENCE_REVIEW_THRESHOLD,
  SCORING_METHODS,
  DEFAULT_SCORING_METHOD_ID,
  getScoringMethod,
  mapAppLikelihoodToNebosh,
  mapAppSeverityToNebosh,
  neboshScore,
  neboshBand,
  mapAppLikelihoodToKinney,
  mapAppSeverityToKinneyConsequence,
  fineKinneyScore,
  fineKinneyBand,
  FINE_KINNEY_DEFAULT_EXPOSURE,
} from "../src/index";

describe("score", () => {
  it("multiplies likelihood by severity", () => {
    expect(score(3, 4)).toBe(12);
    expect(score(1, 1)).toBe(1);
    expect(score(5, 5)).toBe(25);
  });
});

describe("band boundaries (ADOSH-SF Table 3)", () => {
  it("bands the low range 1-3", () => {
    expect(band(1).key).toBe("low");
    expect(band(2).key).toBe("low");
    expect(band(3).key).toBe("low");
  });

  it("bands the moderate range 4-6", () => {
    expect(band(4).key).toBe("moderate");
    expect(band(5).key).toBe("moderate");
    expect(band(6).key).toBe("moderate");
  });

  it("bands the high range 8-12 (7 is an unreachable gap, still classified high-side)", () => {
    expect(band(8).key).toBe("high");
    expect(band(9).key).toBe("high");
    expect(band(10).key).toBe("high");
    expect(band(12).key).toBe("high");
  });

  it("bands the extreme range 15-25", () => {
    expect(band(15).key).toBe("extreme");
    expect(band(16).key).toBe("extreme");
    expect(band(20).key).toBe("extreme");
    expect(band(25).key).toBe("extreme");
  });

  it("only reachable P*S products (1-5 x 1-5) land correctly", () => {
    const reachable = [1, 2, 3, 4, 5, 6, 8, 9, 10, 12, 15, 16, 20, 25];
    const expected = [
      "low", "low", "low", "moderate", "moderate", "moderate",
      "high", "high", "high", "high",
      "extreme", "extreme", "extreme", "extreme",
    ];
    reachable.forEach((s, i) => expect(band(s).key).toBe(expected[i]));
  });

  it("bandFor matches band(score(l,s)) for every 1-5 x 1-5 pair", () => {
    for (let l = 1; l <= 5; l++) {
      for (let s = 1; s <= 5; s++) {
        expect(bandFor(l, s).key).toBe(band(score(l, s)).key);
      }
    }
  });
});

describe("needsReview / confidence gating", () => {
  it("flags missing confidence as needing review", () => {
    expect(needsReview(undefined)).toBe(true);
    expect(needsReview(null)).toBe(true);
  });

  it("flags non-finite confidence as needing review", () => {
    expect(needsReview(NaN)).toBe(true);
  });

  it("flags confidence strictly below the threshold", () => {
    expect(needsReview(CONFIDENCE_REVIEW_THRESHOLD - 0.01)).toBe(true);
  });

  it("does not flag confidence at or above the threshold", () => {
    expect(needsReview(CONFIDENCE_REVIEW_THRESHOLD)).toBe(false);
    expect(needsReview(0.95)).toBe(false);
  });
});

describe("validateHazards — rejects malformed input instead of coercing it", () => {
  it("rejects a candidate missing likelihood, rather than defaulting it to a plausible number", () => {
    const { accepted, rejected } = validateHazards([
      { name: "Slip", key: "slip", severity: 4, confidence: 0.9 },
    ]);
    expect(accepted).toHaveLength(0);
    expect(rejected).toHaveLength(1);
    expect(rejected[0].reason).toMatch(/likelihood/i);
  });

  it("rejects a candidate with an out-of-range severity (e.g. model hallucinated 7)", () => {
    const { accepted, rejected } = validateHazards([
      { name: "Fall", key: "height", likelihood: 3, severity: 7, confidence: 0.9 },
    ]);
    expect(accepted).toHaveLength(0);
    expect(rejected).toHaveLength(1);
  });

  it("rejects non-integer likelihood/severity rather than rounding it silently", () => {
    const { accepted, rejected } = validateHazards([
      { name: "Fire", key: "fire", likelihood: 3.5, severity: 4, confidence: 0.9 },
    ]);
    expect(accepted).toHaveLength(0);
    expect(rejected).toHaveLength(1);
  });

  it("never returns a NaN score for any accepted hazard", () => {
    const { accepted } = validateHazards([
      { name: "Electrical", key: "electrical", likelihood: 4, severity: 5 }, // confidence omitted
    ]);
    expect(accepted).toHaveLength(1);
    expect(Number.isNaN(accepted[0].score)).toBe(false);
    expect(accepted[0].score).toBe(20);
  });

  it("treats a missing confidence as 0 (unknown), never defaults it upward, and flags for review", () => {
    const { accepted } = validateHazards([
      { name: "Chemical", key: "chemical", likelihood: 2, severity: 3 },
    ]);
    expect(accepted[0].confidence).toBe(0);
    expect(accepted[0].needsReview).toBe(true);
  });

  it("accepts a well-formed high-confidence candidate and does not flag it for review", () => {
    const { accepted, rejected } = validateHazards([
      { name: "Machinery", key: "machinery", likelihood: 3, severity: 3, confidence: 0.85 },
    ]);
    expect(rejected).toHaveLength(0);
    expect(accepted[0].needsReview).toBe(false);
    expect(accepted[0].band.key).toBe("high"); // 3x3=9 falls in the 8-12 High band
  });

  it("handles a non-array response (e.g. API returned an error object) without throwing", () => {
    const { accepted, rejected } = validateHazards({ error: "rate limited" });
    expect(accepted).toHaveLength(0);
    expect(rejected).toHaveLength(1);
  });
});

describe("hasUnreviewedHazards", () => {
  it("is true when a needs-review hazard has not been human-reviewed", () => {
    expect(hasUnreviewedHazards([{ needsReview: true }])).toBe(true);
  });

  it("is false once the human has reviewed it", () => {
    expect(hasUnreviewedHazards([{ needsReview: true, reviewedByHuman: true }])).toBe(false);
  });

  it("is false when nothing needs review", () => {
    expect(hasUnreviewedHazards([{ needsReview: false }])).toBe(false);
  });
});

describe("selectable scoring methodology — ADOSH-SF adapter never diverges from score()/band()", () => {
  it("is the default method", () => {
    expect(DEFAULT_SCORING_METHOD_ID).toBe("adosh-sf");
    expect(getScoringMethod(undefined).id).toBe("adosh-sf");
  });

  it("scoreFromAppScale/bandForScore match score()/band() exactly across every 1-5 x 1-5 pair — the regression guard for 'switching methods never changes ADOSH-SF's own numbers'", () => {
    const adosh = SCORING_METHODS["adosh-sf"];
    for (let l = 1; l <= 5; l++) {
      for (let s = 1; s <= 5; s++) {
        const expectedScore = score(l, s);
        expect(adosh.scoreFromAppScale(l, s, FINE_KINNEY_DEFAULT_EXPOSURE)).toBe(expectedScore);
        expect(adosh.bandForScore(expectedScore).key).toBe(band(expectedScore).key);
      }
    }
  });

  it("has no bridging note (nothing bridged - it's the app's native scale)", () => {
    expect(SCORING_METHODS["adosh-sf"].bridgingNote).toBeNull();
  });
});

describe("getScoringMethod — falls back to ADOSH-SF for unknown/missing ids", () => {
  it("falls back for an unrecognised id", () => {
    expect(getScoringMethod("some-future-method").id).toBe("adosh-sf");
  });

  it("falls back for null/undefined", () => {
    expect(getScoringMethod(null).id).toBe("adosh-sf");
    expect(getScoringMethod(undefined).id).toBe("adosh-sf");
  });

  it("returns the requested method when it exists", () => {
    expect(getScoringMethod("fine-kinney").id).toBe("fine-kinney");
    expect(getScoringMethod("nebosh-hsg65").id).toBe("nebosh-hsg65");
  });
});

describe("NEBOSH/HSG65 bridging", () => {
  it("compresses the app's 1-5 scale into NEBOSH's 1-3 scale as {1,2}->1, {3,4}->2, {5}->3", () => {
    expect(mapAppLikelihoodToNebosh(1)).toBe(1);
    expect(mapAppLikelihoodToNebosh(2)).toBe(1);
    expect(mapAppLikelihoodToNebosh(3)).toBe(2);
    expect(mapAppLikelihoodToNebosh(4)).toBe(2);
    expect(mapAppLikelihoodToNebosh(5)).toBe(3);
    // Severity uses the identical bucketing.
    for (let i = 1; i <= 5; i++) {
      expect(mapAppSeverityToNebosh(i)).toBe(mapAppLikelihoodToNebosh(i));
    }
  });

  it("only produces the reachable {1,2,3,4,6,9} scores across every derived 1-3 x 1-3 pair — 5, 7, 8 never occur", () => {
    const seen = new Set<number>();
    for (let l = 1; l <= 5; l++) {
      for (let s = 1; s <= 5; s++) {
        seen.add(neboshScore(mapAppLikelihoodToNebosh(l), mapAppSeverityToNebosh(s)));
      }
    }
    expect([...seen].sort((a, b) => a - b)).toEqual([1, 2, 3, 4, 6, 9]);
  });

  it("bands 1-2 Low, 3-4 Medium, 6-9 High", () => {
    expect(neboshBand(1).key).toBe("low");
    expect(neboshBand(2).key).toBe("low");
    expect(neboshBand(3).key).toBe("moderate");
    expect(neboshBand(4).key).toBe("moderate");
    expect(neboshBand(6).key).toBe("high");
    expect(neboshBand(9).key).toBe("high");
  });

  it("the registry entry matches the standalone functions", () => {
    const nebosh = SCORING_METHODS["nebosh-hsg65"];
    expect(nebosh.scoreFromAppScale(4, 5, FINE_KINNEY_DEFAULT_EXPOSURE)).toBe(
      neboshScore(mapAppLikelihoodToNebosh(4), mapAppSeverityToNebosh(5))
    );
  });
});

describe("Fine-Kinney bridging", () => {
  it("maps app Likelihood 1-5 to Kinney's native scale monotonically", () => {
    const mapped = [1, 2, 3, 4, 5].map(mapAppLikelihoodToKinney);
    expect(mapped).toEqual([0.2, 1, 3, 6, 10]);
  });

  it("maps app Severity 1-5 to Kinney's native Consequence scale monotonically, never reaching Kinney's own 100 (by design)", () => {
    const mapped = [1, 2, 3, 4, 5].map(mapAppSeverityToKinneyConsequence);
    expect(mapped).toEqual([1, 3, 7, 15, 40]);
    expect(mapped).not.toContain(100);
  });

  it("clamps out-of-range/non-integer app levels rather than throwing or indexing out of bounds", () => {
    expect(mapAppLikelihoodToKinney(0)).toBe(mapAppLikelihoodToKinney(1));
    expect(mapAppLikelihoodToKinney(6)).toBe(mapAppLikelihoodToKinney(5));
    expect(mapAppLikelihoodToKinney(3.4)).toBe(mapAppLikelihoodToKinney(3));
  });

  it("scores as Likelihood x Exposure x Consequence", () => {
    expect(fineKinneyScore(3, 6, 15)).toBe(270);
  });

  it("bands lower-bound-inclusive: <20 Slight, 20-<70 Possible, 70-<160 Substantial, 160-<320 High, >=320 Very High", () => {
    expect(fineKinneyBand(19.9).key).toBe("slight");
    expect(fineKinneyBand(20).key).toBe("possible");
    expect(fineKinneyBand(69.9).key).toBe("possible");
    expect(fineKinneyBand(70).key).toBe("substantial");
    expect(fineKinneyBand(159.9).key).toBe("substantial");
    expect(fineKinneyBand(160).key).toBe("high");
    expect(fineKinneyBand(319.9).key).toBe("high");
    expect(fineKinneyBand(320).key).toBe("very-high");
  });

  it("an app-max hazard (L5/S5) at the default exposure lands in Very High, and an app-min hazard (L1/S1) lands in Slight", () => {
    const fineKinney = SCORING_METHODS["fine-kinney"];
    const maxScore = fineKinney.scoreFromAppScale(5, 5, FINE_KINNEY_DEFAULT_EXPOSURE);
    const minScore = fineKinney.scoreFromAppScale(1, 1, FINE_KINNEY_DEFAULT_EXPOSURE);
    expect(fineKinney.bandForScore(maxScore).key).toBe("very-high");
    expect(fineKinney.bandForScore(minScore).key).toBe("slight");
  });

  it("declares usesExposure and a non-null bridging note, unlike the other two methods", () => {
    expect(SCORING_METHODS["fine-kinney"].usesExposure).toBe(true);
    expect(SCORING_METHODS["fine-kinney"].bridgingNote).not.toBeNull();
    expect(SCORING_METHODS["adosh-sf"].usesExposure).toBe(false);
    expect(SCORING_METHODS["nebosh-hsg65"].usesExposure).toBe(false);
  });
});
