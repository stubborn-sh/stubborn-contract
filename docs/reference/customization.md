# Customization

Stubborn Contract exposes several SPIs for adapting stub generation, WireMock behavior, and test generation to your needs.

## Custom contract converter

`ContractConverter` lets you load contracts from non-standard formats (e.g., OpenAPI, Pact, custom DSL). Implement the SPI and register it as a Spring bean or via `ServiceLoader`:

```java
public class OpenApiContractConverter implements ContractConverter<OpenApiDocument> {

    @Override
    public boolean isAccepted(File file) {
        return file.getName().endsWith(".yaml") && isOpenApiFile(file);
    }

    @Override
    public Collection<Contract> convertFrom(File file) {
        OpenApiDocument doc = parseOpenApi(file);
        return doc.getPaths().entrySet().stream()
            .flatMap(e -> toContracts(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public OpenApiDocument convertTo(Collection<Contract> contracts) {
        throw new UnsupportedOperationException("OpenAPI export not supported");
    }
}
```

Register via `META-INF/services/sh.stubborn.contract.verifier.converter.ContractConverter` or `@Component` (Spring Boot auto-detection).

## Custom stub strategy

`WireMockStubStrategy` controls how a WireMock stub is built from a single contract. `BaseWireMockStubStrategy` is a convenience base you can extend. Override `buildClientRequest` or `buildServerResponse` to add custom headers, transform the body, or apply dynamic matchers:

```java
public class CachingStubStrategy extends BaseWireMockStubStrategy {
    // Override buildClientRequest() or buildServerResponse() as needed
}
```

Register by providing the class name in the plugin configuration or via `ServiceLoader`.

## WireMock customizer

`WireMockConfigurationCustomizer` lets you add WireMock extensions, transformers, or server-level configuration:

```java
@Configuration
public class WireMockConfig {

    @Bean
    public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
        return config -> config
            .extensions(new ResponseTemplateTransformer(true))
            .maxRequestJournalEntries(100);
    }
}
```

Register this bean in a `@Configuration` class in your producer tests.

## Response template transformer

WireMock's Handlebars-based response templates let stubs return dynamic values extracted from the request:

```java
@Bean
public WireMockConfigurationCustomizer wireMockConfigurer() {
    return config -> config.extensions(new ResponseTemplateTransformer(false));
}
```

Then reference request data in stub body templates:

```json
{
  "id": "{{request.pathSegments.[2]}}",
  "name": "{{jsonPath request.body '$.name'}}"
}
```

Set to `true` (global mode) to apply templates to all responses, or `false` (per-stub) to enable via `transformers` in each stub mapping.

## Custom test base class

The generated producer tests extend a base class you define. Override it to set up your Spring context, mock dependencies, or configure Hamcrest matchers:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class FraudDetectionBase {

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }
}
```

Set the base class in `pom.xml`:

```xml
<plugin>
  <groupId>sh.stubborn.contract</groupId>
  <artifactId>stubborn-contract-maven-plugin</artifactId>
  <configuration>
    <baseClassForTests>com.example.FraudDetectionBase</baseClassForTests>
  </configuration>
</plugin>
```

## Per-package base classes

For large projects with multiple API surfaces, map contract directories to different base classes:

```xml
<baseClassMappings>
  <baseClassMapping>
    <contractPackageRegex>.*fraud.*</contractPackageRegex>
    <baseClassFQN>com.example.FraudBase</baseClassFQN>
  </baseClassMapping>
  <baseClassMapping>
    <contractPackageRegex>.*payment.*</contractPackageRegex>
    <baseClassFQN>com.example.PaymentBase</baseClassFQN>
  </baseClassMapping>
</baseClassMappings>
```

## Custom message verifier

For messaging tests with non-standard brokers, implement `MessageVerifierSender` and `MessageVerifierReceiver`:

```java
@Component
public class CustomMessageVerifier implements MessageVerifierSender<Message<?>> {
    @Override
    public void send(Message<?> message, String destination, @Nullable YamlContract contract) {
        // send via your messaging infrastructure
    }
}
```

## See also

- [Maven Plugin](./maven-plugin)
- [Gradle Plugin](./gradle-plugin)
- [Stub Runner Reference](./stub-runner)
