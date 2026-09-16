# Interlocks

**Phase 7 — not implemented.**

## Problem

Two mechanisms can each be inside their own soft limits and still collide, unspool a cable, or block a camera.

## Constraint style

Prefer **named, testable** constraints over scattered conditionals.

Examples (illustrative, not a robot CAD):

- intake must not feed while a reject sensor is true
- feeder motion requires intake not jammed
- hood motion requires turret not slewing through a blocked zone
- turret slew requires hood inside a safe band
- an extension axis requires turret stowed or a measured clearance
- drivetrain speed depends on extension
- mechanism must not obstruct a critical sensor (ViDAR cameras)

Named BIOBUZZ constraints (NECTAR color reject, FLOWER tray, BumbleBee port names) belong in TeamCode. See [library-vs-teamcode.md](library-vs-teamcode.md).

## Outcomes

`REJECTED`, `DEFERRED`, `CLAMPED`, `REPLACED` (safe intermediate), or request driver confirmation.

Every intervention has a reason string. Generated intermediates must not cycle.

## Non-goals

Interlocks do not schedule the match. They only permit or reshape mechanism goals.
