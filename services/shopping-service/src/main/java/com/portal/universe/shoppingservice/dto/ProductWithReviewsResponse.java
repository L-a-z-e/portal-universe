package com.portal.universe.shoppingservice.dto;

import java.util.List;

public record ProductWithReviewsResponse(
        Long id,
        String name,
        String description,
        Double price,
        Integer stock,
        List<BlogResponse> reviews
) {}
