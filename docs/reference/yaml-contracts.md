# YAML Contract Schema

YAML contracts are the recommended format for writing Stubborn Contract tests. Each `.yml` file in your `src/test/resources/contracts/` directory is one contract.

::: tip Generated from the code
The field and value tables on this page are generated from the `YamlContract` model and its enums
by `docs/.vitepress/reference-gen.mjs`, and the examples are imported from real contract files —
so nothing here can drift from the implementation.
:::

## Top-level fields

<!--@include: ./_generated/yaml-top-level.md-->

`request`/`response` are used by HTTP contracts; `input`/`outputMessage` by messaging contracts.

## `request` object

<!--@include: ./_generated/yaml-request.md-->

## `response` object

<!--@include: ./_generated/yaml-response.md-->

## Matchers

Each entry in a `matchers.*` list (for example `matchers.body`) applies to one path:

<!--@include: ./_generated/yaml-matcher-entry.md-->

### Matcher types — request / stub side

<!--@include: ./_generated/yaml-stub-matcher-types.md-->

### Matcher types — response / test side

The test side additionally supports `by_command`:

<!--@include: ./_generated/yaml-test-matcher-types.md-->

### Predefined regex shortcuts

Use one of these as the matcher's `predefined` value instead of a raw `value` regex:

<!--@include: ./_generated/yaml-predefined-regex.md-->

## Messaging fields

### `input` object

<!--@include: ./_generated/yaml-input.md-->

### `outputMessage` object

<!--@include: ./_generated/yaml-output-message.md-->

## Full HTTP example

<<< @/examples/contract-http.yml

## Full messaging example

<<< @/examples/contract-messaging.yml

## See also

- [HTTP Contracts](./http-contracts)
- [Messaging Contracts](./messaging-contracts)
- [Contract DSL](./contract-dsl)
