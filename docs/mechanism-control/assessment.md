# Assessment (initial)

Evidence basis: [research.md](research.md), [build-vs-adopt.md](build-vs-adopt.md), and the Phase 0 scaffold. **No Allsparks on-robot MIMIC dataset exists yet.** Elevator hardware is not fully selected ([elevator-target.md](elevator-target.md)).

## Should MIMIC be a standalone project?

**Yes, as a thin lifecycle/safety/integration layer** — not as a general controller, scheduler, or YAMS clone.

## Contribute / wrap instead?

| Project | Recommendation |
|---------|----------------|
| YAMS | Do not wrap; FRC SPARK/TalonFX stack |
| TRC | Do not require; whole framework |
| NextControl | Optional TeamCode adapter later; **do not** Maven-depend (GPL-3.0). Contributing a trapezoid interpolator upstream is valuable |

## Genuinely missing in current FTC libraries

Named calibration lifecycle, direction-aware software limits + **last** safety gate, cross-mechanism interlocks, fault severity with bounded recovery, AMPER request/grant **without** AMPER writing motors, phased educational enablement that starts at observation.

Control math is **not** missing (NextControl, FTCLib, Hub `RUN_TO_POSITION`).

## Rookie-team worthwhile phases

| Phase | Worth for rookies? | Why |
|-------|--------------------|-----|
| **0** | **Yes** | Teaches what must be measured; zero behavior change |
| **1** | **Yes** | Graphs; still safe |
| **2** | **Yes after** direction/limit graphs | Prevents “ticks = inches” mistakes |
| **3** | **Yes** for any gravity or slide | Best reliability per complexity after 0–2 |
| **4** | After 2–3 work | Profiles need a trusted pose |
| **5** | Only if two independently sensed sides | Wait for elevator architecture |
| **6–7** | After multiple mechanisms exist | Interlocks need real geometry |
| **8** | Selective | Easy to over-retry |
| **9** | After AMPER Phase 1 data | Electrical + mechanical |
| **10** | Research / later | Characterization tooling |

## Best benefit / complexity

1. **Phase 0–1 observation** — highest teaching ROI.  
2. **Phase 2 homing + Phase 3 safety gate** — practical damage prevention.  
3. **Phase 4 profiles** — smoothness after pose is trusted.  
4. **Phase 5 anti-racking** — wait until the elevator’s shaft/sensing is known.

## Wait until the elevator physically exists

- Tower sync / anti-racking
- Ratchet interlocks
- Gravity FF constants
- Holding vs FLOAT/BRAKE characterization
- Any homing toward a hard stop

## Primarily research

- SystemCore mechanism APIs (**FH**)
- Full SysId-equivalent tooling
- Automatic intermediate-goal solvers that cannot cycle
- Predictive stall models

## AMPER

MIMIC submits intent; AMPER returns a clip; MIMIC still gates mechanically. Types exist; no clipping in Phase 0.

## Pedro, ViDAR, schedulers

Coexist: snapshot in subsystem `periodic`; do not path, see, or schedule globally.

## Can current FTC hardware execute each phase?

| Phase | Provisional judgment |
|-------|----------------------|
| 0–1 | Yes — sampling |
| 2–3 | Yes — team loop + DIO; not SPARK firmware |
| 4 | Yes at OpMode rate if profiled in Java; Hub `RUN_TO_POSITION` is not a trapezoid |
| 5 | Maybe — depends on independent sensing vs common shaft |
| 6–8 | Yes as software; quality depends on sensors |
| 9 | Yes logically; sensing freshness shared with AMPER |
| 10 | Desktop sim yes; hardware ID limited vs WPILib SysId |

## SystemCore

**Verified:** none claimed.  
**Possible:** richer motor/sensor APIs (**FH**). Boundary class + blocked issue until primary docs exist.
