package vn.ticketscenter.dto.common;

import java.util.List;

/**
 * Generic paginated list: {"items":[...],"page":1,"pageSize":20,"total":0}
 * Owner: Khánh (KHANH-02)
 */
public record Page<T>(List<T> items, int page, int pageSize, long total) {
    public static <T> Page<T> empty(int page, int pageSize) {
        return new Page<>(List.of(), page, pageSize, 0);
    }
    public static <T> Page<T> of(List<T> items, int page, int pageSize, long total) {
        return new Page<>(items, page, pageSize, total);
    }
    public int offset() { return (page - 1) * pageSize; }
}
