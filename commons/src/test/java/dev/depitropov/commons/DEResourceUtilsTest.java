package dev.depitropov.commons;

import static dev.depitropov.commons.DEResourceUtils.copyToFile;
import static dev.depitropov.commons.DEResourceUtils.findResourceInputStream;
import static dev.depitropov.commons.DEResourceUtils.loadResource;
import static dev.depitropov.commons.DEResourceUtils.loadResourceAsBytes;
import static dev.depitropov.commons.DEResourceUtils.loadResourceLFLineSep;
import static dev.depitropov.commons.DEResourceUtils.loadResourceSystemLineSep;
import static dev.depitropov.commons.TempDirectoryUtils.executeWithTempDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DEResourceUtilsTest {

  @Test
  void findResourceInputStreamTest() throws IOException {
    executeWithTempDirectory(
        tempDir -> {
          final Path filePath = Path.of(tempDir.toAbsolutePath().toString(), "test.txt");

          FileUtils.writeStringToFile(filePath.toFile(), "test", UTF_8);

          try (final InputStream is =
              findResourceInputStream(filePath.toAbsolutePath().toString())) {
            assertEquals("test", IOUtils.toString(is, UTF_8));
          }
        });

    try (final InputStream is = findResourceInputStream("test-resource-utils-1.txt")) {
      assertEquals("test1".strip(), IOUtils.toString(is, UTF_8).strip());
    }

    assertThrows(IOException.class, () -> findResourceInputStream("unknown" + UUID.randomUUID()));
  }

  @Test
  void loadResourceFromClassTest() {
    /*
     * Here we couple our testing with one of our dependencies as we cannot test for our test
     * resources as they can be in the system class path while the dependent jar are loaded through
     * the class/class loader.
     */
    assertTrue(loadResource("/META-INF/maven/commons-io/commons-io/pom.properties").length() > 0);
  }

  @Test
  void loadResourceTest() {
    assertEquals("test1".strip(), loadResource("test-resource-utils-1.txt").strip());
  }

  @Test
  void loadResourceLFLineSepTest() {
    assertEquals("test1\n", loadResourceLFLineSep("test-resource-utils-1.txt"));
  }

  @Test
  void loadResourceSystemLineSepTest() {
    assertEquals(
        "test1" + System.lineSeparator() + "test2",
        loadResourceSystemLineSep("test-resource-utils-2.txt"));
  }

  @Test
  void loadResourceAsBytesTest() {
    Assertions.assertArrayEquals(
        new byte[] {0x01, 0x02, 0x03}, loadResourceAsBytes("test-resource-utils-1.dat"));
  }

  @Test
  void copyToFileTest() {
    executeWithTempDirectory(
        tempDir -> {
          final Path filePath = Path.of(tempDir.toAbsolutePath().toString(), "test.txt");

          copyToFile("test-resource-utils-1.txt", filePath);

          assertEquals("test1", FileUtils.readFileToString(filePath.toFile(), UTF_8).strip());
        });
  }
}
