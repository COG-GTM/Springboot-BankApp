# Bank of America — Devin Security Swarm (Hero Demo B) presenter script

**Audience:** Gully Patel, senior Developer Experience / platform leader (BofA). Not a CISO.
**Meeting:** 30-minute first meeting. This segment gets ~4 minutes live, plus a ~2-minute Semantic Galaxy moment on the companion deck.
**Framing for this audience:** security remediation is developer toil. Devin removes the toil; AppSec keeps the policy, branch protection and required reviewers keep the merge decision.

**Honesty rule — say this out loud at the start of the walkthrough:**
> "Everything you're about to see runs on a demonstration retail-banking codebase built in Spring Boot. It is not Bank of America code and it isn't meant to look like it."

Internal-only IDs are in this script for navigation. Do not put scan IDs, finding IDs, profile IDs, or private PR URLs on any customer-facing slide.

---

## Cast for this demo

| Role | What it is | Where |
|---|---|---|
| **Hero finding** | "Negative transfer amount allows stealing funds from any account" — critical, open. Controller entry point `BankController.java:82-96`, service sink `AccountService.java:103-117`, same root cause again at `AccountService.java:64-69`. | hero scan, finding `sfind-fd3d247f4788449092f169f9c7a447d1` |
| **Runtime-validation example** | Finding with validation evidence and repro steps in a separate scan | `.../code-scan/a81a05cb4c1246548e8e522283b359e5?finding=sfind-9d2e0681693c415cbc26096d03d5bbbe` |
| **Remediation example (pre-assigned — do NOT assign live)** | Session → PR #298: service-layer positive-amount validation plus Mockito tests. Open, mergeable, 1/1 checks passing. | session `6f1081397e3e4231a9c4da33c533668d`, PR #298 |
| **Secondary remediation (optional)** | Jenkins `DOCKER_TAG` command injection → PR #287. **In review, CI currently failing — never present as green.** | PR #287 |
| **Semantic Galaxy** | `galaxy_demo.html`, offline, opens from `file://` | companion deck, full-bleed |

**Why the hero finding is the hero, stated precisely:** it is a business-logic flaw a signature-based scanner does not carry a rule for. The scan records it as a **cross-file exploit path** — the entry point is in the controller, the missing guard and the money movement are in the service. It is *not* a multi-finding CVE chain, and the scan does not claim one. Say "exploit path", not "chain".

---

## Step 1 — Set the Scene (~35s)

**What to show.** Nothing yet, or the deck's Hero Demo B title slide. Have the hero scan already open in a second tab: `https://app.devin.ai/org/devin-gtm/code-scan/faf25804e03b4628b5362755ad6cc995`

**What to say.**
1. "Scanners produce alerts, not fixes. Every alert your scanners raise eventually lands on a developer who didn't write the finding, has to reconstruct the context, and then writes ten lines of validation."
2. "For a 30,000-developer organisation, that reconstruct-the-context step is the cost. It's not security work, it's toil, and it's spread across every team you support."
3. "What I'll show is the loop end to end: threat model encoded as a profile, finding, runtime validation, assign to Devin, reviewable PR. AppSec still owns the policy. Branch protection and required reviewers still decide what merges."

**Transition.** "It starts with your threat model, not with our defaults — so let's start where a security team encodes what it actually cares about."

---

## Step 2 — Scanning Profiles (~40s)

**What to show.**
- `https://app.devin.ai/org/devin-gtm/code-scan/profiles/create` — the create form. Point at: threat model / attacker persona, runtime-validation instructions, reporting format, include/exclude rules, batch size, regex/glob targeting.
- Then an existing profile: `https://app.devin.ai/org/devin-gtm/code-scan/profiles/7cfe94046c4642b8b08fbbd4b9355841`. Scroll the threat-model text.

**What to say.**
1. "A profile is where a security team encodes its existing threat model. Attacker persona, what counts as in scope, what evidence a finding must carry before anyone is allowed to page a developer."
2. "You don't write this from a blank page — point Devin at the threat-model documents you already have and it drafts the profile."
3. "For a platform team, the profile is the governance surface: one artifact your AppSec group owns, applied consistently across every repo, instead of 30,000 developers each interpreting the policy."

