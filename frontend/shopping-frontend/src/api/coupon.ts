import { getApiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type { Coupon, UserCoupon, CouponCreateRequest } from '@/dto/coupon'

const API_PREFIX = '/api/v1/shopping'

export const couponApi = {
  getAvailableCoupons: async () => {
    const response = await getApiClient().get<ApiResponse<Coupon[]>>(
      `${API_PREFIX}/coupons`
    )
    return response.data
  },

  issueCoupon: async (couponId: number) => {
    const response = await getApiClient().post<ApiResponse<UserCoupon>>(
      `${API_PREFIX}/coupons/${couponId}/issue`
    )
    return response.data
  },

  getUserCoupons: async () => {
    const response = await getApiClient().get<ApiResponse<UserCoupon[]>>(
      `${API_PREFIX}/coupons/my`
    )
    return response.data
  },

  getAvailableUserCoupons: async () => {
    const response = await getApiClient().get<ApiResponse<UserCoupon[]>>(
      `${API_PREFIX}/coupons/my/available`
    )
    return response.data
  }
}

export const adminCouponApi = {
  getCoupons: async (page = 1, size = 10) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PageResponse<Coupon>>>(
      `${API_PREFIX}/admin/coupons?${params}`
    )
    return response.data
  },

  getCoupon: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<Coupon>>(
      `${API_PREFIX}/admin/coupons/${id}`
    )
    return response.data
  },

  createCoupon: async (data: CouponCreateRequest) => {
    const response = await getApiClient().post<ApiResponse<Coupon>>(
      `${API_PREFIX}/admin/coupons`,
      data
    )
    return response.data
  },

  deactivateCoupon: async (id: number) => {
    const response = await getApiClient().delete<ApiResponse<void>>(
      `${API_PREFIX}/admin/coupons/${id}`
    )
    return response.data
  }
}
