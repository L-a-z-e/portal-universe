package com.portal.universe.shoppingservice.order.dto;

import com.portal.universe.shoppingservice.common.domain.Address;

/**
 * 주소 응답 DTO입니다.
 */
public record AddressResponse(
        String receiverName,
        String receiverPhone,
        String zipCode,
        String address1,
        String address2,
        String fullAddress
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getZipCode(),
                address.getAddress1(),
                address.getAddress2(),
                address.getFullAddress()
        );
    }
}
