# Interlocks

**Phase 7 — not implemented.**

## Problem

Two mechanisms can each be inside their own soft limits and still collide, unspool a cable, or block a camera.

## Constraint style

Prefer **named, testable** constraints over scattered conditionals.

Examples (illustrative — not Allsparks CAD):

- intake must not feed the turret while the color sensor reports reject
- feeder motion requires intake not jammed
- hood motion requires turret not slewing through a blocked zone
- turret slew requires hood inside a safe band
- flower-tray extension requires turret stowed or a measured clearance
- drivetrain speed depends on extension
- mechanism must not obstruct a critical sensor (ViDAR cameras)

## Outcomes

`REJECTED`, `DEFERRED`, `CLAMPED`, `REPLACED` (safe intermediate), or request driver confirmation.

Every intervention has a reason string. Generated intermediates must not cycle.

## Non-goals

Interlocks do not schedule the match. They only permit or reshape mechanism goals.
