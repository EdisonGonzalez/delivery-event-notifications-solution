package com.delivery.event.notification.infrastructure.web.dto;

import java.util.List;

/**
 * DTO for paginated responses.
 *
 * @param <T> The type of content in the page.
 */
public record PageDto<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean hasNextPage,
    boolean hasPreviousPage) {}

