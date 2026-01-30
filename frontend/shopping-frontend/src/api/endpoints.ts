/**
 * Shopping API Endpoints
 *
 * Backend API와 통신하는 함수들
 */
import { getApiClient } from './client'
import type {
  ApiResponse,
  PagedResponse,
  Product,
  ProductCreateRequest,
  ProductUpdateRequest,
  Inventory,
  InventoryUpdateRequest,
  Cart,
  AddCartItemRequest,
  UpdateCartItemRequest,
  Order,
  CreateOrderRequest,
  CancelOrderRequest,
  Payment,
  ProcessPaymentRequest,
  Delivery,
  UpdateDeliveryStatusRequest,
  StockMovement,
  Coupon,
  UserCoupon,
  CouponCreateRequest,
  TimeDeal,
  TimeDealCreateRequest,
  TimeDealPurchase
} from '@/types'

const API_PREFIX = '/api/v1/shopping'

// ============================================
// Product API
// ============================================

export const productApi = {
  /**
   * 상품 목록 조회
   */
  getProducts: async (page = 0, size = 12, category?: string) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    if (category) {
      params.append('category', category)
    }
    const response = await getApiClient().get<ApiResponse<PagedResponse<Product>>>(
      `${API_PREFIX}/products?${params}`
    )
    return response.data
  },

  /**
   * 상품 상세 조회
   */
  getProduct: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<Product>>(
      `${API_PREFIX}/products/${id}`
    )
    return response.data
  },

  /**
   * 상품 검색
   */
  searchProducts: async (keyword: string, page = 0, size = 12) => {
    const params = new URLSearchParams({
      keyword,
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PagedResponse<Product>>>(
      `${API_PREFIX}/search/products?${params}`
    )
    return response.data
  },

  /**
   * 상품 생성 (관리자)
   */
  createProduct: async (data: ProductCreateRequest) => {
    const response = await getApiClient().post<ApiResponse<Product>>(
      `${API_PREFIX}/products`,
      data
    )
    return response.data
  },

  /**
   * 상품 수정 (관리자)
   */
  updateProduct: async (id: number, data: ProductUpdateRequest) => {
    const response = await getApiClient().put<ApiResponse<Product>>(
      `${API_PREFIX}/products/${id}`,
      data
    )
    return response.data
  },

  /**
   * 상품 삭제 (관리자)
   */
  deleteProduct: async (id: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/products/${id}`
    )
    return response.data
  }
}

// ============================================
// Inventory API
// ============================================

export const inventoryApi = {
  /**
   * 재고 조회
   */
  getInventory: async (productId: number) => {
    const response = await getApiClient().get<ApiResponse<Inventory>>(
      `${API_PREFIX}/inventory/${productId}`
    )
    return response.data
  },

  /**
   * 여러 상품 재고 조회
   */
  getInventories: async (productIds: number[]) => {
    const response = await getApiClient().post<ApiResponse<Inventory[]>>(
      `${API_PREFIX}/inventory/batch`,
      { productIds }
    )
    return response.data
  },

  /**
   * 재고 초기화 (관리자 - 신규 상품)
   */
  initializeInventory: async (productId: number, data: InventoryUpdateRequest) => {
    const response = await getApiClient().post<ApiResponse<Inventory>>(
      `${API_PREFIX}/inventory/${productId}`,
      data
    )
    return response.data
  },

  /**
   * 재고 추가 (관리자)
   */
  addStock: async (productId: number, data: InventoryUpdateRequest) => {
    const response = await getApiClient().put<ApiResponse<Inventory>>(
      `${API_PREFIX}/inventory/${productId}/add`,
      data
    )
    return response.data
  }
}

// ============================================
// Cart API
// ============================================

export const cartApi = {
  /**
   * 장바구니 조회
   */
  getCart: async () => {
    const response = await getApiClient().get<ApiResponse<Cart>>(
      `${API_PREFIX}/cart`
    )
    return response.data
  },

  /**
   * 장바구니에 상품 추가
   */
  addItem: async (data: AddCartItemRequest) => {
    const response = await getApiClient().post<ApiResponse<Cart>>(
      `${API_PREFIX}/cart/items`,
      data
    )
    return response.data
  },

  /**
   * 장바구니 항목 수량 변경
   */
  updateItem: async (itemId: number, data: UpdateCartItemRequest) => {
    const response = await getApiClient().put<ApiResponse<Cart>>(
      `${API_PREFIX}/cart/items/${itemId}`,
      data
    )
    return response.data
  },

  /**
   * 장바구니 항목 삭제
   */
  removeItem: async (itemId: number) => {
    const response = await getApiClient().delete<ApiResponse<Cart>>(
      `${API_PREFIX}/cart/items/${itemId}`
    )
    return response.data
  },

  /**
   * 장바구니 비우기
   */
  clearCart: async () => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/cart`
    )
    return response.data
  }
}

