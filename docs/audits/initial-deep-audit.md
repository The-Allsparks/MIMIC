# MIMIC initial deep audit

| Field | Value |
|-------|--------|
| **Date of audit** | 2026-08-17 |
| **Audited commit SHA** | `5847806f094f846cb3e8a4adf7ad0b355c4034fb` |
| **Branch audited** | `feature/phase-0-scaffold` |
| **Repository** | [The-Allsparks/MIMIC](https://github.com/The-Allsparks/MIMIC) |
| **Default branch** | `main` (`bb2ec29` — public-library establishment only) |
| **Auditor identity** | `TA-C-GHill` |
| **Open implementation PR** | Draft [PR #1](https://github.com/The-Allsparks/MIMIC/pull/1) (CI green on Ubuntu, Windows, and docs-structure) |

This audit examines the Phase 0 scaffold against the project’s stated purpose, architecture, safety model, and existing GitHub backlog (issues #2–#23). Findings cite code, tests, or documentation. Speculative defects without evidence are omitted.

---

## Executive summary

MIMIC is an early-stage **passive observation** library. The implementation on `feature/phase-0-scaffold` matches the documented Phase 0 contract: interfaces, immutable snapshots, units, fake hardware, supplier-wired REV adapters, logging, and feature flags that refuse actuation. **No motor or servo output path exists.** That is the correct maturity for this project.

`main` does not yet contain that scaffold. Continued development must treat draft PR #1 as the current implementation stream and must not start a competing Phase 0 rewrite.

The highest-value remaining work is **not** later-phase control. It is:

1. Correct Phase 0 measurement invariants that are documented but wrong or untested (stale classification — #25 records this as observer liveness with tests; Hub-age remains unsolved).
2. Finish repository hygiene so Phase 0 issues close when PR #1 merges.
3. Keep later phases disabled until robot observation exists.

There are **no safety blockers** in the current code: it cannot command hardware. Finding C1 (`MechanismObserver` stale detection) is recorded as **observer liveness** with unit tests on this branch (#25). Hub sample-age is still impossible from `DoubleSupplier`. Enabling later phases that treat `STALE` as Hub health would still be unsafe.

---

## Project purpose

MIMIC is **Mechanism Integration, Motion, Interlocks, and Calibration for FTC**. It is a lifecycle, safety, and integration layer for elevators, arms, extensions, intakes, turrets, servos, and coupled mechanisms.

Intended users: beginning students (observe first), advanced students (calibration and limits), mentors (safety review), and downstream Allsparks libraries (TRACE sink later; AMPER grant later).

Explicit responsibilities:

- Know whether a mechanism pose is trusted.
- Observe sensors without writing actuators (Phase 0/1).
- Later: calibrate, limit, profile, synchronize, interlock, recover, and request power from AMPER.

Explicit non-responsibilities:

| Question | Owner |
|----------|--------|
| Chassis path | Pedro Pathing |
| Field perception | ViDAR |
| Electrical allocation | AMPER |
| High-level scoring tasks | Future HELM / behavior layer |
| Global command scheduling | Ivy, NextFTC, or team scheduler |
| Feedback math | Optional adapter (not a compile dependency) |

---

## Current maturity

| Item | Status |
|------|--------|
| Version | `0.1.0-SNAPSHOT` |
| Implemented phase | Phase 0 contracts on `feature/phase-0-scaffold` only |
| Production safety claims | None — correctly refused |
| Hardware validation | None. Desktop unit tests only |
| Elevator hardware | Not selected ([elevator-target.md](../mechanism-control/elevator-target.md)) |
| Maven / FTC SDK publish | Not started; TeamCode copy is the documented integration path |
| Releases | None |

Readiness gate from the project itself: **MIMIC must measure and validate before coordinating active mechanisms.** That gate is unmet. Phases 2–10 must remain blocked.

---

## Implemented capabilities

Verified in source and tests at the audited SHA:

- `MimicSession` observes, logs, rejects goals with `NO_ACTIVE_CONTROL`, and logs `stop()` without writing hardware (`MimicSessionTest`).
- Construction throws if any actuation-classified flag is true.
- `MechanismObserver` captures position, velocity, finite-difference acceleration, effort, current, limits, optional absolute/redundant sensors, disagreement, and loop duration.
- Missing/NaN/throwing suppliers become `MISSING` or `UNSUPPORTED`; values are not invented.
- `MechanismUnits` rejects non-positive ticks-per-unit; direction invert is tested.
- Fake hardware write counters stay at 0 through observe/periodic/stop/requestGoal.
- REV adapters are supplier-wired; desktop build has no FTC SDK dependency.
- AMPER request/grant types are inert; unrestricted grant reason is `FEATURE_DISABLED`.
- `SystemCoreAdapterBoundary` remains unimplemented.
- Documentation set, CI (`./gradlew check` + docs-structure), Dependabot, MIT license, CoC, SECURITY, contributing guide, issue/PR templates.

---

## Documented but unimplemented capabilities

| Claim | Evidence | Notes |
|-------|----------|--------|
| Stale measurements degrade cleanly | [phases.md](../mechanism-control/phases.md) Phase 0 acceptance; `MeasurementValidity.STALE` exists | Recorded as observer liveness in #25. Hub sample-age is still impossible from `DoubleSupplier`. See finding C1. |
| Phase 1 richer telemetry when `phase1PassiveObservation` is true | `MimicSession` class Javadoc | Flag is stored; `observe()` never reads it. See finding U1. |
| `LIMIT_ASSERTED` log events | `MimicEventType` | Never emitted. Informational for Phase 0. |
| CSV export for TRACE | `MimicEventLogger` Javadoc | Packed `k=v` field column, not a TRACE schema. Acceptable foundation. |
| Phases 2–10 architecture objects | [architecture.md](../mechanism-control/architecture.md) | Intentionally absent. Must stay absent. |

---

## Architecture findings

### A1 — Snapshot drops position and velocity validity — MEDIUM / ARCHITECTURE

`MechanismObserver.capture()` builds `SensorSample` objects for position and velocity, then stores only the `double` values on `MechanismSnapshot`. Downstream code cannot distinguish `STALE`, `MISSING`, and `UNSUPPORTED` for those channels without reconstructing from `sensorValid()` and `NaN`.

`SnapshotValidator.classifyPosition` guesses: disagreement → `DISAGREEING`, else `MISSING`. It cannot report `STALE`.

**Evidence:** `MechanismObserver.java` capture assembly; `MechanismSnapshot` fields; `SnapshotValidator.java`.

**Why it matters:** Phase 1 telemetry and Phase 2 calibration invalidation need per-channel validity. Fixing this now is a seam; delaying it forces a snapshot schema change later.

### A2 — `isAnyActuationEnabled()` over-approximates — LOW / ARCHITECTURE

Flags for Phase 6 (semantic states), Phase 8 (faults), Phase 9 (AMPER types), and Phase 10 (simulation) are treated as actuation. That is conservative and currently useful because `MimicSession` refuses those flags. It will block legitimate passive simulation and state machines later.

**Evidence:** `MimicFeatureFlags.isAnyActuationEnabled()`.

### A3 — Phase 0 contracts flag is unused — LOW / ARCHITECTURE

`phase0Contracts` defaults true and is never consulted by `MimicSession`. Turning it off does not disable observation.

### A4 — No god object, no hardware writes, dependency direction is sound — INFORMATIONAL / ARCHITECTURE

`MimicSession` is the largest type and remains an observer/logger. REV adapters depend on suppliers, not the FTC SDK. AMPER types have no compile dependency on AMPER. TRACE is a documented field-name intention only. No circular Allsparks compile dependencies.

---

## Correctness findings

### C1 — Stale detection measures loop period, not sensor age — HIGH / CORRECTNESS

`MechanismObserver.freshness()` does:

```text
age = now - lastCaptureNanos
if (age > staleAfterNanos) return STALE
```

Suppliers are read at capture time. `age` is therefore the time since the previous `capture()` call (loop period), not the age of a hardware sample.

Consequences:

- A 20 ms robot loop with `staleAfterNanos = 50_000_000` never marks samples stale, even if the Hub is frozen and the supplier returns the same cached number.
- A paused OpMode or a loop slower than the threshold marks **every** subsequent live sample stale.

**Status (#25, local on PR #1):** The formula is **observer liveness**, documented as such (class / `staleAfterNanos` / `freshness()` Javadoc; `phases.md`; glossary). Unit tests in `MechanismObserverTest` lock first-capture, `<= 0` off, gap above/equal/within threshold, recovery, and frozen-supplier-on-fast-loop remains `VALID`. **Hub sample-age is not solved** and cannot be solved from `DoubleSupplier` alone. Later disable-on-stale must not treat `STALE` as Hub health.

**Evidence:** `MechanismObserver.java` `freshness()`; `MechanismObserverTest` liveness cases; `phases.md` Phase 0 acceptance.

This is fail-safe in one direction (false stale) and fail-dangerous later if callers assume Hub age (never stale on a frozen cache). It does not command hardware today.

### C2 — `sensorValid` requires usable velocity — MEDIUM / CORRECTNESS

```text
sensorValid = position.isUsable() && velocity.isUsable() && !disagreeing
```

A position-only mechanism (potentiometer, analog absolute encoder, some servos) is always invalid. That is conservative, but it makes Phase 0 observation of analog-only mechanisms look failed.

**Evidence:** `MechanismObserver.capture()`; `RevAnalogSensorObserver` exists for exactly this hardware.

**Status (#27):** `sensorValid` is usable primary pose, `UNSUPPORTED` or usable velocity, and no disagreement. Omitted `ticksPerSecond` no longer clears the flag. Omitted `ticks` still does. Analog-only observation wires the mapped analog value as `ticks`; `absoluteSensor` is not a substitute primary pose. `classifyPosition` reports `DISAGREEING` for position-only redundant offset; wired `MISSING`/`STALE` velocity is not treated as disagreement.

### C3 — Missing limit switches log as not asserted — MEDIUM / CORRECTNESS

`LimitSwitchSample.missing()` sets `asserted = false`. `MimicEventLogger.recordObservation` logs `Boolean.toString(snapshot.lowerLimit().asserted())` without validity. A disconnected switch is exported as `lower=false`.

The safety model notes that a disconnected **normally-closed** switch can look asserted at the electrical level. The exception/disconnect path used by fakes is different: it reports missing and not asserted. That is reasonable for throws, but the CSV will hide missing as “not at limit.”

**Evidence:** `LimitSwitchSample.missing()`; `MimicEventLogger.recordObservation`; [safety-model.md](../mechanism-control/safety-model.md).

### C4 — `requestGoal` timestamp is 0 before first observe — LOW / CORRECTNESS

Harmless in Phase 0. Worth a named reason or clock read later.

### C5 — Finite-difference acceleration has no bound — LOW / CORRECTNESS

First sample is `NaN` (correct). A huge `dt` or a velocity jump produces a huge acceleration with no validity flag. Acceptable for Phase 0 graphs; not a control input yet.

---

## Safety findings

### S1 — Passive modes cannot command hardware — INFORMATIONAL / SAFETY

`FakeActuator.setPower` / `setPosition` are never called from library code. `MimicSession.stop()` only logs. Session construction rejects actuation flags. CONTRIBUTING and the PR template forbid enabling output without review.

Grep of `src/main` shows no `setPower`, `setVelocity`, or servo writes.

### S2 — Replay / simulation cannot produce physical outputs — INFORMATIONAL / SAFETY

No replay engine. Fake hardware is in-memory. Phase 10 is a flag only.

### S3 — Later-phase enablement is a student hazard, not a present bypass — HIGH / SAFETY (process)

Feature-flag builders can turn on Phase 2–10 in application code, but `MimicSession` throws rather than actuating. The residual risk is a future PR that removes the throw and writes motors. Process controls (PR template, CONTRIBUTING, draft PR #1) exist. Branch protection on `main` does **not** exist (finding R1).

### S4 — Gravity / ratchet / OpMode-stop physical safety is not yet in software — INFORMATIONAL / SAFETY

Correctly deferred. Phase 0 `stop()` does not hold a gravity load; FTC Hub OpMode-stop already removes power. Documentation states software cannot replace mechanical holding.

---

## Performance findings

All items below are **predicted**, not measured on a Control Hub.

### P1 — Per-loop allocation in the logger — MEDIUM / PERFORMANCE

Every `observe()` allocates a `LinkedHashMap`, boxed strings, and a `MimicEvent`. Default capacity is 2048. At 50 Hz that is 40 s of samples then `ArrayList.remove(0)` on every additional event (`O(n)` per overflow).

**Evidence:** `MimicEventLogger.record` / `recordObservation`; `MimicSession.create` uses capacity 2048.

Create a benchmark issue; do not “optimize” without measurements.

### P2 — Current polling cost is documented, not measured — MEDIUM / PERFORMANCE

[phase-0-plan.md](../mechanism-control/phase-0-plan.md) lists Hub current-sample cost as a next hardware task. Issue #6 covers it. Blocked on a robot.

### P3 — Loop overhead stats exist only in unit tests — INFORMATIONAL / PERFORMANCE

`LoopOverheadStats` is exercised with a fake clock (`MimicSessionTest.loopOverheadIsMeasured`). Desktop numbers are not Control Hub numbers.

---

## API / usability findings

### U1 — Phase 1 flag is a no-op — MEDIUM / USABILITY

`MimicFeatureFlags.passiveObservation()` and `MimicSession` Javadoc promise richer Phase 1 logging. Behavior is identical to Phase 0. Students who enable the flag will see no change.

Related to existing issue #6; do not duplicate a second Phase 1 telemetry epic.

### U2 — First-use path is honest but not copy-paste OpMode-complete — LOW / USABILITY

README quick start is `gradlew test`. Examples are sketches without `hardwareMap`. That is correct for a desktop `java-library`. A later student OpMode sample in TeamCode is out of scope until hardware exists.

### U3 — Defaults are safe — INFORMATIONAL / USABILITY

`MimicFeatureFlags.defaults()` is Phase 0 only. `disagreementThreshold` defaults to `+Infinity` (disagreement off). `staleAfterNanos` defaults to 0 (freshness check off). Safe; teams must opt into liveness checks. C1 is documented as observer liveness, not Hub sample-age.

---

## Testing findings

### T1 — No test for `STALE` — HIGH / TESTING

Covered: units, direction, missing/NaN, disconnected limits, disagreement, observer-liveness `STALE` / `staleAfterNanos` (#25), goal rejection, actuation-flag throw, zero writes, AMPER unrestricted grant, doc links, SystemCore placeholder.

**Status (#25, local on PR #1):** `MechanismObserverTest` now asserts `STALE` on `absoluteSensor()`, not via `SnapshotValidator.classifyPosition` (A1 / #26 still cannot report `STALE` on primary pose). Remaining missing: OUT_OF_RANGE (validator only), Phase 1 flag behavior, logger overflow/drop count, analog observer, `RevMotorObserver.create` wiring.

`SnapshotValidator.classifyPosition` OUT_OF_RANGE is untested.

### T2 — Tests assert behavior, not only internals — INFORMATIONAL / TESTING

`MimicSessionTest` asserts write counts, goal disposition, and snapshot values. Good.

### T3 — No Android / FTC SDK compile job — MEDIUM / TESTING

By design for Phase 0. Compatibility risk is real once TeamCode copies the library. Track as a later CI issue; do not add the SDK to the desktop classpath now.

### T4 — CI is green for PR #1 — INFORMATIONAL / TESTING

Run `32051590192`: test (ubuntu), test (windows), docs-structure all success. CI runs on PRs to `main` and pushes to `main` only.

---

## Documentation findings

### D1 — Documentation matches Phase 0 intent — INFORMATIONAL / DOCUMENTATION

Research, build-vs-adopt, architecture, phases, safety model, and elevator unknowns are unusually complete for a 0.1 scaffold. Maturity labels are honest.

### D2 — `MimicSession` Javadoc overclaims Phase 1 — MEDIUM / DOCUMENTATION

See U1. Javadoc should match code until Phase 1 lands.

### D3 — Phase 0 issues #2–#5 and #7 are implemented in PR #1 but still open with unchecked boxes — MEDIUM / DOCUMENTATION

Backlog drift. Issues were filed after the scaffold and describe work already on the branch. They should be linked from PR #1 and closed on merge, not re-implemented.

### D4 — No `docs/audits/` existed before this document — INFORMATIONAL / DOCUMENTATION

This file is the first audit record.

---

## Dependency findings

### Dep1 — GitHub Actions tagged, not SHA-pinned — MEDIUM / SECURITY

`.github/workflows/ci.yml` uses `actions/checkout@v4` and `actions/setup-java@v4`. Dependabot updates `github-actions` monthly. Pinning to SHAs is the stronger supply-chain posture used by many FTC org templates.

### Dep2 — Gradle wrapper 8.7, JUnit 5.10.2 BOM, Java 11 source — INFORMATIONAL / SECURITY

No third-party runtime dependencies. MIT license. No secrets in repo. `gradle-wrapper.properties` uses the official distribution URL with `validateDistributionUrl=true`.

### Dep3 — NextControl / YAMS / WPILib correctly absent — INFORMATIONAL / COMPATIBILITY

CONTRIBUTING forbids adding them without license review. `build.gradle` has only JUnit.

---

## Repository-health findings

### R1 — `main` has no branch protection or rulesets — HIGH / SECURITY

`gh api .../branches/main/protection` → 404. Rulesets empty. Admin permission is available on this identity, but enabling protection is a maintainer policy choice (required checks, required reviews, no force-push). **Do not enable unilaterally without documenting the reviewer count.** Automatic merge is **not** authorized.

### R2 — Draft PR #1 is the entire product and is not linked to issues #2–#7 — MEDIUM / INTEGRATION

CI green. `risks.md` says the draft should remain draft until maintainers accept Phase 0. `AUTOMATIC_MERGE=false`. Human review is required before merge.

### R3 — Labels and milestones exist; no GitHub Project visible — LOW / DOCUMENTATION

Milestones Phase 0–10 and SystemCore exist. Token lacks `read:project`. Use this audit plus `priority-ledger.md` until a project board is authorized.

### R4 — Existing issues use the phase-work template, not the orchestrator template — INFORMATIONAL / DOCUMENTATION

Issues #2–#23 are actionable enough. New audit-driven issues use the fuller structure. Do not mass-rewrite old issues.

### R5 — No releases, changelog is Unreleased, version 0.1.0-SNAPSHOT — INFORMATIONAL

Correct.

---

## Cross-project integration findings

| Project | Boundary in MIMIC | Leakage? |
|---------|-------------------|----------|
| ViDAR | Docs only; future interlock **input** | No |
| Pedro Pathing | Docs only | No |
| AMPER | Inert request/grant types; AMPER must not write motors | No compile dependency |
| BEACON / TRACE / HELM | TRACE-compatible log field names only | No |
| Robot application | Team owns `setPower` in Phase 0 | Correct |
| NextControl | Explicitly not a Gradle dependency | Correct |

Normal conceptual direction `ViDAR/Pedro/AMPER/MIMIC/BEACON → TRACE → HELM` is respected. Do not add compile-time edges.

---

## Readiness assessment

| Gate | Met? |
|------|------|
| Phase 0 desktop contracts | **Yes**, on PR #1; C1 recorded as observer liveness (#25, CI pending) |
| Phase 0 robot observation | **No** (issue #6, hardware) |
| Phase 1 richer telemetry | **No** (flag no-op) |
| Phase 2+ active control | **No — blocked** |
| Production safety claim | **Forbidden** |
| Merge of PR #1 | **Human approval required** |

MIMIC is ready to be a **teaching and observation scaffold** after PR #1 is accepted and C1 is fixed. It is not ready to coordinate mechanisms.

---

## Recommended work order

1. Record this audit and a priority ledger in-repo.
2. Link Phase 0 issues to PR #1; do not re-implement #2–#5, #7.
3. **Fix stale classification and add tests** (C1 + T1) on the current Phase 0 branch — implemented locally as observer liveness (#25); Hub-age remains unsolved.
4. Preserve per-channel position/velocity validity on snapshots (A1) — small schema seam.
5. Align Phase 1 Javadoc/flag (U1/D2) or implement the first Phase 1 logging slice without hardware.
6. Pin Actions SHAs (Dep1).
7. Maintainer decision: branch protection (R1) and merge of PR #1.
8. Hardware: issue #6 (blocked).
9. Only then consider Phase 2 design review — still no actuation.

---

## Deferred or rejected ideas

| Idea | Decision |
|------|----------|
| Implement homing, limits, PID, AMPER clipping | **Rejected now.** Readiness gate unmet. |
| Add FTC SDK to desktop CI | **Deferred.** Would break the no-SDK design. |
| Elevator-specific controller | **Rejected** until hardware is selected. |
| NextControl compile dependency | **Rejected** (GPL-3.0). |
| SystemCore adapters | **Blocked** on primary docs (issue #23). |
| Logger ring-buffer rewrite | **Deferred** until P1 is measured. |
| Enable branch protection without reviewer policy | **Deferred** to human decision. |
| Merge PR #1 from this orchestrator | **Rejected.** `AUTOMATIC_MERGE` is not authorized. |

---

## Finding index

| ID | Severity | Type | Ready? | Track as |
|----|----------|------|--------|----------|
| C1 | HIGH | CORRECTNESS | Implemented locally as liveness (#25); Hub-age unsolved | [#25](https://github.com/The-Allsparks/MIMIC/issues/25) |
| T1 | HIGH | TESTING | Implemented locally (#25); A1 still blocks `classifyPosition` STALE | Same as #25 |
| R1 | HIGH | SECURITY | Human decision | [#31](https://github.com/The-Allsparks/MIMIC/issues/31) |
| S3 | HIGH | SAFETY | Process | #31 + CONTRIBUTING |
| A1 | MEDIUM | ARCHITECTURE | Yes after C1 or parallel | [#26](https://github.com/The-Allsparks/MIMIC/issues/26) |
| C2 | MEDIUM | CORRECTNESS | Implemented locally (#27) | [#27](https://github.com/The-Allsparks/MIMIC/issues/27) |
| C3 | MEDIUM | CORRECTNESS | Yes | [#28](https://github.com/The-Allsparks/MIMIC/issues/28) |
| U1 / D2 | MEDIUM | USABILITY | Yes | [#29](https://github.com/The-Allsparks/MIMIC/issues/29); full telemetry #6 |
| P1 | MEDIUM | PERFORMANCE | Research | [#32](https://github.com/The-Allsparks/MIMIC/issues/32) |
| P2 | MEDIUM | PERFORMANCE | Blocked (hardware) | Existing #6 |
| Dep1 | MEDIUM | SECURITY | Yes | [#30](https://github.com/The-Allsparks/MIMIC/issues/30) |
| T3 | MEDIUM | TESTING | Later | [#34](https://github.com/The-Allsparks/MIMIC/issues/34) |
| D3 | MEDIUM | DOCUMENTATION | Yes (comments) | PR #1 linking |
| A2 | LOW | ARCHITECTURE | Later | [#33](https://github.com/The-Allsparks/MIMIC/issues/33) |
| A3 | LOW | ARCHITECTURE | Later | Fold into #33 |
| C4 | LOW | CORRECTNESS | Later | Optional |
| C5 | LOW | CORRECTNESS | Later | Optional |
| U2 | LOW | USABILITY | Blocked (hardware) | Existing #6 |
| R3 | LOW | DOCUMENTATION | Token | Informational |
| S1 S2 S4 A4 T2 T4 D1 D4 Dep2 Dep3 R4 R5 | INFORMATIONAL | — | — | No issue |

---

## Evidence and references

- Source tree under `src/main/java/org/allsparks/mimic/`
- Tests under `src/test/java/org/allsparks/mimic/`
- Docs under `docs/mechanism-control/`
- CI: `.github/workflows/ci.yml`; Actions run `32051590192`
- GitHub: issues #2–#23, milestones 1–12, draft PR #1
- [phases.md](../mechanism-control/phases.md), [architecture.md](../mechanism-control/architecture.md), [safety-model.md](../mechanism-control/safety-model.md), [phase-0-plan.md](../mechanism-control/phase-0-plan.md)
