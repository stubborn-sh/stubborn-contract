# Proposal: Migrate test generation from string concatenation to a model + typed renderers

**Status:** Draft for review
**Module:** `stubborn-contract-verifier`
**Author:** Claude Code (with Marcin Grzejszczak)

## 1. Problem

The verifier generates contract tests by **concatenating strings**. `SingleTestGenerator.buildClass(...)` returns a `String`, assembled by ~186 classes in the `sh.stubborn.contract.verifier.builder` package that append fragments to `BlockBuilder` — a thin wrapper over `StringBuilder` (`addLine`, `indent`/`unindent`, `startBlock`/`endBlock`, `append`).

This is hard to maintain and reason about:

- **Presentation and logic are entangled.** Every builder mixes *decisions* (which assertion, which DSL call) with *formatting* (indentation, braces, line endings, quoting). Whitespace/brace bugs are easy to introduce and invisible until a generated test fails to compile.
- **Two output languages, one imperative pipeline.** The generator emits **Java** (JUnit 5, TestNG, JUnit 4) *and* **Groovy/Spock**. The language difference is expressed as conditionals scattered through the builder classes rather than as a clean seam.
- **High surface area.** ~186 small classes exist largely to place tokens and manage indentation — accidental complexity that a proper code-generation layer removes.

## 2. Output matrix (what must be preserved)

| Target framework | Language / extension |
|---|---|
| JUnit 5 | `.java` |
| TestNG | `.java` |
| JUnit 4 | `.java` |
| **Spock** | **`.groovy`** |
| Custom | configurable |

Request/response styles that must keep generating identically: RestAssured (MockMvc / standalone / WebTestClient), JAX-RS, plus messaging (Camel, Integration, JMS, Kafka), and the JSON (JsonAssert/JsonPath) and XML (XmlUnit/XPath) body-verification blocks.

## 3. Constraint: keep (and finish) making the core Groovy-free

PR #57 removed the Groovy **source files** from `stubborn-contract-verifier` production and added a `ban-groovy-in-production` enforcer (in `stubborn-build`). Two nuances the inventory surfaced:

- The enforcer only bans `src/main/**/*.groovy` **source files** — it does **not** ban `groovy.*` **library imports** inside `.java` files.
- Production `.java` files still import Groovy at the library level (e.g. `builder/BodyParser.java`, `builder/JsonBodyVerificationBuilder.java`, `builder/TestSideRequestTemplateModel.java`, `builder/Path.java`, plus several `util/*` helpers).

So the core is Groovy-*source*-free but not Groovy-*library*-free. The chosen engines must be **pure Java**, and this migration is the natural point to **finish** the job: replace those `groovy.*` usages with pure-Java equivalents (`JsonOutput.toJson` → the Jackson `ObjectMapper` already on the classpath; `GString` handling → plain `String`/`CharSequence`; `@CompileStatic` → delete) and add a `maven-enforcer` **banned-import rule on `groovy.**`** for this module so it can't regress. That closes the gap the current source-only enforcer leaves. (The Groovy *DSL* modules — `specs/stubborn-contract-spec-groovy` — legitimately keep Groovy and are out of scope.)

## 4. Engine choice — JavaPoet (Java targets) + Handlebars (Spock)

**We are NOT using StringTemplate 4.** An earlier draft proposed ST4; it is dropped: `org.antlr:ST4` last shipped in 2022 and is effectively in maintenance-only mode, and adding a stale dependency to *fix* a maintainability problem is self-defeating.

The two hard constraints narrow the field sharply:

- **Must emit Groovy/Spock too**, not just Java — this eliminates every Java-only "Poet"/AST library (JavaPoet, JavaParser, Roaster, Spoon) *as a sole solution*.
- **Must be pure Java** (no transitive Groovy) to satisfy §3 — this eliminates Groovy's own `TemplateEngine`/AST.

| Option | Emits Java | Emits Groovy | Pure-Java | Maintained | Verdict |
|---|---|---|---|---|---|
| **JavaPoet** (Palantir fork `com.palantir.javapoet`) | ✅ *typed, structurally correct* | ❌ (models the Java language) | ✅ | ✅ active | **chosen for Java targets** |
| **Handlebars.java** (`com.github.jknack`) | ✅ | ✅ | ✅ | already a verifier dep | **chosen for Spock** |
| StringTemplate 4 | ✅ | ✅ | ✅ | ❌ ~2022 | rejected (unmaintained) |
| JavaParser / Roaster / Spoon | ✅ | ❌ | ✅ | ✅ | Java-only |
| Groovy `TemplateEngine` / AST | ✅ | ✅ | ❌ **pulls in Groovy** | ✅ | violates §3 |
| Velocity / FreeMarker / Pebble / JTE / Mustache.java | ✅ | ✅ | ✅ | ✅ (varies) | viable single-engine fallbacks (see below) |

