package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import org.springframework.test.util.ReflectionTestUtils;

public final class InventoryFixture {

    private InventoryFixture() {}

    public static Inventory create() {
        return builder().build();
    }

    public static InventoryBuilder builder() {
        return new InventoryBuilder();
    }

    public static class InventoryBuilder {
        private Long id = 1L;
        private Long productId = 1L;
        private int availableQuantity = 100;
        private int reservedQuantity = 0;
        private Integer totalQuantity;

        public InventoryBuilder id(Long id) { this.id = id; return this; }
        public InventoryBuilder productId(Long productId) { this.productId = productId; return this; }
        public InventoryBuilder availableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; return this; }
        public InventoryBuilder reservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; return this; }
        public InventoryBuilder totalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; return this; }

        public Inventory build() {
            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .initialQuantity(availableQuantity)
                    .build();
            ReflectionTestUtils.setField(inventory, "id", id);
            ReflectionTestUtils.setField(inventory, "availableQuantity", availableQuantity);
            ReflectionTestUtils.setField(inventory, "reservedQuantity", reservedQuantity);
            ReflectionTestUtils.setField(inventory, "totalQuantity",
                    totalQuantity != null ? totalQuantity : availableQuantity + reservedQuantity);
            return inventory;
        }
    }
}
