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

## 3. Hard constraint: the core stays Groovy-free

PR #57 made `stubborn-contract-verifier` **production code Groovy-free** and added a `ban-groovy-in-production` enforcer (in `stubborn-build`). The verifier must still *emit* Groovy/Spock **text**, but it must not put Groovy on its own runtime classpath. Any chosen engine must be **pure Java**.

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

## 6. Safety net — the existing tests are the contract

Requirement from the ask: *"we have good tests so they should still pass."* We exploit that with a two-layer net:

1. **Golden-master (characterization) harness.** Before touching the generator, capture the *exact current output* of `SingleTestGenerator` for the full contract-fixture corpus (every framework × test-mode × body style) as committed golden files. The new ST4 generator must reproduce them. Normalisation is configurable (see open decision 10.2): either byte-for-byte, or modulo whitespace/import-order.
2. **Compile-and-run parity.** The suite already generates tests, compiles them, and runs them against WireMock/messaging fixtures. That end-to-end pass/fail is the ultimate oracle — if generated tests still compile and go green, behaviour is preserved even where we intentionally tidy formatting.

Both engines remain runnable behind a feature flag during migration, so we can diff old vs new per fixture in CI until parity is proven.

## 7. Phased rollout

Each phase is independently shippable and leaves the build green.

- **Phase 0 — Safety net.** Add the golden-master harness over the current generator. No behaviour change. Establishes the oracle.
- **Phase 1 — Dependency & seam.** Add ST4 (`org.antlr:ST4`) to the verifier (compile scope; verify the Groovy-free enforcer + ArchUnit still pass — ST4 has no Groovy). Introduce `TestClassModel` + an ST4 `SingleTestGenerator` implementation behind a feature flag, initially delegating to the old generator.
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
