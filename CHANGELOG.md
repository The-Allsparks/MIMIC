# Changelog

All notable changes to MIMIC will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial public repository scaffold for The Allsparks FTC Team 36117.
- Phase 0 `STALE` is documented and tested as observer liveness (gap since the previous `capture()` start), not Control Hub sample age. `staleAfterNanos <= 0` remains the default off switch ([#25](https://github.com/The-Allsparks/MIMIC/issues/25)).
- Source-backed mechanism-control research, build-versus-adopt decision, architecture, phased roadmap, and student documentation.
- CI for compile, unit tests, and relative documentation link checks.

### Safety

- All motor and servo output features remain disabled by default.
- `MimicSession` refuses actuation feature flags and rejects goals with `NO_ACTIVE_CONTROL`.
