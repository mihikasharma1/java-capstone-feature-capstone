package assembly.general.api.dto;

import lombok.Getter;

import java.util.List;

// Generic wrapper — reused later in Milestone 4 for /reservations/history,
// which has the identical page/size/totalElements/totalPages/last shape.
@Getter
public class PagedResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public PagedResponse(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }
}