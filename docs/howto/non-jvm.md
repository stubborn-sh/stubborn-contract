# Using Stubborn Contract Without the JVM

Stubborn Contract was originally built for the JVM but the contract format — YAML files describing HTTP and messaging interactions — is language-agnostic. The `@stubborn-sh` npm packages provide first-class support for Node.js consumers and producers.

## Packages

| Package | Purpose |
|---|---|
| `@stubborn-sh/jest` | Jest matcher for verifying contracts against real implementations |
| `@stubborn-sh/stub-server` | WireMock-compatible HTTP stub server powered by contract stubs |
| `@stubborn-sh/publisher` | Publish contracts and stubs to the Stubborn Broker |
| `@stubborn-sh/verifier` | Verify a running Node.js producer against its contracts |

## Consumer-side testing (Node.js)

Start the stub server from your contracts and run your consumer tests against it:

```bash
npm install --save-dev @stubborn-sh/stub-server
```

```ts
// jest.setup.ts
import { StubServer } from '@stubborn-sh/stub-server'

let stubServer: StubServer

beforeAll(async () => {
  stubServer = await StubServer.start({
    stubsDir: './stubs',        // directory with WireMock JSON stubs
    port: 8090,
  })
})

afterAll(async () => {
  await stubServer.stop()
})
```

```ts
// order.consumer.test.ts
import { fetchOrder } from '../src/order-client'

test('fetches order by id', async () => {
  const order = await fetchOrder(1)   // hits the stub server on port 8090
  expect(order.status).toBe('CREATED')
})
```

## Producer-side contract verification (Node.js)

If your producer is a Node.js HTTP service, use `@stubborn-sh/verifier` to run the contract tests against it:

```bash
npm install --save-dev @stubborn-sh/verifier
```

```ts
// contracts.test.ts
import { ContractVerifier } from '@stubborn-sh/verifier'

describe('Contract verification', () => {
  const verifier = new ContractVerifier({
    contractsDir: './contracts',
    baseUrl: 'http://localhost:3000',   // your running producer
  })

  verifier.generateTests()   // creates one test per contract
})
```

Start your Express/Fastify/NestJS app before the tests and point the verifier at it:

```ts
// jest.setup.ts
import app from '../src/app'

let server: http.Server

beforeAll(() => {
  server = app.listen(3000)
})

afterAll(() => {
  server.close()
})
```

## Publishing contracts to the Broker

Use `@stubborn-sh/publisher` to publish your contracts and stubs to the [Stubborn Broker](https://docs.stubborn.sh/stubborn/features/contract-publishing):

```bash
npm install --save-dev @stubborn-sh/publisher
```

```ts
// publish.ts
import { publish } from '@stubborn-sh/publisher'

await publish({
  brokerUrl: 'https://your-broker.example.com',
  appName: 'order-service',
  appVersion: process.env.npm_package_version!,
  contractsDir: './contracts',
  stubsDir: './stubs',
  token: process.env.BROKER_TOKEN,
})
```

Add to your CI pipeline after tests pass:

```yaml
# .github/workflows/ci.yml
- name: Publish contracts
  run: npx ts-node scripts/publish.ts
  env:
    BROKER_TOKEN: ${{ secrets.BROKER_TOKEN }}
```

## Jest integration

`@stubborn-sh/jest` provides Jest-native matchers for asserting contract compliance:

```ts
import '@stubborn-sh/jest'

test('response matches contract', async () => {
  const response = await fetch('http://localhost:3000/orders/1')
  const body = await response.json()

  await expect(response).toMatchContract('./contracts/get-order.yml')
})
```

## Writing YAML contracts for Node.js services

The contract format is identical to JVM contracts. Use the [YAML Schema reference](/reference/yaml-contracts):

```yaml
description: "Return order by ID"
request:
  method: GET
  url: /orders/1
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    id: 1
    status: CREATED
    amount: 49.99
  matchers:
    body:
      - path: $.id
        type: by_type
      - path: $.status
        type: by_regex
        value: "CREATED|PROCESSING|SHIPPED|DELIVERED"
```

## Mixed-language teams

In a team where the producer is a JVM service and the consumer is Node.js (or vice versa), no special configuration is needed. The contracts are plain YAML files committed to the producer repo. The consumer team uses the generated WireMock stubs via `@stubborn-sh/stub-server`, regardless of what technology produced them.

::: info Cross-language contract flow
1. JVM producer publishes stubs to the Broker
2. Node.js consumer downloads stubs: `@stubborn-sh/stub-server` loads them
3. Consumer tests pass — the contract is verified on both sides
:::

## See also

- [Stubborn Broker docs](https://docs.stubborn.sh/stubborn/) — contract registry for cross-team sharing
- [HTTP Contracts](/reference/http-contracts)
- [YAML Schema](/reference/yaml-contracts)
