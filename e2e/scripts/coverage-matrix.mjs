#!/usr/bin/env node
// Builds qa/coverage-matrix.md from qa/manual-test-plan.csv, the Playwright specs, qa/not-automated.json
// and (when present) e2e/results.json. Exits 1 if any plan row is unaccounted for or accounted twice.
// Usage: node scripts/coverage-matrix.mjs [--check] [--results results.json] [--out ../qa/coverage-matrix.md]
import { readFileSync, readdirSync, writeFileSync, existsSync, appendFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const e2eDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoDir = resolve(e2eDir, '..');
const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(name);
  return i >= 0 ? args[i + 1] : fallback;
};
const checkOnly = args.includes('--check');
const resultsPath = resolve(e2eDir, opt('--results', 'results.json'));
const outPath = resolve(e2eDir, opt('--out', '../qa/coverage-matrix.md'));

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
  row.push(field);
  if (row.some((f) => f !== '')) rows.push(row);
  const [header, ...body] = rows;
  return body.map((r) => Object.fromEntries(header.map((h, i) => [h, r[i] ?? ''])));
}

const plan = parseCsv(readFileSync(join(repoDir, 'qa/manual-test-plan.csv'), 'utf8'));
const notAutomated = JSON.parse(readFileSync(join(repoDir, 'qa/not-automated.json'), 'utf8'));
const errors = [];

const planIds = new Set();
for (const row of plan) {
  if (planIds.has(row.id)) errors.push(`${row.id} appears twice in the plan`);
  planIds.add(row.id);
}

// Static scan: every test('MT-xxx ...') in the specs.
const automated = new Map();
const testsDir = join(e2eDir, 'tests');
for (const file of readdirSync(testsDir).filter((f) => f.endsWith('.spec.ts')).sort()) {
  const src = readFileSync(join(testsDir, file), 'utf8');
  for (const m of src.matchAll(/\btest(?:\.\w+)?\(\s*['"`](MT-\d{3})\b([^'"`]*)['"`]/g)) {
    const [, id, rest] = m;
    const line = src.slice(0, m.index).split('\n').length;
    const spec = `e2e/tests/${file}:${line}`;
    if (automated.has(id)) errors.push(`${id} has more than one test (${automated.get(id).spec}, ${spec})`);
    automated.set(id, { spec, title: `${id}${rest}` });
  }
}

const excluded = new Map();
for (const entry of notAutomated) {
  if (excluded.has(entry.id)) errors.push(`${entry.id} is listed twice in qa/not-automated.json`);
  if (!entry.reason?.trim()) errors.push(`${entry.id} in qa/not-automated.json has no reason`);
  excluded.set(entry.id, entry);
}

for (const id of [...automated.keys(), ...excluded.keys()]) {
  if (!planIds.has(id)) errors.push(`${id} is referenced but is not in qa/manual-test-plan.csv`);
}
for (const { id } of plan) {
  const a = automated.has(id), e = excluded.has(id);
  if (a && e) errors.push(`${id} is both automated and listed as not automated`);
  if (!a && !e) errors.push(`${id} is neither automated nor listed in qa/not-automated.json`);
}

// Results from Playwright's JSON reporter, keyed by MT id.
const results = new Map();
const haveResults = !checkOnly && existsSync(resultsPath);
if (haveResults) {
  const report = JSON.parse(readFileSync(resultsPath, 'utf8'));
  const walk = (suite) => {
    for (const spec of suite.specs ?? []) {
      const id = spec.title.match(/^MT-\d{3}/)?.[0];
      if (!id) continue;
      const statuses = spec.tests.map((t) => t.status);
      const status = statuses.includes('unexpected') ? 'failed'
        : statuses.includes('flaky') ? 'flaky'
        : statuses.every((s) => s === 'skipped') ? 'skipped' : 'passed';
      results.set(id, status);
    }
    for (const child of suite.suites ?? []) walk(child);
  };
  for (const suite of report.suites ?? []) walk(suite);
  for (const id of automated.keys()) {
    if (!results.has(id)) errors.push(`${id} has a test but no result in ${relative(repoDir, resultsPath)}`);
  }
}

const badge = { passed: 'PASS', failed: 'FAIL', flaky: 'FLAKY', skipped: 'SKIPPED' };
const esc = (s) => String(s).replace(/\|/g, '\\|').replace(/\n/g, ' ');
const counts = { automated: 0, notAutomated: 0, passed: 0, failed: 0, other: 0 };
const lines = [];
for (const row of plan) {
  const a = automated.get(row.id);
  const e = excluded.get(row.id);
  let coverage, evidence, result;
  if (a) {
    counts.automated++;
    coverage = 'Automated';
    evidence = `\`${a.spec}\``;
    const r = results.get(row.id);
    result = haveResults ? badge[r] ?? 'NO RESULT' : 'not run';
    if (r === 'passed') counts.passed++;
    else if (r === 'failed') counts.failed++;
    else if (haveResults) counts.other++;
  } else if (e) {
    counts.notAutomated++;
    coverage = `Not automated (${e.category})`;
    evidence = e.reason;
    result = 'manual';
  } else {
    coverage = 'UNACCOUNTED';
    evidence = '';
    result = '';
  }
  lines.push(`| ${row.id} | ${esc(row.area)} | ${esc(row.title)} | ${row.priority} | ${coverage} | ${result} | ${esc(evidence)} |`);
}

const generatedFrom = haveResults
  ? `Playwright results from \`${relative(repoDir, resultsPath)}\` (${new Date().toISOString()})`
  : 'static scan only (no Playwright results)';
const md = [
  '# Manual regression plan: automation coverage matrix',
  '',
  'Generated by `e2e/scripts/coverage-matrix.mjs` from `qa/manual-test-plan.csv`, `e2e/tests/*.spec.ts` and `qa/not-automated.json`. Do not edit by hand.',
  '',
  `Source: ${generatedFrom}.`,
  '',
  `- Plan rows: **${plan.length}**`,
  `- Automated (Playwright, Chromium, against MySQL 8): **${counts.automated}**` +
    (haveResults ? ` (passed ${counts.passed}, failed ${counts.failed}${counts.other ? `, other ${counts.other}` : ''})` : ''),
  `- Not automated, stays manual: **${counts.notAutomated}**`,
  `- Unaccounted: **${plan.length - counts.automated - counts.notAutomated}**`,
  '',
  '| ID | Area | Title | Priority | Coverage | Result | Test / reason not automated |',
  '|---|---|---|---|---|---|---|',
  ...lines,
  '',
].join('\n');

if (errors.length) {
  console.error('Coverage accounting errors:\n' + errors.map((e) => `  - ${e}`).join('\n'));
}
if (!checkOnly) {
  writeFileSync(outPath, md);
  console.log(`Wrote ${relative(repoDir, outPath)}`);
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, md);
}
console.log(
  `${plan.length} rows: ${counts.automated} automated, ${counts.notAutomated} not automated` +
    (haveResults ? `; results: ${counts.passed} passed, ${counts.failed} failed` : ''),
);
process.exit(errors.length ? 1 : 0);
