package dev.depitropov.commons;

public final class MdcKeys {

  /** This is automatically injected by Datadog agent if env DD_LOGS_INJECTION=true */
  public static final String MDC_TRACE_ID = "dd.trace_id";

  /** This is automatically injected by Datadog agent if env DD_LOGS_INJECTION=true */
  public static final String MDC_SPAN_ID = "dd.span_id";

  /** This is resolved from JWT username claim. */
  public static final String MDC_USER = "user";

  /** This is resolved from JWT userId claim. */
  public static final String MDC_USER_ID = "userId";

  /** This is resolved from JWT companyId claim. */
  public static final String MDC_COMPANY_ID = "companyId";

  /** This is resolved from: com.google.pubsub.v1.PubsubMessage#getMessageId() */
  public static final String MDC_MSG_ID = "msgId";

  /** This is used as prefix to each log message */
  public static final String MDC_LOG_PREFIX = "logPrefix";

  /** This is resolved from HTTP request. */
  public static final String MDC_USER_AGENT = "userAgent";
}
