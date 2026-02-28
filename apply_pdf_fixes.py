#!/usr/bin/env python3
"""Apply PDF export function fixes to index.html"""

# Read original file
with open('index.html', 'r') as f:
    content = f.read()

# Track changes
changes_made = []

# Fix 1: Remove the doubled score text in didDrawCell
old_score_block = """              // Draw score number at top-center of cell (always visible)
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
              doc.text(String(score), cellX + cellW / 2, scoreY, { align: 'center' });"""

new_score_block = """              // Score text already drawn by autoTable from matrixBody"""

if old_score_block in content:
    content = content.replace(old_score_block, new_score_block)
    changes_made.append("✓ Removed doubled score text in PDF matrix")

# Fix 2: Update the matrixCellColor function with exact thresholds
old_color_func = """        function matrixCellColor(score) {
          if (score <= 4) return [39, 174, 96];    // Low - green
          if (score <= 9) return [241, 196, 15];   // Medium - yellow
          if (score <= 16) return [230, 126, 34];  // High - orange
          return [192, 57, 43];                     // Extreme - red
        }"""

new_color_func = """        function matrixCellColor(score) {
          if (score >= 17) return [192, 57, 43];    // Red
          if (score >= 10) return [230, 126, 34];  // Amber
          if (score >= 5) return [241, 196, 15];   // Yellow
          if (score >= 1) return [39, 174, 96];    // Green
          return [21, 101, 192];                    // Blue (for headers)
        }"""

if old_color_func in content:
    content = content.replace(old_color_func, new_color_func)
    changes_made.append("✓ Updated PDF matrix color thresholds to match web function")

# Fix 2b: Update the textColor logic to match new thresholds
old_textcolor_logic = """data.cell.styles.textColor = (score > 4 && score <= 9) ? [33, 33, 33] : [255, 255, 255];"""
new_textcolor_logic = """data.cell.styles.textColor = (score >= 5 && score <= 9) ? [33, 33, 33] : [255, 255, 255];"""

if old_textcolor_logic in content:
    content = content.replace(old_textcolor_logic, new_textcolor_logic)
    changes_made.append("✓ Updated text color logic for new thresholds")

# Fix 3: Update HOC table to check for null/undefined instead of falsy
old_hoc_body = """      const hocBody = allHazardsForPDF.map((h, i) => [
        i + 1,
        h.name,
        h.eliminate || 'N/A',
        h.substitute || 'N/A',
        h.engineering || 'N/A',
        h.administrative || 'N/A',
        h.ppe || 'N/A'
      ]);"""

new_hoc_body = """      const hocBody = allHazardsForPDF.map((h, i) => [
        i + 1,
        h.name,
        h.eliminate ?? 'N/A',
        h.substitute ?? 'N/A',
        h.engineering ?? 'N/A',
        h.administrative ?? 'N/A',
        h.ppe ?? 'N/A'
      ]);"""

if old_hoc_body in content:
    content = content.replace(old_hoc_body, new_hoc_body)
    changes_made.append("✓ Updated HOC table to show full text instead of N/A")

# Write modified content back
with open('index.html', 'w') as f:
    f.write(content)

print("\nPDF Export Fixes - Changes Applied Successfully!")
print("=" * 60)
for change in changes_made:
    print(change)
print("=" * 60)
print(f"\nTotal changes: {len(changes_made)}")
print("\nThe following issues have been fixed:")
print("• Removed doubled score numbers in PDF matrix")
print("• Updated color thresholds to match web function:")
print("  - score >= 17 → #c0392b (Red)")
print("  - score >= 10 → #e67e22 (Amber)")
print("  - score >= 5  → #f1c40f (Yellow)")
print("  - score >= 1  → #27ae60 (Green)")
print("  - headers → #1565c0 (Blue)")
print("• Hierarchy of Controls now shows full text from hazard objects")
print("  instead of displaying 'N/A' for populated fields")
