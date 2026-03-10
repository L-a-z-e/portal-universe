package com.portal.universe.shoppingsellerservice.support.fixture;

import com.portal.universe.shoppingsellerservice.inventory.domain.Inventory;
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
        private Long id;
        private Long productId = 1L;
        private Integer availableQuantity = 100;
        private Integer reservedQuantity = 0;
        private Integer totalQuantity = 100;
        private Long version;

        public InventoryBuilder id(Long id) { this.id = id; return this; }
        public InventoryBuilder productId(Long productId) { this.productId = productId; return this; }
        public InventoryBuilder availableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; return this; }
        public InventoryBuilder reservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; return this; }
        public InventoryBuilder totalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; return this; }
        public InventoryBuilder version(Long version) { this.version = version; return this; }

        public Inventory build() {
            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .initialQuantity(availableQuantity)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(inventory, "id", id);
            }
            if (reservedQuantity != 0) {
                ReflectionTestUtils.setField(inventory, "reservedQuantity", reservedQuantity);
            }
            if (!totalQuantity.equals(availableQuantity)) {
                ReflectionTestUtils.setField(inventory, "totalQuantity", totalQuantity);
            }
            if (version != null) {
                ReflectionTestUtils.setField(inventory, "version", version);
            }
            return inventory;
        }
    }
}
