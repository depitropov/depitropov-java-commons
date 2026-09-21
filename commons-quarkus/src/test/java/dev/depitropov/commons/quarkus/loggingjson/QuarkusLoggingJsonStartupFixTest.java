package dev.depitropov.commons.quarkus.loggingjson;

import static io.quarkiverse.loggingjson.providers.KeyValueStructuredArgument.kv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Slf4j
class QuarkusLoggingJsonStartupFixTest {

  @Test
  void onStart() {

    //noinspection LoggingPlaceholderCountMatchesArgumentCount
    log.info("Test log message", kv("key", Map.of("key1", "value1", "key2", Instant.now())));

    assertThat(QuarkusLoggingJsonStartupFix.isReplaced.get()).isTrue();
  }
}
