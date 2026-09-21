package dev.depitropov.commons.quarkus;

import lombok.Builder;

@Builder(toBuilder = true)
public record QuarkusMdcData(
    String user, String userId, String companyId, String msgId, String logPrefix) {}