// ============================================
// Order API
// ============================================

export const orderApi = {
  /**
   * 주문 목록 조회
   */
  getOrders: async (page = 0, size = 10) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PagedResponse<Order>>>(
      `${API_PREFIX}/orders?${params}`
    )
    return response.data
  },

  /**
   * 주문 상세 조회
   */
  getOrder: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Order>>(
      `${API_PREFIX}/orders/${orderNumber}`
    )
    return response.data
  },

  /**
   * 주문 생성 (장바구니에서 주문으로)
   */
  createOrder: async (data: CreateOrderRequest) => {
    const response = await getApiClient().post<ApiResponse<Order>>(
      `${API_PREFIX}/orders`,
      data
    )
    return response.data
  },

  /**
   * 주문 취소
   */
  cancelOrder: async (orderNumber: string, data: CancelOrderRequest) => {
    const response = await getApiClient().post<ApiResponse<Order>>(
      `${API_PREFIX}/orders/${orderNumber}/cancel`,
      data
    )
    return response.data
  }
}

// ============================================
// Payment API
// ============================================

export const paymentApi = {
  /**
   * 결제 정보 조회
   */
  getPayment: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Payment>>(
      `${API_PREFIX}/payments/${orderNumber}`
    )
    return response.data
  },

  /**
   * 결제 처리
   */
  processPayment: async (data: ProcessPaymentRequest) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${API_PREFIX}/payments`,
      data
    )
    return response.data
  },

  /**
   * 결제 취소/환불
   */
  cancelPayment: async (orderNumber: string) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${API_PREFIX}/payments/${orderNumber}/cancel`
    )
    return response.data
  }
}

// ============================================
// Delivery API
// ============================================

export const deliveryApi = {
  /**
   * 배송 정보 조회 (주문번호)
   */
  getDeliveryByOrder: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Delivery>>(
      `${API_PREFIX}/deliveries/order/${orderNumber}`
    )
    return response.data
  },

  /**
   * 배송 추적 (운송장번호)
   */
  trackDelivery: async (trackingNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Delivery>>(
      `${API_PREFIX}/deliveries/${trackingNumber}`
    )
    return response.data
  },

  /**
   * 배송 상태 업데이트 (관리자/배송사)
   */
  updateDeliveryStatus: async (trackingNumber: string, data: UpdateDeliveryStatusRequest) => {
    const response = await getApiClient().put<ApiResponse<Delivery>>(
      `${API_PREFIX}/deliveries/${trackingNumber}/status`,
      data
    )
    return response.data
  }
}

// ============================================
// Stock Movement API (Admin)
// ============================================

export const stockMovementApi = {
  /**
   * 재고 이동 이력 조회
   */
  getMovements: async (productId: number, page = 0, size = 20) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PagedResponse<StockMovement>>>(
      `${API_PREFIX}/inventory/${productId}/movements?${params}`
    )
    return response.data
  }
}

// ============================================
// Admin Product API
// ============================================

