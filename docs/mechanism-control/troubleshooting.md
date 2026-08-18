# Troubleshooting

| Symptom | Likely cause | What to do |
|---------|--------------|------------|
| Position always 0 | Encoder not plugged, wrong motor, or reset every loop | Phase 0 graphs; do not home |
| Position sign wrong | `DirectionSign` or motor invert mismatch | Fix mapping; re-record |
| `MISSING` limit | Supplier threw (disconnected) | Check DIO wiring; NC vs NO |
| `UNSUPPORTED` current | No current supplier wired | Optional; measure overhead before adding |
| `UNSUPPORTED` velocity | No `ticksPerSecond` supplier wired | Optional; a position-only observer can still be `sensorValid` |
| `UNSUPPORTED` position / omitted ticks | No `.ticks(...)` supplier | Primary pose is required; snapshot stays invalid |
| Analog-only looks `sensorValid=false` | Analog wired only as `absoluteSensor` | Wire the mapped analog value as `.ticks(...)` and omit `.ticksPerSecond(...)`; `absoluteSensor` is a second channel |
| Disagreement invalidates snapshot | Redundant encoder offset or one side slipping | Do not average blindly |
| Loop time high | `getCurrent` on many motors | Poll slower ([GM0](https://gm0.org/en/latest/docs/software/adv-control-system/sdk-motors.html)) |
| Goal always rejected | Phase 0 `NO_ACTIVE_CONTROL` | Expected |
| Session constructor throws | Actuation flag enabled | Use `defaults()` |
| Mechanism moved unexpectedly | **Not MIMIC Phase 0** — your OpMode wrote power | Confirm `FakeActuator` tests; on robot, search `setPower` |

More hazards: [safety-model.md](safety-model.md).
