package vn.ticketscenter.dto.common;

import java.util.Set;

/** Pagination and sort parameters. 1-based, default 20, max 100. */
public record PageRequest(int page, int pageSize, String sort) {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MIN_PAGE = 1;

    public PageRequest {
        if ((long) (page - 1) * pageSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Page offset is too large");
        }
        if (page < MIN_PAGE) throw new IllegalArgumentException("page >= 1, got: " + page);
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("pageSize 1-100, got: " + pageSize);
    }

    public static PageRequest defaults() {
        return new PageRequest(1, DEFAULT_PAGE_SIZE, null);
    }

    public static PageRequest of(int page, int pageSize, String sort, Set<String> allowedSorts) {
        if (sort != null && !sort.isBlank() && !allowedSorts.contains(sort.trim()))
            throw new IllegalArgumentException("Unsupported sort: " + sort);
        return new PageRequest(
                page, pageSize, (sort == null || sort.isBlank()) ? null : sort.trim());
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
