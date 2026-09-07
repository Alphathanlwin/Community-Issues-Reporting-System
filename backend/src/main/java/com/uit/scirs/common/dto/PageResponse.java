package com.uit.scirs.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * The paged-list envelope described in api-standards.md § Response Formats:
 * {@code { content, page, size, totalElements, totalPages }}.
 *
 * <p>Deliberately a hand-rolled shape rather than returning Spring Data's
 * {@link Page} straight out of a controller — that type's default JSON
 * serialization is verbose and its structure is not guaranteed stable across
 * Spring versions, which the frontend contract depends on.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
