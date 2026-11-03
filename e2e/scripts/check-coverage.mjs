#!/usr/bin/env node
// Fails unless qa/coverage-matrix.csv accounts for every row of qa/manual-test-plan.csv exactly once,
// every "automated" row has a matching Playwright test, and every "manual" row has a reason and no test.
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

function parseCsv(text) {
  const rows = [];
  let row = [], field = '', quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') { field += '"'; i++; }
      else if (c === '"') quoted = false;
      else field += c;
    } else if (c === '"') quoted = true;
    else if (c === ',') { row.push(field); field = ''; }
    else if (c === '\n' || c === '\r') {
      if (c === '\r' && text[i + 1] === '\n') i++;
      row.push(field); field = '';
      if (row.some((f) => f !== '')) rows.push(row);
      row = [];
    } else field += c;
  }
  if (field !== '' || row.length) { row.push(field); rows.push(row); }
  const [header, ...body] = rows;
  return body.map((r) => Object.fromEntries(header.map((h, i) => [h, r[i] ?? ''])));
}

const plan = parseCsv(readFileSync(join(root, 'qa/manual-test-plan.csv'), 'utf8'));
const matrix = parseCsv(readFileSync(join(root, 'qa/coverage-matrix.csv'), 'utf8'));

const testsDir = join(root, 'e2e/tests');
const testTitles = new Map();
for (const file of readdirSync(testsDir).filter((f) => f.endsWith('.spec.ts'))) {
  const src = readFileSync(join(testsDir, file), 'utf8');
  for (const m of src.matchAll(/\btest(?:\.\w+)?\(\s*'(MT-\d{3}) ([^']*)'/g)) {
    const id = m[1];
    if (!testTitles.has(id)) testTitles.set(id, []);
    testTitles.get(id).push({ file: `e2e/tests/${file}`, title: `${id} ${m[2]}` });
  }
}

const errors = [];
const planIds = plan.map((r) => r.id);
const matrixById = new Map();
for (const row of matrix) {
  if (matrixById.has(row.id)) errors.push(`${row.id}: listed more than once in the coverage matrix`);
  matrixById.set(row.id, row);
}

for (const p of plan) {
  const m = matrixById.get(p.id);
  if (!m) { errors.push(`${p.id}: missing from the coverage matrix`); continue; }
  if (m.title !== p.title) errors.push(`${p.id}: matrix title "${m.title}" does not match plan "${p.title}"`);
  const tests = testTitles.get(p.id) ?? [];
  if (m.status === 'automated') {
    if (tests.length !== 1) errors.push(`${p.id}: marked automated but has ${tests.length} Playwright tests`);
    else {
      const t = tests[0];
      if (m.automated_test !== `${t.file} :: ${t.title}`) errors.push(`${p.id}: automated_test "${m.automated_test}" should be "${t.file} :: ${t.title}"`);
      if (t.title !== `${p.id} ${p.title}`) errors.push(`${p.id}: test title "${t.title}" does not match plan title`);
    }
  } else if (m.status === 'manual') {
    if (!m.notes.trim()) errors.push(`${p.id}: manual row needs a reason in notes`);
    if (tests.length) errors.push(`${p.id}: marked manual but a Playwright test exists`);
  } else errors.push(`${p.id}: unknown status "${m.status}"`);
}
for (const id of matrixById.keys()) if (!planIds.includes(id)) errors.push(`${id}: in the coverage matrix but not in the plan`);
for (const id of testTitles.keys()) if (!planIds.includes(id)) errors.push(`${id}: Playwright test has no plan row`);

const automated = matrix.filter((r) => r.status === 'automated').length;
const manual = matrix.filter((r) => r.status === 'manual');
console.log(`Plan rows: ${plan.length}. Automated: ${automated}. Manual: ${manual.length} (${manual.map((r) => r.id).join(', ')}).`);
if (errors.length) {
  console.error(`Coverage matrix check FAILED:\n- ${errors.join('\n- ')}`);
  process.exit(1);
}
console.log('Coverage matrix check passed: every plan row is accounted for exactly once.');
