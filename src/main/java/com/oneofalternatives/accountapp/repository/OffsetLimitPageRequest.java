package com.oneofalternatives.accountapp.repository;

import jakarta.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@EqualsAndHashCode
@ToString
public class OffsetLimitPageRequest implements Pageable {

    private final int offset;
    private final int limit;
    private final Sort sort;

    public OffsetLimitPageRequest(int offset, int limit, @Nonnull Sort sort) {
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    public static OffsetLimitPageRequest of(int offset, int limit, @Nonnull Sort sort) {
        return new OffsetLimitPageRequest(offset, limit, sort);
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Nonnull
    @Override
    public Sort getSort() {
        return sort;
    }

    @Nonnull
    @Override
    public Pageable next() {
        return new OffsetLimitPageRequest(offset + limit, limit, sort);
    }

    @Nonnull
    @Override
    public Pageable previousOrFirst() {
        return new OffsetLimitPageRequest(Math.max(offset - limit, 0), limit, sort);
    }

    @Nonnull
    @Override
    public Pageable first() {
        return new OffsetLimitPageRequest(0, limit, sort);
    }

    @Nonnull
    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetLimitPageRequest(pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
