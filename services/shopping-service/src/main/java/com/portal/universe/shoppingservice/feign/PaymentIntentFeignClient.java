package com.portal.universe.shoppingservice.feign;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.feign.dto.CreatePaymentIntentRequest;
import com.portal.universe.shoppingservice.feign.dto.PaymentIntentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-intent", url = "${feign.payment-service.url}", path = "/internal")
public interface PaymentIntentFeignClient {

    @PostMapping("/intents")
    ApiResponse<PaymentIntentResponse> createIntent(@RequestBody CreatePaymentIntentRequest request);

    @PostMapping("/refund/{orderNumber}")
    ApiResponse<Void> refundForCompensation(@org.springframework.web.bind.annotation.PathVariable String orderNumber);
}
