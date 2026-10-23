#!/usr/bin/env node
// Fails unless qa/coverage-matrix.md accounts for every row of qa/manual-test-plan.csv exactly once,
// every "Automated" row has exactly one Playwright test titled with its ID, and every
// "Not automated" row has a reason and no test.
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const errors = [];

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
      row.push(field); rows.push(row); row = []; field = '';
    } else field += c;
  }
  if (field || row.length) { row.push(field); rows.push(row); }
  return rows.filter((r) => r.some((f) => f.trim()));
}

const [header, ...planRows] = parseCsv(readFileSync(join(root, 'qa/manual-test-plan.csv'), 'utf8'));
const idCol = header.indexOf('id');
const planIds = planRows.map((r) => r[idCol]);
if (new Set(planIds).size !== planIds.length) errors.push('qa/manual-test-plan.csv has duplicate IDs');

const matrix = new Map();
for (const line of readFileSync(join(root, 'qa/coverage-matrix.md'), 'utf8').split('\n')) {
  const cells = line.split('|').map((c) => c.trim());
  if (!/^MT-\d{3}$/.test(cells[1] ?? '')) continue;
  const [, id, , , , status, test, notes] = cells;
  if (matrix.has(id)) errors.push(`${id} appears more than once in the coverage matrix`);
  matrix.set(id, { status, test, notes });
}

const testsById = new Map();
const testsDir = join(root, 'e2e/tests');
for (const file of readdirSync(testsDir).filter((f) => f.endsWith('.spec.ts'))) {
  const source = readFileSync(join(testsDir, file), 'utf8');
  for (const m of source.matchAll(/\btest(?:\.\w+)?\(\s*['"`](MT-\d{3})\b/g)) {
    testsById.set(m[1], [...(testsById.get(m[1]) ?? []), file]);
  }
}

for (const id of planIds) {
  const entry = matrix.get(id);
  const tests = testsById.get(id) ?? [];
  if (!entry) { errors.push(`${id} is in the plan but missing from the coverage matrix`); continue; }
  if (entry.status === 'Automated') {
    if (tests.length !== 1) errors.push(`${id} is marked Automated but has ${tests.length} Playwright tests`);
    else if (!entry.test.includes(tests[0])) errors.push(`${id} matrix points at "${entry.test}" but the test is in ${tests[0]}`);
  } else if (entry.status === 'Not automated') {
    if (tests.length) errors.push(`${id} is marked Not automated but has a test in ${tests.join(', ')}`);
    if (!entry.notes) errors.push(`${id} is marked Not automated without a reason`);
  } else errors.push(`${id} has unknown status "${entry.status}"`);
}
for (const id of [...matrix.keys(), ...testsById.keys()]) {
  if (!planIds.includes(id)) errors.push(`${id} is not in qa/manual-test-plan.csv`);
}

const automated = planIds.filter((id) => matrix.get(id)?.status === 'Automated').length;
if (errors.length) {
  console.error(`Coverage matrix check FAILED:\n- ${[...new Set(errors)].join('\n- ')}`);
  process.exit(1);
}
console.log(`Coverage matrix OK: ${planIds.length} plan rows, ${automated} automated, ${planIds.length - automated} not automated (with reasons).`);
