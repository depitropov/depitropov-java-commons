package dev.depitropov.commons.quarkus.loggingjson;

import io.quarkiverse.loggingjson.JsonFormatter;
import io.quarkiverse.loggingjson.JsonProvider;
import io.quarkiverse.loggingjson.config.Config;
import io.quarkiverse.loggingjson.config.ConfigConsole;
import io.quarkiverse.loggingjson.config.ConfigFile;
import io.quarkiverse.loggingjson.config.ConfigSocket;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 * This is a workaround for the problem described here in native mode:
 *
 * <p><a href="https://github.com/quarkiverse/quarkus-logging-json/issues/305">Quarkus Logging Json
 * Issue #305</a>
 *
 * <p>In short it register a new JsonFormatter with a custom JsonFactory that uses a custom
 * ObjectMapper that has the needed JavaTimeModule registered.
 */
@ApplicationScoped
@Slf4j
public class QuarkusLoggingJsonStartupFix {

  static final AtomicBoolean isReplaced = new AtomicBoolean(false);

  @SneakyThrows
  @SuppressWarnings({"java:S1181"})
  void onStart(@Observes final StartupEvent ev) {
    log.info("The application is starting...");

    try {
      setJsonFormatter();
    } catch (final Throwable t) {
      log.error("Failed to set JsonFactory, error: {}", t.getMessage(), t);
    }
  }

  @SuppressWarnings("resource")
  public static void setJsonFormatter() {
    final QuarkusDelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;

    final AsyncHandler handler =
        Arrays.stream(delayedHandler.getHandlers())
            .filter(AsyncHandler.class::isInstance)
            .findFirst()
            .map(AsyncHandler.class::cast)
            .orElseThrow();

    final ConsoleHandler consoleHandler =
        Arrays.stream(handler.getHandlers())
            .filter(ConsoleHandler.class::isInstance)
            .findFirst()
            .map(ConsoleHandler.class::cast)
            .orElseThrow();

    if (consoleHandler.getFormatter() instanceof final JsonFormatter jsonFormatter) {
      replaceJsonFormatter(consoleHandler, jsonFormatter);
    }
  }

  @SuppressWarnings("java:S112")
  private static void replaceJsonFormatter(
      final ConsoleHandler consoleHandler, final JsonFormatter oldJsonFormatter) {

    if (isReplaced.getAndSet(true)) {
      log.info("JsonFormatter already replaced");

      return;
    }

    try {
      final List<JsonProvider> providers = getProviders(oldJsonFormatter);

      final String recordDelimiter = getRecordDelimiter(oldJsonFormatter);

      final Config config = createConfig(recordDelimiter);

      final CustomJacksonJsonFactory customFactory = new CustomJacksonJsonFactory();
      customFactory.setConfig(config);

      consoleHandler.setFormatter(new JsonFormatter(providers, customFactory, config));

    } catch (final RuntimeException re) {

      isReplaced.set(false);

      throw re;

    } catch (final Exception t) {

      isReplaced.set(false);

      throw new RuntimeException("Failed to replace JsonFormatter", t);
    }

    log.info("JsonFormatter replaced");
  }

  @SneakyThrows
  @SuppressWarnings({"unchecked", "java:S3011"})
  private static List<JsonProvider> getProviders(final JsonFormatter jsonFormatter) {

    final Field jsonFactoryField = JsonFormatter.class.getDeclaredField("providers");

    jsonFactoryField.setAccessible(true);

    return (List<JsonProvider>) jsonFactoryField.get(jsonFormatter);
  }

  @SneakyThrows
  @SuppressWarnings({"java:S3011"})
  private static String getRecordDelimiter(final JsonFormatter jsonFormatter) {

    final Field recordDelimiterField = JsonFormatter.class.getDeclaredField("recordDelimiter");

    recordDelimiterField.setAccessible(true);

    return (String) recordDelimiterField.get(jsonFormatter);
  }

  private static Config createConfig(final String recordDelimiter) {
    return new Config() {
      @Override
      public ConfigConsole console() {
        return null;
      }

      @Override
      public ConfigFile file() {
        return null;
      }

      @Override
      public ConfigSocket socket() {
        return null;
      }

      @Override
      public FieldsConfig fields() {
        return null;
      }

      @Override
      public boolean prettyPrint() {
        return false;
      }

      @Override
      public String recordDelimiter() {
        return recordDelimiter;
      }

      @Override
      public Map<String, AdditionalFieldConfig> additionalField() {
        return null;
      }

      @Override
      public LogFormat logFormat() {
        return LogFormat.DEFAULT;
      }
    };
  }
}
