package dev.depitropov.commons.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;

class DEWireMockUtilsTest {

  @Test
  void startWireMockServerUntilFreePortFoundTest() {
    final WireMockServer wireMockServer = DEWireMockUtils.startWireMockServerUntilFreePortFound();

    assertThat(wireMockServer).isNotNull();

    wireMockServer.stop();
  }

  @Test
  void findRandomPortTest() {
    assertThat(DEWireMockUtils.findRandomPort())
        .isGreaterThanOrEqualTo(1024)
        .isLessThanOrEqualTo(65535);
  }

  @Test
  void testFindRandomPortTest() {
    assertThat(DEWireMockUtils.findRandomPort(10, 100))
        .isGreaterThanOrEqualTo(10)
        .isLessThanOrEqualTo(100);
  }
}
