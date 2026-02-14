import axios from 'axios'
import type { AxiosInstance } from 'axios'
import { getPortalApiClient } from '@portal/react-bridge'

const fallbackClient = axios.create({ baseURL: '' })

const getClient = (): AxiosInstance => {
  return getPortalApiClient() ?? fallbackClient
}

// Seller API
export const sellerApi = {
  getProfile: () => getClient().get('/api/v1/seller/sellers/me'),
  register: (data: any) => getClient().post('/api/v1/seller/sellers/register', data),
  updateProfile: (data: any) => getClient().put('/api/v1/seller/sellers/me', data),
}

// Product API (판매자용)
export const sellerProductApi = {
  getProducts: (params?: any) => getClient().get('/api/v1/seller/products', { params }),
  getProduct: (id: number) => getClient().get(`/api/v1/seller/products/${id}`),
  createProduct: (data: any) => getClient().post('/api/v1/seller/products', data),
  updateProduct: (id: number, data: any) => getClient().put(`/api/v1/seller/products/${id}`, data),
  deleteProduct: (id: number) => getClient().delete(`/api/v1/seller/products/${id}`),
}

// Inventory API
export const sellerInventoryApi = {
  getInventory: (productId: number) => getClient().get(`/api/v1/seller/inventory/${productId}`),
  addStock: (productId: number, data: any) => getClient().put(`/api/v1/seller/inventory/${productId}/add`, data),
  getMovements: (productId: number, params?: any) => getClient().get(`/api/v1/seller/inventory/${productId}/movements`, { params }),
}

// Order API (판매자 주문 관리)
export const sellerOrderApi = {
  getOrders: (params?: any) => getClient().get('/api/v1/seller/orders', { params }),
  getOrder: (orderNumber: string) => getClient().get(`/api/v1/seller/orders/${orderNumber}`),
  updateOrderStatus: (orderNumber: string, data: any) => getClient().put(`/api/v1/seller/orders/${orderNumber}/status`, data),
}

// Delivery API
export const sellerDeliveryApi = {
  updateDeliveryStatus: (trackingNumber: string, data: any) =>
    getClient().put(`/api/v1/seller/deliveries/${trackingNumber}/status`, data),
}

// Coupon API (판매자 쿠폰 관리)
export const sellerCouponApi = {
  getCoupons: (params?: any) => getClient().get('/api/v1/seller/coupons', { params }),
  createCoupon: (data: any) => getClient().post('/api/v1/seller/coupons', data),
  deleteCoupon: (id: number) => getClient().delete(`/api/v1/seller/coupons/${id}`),
}

// TimeDeal API (판매자 타임딜 관리)
export const sellerTimeDealApi = {
  getTimeDeals: (params?: any) => getClient().get('/api/v1/seller/time-deals', { params }),
  createTimeDeal: (data: any) => getClient().post('/api/v1/seller/time-deals', data),
  deleteTimeDeal: (id: number) => getClient().delete(`/api/v1/seller/time-deals/${id}`),
}

// Queue API
export const sellerQueueApi = {
  activateQueue: (eventType: string, eventId: string) =>
    getClient().post(`/api/v1/seller/queue/${eventType}/${eventId}/activate`),
  deactivateQueue: (eventType: string, eventId: string) =>
    getClient().post(`/api/v1/seller/queue/${eventType}/${eventId}/deactivate`),
}

// Settlement API
export const settlementApi = {
  getPeriods: (params?: any) => getClient().get('/api/v1/settlement/periods', { params }),
  getPeriod: (id: number) => getClient().get(`/api/v1/settlement/periods/${id}`),
  getSellerSettlements: (sellerId: number, params?: any) =>
    getClient().get(`/api/v1/settlement/sellers/${sellerId}`, { params }),
}
