# Quality Gates

Stubborn Contract enforces two automated quality gates on its **core, spring-less**
module set: line + branch **code coverage** (JaCoCo) and **mutation coverage** (PIT)
on the classes a change actually touches.

Both gates currently run in **WARN mode** — they report but do not block. Once the
test backfill lands they are flipped to **blocking** (see [Flipping to blocking](#flipping-to-blocking)).

## The gate set (12 modules)

The gates apply only to the core, framework-free modules:

| # | Module |
|---|--------|
| 1 | `stubborn-contract-jsonassert` |
| 2 | `stubborn-contract-xmlassert` |
| 3 | `specs/stubborn-contract-spec-java` |
| 4 | `specs/stubborn-contract-spec-groovy` |
| 5 | `stubborn-contract-verifier` |
| 6 | `stubborn-contract-generator` |
| 7 | `stubborn-contract-stub-runner` |
| 8 | `stubborn-contract-wiremock` |
| 9 | `stubborn-contract-tools/stubborn-contract-converters` |
| 10 | `stubborn-contract-messaging-kafka` |
| 11 | `stubborn-contract-messaging-rabbit` |
| 12 | `stubborn-contract-messaging-jms` |

`stubborn-contract-messaging-tck` is the integration harness, not a gate target, and
is intentionally excluded. The `-spring`, `-spring-boot`, `-spring-cloud`, and
`-quarkus` tiers are also out of scope.

This list is duplicated in three places that **must stay in sync**:

- `scripts/quality/coverage-report.sh` and `scripts/quality/changed-core-classes.sh` (the `GATE_MODULES` arrays)
- `.github/workflows/quality-gates.yaml` (the `GATE_MODULES` env var)
- the `pitest-maven` plugin declaration in each of the 12 module POMs

## Gate 1 — Coverage (JaCoCo aggregate)

- **Floor:** LINE ≥ 80% and BRANCH ≥ 80%, measured as an **aggregate** across the 12 modules.
- **Tooling:** `jacoco-maven-plugin` (managed in `stubborn-contract-build/pom.xml`).
- The `jacoco-check` execution (bound to `verify`) enforces the floor per module BUNDLE
  when JaCoCo is active. JaCoCo instrumentation is only active under the `-Psonar`
  profile (root `pom.xml`) or the CI coverage job — it is **not** wired into the normal
  build.
- The aggregate figure across all 12 modules is computed by `scripts/quality/coverage-report.sh`.

Thresholds are properties in `stubborn-contract-build/pom.xml`:

```xml
<jacoco.line.min>0.80</jacoco.line.min>
<jacoco.branch.min>0.80</jacoco.branch.min>
<jacoco.haltOnFailure>false</jacoco.haltOnFailure>  <!-- WARN mode -->
```

### Run locally

```bash
# Produce coverage data for the gate modules (any subset works)
./mvnw -Psonar test -pl stubborn-contract-verifier -am

# Aggregate + print LINE / BRANCH across whichever gate modules have a jacoco.csv
scripts/quality/coverage-report.sh            # report only, always exit 0
scripts/quality/coverage-report.sh --enforce  # exit non-zero if LINE<80 or BRANCH<80
```

## Gate 2 — Mutation coverage (PIT, diff-scoped)

- **Floor:** 90% mutation coverage on **changed core classes only** (not the whole module).
- **Tooling:** `pitest-maven` 1.20.3 + `pitest-junit5-plugin` 1.2.3 (managed in
  `stubborn-contract-build/pom.xml`; each gate module declares the plugin to inherit it).
- The CI `mutation-diff` job diffs the PR against its merge base, maps changed
  `src/main/java/**.java` files to fully-qualified class names via
  `scripts/quality/changed-core-classes.sh`, and runs PIT per affected module with
  `-DtargetClasses=<changed classes>` and `-DmutationThreshold=90`.

The default `pitest.mutationThreshold` property is `0` (warn mode). The CI diff job
overrides it to `90` on the command line so the real floor applies to changed classes
even while the module-wide default stays in warn mode.

### Run locally

```bash
# Whole-module mutation run (uses the managed config; PIT defaults targetClasses to sh.stubborn.*)
./mvnw -pl stubborn-contract-verifier -am test-compile \
  org.pitest:pitest-maven:mutationCoverage

# Reproduce the CI diff gate: only classes changed vs origin/main, 90% floor
scripts/quality/changed-core-classes.sh origin/main | while IFS=$'\t' read -r module classes; do
  ./mvnw -pl "${module}" -am test-compile \
    org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses="${classes}" -DtargetTests='sh.stubborn.*' -DmutationThreshold=90
done
```

HTML + CSV reports land in each module's `target/pit-reports/`.

## CI

`.github/workflows/quality-gates.yaml` runs on every pull request (and via
`workflow_dispatch`) with two jobs:

- **coverage** — builds the 12 gate modules with `-Psonar`, then runs
  `coverage-report.sh` and writes the aggregate to the job summary.
- **mutation-diff** — checks out full history, computes changed core classes, and runs
  PIT at a 90% floor per affected module. If no core classes changed it succeeds with
  `no core classes changed`.

Both jobs are `continue-on-error: true` (WARN mode).

## Flipping to blocking

Once the test backfill lands, make the gates enforcing:

1. `stubborn-contract-build/pom.xml`: set `<jacoco.haltOnFailure>true</jacoco.haltOnFailure>`.
2. `stubborn-contract-build/pom.xml`: set `<pitest.mutationThreshold>90</pitest.mutationThreshold>`.
3. `.github/workflows/quality-gates.yaml`: remove `continue-on-error: true` from both jobs.
