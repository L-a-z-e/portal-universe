package com.portal.universe.shoppingsettlementservice.settlement.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsettlementservice.settlement.domain.*;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementPeriodResponse;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementResponse;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementRepository;
import com.portal.universe.shoppingsettlementservice.support.fixture.SettlementFixture;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementServiceImpl")
class SettlementServiceImplTest {

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Mock
    private SettlementPeriodRepository periodRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Nested
    @DisplayName("getPeriods")
    class GetPeriods {

        @Test
        @DisplayName("should_returnPeriodList_when_validPeriodType")
        void should_returnPeriodList_when_validPeriodType() {
            SettlementPeriod period = SettlementFixture.createPeriod();
            Pageable pageable = PageRequest.of(0, 10);
            Page<SettlementPeriod> page = new PageImpl<>(List.of(period));

            when(periodRepository.findByPeriodTypeOrderByStartDateDesc(eq(PeriodType.DAILY), any(Pageable.class)))
                    .thenReturn(page);

            List<SettlementPeriodResponse> result = settlementService.getPeriods("DAILY", pageable);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).periodType()).isEqualTo("DAILY");
        }

        @Test
        @DisplayName("should_returnEmptyList_when_noPeriods")
        void should_returnEmptyList_when_noPeriods() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SettlementPeriod> emptyPage = new PageImpl<>(List.of());

            when(periodRepository.findByPeriodTypeOrderByStartDateDesc(eq(PeriodType.DAILY), any(Pageable.class)))
                    .thenReturn(emptyPage);

            List<SettlementPeriodResponse> result = settlementService.getPeriods("DAILY", pageable);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_throwException_when_invalidPeriodType")
        void should_throwException_when_invalidPeriodType() {
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> settlementService.getPeriods("INVALID", pageable))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getPeriod")
    class GetPeriod {

        @Test
        @DisplayName("should_returnPeriod_when_exists")
        void should_returnPeriod_when_exists() {
            SettlementPeriod period = SettlementFixture.createPeriod();
            when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

            SettlementPeriodResponse result = settlementService.getPeriod(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.periodType()).isEqualTo("DAILY");
        }

        @Test
        @DisplayName("should_throwCustomBusinessException_when_notFound")
        void should_throwCustomBusinessException_when_notFound() {
            when(periodRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> settlementService.getPeriod(999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getSellerSettlements")
    class GetSellerSettlements {

        @Test
        @DisplayName("should_returnPagedSettlements_when_sellerHasSettlements")
        void should_returnPagedSettlements_when_sellerHasSettlements() {
            Settlement settlement = SettlementFixture.createSettlement();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Settlement> page = new PageImpl<>(List.of(settlement));

            when(settlementRepository.findBySellerIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                    .thenReturn(page);

            Page<SettlementResponse> result = settlementService.getSellerSettlements(100L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).sellerId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noSettlements")
        void should_returnEmptyPage_when_noSettlements() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Settlement> emptyPage = new PageImpl<>(List.of());

            when(settlementRepository.findBySellerIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<SettlementResponse> result = settlementService.getSellerSettlements(100L, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("confirmPeriod")
    class ConfirmPeriod {

        @Test
        @DisplayName("should_confirmAllSettlements_when_periodHasSettlements")
        void should_confirmAllSettlements_when_periodHasSettlements() {
            Settlement s1 = SettlementFixture.settlementBuilder().id(1L).sellerId(100L).build();
            Settlement s2 = SettlementFixture.settlementBuilder().id(2L).sellerId(200L).build();

            when(settlementRepository.findByPeriodId(1L)).thenReturn(List.of(s1, s2));

            settlementService.confirmPeriod(1L);

            assertThat(s1.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
            assertThat(s2.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should_doNothing_when_noSettlementsForPeriod")
        void should_doNothing_when_noSettlementsForPeriod() {
            when(settlementRepository.findByPeriodId(1L)).thenReturn(List.of());

            settlementService.confirmPeriod(1L);

            verify(settlementRepository).findByPeriodId(1L);
            verifyNoMoreInteractions(settlementRepository);
        }
    }

    @Nested
    @DisplayName("markPeriodPaid")
    class MarkPeriodPaid {

        @Test
        @DisplayName("should_markAllSettlementsPaidAndCompletePeriod_when_valid")
        void should_markAllSettlementsPaidAndCompletePeriod_when_valid() {
            Settlement s1 = SettlementFixture.settlementBuilder().id(1L).build();
            SettlementPeriod period = SettlementFixture.createPeriod();

            when(settlementRepository.findByPeriodId(1L)).thenReturn(List.of(s1));
            when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

            settlementService.markPeriodPaid(1L);

            assertThat(s1.getStatus()).isEqualTo(SettlementStatus.PAID);
            assertThat(s1.getPaidAt()).isNotNull();
            assertThat(period.getStatus()).isEqualTo(PeriodStatus.COMPLETED);
        }

        @Test
        @DisplayName("should_throwException_when_periodNotFound")
        void should_throwException_when_periodNotFound() {
            when(settlementRepository.findByPeriodId(999L)).thenReturn(List.of());
            when(periodRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> settlementService.markPeriodPaid(999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }
}
