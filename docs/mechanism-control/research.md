# Research: FTC / FRC mechanism control (source-backed)

**Access date for all URLs unless noted:** 2026-08-17  
**Label key:** **VF** verified fact · **OI** observed implementation · **EI** engineering inference · **UH** untested hypothesis · **FH** future-hardware possibility

This report distinguishes firmware behavior on FRC smart controllers from capabilities available through the current FTC SDK and REV hubs. FRC SPARK/TalonFX features are **not** claimed as current FTC Hub features.

Companion decision: [build-vs-adopt.md](build-vs-adopt.md). Citation table: [references.md](references.md).

---

## 1. How FTC teams currently control elevators, arms, and extensions

| Pattern | Evidence | Label |
|---------|----------|-------|
| Open-loop joystick power (`RUN_WITHOUT_ENCODER`) | REV encoder tutorial; GM0 motor power is PWM fraction of battery voltage[^rev-enc][^gm0-motors] | VF |
| Firmware position hold (`RUN_TO_POSITION`) | REV documents target ticks, then mode, then max velocity; motor holds after arrival[^rev-enc] | VF |
| Firmware velocity (`RUN_USING_ENCODER` / `setVelocity`) | REV: Hub uses encoder to target velocity[^rev-enc] | VF |
| Team-code PID + constant gravity FF | NextControl linear-slide example writes `motor.power` from `posPid` + `elevatorFF` every loop[^next-slides] | OI |
| Framework motors (TRC) | `TrcMotor`: limits, stall, sync, zero calibration, gravity, voltage compensation[^trc] | OI (README) |
| State machines for intakes/arms | GM0 FSM chapter — common FTC structure[^gm0-fsm] | VF (practice) |

**EI:** Competitive FTC elevators usually combine a known start pose or limit-switch home with either Hub `RUN_TO_POSITION` or a loop PID. Few teams publish a separate safety-gate layer.

---

## 2. FTC SDK motor modes and limitations

**VF:** `DcMotor.RunMode` includes `RUN_WITHOUT_ENCODER`, `RUN_USING_ENCODER`, `RUN_TO_POSITION`, and `STOP_AND_RESET_ENCODER`.[^javadoc-runmode]

| Mode | What it does | Limitation |
|------|----------------|------------|
| `RUN_WITHOUT_ENCODER` | Power −1…1 as PWM of **input voltage**[^gm0-motors] | Speed varies with battery and load |
| `RUN_USING_ENCODER` | Velocity targeting on Hub | Still not a motion profile; coefficients use internal units[^gm0-motors] |
| `RUN_TO_POSITION` | Firmware position PID; holds at target[^rev-enc] | Order of `setTargetPosition` then `setMode` matters historically; stall if target unreachable[^rev-enc][^issue-913] |
| `STOP_AND_RESET_ENCODER` | Reinterprets current pose as zero; removes power as a side effect; brake vs float unspecified[^javadoc-runmode] | Not a physical home; GM0 notes it is not really a run mode[^gm0-motors] |

**VF:** `DcMotorEx` adds `getVelocity()` (ticks/s), `setVelocity`, and `getCurrent`.[^javadoc-motor]  
**VF (community):** Motor current is **not** bulk-read; over-current alerts may be.[^gm0-motors]

**EI:** `RUN_TO_POSITION` is not a trapezoidal profile. Abrupt target changes can still command aggressive acceleration.

---

## 3. Encoder zeroing and position meaning

**VF:** REV: quadrature encoders count ticks from an origin you choose; they do not know absolute pose. Teams should `STOP_AND_RESET_ENCODER` in init **and** physically reset the mechanism.[^rev-enc]

**VF:** SDK: reset does not move the motor to a mechanical zero; it relabels the current count.[^javadoc-runmode]

**Teach:** Encoder counts do not automatically identify a physical position. That is Phase 2’s lesson.

---

## 4. Absolute versus incremental sensing

**VF:** REV contrasts quadrature (stopwatch) with absolute encoders such as the Through Bore Encoder (clock).[^rev-enc]

**VF (GM0):** High-CPR encoders on Expansion Hub ports 1–2 can lose steps because those ports are software-decoded; ports 0 and 3 are hardware-decoded.[^gm0-motors]

**EI:** Absolute sensors still need a documented mapping (volts→angle, counts→mm) and a validity check if disconnected.

---

## 5. Limit-switch behavior

**VF:** FTC reads switches via `DigitalChannel` (RobotCore 11.2.0 javadoc exists).[^javadoc-dio]

**OI:** Community elevator homing polls a digital channel, then resets encoders; polarity and bounce are team problems.[^ftc-forum-reset]

**EI:** Normally-open vs normally-closed must be documented. A disconnected NC switch can look “pressed.” Bounce can false-home without debounce (Phase 2).

