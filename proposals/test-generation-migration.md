# Test-generation migration: string builders → JavaPoet model + typed renderers

Status: **in progress** (model path implemented and flag-gated; legacy remains the
default). This document is the design reference cited from the code
(`ModelBasedTestGenerator`, `TestClassModel`, `TestGenerationGoldenMasterTests`).

## Motivation

The build-time contract test generator historically produced test sources by
concatenating strings in a large family of visitor/builder classes
(`*Given`/`*When`/`*Then`, `SingleMethodBuilder`, `ClassBodyBuilder`,
`MethodMetadataBuilder`, …). That approach:

- couples *what* to emit with *how* it is formatted (indentation, imports,
  semicolons), so every framework/mode combination re-implements layout;
- makes imports, generics, and escaping error-prone (hand-built strings);
- is hard to test in isolation — the only real oracle is the full generated
  source string.

The migration introduces an explicit **model** of a generated test class and
**typed renderers** that turn the model into source. The model is produced once;
each renderer owns layout for its target language.

## Why JavaPoet (+ Handlebars for Spock)

- **JavaPoet** (`com.palantir.javapoet`) renders Java sources with correct
  imports, indentation, and escaping from a typed builder API. It is the renderer
  for all Java targets (JUnit5, TestNG; MockMvc and explicit/RestAssured modes).
- **Groovy/Spock** cannot be modelled by JavaPoet (different language). The plan
  is a **Handlebars** (`com.github.jknack:handlebars`, already a managed
  dependency) template renderer for the Spock target, driven by the same model.

## Architecture

### Model (records, package `sh.stubborn.contract.verifier.builder`)

- `TestClassModel` — the class: package, name, annotations, imports, methods.
- `TestMethodModel(name, annotations, bodyLines, @Nullable RequestModel request,
  @Nullable ResponseModel response)`.
- `RequestModel(given, whenBlock)` — the `// given:` and `// when:` sections.
- `ResponseModel(StatementList thenBlock)` — the `// then:` section.
- `FluentStatement(head, continuations)` — a fluent call chain; only the last
  line is terminated with `;` on render.
- `StatementList(statements)` — a list where **every** line is terminated.
- `AnnotationModel` — an annotation with members.

The two statement shapes exist because assertion blocks differ: a fluent chain
(`assertThat(...).body(...) ... ;` — one terminator) versus a list of independent
statements (each terminated).

### Builders

- `ModelBuilder` — assembles a `TestClassModel` from the parsed contracts. Its
  `methodModel(...)` uses a layered gate:
  - response-eligible → `ResponseBodyLineProducer.andBlockLines(...)`
    (drives `GenericHttpBodyThen` directly, without `SingleMethodBuilder`);
  - request-only → `LegacyMethodBodyExtractor.responseBodyLines(...)`;
  - otherwise → verbatim `bodyLines` captured from the legacy pipeline.
- `RequestModelBuilder` / `ResponseModelBuilder` — build the request/response
  parts when the contract is *eligible* (see below).

### Renderers

- `JavaPoetTestRenderer` — Java targets. `toMethodSpec` emits `// given:` + given,
  `// when:` + when, `// then:` + thenBlock (when a response is present), then the
  verbatim `// and:` body-line tail.
- `HandlebarsSpockRenderer` — **planned**, not yet implemented. Spock currently
  routes to the legacy generator.

## Eligibility gate and the escape hatch

`RequestModelBuilder.eligible(...)` returns true only for:

- framework ∈ {JUNIT5, TESTNG} (i.e. **not Spock**),
- mode ∈ {MOCKMVC, EXPLICIT},
- HTTP contracts,
- a request with a URL.

Everything else (Spock; WebTestClient/WebClient; JAX-RS; messaging; no-URL
contracts) falls back to a **verbatim legacy capture**: the model still holds the
body, but the lines are produced by the legacy pipeline. This escape hatch keeps
output byte-identical for not-yet-migrated shapes while the model path grows.

## The flag

`TestGenerator` chooses the generator in `singleTestGenerator()`:

```java
private static final String MODEL_BASED_GENERATOR_PROPERTY =
        "stubborn.contract.verifier.model-based-generator"; // default: off
```

- **off (default):** `ServiceLoader<SingleTestGenerator>` → `JavaTestGenerator`
  (legacy).
