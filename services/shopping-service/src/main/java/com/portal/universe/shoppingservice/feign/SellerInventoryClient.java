package com.portal.universe.shoppingservice.feign;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.feign.dto.StockReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "shopping-seller-inventory", url = "${feign.shopping-seller-service.url}", path = "/internal/inventory")
public interface SellerInventoryClient {

    @PostMapping("/reserve")
    ApiResponse<Void> reserveStock(@RequestBody StockReserveRequest request);

    @PostMapping("/deduct")
    ApiResponse<Void> deductStock(@RequestBody StockReserveRequest request);

    @PostMapping("/release")
    ApiResponse<Void> releaseStock(@RequestBody StockReserveRequest request);
}
