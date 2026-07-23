# How Can I Debug the Mapping, Request, or Response Being Sent by WireMock?

Starting from version `1.2.0`, we set WireMock logging to `info` and set the WireMock notifier to being verbose. Now you can exactly know what request was received by the WireMock server and which matching response definition was picked.

To turn off this feature, set WireMock logging to `ERROR` in your `application.properties`:

```properties
logging.level.com.github.tomakehurst.wiremock=ERROR
```

Or in `application.yml`:

```yaml
logging:
  level:
    com.github.tomakehurst.wiremock: ERROR
```

When verbose logging is enabled, you will see output similar to the following in your logs:

```
2021-01-01 12:00:00.000  INFO --- [qtp123-45] WireMock : Request received:
                                      ...
                                      Matched response definition:
                                      ...
```

This detailed logging helps you understand exactly what is being matched and what response is being returned, which is particularly useful when troubleshooting stubbing issues.