- **on:** `ModelBasedTestGenerator`, which routes Spock → legacy delegate and Java
  → `ModelBuilder` + `JavaPoetTestRenderer`.

The default stays **off** until the phased cutover is complete and the safety nets
below prove parity.

## Safety nets

Both live in `stubborn-contract-generator`'s test sources.

- **`TestGenerationGoldenMasterTests`** — byte-exact snapshot oracle of the
  **legacy** `JavaTestGenerator`. Corpus = a set of focused HTTP contracts ×
  {JUNIT5/MOCKMVC, JUNIT5/EXPLICIT, TESTNG/MOCKMVC, **SPOCK/MOCKMVC**}. Snapshots
  under `src/test/resources/testgen-golden/`; regenerate an intentional change with
  `-Dtestgen.golden.update=true`.
- **`ModelBasedScaffoldParityTests`** — runs the **model** path against the same
  golden files, Java rows only, under a **normalized** bar (blank-line/trim
  insensitive; imports compared as a set) **and** requires the generated source to
  compile (`SyntaxChecker`). The bar is deliberately not byte-exact because
  JavaPoet's indentation and import ordering differ from the legacy string
  builders, while each method body is captured verbatim.

The golden master is the contract: any change to observable output must be a
deliberate, reviewed golden update.

## Phase history

- **Phase 0** — golden-master snapshot harness.
- **Phase 1** — JavaPoet dependency, `TestClassModel`, flag-gated delegating seam.
- **Phase 2** — Java class scaffold produced by JavaPoet.
- **Phase 3** — model-based request: URL/headers/cookies/query params, body,
  multipart, async, file-based bodies.
- **Phase 4** — model-based response: status, headers, cookies, and severing the
  response-body tail from `SingleMethodBuilder`
  (`ResponseBodyLineProducer` drives `GenericHttpBodyThen` directly).

## Current state

- Java (JUnit5, TestNG) × (MockMvc, Explicit) HTTP contracts render through the
  model + `JavaPoetTestRenderer`, guarded by `ModelBasedScaffoldParityTests`.
- Spock is still 100% legacy.
- `SingleMethodBuilder` and the visitor tree are still **live** on the model path
  via `LegacyMethodBodyExtractor`, `ResponseBodyLineProducer`, `ClassBodyBuilder`,
  and `MethodMetadataBuilder`, so they cannot be deleted yet.
- `ModelBuilder`'s import harvest still invokes the full legacy `JavaTestGenerator`
  once, purely to collect the import set — a remaining coupling to legacy code.

## Endgame (Phase 5)

The agreed endgame is: **flip the Java default to JavaPoet, build a Handlebars
Spock renderer, then delete all legacy scaffold.** Concretely, in dependency
order:

1. **Model the import set** so `ModelBuilder` no longer calls `JavaTestGenerator`
   for eligible Java classes (guarded by the parity test's import-set assertion).
2. **Shrink the escape hatch** — migrate the remaining Java shapes
   (WebTestClient/WebClient, JAX-RS) onto the model so the verbatim fallback is
   only Spock/messaging.
3. **Handlebars Spock renderer** — a template renderer driven by the same model,
   parameterised over a JAVA/GROOVY "dialect" seam: body parser
   (`RestAssuredBodyParser` vs `SpockRestAssuredBodyParser`), comparison builder
   (`ComparisonBuilder.JAVA_HTTP_INSTANCE` vs
   `GroovyComparisonBuilder.SPOCK_HTTP_INSTANCE`), line terminator (`;` vs none),
   and label style (`// x` vs `x`). Prove parity against the Spock golden rows.
4. **Flip the default** to the model path for all frameworks and update the golden
   master to the model output (a deliberate, reviewed snapshot change).
5. **Delete the legacy scaffold** — the `*Given`/`*When`/`*Then` visitor family,
   `SingleMethodBuilder`, and the legacy builders (~5.8k LOC). API-visible removals
   in the non-`-spring*` modules require `revapi` justifications.

### Prerequisites

- The generator module must be reliably green across the CI JDK matrix before the
  default is flipped — the golden/parity nets are the only guard, so they must run
  everywhere. (Two known test-hygiene issues are tracked separately: a runtime
  in-JVM compile failure in `YamlMockMvcMethodBodyBuilderTests` under newer JDKs,
  and a shared-classpath-resource mutation in `SingleTestGeneratorTests#setup`
  that deletes `request.json`/`response.json`.)
