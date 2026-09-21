package dev.depitropov.commons.temporal;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record WorkflowExecutionTime(Instant startTime, Instant executionTime, Instant closeTime) {}
