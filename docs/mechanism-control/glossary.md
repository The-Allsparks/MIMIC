# Glossary

| Term | Meaning in MIMIC |
|------|------------------|
| **Actuator** | Motor or servo that can produce motion or hold. |
| **Mechanism** | One coordinated degree of freedom (elevator, turret, feeder, …), possibly with multiple actuators. |
| **Family** | Job of a mechanism: intake, transfer, launcher, lift, arm, end effector, climber, field element, or passive. See [mechanism-kinds.md](mechanism-kinds.md). |
| **Construct** | Standard layout under a family, or a custom `ConstructDescriptor`. |
| **Transfer** | Internal path that moves a piece through the robot. Prefer this over "shoot". Launching off the robot is the launcher family. Aim (turret, hood) is the arm family. |
| **Blueprint** | Catalog id + construct. Metadata only; it does not write hardware. |
| **Configuration** | Immutable instance metadata: topology, sensor roles, capabilities, named-state names, calibration/limit/sync/control declarations. Validated; never writes hardware. |
| **Actuator topology** | How motors/servos are arranged. Independent of construct. Count greater than one does not imply synchronization. Linked motors share a command; two independent towers need two sensors. |
| **Sync contract** | Declared independent-actuator disagreement limit and stop action. Not anti-racking correction and not permission to move. |
| **Sensor role** | Job of a measurement (home, piece entry, redundant position). Not an FTC device class. |
| **Encoder** | Sensor that reports motion; usually quadrature ticks on FTC motors. |
| **Absolute encoder** | Sensor with a fixed origin (“clock”), not just a count since reset. |
| **Incremental encoder** | Quadrature count (“stopwatch”) from a chosen zero. |
| **Calibration** | Trust that software pose matches physical pose. |
| **Calibration contract** | Declared homing bounds (direction, max travel, timeout, debounce, encoder reset policy). Not permission to move. |
| **Homing** | Procedure that establishes calibration. |
| **Zeroing** | Relabeling the current count as zero; not automatically a home. |
| **Hard limit** | Physical stop or switch that must not be driven further into. |
| **Soft limit** | Software position bound. |
| **Limit contract** | Declared soft/hard bounds, stopping margin, wrap policy, and missing-switch rule. Not the Phase 3 actuator safety gate and not permission to move. |
| **Debounce** | Requiring a switch to stay asserted before trusting it. |
| **Feedback** | Correction from measured error (e.g. PID). |
| **Feedforward** | Open-loop term from the model (gravity, friction, \(k_V\), \(k_A\)). |
| **Gravity compensation** | Feedforward that counters gravity (constant or \(\cos\theta\)). |
| **Motion profile** | Time-varying setpoint that respects max velocity/acceleration. |
| **Setpoint** | Instantaneous reference the controller tracks (may differ from the goal). Java type `Setpoint` (`position`, `velocity`, `unitSymbol`); not permission to move. |
| **Controller adapter** | `MechanismControllerAdapter`: setpoint in, effort out, no hardware. Implementations live in TeamCode or tests, not as MIMIC compile dependencies. |
| **Saturation** | Output clipped at a limit. |
| **Integral windup** | Integral growing while saturated, causing overshoot when released. |
| **Interlock** | Named constraint involving this mechanism and another state. |
| **Backdrive** | External torque turning the actuator. |
| **Stall** | High current plus no motion for a timeout, not a single current sample. Observe-only `StallDetector` / `StallSuspicion`. Missing current is unsupported, not stalled. Not a reverse-clear. |
| **Jam** | Same observe heuristic as stall (`JamDetector` / `JamSuspicion`). Reverse-clear remains forbidden. |
| **Synchronization** | Keeping multiple actuators consistent. |
| **Anti-racking** | Preventing structural twist from side-to-side disagreement. |
| **Degraded operation** | Reduced capability while remaining as safe as possible. |
| **sensorValid** | Aggregate: required wired channels usable and not disagreeing. Primary pose is required (omitted `ticks` keeps it false). Velocity is required only if `ticksPerSecond` is wired. |
| **STALE** | Observer not called within `staleAfterNanos` (loop-call gap). Not Hub sample age and not “encoder disconnected.” Frozen supplier values on a timely loop stay `VALID`. |

Student exercise: pick three terms and give an elevator example for each.
