# BIOBUZZ mechanism targets (do not invent hardware)

Shop lock 15 Sep 2026: BumbleBee will **not** build an elevator this season.

Until parts are selected, MIMIC implements **no mechanism-specific controller**. Observe with generic `MechanismUnits` and `MimicSession`. Extract reusable classes only after the real mechanism exists.

## Intended mechanisms (planning, not BOM)

| Mechanism | Role | Sensors / notes | Status |
|-----------|------|-----------------|--------|
| Intake | Acquire NECTAR | Color sensor to **reject** wrong-alliance NECTAR | Hardware unselected |
| Feeder | Feed the turret | Must not jam against a rejected piece | Hardware unselected |
| Turret | Aim | Controllable yaw. Interlock with hood and feeder | Hardware unselected |
| Hood | Launch angle | Controllable. Interlock with turret motion | Hardware unselected |
| Flower tray (optional) | Extension to feed a FLOWER | Stretch. Do not start until G1/G2 say it is required | Hardware unselected |

Do not invent motors, gear ratios, color-sensor ports, or PID gains here.

## Explicitly not this season

A counterbalanced elevator / capstan lift is **out**. Historical notes: [elevator-target.md](elevator-target.md).

## Must be documented before mechanism code

| Question | Status |
|----------|--------|
| Intake roller vs belt vs other | **Unknown** |
| Color sensor part and reject mechanic (spit vs divert) | **Unknown** |
| Turret travel and hard stops | **Unknown** |
| Hood travel and hard stops | **Unknown** |
| Feeder vs turret collision | **Unknown** |
| Flower-tray stroke and wall clearance | **Unknown** |
| Homing direction per axis | **Unknown** — do not guess |
| Behavior after power loss | **Unknown** |

## Phase 0 use

Observe whatever motor, servo, and color sensor you eventually wire. Named interlocks (intake reject vs feeder, turret vs hood) wait until the physical layout is known.

G1 Strategy (S002) still owns whether the turret/hood path is the MVP. This file records software intent so MIMIC does not keep an elevator as the first target.