**Transition.** "Once the profile exists, running it is the boring part — which is exactly what you want."

---

## Step 3 — Run Scans (~30s)

**What to show.** The scan launch surface: repo picker plus profile picker. Contrast the interactive first run (supervised) with the scheduled run (the operating loop). Mention the org's recurring daily scan.

**What to say.**
1. "First run is interactive and supervised — you watch it, tune the profile, and throw away the noise before anyone sees it."
2. "Then it becomes scheduled, and the daily run is incremental, so the cost tracks security-relevant code rather than repo size. The same primitive works at one scan or a thousand."
3. "Phasing, if it comes up: Discovery, Pilot, Activated, Steady State. You do not turn this on for 30,000 developers on day one."

**Transition.** "Here's the output of a completed run against that demonstration banking codebase."

---

## Step 4 — Findings (hero moment, ~90s)

**What to show.**
1. Hero scan: `https://app.devin.ai/org/devin-gtm/code-scan/faf25804e03b4628b5362755ad6cc995`. Filter to the repo so only the Spring Boot demo repo's findings show (5 findings: 1 critical, 3 medium, 1 low).
2. Open the critical: "Negative transfer amount allows stealing funds from any account".
3. Scroll to the cited code. Show **both** files: `BankController.java:82-96` and `AccountService.java:103-117`.
4. Optional if you want a second, textbook example on screen: the representative finding from the other completed scan — `https://app.devin.ai/org/devin-gtm/code-scan/185dc9b0a8bf43b894f7825e97308822` and `...?finding=sfind-5d771736debd4620a5a3e056bc610a42`.

**What to say.**
1. "The critical one isn't a CVE and isn't a pattern match. `POST /transfer` binds an amount straight off the request. The service checks `balance.compareTo(amount) < 0` — that's the only guard, and with an amount of minus one hundred it's false, so the guard passes. Sender balance goes up a hundred, the named victim's goes down a hundred."
2. "Notice where the two halves live. The entry point is the controller; the missing check and the money movement are in the service. A single-file rule doesn't see that, and the scan says so explicitly — it records the path from entry point to sink, in the developer's own words, with the lines cited."
3. "That's the part that matters for your developers: they open this and there is nothing left to reconstruct. What was found, why it matters, the exact path, and the two other methods with the same root cause."

**Transition.** "The obvious question is how much of this is real. That's the next tab."

---

## Step 5 — Runtime Validation (~40s)

**What to show.** `https://app.devin.ai/org/devin-gtm/code-scan/a81a05cb4c1246548e8e522283b359e5?finding=sfind-9d2e0681693c415cbc26096d03d5bbbe` — scroll to the validation evidence and repro steps.

**What to say.**
1. "Where it can, Devin doesn't just assert exploitability — it reproduces it in an isolated environment and attaches the evidence and the repro steps."
2. "That is the false-positive control. Findings that can't be substantiated don't reach a developer's queue, which is the difference between a scanner your teams trust and one they filter to a folder."
3. "Find, validate, fix, ship — validation is the gate between the first two and the last two."

**Transition.** "So: validated finding. Now the part your developers actually feel."

---

## Step 6 — Assign to Devin (~55s)

**What to show.**
1. From the hero finding, show the **Assign to Devin** control — describe it, **do not click it**.
2. Switch to the pre-existing example: session `https://app.devin.ai/sessions/6f1081397e3e4231a9c4da33c533668d` → PR `https://github.com/COG-GTM/Springboot-BankApp/pull/298`. Show the diff: a `validateAmount` helper rejecting null and non-positive amounts, called at the top of `deposit`, `withdraw` and `transferAmount`, plus Mockito tests covering the negative-transfer case. Show checks passing.
3. Mention automatic duplicate dismissal: `https://app.devin.ai/org/devin-gtm/code-scan/185dc9b0a8bf43b894f7825e97308822?status=dismissed`.

