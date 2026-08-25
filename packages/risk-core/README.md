# @riskapp/risk-core

The single, authoritative implementation of risk scoring and banding for
VisualRiskAssessor. Every client (the web app, and any future PWA/native
client) must call into this module rather than reimplementing the matrix
inline — that duplication (8+ copies of the banding thresholds inside
`index.html` alone, and disagreeing bands across the web/Kotlin/Expo
codebases) was the root cause of a long series of single-bug PRs.

Source of the matrix: ADOSH-SF Technical Guideline "Process of Risk
Management", Version 4.0, Table 3 — Risk Rating.

## Usage in the browser

This package is TypeScript, tested with vitest, and built to a single
IIFE bundle (`dist/risk-core.js`) that exposes `window.RiskCore`. The
built file is committed at the repo root as `risk-core.js` and loaded via
a plain `<script>` tag in `index.html`, since the app has no build step.

**To change the matrix or scoring logic:**

```
cd packages/risk-core
npm install
npm test              # 21 tests pin every band boundary
npm run build          # writes dist/risk-core.js
cp dist/risk-core.js ../../risk-core.js
```

Do not edit thresholds directly in `index.html`.
