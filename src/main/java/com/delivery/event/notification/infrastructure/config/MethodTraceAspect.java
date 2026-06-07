package com.delivery.event.notification.infrastructure.config;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that traces every method invocation inside the application package.
 *
 * <p>Activated only when {@code logging.trace.enabled=true}. Each intercepted call emits two
 * TRACE-level log lines:
 *
 * <pre>
 * [TRACE] --&gt; ClassName.method() args=[arg1, arg2]
 * [TRACE] &lt;-- ClassName.method() result=... (12ms)
 * </pre>
 *
 * On exception the exit line reports the exception type instead of a return value.
 *
 * <p>Enable in {@code application.yml} (or per-profile):
 *
 * <pre>
 * logging:
 *   trace:
 *     enabled: true
 *   level:
 *     com.delivery.event.notification.infrastructure.config.MethodTraceAspect: TRACE
 * </pre>
 */
@Aspect
@Component
@ConditionalOnProperty(name = "logging.trace.enabled", havingValue = "true")
public class MethodTraceAspect {

  private static final Logger log = LoggerFactory.getLogger(MethodTraceAspect.class);

  /** Maximum characters used to represent a single argument or return value in the log line. */
  private static final int MAX_VALUE_LENGTH = 200;

  /**
   * Intercepts every method inside {@code com.delivery.event.notification} (all sub-packages),
   * excluding this aspect itself and security/filter infrastructure to avoid CGLIB proxy issues
   * with Servlet filter registration.
   */
  @Around(
      "within(com.delivery.event.notification..*)"
          + " && !within(com.delivery.event.notification.infrastructure.config..*)"
          + " && !target(jakarta.servlet.Filter)"
          + " && !within(com.delivery.event.notification.infrastructure.config.MethodTraceAspect)")
  public Object trace(ProceedingJoinPoint pjp) throws Throwable {
    if (!log.isTraceEnabled()) {
      return pjp.proceed();
    }

    MethodSignature sig = (MethodSignature) pjp.getSignature();
    String className = sig.getDeclaringType().getSimpleName();
    String methodName = sig.getName();
    String args = formatArgs(pjp.getArgs());

    log.trace("[TRACE] --> {}.{}() args={}", className, methodName, args);
    long start = System.currentTimeMillis();

    try {
      Object result = pjp.proceed();
      long elapsed = System.currentTimeMillis() - start;
      log.trace(
          "[TRACE] <-- {}.{}() result={} ({}ms)", className, methodName, truncate(result), elapsed);
      return result;
    } catch (Throwable ex) {
      long elapsed = System.currentTimeMillis() - start;
      log.trace(
          "[TRACE] <-- {}.{}() threw {}('{}') ({}ms)",
          className,
          methodName,
          ex.getClass().getSimpleName(),
          ex.getMessage(),
          elapsed);
      throw ex;
    }
  }

  private String formatArgs(Object[] args) {
    if (args == null || args.length == 0) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(truncate(args[i]));
    }
    sb.append("]");
    return sb.toString();
  }

  private String truncate(Object value) {
    if (value == null) {
      return "null";
    }
    String str;
    if (value.getClass().isArray()) {
      str = Arrays.deepToString(new Object[] {value});
    } else {
      str = value.toString();
    }
    if (str.length() > MAX_VALUE_LENGTH) {
      return str.substring(0, MAX_VALUE_LENGTH) + "...[truncated]";
    }
    return str;
  }
}

