package dev.depitropov.commons;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectSerializer {

  String toString(Object object);

  String toStringSuppressLog(Object object);

  String toStringThrow(Object object);

  <T> T toObject(String json, ClassTypeProvider<T> classTypeProvider);

  <T> T toObjectSuppressLog(String json, ClassTypeProvider<T> classTypeProvider);

  <T> T toObjectThrow(String json, ClassTypeProvider<T> classTypeProvider);

  ObjectMapper objectMapper();
}
