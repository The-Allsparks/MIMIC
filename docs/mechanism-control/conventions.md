# Repository convention assessment (The Allsparks)

Prepared before MIMIC creation. MIMIC did **not** previously exist under `The-Allsparks/MIMIC` (verified 2026-08-17 via GitHub).

## Repositories inspected

| Repo | Visibility | Default branch | License | Notes |
|------|------------|----------------|---------|-------|
| [AMPER](https://github.com/The-Allsparks/AMPER) | Public | `main` | MIT | Closest analog: phased FTC Java library + research docs + CI |
| [ViDAR](https://github.com/The-Allsparks/ViDAR) | Public | `main` | MIT | FTC capability library; Java 11 `java-pure` tests |
| [ftc-dev-tools](https://github.com/The-Allsparks/ftc-dev-tools) | Public | `main` | Apache-2.0 | Governance templates / Dependabot / CoC |
| [ftc-team-analysis](https://github.com/The-Allsparks/ftc-team-analysis) | Public | `main` | MIT | Web tool; lighter robotics conventions |
| SponsorshipPlan | Private | `main` | — | Ignored for OSS library norms |

## Conventions adopted for MIMIC

| Topic | Followed from | MIMIC choice |
|-------|---------------|--------------|
| Public OSS | AMPER / ViDAR | Public |
| License | AMPER / ViDAR | **MIT** |
| Branch | Org default | `main` |
| Java + Gradle | AMPER | Root `java-library`, Java 11, CI Temurin 17, JUnit 5 |
| Package naming | AMPER | `org.allsparks.mimic` |
| LF + `.gitattributes` | AMPER / ViDAR | Yes |
| CoC / SECURITY / PR template / Dependabot | AMPER | Adapted (mechanism safety language) |
| Issue templates | AMPER | Bug / feature / phase work |
| Feature flags + phases | AMPER | `MimicFeatureFlags`, `MimicPhase` |
| Supplier-wired REV adapters | AMPER | No FTC SDK on desktop classpath |
| Doc link unit test | AMPER | `DocLinkCheckerTest` |
| Topics | Requested list | Applied on GitHub when API available |

## Deliberate differences from AMPER

- Domain is mechanism lifecycle, not electrical coordination.
- Additional labels: `calibration`, `motion-control`, `interlock`, `fault-handling`, `amper`.
- NextControl is **not** a Gradle dependency (GPL-3.0).
- Elevator hardware remains undocumented unknowns rather than invented CAD.
