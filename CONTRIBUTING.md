# Contributing to MIMIC

MIMIC is maintained by [The Allsparks](https://github.com/The-Allsparks) (FTC Team 36117) for our team and the wider FTC community.

## Setup

```powershell
git clone https://github.com/The-Allsparks/MIMIC.git
cd MIMIC
.\gradlew.bat test
```

## Rules of engagement

1. **Do not enable motor or servo output** in PRs without explicit maintainer review and documented acceptance tests.
2. Phase 0 and Phase 1 must remain behavior-neutral for actuators.
3. Distinguish **verified fact**, **engineering inference**, and **untested hypothesis** in documentation.
4. Never describe an FRC motor-controller capability as a current FTC capability without evidence.
5. Do not invent Allsparks elevator hardware that has not been selected.
6. Do not commit secrets, Wi-Fi passwords, tokens, or student PII.
7. Do not add NextControl, YAMS, or WPILib as compile dependencies without a documented license review.

## Pull requests

- Prefer small, reviewable PRs.
- Include motivation, phase impact, test evidence, and safety notes.
- Update docs when behavior or maturity labels change.
- Run `.\gradlew.bat test` (or `./gradlew test`) before requesting review.

## Line endings

The repository stores LF line endings (see [.gitattributes](.gitattributes)).

## License

Contributions are accepted under the MIT License ([LICENSE](LICENSE)). No CLA is required.
