# MIMIC

**Mechanism Integration, Motion, Interlocks, and Calibration for FTC**

MIMIC is an FTC-first mechanism lifecycle and safety framework for elevators,
arms, extensions, intakes, turrets, servos, and coupled mechanisms.

MIMIC begins with passive observation of mechanism sensors and commands. Teams
can then progressively enable calibration, homing, limits, profiled motion,
multi-actuator synchronization, cross-mechanism interlocks, fault recovery,
simulation, and robot-wide power coordination through AMPER.

MIMIC does not require teams to replace their command scheduler or preferred
control algorithms. Its purpose is to make mechanism behavior safe,
observable, testable, and understandable.

---

## Built by The Allsparks

MIMIC is created and maintained by **[The Allsparks](https://github.com/The-Allsparks)** (FTC Team **#36117**).

It complements the team’s software ecosystem:

* **[ViDAR](https://github.com/The-Allsparks/ViDAR)** provides field perception and spatial awareness.
* **Pedro Pathing** provides chassis localization and motion.
* **MIMIC** manages the lifecycle and safe movement of mechanisms.
* **[AMPER](https://github.com/The-Allsparks/AMPER)** monitors and coordinates electrical demand.

Repository: **[The-Allsparks/MIMIC](https://github.com/The-Allsparks/MIMIC)**

> **Disclaimer:** MIMIC is community-developed and unofficial. It is **not** affiliated with or endorsed by FIRST, REV Robotics, CTRE, NI, or other referenced vendors. Teams must verify legality and performance against the current-season FTC Game Manual.

---

## Current status

| Item | Status |
|------|--------|
| **Version** | `0.1.0-SNAPSHOT` |
| **Implemented phase** | **Phase 0** (contracts, snapshots, fake hardware, passive REV adapters) |
| **Phase 1** | Designed; flag exists, richer telemetry still experimental |
| **Phases 2–10** | Designed / experimental / **disabled by default** |
| **Active motor or servo output** | **Disabled.** Do not enable without review and acceptance tests. |
| **Production safety claims** | **None.** This scaffold has not been validated on a real FTC mechanism. |

**No active phase should be enabled without mechanism-specific testing.**

Supported targets for this scaffold:

* **FTC SDK:** current public [FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController) season releases (Java TeamCode integration). Desktop tests compile against Java 11 without the SDK on the classpath.
* **Hardware:** REV Control Hub and Expansion Hub sensors exposed through the FTC SDK (`DcMotorEx` position/velocity/current, `DigitalChannel`, analog ports). Wiring is supplier-based so this library does not command hardware.
* **Library build:** Java 11 source/target; CI uses Temurin 17 to compile and test.

### Current limitations

* Phase 0 provides interfaces, immutable snapshots, unit conventions, fake hardware, logging, and read-only REV adapters. It does **not** change motor or servo output.
* Encoder ticks are **not** a physical pose until calibration exists (Phase 2). Phase 0 never homes.
* Hub current sampling cost, encoder port accuracy, and limit-switch polarity must be measured on your robot.
* MIMIC does **not** implement FRC SPARK/TalonFX firmware motion magic, REVLib onboard soft limits, or unverified SystemCore features.
* The Allsparks elevator hardware is **not yet selected**; elevator-specific code is withheld until sensors and drive arrangement are known.

### Software limits do not replace mechanical design

MIMIC cannot replace:

* physical hard stops where those are necessary;
* correct motor and encoder direction;
* documented gearing and units;
* a ratchet, brake, or counterbalance on a gravity load;
* adult supervision during mechanism bring-up.

**Incorrect homing, units, direction, or gearing can cause mechanism damage.** Software limits are an additional check, not a substitute for mechanical stops or a well-built mechanism.

### Relationship to other control libraries

MIMIC is a **lifecycle, safety, and integration layer**. It is not a replacement for:

| Project | Role relative to MIMIC |
|---------|------------------------|
| **NextControl** | Optional future adapter for feedback, feedforward, filters, and interpolators. **Not a compile dependency** (GPL-3.0). Trapezoidal interpolators were not merged on `main` as of this research. |
| **YAMS** | FRC/WPILib mechanism library over SPARK and TalonFX. Does not meet current FTC Control Hub requirements. |
| **TRC** | Capable FTC framework with motor homing, limits, and stall protection, but it is a whole robot framework rather than a modular safety layer. |
| **FTCLib** | Optional source of PIDF / trapezoid primitives (FIRST BSD). Not pulled into this scaffold. |
| **WPILib** | Authoritative FRC control documentation. Concepts transfer; classes and firmware do not run on the Control Hub. |
| **Vendor motor-controller firmware** | CTRE Motion Magic and REVLib SPARK soft limits run **on FRC devices**, not on REV Hub motor ports. |

See [build-versus-adopt](docs/mechanism-control/build-vs-adopt.md) and [research](docs/mechanism-control/research.md).

---

## Documentation

| Doc | Purpose |
|-----|---------|
| [Mechanism-control overview](docs/mechanism-control/README.md) | Student entry point |
| [Research](docs/mechanism-control/research.md) | Source-backed findings |
| [Build vs adopt](docs/mechanism-control/build-vs-adopt.md) | Why MIMIC is a standalone layer |
| [Architecture](docs/mechanism-control/architecture.md) | Module boundaries and loop order |
| [Phases](docs/mechanism-control/phases.md) | Phase goals and acceptance |
| [Lifecycle](docs/mechanism-control/lifecycle.md) | Calibration and state model |
| [Safety model](docs/mechanism-control/safety-model.md) | Hazards and fail-safe behavior |
| [Calibration](docs/mechanism-control/calibration.md) | Homing strategies |
| [Motion control](docs/mechanism-control/motion-control.md) | Profiles and adapters |
| [Interlocks](docs/mechanism-control/interlocks.md) | Cross-mechanism constraints |
| [Fault handling](docs/mechanism-control/fault-handling.md) | Severity and recovery |
| [AMPER integration](docs/mechanism-control/amper-integration.md) | Power request/grant boundary |
| [Testing](docs/mechanism-control/testing.md) | Unit / sim / robot procedures |
| [Tuning](docs/mechanism-control/tuning.md) | Flags and graphs |
| [Integration](docs/mechanism-control/integration.md) | OpModes and schedulers |
| [Troubleshooting](docs/mechanism-control/troubleshooting.md) | Failure modes |
| [Glossary](docs/mechanism-control/glossary.md) | Vocabulary |
| [References](docs/mechanism-control/references.md) | Citation table |
| [Examples](examples/README.md) | Integration sketches |
| [Phase 0 file plan](docs/mechanism-control/phase-0-plan.md) | Exact implementation plan |
| [Initial deep audit](docs/audits/initial-deep-audit.md) | 2026-08-17 architecture, safety, and backlog audit |
| [Priority ledger](docs/audits/priority-ledger.md) | Orchestrator work order |
| [Assessment](docs/mechanism-control/assessment.md) | Benefit vs complexity judgment |
| [Risks](docs/mechanism-control/risks.md) | Open questions |
| [Elevator target](docs/mechanism-control/elevator-target.md) | Known vs unknown Allsparks hardware |

---

## Quick start (desktop)

```powershell
git clone https://github.com/The-Allsparks/MIMIC.git
cd MIMIC
.\gradlew.bat test
```

On Linux/macOS:

```bash
./gradlew test
```

---

## Design principles

1. **Passive first.** Observe and teach before commanding hardware.
2. **Feature-flagged phases.** Each phase is independently testable and reversible.
3. **Fail safe.** Missing sensors disable active control; they do not invent trust.
4. **Do not duplicate proven math.** Controllers are replaceable adapters.
5. **Do not replace schedulers or chassis libraries.** Ivy, NextFTC, Pedro Pathing, and AMPER keep their jobs.
6. **Honest maturity.** Do not advertise generalized safety before real mechanisms are tested.

---

## License

MIT — same open-source license family as [ViDAR](https://github.com/The-Allsparks/ViDAR) and [AMPER](https://github.com/The-Allsparks/AMPER). See [LICENSE](LICENSE).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md).
