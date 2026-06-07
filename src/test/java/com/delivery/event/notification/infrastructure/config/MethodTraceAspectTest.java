package com.delivery.event.notification.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link MethodTraceAspect}.
 *
 * <p>LENIENT strictness is required because stubs for signature/args are only consumed when the
 * TRACE log level is active. In a standard test run the logger is at INFO/DEBUG, so those stubs go
 * unused but are still semantically correct.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MethodTraceAspectTest {

  private MethodTraceAspect aspect;

  @Mock private ProceedingJoinPoint pjp;

  @Mock private MethodSignature signature;

  @BeforeEach
  void setUp() {
    aspect = new MethodTraceAspect();
  }

  @Test
  void trace_shouldProceedAndReturnResult() throws Throwable {
    when(pjp.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(Object.class);
    when(signature.getName()).thenReturn("someMethod");
    when(pjp.getArgs()).thenReturn(new Object[] {"arg1", 42});
    when(pjp.proceed()).thenReturn("expectedResult");

    Object result = aspect.trace(pjp);

    assertEquals("expectedResult", result);
    verify(pjp, times(1)).proceed();
  }

  @Test
  void trace_shouldProceedWithNoArgs() throws Throwable {
    when(pjp.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(Object.class);
    when(signature.getName()).thenReturn("noArgMethod");
    when(pjp.getArgs()).thenReturn(new Object[] {});
    when(pjp.proceed()).thenReturn(null);

    Object result = aspect.trace(pjp);

    assertNull(result);
    verify(pjp, times(1)).proceed();
  }

  @Test
  void trace_shouldPropagateRuntimeException() throws Throwable {
    when(pjp.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(Object.class);
    when(signature.getName()).thenReturn("failingMethod");
    when(pjp.getArgs()).thenReturn(new Object[] {});
    when(pjp.proceed()).thenThrow(new RuntimeException("boom"));

    assertThrows(RuntimeException.class, () -> aspect.trace(pjp));
    verify(pjp, times(1)).proceed();
  }

  @Test
  void trace_shouldHandleNullArgs() throws Throwable {
    when(pjp.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(Object.class);
    when(signature.getName()).thenReturn("nullArgsMethod");
    when(pjp.getArgs()).thenReturn(null);
    when(pjp.proceed()).thenReturn("ok");

    Object result = aspect.trace(pjp);

    assertEquals("ok", result);
    verify(pjp, times(1)).proceed();
  }

  @Test
  void trace_shouldHandleLargeArguments() throws Throwable {
    when(pjp.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(Object.class);
    when(signature.getName()).thenReturn("bigArgMethod");
    when(pjp.getArgs()).thenReturn(new Object[] {"x".repeat(500)});
    when(pjp.proceed()).thenReturn("ok");

    Object result = aspect.trace(pjp);

    assertEquals("ok", result);
    verify(pjp, times(1)).proceed();
  }

  @Test
  void trace_shouldPropagateCheckedException() throws Throwable {
    when(pjp.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(Object.class);
    when(signature.getName()).thenReturn("checkedMethod");
    when(pjp.getArgs()).thenReturn(new Object[] {});
    when(pjp.proceed()).thenThrow(new Exception("checked"));

    assertThrows(Exception.class, () -> aspect.trace(pjp));
    verify(pjp, times(1)).proceed();
  }
}
