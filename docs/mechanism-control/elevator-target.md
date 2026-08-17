# Allsparks elevator target (do not invent hardware)

The first **intended** real mechanism is a counterbalanced elevator with a ratchet. Until parts are selected, MIMIC implements **no elevator-specific controller**.

## Stated design intent (planning, not BOM)

- Two elevator towers
- Drawer-slide stages
- Shared or coupled capstan drive (arrangement **unconfirmed**)
- Counterbalance
- Up and down cable paths
- Possible independent tower sensing (**unconfirmed**)
- Ratchet engagement and release (**sensor unconfirmed**)
- Gravity load and safe holding
- Possible mechanical lock
- Interaction with AMPER

## Must be documented before elevator code

| Question | Status |
|----------|--------|
| Actual sensor arrangement | **Unknown** |
| Independently actuated sides? | **Unknown** |
| Independently measurable sides? | **Unknown** |
| Common shaft mechanically enforcing sync? | **Unknown** |
| Safe homing direction | **Unknown** — do not guess “down” |
| Physical limits / hard stops | **Unknown** |
| Ratchet state sensing | **Unknown** |
| Behavior after power loss | **Unknown** |
| Minimum holding effort | **Unknown** — counterbalance may reduce but not eliminate hold |

## Sync implication

If a common shaft **mechanically** ties both sides, independent PID correction can fight the structure. If sides are independent, disagreement monitoring matters. **Do not implement Phase 5 anti-racking until this is known.**

## Phase 0 use

Observe whatever motor and DIO you eventually wire, using generic `MechanismUnits` and `MimicSession`. Extract reusable elevator classes only after the real mechanism exists.
