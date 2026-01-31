/**
 * E2E Test Data Seed Setup
 *
 * Creates test products, coupons, and time-deals via Admin API
 * so that admin E2E tests can edit/delete real data.
 *
 * Depends on: admin-setup (uses admin access token)
 */
import { test as setup } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const TOKEN_FILE = path.resolve(__dirname, '.auth/admin-access-token.json')

function getAdminAccessToken(): string | null {
  try {
    if (!fs.existsSync(TOKEN_FILE)) return null
    const data = JSON.parse(fs.readFileSync(TOKEN_FILE, 'utf-8'))
    return data.accessToken || null
  } catch {
    return null
  }
}

const BASE_URL = process.env.BASE_URL || 'https://localhost:30000'

// Gateway routes shopping admin API via /api/v1/shopping/admin/...
// which is rewritten to /admin/... by StripPrefix=3
const ADMIN_API = `${BASE_URL}/api/v1/shopping/admin`
const PUBLIC_API = `${BASE_URL}/api/v1/shopping`

interface ProductResponse {
  id: number
  name: string
  description: string
  price: number
  stock: number
}

interface ApiResponse<T> {
  success: boolean
  data: T
  error: unknown
}

const TEST_PRODUCTS = [
  { name: 'E2E Test Product 1', description: 'E2E 테스트용 상품 1', price: 10000, stock: 100 },
  { name: 'E2E Test Product 2', description: 'E2E 테스트용 상품 2', price: 25000, stock: 50 },
  { name: 'E2E Test Product 3', description: 'E2E 테스트용 상품 3', price: 50000, stock: 200 },
]

async function apiGet<T>(url: string, token: string): Promise<T | null> {
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json',
      },
      // Self-signed cert in Docker
      ...(process.env.NODE_TLS_REJECT_UNAUTHORIZED === '0' ? {} : {}),
    })
    if (!resp.ok) return null
    const body: ApiResponse<T> = await resp.json()
    return body.success ? body.data : null
  } catch {
    return null
  }
}

async function apiPost<T>(url: string, token: string, body: unknown): Promise<T | null> {
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    body: JSON.stringify(body),
    })
    if (!resp.ok) {
      const text = await resp.text()
      console.log(`  POST ${url} failed: ${resp.status} ${text.substring(0, 200)}`)
      return null
    }
    const json: ApiResponse<T> = await resp.json()
    return json.success ? json.data : null
  } catch (e) {
    console.log(`  POST ${url} error: ${e}`)
    return null
  }
}

setup('seed test data', async () => {
  // Disable TLS verification for self-signed certs
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'

  const token = getAdminAccessToken()
  if (!token) {
    console.log('Data seed: No admin token available, skipping seed')
    return
  }

  console.log('Data seed: Starting test data seeding...')

  // --- Products ---
  // Check existing products
  const existingProducts = await apiGet<{ content: ProductResponse[] }>(
    `${PUBLIC_API}/products?page=0&size=100`,
    token,
  )

  const existingNames = new Set(
    existingProducts?.content?.map((p) => p.name) ?? [],
  )

  let createdProducts: ProductResponse[] = []

  for (const product of TEST_PRODUCTS) {
    if (existingNames.has(product.name)) {
      console.log(`  Product "${product.name}" already exists, skipping`)
      const existing = existingProducts?.content?.find((p) => p.name === product.name)
      if (existing) createdProducts.push(existing)
      continue
    }

    const created = await apiPost<ProductResponse>(
      `${ADMIN_API}/products`,
      token,
      product,
    )
    if (created) {
      console.log(`  Product "${product.name}" created (id: ${created.id})`)
      createdProducts.push(created)
    }
  }

  // --- Coupon ---
  const now = new Date()
  const oneMonthLater = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)

  const testCoupon = {
    code: `E2E-TEST-${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`,
    name: 'E2E Test Coupon',
    description: 'E2E 테스트용 쿠폰',
    discountType: 'PERCENTAGE',
    discountValue: 10,
    minimumOrderAmount: 5000,
    maximumDiscountAmount: 10000,
    totalQuantity: 100,
    startsAt: now.toISOString().replace('Z', ''),
    expiresAt: oneMonthLater.toISOString().replace('Z', ''),
  }

  const couponResult = await apiPost(
    `${ADMIN_API}/coupons`,
    token,
    testCoupon,
  )
  if (couponResult) {
    console.log(`  Coupon "${testCoupon.code}" created`)
  } else {
    console.log(`  Coupon "${testCoupon.code}" creation skipped (may already exist)`)
  }

  // --- Time Deal ---
  if (createdProducts.length > 0) {
    const twoHoursLater = new Date(now.getTime() + 2 * 60 * 60 * 1000)
    const oneDayLater = new Date(now.getTime() + 24 * 60 * 60 * 1000)

    const testTimeDeal = {
      name: 'E2E Test Time Deal',
      description: 'E2E 테스트용 타임딜',
      startsAt: twoHoursLater.toISOString().replace('Z', ''),
      endsAt: oneDayLater.toISOString().replace('Z', ''),
      products: [
        {
          productId: createdProducts[0].id,
          dealPrice: createdProducts[0].price * 0.8,
          dealQuantity: 10,
          maxPerUser: 2,
        },
      ],
    }

    const timeDealResult = await apiPost(
      `${ADMIN_API}/time-deals`,
      token,
      testTimeDeal,
    )
    if (timeDealResult) {
      console.log(`  Time deal "${testTimeDeal.name}" created`)
    } else {
      console.log(`  Time deal creation skipped (may already exist or product unavailable)`)
    }
  }

  console.log('Data seed: Completed')
})
