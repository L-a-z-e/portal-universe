package com.portal.universe.shoppingsellerservice.seller.domain;

import com.portal.universe.shoppingsellerservice.support.fixture.SellerFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Seller")
class SellerTest {

    @Nested
    @DisplayName("status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("should start with PENDING status")
        void should_start_as_pending() {
            Seller seller = SellerFixture.create();

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.PENDING);
            assertThat(seller.isPending()).isTrue();
            assertThat(seller.isActive()).isFalse();
        }

        @Test
        @DisplayName("should transition to ACTIVE on approve")
        void should_become_active_on_approve() {
            Seller seller = SellerFixture.create();

            seller.approve("admin-001", "Good application");

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.ACTIVE);
            assertThat(seller.isActive()).isTrue();
            assertThat(seller.isPending()).isFalse();
            assertThat(seller.getReviewedBy()).isEqualTo("admin-001");
            assertThat(seller.getReviewComment()).isEqualTo("Good application");
            assertThat(seller.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition to REJECTED on reject")
        void should_become_rejected_on_reject() {
            Seller seller = SellerFixture.create();

            seller.reject("admin-001", "Incomplete docs");

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.REJECTED);
            assertThat(seller.getReviewedBy()).isEqualTo("admin-001");
            assertThat(seller.getReviewComment()).isEqualTo("Incomplete docs");
            assertThat(seller.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition to SUSPENDED on suspend")
        void should_become_suspended() {
            Seller seller = SellerFixture.createApproved();

            seller.suspend();

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.SUSPENDED);
            assertThat(seller.isActive()).isFalse();
        }

        @Test
        @DisplayName("should transition to WITHDRAWN on withdraw")
        void should_become_withdrawn() {
            Seller seller = SellerFixture.create();

            seller.withdraw();

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.WITHDRAWN);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update business info fields")
        void should_update_fields() {
            Seller seller = SellerFixture.create();

            seller.update("New Name", "010-9999", "new@test.com", "New Bank", "999-888");

            assertThat(seller.getBusinessName()).isEqualTo("New Name");
            assertThat(seller.getPhone()).isEqualTo("010-9999");
            assertThat(seller.getEmail()).isEqualTo("new@test.com");
            assertThat(seller.getBankName()).isEqualTo("New Bank");
            assertThat(seller.getBankAccount()).isEqualTo("999-888");
        }
    }

    @Nested
    @DisplayName("builder defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("should default commission rate to 10.00")
        void should_default_commission_rate() {
            Seller seller = Seller.builder()
                    .userId("test-user")
                    .businessName("Test Store")
                    .build();

            assertThat(seller.getCommissionRate()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(seller.getStatus()).isEqualTo(SellerStatus.PENDING);
        }
    }
}
