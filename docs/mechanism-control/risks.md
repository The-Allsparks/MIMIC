# Risks and unresolved questions

## Unresolved (need team input)

1. Final elevator drive: shared capstan vs independent towers vs common shaft?
2. Which sensors are actually purchased (through-bore, analog, DIO limits, ratchet sense)?
3. Ratchet: powered release, servo, or manual? Sensed or not?
4. Counterbalance characteristics after power loss.
5. Command framework choice (Ivy vs NextFTC vs other) for Phase 6.
6. Whether Allsparks will ever add NextControl in TeamCode despite GPL.

## Technical risks

| Risk | Mitigation |
|------|------------|
| Students enable Phase 4 flags on a real robot | Session refuses actuation flags; docs; PR template |
| Invented hardware in code | [elevator-target.md](elevator-target.md) lists unknowns |
| GPL NextControl accidentally added to `build.gradle` | License section; CONTRIBUTING |
| Claiming FRC firmware as FTC | Research labels; review checklist |
| Current polling blows loop time | Document GM0; measure |
| Encoder ports 1–2 lose high-CPR counts | GM0 warning; prefer ports 0/3 |
| Counterbalance failure + zero \(k_G\) | Never assume hold is free |

## Process

Do not merge active-control PRs without explicit authorization. This scaffold’s draft PR should remain draft until maintainers accept Phase 0.
