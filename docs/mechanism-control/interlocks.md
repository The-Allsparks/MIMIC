# Interlocks

**Contract now. Engine that changes output: Phase 7 / [#19](https://github.com/The-Allsparks/MIMIC/issues/19) — not implemented.**

## Problem

Two mechanisms can each be inside their own soft limits and still collide, unspool a cable, or feed a launcher that is not ready.

Cross-mechanism rules such as “feeder only when launcher ready” or “intake may run only when deployed” are named, testable contracts. They are not a robot-wide scheduler.

## `InterlockRule` (contract)

Sketch:

```text
InterlockRule.when("feeder").requires("launcher", READY).onFail(REJECT)
InterlockRule.when("intake").requires("deployer", "DEPLOYED").onFail(REJECT)
```

| Field | Meaning |
|-------|---------|
| `when` | Source mechanism whose requested goal is gated |
| `requires` | Other mechanism plus a named state or `READY` |
| `onFail` | Explicit outcome: `REJECT`, `DEFER`, `CLAMP`, `INTERMEDIATE`, `CONFIRM` |
| `name` | Student-facing id; default is `source-requires-other-state` |
| `sources()` | The source id and the required other id |

`READY` may be satisfied by a `namedStates` value `READY` or by a `Readiness.ready()` bit copied into `InterlockInputs`. Evaluation does not feed `Readiness`, does not spin a launcher, and does not write motors.

`MimicSession` does not register rules. `permitsMotion()` is always false. Do not create `Command`, `Subsystem`, `Scheduler`, or `InterlockManager` types here. SHIFT semantic intents and HELM orchestration stay outside MIMIC.

## Evaluation tables

`InterlockRule.evaluate(InterlockInputs)` returns `GoalResult` with an existing `GoalDisposition`. Missing evidence is not a pass.

| `onFail` | `GoalDisposition` | Reason |
|----------|-------------------|--------|
| (requirement met) | `ACCEPTED` | `INTERLOCK_SATISFIED` |
| `REJECT` | `REJECTED` | `INTERLOCK_REJECTED` |
| `DEFER` | `DEFERRED` | `INTERLOCK_DEFERRED` |
| `CLAMP` | `CLAMPED` | `INTERLOCK_CLAMPED` |
| `INTERMEDIATE` | `REPLACED` | `INTERLOCK_INTERMEDIATE` |
| `CONFIRM` | `DEFERRED` | `INTERLOCK_CONFIRM_REQUIRED` |

`CONFIRM` is not a new `GoalDisposition`. It names “wait for an explicit confirmation” without a driver-prompt scheduler.

`CLAMP` requires `clampTo(...)`. `INTERMEDIATE` requires `intermediate(...)`. Those strings are declared targets, not motor commands.

## Deadlock loops

Generated intermediates must not cycle. An intermediate must not equal the source mechanism. Two `INTERMEDIATE` rules must not replace each other’s source. `InterlockRule.generatedIntermediatesCycle` reports that data-model hazard. It does not schedule either goal. The engine that would command intermediates is [#19](https://github.com/The-Allsparks/MIMIC/issues/19).

## Examples (illustrative, not a robot CAD)

- intake must not run while the deployer is `STOWED`
- feeder motion requires launcher `READY`
- hood motion requires turret not slewing through a blocked zone
- turret slew requires hood inside a safe band
- an extension axis requires turret stowed or a measured clearance

Named season constraints belong in TeamCode. Core types use generic names such as feeder, launcher, intake, and deployed. See [library-vs-teamcode.md](library-vs-teamcode.md).

## Non-goals

Interlocks do not schedule the match. They only permit or reshape mechanism goals. Parent [#19](https://github.com/The-Allsparks/MIMIC/issues/19) is the engine that would later command hardware.
