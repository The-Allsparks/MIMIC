# Changelog

All notable changes to MIMIC will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Optional `MimicEventSink` on `MimicSession` (NOOP default). TRACE adapters implement it; MIMIC does not import TRACE. The in-memory event ring stays.
- Contracts input SPI: `MimicSignals`, `InputValuesReads`, and `MechanismObserver.Builder.declareInputs` / `readFrom` / `physicalDoubles` / `physicalBooleans`. MIMIC declares encoder, limit, and extra `SignalKey`s and can observe a published snapshot. It does **not** depend on `org.allsparks.pulse`. Standalone suppliers still call hardware when no registrar is supplied. MIMIC's observer-liveness `STALE` is unchanged. Requested/applied effort stay last-command values, not sampled inputs. Motor current is declared `OPTIONAL`.
- Phase 0 observation sketches for the remaining catalog families: Arm, End effector, Climber, Field element, and Passive. Declaration and `observe()` only. Climber sketch forbids auto-release and homing under load. No Phase 2–10 flags.
- Observe-only `PieceTracker.capacity(3).identitySlot(0, teamProvidedLabel)`: occupancy, TeamCode identity strings, count, capacity, confidence, and entry-vs-count reconcile on top of `PieceObservation.from(snapshot)`. Unknown is first-class: zero sensors stay unknown occupancy, not empty, and disagreement does not invent a count. Unused by `MimicSession`; no auto-spit; FakeActuator writes stay 0 ([#64](https://github.com/The-Allsparks/MIMIC/issues/64)).
- Richer `PresetSuggestion` optional-role lists and catalog hazard notes (`StandardPresets.suggestionFor(ELEVATOR).optionalRoles()`). Example sensors stay the minimum valid desktop example; optional roles are extras a team can add or delete and are not copied into `exampleConfiguration`. Listing an optional role does not enable a capability or `MULTI_ACTUATOR_SYNCHRONIZATION`. FakeActuator writes stay 0 ([#65](https://github.com/The-Allsparks/MIMIC/issues/65)).
- Policy table `FaultPolicy.severity(FaultKind, DegradedBehavior)` onto documented severities. Each catalog kind has a default. Latch unknown does not auto-release. Unused by `MimicSession`; `status()` stays `DEGRADED` on invalid snapshot only; Phase 8 flags stay off; FakeActuator writes stay 0. Recovery motion remains [#20](https://github.com/The-Allsparks/MIMIC/issues/20); AMPER grant output remains [#21](https://github.com/The-Allsparks/MIMIC/issues/21) ([#63](https://github.com/The-Allsparks/MIMIC/issues/63)).
- Named `InterlockRule` contracts (`when("feeder").requires("launcher", READY).onFail(REJECT)`) with table evaluation onto existing `GoalDisposition`. Outcomes reject / defer / clamp / intermediate / confirm; confirm maps to `DEFERRED` + `INTERLOCK_CONFIRM_REQUIRED`. Generated intermediates must not cycle. Unused by `MimicSession`; no Command / Subsystem / Scheduler / `InterlockManager`; `permitsMotion()` stays false; Phase 7 flags stay off; FakeActuator writes stay 0. Engine that would command hardware remains [#19](https://github.com/The-Allsparks/MIMIC/issues/19) ([#62](https://github.com/The-Allsparks/MIMIC/issues/62)).
- Immutable `SyncContract` (`maxDisagreement(canonicalUnits).action(STOP_MECHANISM)`) attached optionally to `MechanismConfiguration`. Default absent. Reuses existing `ActuatorTopology` (`mechanicallyLinkedMotors` vs `independentlySensedMotors`) and `DegradedBehavior.STOP_MECHANISM`. `actuatorCount > 1` still does not imply sync. Linked topology plus a `SyncContract` is rejected. Unused by `MimicSession`; `permitsMotion()` / `appliesSideCorrection()` stay false; Phase 5 flags stay off; FakeActuator writes stay 0 ([#61](https://github.com/The-Allsparks/MIMIC/issues/61)).
- Observe-only `StallDetector.update(snapshot) -> StallSuspicion` (and `JamDetector` / `JamSuspicion`). Stall is high current plus no motion for a required timeout, not a single current sample. Missing / NaN / unwired current is unsupported, not stalled. Current-only is not a hard limit. Unused by `MimicSession`; reverse-clear stays forbidden; Phase 8 flags stay off ([#60](https://github.com/The-Allsparks/MIMIC/issues/60)).
- Immutable `Readiness` evaluator: `atSpeed(snapshot, minVel, hysteresis, dwellNanos)` and `inTolerance(...)`. One usable loop inside the band is not ready; dwell must elapse on valid samples. Invalid / `MISSING` / `UNSUPPORTED` / `STALE` velocity is not at-speed. Hysteresis holds ready through a small dip. `feed` does not write hardware; unused by `MimicSession` ([#59](https://github.com/The-Allsparks/MIMIC/issues/59)).
- `MechanismControllerAdapter` / immutable `Setpoint` (position, velocity, unit). Setpoint in, dimensionless effort out; no `setPower`. Unused by `MimicSession`; Phase 4 flags stay off; no NextControl/FTCLib/WPILib compile dependency. Fake adapter unit test does not write `FakeActuator` ([#58](https://github.com/The-Allsparks/MIMIC/issues/58)).
- Immutable `LimitContract` (soft/hard bounds, stopping margin, wrap policy, missing-switch rule) attached optionally to `MechanismConfiguration`. Pure predicates refuse travel into a limit when the switch sample is missing; wrap-aware rotary arcs are documented. Declaration only: `MimicSession` does not call the contract, `permitsMotion()` is false, and FakeActuator writes stay 0 ([#57](https://github.com/The-Allsparks/MIMIC/issues/57)).
- Immutable `CalibrationContract` (direction, max travel, timeout, debounce, encoder reset policy) attached optionally to `MechanismConfiguration`. Declaration only: missing timeout/travel are rejected for homing-capable strategies, `HARD_STOP_CURRENT` stays blocked, and `MimicSession` remains `UNCALIBRATED` / `NO_ACTIVE_CONTROL` ([#56](https://github.com/The-Allsparks/MIMIC/issues/56)).
- Observe-only piece presence and count (`PieceObservation`) from `PIECE_ENTRY` / `PIECE_EXIT` / `PIECE_COUNT`. Missing or unwired sensors are unknown occupancy, not empty and not a count of zero. No identity tracker and no actuation ([#54](https://github.com/The-Allsparks/MIMIC/issues/54)).
- Optional named extras on `MechanismSnapshot` (`sample(name)` / `role(SensorRole)`). Missing lookups are `UNSUPPORTED`, not a fake `false`. Extra suppliers do not replace position, velocity, or limit fields and do not write hardware ([#53](https://github.com/The-Allsparks/MIMIC/issues/53)).
- Immutable named-state name sets on `MechanismConfiguration` (`.namedStates(...)`). Metadata only; empty and duplicate names are rejected. Does not schedule motion ([#52](https://github.com/The-Allsparks/MIMIC/issues/52)).
- Generic mechanism catalog research ([generic-mechanism-catalog.md](docs/mechanism-control/generic-mechanism-catalog.md)) and gap matrix ([generic-mechanism-gap-matrix.md](docs/mechanism-control/generic-mechanism-gap-matrix.md)).
- Passive `org.allsparks.mimic.config`: `MechanismConfiguration`, `ActuatorTopology`, `SensorRole`, `Capability`, `ConstructDescriptor`, validation. Metadata only; no actuation.
- Expanded families (Arm, End effector, Climber, Field element, Passive) and standard constructs. Custom constructs without editing the enum.
- Generic mechanism templates: families Intake, Transfer (internal path; not "shoot"), Launcher, and Lift, with constructs including roller intake, feeder, flywheel, catapult, spinapult, turret, hood, and elevator (`org.allsparks.mimic.templates`). Catalog only; no actuation.
- Initial public repository scaffold for The Allsparks FTC Team 36117.
- Phase 0 `STALE` is documented and tested as observer liveness (gap since the previous `capture()` start), not Control Hub sample age. `staleAfterNanos <= 0` remains the default off switch ([#25](https://github.com/The-Allsparks/MIMIC/issues/25)).
- `MechanismSnapshot` preserves position and velocity `SensorSample` validity so `STALE` is distinguishable from `MISSING` / `UNSUPPORTED`. Observation logs keep `pos` / `vel` and add `posValid` / `velValid` ([#26](https://github.com/The-Allsparks/MIMIC/issues/26)).
- Observation logs keep `lower` / `upper` and add `lowerValid` / `upperValid`. Unusable limit asserted state exports as `n/a` so a disconnected switch is not graphed as not-at-limit ([#28](https://github.com/The-Allsparks/MIMIC/issues/28)).
- Source-backed mechanism-control research, build-versus-adopt decision, architecture, phased roadmap, and student documentation.
- CI for compile, unit tests, and relative documentation link checks.

### Changed

- `MechanismConstruct.TURRET` and `HOOD` are Arm (aim), not Launcher (energy). `CLAW` is End effector, not Intake. Justified in the generic catalog.
- `sensorValid` means required wired channels are usable and not disagreeing. Velocity is required only when `ticksPerSecond` is wired (`UNSUPPORTED` velocity does not clear the flag). Analog-only observation wires mapped analog as `ticks`; omitted ticks still keeps the snapshot invalid ([#27](https://github.com/The-Allsparks/MIMIC/issues/27)).

### Safety

- All motor and servo output features remain disabled by default.
- `MimicSession` refuses actuation feature flags and rejects goals with `NO_ACTIVE_CONTROL`.
