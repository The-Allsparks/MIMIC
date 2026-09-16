# Generic mechanism catalog

Season-independent taxonomy for MIMIC. This document is research and design. It does not enable motors or servos.

**Access date for archive URLs:** 2026-09-16 unless noted.

Label key (same as [research.md](research.md)):

- **GF** game-required function (official manual or one-page)
- **OI** common implementation observed across teams
- **PA** proposed MIMIC abstraction
- **EI** engineering inference
- **UH** untested hypothesis

Do not treat one successful robot as the universal design. Robot ports, dimensions, named scoring poses, game-piece identities, and operator bindings belong in TeamCode ([library-vs-teamcode.md](library-vs-teamcode.md)).

---

## 1. Methodology and sources

### 1.1 What was asked

Which mechanism jobs recur across FTC seasons, which actuator and sensor layouts teams commonly use, and which reusable software capabilities MIMIC should offer without becoming a season robot or a competing scheduler.

### 1.2 Official game evidence (GF)

Primary archive: [FIRST FTC Past Seasons Archive](https://ftc-resources.firstinspires.org/ftc/archive).

| Season | Official material used |
|--------|------------------------|
| 2025–2026 DECODE | [Competition Manual](https://ftc-resources.firstinspires.org/ftc/archive/2026/game/manual) Game Overview and scoring (ARTIFACTS, GOAL, BASE) |
| 2024–2025 INTO THE DEEP | [Competition Manual](https://ftc-resources.firstinspires.org/ftc/archive/2025/game/manual); [one-page](https://ftc-resources.firstinspires.org/ftc/archive/2025/game/game-one-page) |
| 2023–2024 CENTERSTAGE | [one-page](https://ftc-resources.firstinspires.org/ftc/archive/2024/game-one-page); [season game page](https://ftc-resources.firstinspires.org/ftc/archive/2024/game) |
| 2022–2023 POWERPLAY | [season game page](https://ftc-resources.firstinspires.org/ftc/archive/2023/game) |
| 2021–2022 FREIGHT FRENZY | [season game page](https://ftc-resources.firstinspires.org/ftc/archive/2022/game) |
| 2020–2021 ULTIMATE GOAL through 2014–2015 CASCADE EFFECT | Archive season entries plus FIRST one-pages / Game Manual Part 2 overviews linked from the archive |

Game manuals establish **what robots needed to accomplish**. They do not prescribe roller intakes, flywheels, or slides.

### 1.3 Implementation evidence (OI)

Used only to confirm *how* teams often implemented those jobs, never as a single-team standard:

- [Game Manual 0](https://gm0.org/) motors, control loops, and FSMs ([references.md](references.md))
- Existing MIMIC [research.md](research.md) (REV Hub sensing, homing, limits, stall heuristics)
- Public robot reveals and open TeamCode, cited in the season table only when a pattern appears on **multiple** independent robots

### 1.4 Out of scope for this catalog

- Chassis motion (Pedro Pathing)
- Season game-piece type names in the Java API
- Active control, homing motion, or `setPower`
- A MIMIC scheduler
- Claiming production safety

---

## 2. Season evidence table

Each row: official job (GF), recurring hardware patterns (OI), MIMIC families those patterns map to (PA).

| Season | Game-required functions (GF) | Common implementations (OI) | MIMIC families (PA) | Notes |
|--------|------------------------------|-----------------------------|---------------------|-------|
| **DECODE** 2025–26 | Collect and launch ARTIFACTS through a GOAL; classified vs overflow; depot; return to BASE | Roller intake, transfer/indexer, flywheel, hood and/or turret, feeder, color/presence sensors | Intake, Transfer, Launcher, Arm (hood/turret), End effector (gate/diverter if on robot) | **Correction:** endgame is BASE parking, not a hang. Field GATE is a field element robots may push; robot analog is `PUSHER` / `FIELD_ELEMENT`, not a launcher. |
| **INTO THE DEEP** 2024–25 | Score SAMPLES in baskets/nets; hang SPECIMENS on chambers; PARK or ASCEND rungs | Claw/gripper, pivot arm, wrist, slides/lift, intake, winch/hook hang | Intake, End effector, Arm, Lift, Climber | Specimen hang on a chamber is scoring with a gripper+arm, not Climber. Climber is ASCEND. |
| **CENTERSTAGE** 2023–24 | Place PIXELS on backdrop/backstage; mosaics; launch drone; endgame hang on rigging | Intake, transfer, slides/elevator, outtake arm/wrist/claw, drone launcher, hang | Intake, Transfer, Lift, Arm, End effector, Launcher (drone as deployer/puncher analog), Climber | Drone is a one-shot deployer more than a flywheel. |
| **POWERPLAY** 2022–23 | Deliver CONES onto junctions; stack; substations | Claw, elevator/slides, turret, pivot, wrist | End effector, Lift, Arm | Turret is rotary aim/position, not launch energy. |
| **FREIGHT FRENZY** 2021–22 | Collect freight; score in hub/shared hub; carousel duck/team shipping element; capping | Intake, conveyor, bucket/arm or lift, carousel spinner, capper | Intake, Transfer, Lift, Arm, End effector, Field element | Carousel is field-element interaction. |
| **ULTIMATE GOAL** 2020–21 | Collect rings; launch into goals; wobble-goal transport | Intake, conveyor/indexer, flywheel, hood/turret, feeder, wobble arm+claw | Intake, Transfer, Launcher, Arm, End effector | Canonical flywheel+hood+indexer stack. |
| **SKYSTONE** 2019–20 | Collect stones; stack on foundation; move foundation; capstone | Intake, transfer, elevator, gripper, foundation latch, capper | Intake, Transfer, Lift, End effector, Field element | Foundation mover is field-element, not a piece gripper. |
| **ROVER RUCKUS** 2018–19 | Land; collect minerals; score in lander; hang | Intake, bucket, arm/lift, hanging latch/winch, team marker | Intake, Transfer, Lift, Arm, End effector, Climber, Field element (marker) | Hang is Climber (loaded latch). |
| **RELIC RECOVERY** 2017–18 | Glyphs into cryptobox; jewel; relic | Intake, lift, gripper, relic extension+arm+claw, jewel arm | Intake, Lift, Arm, End effector | Relic is extension (Lift) plus arm plus claw. |
| **VELOCITY VORTEX** 2016–17 | Launch particles; press beacons; cap-ball lift | Intake, conveyor, flywheel, beacon pusher, lift | Intake, Transfer, Launcher, Field element, Lift | Beacon is field-element. |
| **RES-Q** 2015–16 | Debris to goals; climbers/zipline; hang from pull-up bar; beacon repair | Intake, conveyor, bucket, winch/hook, beacon/marker | Intake, Transfer, End effector, Climber, Field element | Official one-page: hang from Pull-up Bar. |
| **CASCADE EFFECT** 2014–15 | Balls into rolling goals; kickstand; move goals | Intake, conveyor/elevator, hopper, gate/dumper, goal capture | Intake, Transfer, Lift, End effector, Field element | Rolling-goal capture is field-element. |

Starting hypothesis vs evidence:

- DECODE launcher stack: **confirmed GF+OI**. DECODE hang: **rejected** (BASE is parking).
- Climber family still required: INTO THE DEEP ASCEND, CENTERSTAGE hang, ROVER RUCKUS, RES-Q, and others.
- Vacuum intake: **not a GF**. Rare OI; legality is season-rule-dependent. Catalog as optional construct, not a controller.

Current-season robot lists (BumbleBee / BIOBUZZ) stay in TeamCode. See [biobuzz-mechanism-targets.md](biobuzz-mechanism-targets.md).

---

## 3. Normalized taxonomy

### 3.1 Families (jobs)

Families are library-owned. A custom construct must pick one. A new family requires a MIMIC change.

| Family | Job | Not this family |
|--------|-----|-----------------|
| **Intake** | Acquire a piece from the field | Internal conveyance; gripping jaws used after acquisition |
| **Transfer** | Move a piece along a path inside the robot | Launching off the robot; chassis motion |
| **Launcher** | Impart energy so a piece leaves the robot | Aim axes (turret/hood); feeder (last transfer stage) |
| **Lift** | Linear positioning of a carriage, stage, or tray | Rotary joints |
| **Arm** | Rotary positioning | Launch energy; linear slides |
| **End effector** | Named-state tool at the end of a kinematic chain | The arm or lift that carries it |
| **Climber** | Support the robot from a field hang structure while loaded | Parking in a zone; scoring a piece onto a bar |
| **Field element** | Actuate a field scoring device the robot does not own | Game piece in the robot |
| **Passive** | Funnel, guide, or deployable structure with no piece-path job of its own | Any actively controlled axis |

**Storage/indexing is not a family.** Indexer, hopper, magazine, and occupancy tracking are Transfer constructs plus optional `PIECE_TRACKING`. A bin with no actuator is Passive or an unpowered Transfer hopper.

**Field-element interaction is a family.** The job is changing a field device (carousel, foundation, beacon). Mixing it into End effector hides alliance and field-ownership hazards.

Drivetrain motion is out of MIMIC.

### 3.2 Constructs (layouts)

Standard presets live in `MechanismConstruct`. Teams add layouts with `ConstructDescriptor.custom` without editing that enum.

**First-class** (own preset):

| Family | Constructs |
|--------|------------|
| Intake | roller, compliant wheel, vectored, over-the-top, under-bumper, deployable, spatula, vacuum (legality team-verified) |
| Transfer | belt, roller path, pinch-roller path, feeder, color sorter, indexer, rotary magazine, hopper, piece accumulator |
| Launcher | flywheel, catapult, spinapult, puncher |
| Lift | elevator, linear slide, telescoping extension, capstan, rack-and-pinion, lead-screw |
| Arm | pivot arm, shoulder, elbow, wrist, turret, hood, four-bar, virtual four-bar, linkage deployer |
| End effector | claw/gripper, gate, diverter, bucket, latch, hook, pusher |
| Climber | winch, hook deployer, ratchet-assisted hang |
| Field element | carousel spinner, foundation grabber, beacon pusher, marker/drone deployer |
| Passive | funnel/guide, deployable structure |

**Configured variants, not extra controllers:**

- Cascade vs continuous lift: same Lift construct; differ in conversion, rigging, and synchronization configuration.
- Linked dual-slide: `ELEVATOR` or `LINEAR_SLIDE` plus independently sensed motors.
- Two motors on one shaft: mechanically linked topology; **do not** enable independent synchronization.
- Dual flywheel: `FLYWHEEL` plus opposed or independently sensed topology.
- Turreted launcher: compose `TURRET` + `HOOD` + `FLYWHEEL` + `FEEDER`.
- Intake claw: compose deployable intake or spatula with `CLAW`.

**Vacuum:** optional Intake construct. Teams must verify the current Game Manual. MIMIC ships no vacuum controller.

### 3.3 Actuator topology (independent of construct)

| Topology | Meaning | Sync implication |
|----------|---------|------------------|
| None / passive | No MIMIC-owned actuator | Must use `PASSIVE_OBSERVATION` (or Passive family) |
| Positional servo | Commanded pose; **not** measured position | Do not treat command as a position sensor |
| Continuous-rotation servo | Open-loop or velocity-like effort | |
| Single DC motor | One commanded axis | |
| Mechanically linked motors | Same command; shaft or gearbox ties them | Independent side correction is unsafe |
| Independently sensed motors | Separate motion possible | Sync is an **opt-in** capability |
| Opposed motors | Counter-rotating or pinch pair | May share velocity target without position sync |
| Motor plus servo release | Stored-energy or latch release | Release is a second named-state axis or composed mechanism |
| Motor plus brake | Brake is a binary actuator/sensor | |
| Motor plus ratchet | Ratchet state must be sensed, not assumed | |

Students should keep three hardware facts apart:

- **Linked motors share a command.** Two motors on one shaft or gearbox are `mechanicallyLinkedMotors`. Do not independently synchronize a common shaft.
- **Two towers need two sensors.** Independently sensed sides (`independentlySensedMotors`) are the only topology that may opt into `MULTI_ACTUATOR_SYNCHRONIZATION` and an optional `SyncContract`.
- **`actuatorCount > 1` does not mean sync.** `ActuatorTopology.impliesIndependentSynchronization()` is always false. Count, linkage, and an explicit contract are different ideas.

`SyncContract.maxDisagreement(canonicalUnits).action(STOP_MECHANISM)` is an optional declaration on `MechanismConfiguration`. Default is absent. `MimicSession` does not call it. Validation rejects a `SyncContract` on mechanically linked topology. This is not anti-racking output ([#16](https://github.com/The-Allsparks/MIMIC/issues/16)). Allsparks elevator CAD (whether the shop robot is linked or independent) stays hardware ([#15](https://github.com/The-Allsparks/MIMIC/issues/15)).

Multi-axis assemblies are multiple `MechanismConfiguration` objects composed by the OpMode, not one topology value.

### 3.4 Motion kinds

Unchanged catalog hints: `CONTINUOUS`, `POSITIONED`, `DISCRETE`. Observation still uses `MechanismUnits`. These are not controllers.

---

## 4. Sensor-role catalog

Roles are library-owned. Channel **names** are team-owned. No FTC device class belongs in core configuration.

Phase 0 `MechanismObserver` still has fixed channels (position, velocity, current, limits, absolute, redundant). Configuration **declares** roles; wiring them into the observer is a later issue.

| Role | Typical FTC mapping (OI) | Units | May calibrate? | Hard boundary? | Fallback if missing |
|------|--------------------------|-------|----------------|----------------|---------------------|
| `RELATIVE_POSITION` | Motor encoder | canonical length or angle | After a home or known pose | Soft limits only | Mechanism uncalibrated |
| `ABSOLUTE_POSITION` | Absolute encoder, potentiometer | canonical | Yes at startup | Soft limits if mapped | Cannot claim pose |
| `VELOCITY` | Encoder velocity | canonical / s | No | No | No at-speed; no stall-from-velocity |
| `ACCELERATION` | Derived or IMU | canonical / s^2 | No | No | Omit; do not invent |
| `RETRACT_LIMIT` | Limit/touch, Hall | asserted boolean | Home/retract | Yes, if usable | Missing is not "not asserted" |
| `EXTEND_LIMIT` | Limit/touch, Hall | asserted boolean | Rarely | Yes, if usable | Same |
| `HOME_INDEX` | Index pulse, Hall, switch | asserted or edge | Yes | Usually not a travel stop | Cannot home-on-index |
| `PIECE_ENTRY` | Beam-break, color, distance | asserted / class | No | No | Unknown presence, not empty |
| `PIECE_EXIT` | Beam-break | asserted | No | No | Unknown occupancy; counting degrades |
| `PIECE_IDENTITY` | Color / vision class | enum-like string in TeamCode | No | No | Unknown identity |
| `PIECE_COUNT` | Derived or dedicated counter | integer | No | No | Unknown occupancy, not zero |
| `ACTUATOR_CURRENT` | Hub `getCurrent` | A | Stall-home only if **explicitly** permitted | Never the only hard limit | Stall detection off |
| `MOTOR_TEMPERATURE` | When a device reports it | deg | No | Thermal degrade later | Unsupported |
| `LOAD_TENSION` | Load cell | N or team unit | No | Load limit later | Climb/grip force unknown |
| `LATCH_ENGAGED` | Switch, Hall | asserted | No | Release interlock | Latch unknown; no auto-release |
| `BRAKE_ENGAGED` | Switch or command confirm | asserted | No | Hold policy | Do not assume braked |
| `OBJECT_CONTACT` | Touch, current, distance | asserted | No | Grip complete later | Gripper has no object-detected |
| `ACTUATOR_SIDE_POSITION` | Motor encoder | canonical | With home | Soft | |
| `OUTPUT_SIDE_POSITION` | External quadrature | canonical | With home | Soft; skew detect | No output-side check |
| `REDUNDANT_POSITION` | Second encoder | canonical | Disagreement | Stop on disagree later | Degraded per config |
| `ORIENTATION` | IMU on the moving assembly | rad | Gravity FF later | Envelope later | No gravity from IMU |
| `EXTERNAL_SERVO_FEEDBACK` | Separate analog/encoder on a servo linkage | canonical | Yes | Soft | Servo remains command-only |

Validity for any role: `VALID`, `STALE`, `MISSING`, `OUT_OF_RANGE`, `UNSUPPORTED`, `DISAGREEING` ([MeasurementValidity](../../src/main/java/org/allsparks/mimic/observe/MeasurementValidity.java)).

**Never** model servo *command* position as measured physical position.

Polarity/direction lives on the declaration and on `MechanismUnits`, not on the role enum.

---

## 5. Mechanism / sensor / feature matrix

Suggested sensors and features are **presets**, not wiring. A construct default is not "this robot has that sensor."

| Family | Construct | Typical topology | Required roles (if that capability is enabled) | Optional roles | Calibration choices | Control domain | Reusable features (later) | Hazards | Often composed with | Season evidence |
|--------|-----------|------------------|--------------------------------------------------|----------------|---------------------|----------------|---------------------------|---------|---------------------|-----------------|
| Intake | Roller / compliant / vectored / over-top / under-bumper | Single motor or CR servo | None for observe | Velocity, current, piece entry, deploy limits | Usually none | Open-loop effort | In/out/stop, reverse, jam, bounded clear, deploy interlock | Jam, eject into field, running while stowed | Deployer, color sorter | DECODE, CENTERSTAGE, UG, VV |
| Intake | Deployable | Motor or servo + intake | Deploy limits if motion | | Home or named poses | Named state or position | Deploy interlock | Collision with lift | Roller intake | Many |
| Intake | Spatula | Positional servo or motor | | Contact | Known pose | Named state | | Sweep collision | Claw | ITD, SKYSTONE |
| Intake | Vacuum | Motor blower | Legality team-verified | Presence, current | None | Open-loop | | Illegal pneumatics, loss of piece | | Rare; not GF |
| Transfer | Belt / roller / pinch path | Single motor | | Encoder, entry, exit, current | None | Open-loop or velocity | Occupancy, count, anti-double-feed, jam, stop-on-full | Double feed, jam | Intake, feeder, indexer | FF, UG, VV, CASCADE |
| Transfer | Indexer / magazine | Motor | Position or index for discrete advance | Pocket occupancy, entry/exit | Home/index | Discrete index | Advance-one, wrap, missed-index | Skip pocket, crush piece | Feeder, launcher | UG, DECODE OI |
| Transfer | Feeder | Motor | | Entry, launcher-ready input (interlock later) | None | Open-loop | Feeder interlock | Fire into unloaded flywheel | Flywheel | UG, DECODE OI |
| Transfer | Hopper / accumulator | None or motor agitator | | Occupancy | None | Passive or open-loop | Capacity | Overflow onto field | Intake, gate | CASCADE, RR |
| Transfer | Color sorter | Motor + diverter compose | Identity + presence | | None | Named state + effort | Reject routing | Wrong reject | Diverter | DECODE OI |
| Lift | Elevator / slide / extension / capstan / rack / lead-screw | Single or linked or independently sensed motors | Position if soft limits | Home, upper limit, absolute, redundant, current | Home switch, absolute, known pose | Profiled position | Homing, limits, hold, gravity, skew, stall | Fall, rack, overrun | Wrist, gripper | ITD, CS, PP, RR |
| Arm | Pivot / shoulder / elbow / wrist / four-bar | Motor, optionally absolute | Absolute or relative+home for pose | Limits, current, orientation | Absolute preferred | Profiled position | Wrap policy, gravity FF, envelope | Cable wrap, gravity drop | Lift, gripper, hood | ITD, PP, RR |
| Arm | Turret | Motor | Absolute or relative+home | CW/CCW limits | Absolute or index | Profiled position | Shortest path, cable wrap, settle | Continuous spin vs cable | Hood, flywheel, lift | DECODE OI, PP, UG |
| Arm | Hood | Motor or servo | Absolute or external servo feedback | | Absolute or named | Position or named state | Aim tolerance | Treating servo command as pose | Flywheel | DECODE OI, UG |
| Launcher | Flywheel | Single or opposed motors | Velocity if ready-at-speed | Current, feeder entry, exit | None | Velocity | At-speed, droop, feeder interlock | Feeding while slow | Feeder, turret, hood | DECODE, UG, VV |
| Launcher | Catapult / puncher / spinapult | Motor plus release | Cocked/home, release latch | Current, position | Home | Stored-energy cycle | One-shot, no repeat fire | Stored energy | Hood, turret | CS drone analog, some DECODE OI |
| End effector | Claw / gripper | Positional servo | None | Object contact, distance, external feedback, load | Named open/close | Named state | Open/close/hold; no command-as-pose | Crush, drop | Arm, lift | ITD, PP, SKYSTONE |
| End effector | Bucket / gate / diverter / latch / hook / pusher | Servo or motor | Optional state switches | Presence, Hall | Named states | Named state | Commanded vs confirmed, fail-safe | Unconfirmed dump | Transfer, climber | FF, CASCADE, SKYSTONE |
| Climber | Winch / hook deployer / ratchet hang | Motor plus latch/ratchet | Encoder, retract limit | Tension, latch, redundant | Staged; no silent home under load | Named state + position | Staged climb, no auto-release | Drop from hang | Hook, latch | ITD, CS, RR, RES-Q |
| Field element | Carousel / foundation / beacon / marker deployer | Motor or servo | Optional confirm switch | | Named or one-shot | Named state or open-loop | One-shot, re-arm | Spinning alliance carousel illegally; leaving latch on field | | FF, SKYSTONE, VV, RR |
| Passive | Funnel / deployable structure | None | None | Optional deploy confirm | None | Passive observation | | Assuming it is driven | Intake | Many |

---

## 6. Common state machines (design only)

These are teaching sketches for later phases. They are not implemented and must not write hardware.

Teams may declare the allowed names on `MechanismConfiguration` as metadata, for example `.namedStates("OPEN", "CLOSED", "HOLDING")`. Duplicate and empty names are rejected at validation. The list is not a scheduler, does not request motion, and does not enable [#18](https://github.com/The-Allsparks/MIMIC/issues/18).

**Roller intake:** `STOPPED` / `INTAKING` / `OUTTAKING` / (`JAM_CLEARING` bounded). Missing piece sensor stays in commanded direction.

**Indexer:** `IDLE` / `ADVANCING` / `SETTLED` / `MISSED_INDEX`. Wrap-aware pocket occupancy.

**Elevator/arm:** `UNCALIBRATED` / `HOMING` / `HOLDING` / `MOVING` / `FAULTED`. Matches existing `CalibrationState`.

**Flywheel:** `IDLE` / `SPINNING_UP` / `AT_SPEED` / `DROOPED` / `SPINNING_DOWN`.

**Catapult/puncher:** `SAFE` / `COCKING` / `COCKED` / `FIRING` / `RECOVERING`. Repeat fire forbidden until `SAFE` or `COCKED` by policy.

**Gripper:** `OPEN` / `CLOSING` / `CLOSED` / `HOLDING` / `OPENING`. Object-detected is optional.

**Climber:** `STOWED` / `HOOKING` / `LOADED` / `HANGING` / `EXPLICIT_RECOVERY`. No automatic release from `LOADED`/`HANGING`.

Semantic names such as `STOWED`/`SCORING` belong in TeamCode or later Phase 6. They request goals; they do not schedule the robot ([#18](https://github.com/The-Allsparks/MIMIC/issues/18)).

---

## 7. Cross-cutting feature catalog

| Feature group | Contents | Phase intent |
|---------------|----------|--------------|
| Observation | Immutable snapshot, timestamps, validity, debounce, edges, redundant compare | 0–1 (partially exists) |
| Calibration | None, known pose, absolute, home switch, permitted stall-home, index, manual, retained, suspect | 2 (blocked) |
| Limits | Hard, soft, direction-aware, stopping margin, missing switch != not asserted, wrap-aware rotary | 3 (blocked) |
| Control domains | Effort, voltage-compensated effort, velocity, position, profiled position, discrete index, named state, stored-energy, passive | 4+ adapters (blocked) |
| Feedforward seams | Simple rotating, flywheel, vertical linear, arm gravity, horizontal extension, external adapter | 4 (no unvalidated math now) |
| Readiness | Position/velocity tolerance, dwell, at-speed, transition complete, acquired, feeder ready, homed, interlock, timeout, faulted | `Readiness.atSpeed` / `inTolerance` now ([#59](https://github.com/The-Allsparks/MIMIC/issues/59)); feeder/homed/interlock later |
| Faults | Invalid, disagree, unexpected limit, stall, jam, skew, failed home, timeout, unexpected motion, lost calibration, insufficient AMPER grant, latch unknown | Observe-only stall/jam suspicion now ([#60](https://github.com/The-Allsparks/MIMIC/issues/60)); reverse-clear / Phase 8 recovery later |
| Interlocks | Named constraints; reject / defer / clamp / intermediate / confirm; no deadlock scheduler | 7 |
| Piece tracking | Observe-only presence/count now (`PieceObservation` from `PIECE_ENTRY` / `PIECE_EXIT` / `PIECE_COUNT`). Missing sensor is unknown occupancy, not empty. Identity, capacity, occupancy reconcile, confidence, reject routing wait for [#64](https://github.com/The-Allsparks/MIMIC/issues/64) | [#54](https://github.com/The-Allsparks/MIMIC/issues/54) observe; tracker later; no season piece names |

MIMIC owns mechanism safety. AMPER only grants or limits electrical allocation ([amper-integration.md](amper-integration.md)). SHIFT semantic intents and HELM orchestration stay outside MIMIC. TRACE may later sink `MimicEventLogger` fields. The FTC OpMode remains the composition root.

### 7.1 Piece presence and count (observe-only)

`PieceObservation.from(snapshot)` ([PieceObservation.java](../../src/main/java/org/allsparks/mimic/observe/PieceObservation.java)) reads `snapshot.role(SensorRole.PIECE_ENTRY)`, `PIECE_EXIT`, and `PIECE_COUNT`. Presence is `VALID`, `MISSING`, or `UNSUPPORTED`. A missing or unwired beam-break is unknown occupancy, not empty. A `VALID` count of `0` is known empty; an omitted count channel is unknown, not zero. Students should check `isUnknown()` / `occupancyUnknown()` before treating `present() == false` as an empty path.

This is not a tracker. Core Java must not name season pieces. Identity strings, pocket capacity, confidence, reconcile, and reject routing stay in [#64](https://github.com/The-Allsparks/MIMIC/issues/64). No actuation.

### 7.2 Readiness and settling (pure evaluation)

`Readiness.atSpeed(snapshot, minVel, hysteresis, dwellNanos)` ([Readiness.java](../../src/main/java/org/allsparks/mimic/observe/Readiness.java)) reads `snapshot.velocitySample()`. One valid loop at or above `minVel` is not ready: dwell must elapse on consecutive usable samples. `MISSING` / `UNSUPPORTED` / `STALE` (and other non-usable) velocity is not at-speed and resets the window. After ready, dropping slightly below `minVel` stays ready until velocity falls through `minVel - hysteresis`. `Readiness.inTolerance(snapshot, target, tolerance, hysteresis, dwellNanos)` is the position dual (`tolerance + hysteresis` to stay). `feed(snapshot)` returns a new immutable evaluator; it does not write hardware. `MimicSession` does not call it.

This is not a feeder interlock engine and does not fire a launcher. Homed, acquired, timeout, and faulted remain later.

### 7.3 Stall and jam suspicion (observe-only)

`StallDetector.update(snapshot)` ([StallDetector.java](../../src/main/java/org/allsparks/mimic/observe/StallDetector.java)) reads `snapshot.currentAmps()` and `snapshot.velocitySample()`. A stall is high current plus no motion for a required timeout, not a single current sample. One qualifying loop is not suspected. Missing, NaN, or unwired current is unsupported, not stalled. Unusable velocity is also unsupported: current-only is not a hard limit. `JamDetector` / `JamSuspicion` use the same heuristic. `update` does not write hardware. `MimicSession` does not call it. Reverse-clear and bounded jam clearing ([#20](https://github.com/The-Allsparks/MIMIC/issues/20)) stay forbidden. Do not enable `MimicFeatureFlags.phase8Faults`.

### 7.4 Multi-actuator topology and sync contracts (declaration-only)

`ActuatorTopology.mechanicallyLinkedMotors(n)` versus `independentlySensedMotors(n)` is the linked-versus-independent distinction. Linked motors share a command. Independent towers may later compare two sensors. `actuatorCount > 1` does not imply sync: `impliesIndependentSynchronization()` stays false.

`SyncContract` ([SyncContract.java](../../src/main/java/org/allsparks/mimic/config/SyncContract.java)) records `maxDisagreement` in canonical units and a `DegradedBehavior` action (reuse `STOP_MECHANISM`). Attach it optionally on `MechanismConfiguration`; default absent. `MimicSession` does not call it. `permitsMotion()` and `appliesSideCorrection()` are false. A `SyncContract` on mechanically linked topology is rejected: you do not independently sync a common shaft. Phase 5 flags stay off. Anti-racking output stays [#16](https://github.com/The-Allsparks/MIMIC/issues/16). Elevator CAD stays [#15](https://github.com/The-Allsparks/MIMIC/issues/15).

---

## 8. Design decisions

| ID | Decision | Why |
|----|----------|-----|
| D1 | Families are a closed enum | Jobs are a stable taxonomy. Custom robots pick a family. |
| D2 | Constructs are presets plus `ConstructDescriptor.custom` | Avoid "enum as the only extension." |
| D3 | Storage/indexing is Transfer + capability | Occupancy is a feature of a path, not a job. |
| D4 | Field element is its own family | Field-device hazards differ from grippers. |
| D5 | Turret and hood are Arm, not Launcher | Aim is rotary positioning composed with launch energy. |
| D6 | Claw is End effector, not Intake | Acquisition vs gripping are different jobs. |
| D7 | Cascade/continuous are not separate controllers | Conversion and rigging only. |
| D8 | Topology is independent of construct | Linked vs independent sides have different faults. |
| D9 | Configuration does not contain FTC hardware types | Core stays desktop Java 11. |
| D10 | Observer snapshot shape unchanged in this slice | Join key is `mechanismId`. Role maps later. |
| D11 | Presets are suggestions | A default feature is not wired hardware. |
| D12 | No competing scheduler | Ivy / NextFTC / team code keep composition. |
| D13 | Servo command is not measured pose | Requires `EXTERNAL_SERVO_FEEDBACK`. |
| D14 | Vacuum is catalog-only | Legality and rarity. |
| D15 | Java 11, no records | Matches [build.gradle](../../build.gradle). |
| D16 | Phases 2–10 stay disabled | [phases.md](phases.md) gate. |

Answers to the architecture questions:

1. Combination of enum presets and immutable custom descriptors.
2. `ConstructDescriptor.custom(id, family, motionKind, summary)`.
3. Preset / instance config / hardware binding / runtime / tuning / TeamCode split as in the table in section 3 and [library-vs-teamcode.md](library-vs-teamcode.md).
4. Named `SensorDeclaration` list, not nullable constructors.
5. `ConfigurationValidator` contradiction codes (homing, sync, soft limits, at-speed, counting, zero actuators, servo-as-pose).
6. Per-role `DegradedBehavior` map; required roles cannot `IGNORE_OPTIONAL`.
7. Small capability declarations; no universal mechanism class.
8. Now: catalog + config + validation. Later: wiring, trackers, controllers, gates.
9. Breaking: `TURRET`/`HOOD` family Arm; `CLAW` family End effector. `MechanismBlueprint` kept.
10. Same `mechanismId`; snapshot API unchanged.

---

## 9. Rejected abstractions

- One giant mechanism enum that picks control, sensing, and safety.
- `actuatorCount > 1` implies synchronization.
- Storage/indexing as a tenth job family.
- God-class `UniversalMechanism`.
- MIMIC-owned command scheduler.
- Season piece names (`ARTIFACT`, pollen, nectar) in core types.
- Cascade vs continuous as different control classes.
- Servo command as `RELATIVE_POSITION`.
- FTC `DcMotor` / `HardwareMap` in `mimic-core` configuration.
- Implementing feedforward math "to fill the interface."
- Enabling Phase 2–10 because metadata exists.
- Treating DECODE BASE as a climber.

---

## 10. Open questions

- When should `MechanismSnapshot` grow named role samples versus keeping fixed channels plus a side map?
- Is `COLOR_SORTER` a Transfer construct or always `DIVERTER` + identity sensor + TeamCode policy?
- Should `DUAL_FLYWHEEL` become a preset after more OI, or remain topology-only?
- Stall-to-home: keep declaration-only until a mechanism-specific safety review ([#8](https://github.com/The-Allsparks/MIMIC/issues/8)).
- FTC SDK packaging stays [#34](https://github.com/The-Allsparks/MIMIC/issues/34), not this catalog.

---

## 11. Traceability (features back to seasons)

| Proposed capability | GF seasons that create the need | OI confirmation |
|---------------------|---------------------------------|-----------------|
| Roller intake observe | Every piece-collection season | GM0 / common robots |
| Piece identity / reject | DECODE classified colors; CENTERSTAGE mosaics | Color sensors on intakes |
| Indexer / feeder / at-speed | DECODE, UG, VV launch goals | Flywheel + feeder interlock |
| Linear pose, home, limits | ITD baskets, PP junctions, CS backdrop | Slides/elevators |
| Rotary absolute pose | PP turret, ITD arm, UG hood | Absolute encoders / pots |
| Named-state gripper | ITD specimen, PP cone, SKYSTONE | Servo claws |
| Climber loaded latch | ITD ASCEND, CS hang, RR, RES-Q | Winch + hook |
| Field-element one-shot | FF carousel, SKYSTONE foundation, VV beacon | Spinners and latches |
| Jam / stall | Intake/transfer seasons | Current + no velocity |
| Multi-actuator skew | Tall slides (ITD, PP, CS) | Independent towers OI |
| Stored-energy cycle | Catapult/puncher/drone | CS drone, some launchers |

---

## 12. Implementation slice (this repository)

Allowed now: families, constructs, topology, sensor roles, capabilities, immutable configuration, named-state name sets, snapshot role extras, observe-only piece presence/count (`PieceObservation`), pure `Readiness` at-speed / in-tolerance evaluation, observe-only stall/jam suspicion (`StallDetector` / `JamDetector`), optional unused `SyncContract` on independently sensed topology, validation, tests, this document, [gap matrix](generic-mechanism-gap-matrix.md). Catalog metadata tracked as [#49](https://github.com/The-Allsparks/MIMIC/issues/49)–[#54](https://github.com/The-Allsparks/MIMIC/issues/54); readiness as [#59](https://github.com/The-Allsparks/MIMIC/issues/59); stall/jam as [#60](https://github.com/The-Allsparks/MIMIC/issues/60); topology vs sync contracts as [#61](https://github.com/The-Allsparks/MIMIC/issues/61).

Forbidden now: motor/servo writes, homing motion, limit enforcement, controllers, interlock engines, piece-tracker runtime, Phase 2–10 flags. Follow-up issues: [#55](https://github.com/The-Allsparks/MIMIC/issues/55)–[#70](https://github.com/The-Allsparks/MIMIC/issues/70). Active control remains behind [#69](https://github.com/The-Allsparks/MIMIC/issues/69).

Java: `org.allsparks.mimic.templates` (families/constructs/blueprints), `org.allsparks.mimic.config` (configuration), and `org.allsparks.mimic.observe` (snapshot extras, `PieceObservation`, `Readiness`, `StallDetector`).
