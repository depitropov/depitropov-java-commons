package dev.depitropov.commons;

import static java.util.Objects.*;
import static org.apache.commons.lang3.Validate.notEmpty;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.apache.commons.codec.binary.Base32;

public final class DEUuidUtils {

  public static final int DEFAULT_MAX_ID_SIZE = 128;

  public static final String REGEXP_MATCH_VALID_ID = "^[a-zA-Z0-9_-]+$";

  public static String uuidToBase32RandomLowerCase() {
    return uuidToBase32Random().toLowerCase();
  }

  public static String uuidToBase32Random() {
    return uuidToBase32(UUID.randomUUID());
  }

  public static UUID base32ToUuid(final String base32Uuid) {

    notEmpty(base32Uuid);

    final Base32 base32 = new Base32();
    return byteArrayToUuid(base32.decode(base32Uuid.toUpperCase()));
  }

  public static String uuidToBase32(final UUID uuid) {

    requireNonNull(uuid);

    final Base32 base32 = new Base32();
    return base32.encodeAsString(uuidToByteArray(uuid)).replace("=", "").toUpperCase();
  }

  public static byte[] uuidToByteArray(final UUID uuid) {

    requireNonNull(uuid);

    final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);

    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());

    return bb.array();
  }

  public static UUID byteArrayToUuid(final byte[] bytes) {

    requireNonNull(bytes);

    final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

    final long high = byteBuffer.getLong();
    final long low = byteBuffer.getLong();

    return new UUID(high, low);
  }

  public static boolean isValidId(final String id) {
    return isValidId(id, DEFAULT_MAX_ID_SIZE);
  }

  public static boolean isValidId(final String id, final int expectedMaxSize) {
    return id != null && id.length() <= expectedMaxSize && id.matches(REGEXP_MATCH_VALID_ID);
  }
}