**Decision: a shared pure-data model rendered by two engines.**

- **JavaPoet (Palantir fork) for the Java targets (JUnit 5 / TestNG / JUnit 4).** JavaPoet models the Java language with typed `TypeSpec`/`MethodSpec`/`CodeBlock` builders, so it is **structurally impossible to emit malformed Java** — braces, imports, indentation, and escaping are correct by construction. That kills the *exact* bug class this migration exists to remove, for the 3 of 4 targets that are the bulk of usage. Square archived the original JavaPoet; the **Palantir fork is actively maintained**.
- **Handlebars.java for Spock (`.groovy`).** Groovy is the one target JavaPoet can't model. Handlebars is **already on the verifier's classpath** (`stubborn-contract-verifier/pom.xml`, with production helpers under `builder/handlebars/`), so it adds **zero new dependency**; it is **logic-less** (a Mustache superset), which enforces the same model/view separation ST4 would have — templates can only render attributes, iterate, and branch, not hold logic. Spock is also the *shrinking* target given the JUnit-5 direction, so it is the right place to spend the least effort.

**Single-engine fallback.** If maintaining two renderers is undesirable, one logic-less template engine can emit both languages: **Mustache.java** (active, minimal, logic-less) or **JTE** (most actively developed, compiles templates, but web-oriented + adds a build step). Both are pure Java. The trade-off vs. the hybrid is losing JavaPoet's compile-by-construction guarantee on the Java side.

## 5. Target architecture

Introduce a clean **model → view** seam behind the existing `SingleTestGenerator` interface (unchanged public API):

```
Contract(s)  ──►  TestClassModel            ──►  JavaPoetRenderer   ──►  String (.java)
                  (pure data, no formatting)      HandlebarsRenderer ──►  String (.groovy)
```

- **`TestClassModel`** — an immutable data model describing the class and its methods: package, imports, class annotations, and per-method a structured description of *given* (setup), *when* (request/trigger), and *then* (status + header + body assertions). No indentation, no braces, no language tokens. This is where today's builder *decisions* move to.
- **Renderers** —
  - `JavaPoetTestRenderer` walks `TestClassModel` into JavaPoet `TypeSpec`/`MethodSpec`/`CodeBlock`, parametrised by a `TestFrameworkDefinition` (JUnit5 vs TestNG vs JUnit4 annotations/imports). Cross-cutting Java concerns (imports, static imports, escaping) are JavaPoet's job.
  - `HandlebarsSpockRenderer` renders the same model through a small set of logic-less `*.hbs` partials (`spock-class.hbs`, `given/when/then` fragments) producing Spock `given:/when:/then:`, `def`, GString quoting, `@Unroll`.

The `builder` package shrinks from ~186 formatting classes to: the model, a `ModelBuilder` that walks contracts into `TestClassModel`, the two renderers, and the `.hbs` partials.

**Refactor leverage — the public surface is tiny.** Only four things in the generator are public / revapi-gated (`stubborn-contract-verifier` is on the `revapi-maven-plugin` list): `SingleTestGenerator` (the SPI, resolved via `ServiceLoader` in `TestGenerator`, `JavaTestGenerator` fallback), `JavaTestGenerator` (public, no-arg ctor), `BlockBuilder` (**public**), and `sh.stubborn.contract.verifier.TestGenerator`. **Everything else in `builder/` — all ~180 fragment/visitor/metadata classes — is package-private and free to rewrite** without revapi impact. Strategy for `BlockBuilder`: keep it as a public compatibility shell (retain signatures for revapi) but let the renderers own formatting; it becomes a legacy adapter we can deprecate rather than delete.

**Where today's Java-vs-Spock split lives** (these become renderer differences): line ending `;` vs none, label prefix `// ` vs real `given:/when:/then:`, class modifier `public` vs none, suffix `Test` vs `Spec`, parent `extends X` vs `Specification`, method return `void` vs `def`, and string quoting `"…"` vs `'…'`/GString — carried today by `JavaClassMetaData`/`GroovyClassMetaData`, `JUnitMethodMetadata`/`SpockMethodMetadata`, `BodyParser`/`GroovyBodyParser`, and `ComparisonBuilder`/`GroovyComparisonBuilder`. With JavaPoet the Java side gets these for free; the Handlebars partials carry the Spock side. The trickiest legacy behavior to preserve is `BlockBuilder`'s **lazy, character-inspecting line-ending append** (`addAtTheEnd`) — the model carries explicit statement boundaries so neither renderer needs it.

