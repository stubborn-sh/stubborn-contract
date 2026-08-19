# Changelog

All notable changes to Stubborn Contract are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [0.1.0] - 2026-08-19

### ♻️ Migration
- End-to-end OpenRewrite recipe tests (verifier props + full composite) (#129) (#129)
- Add Stub Runner property-key migration recipe (#110) (#110)

### ✅ Tests
- Migrate off Spock to JUnit 5 (#142) (#142)
- Stop deleting shared request.json fixtures; restore file-body golden corpus (#80) (#80)
- Add test case - find stub file in jar

### ⬆️ Dependencies
- Bump kotlin.version from 2.2.21 to 2.4.10 (#125) (#125)
- Bump org.jspecify:jspecify from 1.0.0 to 1.0.1 (#123) (#123)
- Bump org.junit:junit-bom from 6.0.3 to 6.1.3 (#121) (#121)
- Bump camel.version from 4.21.0 to 4.22.0 (#120) (#120)
- Bump actions/cache from 4.2.0 to 6.1.0 (#126) (#126)
- Bump actions/upload-artifact from 4.6.2 to 7.0.1 (#124) (#124)
- Bump actions/setup-java from 4.8.0 to 5.7.0 (#119) (#119)
- Bump actions/checkout from 4.3.1 to 7.0.1 (#118) (#118)
- Bump docker/login-action from 3.7.0 to 4.6.0 (#117) (#117)

### 🏗️ Build & CI
- Drop dead snapshot repo + cover Quarkus messaging transport selection (#153) (#153)
- Extract inline GHA shell logic into BATS-tested scripts (#104) (#104)
- Sever the model-based response-body tail from SingleMethodBuilder (#77) (#77)

### 🐛 Bug Fixes
- Change scope of constants
- Extract fqn to constants and add tests
- Correct class name of HttpTransporterFactory
- Potential fix for CI problems
- Potential fix for CI problems
- Added tests for #2367; fixes gh-2368
- Adds an empty list to nonFilteredFileExtensions (#2367)
- Use exec for better PID 1 handling (#2156)
- Adoc code highlighting syntax (#1817) (#1820)
- Support multiple stubs files directories (bug fix) (#1801)
- Pom.xml to reduce vulnerabilities (#1576)
- Pom.xml to reduce vulnerabilities (#1504)
- Pom.xml to reduce vulnerabilities (#1503)
- Pom.xml to reduce vulnerabilities (#1473)
- Pom.xml to reduce vulnerabilities (#1214)
- Pom.xml to reduce vulnerabilities (#1214)
- #1160
- #1160

### 📚 Documentation
- Drop the Spring Cloud CLI launcher section (#149) (#149)
- Generate reference tables from code + CI drift-gate (single source of truth) (#148) (#148)
- Quarkus tier + SCC-reference cleanup; fix docker camelVersion drift (Publish Docker Images red) (#147) (#147)
- Reflect Groovy relocation; rename groovyDsl→contractDsl (#134) (#134)
- Correct module map — messaging abstractions live in the core (Spring-free) (#106) (#106)
- Sweep Maven-plugin site asciidoc + remove vestigial README generator source (#97) (#97)
- Bring READMEs and reference docs current (coordinates, Handlebars rendering, migration link) (#95) (#95)
- Add the referenced test-generation migration design doc (#79) (#79)
- Module guide, consumer installation, WireMock pages + migration corrections (#67) (#67)
- Update JUnit5 example, fixes gh-1746 (#1757)
- Explain how to have multiple contracts in Groovy (#1755)

### 🔧 Other
- Pin httpclient5 5.6.4 (CVE-2026-71290) + docs nav (#151) (#151)
- SECURITY.md + eliminate disabled tests (#135) (#135)
- Resolve classpath stubs placed directly under the artifact folder (#130) (#130)
- Bump actions/setup-node from 4 to 7 (#54) (#54)
- Align kafka-avro-serializer with Spring Boot 4.1 (7.9.8 → 8.2.1) (#103) (#103)
- Bump actions/setup-java from 4.8.0 to 5.7.0 (#50) (#50)
- Rename packages, groupIds, artifactIds to sh.stubborn
- Rename packages, group IDs, artifact IDs, version
- //github.com/spring-cloud/spring-cloud-contract/issues/1935 (#1937)
- Fix validations for xml #1494 (#1495)
- Handle xml with namespaces for validations #1427 (#1446)
- Handle xml with namespaces for validations #1427 (#1446)
- Handle xml with namespaces closes #1427 (#1445)
- Gradle plugin now applies properties set in build.gradle property
- Further DirtiesContext fix where the context is refreshed during the test phase (#1048)
- Further DirtiesContext fix where the context is refreshed during the test phase (#1048)
- Register port now only handles auto port values. This is to avoid the httpsPortDynamic being set to true on the context initialisation, but then being updated to false when the context is refreshed. (#1040)
- Wiremock: Fix for Spring context always being set to Dirty. (#884)
- Rest Docs stubs with no body
- Added logging config for debugging to FAQ (#259)
- Created new DslProperty creation methods (#247)
- Verifying if lower assertj version will not cause Travis fail.
- Run

### 🚀 Features
- Out-of-the-box JSON conversion for @JmsListener + Kafka/Rabbit consumer doc fixes (#157) (#157)
- Make the OOB Rabbit converter ignore the __TypeId__ header (#156) (#156)
- Force INFERRED type precedence on the OOB Rabbit converter (#155) (#155)
- Back off OOB Kafka converter when a value-deserializer is configured (#154) (#154)
- Out-of-the-box JSON conversion for consumer contract tests (#152) (#152)
- Shared conformance suite across Kafka, Rabbit and JMS (#127) (#127)
- Split sender and receiver into independent types (#116) (#116)
- Spring-free JMS MessageVerifier building block (#115) (#115)
- Stub-runner messaging integration (real broker, CDI-free) (#114) (#114)
- Spring-free RabbitMQ MessageVerifier building block (#113) (#113)
- Precise, deterministic receive for the real-broker lane (#112) (#112)
- Consolidate Kafka onto a Spring-free building block (#111) (#111)
- Quarkus stub-runner integration (#109) (#109)
- Model-based response cookie assertions (JavaPoet Phase 4, slice 2) (#76) (#76)
- Model-based response status + header assertions (JavaPoet Phase 4, slice 1) (#75) (#75)
- Model-based file-based request bodies (JavaPoet Phase 3, slice 5) (#74) (#74)
- Model-based multipart + async (JavaPoet Phase 3, slice 4) (#73) (#73)
- Model-based cookies + query params (JavaPoet Phase 3, slice 3) (#72) (#72)
- Model-based request body emission (JavaPoet Phase 3, slice 2) (#71) (#71)
- Model-based request emission (JavaPoet Phase 3, slice 1) (#70) (#70)
- Add Gradle build-script migration recipe (SCC → Stubborn) (#68) (#68)


