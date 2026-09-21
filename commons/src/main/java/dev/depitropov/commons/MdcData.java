package dev.depitropov.commons;

import lombok.Builder;

@Builder(toBuilder = true)
public record MdcData(
    String traceId,
    String spanId,
    String user,
    String userId,
    String companyId,
    String msgId,
    String logPrefix,
    String userAgent) {}
