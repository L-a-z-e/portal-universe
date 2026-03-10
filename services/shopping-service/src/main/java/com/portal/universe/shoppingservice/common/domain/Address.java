package com.portal.universe.shoppingservice.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송 주소 정보를 나타내는 임베더블 클래스입니다.
 * Order, Delivery 등 여러 엔티티에서 재사용됩니다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "address1", length = 255)
    private String address1;

    @Column(name = "address2", length = 255)
    private String address2;

    @Builder
    public Address(String receiverName, String receiverPhone, String zipCode, String address1, String address2) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.address1 = address1;
        this.address2 = address2;
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (zipCode != null) {
            sb.append("[").append(zipCode).append("] ");
        }
        if (address1 != null) {
            sb.append(address1);
        }
        if (address2 != null) {
            sb.append(" ").append(address2);
        }
        return sb.toString().trim();
    }
}
