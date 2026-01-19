package com.portal.universe.shoppingservice.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchResult {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;

    // Highlighted fields (검색어 강조)
    private String highlightedName;
    private String highlightedDescription;

    private Double score;

    public void setHighlights(Map<String, String> highlights) {
        if (highlights.containsKey("name")) {
            this.highlightedName = highlights.get("name");
        }
        if (highlights.containsKey("description")) {
            this.highlightedDescription = highlights.get("description");
        }
    }
}
