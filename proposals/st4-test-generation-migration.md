# Proposal: Migrate test generation from string concatenation to StringTemplate 4

**Status:** Draft for review
**Module:** `stubborn-contract-verifier`
**Author:** Claude Code (with Marcin Grzejszczak)

## 1. Problem

The verifier generates contract tests by **concatenating strings**. `SingleTestGenerator.buildClass(...)` returns a `String`, assembled by ~186 classes in the `sh.stubborn.contract.verifier.builder` package that append fragments to `BlockBuilder` — a thin wrapper over `StringBuilder` (`addLine`, `indent`/`unindent`, `startBlock`/`endBlock`, `append`).

This is hard to maintain and reason about:

- **Presentation and logic are entangled.** Every builder mixes *decisions* (which assertion, which DSL call) with *formatting* (indentation, braces, line endings, quoting). Whitespace/brace bugs are easy to introduce and invisible until a generated test fails to compile.
- **Two output languages, one imperative pipeline.** The generator emits **Java** (JUnit 5, TestNG, JUnit 4) *and* **Groovy/Spock**. The language difference is expressed as conditionals scattered through the builder classes rather than as a clean seam.
- **High surface area.** ~186 small classes exist largely to place tokens and manage indentation — accidental complexity that a templating engine removes.

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
- Four production `.java` files still import Groovy at the library level: `builder/BodyParser.java` (`groovy.json.JsonOutput`, `groovy.lang.GString`), `builder/JsonBodyVerificationBuilder.java` (`JsonOutput`), `builder/TestSideRequestTemplateModel.java` (`JsonOutput`, `GString`), `builder/Path.java` (`groovy.transform.CompileStatic`).

So the core is Groovy-*source*-free but not Groovy-*library*-free. The chosen engine must be **pure Java** (ST4 is — zero deps), and this migration is the natural point to **finish** the job: replace those four `groovy.*` usages with pure-Java equivalents (`JsonOutput.toJson` → a small JSON writer or Jackson already on the classpath; `GString` handling → plain `String`/`CharSequence`; `@CompileStatic` → delete) and add a `maven-enforcer` **banned-import rule on `groovy.**`** for this module so it can't regress. That closes the gap the current source-only enforcer leaves.

## 4. Why StringTemplate 4 (ST4)

We evaluated typed source-builders and template engines:

| Option | Emits Java | Emits Groovy | Pure-Java core | Verdict |
|---|---|---|---|---|
| **JavaPoet** (Square/Palantir fork) | ✅ typed | ❌ (models the Java language) | ✅ | Java-only; can't do Spock |
| JavaParser / Roaster / Spoon | ✅ | ❌ | ✅ | Java-only |
| KotlinPoet | ❌ | ❌ | ✅ | Kotlin-only |
| Groovy `TemplateEngine` / AST | ✅ | ✅ | ❌ **pulls in Groovy** | violates §3 |
| Velocity / FreeMarker | ✅ | ✅ | ✅ | permit arbitrary logic in templates → drift back to spaghetti |
| **StringTemplate 4** | ✅ | ✅ | ✅ (~230 KB, no deps) | **chosen** |

