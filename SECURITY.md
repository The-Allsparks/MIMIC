# Security Policy

## Supported versions

| Version | Supported |
| ------- | --------- |
| 0.1.x | Yes |

## Reporting a vulnerability

Please do **not** open a public issue for security problems that could put robots, students, or machines at risk.

Prefer:

1. GitHub Security Advisories for this repository (when available), or
2. A private email contact published by the maintainers

Include:

- A description of the issue
- Steps to reproduce
- Impact assessment (for example: unexpected motor motion, unsafe disable of gravity holds, credential exposure)

## Safety expectations for this project

MIMIC intentionally:

- Keeps **motor and servo output disabled by default**
- Treats missing or stale sensing as a reason to **avoid** active control, not invent values
- Documents that software cannot replace physical hard stops, correct direction, or mechanical holding devices

If you discover a path that enables actuator output without an explicit feature flag, or that drops gravity-critical holds below a declared safe minimum, treat it as a safety defect.

## Secrets

Never store passwords, Wi-Fi credentials, API keys, or tokens in the repository, issues, or exported logs.