**VF (FRC contrast, do not transplant):** REVLib SPARK `LimitSwitchConfig` can stop the motor **in the controller** and optionally set position on trigger.[^revlib-ls] That is SPARK firmware, not REV Hub motor ports.

---

## 6. Homing strategies

| Strategy | FTC feasibility | Label |
|----------|-----------------|-------|
| Drive to limit switch, debounce, reset encoder | Common team code | OI / EI |
| Absolute encoder at startup | Feasible if sensor is legal and mapped | EI |
| Known match-start pose | REV recommends repeatable init pose[^rev-enc] | VF (recommendation) |
| Hard-stop stall home | Risky; stall current + structure | EI; only if mechanically safe and rules-compliant |
| Redundant agreement | Team code | EI |

TRC advertises zero-position calibration on `TrcMotor`.[^trc] **OI** of README, not a line-by-line source audit of every strategy.

---

## 7. Soft versus hard limits

| Kind | Meaning | Who enforces on FTC Hub? |
|------|---------|---------------------------|
| Hard limit | Physical stop or switch that must not be driven further into | Switch + team code (or SPARK firmware on FRC) |
| Soft limit | Software position bound | **Team code** on Hub ports |

**VF (FRC):** SPARK `SoftLimitConfig` disables actuation past a feedback position **on the SPARK**.[^revlib-soft]  
**VF (FRC):** TalonFX Motion Magic can ignore software limits during calibration (`OverrideSoftLimits`).[^ctre-mm]

**EI:** FTC Hub has no equivalent onboard soft-limit register for `DcMotor` ports. MIMIC Phase 3 must implement direction-aware blocking in software, with a final safety gate.

---

## 8. Feedback, feedforward, gravity, profiling

**VF:** GM0 control-loops chapter covers PID intuition for FTC.[^gm0-pid]  
**VF:** WPILib `ProfiledPIDController` follows a trapezoid toward a **goal**; the setpoint is not the user’s instantaneous target.[^wpilib-ppid]  
**VF:** WPILib elevator FF: \(V = K_g + K_s \mathrm{sgn}(\dot d) + K_v \dot d + K_a \ddot d\); arm FF multiplies \(K_g\) by \(\cos\theta\).[^wpilib-ff][^wpilib-sysid]  
**OI:** NextControl slide example uses `posPid` + `elevatorFF` without a profile in the published snippet.[^next-slides]  
**VF:** NextControl trapezoid interpolator PR #8 closed unmerged (2026-08-17).

**EI:** Gravity compensation on a **counterbalanced** elevator is smaller and can reverse if the counterbalance fails — do not copy FRC `kG` blindly (see [elevator-target.md](elevator-target.md)).

---

## 9. Actuator following, synchronized independents, anti-racking

| Arrangement | Appropriate control | Label |
|-------------|---------------------|-------|
| Two motors, one shaft | Leader/follower **commands** (same power/velocity). Independent PID can fight the shaft | EI |
| Two towers, independent sensing, coupled carriage | Position-difference monitor + bounded correction or shutdown | EI |
| Mechanically independent sides | True dual-loop sync | EI |

TRC advertises “multiple motors with synchronization (motor followers).”[^trc] **OI** README.

**UH:** Anti-racking gains without a jam detector can rack the structure harder. Phase 5 must bound correction and prefer shutdown.

---

## 10. Stall detection

**EI:** Current high + velocity near zero for a timeout is a common heuristic.  
**VF:** `DcMotorEx.getCurrent` exists; not bulk-read.[^gm0-motors][^javadoc-motor]  
**Latency:** **UH/MR** — Hub sample delay vs mechanical stall. Do not treat current-only stall as instantaneous.

TRC advertises stall protection that cuts power and optional reset timeout.[^trc]

---

## 11. Mechanism state machines and interlocks

**VF:** GM0 documents FSMs for sequencing.[^gm0-fsm]  
**EI:** Semantic states (`STOWED`, `SCORING`) should **request goals**, not write power.  
**EI:** Two safe mechanisms can collide (elevator down vs intake up). Named interlocks belong in software tests, not scattered `if`s.

---

## 12. Failure detection, recovery, simulation, characterization

| Topic | FTC today | Label |
|-------|-----------|-------|
| Simulation | Desktop fake hardware (this repo); WPILib sim is FRC | VF / EI |
| SysId | WPILib PC app + roboRIO routine[^wpilib-sysid] | VF FRC; not FTC-native |
| Characterization | Log voltage/effort vs velocity; export CSV | EI |
| Recovery | Bounded retries vs stop | EI |

**EI:** Do not recreate all of SysId in Phase 0–4. Prefer compatible data export.

---

## 13. What FRC smart controllers do locally vs what FTC must do in the loop

