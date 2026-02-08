package com.portal.universe.shoppingservice.search.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.shoppingservice.search.dto.ProductSearchRequest;
import com.portal.universe.shoppingservice.search.dto.ProductSearchResult;
import com.portal.universe.shoppingservice.search.dto.SearchResponse;
import com.portal.universe.shoppingservice.search.service.ProductSearchService;
import com.portal.universe.shoppingservice.search.service.SuggestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;
    private final SuggestService suggestService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<SearchResponse<ProductSearchResult>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword(keyword)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sort(sort)
                .page(page)
                .size(size)
                .build();

        SearchResponse<ProductSearchResult> response = productSearchService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "5") int size) {
        List<String> suggestions = suggestService.suggest(keyword, size);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<String>>> getPopularKeywords(
            @RequestParam(required = false, defaultValue = "10") int size) {
        List<String> keywords = suggestService.getPopularKeywords(size);
        return ResponseEntity.ok(ApiResponse.success(keywords));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<String>>> getRecentKeywords(
            @CurrentUser AuthUser user,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<String> keywords = suggestService.getRecentKeywords(user.uuid(), size);
        return ResponseEntity.ok(ApiResponse.success(keywords));
    }

    @PostMapping("/recent")
    public ResponseEntity<ApiResponse<Void>> addRecentKeyword(
            @CurrentUser AuthUser user,
            @RequestParam String keyword) {
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.success(null));
        }
        suggestService.addRecentKeyword(user.uuid(), keyword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/recent/{keyword}")
    public ResponseEntity<ApiResponse<Void>> deleteRecentKeyword(
            @CurrentUser AuthUser user,
            @PathVariable String keyword) {
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.success(null));
        }
        suggestService.deleteRecentKeyword(user.uuid(), keyword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/recent")
    public ResponseEntity<ApiResponse<Void>> clearRecentKeywords(
            @CurrentUser AuthUser user) {
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.success(null));
        }
        suggestService.clearRecentKeywords(user.uuid());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
