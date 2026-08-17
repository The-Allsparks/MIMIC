# AMPER integration

**Phase 9 — not implemented.** Types `AmperPowerRequest` and `AmperPowerGrant` exist so the contract can be reviewed.

## Ownership

- **MIMIC** owns mechanism safety (limits, interlocks, calibration, gate).
- **AMPER** owns global power allocation.
- AMPER **must not** command mechanism hardware.

## MIMIC may submit

requested effort, estimated current, safe minimum holding effort, gravity-critical, interruptibility, acceptable delay, expected duration, motion phase.

## After a grant

1. Apply allowed effort.
2. Prevent controller windup.
3. Pause or slow the profile if needed.
4. **Reject electrically allowed but mechanically unsafe** motion (gate still wins).
5. Log requested vs applied.

Missing AMPER data → unrestricted advisory grant (`FEATURE_DISABLED`) in Phase 0; later phases must fail safe rather than invent a budget.

## Gravity and ratchets

A grant of 0 is not always safe. `gravityCritical` plus `safeMinimumHoldingEffort` exist so AMPER does not drop a loaded elevator. Ratchet engaged may lower electrical need — only after the ratchet state is **sensed**, not assumed.

See AMPER architecture: https://github.com/The-Allsparks/AMPER
