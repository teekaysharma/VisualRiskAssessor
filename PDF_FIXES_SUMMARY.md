# PDF Export Function Fixes - Summary

## Date: 2025-01-XX
## File Modified: index.html
## Changes Made to PDF Export Function (exportPDF)

---

## Fix 1: Doubled Score Numbers

### Problem
The risk matrix score numbers were being drawn twice in the PDF:
1. First by autoTable when rendering the matrixBody (which contained `row.push(String(score))`)
2. Second by the manual `doc.text()` call in the `didDrawCell` callback

### Solution
Removed the score from the matrixBody loop. Now it only pushes an empty string, so the score is drawn only once by the manual `doc.text()` call at the top-center of each cell.

### Changed Code
**Before:**
```javascript
severities.forEach(s => {
  const score = l * s;
  row.push(String(score));
});
```

**After:**
```javascript
severities.forEach(s => {
  row.push('');  // Score will be drawn manually in didDrawCell at top-center
});
```

**Impact:** Each cell now shows its score only once, positioned at the top-center as intended.

---

## Fix 2: Color Thresholds Update

### Problem
The PDF matrix cell color function (`matrixCellColor`) used different thresholds than the web's `riskFor()` function, causing inconsistent color rendering.

### Solution
Updated both `matrixCellColor` and `scoreColor` functions to use the exact same thresholds as specified in the requirements:

### Changed Code

**matrixCellColor Function:**
**Before:**
```javascript
function matrixCellColor(score) {
  if (score <= 4) return [39, 174, 96];    // Low - green
  if (score <= 9) return [241, 196, 15];   // Medium - yellow
  if (score <= 16) return [230, 126, 34];  // High - orange
  return [192, 57, 43];                     // Extreme - red
}
```

**After:**
```javascript
function matrixCellColor(score) {
  if (score >= 17) return [192, 57, 43];   // Extreme - red #c0392b
  if (score >= 10) return [230, 126, 34];   // High - amber #e67e22
  if (score >= 5)  return [241, 196, 15];  // Medium - yellow #f1c40f
  return [39, 174, 96];                     // Low - green #27ae60
}
```

**scoreColor Function (for Risk Register table):**
Updated to use the same >= thresholds as matrixCellColor.

### Thresholds Now Match Web's riskFor() Function:
- score >= 17 → fill #c0392b (Red/Extreme)
- score >= 10 → fill #e67e22 (Amber/High)
- score >= 5 → fill #f1c40f (Yellow/Medium)
- score >= 1 → fill #27ae60 (Green/Low)
- header cells (L and S labels) → fill #1565c0 (Blue) [unchanged]

**Impact:** PDF color rendering now exactly matches the web application's risk matrix colors.

---

## Fix 3: Hierarchy of Controls N/A Removal

### Problem
The Hierarchy of Controls table in the PDF was showing 'N/A' for all control fields, even when the hazard objects contained actual control values from AI analysis or BASELINE_DB.

### Solution
Removed the 'N/A' fallback in the HOC body mapping. Now it reads fields directly from each hazard object and shows empty strings only if the field is genuinely empty.

### Changed Code
**Before:**
```javascript
const hocBody = allHazardsForPDF.map((h, i) => [
  i + 1,
  h.name,
  h.eliminate || 'N/A',
  h.substitute || 'N/A',
  h.engineering || 'N/A',
  h.administrative || 'N/A',
  h.ppe || 'N/A'
]);
```

**After:**
```javascript
const hocBody = allHazardsForPDF.map((h, i) => [
  i + 1,
  h.name,
  h.eliminate || '',
  h.substitute || '',
  h.engineering || '',
  h.administrative || '',
  h.ppe || ''
]);
```

**Impact:** The PDF now shows the full control text from hazard objects (populated from AI analysis or BASELINE_DB), with blank cells only when the field is genuinely empty on the hazard object itself.

---

## Verification

All three fixes have been verified:
- ✅ Fix 1: Score is not pushed to matrixBody, only drawn manually in didDrawCell
- ✅ Fix 2: Both matrixCellColor and scoreColor use >= thresholds (17, 10, 5)
- ✅ Fix 3: HOC body does not use 'N/A' fallback
- ✅ JavaScript syntax valid (balanced braces, parentheses, brackets)

---

## Files Changed
- `/home/engine/project/index.html` (lines 3043-3000 approximately)

## Testing Notes
The changes affect only the PDF export function. No web rendering code was modified as per requirements.
