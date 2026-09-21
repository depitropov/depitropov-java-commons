package dev.depitropov.commons;

import java.time.format.DateTimeFormatter;

public final class DETimeUtils {

  public static final String UTC = "UTC";

  public static final String DATE_TIME_FORMAT_ISO = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]";

  public static final DateTimeFormatter DATE_TIME_FORMATTER_ISO =
      DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_ISO);

  public static final String DATE_FORMAT_ISO = "yyyy-MM-dd";

  public static final DateTimeFormatter DATE_FORMATTER_ISO =
      DateTimeFormatter.ofPattern(DATE_FORMAT_ISO);
}
