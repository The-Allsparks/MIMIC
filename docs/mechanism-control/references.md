# References

Access date: **2026-08-17** unless noted. Prefer primary sources.

| Title | Org / author | URL | Date / rev | Hardware / software gen | Claim supported | Directly FTC? | Limitations |
|-------|--------------|-----|------------|-------------------------|-----------------|---------------|-------------|
| Encoder Basics | REV Robotics | https://docs.revrobotics.com/duo-control/hello-robot-java/part-3/using-encoder | Retrieved 2026-08-17 | REV Hub + FTC SDK | RunMode usage; quadrature vs absolute; init reset | Yes | Tutorial, not a full spec |
| Duo Control home | REV Robotics | https://docs.revrobotics.com/duo-control/ | Retrieved 2026-08-17 | Duo Control | Canonical REV FTC docs | Yes | Evolving product lines |
| FTC Docs | FIRST | https://ftc-docs.firstinspires.org/ | Retrieved 2026-08-17 | FTC | Official portal | Yes | Broad |
| FtcRobotController | FIRST | https://github.com/FIRST-Tech-Challenge/FtcRobotController | v11.0+ DECODE season | FTC SDK | Canonical SDK | Yes | Season branches differ |
| RobotCore Javadoc | FIRST / javadoc.io | https://javadoc.io/doc/org.firstinspires.ftc/RobotCore | 11.2.0 seen | RobotCore | RunMode, DcMotorEx, DigitalChannel | Yes | Pin Maven version in team notes |
| SDK Motors | Game Manual 0 | https://gm0.org/en/latest/docs/software/adv-control-system/sdk-motors.html | Retrieved 2026-08-17 | REV Hub | PWM; BRAKE/FLOAT; current not bulk-read; encoder ports 0/3 vs 1/2 | Yes (community) | Wiki — verify vs SDK |
| Control Loops | Game Manual 0 | https://gm0.org/en/latest/docs/software/concepts/control-loops.html | Retrieved 2026-08-17 | FTC | PID teaching | Yes (community) | Conceptual |
| Finite State Machines | Game Manual 0 | https://gm0.org/en/latest/docs/software/concepts/finite-state-machines.html | Retrieved 2026-08-17 | FTC | FSM teaching | Yes (community) | Conceptual |
| Control Systems | NextFTC | https://nextftc.dev/control/usage/control-systems | Retrieved 2026-08-17 | NextControl | Four elements; elevator/arm FF | Yes | Docs may mention unimplemented interpolators |
| Linear Slides Example | NextFTC | https://nextftc.dev/control/examples/slides | Retrieved 2026-08-17 | NextControl | posPid + elevatorFF loop | Yes | Hypothetical gains |
| NextControl | NextFTC | https://nextftc.dev/control/ | Retrieved 2026-08-17 | FTC | Library intro | Yes | GPL-3.0 |
| NextControl source | NextFTC | https://github.com/NextFTC/NextControl | main, 2026-08-17 | Kotlin | GPL-3.0; interpolators on main | Yes | Trapezoid PR #8 closed unmerged |
| YAMS | YASS | https://github.com/Yet-Another-Software-Suite/YAMS | LGPL-3.0 | FRC WPILib 2026 | SPARK/TalonFX mechanisms | Name says FTC; **impl FRC** | Not Control Hub motors |
| YAMS docs | YASS | https://yagsl.gitbook.io/yams | Retrieved 2026-08-17 | FRC | WPILib command-based mechanisms | No | FRC hardware |
| FtcTemplate | Titan Robotics (trc492) | https://github.com/trc492/FtcTemplate | MIT | FTC framework | TrcMotor limits, stall, sync, homing | Yes | Whole framework; README not a file-level audit |
| FTCLib | FTCLib | https://github.com/FTCLib/FTCLib | FIRST BSD (releases note) | FTC | PIDF, TrapezoidProfile | Yes | Broad library |
| Profiled PID | WPILib | https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/profiled-pidcontroller.html | Stable | FRC | Goal vs setpoint | No | FRC |
| Trapezoidal profiles | WPILib | https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/trapezoidal-profiles.html | Stable | FRC | Profile API | No | FRC |
| Feedforward | WPILib | https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/feedforward.html | Stable | FRC | Elevator/arm FF | Partial | Units differ on FTC |
| SysId | WPILib | https://docs.wpilib.org/en/stable/docs/software/advanced-controls/system-identification/introduction.html | Stable | FRC | Characterization tool | No | roboRIO workflow |
| Motion Magic | CTRE | https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/device-specific/talonfx/motion-magic.html | Phoenix 6 | Talon FX | Onboard profiles | No | Not REV Hub ports |
| SPARK soft limits | REVLib | https://codedocs.revrobotics.com/java/com/revrobotics/spark/config/softlimitconfig | FRC-REVLib | SPARK | Firmware soft limits | No | FRC device |
| SPARK limit switches | REVLib | https://codedocs.revrobotics.com/java/com/revrobotics/spark/config/limitswitchconfig | FRC-REVLib | SPARK | Firmware limit actions | No | FRC device |
| RUN_TO_POSITION cycle | FIRST GitHub | https://github.com/FIRST-Tech-Challenge/FtcRobotController/issues/913 | Issue thread | FTC SDK | Command order | Yes | Historical firmware sensitivity |
| STOP_AND_RESET discussion | FTC Community | https://ftc-community.firstinspires.org/t/stop-and-reset-encoder-sometimes-doesnt-stop-motor/922 | Forum | FTC | Reset side effects | Yes | Forum, not spec |
| AMPER | The Allsparks | https://github.com/The-Allsparks/AMPER | 2026 | FTC | Power grant contract sibling | Yes | Electrical domain |
| ViDAR | The Allsparks | https://github.com/The-Allsparks/ViDAR | 2026 | FTC | Perception sibling | Yes | Vision domain |

When citing public team code later, link a **stable commit** and file path, and confirm the snippet implements the claimed behavior.