| Feature | Where it runs (FRC) | Current FTC Hub |
|---------|---------------------|-----------------|
| Motion Magic trapezoid + kG | TalonFX firmware[^ctre-mm] | Team loop (or Hub `RUN_TO_POSITION` without a true profile) |
| SPARK soft limits / limit switches | SPARK firmware[^revlib-soft][^revlib-ls] | Team loop + DIO |
| Supply/stator current limits | TalonFX firmware (see AMPER research) | Team effort cap / AMPER |
| 1 kHz+ inner loop | Motor controller | OpMode ~50–80 Hz typical (**EI**) |

**VF:** Hub `RUN_TO_POSITION` **does** run on the Lynx module, not in Java — but it is not Motion Magic and still needs a valid target and encoder zero.[^rev-enc]

---

## 14. What YAMS / TRC / NextControl provide

See [build-vs-adopt.md](build-vs-adopt.md) for the decision. Summary:

| Library | Provides | Does not provide for Allsparks FTC |
|---------|----------|-------------------------------------|
| YAMS | FRC mechanism classes, SPARK/Talon wrappers, WPILib sim | Control Hub `DcMotor`, AMPER, phased FTC education |
| TRC | Homing, limits, stall, followers inside a full framework | Modular drop-in without adopting TRC |
| NextControl | PID, elevator/arm FF, filters; interpolator architecture | Calibration lifecycle, interlocks, AMPER, merged trapezoid, MIT-safe dependency |

---

## 15. What MIMIC should and should not provide

**Should:** observation, units, calibration policy, goal permission, interlocks, faults, safety gate, AMPER requests, educational phases.

**Should not:** Pedro pathing, ViDAR perception, AMPER power policy, global scheduler, another general PID implementation as the product.

---

## 16. Capability matrix

| # | Category | Examples | MIMIC implication |
|---|----------|----------|-------------------|
| 1 | FRC smart-controller firmware | Motion Magic, SPARK soft limits | Study only |
| 2 | WPILib | ProfiledPID, SysId, ElevatorFeedforward | Study / optional math ideas |
| 3 | FTC libraries | NextControl, FTCLib TrapezoidProfile, TRC TrcMotor | Adapters or non-goals |
| 4 | Current FTC SDK + REV Hub | RunMode, encoders, DIO, `getCurrent` | Phase 0 adapters |
| 5 | Team-level FTC code | Homing FSM, interlocks, safety gate | MIMIC core (later phases) |
| 6 | Future SystemCore | Unknown mechanism APIs | **FH** — `SystemCoreAdapterBoundary` only |
| 7 | Proposed without production precedent | AMPER-aware profile rewind + named interlock solver | Experimental phases |

---

## Footnotes

[^rev-enc]: REV Robotics, “Encoder Basics.” https://docs.revrobotics.com/duo-control/hello-robot-java/part-3/using-encoder
[^gm0-motors]: Game Manual 0, “SDK Motors.” https://gm0.org/en/latest/docs/software/adv-control-system/sdk-motors.html
[^gm0-pid]: Game Manual 0, “Control Loops.” https://gm0.org/en/latest/docs/software/concepts/control-loops.html
[^gm0-fsm]: Game Manual 0, “Finite State Machines.” https://gm0.org/en/latest/docs/software/concepts/finite-state-machines.html
[^javadoc-runmode]: RobotCore Javadoc, `DcMotor.RunMode`. https://javadoc.io/doc/org.firstinspires.ftc/RobotCore
[^javadoc-motor]: RobotCore Javadoc, `DcMotorEx`. https://javadoc.io/doc/org.firstinspires.ftc/RobotCore
[^javadoc-dio]: RobotCore 11.2.0, `DigitalChannel`. https://javadoc.io/doc/org.firstinspires.ftc/RobotCore/11.2.0
[^issue-913]: FtcRobotController #913, RUN_TO_POSITION command cycle. https://github.com/FIRST-Tech-Challenge/FtcRobotController/issues/913
[^ftc-forum-reset]: FTC Community, STOP_AND_RESET_ENCODER discussion. https://ftc-community.firstinspires.org/t/stop-and-reset-encoder-sometimes-doesnt-stop-motor/922
[^revlib-ls]: REVLib, `LimitSwitchConfig`. https://codedocs.revrobotics.com/java/com/revrobotics/spark/config/limitswitchconfig
[^revlib-soft]: REVLib, `SoftLimitConfig`. https://codedocs.revrobotics.com/java/com/revrobotics/spark/config/softlimitconfig
[^ctre-mm]: CTRE Phoenix 6, Motion Magic. https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/device-specific/talonfx/motion-magic.html
[^wpilib-ppid]: WPILib, Profiled PID. https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/profiled-pidcontroller.html
[^wpilib-ff]: WPILib, Feedforward. https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/feedforward.html
[^wpilib-sysid]: WPILib, SysId introduction. https://docs.wpilib.org/en/stable/docs/software/advanced-controls/system-identification/introduction.html
[^next-slides]: NextFTC, Linear Slides Example. https://nextftc.dev/control/examples/slides
[^trc]: trc492/FtcTemplate README. https://github.com/trc492/FtcTemplate
