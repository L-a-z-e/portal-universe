/**
 * Cart Store (Zustand)
 *
 * 장바구니 상태 관리
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import { AxiosError } from 'axios'
import { cartApi } from '@/api'
import type { Cart, CartItem, AddCartItemRequest } from '@/types'

interface CartState {
  // State
  cart: Cart | null
  loading: boolean
  error: string | null

  // Computed
  itemCount: number
  totalAmount: number

  // Actions
  fetchCart: () => Promise<void>
  addItem: (productId: number, productName: string, price: number, quantity: number) => Promise<void>
  updateItemQuantity: (itemId: number, quantity: number) => Promise<void>
  removeItem: (itemId: number) => Promise<void>
  clearCart: () => Promise<void>
  reset: () => void
}

const initialState = {
  cart: null,
  loading: false,
  error: null,
  itemCount: 0,
  totalAmount: 0
}

const getErrorMessage = (error: unknown, fallback: string): string => {
  if (error instanceof AxiosError) {
    return error.message || fallback
  }
  if (error instanceof Error) {
    return error.message || fallback
  }
  return fallback
}

export const useCartStore = create<CartState>()(
  devtools(
    (set, get) => ({
      ...initialState,

      /**
       * 장바구니 조회
       */
      fetchCart: async () => {
        set({ loading: true, error: null })
        try {
          const response = await cartApi.getCart()
          const cart = response.data
          set({
            cart,
            itemCount: cart.itemCount,
            totalAmount: cart.totalAmount,
            loading: false
          })
        } catch (error) {
          // 장바구니가 없는 경우 (신규 사용자)
          if (error instanceof AxiosError && error.response?.status === 404) {
            set({
              cart: {
                id: 0,
                userId: '',
                status: 'ACTIVE',
                items: [],
                totalAmount: 0,
                itemCount: 0,
                totalQuantity: 0,
                createdAt: new Date().toISOString()
              },
              itemCount: 0,
              totalAmount: 0,
              loading: false
            })
          } else {
            set({
              error: getErrorMessage(error, 'Failed to fetch cart'),
              loading: false
            })
          }
        }
      },

      /**
       * 상품 추가
       */
      addItem: async (productId: number, productName: string, price: number, quantity: number) => {
        set({ loading: true, error: null })
        try {
          const response = await cartApi.addItem({
            productId,
            quantity
          })
          const cart = response.data
          set({
            cart,
            itemCount: cart.itemCount,
            totalAmount: cart.totalAmount,
            loading: false
          })
        } catch (error) {
          set({
            error: getErrorMessage(error, 'Failed to add item'),
            loading: false
          })
          throw error
        }
      },

      /**
       * 수량 변경
       */
      updateItemQuantity: async (itemId: number, quantity: number) => {
        set({ loading: true, error: null })
        try {
          const response = await cartApi.updateItem(itemId, { quantity })
          const cart = response.data
          set({
            cart,
            itemCount: cart.itemCount,
            totalAmount: cart.totalAmount,
            loading: false
          })
        } catch (error) {
          set({
            error: getErrorMessage(error, 'Failed to update item'),
            loading: false
          })
          throw error
        }
      },

      /**
       * 상품 제거
       */
      removeItem: async (itemId: number) => {
        set({ loading: true, error: null })
        try {
          const response = await cartApi.removeItem(itemId)
          const cart = response.data
          set({
            cart,
            itemCount: cart.itemCount,
            totalAmount: cart.totalAmount,
            loading: false
          })
        } catch (error) {
          set({
            error: getErrorMessage(error, 'Failed to remove item'),
            loading: false
          })
          throw error
        }
      },

      /**
       * 장바구니 비우기
       */
      clearCart: async () => {
        set({ loading: true, error: null })
        try {
          await cartApi.clearCart()
          set({
            cart: {
              id: 0,
              userId: '',
              status: 'ACTIVE',
              items: [],
              totalAmount: 0,
              itemCount: 0,
              totalQuantity: 0,
              createdAt: new Date().toISOString()
            },
            itemCount: 0,
            totalAmount: 0,
            loading: false
          })
        } catch (error) {
          set({
            error: getErrorMessage(error, 'Failed to clear cart'),
            loading: false
          })
          throw error
        }
      },

      /**
       * 스토어 초기화
       */
      reset: () => {
        set(initialState)
      }
    }),
    { name: 'CartStore' }
  )
)

export default useCartStore