## 6. Safety net — the existing tests are the contract

Requirement from the ask: *"we have good tests so they should still pass."* The inventory confirms a strong existing oracle we build on — plus one piece we need to add:

1. **Existing compile oracle (already there).** `src/test/groovy/.../util/SyntaxChecker.groovy` actually **compiles every generated test** — JDK `javax.tools.JavaCompiler` for Java output and Groovy `CompilerConfiguration`/AST for Spock output — via `tryToCompile(name, generatedTest)`. The `*MethodBodyBuilderTests` drive hundreds of inline `Contract.make { … }` cases through it, and `SingleTestGeneratorTests` parametrises framework × test-mode. So "does the generated source compile" is already enforced — ideal for a generator swap. There is currently **no `expected/*.java` golden-file tree**; today's assertions are *compile-succeeds + contains-substring*.
2. **New golden-master snapshot harness (add in Phase 0).** Because no golden tree exists, we add one: capture the *exact current output* of `SingleTestGenerator` across the corpus (`src/test/resources/contractsToCompile/`, `contracts/dsl-to-yaml/`, `yml/`, `dsl/`, `body_builder/`, plus a representative slice of the inline `Contract.make` cases) as committed snapshots. The new generator must reproduce them under the agreed parity bar (§10.2).
3. **Compile-and-run parity.** Beyond compile-checking, the suite runs generated tests against WireMock/messaging fixtures — the ultimate behavioural oracle.

Both engines remain runnable behind a feature flag during migration, so CI diffs old vs new per fixture until parity is proven.

## 7. Phased rollout

Each phase is independently shippable and leaves the build green.

- **Phase 0 — Safety net.** Add the golden-master harness over the current generator. No behaviour change. Establishes the oracle.
- **Phase 1 — Dependency & seam.** Add JavaPoet (`com.palantir.javapoet:javapoet`) to the verifier (compile scope). Introduce `TestClassModel` + a new `SingleTestGenerator` implementation behind a feature flag, initially delegating to the old generator.
- **Phase 1b — Finish Groovy-library removal.** Replace the remaining `groovy.*` library usages (§3) with pure-Java equivalents (Jackson `ObjectMapper` for JSON, plain `String` for `GString`) and add a `maven-enforcer` banned-import rule on `groovy.**` for this module. Independent of the engine work; part of the "Groovy-free core" goal.
- **Phase 2 — Class scaffold.** Move class-level generation (package, imports, class annotations, method signatures, `given/when/then` skeleton) to the JavaPoet renderer + Handlebars Spock partials. Bodies still delegate to legacy builders via an escape hatch. Prove golden parity for scaffolding.
- **Phase 3 — Request/when.** Port RestAssured (MockMvc/standalone/WebTestClient), JAX-RS, and messaging trigger generation to the model + renderers.
- **Phase 4 — Response/then.** Port status/header/cookie assertions and the JSON and XML body-verification blocks (the largest bucket).
- **Phase 5 — Cutover & cleanup.** Flip the flag to the new generator by default; delete the legacy `builder` string classes and the flag; keep the golden harness as a regression guard.

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Subtle formatting drift breaks byte-exact golden tests | Decide the parity bar up front (§10.2); prefer normalized-golden + compile-and-run; JavaPoet gives consistent Java formatting for free |
| Groovy-side (Handlebars) templates aren't type-checked | Compile-and-run parity in CI (`SyntaxChecker` already compiles Spock output); the Spock partials are small, logic-less, and reviewed; golden diffs catch regressions. The Java side is type-safe by construction via JavaPoet. |
| Two renderers diverge over time | They share one `TestClassModel`; a renderer that can't express a model node fails loudly. Spock is a shrinking target. |
| Groovy accidentally re-enters the core via a transitive dep | JavaPoet + Handlebars are pure Java; enforcer + `CoreModuleArchTest` gate every build; new `groovy.**` banned-import rule (1b) |
| Large blast radius / long-lived branch | Strict phasing behind a flag; each phase merges independently with both engines green |
| Custom/user-extension test frameworks | `CustomDefinition` path kept; JavaPoet renderer parametrised by `TestFrameworkDefinition` so custom Java targets still plug in |

## 9. Effort (rough)

- Phase 0: small (harness + fixtures wiring).
- Phases 1–2: medium (new model + JavaPoet scaffolding + Spock partials).
- Phases 3–4: the bulk (request + assertion bodies; JSON/XML the heaviest — ported once into JavaPoet `CodeBlock`s, once into Handlebars partials).
- Phase 5: small–medium (delete legacy, docs).

