# stubborn-extras

Community-maintained integrations that live in the Stubborn Contract monorepo.
These modules are **best-effort**: they are not part of the core release guarantee
and may have a different compatibility matrix or release cadence.

## Modules

| Module | Description | Status |
|--------|-------------|--------|
| `stubborn-spec-kotlin` | Kotlin DSL for writing contracts (`.kt` files) | Best effort |
| `stubborn-contract-gradle-plugin` | Gradle plugin for contract verification and stub publishing | Best effort |
| `stubborn-contract-gradle-portal-plugin` | Gradle Plugin Portal publishing helper | Best effort |

## Support policy

- Core `sh.stubborn:*` modules follow semantic versioning with a binary-compat gate (revapi).
- Modules in `stubborn-extras/` are released on a best-effort basis alongside the core.
- Breaking changes may happen without a major version bump — check release notes.
- Community PRs for these modules are especially welcome.

## Kotlin DSL

Use `.kt` compiled contracts (not `.kts` scripts). KTS scripting support was deprecated
in Stubborn Contract 0.1 and will be removed in the next major release because the
`kotlin.script.experimental` host was removed in Kotlin 2.3+.

```kotlin
import sh.stubborn.contract.spec.ContractDsl.Companion.contract

contract {
    request {
        method = GET
        url = url("/api/hello")
    }
    response {
        status = OK
    }
}
```

## Gradle plugin

```groovy
plugins {
    id 'sh.stubborn.contract' version '0.1.0-SNAPSHOT'
}
```

For snapshot releases add the Maven Central snapshots repository to your
`settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven { url 'https://central.sonatype.com/repository/maven-snapshots/' }
        gradlePluginPortal()
    }
}
```
