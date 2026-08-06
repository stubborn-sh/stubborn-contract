# Docker image acceptance test

An acceptance test **of** the `stubborn-contract` Docker image (not to be
confused with `project/src/test/...`, which is the image's *in-image*
contract-test harness).

It builds the image, runs a container against a minimal fixture contract
project, and asserts the image actually works:

- the container exits `0`,
- the generated contract **test sources** appear in the output, and
- the producer **`*-stubs.jar`** is emitted.

## ⚠️ CI-validated only

These assets were authored **by inspection of the image's runtime interface**.
Docker is not available in the environment they were written in, so the image
build/run has **not** been executed locally. Expect the first CI runs may need a
tweak or two. The workflow that runs this is
[`.github/workflows/docker-acceptance.yml`](../../../../../.github/workflows/docker-acceptance.yml).

## The image runtime contract this exercises

Reverse-engineered from `Dockerfile`, `project/build.sh`, `project/build.gradle`
and `docs/reference/docker.md`:

| Aspect | Value |
| --- | --- |
| Entry point | `CMD ["./build.sh"]` in `WORKDIR /stubborn-contract/` |
| Contracts input | mounted at **`/contracts`** (`contractsDslDir = new File("/contracts")`) |
| Output | whole Gradle `build/` dir copied to **`/stubborn-contract-output`** (`copyOutput` task) → stub jar at `libs/<name>-<version>-stubs.jar`, generated tests under `generated-test-sources/` |
| Test mode | **EXPLICIT** (hard-coded) → generated tests fire real HTTP at `APPLICATION_BASE_URL` |
| Runs as | uid `1000` (user `scc`) → the bind-mounted output dir must be writable by it |
| Key env | `APPLICATION_BASE_URL`, `PROJECT_NAME`, `PROJECT_GROUP`, `PROJECT_VERSION`, `PUBLISH_ARTIFACTS`, `FAIL_ON_NO_CONTRACTS`, `MESSAGING_TYPE` |

Because the mode is EXPLICIT, the fixture includes a tiny stdlib-only HTTP app
(`fixtures/app.py`) that satisfies the one fixture contract
(`contracts/health.yml`). The container reaches it via `--network host` at
`localhost:${FIXTURE_PORT}`. `PUBLISH_ARTIFACTS=false` keeps the run fully
offline (no artifact manager needed).

## How dependencies are resolved (why CI installs the reactor first)

The image's inner Gradle build resolves the stubborn-contract artifacts
(`stubborn-contract-gradle-plugin`, the `stubborn-contract-dependencies` BOM,
…) at `${verifierVersion}` primarily from the container's **baked-in local
Maven repo**. The `Dockerfile` copies `target/maven_dependencies` (produced by
the docker module's `maven-dependency-plugin:copy-dependencies`) into
`/home/scc/.m2/repository`, and `target/gradle_dependencies` (produced by
`get_dependencies.sh`) into `/home/scc/.gradle`. `central-snapshots` is only a
fallback.

So CI must `install` the whole reactor into `~/.m2` **before** packaging the
docker module — otherwise `copy-dependencies` has nothing to bake in. The
workflow does exactly that (mirroring `publish-docker.yml`).

## Running it locally (needs Docker)

```bash
# From the repo root, one-shot (installs reactor + builds image + runs):
SC_INSTALL_REACTOR=true \
  SC_DOCKER_IMAGE="$(docker/stubborn-contract-docker/src/test/acceptance/build-image.sh)" \
  bats docker/stubborn-contract-docker/src/test/acceptance/docker-acceptance.bats

# Or, if the image is already built and the reactor already installed:
SC_BUILD_IMAGE=true \
  bats docker/stubborn-contract-docker/src/test/acceptance/docker-acceptance.bats

# Or against a pre-built image ref:
SC_DOCKER_IMAGE=stubborn-acceptance/stubborn-contract:0.1.0-SNAPSHOT \
  bats docker/stubborn-contract-docker/src/test/acceptance/docker-acceptance.bats
```

Environment knobs: `SC_DOCKER_IMAGE`, `SC_BUILD_IMAGE`, `SC_INSTALL_REACTOR`,
`FIXTURE_PORT` (default `8888`), `SC_DEBUG` (`true` → image Gradle `--debug`),
`DOCKER_ORG` (default `stubborn-acceptance`).

## Things to watch on the first CI run

- **Image build time.** `get_dependencies.sh` runs a full inner Gradle build and
  the Dockerfile uses `noCache=true`; the build step can take several minutes.
- **Dep baking.** If the inner build can't resolve
  `sh.stubborn:stubborn-contract-gradle-plugin`, confirm the reactor install
  actually populated `~/.m2/repository/sh/stubborn/...` before the docker
  package step.
- **EXPLICIT assertions.** The generated test asserts status `200`, JSON
  `status == "UP"`, and `Content-Type: application/json`. `fixtures/app.py`
  returns exactly those; if a plugin version changes the generated assertions,
  adjust the fixture app/contract together.
- **Output permissions.** The container writes as uid `1000`; the test
  `chmod 777`s the bind-mounted output dir. If writes fail, check that mount.
