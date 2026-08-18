# Interlocks

**Phase 7 — not implemented.**

## Problem

Two mechanisms can each be inside their own soft limits and still collide, unspool a cable, or block a camera.

## Constraint style

Prefer **named, testable** constraints over scattered conditionals.

Examples (illustrative — not Allsparks CAD):

- elevator descent requires intake clearance
- arm motion requires elevator height
- extension limit depends on elevator height
- ratchet release precedes powered descent
- drivetrain speed depends on extension
- mechanism must not obstruct a critical sensor (ViDAR cameras)

## Outcomes

`REJECTED`, `DEFERRED`, `CLAMPED`, `REPLACED` (safe intermediate), or request driver confirmation.

Every intervention has a reason string. Generated intermediates must not cycle.

## Non-goals

Interlocks do not schedule the match. They only permit or reshape mechanism goals.
