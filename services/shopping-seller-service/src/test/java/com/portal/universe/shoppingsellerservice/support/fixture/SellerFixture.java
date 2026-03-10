package com.portal.universe.shoppingsellerservice.support.fixture;

import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import com.portal.universe.shoppingsellerservice.seller.domain.SellerStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

public final class SellerFixture {

    public static final String DEFAULT_USER_ID = "seller-uuid-001";
    public static final String DEFAULT_BUSINESS_NAME = "Test Store";
    public static final String DEFAULT_BUSINESS_NUMBER = "123-45-67890";

    private SellerFixture() {}

    public static Seller create() {
        return builder().build();
    }

    public static Seller createApproved() {
        Seller seller = builder().build();
        seller.approve("admin-uuid", "Approved");
        return seller;
    }

    public static SellerBuilder builder() {
        return new SellerBuilder();
    }

    public static class SellerBuilder {
        private Long id;
        private String userId = DEFAULT_USER_ID;
        private String businessName = DEFAULT_BUSINESS_NAME;
        private String businessNumber = DEFAULT_BUSINESS_NUMBER;
        private String representativeName = "Test Rep";
        private String phone = "010-1234-5678";
        private String email = "seller@test.com";
        private String bankName = "Test Bank";
        private String bankAccount = "111-222-333333";
        private BigDecimal commissionRate = new BigDecimal("10.00");
        private SellerStatus status;

        public SellerBuilder id(Long id) { this.id = id; return this; }
        public SellerBuilder userId(String userId) { this.userId = userId; return this; }
        public SellerBuilder businessName(String businessName) { this.businessName = businessName; return this; }
        public SellerBuilder businessNumber(String businessNumber) { this.businessNumber = businessNumber; return this; }
        public SellerBuilder representativeName(String representativeName) { this.representativeName = representativeName; return this; }
        public SellerBuilder phone(String phone) { this.phone = phone; return this; }
        public SellerBuilder email(String email) { this.email = email; return this; }
        public SellerBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public SellerBuilder bankAccount(String bankAccount) { this.bankAccount = bankAccount; return this; }
        public SellerBuilder commissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; return this; }
        public SellerBuilder status(SellerStatus status) { this.status = status; return this; }

        public Seller build() {
            Seller seller = Seller.builder()
                    .userId(userId)
                    .businessName(businessName)
                    .businessNumber(businessNumber)
                    .representativeName(representativeName)
                    .phone(phone)
                    .email(email)
                    .bankName(bankName)
                    .bankAccount(bankAccount)
                    .commissionRate(commissionRate)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(seller, "id", id);
            }
            if (status != null && status != SellerStatus.PENDING) {
                ReflectionTestUtils.setField(seller, "status", status);
            }
            return seller;
        }
    }
}
