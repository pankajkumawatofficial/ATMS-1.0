package com.learning.atm.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * One page of results plus the numbers a client needs to render pagination controls.
 *
 * <p>Learning note: Spring's {@code Page} serializes fine but its JSON shape is an
 * implementation detail that has changed between versions. Wrapping it in a record of our own
 * keeps the API contract explicit and stable, and lets the UI rely on {@code totalPages} and
 * {@code last} instead of computing them.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

	/** Maps every entity on the page to its DTO in one pass. */
	public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}
}
