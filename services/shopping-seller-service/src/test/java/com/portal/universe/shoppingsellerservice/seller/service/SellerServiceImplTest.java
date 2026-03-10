package com.portal.universe.shoppingsellerservice.seller.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.SellerApprovedEvent;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import com.portal.universe.shoppingsellerservice.seller.domain.SellerStatus;
import com.portal.universe.shoppingsellerservice.seller.dto.*;
import com.portal.universe.shoppingsellerservice.seller.repository.SellerRepository;
import com.portal.universe.shoppingsellerservice.support.fixture.SellerFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerServiceImpl")
class SellerServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SellerServiceImpl sellerService;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register new seller when userId is unique")
        void should_register_new_seller() {
            // given
            String userId = "new-user-001";
            SellerRegisterRequest request = new SellerRegisterRequest(
                    "New Store", "123-45-67890", "Rep Name",
                    "010-1111-2222", "new@test.com", "Bank", "111-222"
            );
            when(sellerRepository.existsByUserId(userId)).thenReturn(false);
            when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SellerResponse response = sellerService.register(userId, request);

            // then
            assertThat(response.businessName()).isEqualTo("New Store");
            assertThat(response.status()).isEqualTo(SellerStatus.PENDING.name());
            verify(sellerRepository).save(any(Seller.class));
        }

        @Test
        @DisplayName("should throw SELLER_ALREADY_EXISTS when userId is duplicate")
        void should_throw_when_already_exists() {
            // given
            String userId = "existing-user";
            SellerRegisterRequest request = new SellerRegisterRequest(
                    "Store", "123-45-67890", "Rep", "010", "e@t.com", "Bank", "111"
            );
            when(sellerRepository.existsByUserId(userId)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> sellerService.register(userId, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.SELLER_ALREADY_EXISTS));
        }
    }

    @Nested
    @DisplayName("apply")
    class Apply {

        @Test
        @DisplayName("should submit seller application when userId is unique")
        void should_submit_application() {
            // given
            String userId = "applicant-001";
            SellerApplyRequest request = new SellerApplyRequest(
                    "Apply Store", "123-45-67890", "Rep", "010", "a@t.com", "Bank", "111", "I want to sell"
            );
            when(sellerRepository.existsByUserId(userId)).thenReturn(false);
            when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SellerResponse response = sellerService.apply(userId, request);

            // then
            assertThat(response.businessName()).isEqualTo("Apply Store");
            assertThat(response.status()).isEqualTo(SellerStatus.PENDING.name());
            verify(sellerRepository).save(any(Seller.class));
        }

        @Test
        @DisplayName("should throw SELLER_ALREADY_EXISTS when userId is duplicate")
        void should_throw_when_already_exists() {
            // given
            String userId = "existing-user";
            SellerApplyRequest request = new SellerApplyRequest(
                    "Store", "123", "Rep", "010", "e@t.com", "Bank", "111", "reason"
            );
            when(sellerRepository.existsByUserId(userId)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> sellerService.apply(userId, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.SELLER_ALREADY_EXISTS));
        }
    }

    @Nested
    @DisplayName("getMyApplication")
    class GetMyApplication {

        @Test
        @DisplayName("should return seller application when found")
        void should_return_application() {
            // given
            String userId = SellerFixture.DEFAULT_USER_ID;
            Seller seller = SellerFixture.builder().id(1L).userId(userId).build();
            when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

            // when
            SellerResponse response = sellerService.getMyApplication(userId);

            // then
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.businessName()).isEqualTo(SellerFixture.DEFAULT_BUSINESS_NAME);
        }

        @Test
        @DisplayName("should throw SELLER_APPLICATION_NOT_FOUND when not found")
        void should_throw_when_not_found() {
            // given
            String userId = "nonexistent";
            when(sellerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerService.getMyApplication(userId))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.SELLER_APPLICATION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfo {

        @Test
        @DisplayName("should return seller info when found")
        void should_return_info() {
            // given
            String userId = SellerFixture.DEFAULT_USER_ID;
            Seller seller = SellerFixture.createApproved();
            when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

            // when
            SellerResponse response = sellerService.getMyInfo(userId);

            // then
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.status()).isEqualTo(SellerStatus.ACTIVE.name());
        }

        @Test
        @DisplayName("should throw SELLER_NOT_FOUND when not found")
        void should_throw_when_not_found() {
            // given
            when(sellerRepository.findByUserId("unknown")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerService.getMyInfo("unknown"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.SELLER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update seller info")
        void should_update_seller() {
            // given
            String userId = SellerFixture.DEFAULT_USER_ID;
            Seller seller = SellerFixture.builder().id(1L).userId(userId).build();
            SellerUpdateRequest request = new SellerUpdateRequest(
                    "Updated Store", "010-9999-8888", "updated@test.com", "New Bank", "999-888"
            );
            when(sellerRepository.findByUserId(userId)).thenReturn(Optional.of(seller));

            // when
            SellerResponse response = sellerService.update(userId, request);

            // then
            assertThat(response.businessName()).isEqualTo("Updated Store");
            assertThat(response.phone()).isEqualTo("010-9999-8888");
            assertThat(response.email()).isEqualTo("updated@test.com");
        }

        @Test
        @DisplayName("should throw SELLER_NOT_FOUND when not found")
        void should_throw_when_not_found() {
            // given
            when(sellerRepository.findByUserId("unknown")).thenReturn(Optional.empty());
            SellerUpdateRequest request = new SellerUpdateRequest("S", "0", "e@t.com", "B", "1");

            // when & then
            assertThatThrownBy(() -> sellerService.update("unknown", request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.SELLER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getSellersByStatus")
    class GetSellersByStatus {

        @Test
        @DisplayName("should return sellers filtered by status")
        void should_return_sellers_by_status() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Seller seller = SellerFixture.builder().id(1L).build();
            when(sellerRepository.findByStatus(SellerStatus.PENDING, pageable))
                    .thenReturn(new PageImpl<>(List.of(seller)));

            // when
            Page<SellerResponse> result = sellerService.getSellersByStatus("PENDING", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(SellerStatus.PENDING.name());
        }
    }

    @Nested
    @DisplayName("getAllSellers")
    class GetAllSellers {

        @Test
        @DisplayName("should return all sellers with pagination")
        void should_return_all_sellers() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Seller s1 = SellerFixture.builder().id(1L).userId("u1").build();
            Seller s2 = SellerFixture.builder().id(2L).userId("u2").build();
            when(sellerRepository.findAllByOrderByCreatedAtDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(s1, s2)));

            // when
            Page<SellerResponse> result = sellerService.getAllSellers(pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("reviewSeller")
    class ReviewSeller {

        @Test
        @DisplayName("should approve pending seller and publish event")
        void should_approve_seller() {
            // given
            Seller seller = SellerFixture.builder().id(1L).build(); // PENDING by default
            SellerReviewRequest request = new SellerReviewRequest(true, "Good application");
            when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));

            // when
            SellerResponse response = sellerService.reviewSeller(1L, request, "admin-001");

            // then
            assertThat(response.status()).isEqualTo(SellerStatus.ACTIVE.name());
            assertThat(response.reviewComment()).isEqualTo("Good application");
            verify(eventPublisher).publishEvent(any(SellerApprovedEvent.class));
        }

        @Test
        @DisplayName("should reject pending seller without publishing event")
        void should_reject_seller() {
            // given
            Seller seller = SellerFixture.builder().id(1L).build();
            SellerReviewRequest request = new SellerReviewRequest(false, "Incomplete docs");
            when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));

            // when
            SellerResponse response = sellerService.reviewSeller(1L, request, "admin-001");

            // then
            assertThat(response.status()).isEqualTo(SellerStatus.REJECTED.name());
            assertThat(response.reviewComment()).isEqualTo("Incomplete docs");
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should throw SELLER_NOT_FOUND when seller does not exist")
        void should_throw_when_not_found() {
            // given
            when(sellerRepository.findById(999L)).thenReturn(Optional.empty());
            SellerReviewRequest request = new SellerReviewRequest(true, "comment");

            // when & then
            assertThatThrownBy(() -> sellerService.reviewSeller(999L, request, "admin"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.SELLER_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw SELLER_APPLICATION_NOT_PENDING when seller is not pending")
        void should_throw_when_not_pending() {
            // given
            Seller seller = SellerFixture.builder().id(1L).status(SellerStatus.ACTIVE).build();
            SellerReviewRequest request = new SellerReviewRequest(true, "comment");
            when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));

            // when & then
            assertThatThrownBy(() -> sellerService.reviewSeller(1L, request, "admin"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.SELLER_APPLICATION_NOT_PENDING));
        }
    }
}
