package com.healthlog.demo.dto.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
		List<T> content,
		int currentPage,
		int totalPages,
		boolean hasNext,
		boolean hasPrevious) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getTotalPages(),
				page.hasNext(), page.hasPrevious());
	}

	public <R> PageResponse<R> map(Function<T, R> mapper) {
		return new PageResponse<>(content.stream().map(mapper).toList(), currentPage, totalPages,
				hasNext, hasPrevious);
	}
}
