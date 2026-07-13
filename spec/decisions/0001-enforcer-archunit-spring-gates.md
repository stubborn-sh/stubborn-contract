# ADR-0001: Maven Enforcer + ArchUnit as Spring-dependency gates on core modules

**Status:** Accepted — 2026-07-14
**Issue:** [#32](https://github.com/stubborn-sh/stubborn-contract/issues/32)

## Context

Stubborn Contract is being split from a Spring Boot monolith into a clean
vertical stack. The future shape is:

```
core (no Spring) → -spring → -spring-boot → -spring-cloud
```

The split is worthless if accidental Spring imports creep back into the core
layer. We need machine-enforced gates that catch any violation at build time —
not at code-review time.

## Decision

Two complementary gates run on every core module:

### 1. Maven Enforcer — dependency-level gate

The `ban-spring-in-core` execution is declared in `stubborn-build`'s
`pluginManagement`. By default it is skipped (`stubborn.no-spring.skip=true`).
Any module that wants enforcement opts in by setting the property to `false`:

```xml
<properties>
    <stubborn.no-spring.skip>false</stubborn.no-spring.skip>
</properties>
```

The rule bans direct (non-transitive) Spring artifacts on the compile or
runtime classpath:

```
org.springframework:*
org.springframework.boot:*
org.springframework.cloud:*
```

This catches `pom.xml`-level mistakes: someone adding `spring-core` to a core
module's `<dependencies>`.

### 2. ArchUnit — bytecode-level gate

Each core module gets a `NoSpringDepsTest` that uses ArchUnit to scan compiled
bytecode and assert no class in the module's root package imports anything from
`org.springframework.*`.

This catches code-level mistakes: a class that already had a Spring import
before the Enforcer rule existed, or a Spring type that entered via a
non-Spring transitive (e.g. `io.micrometer` → `spring-core`).

Both gates are additive. Enforcer catches obvious POM mistakes early
(at `validate` phase). ArchUnit catches subtle bytecode-level leaks (at `test`).

## Modules with gates active at Phase 0

| Module | Status |
|---|---|
| `stubborn-jsonassert` | Enforcer ✓, ArchUnit ✓ |
| `stubborn-xmlassert` | Enforcer ✓, ArchUnit ✓ |

Remaining core modules (`stubborn-verifier`, `stubborn-stub-runner`,
`stubborn-spec-java`, `stubborn-converters`) will have gates enabled in
Phase 1 — after Spring utilities are replaced with JDK equivalents.

## Consequences

- Developers adding Spring to any core module get a failing build with a clear
  message: "banned dependency" from Enforcer, or "no classes … should depend
  on `org.springframework`" from ArchUnit.
- The gate is opt-in per module, so Spring-containing modules
  (`stubborn-verifier-spring`, starters, etc.) are unaffected.
- `archunit-junit5` and `junit-jupiter` are added as `test`-scoped
  dependencies in core modules; no production footprint.
