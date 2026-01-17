package com.portal.universe.shoppingservice.order.dto;

import com.portal.universe.shoppingservice.common.domain.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 주소 요청 DTO입니다.
 */
public record AddressRequest(
        @NotBlank(message = "Receiver name is required")
        @Size(max = 100, message = "Receiver name must be at most 100 characters")
        String receiverName,

        @NotBlank(message = "Receiver phone is required")
        @Size(max = 20, message = "Receiver phone must be at most 20 characters")
        String receiverPhone,

        @NotBlank(message = "Zip code is required")
        @Size(max = 10, message = "Zip code must be at most 10 characters")
        String zipCode,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address1,

        @Size(max = 255, message = "Detail address must be at most 255 characters")
        String address2
) {
    public Address toEntity() {
        return Address.builder()
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .zipCode(zipCode)
                .address1(address1)
                .address2(address2)
                .build();
    }
}
