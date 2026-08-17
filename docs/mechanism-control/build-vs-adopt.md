# Build versus adopt

**Access date:** 2026-08-17  
**Decision:** **Build MIMIC as a standalone MIT lifecycle/safety layer.** Do not implement a competing PID library, command scheduler, or path follower. Do not ship redundant production control code in this scaffold.

Label key: **VF** verified fact · **EI** engineering inference · **UH** untested hypothesis

---

## 1. Can YAMS meet the requirements?

**No, not on current FTC Control Hub robots. (VF for hardware target; EI for “announced FTC” gap)**

- GitHub description says “Yet Another Mechanism System for FRC and FTC.”[^yams-repo]
- Official docs and Java overview describe a **WPILib 2026** vendordep over **REV SPARK MAX/FLEX** and **CTRE TalonFX/TalonFXS**, with NetworkTables telemetry and `simIterate()`.[^yams-docs][^yams-overview]
- Example construction uses `TalonFXS`, `ArmFeedforward`, WPILib units, and command-based subsystems.[^yams-docs]
- License: **LGPL-3.0**.[^yams-repo]
- A tree search for Control Hub / `DcMotor` / Lynx adapters was not established in this review; published API surface is FRC smart-controller wrappers.

YAMS is an excellent **FRC** mechanism product. It does not provide FTC-first calibration lifecycle, REV Hub digital limits, AMPER grants, or a phased educational enablement path for Duo Control.

**Upstream contribution?** Possible later if YAMS grows a real FTC adapter. That is not a substitute for Allsparks Phase 0 observation today.

---

## 2. Can TRC supply the behavior without adopting the whole framework?

**Partially, but not as a modular library. (VF for advertised features; EI for adoption cost)**

Titan Robotics’ `FtcTemplate` README documents `TrcMotor` features that overlap MIMIC’s later phases: limit switches, stall protection, motor synchronization/followers, zero-position calibration, gravity compensation, PIDF with joystick speed control, and voltage compensation.[^trc-template]

Those capabilities live inside a **complete robot framework** (motors, servos, lights, task scheduling). Extracting only “elevator safety” without taking TRC’s architecture is not the published product.

License: **MIT** — compatible if we *used* TRC, but using it would replace rather than complement Ivy/NextFTC/Pedro choices.

**Upstream contribution?** High-value if Allsparks were already a TRC shop. We are not requiring that.

---

## 3. Can NextControl provide feedback, feedforward, filtering, and motion-profile mathematics?

**Feedback, feedforward, and filters: yes, as an optional future adapter. Trapezoidal profiles: not verified on `main`. License: GPL-3.0 — do not compile into MIT MIMIC. (VF)**

NextControl is an FTC control library whose `ControlSystem` is four elements: feedback, feedforward, filter, interpolator.[^next-cs] Documented feedforwards include basic, elevator, and arm.[^next-cs] The linear-slide example is positional PID plus `elevatorFF`, writing `motor.power` every loop.[^next-slides]