**What to say.**
1. "One click on the finding produces this: a branch, the service-layer guard, tests that fail without it, and a PR against your normal review process. Not a ticket — a diff."
2. "Nothing here bypasses anything. It's an ordinary pull request: branch protection applies, required reviewers apply, CI applies. The repo owner who can merge is the stakeholder you need in the room — Devin never becomes that person."
3. "Duplicates get dismissed automatically, so the same root cause across repos doesn't turn into forty tickets for forty teams."

**Close for this audience.** "For your developers this is the difference between 'here's an alert, go read the codebase' and 'here's a diff, review it.' AppSec keeps the threat model, you keep the merge gate, and the reconstruct-the-context work — the part that scales with 30,000 developers — is gone."

---

## 60-second version (use when the meeting is running long)

> "One example from that scan. A transfer endpoint takes an amount off the request and hands it to the service. The only guard is 'is your balance less than the amount' — with minus a hundred, that's false, so it passes, and the transfer runs backwards: the attacker gains a hundred, the victim loses a hundred. No CVE, no signature; the entry point is in the controller and the flaw is in the service, so a single-file rule never sees it.
>
> Devin wrote up the path with both files cited, and the fix is already a pull request: one positive-amount check in the service layer, called from deposit, withdraw and transfer, with tests. It's an ordinary PR — branch protection and required reviewers unchanged.
>
> That's the whole pitch for a platform team: your developers stop reconstructing security context, and nothing about who approves a merge changes."

---

## Semantic Galaxy segment (~2 min, companion deck)

Open `galaxy_demo.html` from `file://` — no network needed.

1. **Heat map first (5s of silence).** "That's the demonstration codebase as subsystems, not folders — 79 subsystems, 20 relations between them. The red is where the scan's five findings landed, weighted by severity."
2. **`replay scan`** (~6s sweep). "That's the sweep: the swarm reads the security-relevant surface, then the heat fills in."
3. **`exploit path` / press `c`** (~8s per step, 4 steps). "Step 1, registration is the only anonymous route, so the attacker has an account. Step 2, the controller entry point. Step 3, the guard that a negative amount walks straight through. Step 4, the sink — the two balance writes. Two different files, which is the whole point."
4. **`preview PR fixes`.** "This is hypothetical, labelled as such on the canvas: what the map looks like once the open PR merges. Right now the counter reads five open, because PR #298 is not merged."

Panel copy is customer-safe: no scan IDs, no finding IDs, and the remediation line reads `PR: #298 (private demonstration repository)`.

---

## Independent third-party corroboration (for the deck)

The hero finding is a bespoke logic bug, so it has no CVE of its own. Two honest classes of external reference:

**Same vulnerability class, in a real product, with a real CVE — the strongest one to put on the slide:**
- **CVE-2020-11007 / GHSA-w8rc-pgxq-x2cj — "Negative charge in shopping cart", Shopizer (Java/Spring, critical, CWE-20).** Negative quantity not validated at the controller/API layer, producing a negative order total; fixed by a back-end check on the quantity parameter. Same shape as the hero finding in a different domain.
  `https://github.com/advisories/GHSA-w8rc-pgxq-x2cj` · `https://nvd.nist.gov/vuln/detail/CVE-2020-11007`

