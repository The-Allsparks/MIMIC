# Phases

Every phase must be feature-flagged where applicable, independently testable, observable, reversible, fail-safe when measurements are missing, and **disabled by default** until acceptance tests pass.

Implemented in this repository: **Phase 0 only.** Later phases are documented, not enabled.

---

## Phase 0 — Research, contracts, measurement validation

**Teach:** What information does safe mechanism control require?

**Implement:** interfaces, immutable snapshots, units/direction, clocks, passive REV adapters, fake hardware, validation, logging.

**Acceptance**

- [x] No motor or servo output changes occur (`FakeActuator` write counts stay 0).
- [x] Sensor units and directions are documented (`MechanismUnits`).
- [x] Stale or unsupported measurements degrade cleanly. Phase 0 `STALE` is observer liveness (gap since the previous `capture()` start), not Hub sample age. Frozen supplier values on a timely loop stay `VALID`. `staleAfterNanos <= 0` disables the check (default).
- [x] Loop overhead is measured (`LoopOverheadStats`).
- [x] Build-versus-adopt assessment is complete.

**Enable:** `MimicFeatureFlags.defaults()` (Phase 0 on).

---

## Phase 1 — Passive mechanism observation

**Teach:** How does software know what the mechanism is doing?

No active control. Add richer telemetry, disagreement metrics, timeline logging, graphical Driver Station fields.

**Acceptance (not claimed yet)**

- Recorded values correlate with physical motion (needs robot).
- Telemetry does not damage loop performance (needs measurement).
- Mechanism behavior remains unchanged.
- Invalid sensors demonstrable with fake hardware (partially covered in Phase 0 tests).

Flag: `phase1PassiveObservation` — exists, does not actuate.

---

## Phase 2 — Calibration and homing

**Teach:** Why do encoder counts not automatically identify a physical position?

States: `UNCALIBRATED`, `HOMING`, `CALIBRATED`, `CALIBRATION_SUSPECT`, `FAULTED`.

Homing must declare: direction, max output, max travel, timeout, completion, debounce, encoder reset, failure, recovery.

**Do not implement until Phase 0 robot observation exists and this document is reviewed.**

---

## Phase 3 — Limits and safe manual control

**Teach:** What is the difference between a target, a soft limit, and a hard limit?

Hard/soft limits, direction-aware enforcement, stopping-distance margins, output clamps, restricted uncalibrated motion, jog mode, timeouts, **actuator safety gate**.

Override levels: (1) normal manual, (2) restricted recovery, (3) conspicuous pit-only expert. Override must not silently disable every safety feature.

---

## Phase 4 — Profiled mechanism control

**Teach:** Why should a mechanism follow a moving setpoint instead of jumping to the destination?

Controller adapters (NextControl optional, license-aware; minimal internal controller for tests). Position/velocity goals, trapezoids, FF, gravity, saturation, anti-windup, settling, timeout.

---

## Phase 5 — Multi-actuator synchronization

**Teach:** When should motors follow each other, and when should they be independently controlled?

Do not apply independent side correction to a common shaft unless analysis says so.

---

## Phase 6 — Semantic mechanism states

**Teach:** What is the difference between “move to 500 mm” and “enter the scoring state”?

States request goals. Use the selected scheduler. Do not create a competing global scheduler.

---

## Phase 7 — Cross-mechanism interlocks

**Teach:** How can two individually safe mechanisms become unsafe together?

Named, testable constraints. Outcomes: reject, defer, clamp, intermediate goals, driver confirmation. No deadlock loops.

---

## Phase 8 — Fault detection and bounded recovery

**Teach:** When should the robot retry, continue slowly, or stop?

Severities: `INFO`, `DEGRADED`, `STOP_MECHANISM`, `STOP_DEPENDENCIES`, `STOP_ROBOT`.

---

## Phase 9 — AMPER integration

**Teach:** How do safe mechanism motion and available electrical power interact?

MIMIC owns safety. AMPER owns allocation. AMPER must not command hardware. See [amper-integration.md](amper-integration.md).

---

## Phase 10 — Characterization and simulation

**Teach:** How can measurement replace guess-and-check tuning?

Do not initially recreate all of WPILib SysId.

---

## Educational checklist (every phase)

Document: problem, observes, controls, cannot solve, why it works, what to graph, enable, disable, incorrect behavior, safety precautions, evidence to advance.
