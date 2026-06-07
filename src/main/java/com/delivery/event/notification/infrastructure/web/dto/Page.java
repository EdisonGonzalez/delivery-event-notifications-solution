package com.delivery.event.notification.infrastructure.web.dto;

import java.util.List;

/**
 * Generic pagination response object.
 *
 * @param <T> The type of content in the page.
 */
public record Page<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean hasNextPage,
    boolean hasPreviousPage) {

  /**
   * Factory method to create a Page instance.
   *
   * @param <T> The type of content.
   * @param content The list of items in the page.
   * @param pageNumber The current page number (zero-based).
   * @param pageSize The size of the page.
   * @param totalElements The total number of elements.
   * @return A new Page instance.
   */
  public static <T> Page<T> of(
      List<T> content, int pageNumber, int pageSize, long totalElements) {
    int totalPages = (int) Math.ceil((double) totalElements / pageSize);
    boolean hasNextPage = pageNumber < totalPages - 1;
    boolean hasPreviousPage = pageNumber > 0;
    return new Page<>(
        content, pageNumber, pageSize, totalElements, totalPages, hasNextPage, hasPreviousPage);
  }
}

