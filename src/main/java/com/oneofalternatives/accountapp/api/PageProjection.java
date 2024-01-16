package com.oneofalternatives.accountapp.api;

import java.util.List;

public record PageProjection<T>(
    List<T> content,
    long offset,
    int pageSize,
    int pageNumber,
    int totalPages,
    int numberOfElements,
    long totalElements,
    boolean isFirst,
    boolean isLast
) {

    public List<T> content() {
        return content;
    }
}
