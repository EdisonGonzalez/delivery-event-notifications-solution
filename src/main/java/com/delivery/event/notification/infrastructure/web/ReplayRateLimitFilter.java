package com.delivery.event.notification.infrastructure.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Simple in-process rate limiting for replay requests. */
@Component
public class ReplayRateLimitFilter extends OncePerRequestFilter {

  private final Bucket bucket =
      Bucket.builder()
          .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
          .build();

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (request.getMethod().equalsIgnoreCase("POST")
        && request.getRequestURI().contains("/replay")
        && !bucket.tryConsume(1)) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      return;
    }
    filterChain.doFilter(request, response);
  }
}
