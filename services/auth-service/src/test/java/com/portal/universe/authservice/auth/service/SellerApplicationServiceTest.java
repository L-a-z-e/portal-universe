package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationRequest;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationResponse;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationReviewRequest;
import com.portal.universe.authservice.auth.repository.AuthAuditLogRepository;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.SellerApplicationRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerApplicationService 테스트")
class SellerApplicationServiceTest {

    @Mock
    private SellerApplicationRepository sellerApplicationRepository;

    @Mock
    private RoleEntityRepository roleEntityRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuthAuditLogRepository auditLogRepository;

    @InjectMocks
    private SellerApplicationService sellerApplicationService;

    private static final String USER_ID = "test-uuid";
    private static final String REVIEWER_ID = "admin-uuid";

    private SellerApplication createPendingApplication() {
        return SellerApplication.builder()
                .userId(USER_ID)
                .businessName("Test Business")
                .businessNumber("123-45-67890")
                .reason("Want to sell")
                .build();
    }

    @Nested
    @DisplayName("apply")
    class Apply {

        @Test
        @DisplayName("should_submitApplication_when_noPendingExists")
        void should_submitApplication_when_noPendingExists() {
            // given
            SellerApplicationRequest request = new SellerApplicationRequest(
                    "Test Business", "123-45-67890", "Want to sell"
            );
            when(sellerApplicationRepository.existsByUserIdAndStatus(USER_ID, SellerApplicationStatus.PENDING))
                    .thenReturn(false);
            when(userRoleRepository.findActiveRoleKeysByUserId(USER_ID))
                    .thenReturn(List.of("ROLE_USER"));
            when(sellerApplicationRepository.save(any(SellerApplication.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SellerApplicationResponse result = sellerApplicationService.apply(USER_ID, request);

            // then
            assertThat(result.businessName()).isEqualTo("Test Business");
            assertThat(result.status()).isEqualTo(SellerApplicationStatus.PENDING);
            verify(auditLogRepository).save(any(AuthAuditLog.class));
        }

        @Test
        @DisplayName("should_throwException_when_pendingApplicationExists")
        void should_throwException_when_pendingApplicationExists() {
            // given
            SellerApplicationRequest request = new SellerApplicationRequest(
                    "Test Business", "123-45-67890", "Want to sell"
            );
            when(sellerApplicationRepository.existsByUserIdAndStatus(USER_ID, SellerApplicationStatus.PENDING))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> sellerApplicationService.apply(USER_ID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.SELLER_APPLICATION_ALREADY_PENDING);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_alreadySeller")
        void should_throwException_when_alreadySeller() {
            // given
            SellerApplicationRequest request = new SellerApplicationRequest(
                    "Test Business", "123-45-67890", "Want to sell"
            );
            when(sellerApplicationRepository.existsByUserIdAndStatus(USER_ID, SellerApplicationStatus.PENDING))
                    .thenReturn(false);
            when(userRoleRepository.findActiveRoleKeysByUserId(USER_ID))
                    .thenReturn(List.of("ROLE_USER", "ROLE_SELLER"));

            // when & then
            assertThatThrownBy(() -> sellerApplicationService.apply(USER_ID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.ROLE_ALREADY_ASSIGNED);
                    });
        }
    }

    @Nested
    @DisplayName("getMyApplication")
    class GetMyApplication {

        @Test
        @DisplayName("should_returnApplication_when_pendingExists")
        void should_returnApplication_when_pendingExists() {
            // given
            SellerApplication application = createPendingApplication();
            when(sellerApplicationRepository.findByUserIdAndStatus(USER_ID, SellerApplicationStatus.PENDING))
                    .thenReturn(Optional.of(application));

            // when
            SellerApplicationResponse result = sellerApplicationService.getMyApplication(USER_ID);

            // then
            assertThat(result.businessName()).isEqualTo("Test Business");
        }

        @Test
        @DisplayName("should_throwException_when_noApplicationFound")
        void should_throwException_when_noApplicationFound() {
            // given
            when(sellerApplicationRepository.findByUserIdAndStatus(USER_ID, SellerApplicationStatus.PENDING))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerApplicationService.getMyApplication(USER_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.SELLER_APPLICATION_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("getPendingApplications")
    class GetPendingApplications {

        @Test
        @DisplayName("should_returnPage_when_pendingApplicationsExist")
        void should_returnPage_when_pendingApplicationsExist() {
            // given
            SellerApplication app = createPendingApplication();
            Page<SellerApplication> page = new PageImpl<>(List.of(app));
            Pageable pageable = PageRequest.of(0, 10);
            when(sellerApplicationRepository.findByStatus(SellerApplicationStatus.PENDING, pageable))
                    .thenReturn(page);

            // when
            Page<SellerApplicationResponse> result = sellerApplicationService.getPendingApplications(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("review")
    class Review {

        @Test
        @DisplayName("should_approveAndAssignRole_when_approved")
        void should_approveAndAssignRole_when_approved() {
            // given
            SellerApplication application = createPendingApplication();
            try {
                var idField = SellerApplication.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(application, 1L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            RoleEntity sellerRole = RoleEntity.builder()
                    .roleKey("ROLE_SELLER")
                    .displayName("Seller")
                    .system(false)
                    .build();

            when(sellerApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(roleEntityRepository.findByRoleKey("ROLE_SELLER")).thenReturn(Optional.of(sellerRole));
            when(userRoleRepository.findByUserIdWithRole(USER_ID)).thenReturn(List.of());
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SellerApplicationReviewRequest request = new SellerApplicationReviewRequest(true, "Approved");

            // when
            SellerApplicationResponse result = sellerApplicationService.review(1L, request, REVIEWER_ID);

            // then
            assertThat(result.status()).isEqualTo(SellerApplicationStatus.APPROVED);
            verify(userRoleRepository).save(any(UserRole.class));
        }

        @Test
        @DisplayName("should_reject_when_notApproved")
        void should_reject_when_notApproved() {
            // given
            SellerApplication application = createPendingApplication();
            try {
                var idField = SellerApplication.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(application, 1L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(sellerApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

            SellerApplicationReviewRequest request = new SellerApplicationReviewRequest(false, "Not qualified");

            // when
            SellerApplicationResponse result = sellerApplicationService.review(1L, request, REVIEWER_ID);

            // then
            assertThat(result.status()).isEqualTo(SellerApplicationStatus.REJECTED);
            verify(userRoleRepository, never()).save(any(UserRole.class));
        }

        @Test
        @DisplayName("should_throwException_when_applicationNotFound")
        void should_throwException_when_applicationNotFound() {
            // given
            when(sellerApplicationRepository.findById(999L)).thenReturn(Optional.empty());
            SellerApplicationReviewRequest request = new SellerApplicationReviewRequest(true, "ok");

            // when & then
            assertThatThrownBy(() -> sellerApplicationService.review(999L, request, REVIEWER_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.SELLER_APPLICATION_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_applicationAlreadyProcessed")
        void should_throwException_when_applicationAlreadyProcessed() {
            // given
            SellerApplication application = createPendingApplication();
            application.approve(REVIEWER_ID, "already approved"); // no longer pending
            try {
                var idField = SellerApplication.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(application, 1L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(sellerApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
            SellerApplicationReviewRequest request = new SellerApplicationReviewRequest(true, "ok");

            // when & then
            assertThatThrownBy(() -> sellerApplicationService.review(1L, request, REVIEWER_ID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.SELLER_APPLICATION_ALREADY_PROCESSED);
                    });
        }
    }
}
