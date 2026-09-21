package dev.depitropov.commons;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static dev.depitropov.commons.DEFileUtils.copyURLToFile;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DEFileUtilsTest {

  private WireMockServer wireMockServer;

  private final Random random = new Random();

  private String wireMockBseUrl;

  @BeforeEach
  void setUp() {

    int wireMockPort;

    do {
      do {
        wireMockPort = random.nextInt(65535);

      } while (wireMockPort <= 1024);

      wireMockServer = new WireMockServer(wireMockPort);

      try {
        wireMockServer.start();
      } catch (final FatalStartupException e) {
        continue;
      }

      break;
    } while (true);

    wireMockBseUrl = format("http://localhost:%d", wireMockPort);

    configureFor("localhost", wireMockPort);
  }

  @AfterEach
  void tearDown() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @SuppressWarnings("java:S5443")
  @Test
  void safeDeleteDirTest() throws IOException {
    final Path tempDirWithPrefix = Files.createTempDirectory("test");
    tempDirWithPrefix.toFile().deleteOnExit();

    final Path dir1 =
        Files.createDirectories(Paths.get(tempDirWithPrefix.toAbsolutePath().toString(), "dir1"));

    final Path dirNotExisting =
        Paths.get(tempDirWithPrefix.toAbsolutePath().toString(), "dirNotExisting");

    Files.createTempFile(dir1, "testPrefix", "testSuffix");

    DEFileUtils.safeDeleteDir(null, "r");

    DEFileUtils.safeDeleteDir(tempDirWithPrefix, "unknown_prefix");
    assertTrue(tempDirWithPrefix.toFile().exists());

    DEFileUtils.safeDeleteDir(dirNotExisting, "dirNotExisting");
    assertTrue(tempDirWithPrefix.toFile().exists());

    assertTrue(dir1.toFile().exists());
    DEFileUtils.safeDeleteDir(dir1, "dir1");
    assertFalse(dir1.toFile().exists());
  }

  @Test
  void deleteFileTest() {
    TempDirectoryUtils.executeWithTempDirectory(
        tempDir -> {
          final Path filePath = Path.of(tempDir.toAbsolutePath().toString(), "test.txt");

          FileUtils.writeStringToFile(filePath.toFile(), "test", UTF_8);

          assertTrue(filePath.toFile().exists());

          DEFileUtils.deleteFile(filePath);

          assertFalse(filePath.toFile().exists());

          DEFileUtils.deleteFile(filePath);
        });
  }

  @Test
  void setLFLineSeparatorTest() {
    assertNull(DEFileUtils.setLFLineSep(null));
    assertEquals("", DEFileUtils.setLFLineSep(""));
    assertEquals("\n", DEFileUtils.setLFLineSep("\r"));
    assertEquals("\n\n\n", DEFileUtils.setLFLineSep("\r\r\r"));
    assertEquals("\n\n\n", DEFileUtils.setLFLineSep("\r\n\n\r\n"));
    assertEquals(
        "  Test\n  Line2\n Line3 \nLine4 \n   Line5\n",
        DEFileUtils.setLFLineSep("  Test\r\n  Line2\n Line3 \r\nLine4 \r   Line5\r\n"));
  }

  @SneakyThrows
  @Test
  void copyURLToFileTestMaxAllowdSize() {
    stubFor(
        get(urlEqualTo("/test-big-file"))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE, "application/pdf")
                    .withBody(new byte[10 * 1024 * 1024])));

    final Path temp = Files.createTempFile("copyURLToFileTest", ".pdf");

    copyURLToFile(new URL(wireMockBseUrl + "/test-big-file"), temp.toFile(), 10000, 10000, 0);

    assertEquals(10 * 1024 * 1024, FileUtils.readFileToByteArray(temp.toFile()).length);

    final URL url = new URL(wireMockBseUrl + "/test-big-file");
    final File tempFile = temp.toFile();

    final IOException t =
        assertThrows(IOException.class, () -> copyURLToFile(url, tempFile, 10000, 10000, 100));
    assertEquals("Max allowed size reached: 100", t.getMessage());
  }

  @SneakyThrows
  @Test
  void copyURLToFileTestReadTimeout() {
    stubFor(
        get(urlEqualTo("/test-big-file"))
            .willReturn(
                aResponse()
                    .withFixedDelay(1000)
                    .withHeader(CONTENT_TYPE, "application/pdf")
                    .withBody(new byte[10 * 1024 * 1024])));

    final Path temp = Files.createTempFile("copyURLToFileTest", ".pdf");

    final URL url = new URL(wireMockBseUrl + "/test-big-file");
    final File tempFile = temp.toFile();

    final IOException t =
        assertThrows(IOException.class, () -> copyURLToFile(url, tempFile, 10000, 500, 0));
    assertEquals("Read timed out", t.getMessage());
  }
}
