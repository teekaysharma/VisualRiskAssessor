import { describe, it, expect } from "vitest";
import {
  score,
  band,
  bandFor,
  needsReview,
  validateHazards,
  hasUnreviewedHazards,
  CONFIDENCE_REVIEW_THRESHOLD,
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
