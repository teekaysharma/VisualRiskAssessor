#!/usr/bin/env node
// Syntax-only check of every inline <script> block in index.html.
//
// index.html has no build step, so nothing normally parses its JS before
// it ships to GitHub Pages — a broken template literal (this project has
// had exactly that bug before) or a stray brace would only surface live,
// in a browser, after it's already deployed. This just runs each inline
// script through `node --check`, which validates syntax without executing
// anything (no DOM/browser globals needed for that).
//
// Deliberately does NOT check the external <script src="..."> tags (CDN
// libraries, risk-core.js) — only the app's own inline code.

import { readFileSync, writeFileSync, unlinkSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const file = process.argv[2] || 'index.html';
const html = readFileSync(file, 'utf8');

// Matches <script> ... </script> blocks that have no src attribute.
const scriptRe = /<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/gi;

let match;
let count = 0;
let failed = false;

while ((match = scriptRe.exec(html)) !== null) {
  const code = match[1];
  if (!code.trim()) continue;
  count++;

  const tmpFile = join(tmpdir(), `index-inline-script-${count}.js`);
  writeFileSync(tmpFile, code);
  try {
    execFileSync(process.execPath, ['--check', tmpFile], { stdio: 'pipe' });
    console.log(`  inline <script> #${count}: OK (${code.split('\n').length} lines)`);
  } catch (err) {
    failed = true;
    console.error(`  inline <script> #${count}: SYNTAX ERROR`);
    console.error(err.stderr ? err.stderr.toString() : err.message);
  } finally {
    unlinkSync(tmpFile);
  }
}

if (count === 0) {
  console.error(`No inline <script> blocks found in ${file} — check the file path/regex.`);
  process.exit(1);
}

if (failed) {
  process.exit(1);
}

console.log(`${count} inline <script> block(s) in ${file}: syntax OK`);
