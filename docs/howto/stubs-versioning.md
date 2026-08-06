# How to Do Stubs Versioning?

This section covers versioning of the stubs, which you can handle in a number of different ways:

- [API Versioning](#api-versioning)
- [JAR Versioning](#jar-versioning)
- [Development or Production Stubs](#development-or-production-stubs)

## API Versioning

What does versioning really mean? If you refer to the API version, there are different approaches:

- Use hypermedia links and do not version your API by any means
- Pass the version through headers and URLs

We do not try to answer the question of which approach is better. You should pick whatever suits your needs and lets you generate business value.

Assume that you do version your API. In that case, you should provide as many contracts with as many versions as you support. You can create a subfolder for every version or append it to the contract name — whatever suits you best.

## JAR Versioning

If, by versioning, you mean the version of the JAR that contains the stubs, then there are essentially two main approaches.

Assume that you do continuous delivery and deployment, which means that you generate a new version of the jar each time you go through the pipeline and that the jar can go to production at any time. For example, your jar version looks like the following (because it got built on the 20.10.2016 at 20:15:21):

```
1.0.0.20161020-201521-RELEASE
```

In that case, your generated stub jar should look like the following:

```
1.0.0.20161020-201521-RELEASE-stubs.jar
```

In this case, you should, inside your `application.yml` or `@AutoConfigureStubRunner` when referencing stubs, provide the latest version of the stubs. You can do that by passing the `+` sign:

```java
@AutoConfigureStubRunner(ids = {"com.example:http-server-dsl:+:stubs:8080"})
```

If the versioning, however, is fixed (for example, `1.0.4.RELEASE` or `2.1.1`), you have to set the concrete value of the jar version:

```java
@AutoConfigureStubRunner(ids = {"com.example:http-server-dsl:2.1.1:stubs:8080"})
```

## Development or Production Stubs

You can manipulate the classifier to run the tests against the current development version of the stubs of other services or the ones that were deployed to production. If you alter your build to deploy the stubs with the `prod-stubs` classifier once you reach production deployment, you can run tests in one case with development stubs and in another case with production stubs.

The following example works for tests that use the development version of the stubs:

```java
@AutoConfigureStubRunner(ids = {"com.example:http-server-dsl:+:stubs:8080"})
```

The following example works for tests that use the production version of stubs:

```java
@AutoConfigureStubRunner(ids = {"com.example:http-server-dsl:+:prod-stubs:8080"})
```

You can also pass those values in properties from your deployment pipeline.
