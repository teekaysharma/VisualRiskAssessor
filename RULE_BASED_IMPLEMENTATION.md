# Rule-Based Hazard Detection Implementation

This document describes the changes needed to implement a keyword-matching engine for fallback object detection.

## Changes to `/home/engine/project/index.html`

### 1. Add KEYWORD_HAZARD_MAPPING constant

Insert this code after line 822 (after `function openFilePicker()`):

```javascript
const KEYWORD_HAZARD_MAPPING = {
  'ladder': { hazardKey: 'height', name: 'Working at Height', L: 3, S: 4 },
  'chemical drum': { hazardKey: 'chemical', name: 'Chemical Exposure', L: 2, S: 5 },
  'cable': { hazardKey: 'electrical', name: 'Electrical Hazard', L: 3, S: 4 },
  'puddle': { hazardKey: 'slip', name: 'Slip/Trip/Fall Hazard', L: 3, S: 3 },
  'liquid': { hazardKey: 'slip', name: 'Slip/Trip/Fall Hazard', L: 3, S: 3 },
  'scaffold': { hazardKey: 'height', name: 'Fall from Height', L: 2, S: 5 },
  'forklift': { hazardKey: 'struck', name: 'Struck by Vehicle', L: 3, S: 4 },
  'person': { hazardKey: 'ppe', name: 'PPE Missing/Inadequate', L: 4, S: 3 }
};
```

### 2. Add ruleBasedHazards function

Insert this code after the KEYWORD_HAZARD_MAPPING constant:

```javascript
function ruleBasedHazards(predictions) {
  const foundHazards = new Map();
  
  predictions.forEach(p => {
    const label = p.class.toLowerCase();
    
    for (const [keyword, mapping] of Object.entries(KEYWORD_HAZARD_MAPPING)) {
      if (label.includes(keyword) || keyword.includes(label)) {
        const hazardKey = mapping.hazardKey;
        const dbEntry = hazardDB[hazardKey] || hazardDB.other;
        
        if (!foundHazards.has(hazardKey)) {
          foundHazards.set(hazardKey, {
            name: mapping.name,
            L: mapping.L,
            S: mapping.S,
            risk: mapping.L * mapping.S,
            conf: 0.75,
            recs: dbEntry.recs,
            source: 'rule-based',
            detected: p.class
          });
        }
      }
    }
  });
  
  const out = [...foundHazards.values()];
  if (out.length === 0) {
    const h = hazardDB.other;
    out.push({
      name: h.name,
      L: h.L,
      S: h.S,
      risk: h.L * h.S,
      conf: 0.5,
      recs: h.recs,
      source: 'rule-based'
    });
  }
  
  return out;
}
```

### 3. Update analyze function to use rule-based detection

In the `analyze` function (around line 1715), modify the logic to call `ruleBasedHazards` instead of `heuristicHazards` when COCO-SSD detects objects.

Replace lines 1767-1770:
```javascript
if (hazards.length === 0) {
  hazards = heuristicHazards(canvas);
  analysisMode = 'heuristic';
}
```

With:
```javascript
if (hazards.length === 0) {
  if (cocoModel) {
    const predictions = await cocoModel.detect(img);
    hazards = ruleBasedHazards(predictions);
    analysisMode = 'rule-based';
  } else {
    hazards = heuristicHazards(canvas);
    analysisMode = 'heuristic';
  }
}
```

### 4. Update mode labels

Add 'rule-based' to the mode labels object (around line 1127):
```javascript
const modeLabels = {
  ai: ['AI Vision', 'mode-ai'],
  coco: ['Object Detection', 'mode-ml'],
  'rule-based': ['Rule-Based', 'mode-ml'],
  heuristic: ['Heuristic', 'mode-heuristic'],
  manual: ['Manual Entry', 'mode-manual'],
  baseline: ['Baseline Context', 'mode-heuristic']
};
```

### 5. Update export mode label

Update the mode label mapping in exportReport function (around line 1784):
```javascript
const modeLabel = {
  ai: 'AI Vision (Groq)',
  coco: 'Object Detection (COCO-SSD)',
  'rule-based': 'Rule-Based Detection (Keyword Matcher)',
  heuristic: 'Heuristic (pixel analysis)',
  manual: 'Manual Entry'
}[analysisMode] || analysisMode;
```

Also update in exportPDF function (around line 1858):
```javascript
const modeLabel = {
  ai: 'AI Vision — Llama 4 Scout (Groq)',
  coco: 'Object Detection (COCO-SSD)',
  'rule-based': 'Rule-Based Detection (Keyword Matcher)',
  heuristic: 'Heuristic (pixel analysis)'
}[analysisMode] || analysisMode;
```

## Summary

These changes implement a keyword-matching engine that:
1. Maintains a lookup table mapping detected object labels to hazard types
2. When AI vision fails, runs the keyword matcher against object labels from COCO-SSD
3. Outputs hazards with source badge "Rule-Based" instead of "AI"
4. Uses specific likelihood/severity scores as specified in the task

The mapping includes:
- "ladder" → Working at Height, L3/S4
- "chemical drum" → Chemical Exposure, L2/S5
- "cable" → Electrical, L3/S4
- "puddle / liquid" → Slip, L3/S3
- "scaffold" → Fall from Height, L2/S5
- "forklift" → Struck by Vehicle, L3/S4
- "worker without helmet" (matched via "person") → PPE, L4/S3
