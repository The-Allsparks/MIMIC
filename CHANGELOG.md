# Changelog

All notable changes to MIMIC will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial public repository scaffold for The Allsparks FTC Team 36117.
- Phase 0 implemented: hardware-independent interfaces, immutable snapshots, units and direction conventions, clocks, passive REV adapters, fake hardware, logging foundations, and validation utilities.
- Source-backed mechanism-control research, build-versus-adopt decision, architecture, phased roadmap, and student documentation.
- CI for compile, unit tests, and relative documentation link checks.

### Safety

- All motor and servo output features remain disabled by default.
- `MimicSession` refuses actuation feature flags and rejects goals with `NO_ACTIVE_CONTROL`.