There is **no typed "Poet"-style builder for Groovy**, so no single AST library can cover both targets — that rules out a JavaPoet-only solution and pushes us to a language-agnostic engine. Among those, **ST4** is purpose-built for code generation and **enforces strict model/view separation** (templates can't contain arbitrary logic — only attribute rendering, conditionals, and iteration). That rigidity is exactly what keeps generated-code templates clean, and it's battle-tested: ANTLR itself emits Java/C#/Go/JavaScript/… from one grammar using ST4 — the same "one model, many target languages" problem we have. It is pure Java with no transitive Groovy, satisfying §3.

Trade-off accepted: ST4 templates are not compile-time type-checked the way JavaPoet output is. We neutralise that risk with the safety net in §6 (we already compile-and-run the generated tests).

## 5. Target architecture

Introduce a clean **model → view** seam behind the existing `SingleTestGenerator` interface (unchanged public API):

```
Contract(s)  ──►  TestClassModel            ──►  ST4 render        ──►  String
                  (pure data, no formatting)      (language .stg)
```

- **`TestClassModel`** — an immutable data model describing the class and its methods: package, imports, class annotations, and per-method a structured description of *given* (setup), *when* (request/trigger), and *then* (status + header + body assertions). No indentation, no braces, no language tokens. This is where today's builder *decisions* move to.
- **Template groups (`.stg`)** — one group per output language:
  - `java.stg` — JUnit5/TestNG/JUnit4 (framework-specific bits parametrised, e.g. `@Test` vs TestNG, imports).
  - `spock.stg` — Spock (`given:/when:/then:` labels, `def`, GString quoting, `@Unroll`).
  - Shared sub-templates for language-neutral structure; a small set of framework flags select annotations/imports.
- **Renderers** — ST4 `AttributeRenderer`s handle cross-cutting formatting (string escaping/quoting per language, number/boolean literals) so the model stays language-agnostic.

The `builder` package shrinks from ~186 formatting classes to: the model, a `ModelBuilder` that walks contracts into `TestClassModel`, the `.stg` files, and a thin ST4 adapter implementing `SingleTestGenerator`.

**Refactor leverage — the public surface is tiny.** Per the inventory, only four things in the generator are public / revapi-gated (`stubborn-contract-verifier` is on the `revapi-maven-plugin` list):

- `SingleTestGenerator` (the SPI — resolved at runtime via `ServiceLoader` in `TestGenerator`, with `JavaTestGenerator` as fallback),
- `JavaTestGenerator` (public, no-arg ctor),
- `BlockBuilder` (**public**, so signature changes need a revapi justification),
- `sh.stubborn.contract.verifier.TestGenerator`.

**Everything else in `builder/` — all ~180 fragment/visitor/metadata classes (`Visitor`, `MethodVisitor`, `Given/When/Then`, `*ClassMetaData`, `*BodyParser`, transport fragments) — is package-private and free to rewrite** without revapi impact. That is the key enabler: we can replace the entire filtered-visitor rendering internals as long as `SingleTestGenerator.buildClass(...)` output and the public `BlockBuilder`/`JavaTestGenerator`/`TestGenerator` signatures stay stable. Strategy for `BlockBuilder`: keep it as a public compatibility shell (retain signatures for revapi) but let the ST4 renderer own formatting; it becomes a legacy adapter we can deprecate rather than delete.

**Where today's Java-vs-Spock split lives** (these become template-group / renderer differences): line ending `;` vs none, label prefix `// ` vs real `given:/when:/then:`, class modifier `public` vs none, suffix `Test` vs `Spec`, parent `extends X` vs `Specification`, method return `void` vs `def`, and string quoting `"…"` vs `'…'`/GString — carried today by `JavaClassMetaData`/`GroovyClassMetaData`, `JUnitMethodMetadata`/`SpockMethodMetadata`, `BodyParser`/`GroovyBodyParser`, and `ComparisonBuilder`/`GroovyComparisonBuilder`. The trickiest behavior to preserve is `BlockBuilder`'s **lazy, character-inspecting line-ending append** (`addAtTheEnd`, which avoids double semicolons/spaces by peeking at the last chars) — templates must reproduce this or the model must carry explicit statement boundaries.

## 6. Safety net — the existing tests are the contract

Requirement from the ask: *"we have good tests so they should still pass."* The inventory confirms a strong existing oracle we build on — plus one piece we need to add:

1. **Existing compile oracle (already there).** `src/test/groovy/.../util/SyntaxChecker.groovy` actually **compiles every generated test** — JDK `javax.tools.JavaCompiler` for Java output and Groovy `CompilerConfiguration`/AST for Spock output — via `tryToCompile(name, generatedTest)`. The `*MethodBodyBuilderTests` (`MethodBodyBuilder`, `MockMvc`, `Xml`, `Messaging`, `JaxRs`, …) drive hundreds of inline `Contract.make { … }` cases through it, and `SingleTestGeneratorTests` parametrises framework × test-mode. So "does the generated source compile" is already enforced — ideal for a template swap (regenerate → recompile). There is currently **no `expected/*.java` golden-file tree**; today's assertions are *compile-succeeds + contains-substring*.
2. **New golden-master snapshot harness (add in Phase 0).** Because no golden tree exists, we add one: capture the *exact current output* of `SingleTestGenerator` across the corpus (`src/test/resources/contractsToCompile/`, `contracts/dsl-to-yaml/`, `yml/`, `dsl/`, `body_builder/`, plus a representative slice of the inline `Contract.make` cases) as committed snapshots. The ST4 generator must reproduce them under the agreed parity bar (10.2).
3. **Compile-and-run parity.** Beyond compile-checking, the suite runs generated tests against WireMock/messaging fixtures — the ultimate behavioural oracle.

Both engines remain runnable behind a feature flag during migration, so CI diffs old vs new per fixture until parity is proven.

## 7. Phased rollout

Each phase is independently shippable and leaves the build green.

- **Phase 0 — Safety net.** Add the golden-master harness over the current generator. No behaviour change. Establishes the oracle.
- **Phase 1 — Dependency & seam.** Add ST4 (`org.antlr:ST4`) to the verifier (compile scope; ST4 has no transitive Groovy). Introduce `TestClassModel` + an ST4 `SingleTestGenerator` implementation behind a feature flag, initially delegating to the old generator.
- **Phase 1b — Finish Groovy-library removal.** Replace the four `groovy.*` library usages (§3) with pure-Java equivalents and add a `maven-enforcer` banned-import rule on `groovy.**` for this module. Can land before or alongside Phase 1; independent of ST4 but part of the "Groovy-free core" goal.
- **Phase 2 — Class scaffold.** Move class-level generation (package, imports, class annotations, method signatures, `given/when/then` skeleton) to `java.stg` + `spock.stg`. Bodies still delegate to legacy builders via an escape hatch. Prove golden parity for scaffolding.
- **Phase 3 — Request/when.** Port RestAssured (MockMvc/standalone/WebTestClient), JAX-RS, and messaging trigger generation to the model + templates.
- **Phase 4 — Response/then.** Port status/header/cookie assertions and the JSON and XML body-verification blocks (the largest bucket).
- **Phase 5 — Cutover & cleanup.** Flip the flag to ST4 by default; delete the legacy `builder` string classes and the flag; keep the golden harness as a regression guard.

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Subtle formatting drift breaks byte-exact golden tests | Decide the parity bar up front (10.2); prefer compile-and-run oracle; use ST4 renderers for consistent quoting/indent |
| ST4 templates not type-checked | Compile-and-run parity in CI; templates are small and reviewed; golden diffs catch regressions |
| Groovy accidentally re-enters the core via a transitive dep | ST4 has zero deps; enforcer + `CoreModuleArchTest` gate every build |
| Large blast radius / long-lived branch | Strict phasing behind a flag; each phase merges independently with both engines green |
| Custom/user-extension test frameworks | `CustomDefinition` path kept; templates parametrised by `TestFrameworkDefinition` so custom targets still plug in |

## 9. Effort (rough)

- Phase 0: small (harness + fixtures wiring).
- Phases 1–2: medium (new model + scaffolding templates).
- Phases 3–4: the bulk (request + assertion bodies; JSON/XML the heaviest).
- Phase 5: small–medium (delete legacy, docs).

Net: a large but well-bounded change; the golden harness makes each step verifiable rather than a leap of faith.

## 10. Open decisions (need your input)

1. **Branch base.** You said "branch off #56", but #56 is byte-identical to `main` (superseded by #57), so this proposal is branched off `main`. Confirm.
2. **Parity bar.** Byte-for-byte golden output, or "generated tests compile and pass" + normalised golden (whitespace/import order)? The latter lets us *improve* formatting as a side benefit; the former is the strictest regression guard. Recommendation: normalised golden + compile-and-run.
3. **Template granularity.** One `.stg` per language with framework flags (recommended), vs one per language×framework.
4. **Scope of first PR.** Ship Phase 0 (safety net) alone first for review, or Phases 0–2 together?

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
