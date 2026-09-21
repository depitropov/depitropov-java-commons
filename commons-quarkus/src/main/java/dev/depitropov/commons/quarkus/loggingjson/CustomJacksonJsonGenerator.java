package dev.depitropov.commons.quarkus.loggingjson;

import io.quarkiverse.loggingjson.JsonGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class CustomJacksonJsonGenerator implements JsonGenerator {

  private final com.fasterxml.jackson.core.JsonGenerator generator;

  CustomJacksonJsonGenerator(final com.fasterxml.jackson.core.JsonGenerator generator) {
    this.generator = generator;
  }

  @Override
  public void writeStartObject() throws IOException {
    generator.writeStartObject();
  }

  @Override
  public void writeEndObject() throws IOException {
    generator.writeEndObject();
  }

  @Override
  public void flush() throws IOException {
    generator.flush();
  }

  @Override
  public void close() throws IOException {
    generator.close();
  }

  @Override
  public void writeFieldName(final String name) throws IOException {
    generator.writeFieldName(name);
  }

  @Override
  public void writeObject(final Object pojo) throws IOException {
    generator.writeObject(pojo);
  }

  @Override
  public void writeObjectFieldStart(final String fieldName) throws IOException {
    generator.writeObjectFieldStart(fieldName);
  }

  @Override
  public void writeObjectField(final String fieldName, final Object pojo) throws IOException {
    generator.writeObjectField(fieldName, pojo);
  }

  @Override
  public void writeArrayFieldStart(final String fieldName) throws IOException {
    generator.writeArrayFieldStart(fieldName);
  }

  @Override
  public void writeEndArray() throws IOException {
    generator.writeEndArray();
  }

  @Override
  public void writeString(final String text) throws IOException {
    generator.writeString(text);
  }

  @Override
  public void writeStringField(final String fieldName, final String value) throws IOException {
    generator.writeStringField(fieldName, value);
  }

  @Override
  public void writeNumberField(final String fieldName, final short value) throws IOException {
    generator.writeNumberField(fieldName, value);
  }

  @Override
  public void writeNumberField(final String fieldName, final int value) throws IOException {
    generator.writeNumberField(fieldName, value);
  }

  @Override
  public void writeNumberField(final String fieldName, final long value) throws IOException {
    generator.writeNumberField(fieldName, value);
  }

  @Override
  public void writeNumberField(final String fieldName, final BigInteger value) throws IOException {
    generator.writeNumberField(fieldName, value);
  }

  @Override
  public void writeNumberField(final String fieldName, final float value) throws IOException {
    generator.writeNumberField(fieldName, value);
  }

  @Override
  public void writeNumberField(final String fieldName, final double value) throws IOException {
    generator.writeNumberField(fieldName, value);
  }

  @Override
  public void writeNumberField(final String fieldName, final BigDecimal value) throws IOException {
    generator.writeNumberField(fieldName, value);
  }
}
