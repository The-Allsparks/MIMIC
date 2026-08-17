# Fault handling

**Phase 8 — not implemented.**

## Detectors (design)

- output without expected motion
- motion without expected output
- impossible sensor jumps
- limit disagreement
- actuator desynchronization
- prolonged target error
- stall patterns (current + no motion + timeout)
- stale data
- calibration loss
- repeated timeout

## Severity

| Level | Meaning |
|-------|---------|
| `INFO` | Log only |
| `DEGRADED` | Reduced speed or disabled optional DOF |
| `STOP_MECHANISM` | Zero this mechanism (hold policy if gravity-critical) |
| `STOP_DEPENDENCIES` | Stop coupled mechanisms |
| `STOP_ROBOT` | Request full stop; never bypass FTC e-stop |

## Bounded recovery

Automatic retry, reduced speed, re-home, driver-confirmed recovery, pit-only reset, disable until reboot.

Unresolved physical faults cannot be hidden by clearing a flag. Retries are counted. Fault history is retained. Recovery must not hammer a hard stop.

## Phase 0

Invalid sensing is logged (`SENSOR_INVALID`) and status becomes `DEGRADED`. No recovery motion.
