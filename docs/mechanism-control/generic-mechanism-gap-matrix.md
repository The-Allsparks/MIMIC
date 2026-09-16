# Generic mechanism gap matrix

Maps the [catalog](generic-mechanism-catalog.md) onto current MIMIC code and issues.

**Audited tree:** `main` after PR #48 plus this catalog slice. Do not trust older issue text when code differs.

**New catalog issues:** [#49](https://github.com/The-Allsparks/MIMIC/issues/49)–[#70](https://github.com/The-Allsparks/MIMIC/issues/70). Do not treat [#70](https://github.com/The-Allsparks/MIMIC/issues/70) or [#69](https://github.com/The-Allsparks/MIMIC/issues/69) as permission to command motors.

**Gates:** Phase 0 only. Phases 2–10 remain disabled ([phases.md](phases.md)). Active-output risk **High** means the work could command motors if implemented carelessly. This slice must stay **None**.

Columns:

- **Code** — exists / partial / docs-only / missing
- **Issue** — existing GitHub issue or new issue to file
- **Dep** — must land first
- **Output risk** — None / Low (observe) / High (actuation)
- **Hardware** — none / Hub / robot
- **Phase** — current phase document
- **Unit / sim** — feasible on desktop?

---

## A. Passive catalog and configuration (this slice; allowed)

| Capability | Code | Issue | Dep | Output risk | Hardware | Phase | Unit | Sim |
|------------|------|-------|-----|-------------|----------|-------|------|-----|
| Mechanism families as jobs | Partial: 4 families in `MechanismFamily` | [#49](https://github.com/The-Allsparks/MIMIC/issues/49) (this PR) | — | None | none | 0 | yes | n/a |
| Standard constructs | Partial: 16 constructs; turret/hood/claw mis-filed | [#49](https://github.com/The-Allsparks/MIMIC/issues/49) | families | None | none | 0 | yes | n/a |
| Custom construct without enum edit | Missing | [#49](https://github.com/The-Allsparks/MIMIC/issues/49) | families | None | none | 0 | yes | n/a |
| Actuator topology | Missing | [#49](https://github.com/The-Allsparks/MIMIC/issues/49) | — | None | none | 0 | yes | n/a |
| Sensor-role declarations | Missing (observer has fixed channels) | [#50](https://github.com/The-Allsparks/MIMIC/issues/50) | — | None | none | 0 | yes | n/a |
| Capability declarations | Missing | [#49](https://github.com/The-Allsparks/MIMIC/issues/49) | — | None | none | 0 | yes | n/a |
| Immutable `MechanismConfiguration` | Missing; `MechanismBlueprint` is id+construct only | [#49](https://github.com/The-Allsparks/MIMIC/issues/49) | topology, roles | None | none | 0 | yes | n/a |
| Configuration validation | Missing | [#51](https://github.com/The-Allsparks/MIMIC/issues/51) | configuration | None | none | 0 | yes | n/a |
| Presets as suggestions | Missing | [#49](https://github.com/The-Allsparks/MIMIC/issues/49); richer later [#65](https://github.com/The-Allsparks/MIMIC/issues/65) | configuration | None | none | 0 | yes | n/a |
| Season-name leak tests | Partial: templates test | [#49](https://github.com/The-Allsparks/MIMIC/issues/49) | catalog | None | none | 0 | yes | n/a |

## B. Observation (exists; extend later)

| Capability | Code | Issue | Dep | Output risk | Hardware | Phase | Unit | Sim |
|------------|------|-------|-----|-------------|----------|-------|------|-----|
| Immutable snapshot | Exists `MechanismSnapshot` | closed #26 | — | None | none | 0 | yes | fake HW |
| Validity enum | Exists | closed #25–#28 | — | None | none | 0 | yes | fake HW |
| Fixed pose/velocity/current/limits | Exists `MechanismObserver` | — | — | None | none | 0 | yes | fake HW |
| Named role samples on snapshot | Exists `sample` / `role` extras | [#53](https://github.com/The-Allsparks/MIMIC/issues/53) | config | None | none | 0–1 | yes | fake HW |
| Piece entry/exit/count observation | Exists `PieceObservation` | [#54](https://github.com/The-Allsparks/MIMIC/issues/54) | snapshot roles | None | optional sensors | 1 | yes | fake digital |
| Debounce / edge utilities | Missing | [#55](https://github.com/The-Allsparks/MIMIC/issues/55) | snapshot | None | none | 0–2 | yes | yes |
| Passive REV adapters | Exists supplier-based | #6 blocked on robot; #34 packaging | #34 | None | Hub | 0–1 | stubs later | n/a |
| Logger / TRACE field names | Exists CSV; not TRACE schema | #32 allocation | — | None | none | 0 | yes | n/a |

## C. Calibration, limits, control (blocked)

| Capability | Code | Issue | Dep | Output risk | Hardware | Phase | Unit | Sim |
|------------|------|-------|-----|-------------|----------|-------|------|-----|
| Calibration strategy **contracts** | `CalibrationContract` declaration-only; session UNCALIBRATED | [#56](https://github.com/The-Allsparks/MIMIC/issues/56); parent #8 | config | None if declaration-only | none | 0 docs / 2 impl | yes | later |
| Homing motion | Missing; session always UNCALIBRATED | #8, #9 | robot observation | **High** | robot | 2 | partial | later |
| Limit policy **contracts** | `LimitContract` declaration-only; session does not call it | [#57](https://github.com/The-Allsparks/MIMIC/issues/57); parent #11 | config | None if declaration-only | none | 0 docs / 3 impl | yes | later |
| Actuator safety gate | Docs only | #10; review [#69](https://github.com/The-Allsparks/MIMIC/issues/69) | calibration + limits | **High** | robot | 3 | yes gate fn | later |
| Soft limits / jog | Docs only | #11 | #10 | **High** | robot | 3 | yes | later |
| Controller adapter contracts | `MechanismControllerAdapter` / `Setpoint`; unused by session; no NextControl dep | [#58](https://github.com/The-Allsparks/MIMIC/issues/58); #12 | config | None if no output | none | 0 docs / 4 impl | yes | later |
| Profiled motion / gravity FF | Docs only | #13, #14 | #12, robot | **High** | robot | 4 | limited | #22 |
| Readiness / settling | Exists `Readiness` at-speed / in-tolerance; unused by session | [#59](https://github.com/The-Allsparks/MIMIC/issues/59) | snapshot | None if pure function | none | 1–4 | yes | yes |

## D. Multi-actuator, interlocks, faults, pieces

| Capability | Code | Issue | Dep | Output risk | Hardware | Phase | Unit | Sim |
|------------|------|-------|-----|-------------|----------|-------|------|-----|
| Topology vs sync contracts | `SyncContract` declaration-only; linked+contract rejected; unused by session | [#61](https://github.com/The-Allsparks/MIMIC/issues/61) (software) | config | None if contracts only | none | 0 docs / 5 impl | yes | later |
| Allsparks elevator CAD sync | Unknown hardware | #15 blocked | CAD | **High** if implemented wrong | robot | 5 | no | no |
| Anti-racking | Missing | #16 | #15 | **High** | robot | 5 | limited | later |
| Named-state **definitions** | `MechanismConfiguration.namedStates()` metadata; `MechanismStatus` remains health | [#52](https://github.com/The-Allsparks/MIMIC/issues/52); parent #18 | config | None if metadata | none | 0–6 | yes | n/a |
| Semantic state engine | Missing | #18 | phases 3–4, scheduler | Low–High | none/robot | 6 | yes table | n/a |
| Interlock contracts | Docs only | [#62](https://github.com/The-Allsparks/MIMIC/issues/62); parent #19 | named states | None if no output | none | 0 docs / 7 impl | yes | n/a |
| Interlock engine | Missing | #19 | #18 | **High** if it commands | none | 7 | yes | n/a |
| Ratchet lifecycle | Docs | #17 blocked hardware | #19 | **High** | robot | 7 | limited | later |
| Fault / degraded policy | Docs severities; session DEGRADED on invalid | [#63](https://github.com/The-Allsparks/MIMIC/issues/63); parent #20 | config degraded map | None if policy only | none | 0 docs / 8 impl | yes | fake |
| Fault recovery that moves | Missing | #20 | #10 | **High** | robot | 8 | partial | later |
| Piece tracker | Missing | [#64](https://github.com/The-Allsparks/MIMIC/issues/64) | piece observe | None if observe-only | sensors | 1+ | yes | yes |
| Jam / stall detect | Observe-only `StallDetector` / `JamDetector`; unused by session | [#60](https://github.com/The-Allsparks/MIMIC/issues/60) | current+velocity+timeout | None (no reverse-clear) | none | 1 then 8 | yes | fake |
| Richer presets | This PR suggestions only | [#65](https://github.com/The-Allsparks/MIMIC/issues/65) | this PR | None | none | 0 | yes | n/a |

## E. Integration, sim, active enablement

| Capability | Code | Issue | Dep | Output risk | Hardware | Phase | Unit | Sim |
|------------|------|-------|-----|-------------|----------|-------|------|-----|
| AMPER request/grant types | Exists inert | #21 | Phase 9 review | None now; High if grant drives output | none | 9 | yes | n/a |
| `isAnyActuationEnabled` too broad | Exists; flags 6/10 as actuation | #33 | Phase 6/10 | None | none | 0 | yes | n/a |
| FTC modules `mimic-core` / `mimic-ftc` | Single Java library | #34 P0 packaging; child pointer [#67](https://github.com/The-Allsparks/MIMIC/issues/67) | policy | None | CI SDK | 0 | SDK job | n/a |
| Simulation models | Fake actuators only | #22; [#66](https://github.com/The-Allsparks/MIMIC/issues/66) | observation data | None if not connected to output | none | 10 | yes | that is the work |
| Hardware acceptance suites | [testing.md](testing.md) cards | [#68](https://github.com/The-Allsparks/MIMIC/issues/68) | #6, robot | Low (procedure) | robot | 1+ | no | n/a |
| Active-control safety review | Flag default false | [#69](https://github.com/The-Allsparks/MIMIC/issues/69) | #10 + robot graphs | **High** | robot | 3+ | n/a | n/a |
| Per-family active implementations | Missing | [#70](https://github.com/The-Allsparks/MIMIC/issues/70) after the gate | active-control gate | **High** | robot | 4–8 | limited | later |
| SystemCore | Boundary type only | #23 blocked | vendor docs | n/a | future HW | n/a | n/a | n/a |

---

## Conflicts with the desired model

| Current type | Conflict | Resolution in this slice |
|--------------|----------|--------------------------|
| `MechanismFamily` four values | Missing arm, end effector, climber, field element, passive | Expand enum |
| `TURRET`/`HOOD` under Launcher | Aim is rotary positioning | Reclassify to Arm |
| `CLAW` under Intake | Grip is end effector | Reclassify |
| `MechanismBlueprint` | No topology, sensors, capabilities | Keep; add `MechanismConfiguration` |
| `MechanismObserver` fixed channels | Not role-based | Keep fixed fields; extras via `sample` / `role` ([#53](https://github.com/The-Allsparks/MIMIC/issues/53)) |
| `MimicSession.requestGoal` | Always `NO_ACTIVE_CONTROL` | Keep |
| `isAnyActuationEnabled` | Metadata must not require Phase 6/10 flags | Do not set those flags; #33 later |
| Ledger SHA / selected issue | Stale vs merged #28 and #48 | Update [priority-ledger.md](../audits/priority-ledger.md) |

## Must remain blocked

Homing that moves, limit gates that write, profiled control, sync correction, interlocks that command, fault recovery motion, AMPER-applied output, simulation that drives actuators, and any Phase 2–10 feature flag defaulting on.

## FTC SDK CI

No SDK integration job exists. Tracked as [#34](https://github.com/The-Allsparks/MIMIC/issues/34). Do not add an unreviewed SDK dependency to the core module.
