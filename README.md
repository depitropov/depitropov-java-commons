# depitropov-commons

Small, dependency-light Java 21 libraries. Four artifacts under `dev.depitropov`,
released together on one version line.

```xml
<dependency>
  <groupId>dev.depitropov</groupId>
  <artifactId>commons</artifactId>
  <version>0.1.0</version>
</dependency>
```

| Artifact | What's in it | Pulls in |
|----------|--------------|----------|
| `commons` | errors, retries, MDC scoping, serialization, strings, ids, files | jackson, commons-lang3/io/codec, slf4j |
| `commons-test` | JSON and exception assertions, WireMock port binding | junit, assertj, json-unit (wiremock optional) |
| `commons-temporal` | non-retryable failures, search attributes, MDC, test setup | temporal-sdk (temporal-testing optional) |
| `commons-quarkus` | logging-json startup + raw-JSON fix, structured log data, MDC | quarkus, quarkiverse logging-json |

## commons

**Errors.** `BusinessRuleException` plus throw-if guards that read as the rule they enforce:

```java
ErrorUtils.isFoundResource(user, "user %s", id);      // 404
ErrorUtils.isValidArgument(amount > 0, "amount > 0"); // 400
ErrorUtils.isAllowed(canEdit, "not your record");     // 403
ErrorUtils.isConflict(!exists, "already taken");      // 409
```

`ErrorCode` / `GeneralErrorCode` map error codes to HTTP statuses and back;
`ErrorDto` is the wire shape; `ExceptionMessageExtractor` lets you teach
`ErrorUtils` how to read a message and code out of your own exception types.

**Retries.** Backoff, retryable classification by exception class, class name or
message, and 429 handling:

```java
RetryUtils.executeWithRetry(() -> client.call());
RetryUtils.executeWithRetry(() -> client.call(), RetryConfig.builder().maxRetries(5).build());
```

**MDC scoping.** Run a block with logging context and get the previous context
back afterwards — Runnable/Supplier, with or without clearing, checked-exception
variants:

```java
MdcUtils.executeWithMdcData(mdcData, () -> handle(message));
```

**Serialization.** `ObjectSerializer` wraps Jackson in three flavours —
throwing, logging, and silent — over `Class`, `TypeReference` or `JavaType`.

**Also:** `DEStringUtils` (masking, truncation, printable-char stripping,
gzip↔base64 log payloads), `DEUuidUtils` (base32 short ids), `DEFileUtils`
(guarded delete, counted copies), `DEResourceUtils`, `TempDirectoryUtils`,
`DECsvSanitizer` (formula-injection guard), `OptionalFieldDto` (absent vs.
explicit null for PATCH), `PageDto`.

## Build

```bash
./mvnw clean install       # needs nothing but a JDK 21
./mvnw spotless:apply      # google-java-format
./utils/git-hooks/install_client_hooks.sh
```

## Release

```bash
./utils/release.sh 1.0.0                # sets version, commits, tags, reopens 1.0.1-SNAPSHOT
git push && git push origin v1.0.0
./mvnw -Prelease deploy                 # javadoc + sources + GPG + Central
```

The `release` profile carries everything Maven Central requires and is kept out
of the default build, so a plain `install` works without a GPG key. Publishing
needs a signing key and a Central Portal token in `~/.m2/settings.xml`.

## License

Apache-2.0.
