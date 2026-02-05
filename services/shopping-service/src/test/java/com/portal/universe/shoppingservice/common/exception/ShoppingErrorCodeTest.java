package com.portal.universe.shoppingservice.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingErrorCodeTest {

    @Test
    @DisplayName("should have unique error codes across all enum values")
    void should_have_unique_codes() {
        ShoppingErrorCode[] values = ShoppingErrorCode.values();
        Set<String> codes = new HashSet<>();

        for (ShoppingErrorCode errorCode : values) {
            boolean added = codes.add(errorCode.getCode());
            assertThat(added)
                    .as("Duplicate error code found: %s in %s", errorCode.getCode(), errorCode.name())
                    .isTrue();
        }

        assertThat(codes).hasSize(values.length);
    }

    @Test
    @DisplayName("should have all error codes starting with S")
    void should_start_with_s() {
        for (ShoppingErrorCode errorCode : ShoppingErrorCode.values()) {
            assertThat(errorCode.getCode())
                    .as("Error code %s should start with S", errorCode.name())
                    .startsWith("S");
        }
    }

    @Test
    @DisplayName("should have valid HTTP status for all error codes")
    void should_have_valid_http_status() {
        for (ShoppingErrorCode errorCode : ShoppingErrorCode.values()) {
            assertThat(errorCode.getStatus())
                    .as("Error code %s should have a valid HTTP status", errorCode.name())
                    .isNotNull()
                    .isInstanceOf(HttpStatus.class);
        }
    }

    @Test
    @DisplayName("should have non-empty message for all error codes")
    void should_have_non_empty_message() {
        for (ShoppingErrorCode errorCode : ShoppingErrorCode.values()) {
            assertThat(errorCode.getMessage())
                    .as("Error code %s should have a non-empty message", errorCode.name())
                    .isNotNull()
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("should cover all domain prefixes (S0XX through S10XX)")
    void should_cover_all_domain_prefixes() {
        Set<String> prefixes = Arrays.stream(ShoppingErrorCode.values())
                .map(e -> {
                    String code = e.getCode();
                    // Extract prefix: S0, S1, S2, ..., S10
                    String numericPart = code.substring(1); // remove "S"
                    if (numericPart.length() == 4) {
                        // S10XX format
                        return "S" + numericPart.substring(0, 2);
                    } else {
                        // S0XX format (first digit is domain)
                        return "S" + numericPart.substring(0, 1);
                    }
                })
                .collect(Collectors.toSet());

        // S0: Product, S1: Cart, S2: Order, S3: Payment, S4: Inventory,
        // S5: Delivery, S6: Coupon, S7: TimeDeal, S8: Queue, S9: Saga, S10: Search
        assertThat(prefixes).contains("S0", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10");
    }
}