export const adminProductApi = {
  /**
   * Admin 상품 목록 조회 (페이징, 정렬, 필터링)
   * 공개 API를 재사용 (목록 조회는 권한 불필요)
   */
  getProducts: async (params: {
    page?: number
    size?: number
    keyword?: string
    category?: string
    status?: string
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  }) => {
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        searchParams.append(key, String(value))
      }
    })

    const response = await getApiClient().get<ApiResponse<PagedResponse<Product>>>(
      `${API_PREFIX}/products?${searchParams}`
    )
    return response.data
  },

  /**
   * 상품 생성 (ADMIN)
   */
  createProduct: async (data: ProductCreateRequest) => {
    const response = await getApiClient().post<ApiResponse<Product>>(
      `${API_PREFIX}/admin/products`,
      data
    )
    return response.data
  },

  /**
   * 상품 수정 (ADMIN)
   */
  updateProduct: async (id: number, data: ProductUpdateRequest) => {
    const response = await getApiClient().put<ApiResponse<Product>>(
      `${API_PREFIX}/admin/products/${id}`,
      data
    )
    return response.data
  },

  /**
   * 상품 삭제 (ADMIN)
   */
  deleteProduct: async (id: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/admin/products/${id}`
    )
    return response.data
  },

  /**
   * 상품 상세 조회 (공개 API 재사용)
   */
  getProduct: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<Product>>(
      `${API_PREFIX}/products/${id}`
    )
    return response.data
  },

  /**
   * 재고 수정 (ADMIN)
   */
  updateStock: async (id: number, stock: number) => {
    const response = await getApiClient().patch<ApiResponse<Product>>(
      `${API_PREFIX}/admin/products/${id}/stock`,
      { stock }
    )
    return response.data
  }
}

// ============================================
// Coupon API
// ============================================

export const couponApi = {
  /**
   * 발급 가능한 쿠폰 목록 조회
   */
  getAvailableCoupons: async () => {
    const response = await getApiClient().get<ApiResponse<Coupon[]>>(
      `${API_PREFIX}/coupons`
    )
    return response.data
  },

  /**
   * 쿠폰 발급
   */
  issueCoupon: async (couponId: number) => {
    const response = await getApiClient().post<ApiResponse<UserCoupon>>(
      `${API_PREFIX}/coupons/${couponId}/issue`
    )
    return response.data
  },

  /**
   * 내 쿠폰 목록 조회
   */
  getUserCoupons: async () => {
    const response = await getApiClient().get<ApiResponse<UserCoupon[]>>(
      `${API_PREFIX}/coupons/my`
    )
    return response.data
  },

  /**
   * 사용 가능한 내 쿠폰 목록 조회
   */
  getAvailableUserCoupons: async () => {
    const response = await getApiClient().get<ApiResponse<UserCoupon[]>>(
      `${API_PREFIX}/coupons/my/available`
    )
    return response.data
  }
}

// ============================================
// TimeDeal API
// ============================================

export const timeDealApi = {
  /**
   * 진행 중인 타임딜 목록 조회
   */
  getActiveTimeDeals: async () => {
    const response = await getApiClient().get<ApiResponse<TimeDeal[]>>(
      `${API_PREFIX}/time-deals`
    )
    return response.data
  },

  /**
   * 타임딜 상세 조회
   */
  getTimeDeal: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<TimeDeal>>(
      `${API_PREFIX}/time-deals/${id}`
    )
    return response.data
  },

  /**
   * 타임딜 구매
   */
  purchaseTimeDeal: async (timeDealProductId: number, quantity: number) => {
    const response = await getApiClient().post<ApiResponse<unknown>>(
      `${API_PREFIX}/time-deals/purchase`,
      { timeDealProductId, quantity }
    )
    return response.data
  },

  /**
   * 내 타임딜 구매 내역 조회
   */
  getMyPurchases: async () => {
    const response = await getApiClient().get<ApiResponse<TimeDealPurchase[]>>(
      `${API_PREFIX}/time-deals/my/purchases`
    )
    return response.data
  }
}

// ============================================
// Admin Coupon API
// ============================================

export const adminCouponApi = {
  /**
   * 쿠폰 목록 조회 (Admin)
   */
  getCoupons: async (page = 0, size = 10) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PagedResponse<Coupon>>>(
      `${API_PREFIX}/admin/coupons?${params}`
    )
    return response.data
  },

  /**
   * 쿠폰 상세 조회
   */
  getCoupon: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<Coupon>>(
      `${API_PREFIX}/admin/coupons/${id}`
    )
    return response.data
  },

  /**
   * 쿠폰 생성 (Admin)
   */
  createCoupon: async (data: CouponCreateRequest) => {
    const response = await getApiClient().post<ApiResponse<Coupon>>(
      `${API_PREFIX}/admin/coupons`,
      data
    )
    return response.data
  },

  /**
   * 쿠폰 비활성화 (Admin)
   */
  deactivateCoupon: async (id: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/admin/coupons/${id}`
    )
    return response.data
  }
}

