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
  StockMovement
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
      `${API_PREFIX}/products/search?${params}`
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
   * 재고 추가 (관리자)
   */
  addStock: async (productId: number, data: InventoryUpdateRequest) => {
    const response = await getApiClient().post<ApiResponse<Inventory>>(
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

// 통합 export
export const shoppingApi = {
  product: productApi,
  inventory: inventoryApi,
  cart: cartApi,
  order: orderApi,
  payment: paymentApi,
  delivery: deliveryApi,
  stockMovement: stockMovementApi
}

export default shoppingApi
