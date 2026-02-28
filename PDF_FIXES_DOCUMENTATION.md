# PDF Export Fixes - Implementation Status

## Summary
This document describes the three fixes needed for the PDF export function in index.html. Due to technical limitations with the available tools (bash terminal not accepting input, EditFile tool failing), the fixes have been prepared but could not be automatically applied.

## Required Fixes

### Fix 1: Remove Doubled Score Numbers in PDF Matrix
**Location:** Lines 3090-3101 in index.html (didDrawCell function)

**Issue:** The score numbers in the PDF risk matrix are being drawn twice:
1. Once by the autoTable body (lines 3041-3048 in matrixBody)
2. Once by the didDrawCell callback (lines 3090-3101)

**Solution:** Remove the score drawing code from didDrawCell (lines 3090-3101) since the score is already in the matrixBody that autoTable renders.

**Code to remove:**
```javascript
// Draw score number at top-center of cell (always visible)
const score = l * s;
doc.setFontSize(9);
doc.setFont('helvetica', 'bold');
// Use dark text on yellow, white on others
if (score > 4 && score <= 9) {
  doc.setTextColor(33, 33, 33);
} else {
  doc.setTextColor(255, 255, 255);
}
const scoreY = cellY + 6; // Top of cell with small padding
doc.text(String(score), cellX + cellW / 2, scoreY, { align: 'center' });
```

**Replace with:**
```javascript
// Score text already drawn by autoTable from matrixBody
```

---

### Fix 2: Update Color Thresholds to Match Web Function
**Location:** Lines 3051-3056 in index.html (matrixCellColor function)

**Issue:** The PDF matrix cell color function uses different thresholds than the web matrix color function riskFor().

**Current (incorrect) matrixCellColor function:**
```javascript
function matrixCellColor(score) {
  if (score <= 4) return [39, 174, 96];    // Low - green
  if (score <= 9) return [241, 196, 15];   // Medium - yellow
  if (score <= 16) return [230, 126, 34];  // High - orange
  return [192, 57, 43];                     // Extreme - red
}
```

**Correct riskFor() function (from web rendering, lines 1020-1026):**
```javascript
function riskFor(score) {
  if (score <= 4) return { label: 'Low Risk', cls: 'risk-low', color: 'var(--ok)', bg: '#27ae60', textColor: '#fff' };
  if (score <= 9) return { label: 'Medium Risk', cls: 'risk-medium', color: 'var(--mid)', bg: '#f1c40f', textColor: '#212121' };
  if (score <= 16) return { label: 'High Risk', cls: 'risk-high', color: 'var(--hi)', bg: '#e67e22', textColor: '#fff' };
  if (score <= 25) return { label: 'Critical Risk', cls: 'risk-extreme', color: 'var(--x)', bg: '#c0392b', textColor: '#fff' };
  return { label: 'Extreme Risk', cls: 'risk-extreme', color: 'var(--x)', bg: '#c0392b', textColor: '#fff' };
}
```

**Solution:** Replace matrixCellColor with exact thresholds as specified:

**New matrixCellColor function:**
```javascript
function matrixCellColor(score) {
  if (score >= 17) return [192, 57, 43];    // Red
  if (score >= 10) return [230, 126, 34];  // Amber
  if (score >= 5) return [241, 196, 15];   // Yellow
  if (score >= 1) return [39, 174, 96];    // Green
  return [21, 101, 192];                    // Blue (for headers)
}
```

**Also update text color logic at line 3076:**
**Current:**
```javascript
data.cell.styles.textColor = (score > 4 && score <= 9) ? [33, 33, 33] : [255, 255, 255];
```

**New:**
```javascript
data.cell.styles.textColor = (score >= 5 && score <= 9) ? [33, 33, 33] : [255, 255, 255];
```

---

### Fix 3: Hierarchy of Controls N/A Issue
**Location:** Lines 2993-3001 in index.html (hocBody mapping)

**Issue:** The HOC table shows 'N/A' for fields that contain actual text, because the code uses the `||` operator which treats any falsy value (including empty strings) as needing 'N/A'. It should only show 'N/A' for null or undefined.

**Current (incorrect) code:**
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

**Solution:** Use the nullish coalescing operator `??` instead of `||` to only show 'N/A' for truly null/undefined values, preserving empty strings and actual text content.

**New code:**
```javascript
const hocBody = allHazardsForPDF.map((h, i) => [
  i + 1,
  h.name,
  h.eliminate ?? 'N/A',
  h.substitute ?? 'N/A',
  h.engineering ?? 'N/A',
  h.administrative ?? 'N/A',
  h.ppe ?? 'N/A'
]);
```

---

## Implementation Notes

### Prepared Scripts
The following scripts have been created but could not be executed due to terminal limitations:

1. `/home/engine/project/apply_pdf_fixes.py` - Python script to apply all three fixes
2. `/home/engine/project/run_pdf_fixes.sh` - Shell script to execute the Python script
3. `/tmp/fix_pdf.py` - Alternative Python script

### Execution Instructions
To apply these fixes manually:

1. Open `/home/engine/project/index.html` in a text editor
2. Apply Fix 1: Replace lines 3090-3101 with `// Score text already drawn by autoTable from matrixBody`
3. Apply Fix 2: Replace lines 3051-3056 with the new matrixCellColor function, and update line 3076
4. Apply Fix 3: Replace lines 2996-3000, changing `||` to `??`

### Testing
After applying the fixes:
1. Open the application in a browser
2. Perform a risk assessment
3. Export the PDF
4. Verify:
   - Score numbers in the matrix appear only once (at top-center)
   - Matrix colors match the web matrix exactly (Red for 17-25, Amber for 10-16, Yellow for 5-9, Green for 1-4)
   - HOC table shows full text for hazard fields instead of 'N/A'

---

## Technical Context

### File Details
- **File:** `/home/engine/project/index.html`
- **Function:** `exportPDF()` starting at line 2766
- **Total lines:** ~3600+

### Related Functions
- **riskFor():** Web matrix color function (lines 1020-1026)
- **matrixCellColor():** PDF matrix color function (lines 3051-3056) - needs updating
- **allHazardsForPDF:** Combined array of baseline, AI, and manual hazards

### Color Mapping
| Score Range | Web Color | Web Hex | Current PDF | PDF RGB | Required PDF RGB | Hex |
|------------|------------|----------|-------------|----------|-----------------|-----|
| 17-25 | Extreme/Red | #c0392b | Red | [192, 57, 43] | [192, 57, 43] | #c0392b |
| 10-16 | High/Amber | #e67e22 | Orange | [230, 126, 34] | [230, 126, 34] | #e67e22 |
| 5-9 | Medium/Yellow | #f1c40f | Yellow | [241, 196, 15] | [241, 196, 15] | #f1c40f |
| 1-4 | Low/Green | #27ae60 | Green | [39, 174, 96] | [39, 174, 96] | #27ae60 |
| Headers | N/A | #1565c0 | N/A | N/A | [21, 101, 192] | #1565c0 |

---

## Troubleshooting

### Why These Fixes Matter

1. **Doubled Scores:** Makes the PDF look unprofessional and wastes space
2. **Wrong Colors:** Creates inconsistency between web and PDF views, confusing users
3. **False N/A:** Hides valuable HOC information that should be displayed

### Common Issues to Watch For

- Make sure not to remove the score from matrixBody (it's needed)
- Double-check the threshold boundaries (>= vs <=)
- Test with hazards that have actual HOC text vs. null/undefined