// ============================================
// Admin TimeDeal API
// ============================================

export const adminTimeDealApi = {
  /**
   * 타임딜 목록 조회 (Admin)
   */
  getTimeDeals: async (page = 0, size = 10) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PagedResponse<TimeDeal>>>(
      `${API_PREFIX}/admin/time-deals?${params}`
    )
    return response.data
  },

  /**
   * 타임딜 상세 조회
   */
  getTimeDeal: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<TimeDeal>>(
      `${API_PREFIX}/admin/time-deals/${id}`
    )
    return response.data
  },

  /**
   * 타임딜 생성 (Admin)
   */
  createTimeDeal: async (data: TimeDealCreateRequest) => {
    const response = await getApiClient().post<ApiResponse<TimeDeal>>(
      `${API_PREFIX}/admin/time-deals`,
      data
    )
    return response.data
  },

  /**
   * 타임딜 취소 (Admin)
   */
  cancelTimeDeal: async (id: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/admin/time-deals/${id}`
    )
    return response.data
  }
}

// ========================================
// Queue API (대기열)
// ========================================
export const queueApi = {
  // 대기열 진입
  enterQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().post<ApiResponse<import('@/types').QueueStatusResponse>>(
      `${API_PREFIX}/queue/${eventType}/${eventId}/enter`
    )
    return response.data
  },

  // 대기열 상태 조회
  getQueueStatus: async (eventType: string, eventId: number) => {
    const response = await getApiClient().get<ApiResponse<import('@/types').QueueStatusResponse>>(
      `${API_PREFIX}/queue/${eventType}/${eventId}/status`
    )
    return response.data
  },

  // 토큰으로 대기열 상태 조회
  getQueueStatusByToken: async (entryToken: string) => {
    const response = await getApiClient().get<ApiResponse<import('@/types').QueueStatusResponse>>(
      `${API_PREFIX}/queue/token/${entryToken}`
    )
    return response.data
  },

  // 대기열 이탈
  leaveQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/queue/${eventType}/${eventId}/leave`
    )
    return response.data
  },

  // 토큰으로 대기열 이탈
  leaveQueueByToken: async (entryToken: string) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/queue/token/${entryToken}`
    )
    return response.data
  },

  // SSE 구독 URL 생성
  getSubscribeUrl: (eventType: string, eventId: number, entryToken: string) => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    return `${baseUrl}${API_PREFIX}/queue/${eventType}/${eventId}/subscribe/${entryToken}`
  }
}

// ========================================
// Admin Queue API
// ========================================
export const adminQueueApi = {
  // 대기열 활성화
  activateQueue: async (eventType: string, eventId: number, request: import('@/types').QueueActivateRequest) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/admin/queue/${eventType}/${eventId}/activate`,
      request
    )
    return response.data
  },

  // 대기열 비활성화
  deactivateQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/admin/queue/${eventType}/${eventId}/deactivate`
    )
    return response.data
  },

  // 대기열 수동 처리
  processQueue: async (eventType: string, eventId: number) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/admin/queue/${eventType}/${eventId}/process`
    )
    return response.data
  }
}