Interpolator docs mention trapezoidal profiles as an example.[^next-cs] Source on `main` includes `ConstantInterpolator` and EMA interpolators. Pull request [#8](https://github.com/NextFTC/NextControl/pull/8) “Trapezoidal motion profiles yay” is **closed and not merged** (`merged_at: null`, 2026-08-17 REST check). Issue [#5](https://github.com/NextFTC/NextControl/issues/5) still listed trapezoidal interpolators as unchecked work in June 2025.

**License:** GNU GPL v3.0.[^next-repo] Linking NextControl into MIMIC’s published artifact would impose GPL obligations on the combination. MIMIC therefore:

- does **not** depend on `dev.nextftc:control`;
- may later document a TeamCode-side adapter that teams add themselves, with a license warning;
- will keep a minimal internal profile/controller for tests if Phase 4 proceeds.

---

## 4. Can FTCLib or another library supply primitives?

**Yes, optionally. (VF)**

FTCLib includes `PIDFController`, `TrapezoidProfile`, and `ProfiledPIDController` (WPILib-style ports).[^ftclib] License moved from MIT to **FIRST BSD** to follow WPILib guidelines.[^ftclib-rel] FIRST BSD is compatible with MIT MIMIC **if** we later vendor or depend on it with attribution.

FTCLib also ships a command framework. MIMIC must not pull the whole “last library you need” stack.

Pedro Pathing remains the chassis motion library. AMPER remains electrical policy. Neither should be reimplemented.

---

## 5. What should MIMIC delegate through adapters?

| Capability | Delegate to |
|------------|-------------|
| PID / SquID / FF / filters | NextControl adapter (TeamCode, license-aware) or FTCLib primitives or a tiny test controller |
| Chassis pathing | Pedro Pathing |
| Field perception | ViDAR |
| Robot-wide power allocation | AMPER (`AmperPowerRequest` / `AmperPowerGrant`) |
| Global command scheduling | Ivy, NextFTC, FTCLib commands, or team scheduler |
| Hub motor firmware velocity/position | FTC SDK `RUN_USING_ENCODER` / `RUN_TO_POSITION` when a team chooses that mode — MIMIC still owns permission/safety |

---

## 6. What should MIMIC implement itself?

- Immutable observation and units
- Calibration lifecycle and homing *policies*
- Goal validation with named reasons
- Interlocks and fault semantics
- Final actuator safety gate (Phase 3+)
- AMPER request construction and grant application without letting AMPER bypass limits
- Feature flags, logging, tests, educational docs

---

## 7. Would contributing upstream create more value than a new project?

| Upstream | Verdict |
|----------|---------|
| YAMS | Wrong hardware generation for current FTC |
| TRC | Would require Allsparks to adopt TRC |
| NextControl | Valuable for trapezoid interpolator / Kalman; **does not** own calibration, interlocks, AMPER, or safety gates. GPL complicates a combined library. |
| FTCLib | Primitives exist; lifecycle/safety layer does not |

Contributing trapezoidal interpolators to NextControl is worthwhile **in addition to** MIMIC, not instead of it.

---

## 8. Durable differentiator

MIMIC’s job is: **Is this mechanism allowed to move, and what is the last safe output?** Existing FTC libraries answer **how to compute a power from error**.

---

## 9. Maintenance burden

A standalone MIT library matching AMPER’s shape (Java 11, no SDK on CI, phased flags) is maintainable by one team. Bundling YAMS+WPILib or GPL NextControl would dominate legal and API churn. Keep the core small.

---

## 10. License compatibility

| Dependency | License | MIMIC MIT compatibility |
|------------|---------|-------------------------|
| FTC SDK (TeamCode copy, not shipped here) | FIRST / season SDK terms | Use on robot; not vendored in this repo |
| AMPER / ViDAR | MIT | Compatible |
| TRC | MIT | Compatible if used; not adopted |
| FTCLib | FIRST BSD | Compatible with attribution |
| YAMS | LGPL-3.0 | Linkable with LGPL conditions; **wrong platform** |
| NextControl / NextFTC | GPL-3.0 | **Do not ship as MIMIC dependency** |
| WPILib | WPILib/BSD-style | FRC; not a Control Hub dependency |

No copy of YAMS, NextControl, or WPILib source is included in this repository.

---

## Stop condition

Research did **not** find a maintained FTC-compatible project that already provides modular calibration, interlocks, fault semantics, AMPER contracts, and phased enablement **without** taking over the robot’s scheduler. Creating MIMIC as a layer is justified. Creating another general controller is not.

[^yams-repo]: Yet-Another-Software-Suite/YAMS, GitHub, LGPL-3.0, description “for FRC and FTC.” https://github.com/Yet-Another-Software-Suite/YAMS
[^yams-docs]: YAMS documentation home. https://yagsl.gitbook.io/yams
[^yams-overview]: `yams/java/overview.html` — WPILib 2026, SPARK and TalonFX.
[^trc-template]: trc492/FtcTemplate README, TrcMotor feature list. https://github.com/trc492/FtcTemplate
[^next-cs]: NextFTC, “Control Systems.” https://nextftc.dev/control/usage/control-systems
[^next-slides]: NextFTC, “Linear Slides Example.” https://nextftc.dev/control/examples/slides
[^next-repo]: NextFTC/NextControl, GPL-3.0. https://github.com/NextFTC/NextControl
[^ftclib]: FTCLib/FTCLib. https://github.com/FTCLib/FTCLib
[^ftclib-rel]: FTCLib releases note FIRST BSD switch. https://github.com/FTCLib/FTCLib/releases