Net: a large but well-bounded change; the golden harness makes each step verifiable rather than a leap of faith.

## 10. Open decisions (defaults chosen; override any)

1. **Branch base.** Off `main` (which now carries #59/#60 and, once merged, #61). Confirmed.
2. **Parity bar.** Default: **normalized golden** (whitespace/import-order insensitive) **+ compile-and-run**, not byte-exact — this lets us *improve* formatting as a side benefit while the compile-and-run oracle guards behaviour. Override to byte-exact if you want the strictest regression guard.
3. **Engine.** Default: **JavaPoet (Palantir) for Java + Handlebars for Spock** (§4). Override to a single logic-less engine (Mustache.java / JTE) if you'd rather not maintain two renderers.
4. **Scope of first PR.** Default: ship **Phase 0** (safety net) alone first for review, then Phases 1–2 together.

## Appendix A — Current generator inventory (from a full code scan)

**Design pattern:** a *hybrid* — a fluent **builder chain** for assembly (`JavaTestGenerator` wires `GeneratedTestClassBuilder` → `ClassBodyBuilder` → `SingleMethodBuilder`, all sharing one mutable `BlockBuilder`) and a **composite + filtered visitor** for rendering. Fragments implement `Acceptor`/`OurCallable` (class-level `Visitor<T>`) or `MethodAcceptor`/`Function` (`MethodVisitor<T>` whose render method is `apply(SingleContractMetadata)`, tagged `@CanIgnoreReturnValue`), with marker sub-interfaces `Given`/`When`/`Then`. At `build()` the list is filtered by `accept(...)` (class-level = first match wins; method-level = all matches in order) and survivors mutate the shared buffer. Selection predicates switch on `TestFramework`, `TestMode`, `CommunicationType`, and content type.

**`builder/` fragment buckets (~186 classes):**
- **(a) Class-level metadata / imports / annotations (~55):** `ClassMetaData` + `JavaClassMetaData`/`GroovyClassMetaData`, `GeneratedClassMetaData`, ~28 `*Imports*`, ~11 `*Annotation*` (`JUnit5MethodAnnotation`, `SpockOrderClassAnnotation`, …), `NameProvider`, `BaseClassProvider`.
- **(b) Method-level given/when/then + metadata (~30):** `SingleMethodBuilder`, `MethodMetadata` + `JUnitMethodMetadata`/`SpockMethodMetadata`, `Given/When/Then`, `BodyMethodVisitor`, `MethodPre/PostProcessor`, `TemplateUpdatingMethodPostProcessor`.
- **(c) Transport fragments (~95):** RestAssured MockMvc/Explicit/WebTestClient (~36), JAX-RS (~21), messaging Camel/Integration/JMS/Kafka (~17), custom-mode (~21) — each with `Java*`/`Spock*` variants and `*BodyParser`/`GroovyBodyParser`, `ComparisonBuilder`/`GroovyComparisonBuilder`.
- **(d) JSON/XML assertion builders (~6):** `JsonBodyVerificationBuilder`, `XmlBodyVerificationBuilder`, `BodyAssertionLineCreator`, `Generic{Json,Xml,Text,Binary,Http}BodyThen`.
- **(e) Helpers/infra (~15):** `BlockBuilder`, `Acceptor`/`OurCallable`/`Visitor`/`MethodVisitor`/`MethodAcceptor`, `Field`, `ContentHelper`, `CommunicationType`, `QueryParamsResolver`, `EscapedString`, `Path`, `BodyReader`, `GeneratedTestClass`, `builder/handlebars/`.

**Entry points / wiring:** `TestGenerator` (`ServiceLoader.load(SingleTestGenerator.class)` → `JavaTestGenerator` fallback) → `TestGeneratorApplication`; Maven `GenerateTestsMojo` and Gradle `GenerateServerTestsTask` both call `new TestGenerator(config).generate()`.

**Test corpus:** end-to-end drivers in `src/test/java/.../builder/` (`SingleTestGeneratorTests`, `MethodBodyBuilderTests` (1215 lines), `Xml/Messaging/JaxRs…MethodBodyBuilderTests`, `Json/XmlBodyVerificationBuilderTests`) + `src/test/groovy/.../builder/` (`GeneratedTestClassTests`, `BlockBuilderTests`); compile oracle `util/SyntaxChecker.groovy`; fixtures in `src/test/resources/{contractsToCompile,contracts/dsl-to-yaml,yml,dsl,body_builder}` plus hundreds of inline `Contract.make { … }` cases.
