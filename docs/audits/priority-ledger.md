# MIMIC priority ledger

Living work-order for the orchestrator. Update after each issue or pull request. Do not treat an empty ready column as "the library is complete."

| Field | Value |
|-------|--------|
| **Updated** | 2026-09-17 |
| **Audited SHA** | `feat/generic-mechanism-catalog` `39186a1` plus family observation sketches |
| **Current implementation stream** | [PR #71](https://github.com/The-Allsparks/MIMIC/pull/71) catalog + declaration-only contracts (#49–#54, #56–#65) |
| **Automatic merge** | **false** — human approval required |
| **Active subagent** | none |
| **Hardware available** | no (TeamCode has not wired a mechanism; Hub not required for this stream) |

Full findings: [initial-deep-audit.md](initial-deep-audit.md). Catalog: [generic-mechanism-catalog.md](../mechanism-control/generic-mechanism-catalog.md). Gaps: [generic-mechanism-gap-matrix.md](../mechanism-control/generic-mechanism-gap-matrix.md). Roadmap: [#24](https://github.com/The-Allsparks/MIMIC/issues/24).

## Priority model

1. Safety blockers
2. Correctness blockers
3. CI or build failures
4. Issues blocking multiple other issues
5. Architectural seams needed by later work
6. Missing tests for upcoming work
7. Small complete user-facing improvements
8. Performance work supported by measurements
9. Documentation and usability
10. Optional advanced capabilities
11. Cosmetic cleanup

An issue is **ready** only when requirements are clear, dependencies are resolved, acceptance criteria are testable, hardware is available or unnecessary, and work will not conflict with an unresolved implementation PR.

## Ledger

| Issue | Priority | Readiness | Dependencies | Status | Branch | PR | Merge | Blocker | Next action |
|-------|----------|-----------|--------------|--------|--------|----|-------|---------|-------------|
| Phase 0 scaffold | Foundation | Merged | — | Closed via PR #1 | `feature/phase-0-scaffold` | [#1](https://github.com/The-Allsparks/MIMIC/pull/1) | Merged | None | Done |
| #2–#5, #7, #25–#28 | Foundation / correctness | Merged | — | Closed | various | #1, #40–#42 | Merged | None | Done |
| [#49](https://github.com/The-Allsparks/MIMIC/issues/49)–[#54](https://github.com/The-Allsparks/MIMIC/issues/54), [#56](https://github.com/The-Allsparks/MIMIC/issues/56)–[#65](https://github.com/The-Allsparks/MIMIC/issues/65) | Architecture seam | Implementing | Phase 0 on `main` | Declaration-only catalog + contracts | `feat/generic-mechanism-catalog` | [#71](https://github.com/The-Allsparks/MIMIC/pull/71) | Not authorized | Human review | Review PR #71; do not merge automatically |
| [#55](https://github.com/The-Allsparks/MIMIC/issues/55) Debounce | Architecture seam | Implementing | — | Separate PR (avoid file conflict with #71) | `feature/issue-55-debounce` | [#73](https://github.com/The-Allsparks/MIMIC/pull/73) | Not authorized | Human review | Review after or with #71 |
| [#30](https://github.com/The-Allsparks/MIMIC/issues/30) Pin Actions SHAs | MEDIUM | Implementing | — | Open | `feature/issue-30-pin-actions` | [#72](https://github.com/The-Allsparks/MIMIC/pull/72) | Not authorized | None | Review; independent of catalog |
| [#29](https://github.com/The-Allsparks/MIMIC/issues/29) Phase 1 flag honesty | MEDIUM | Implementing | docs; #6 for full telemetry | Open | `feature/issue-29-phase1-flag-honesty` | [#74](https://github.com/The-Allsparks/MIMIC/pull/74) | Not authorized | Hardware for full Phase 1 | Review docs/flag extras; do not duplicate #6 |
| [#34](https://github.com/The-Allsparks/MIMIC/issues/34) FTC SDK packaging | P0 integration | Not started | Maintainer policy | Open | — | — | — | Policy | Separate epic; do not add SDK to core |
| [#67](https://github.com/The-Allsparks/MIMIC/issues/67) FTC hardware bindings | Phase 0 pointer | Blocked | #34 | Open | — | — | — | Packaging | Keep supplier adapters; no SDK in core |
| [#6](https://github.com/The-Allsparks/MIMIC/issues/6) Passive REV telemetry | Phase 1 | **Blocked** | Robot + #34 | Open | — | — | — | Hardware | Wait; desktop adapters already exist |
| [#68](https://github.com/The-Allsparks/MIMIC/issues/68) Hardware acceptance | Phase 1 | **Blocked** (sign-off) | #6, robot | Open | — | — | — | Hardware | Family sketches are not signed cards |
| [#69](https://github.com/The-Allsparks/MIMIC/issues/69) Active-control gate | Phase 3 | **Blocked** | #10, #6 graphs | Open | — | — | — | Hardware + review | Do not enable Phase 2–10 |
| [#31](https://github.com/The-Allsparks/MIMIC/issues/31) Branch protection | HIGH | Human decision | Reviewer policy | Open | — | — | — | Maintainer policy | Request decision |
| [#32](https://github.com/The-Allsparks/MIMIC/issues/32) Logger allocation | MEDIUM | Research | Hub measurements | Open | — | — | — | No Hub numbers | Benchmark only |
| [#33](https://github.com/The-Allsparks/MIMIC/issues/33) Actuation flag split | LOW | Deferred | Phase 6/10 | Open | — | — | — | Later phases | Do not set those flags for metadata |
| #8–#23 Phases 2–10 / SystemCore | Active / experimental | **Blocked** | Phase 0 robot observation + review | Open | — | — | — | Readiness gate | Do not implement |
| [#66](https://github.com/The-Allsparks/MIMIC/issues/66), [#70](https://github.com/The-Allsparks/MIMIC/issues/70) | Experimental | **Blocked** | #69 / #22 | Open | — | — | — | Active-control gate | Do not implement |

## Selected next issue

| Field | Value |
|-------|--------|
| **Selected** | Human review of open software PRs #71–#74. No new catalog issue. |
| **Status** | #49–#65 (except #55) are stacked on PR #71 with green CI. Family observation sketches added so every catalog family has a Phase 0 example. |
| **Why highest priority** | Implementation for the allowed stream is in review. Starting #6 / #68 / #69 would claim robot proof that does not exist. |
| **Dependencies** | Maintainer review. Do not merge automatically. |
| **Expected deliverable** | Reviewed PRs; flags stay default-off; FakeActuator writes stay 0 |
| **Expected validation** | `./gradlew check`; CI already green on #71–#74 |
| **Hardware required** | No |

## Stop conditions currently in effect

- **Do not merge** without human approval (`AUTOMATIC_MERGE=false`).
- **Do not enable** Phase 2–10 actuation.
- **Do not invent** robot hardware maps or season game-piece types in this library. See [library-vs-teamcode.md](../mechanism-control/library-vs-teamcode.md).
- **Do not reopen** the merged Phase 0 branch (`feature/phase-0-scaffold`). New work targets `main`.
- **Do not implement** homing motion, limit enforcement that writes, or [#6](https://github.com/The-Allsparks/MIMIC/issues/6) / [#68](https://github.com/The-Allsparks/MIMIC/issues/68) / [#69](https://github.com/The-Allsparks/MIMIC/issues/69) robot sign-off.