**Class-level definitions:**
- MITRE **CWE-840 Business Logic Errors** — `https://cwe.mitre.org/data/definitions/840.html` (a category; MITRE marks it PROHIBITED for mapping to real-world vulnerabilities, so cite it as context, never as this finding's identifier)
- MITRE **CWE-1284 Improper Validation of Specified Quantity in Input** — `https://cwe.mitre.org/data/definitions/1284.html`
- OWASP **Business logic vulnerability** — `https://owasp.org/www-community/vulnerabilities/Business_logic_vulnerability`

**Second, CVE-backed reference from the same scan (a dependency finding, not the hero):**
- **CVE-2023-22102 / GHSA-m6vm-37g8-gqvh — MySQL Connector/J takeover**, affecting `mysql:mysql-connector-java` ≤ 8.0.33, which this codebase pins. Patched only under the relocated coordinate `com.mysql:mysql-connector-j:8.2.0`. GitHub rates it High (CVSS 8.9); the scan rated the finding medium in this codebase's context — if asked, that gap is the point of context-aware triage, not an error.
  `https://github.com/advisories/GHSA-m6vm-37g8-gqvh`

Do not describe CWE-840 or CWE-1284 as a CVE for this finding. They are class definitions; CVE-2020-11007 is the instance.

---

## Readiness checklist

Run this the morning of the meeting.

| # | Check | URL / artifact | Expected |
|---|---|---|---|
| 1 | Hero scan loads, findings present | `https://app.devin.ai/org/devin-gtm/code-scan/faf25804e03b4628b5362755ad6cc995` | Completed scan; filtered to the Spring Boot demo repo: 5 findings (1 critical, 3 medium, 1 low) |
| 2 | Hero finding opens with both files cited | hero scan → "Negative transfer amount allows stealing funds from any account" | Critical, open; `AccountService.java:103-117`, `AccountService.java:64-69`, `BankController.java:82-96` |
| 3 | Scan profile create page | `https://app.devin.ai/org/devin-gtm/code-scan/profiles/create` | Form with threat model, validation instructions, include/exclude, batch size |
| 4 | Example profile | `https://app.devin.ai/org/devin-gtm/code-scan/profiles/7cfe94046c4642b8b08fbbd4b9355841` | Populated threat model text |
| 5 | Completed reference scan | `https://app.devin.ai/org/devin-gtm/code-scan/185dc9b0a8bf43b894f7825e97308822` | Findings list renders |
| 6 | Example finding | `...185dc9b0a8bf43b894f7825e97308822?finding=sfind-5d771736debd4620a5a3e056bc610a42` | Finding detail with cited code |
| 7 | Runtime-validation example | `...a81a05cb4c1246548e8e522283b359e5?finding=sfind-9d2e0681693c415cbc26096d03d5bbbe` | Validation evidence + repro steps visible |
| 8 | Dismissed duplicates view | `...185dc9b0a8bf43b894f7825e97308822?status=dismissed` | Dismissed list non-empty |
| 9 | Remediation session | `https://app.devin.ai/sessions/6f1081397e3e4231a9c4da33c533668d` | Session opens |
| 10 | Remediation PR | `https://github.com/COG-GTM/Springboot-BankApp/pull/298` | Open, mergeable, checks passing, `validateAmount` diff + Mockito tests |
| 11 | Secondary PR (only if used) | PR #287 | Open; **CI failing** — describe as "in review" only |
| 12 | Advisory links | the five URLs above | All load and describe the cited issue |
| 13 | Galaxy offline | `galaxy_demo.html` over `file://` | Loads with no network; light and dark both clean; replay, exploit path, preview PR fixes all work |
| 14 | Second monitor / tabs | all of the above pre-opened | No live logins mid-demo |

**Verified in this session** (see the handoff message for the exact result of each): items 1, 2, 9, 10, 12; the galaxy in item 13 was verified in Chrome over `file://` in both themes. Items 3-8 and 11 are Devin-app pages that need an authenticated browser session — re-check them yourself the morning of the meeting.

---

## Static-screenshot fallback (if the Code Scan surface is unavailable)

Capture these in advance and keep them in the deck appendix, in this order:

1. Scan profile edit view showing the threat-model field.
2. Scan list showing the completed hero scan with its finding counts.
3. Hero finding header: title, critical severity, category, affected file list.
4. Hero finding body: the cited `BankController.java:82-96` snippet.
5. Hero finding body: the cited `AccountService.java:103-117` snippet with the `compareTo` guard.
6. Runtime-validation evidence and repro steps.
7. Dismissed-duplicates list.
8. PR #298 diff — the `validateAmount` helper and its three call sites.
9. PR #298 checks-passing state.
10. Galaxy heat map (`galaxy_heatmap_1280x720.png`) and exploit path (`galaxy_exploit_path_1280x720.png`), both attached alongside this script.

## Do not

- Do not assign a finding live — it creates noise; use PR #298.
- Do not call the hero finding a chain. It is a cross-file exploit path: controller entry point, service sink.
- Do not present PR #287 as green; its CI is failing.
- Do not imply the repository is Bank of America code.
- Do not show scan IDs, finding IDs, profile IDs, or private PR URLs on a customer-facing slide.
