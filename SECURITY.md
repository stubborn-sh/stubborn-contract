# Security Policy

## Supported versions

Stubborn Contract is pre-1.0. Security fixes are made against the latest
released `0.x` line and the `main` branch. Older `0.x` versions do not receive
back-ported fixes — upgrade to the latest release to stay supported.

| Version | Supported          |
|---------|--------------------|
| `0.1.x` | :white_check_mark: |
| `< 0.1` | :x:                |

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
pull requests, or discussions.**

Report privately through GitHub's private vulnerability reporting:

1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability** (this opens a private advisory only you and
   the maintainers can see).
3. Describe the issue with enough detail to reproduce it — affected module and
   version, a proof of concept or steps to reproduce, and the impact you expect.

If you cannot use private reporting, open a minimal public issue that says only
"security issue — requesting a private channel" without any details, and a
maintainer will follow up.

## What to expect

- **Acknowledgement** within a few business days.
- An assessment of the report and, if accepted, a fix on a private branch.
- Coordinated disclosure: we will agree on a disclosure timeline with you and
  credit you in the advisory and release notes unless you prefer to remain
  anonymous.

## Scope

In scope: the published `sh.stubborn:*` artifacts and the code in this
repository. Out of scope: vulnerabilities in third-party dependencies (report
those upstream — though we still want to know so we can pin or suppress), and
issues that require an already-compromised build environment.

## Dependency and code scanning

This project runs automated security tooling in CI: OWASP dependency-check
(post-merge and weekly), CodeQL, SpotBugs + FindSecBugs, and Error Prone /
NullAway. Known-but-unreachable dependency findings are tracked with documented
justifications in `config/dependency-check-suppressions.xml`, each with the
criteria under which the suppression should be removed.
