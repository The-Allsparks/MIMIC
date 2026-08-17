# Glossary

| Term | Meaning in MIMIC |
|------|------------------|
| **Actuator** | Motor or servo that can produce motion or hold. |
| **Mechanism** | One coordinated degree of freedom (elevator, arm, extension, …), possibly with multiple actuators. |
| **Encoder** | Sensor that reports motion; usually quadrature ticks on FTC motors. |
| **Absolute encoder** | Sensor with a fixed origin (“clock”), not just a count since reset. |
| **Incremental encoder** | Quadrature count (“stopwatch”) from a chosen zero. |
| **Calibration** | Trust that software pose matches physical pose. |
| **Homing** | Procedure that establishes calibration. |
| **Zeroing** | Relabeling the current count as zero; not automatically a home. |
| **Hard limit** | Physical stop or switch that must not be driven further into. |
| **Soft limit** | Software position bound. |
| **Debounce** | Requiring a switch to stay asserted before trusting it. |
| **Feedback** | Correction from measured error (e.g. PID). |
| **Feedforward** | Open-loop term from the model (gravity, friction, \(k_V\), \(k_A\)). |
| **Gravity compensation** | Feedforward that counters gravity (constant or \(\cos\theta\)). |
| **Motion profile** | Time-varying setpoint that respects max velocity/acceleration. |
| **Setpoint** | Instantaneous reference the controller tracks (may differ from the goal). |
| **Saturation** | Output clipped at a limit. |
| **Integral windup** | Integral growing while saturated, causing overshoot when released. |
| **Interlock** | Named constraint involving this mechanism and another state. |
| **Backdrive** | External torque turning the actuator. |
| **Stall** | Commanded effort with little or no motion, often high current. |
| **Synchronization** | Keeping multiple actuators consistent. |
| **Anti-racking** | Preventing structural twist from side-to-side disagreement. |
| **Degraded operation** | Reduced capability while remaining as safe as possible. |

Student exercise: pick three terms and give an elevator example for each.
