# MIMIC priority ledger

Living work-order for the orchestrator. Update after each issue or pull request. Do not treat an empty ready column as "the library is complete."

| Field | Value |
|-------|--------|
| **Updated** | 2026-09-16 |
| **Audited SHA** | `origin/main` after [PR #48](https://github.com/The-Allsparks/MIMIC/pull/48) (`e9fdb70`) |
| **Current implementation stream** | `feat/generic-mechanism-catalog` (passive catalog + configuration) |
| **Automatic merge** | **false** — human approval required |
| **Active subagent** | catalog / configuration slice |
| **Hardware available** | no (TeamCode has not wired a mechanism yet) |

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
| Catalog + configuration | Architecture seam | Implementing | Phase 0 on `main` | Passive metadata only | `feat/generic-mechanism-catalog` | — | Not authorized | None | Human review; issues [#49](https://github.com/The-Allsparks/MIMIC/issues/49)–[#51](https://github.com/The-Allsparks/MIMIC/issues/51) |
| [#34](https://github.com/The-Allsparks/MIMIC/issues/34) FTC SDK packaging | P0 integration | Not started | Maintainer policy | Open | — | — | — | Policy | Separate epic; do not add SDK to core |
| [#6](https://github.com/The-Allsparks/MIMIC/issues/6) Passive REV telemetry | Phase 1 | **Blocked** | Robot + #34 | Open | — | — | — | Hardware | Wait |
| [#29](https://github.com/The-Allsparks/MIMIC/issues/29) Phase 1 flag honesty | MEDIUM | Ready (docs) | #6 for full telemetry | Open | — | — | — | Hardware for full Phase 1 | After robot graphs |
| [#30](https://github.com/The-Allsparks/MIMIC/issues/30) Pin Actions SHAs | MEDIUM | Ready | — | Open | — | — | — | None | After this PR |
| [#31](https://github.com/The-Allsparks/MIMIC/issues/31) Branch protection | HIGH | Human decision | Reviewer policy | Open | — | — | — | Maintainer policy | Request decision |
| [#32](https://github.com/The-Allsparks/MIMIC/issues/32) Logger allocation | MEDIUM | Research | Hub measurements | Open | — | — | — | No Hub numbers | Benchmark only |
| [#33](https://github.com/The-Allsparks/MIMIC/issues/33) Actuation flag split | LOW | Deferred | Phase 6/10 | Open | — | — | — | Later phases | Do not set those flags for metadata |
| #8–#23 Phases 2–10 / SystemCore | Active / experimental | **Blocked** | Phase 0 robot observation + review | Open | — | — | — | Readiness gate | Do not implement |

## Selected next issue

| Field | Value |
|-------|--------|
| **Selected** | Passive generic mechanism catalog and configuration (Phase 0 metadata) |
| **Status** | Implementing on `feat/generic-mechanism-catalog`. Child issues to be filed; do not treat #8–#23 as unblocked. |
| **Why highest priority** | Architectural seam: TeamCode cannot declare a generic mechanism without topology, sensor roles, or validation. No actuation. |
| **Dependencies** | `main` includes PR #48 templates |
| **Expected deliverable** | Catalog docs, `org.allsparks.mimic.config`, expanded families/constructs, tests, issue drafts |
| **Expected validation** | `./gradlew check`; FakeActuator writes stay 0 |
| **Hardware required** | No |

## Stop conditions currently in effect

- **Do not merge** without human approval (`AUTOMATIC_MERGE=false`).
- **Do not enable** Phase 2–10 actuation.
- **Do not invent** robot hardware maps or season game-piece types in this library. See [library-vs-teamcode.md](../mechanism-control/library-vs-teamcode.md).
- **Do not reopen** the merged Phase 0 branch (`feature/phase-0-scaffold`). New work targets `main`.