// ============================================
// Search API (Suggest, Popular, Recent)
// ============================================

export const searchApi = {
  suggest: async (keyword: string, size = 5) => {
    const params = new URLSearchParams({ keyword, size: String(size) })
    const response = await getApiClient().get<ApiResponse<string[]>>(
      `${API_PREFIX}/search/suggest?${params}`
    )
    return response.data
  },

  getPopularKeywords: async (size = 10) => {
    const params = new URLSearchParams({ size: String(size) })
    const response = await getApiClient().get<ApiResponse<string[]>>(
      `${API_PREFIX}/search/popular?${params}`
    )
    return response.data
  },

  getRecentKeywords: async (size = 10) => {
    const response = await getApiClient().get<ApiResponse<string[]>>(
      `${API_PREFIX}/search/recent?size=${size}`
    )
    return response.data
  },

  addRecentKeyword: async (keyword: string) => {
    const response = await getApiClient().post<ApiResponse<void>>(
      `${API_PREFIX}/search/recent?keyword=${encodeURIComponent(keyword)}`
    )
    return response.data
  },

  deleteRecentKeyword: async (keyword: string) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/search/recent/${encodeURIComponent(keyword)}`
    )
    return response.data
  },

  clearRecentKeywords: async () => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/search/recent`
    )
    return response.data
  }
}

// ============================================
// Inventory Stream API (SSE)
// ============================================

export const inventoryStreamApi = {
  getStreamUrl: (productIds: number[]) => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const params = productIds.map(id => `productIds=${id}`).join('&')
    return `${baseUrl}${API_PREFIX}/inventory/stream?${params}`
  }
}

// ============================================
// Product Review API (Blog Integration)
// ============================================

export const productReviewApi = {
  getProductWithReviews: async (productId: number) => {
    const response = await getApiClient().get<ApiResponse<import('@/types').ProductWithReviews>>(
      `${API_PREFIX}/products/${productId}/with-reviews`
    )
    return response.data
  }
}

// ============================================
// Admin Payment API
// ============================================

export const adminPaymentApi = {
  refundPayment: async (paymentNumber: string) => {
    const response = await getApiClient().post<ApiResponse<Payment>>(
      `${API_PREFIX}/payments/${paymentNumber}/refund`
    )
    return response.data
  }
}

// ============================================
// Admin Order API
// ============================================

export const adminOrderApi = {
  getOrders: async (params: {
    page?: number
    size?: number
    status?: string
    keyword?: string
  } = {}) => {
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        searchParams.append(key, String(value))
      }
    })
    const response = await getApiClient().get<ApiResponse<PagedResponse<Order>>>(
      `${API_PREFIX}/admin/orders?${searchParams}`
    )
    return response.data
  },

  getOrder: async (orderNumber: string) => {
    const response = await getApiClient().get<ApiResponse<Order>>(
      `${API_PREFIX}/admin/orders/${orderNumber}`
    )
    return response.data
  },

  updateOrderStatus: async (orderNumber: string, status: string) => {
    const response = await getApiClient().put<ApiResponse<Order>>(
      `${API_PREFIX}/admin/orders/${orderNumber}/status`,
      { status }
    )
    return response.data
  }
}

// 통합 export
export const shoppingApi = {
  product: productApi,
  inventory: inventoryApi,
  cart: cartApi,
  order: orderApi,
  payment: paymentApi,
  delivery: deliveryApi,
  stockMovement: stockMovementApi,
  admin: adminProductApi,
  coupon: couponApi,
  timeDeal: timeDealApi,
  adminCoupon: adminCouponApi,
  adminTimeDeal: adminTimeDealApi,
  queue: queueApi,
  adminQueue: adminQueueApi,
  search: searchApi,
  inventoryStream: inventoryStreamApi,
  productReview: productReviewApi,
  adminPayment: adminPaymentApi,
  adminOrder: adminOrderApi
}

export default shoppingApi
