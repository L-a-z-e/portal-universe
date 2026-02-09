/**
 * Shopping API - Barrel Export
 * Domain-separated API modules
 */

export { productApi, adminProductApi, productReviewApi } from './product'
export { inventoryApi, stockMovementApi, inventoryStreamApi } from './inventory'
export { cartApi } from './cart'
export { orderApi, adminOrderApi } from './order'
export { paymentApi, adminPaymentApi } from './payment'
export { deliveryApi } from './delivery'
export { couponApi, adminCouponApi } from './coupon'
export { timeDealApi, adminTimeDealApi } from './timedeal'
export { queueApi, adminQueueApi } from './queue'
export { searchApi } from './search'
export { getApiClient } from './client'
