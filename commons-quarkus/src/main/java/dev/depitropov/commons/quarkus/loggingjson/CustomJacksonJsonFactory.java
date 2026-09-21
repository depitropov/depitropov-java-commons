package dev.depitropov.commons.quarkus.loggingjson;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkiverse.loggingjson.JsonFactory;
import io.quarkiverse.loggingjson.JsonGenerator;
import io.quarkiverse.loggingjson.StringBuilderWriter;
import io.quarkiverse.loggingjson.config.Config;
import java.io.IOException;
import java.util.ServiceConfigurationError;

public class CustomJacksonJsonFactory implements JsonFactory {

  private final com.fasterxml.jackson.core.JsonFactory jsonFactory = createJsonFactory();
  private Config config;

  public CustomJacksonJsonFactory() {
    super();
  }

  @SuppressWarnings({"CallToPrintStackTrace", "java:S106"})
  private com.fasterxml.jackson.core.JsonFactory createJsonFactory() {
    final ObjectMapper objectMapper =
        (new ObjectMapper()).disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    try {
      objectMapper.findAndRegisterModules();
    } catch (final ServiceConfigurationError e) {
      System.err.println(
          "Error occurred while dynamically loading jackson modules, error: " + e.getMessage());

      e.printStackTrace();
    }

    objectMapper.registerModule(new JavaTimeModule());

    return objectMapper.getFactory().disable(Feature.FLUSH_PASSED_TO_STREAM);
  }

  @Override
  public void setConfig(final Config config) {
    this.config = config;
  }

  @Override
  public JsonGenerator createGenerator(final StringBuilderWriter writer) throws IOException {
    return new CustomJacksonJsonGenerator(jsonFactory.createGenerator(writer));
  }
}
