package com.portal.universe.shoppingservice.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse<T> {
    private List<T> results;
    private long totalHits;
    private int page;
    private int size;
    private int totalPages;

    public static <T> SearchResponse<T> of(List<T> results, long totalHits, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalHits / size);
        return SearchResponse.<T>builder()
                .results(results)
                .totalHits(totalHits)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }
}
