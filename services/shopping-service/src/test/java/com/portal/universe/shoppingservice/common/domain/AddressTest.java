package com.portal.universe.shoppingservice.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create address with all fields")
        void should_create_with_all_fields() {
            Address address = Address.builder()
                    .receiverName("홍길동")
                    .receiverPhone("010-1234-5678")
                    .zipCode("12345")
                    .address1("서울시 강남구 테헤란로")
                    .address2("101호")
                    .build();

            assertThat(address.getReceiverName()).isEqualTo("홍길동");
            assertThat(address.getReceiverPhone()).isEqualTo("010-1234-5678");
            assertThat(address.getZipCode()).isEqualTo("12345");
            assertThat(address.getAddress1()).isEqualTo("서울시 강남구 테헤란로");
            assertThat(address.getAddress2()).isEqualTo("101호");
        }
    }

    @Nested
    @DisplayName("getFullAddress")
    class GetFullAddressTest {

        @Test
        @DisplayName("should return full address with zip code and both addresses")
        void should_return_full_address() {
            Address address = Address.builder()
                    .receiverName("홍길동")
                    .receiverPhone("010-1234-5678")
                    .zipCode("12345")
                    .address1("서울시 강남구 테헤란로")
                    .address2("101호")
                    .build();

            String fullAddress = address.getFullAddress();

            assertThat(fullAddress).isEqualTo("[12345] 서울시 강남구 테헤란로 101호");
        }

        @Test
        @DisplayName("should handle null address2 gracefully")
        void should_handle_null_address2() {
            Address address = Address.builder()
                    .receiverName("홍길동")
                    .receiverPhone("010-1234-5678")
                    .zipCode("12345")
                    .address1("서울시 강남구 테헤란로")
                    .address2(null)
                    .build();

            String fullAddress = address.getFullAddress();

            assertThat(fullAddress).isEqualTo("[12345] 서울시 강남구 테헤란로");
        }
    }
}
