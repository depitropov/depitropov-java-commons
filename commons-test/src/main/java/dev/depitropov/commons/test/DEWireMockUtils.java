package dev.depitropov.commons.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import java.util.Random;

public final class DEWireMockUtils {

  public static final int PORT_RANGE_MIN = 1024;

  public static final int PORT_RANGE_MAX = 65535;

  private static final Random random = new Random(System.nanoTime());

  public static WireMockServer startWireMockServerUntilFreePortFound() {
    while (true) {
      final int wireMockPort = findRandomPort();
      final WireMockServer wireMockServer = new WireMockServer(wireMockPort);

      try {
        wireMockServer.start();

        return wireMockServer;
      } catch (final FatalStartupException e) {
        if (!e.getMessage().contains("Failed to bind to")
            && !e.getMessage().contains("Address already in use")) {
          throw e;
        }
      }
    }
  }

  public static int findRandomPort() {
    return findRandomPort(PORT_RANGE_MIN, PORT_RANGE_MAX);
  }

  public static int findRandomPort(final int minPort, final int maxPort) {
    final int portRange = maxPort - minPort;
    return minPort + random.nextInt(portRange + 1);
  }
}
