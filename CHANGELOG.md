# Changelog

All notable changes to MIMIC will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

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
