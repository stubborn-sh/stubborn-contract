# Stub Runner — Git Storage

Stub Runner can serve stubs directly from a Git repository, without publishing to a Maven repository. This is useful for:

- Monorepos where stubs live alongside source code
- Teams that prefer Git as the single source of truth for contracts
- Rapid iteration without publish steps

## Configuration

### Maven plugin

```xml
<plugin>
  <groupId>sh.stubborn</groupId>
  <artifactId>stubborn-contract-maven-plugin</artifactId>
  <configuration>
    <contractsRepositoryUrl>git://https://github.com/myorg/contracts-repo.git</contractsRepositoryUrl>
    <contractsPath>/stubs</contractsPath>
  </configuration>
</plugin>
```

### Stub Runner

```java
@AutoConfigureStubRunner(
    ids = "sh.stubborn:order-service:+:stubs",
    stubsMode = StubRunnerProperties.StubsMode.REMOTE,
    repositoryRoot = "git://https://github.com/myorg/contracts-repo.git"
)
```

Or via Spring Boot properties:

```yaml
stubborn:
  contract:
    stubrunner:
      repository-root: git://https://github.com/myorg/contracts-repo.git
      ids: sh.stubborn:order-service:+:stubs
      stubs-mode: REMOTE
```

## Git URL format

The prefix `git://` signals that Stub Runner should use Git rather than Maven to resolve stubs:

| Format | Example |
|---|---|
| HTTPS | `git://https://github.com/org/repo.git` |
| SSH | `git://git@github.com:org/repo.git` |
| Branch | `git://https://github.com/org/repo.git#my-branch` |
| Tag | `git://https://github.com/org/repo.git#v1.2.3` |

## Authentication

### HTTPS with credentials

Set environment variables (or Spring properties):

```yaml
stubborn:
  contract:
    stubrunner:
      username: ${GIT_USERNAME}
      password: ${GIT_TOKEN}
```

### SSH

SSH authentication uses your system's `~/.ssh/config` automatically via JGit's default `SshSessionFactory`. No additional configuration is required.

## Repository structure

When Stub Runner clones the repository, it looks for stubs under the artifact's path. Default layout:

```
contracts-repo/
└── sh/
    └── stubborn/
        └── order-service/
            └── 1.0.0/
                └── stubs/
                    ├── mappings/
                    │   └── get-order.json
                    └── META-INF/
                        └── sh.stubborn/
                            └── order-service/
                                └── 1.0.0/
                                    └── contracts/
                                        └── get-order.yml
```

Or a flat layout when using `contractsPath`:

```
contracts-repo/
└── stubs/
    └── order-service/
        └── mappings/
            └── *.json
```

## Caching

Stub Runner caches the cloned repository in a local temp directory to avoid repeated clones. The cache is invalidated when the branch tip changes.

Stub Runner manages the cache directory automatically; the location is not configurable.

## See also

- [Stub Runner Overview](./stub-runner)
- [Stub Runner Spring Boot](./stub-runner-spring-boot)
- [How-to: Git as Contract Storage](/howto/git-storage)
