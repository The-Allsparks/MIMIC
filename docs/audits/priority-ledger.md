# MIMIC priority ledger

Living work-order for the orchestrator. Update after each issue or pull request. Do not treat an empty ready column as “the library is complete.”

| Field | Value |
|-------|--------|
| **Updated** | 2026-08-17 |
| **Audited SHA** | `5847806f094f846cb3e8a4adf7ad0b355c4034fb` |
| **Current implementation stream** | Draft [PR #1](https://github.com/The-Allsparks/MIMIC/pull/1) (`feature/phase-0-scaffold`) |
| **Automatic merge** | **false** — human approval required |
| **Active subagent** | none until an issue is selected |
| **Hardware available** | no (elevator not selected) |

Full findings: [initial-deep-audit.md](initial-deep-audit.md). Roadmap: [#24](https://github.com/The-Allsparks/MIMIC/issues/24).

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

| Issue | Priority | Readiness | Dependencies | Status | Subagent | Branch | PR | CI | Merge | Blocker | Next action |
|-------|----------|-----------|--------------|--------|----------|--------|----|----|-------|---------|-------------|
| PR #1 Phase 0 scaffold | Foundation | Review | — | Draft, CI green | — | `feature/phase-0-scaffold` | [#1](https://github.com/The-Allsparks/MIMIC/pull/1) | Green | Not authorized | Human review | Keep as current stream; do not open a competing PR |
| #2 Research | Foundation | Implemented in PR #1 | — | Open | — | same | #1 | Green | Pending #1 | Close on merge of #1 | Commented |
| #3 Build vs adopt | Foundation | Implemented in PR #1 | #2 | Open | — | same | #1 | Green | Pending #1 | Close on merge of #1 | Commented |
| #4 Units conventions | Foundation | Implemented in PR #1 | — | Open | — | same | #1 | Green | Pending #1 | Close on merge of #1 | Commented |
| #5 Phase 0 abstraction | Foundation | Implemented in PR #1; C1 recorded as liveness | #4 | Open | — | same | #1 | Green | Pending #1 | Close on merge of #1 | Wait for #25 CI on PR #1 |
| #7 Fake hardware | Foundation | Implemented in PR #1 | #5 | Open | — | same | #1 | Green | Pending #1 | Close on merge of #1 | Commented |
| [#25](https://github.com/The-Allsparks/MIMIC/issues/25) Stale classification | HIGH correctness | Implemented locally | Phase 0 observer (PR #1) | Observer-liveness docs + tests on PR #1 | ended after implement | `feature/phase-0-scaffold` | #1 | Pending push | Not authorized | None | Push; wait for CI; human merge |
| [#26](https://github.com/The-Allsparks/MIMIC/issues/26) Snapshot validity | MEDIUM architecture | Ready after or with #25 | #25 preferred | Not started | — | — | — | — | — | Prefer #25 first | Queue |
| [#27](https://github.com/The-Allsparks/MIMIC/issues/27) sensorValid velocity | MEDIUM correctness | Ready | #5 | Not started | — | — | — | — | — | None | After #25 |
| [#28](https://github.com/The-Allsparks/MIMIC/issues/28) Missing limit logs | MEDIUM correctness | Ready | logger | Not started | — | — | — | — | — | None | After #25 |
| [#29](https://github.com/The-Allsparks/MIMIC/issues/29) Phase 1 flag honesty | MEDIUM usability | Ready (Javadoc now) | #6 for full telemetry | Not started | — | — | — | — | — | Full Phase 1 needs hardware | After #25 |
| [#30](https://github.com/The-Allsparks/MIMIC/issues/30) Pin Actions SHAs | MEDIUM security | Ready | — | Not started | — | — | — | — | — | None | After correctness slices |
| [#31](https://github.com/The-Allsparks/MIMIC/issues/31) Branch protection | HIGH security | Human decision | Reviewer policy | Not started | — | — | — | — | — | Maintainer policy | Request decision |
| [#32](https://github.com/The-Allsparks/MIMIC/issues/32) Logger allocation | MEDIUM performance | Research | Measurements | Not started | — | — | — | — | — | No Hub numbers | Benchmark only |
| [#6](https://github.com/The-Allsparks/MIMIC/issues/6) Passive REV telemetry | Phase 1 | **Blocked** | Robot + PR #1 | Open | — | — | — | — | — | Hardware | Wait |
| [#33](https://github.com/The-Allsparks/MIMIC/issues/33) Actuation flag split | LOW architecture | Deferred | Phase 6/10 | Not started | — | — | — | — | — | Later phases | Defer |
| [#34](https://github.com/The-Allsparks/MIMIC/issues/34) FTC SDK CI job | MEDIUM testing | Not ready | Maintainer choice | Not started | — | — | — | — | — | Policy | Defer |
| #8–#23 Phases 2–10 / SystemCore | Active / experimental | **Blocked** | Phase 0 robot observation + review | Open | — | — | — | — | — | Readiness gate | Do not implement |

## Selected next issue

| Field | Value |
|-------|--------|
| **Selected** | [#25](https://github.com/The-Allsparks/MIMIC/issues/25) — Fix `MechanismObserver` stale classification and add tests |
| **Status** | Implemented locally on `feature/phase-0-scaffold` (PR #1). Observer liveness documented and tested. Hub sample-age is **not** solved. CI pending. Not committed by the implementer. |
| **Why highest priority** | Highest-severity **ready** correctness gap. Phase 0 `STALE` is now an honest liveness check with tests. Later actuation must not treat it as Hub health. |
| **Dependencies** | Current observer on `feature/phase-0-scaffold` |
| **Expected deliverable** | Correct freshness rule, unit tests, short doc note |
| **Expected validation** | `./gradlew check` (local); CI on PR #1 after orchestrator commit |
| **Hardware required** | No |

## Stop conditions currently in effect

- **Do not merge** without human approval (`AUTOMATIC_MERGE=false`).
- **Do not enable** Phase 2–10 actuation.
- **Do not invent** elevator hardware.
- **Do not open** a second implementation PR while #1 is unresolved.
