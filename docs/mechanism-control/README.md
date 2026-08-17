# Mechanism control — student entry

MIMIC teaches **how a mechanism is known, permitted, moved, and stopped** — not how to drive the chassis or see the field.

## What problem this solves

FTC teams often copy `RUN_TO_POSITION`, a PID loop, or a state machine into every subsystem. Those pieces can work, but they do not answer:

- Is this encoder count a real physical pose?
- Is the requested height legal given the intake?
- What happens if a limit switch bounces or disconnects?
- Who is allowed to write motor power last?

## How to use these docs

1. Read [glossary.md](glossary.md) for vocabulary.
2. Read [research.md](research.md) to see what existing libraries already do.
3. Read [build-vs-adopt.md](build-vs-adopt.md) for why MIMIC is a layer, not another PID library.
4. Follow [phases.md](phases.md). Enable one phase at a time.
5. Use [testing.md](testing.md) before touching a real elevator.

Maintainers: the [initial deep audit](../audits/initial-deep-audit.md) and [priority ledger](../audits/priority-ledger.md) record current maturity and work order.

## Teachable question for Phase 0

**What information does safe mechanism control require?**

You should be able to graph position, velocity, commanded output, and limit-switch state from a recording **without** MIMIC moving the mechanism.
