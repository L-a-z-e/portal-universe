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

const ADMIN_TOKEN_FILE = path.resolve(__dirname, '.auth/admin-access-token.json')
const USER_TOKEN_FILE = path.resolve(__dirname, '.auth/access-token.json')

function getAdminAccessToken(): string | null {
  try {
    if (!fs.existsSync(ADMIN_TOKEN_FILE)) return null
    const data = JSON.parse(fs.readFileSync(ADMIN_TOKEN_FILE, 'utf-8'))
    return data.accessToken || null
  } catch {
    return null
  }
}

function getUserAccessToken(): string | null {
  try {
    if (!fs.existsSync(USER_TOKEN_FILE)) return null
    const data = JSON.parse(fs.readFileSync(USER_TOKEN_FILE, 'utf-8'))
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

  console.log('Data seed: Shopping seed completed')

  // ===============================
  // Blog Data Seed
  // ===============================
  const userToken = getUserAccessToken()
  if (!userToken) {
    console.log('Data seed: No user token available, skipping blog seed')
    console.log('Data seed: Completed')
    return
  }

  const BLOG_API = `${BASE_URL}/api/v1/blog`

  console.log('Data seed: Starting blog data seeding...')

  // Check existing blog posts
  const existingPosts = await apiGet<{ content: { id: string; title: string }[] }>(
    `${BLOG_API}/posts?page=0&size=100`,
    userToken,
  )
  const existingPostTitles = new Set(
    existingPosts?.content?.map((p) => p.title) ?? [],
  )

  const TEST_BLOG_TAGS = ['e2e-test', 'playwright', 'automation', 'blog', 'tutorial']

  const TEST_BLOG_POSTS = [
    {
      title: 'E2E Test Blog Post 1',
      content: '# First Test Post\n\nThis is the first E2E test blog post with some content for testing purposes.\n\n## Section 1\n\nLorem ipsum dolor sit amet.',
      summary: 'First E2E test blog post',
      tags: ['e2e-test', 'playwright'],
      category: 'Testing',
      publishImmediately: true,
    },
    {
      title: 'E2E Test Blog Post 2',
      content: '# Second Test Post\n\nThis is the second E2E test blog post about automation testing.\n\n## Getting Started\n\nLearn how to write automated tests.',
      summary: 'Second E2E test blog post about automation',
      tags: ['e2e-test', 'automation', 'tutorial'],
      category: 'Testing',
      publishImmediately: true,
    },
    {
      title: 'E2E Test Blog Post 3',
      content: '# Third Test Post\n\nA blog post about technology trends and development best practices.\n\n## Tech Stack\n\nModern development tools.',
      summary: 'Third E2E test blog post about tech',
      tags: ['blog', 'tutorial'],
      category: 'Technology',
      publishImmediately: true,
    },
    {
      title: 'E2E Test Draft Post',
      content: '# Draft Post\n\nThis is a draft post that should not be visible to other users.',
      summary: 'A draft post for testing',
      tags: ['e2e-test'],
      category: 'Testing',
      publishImmediately: false,
    },
  ]

  const createdPostIds: string[] = []

  for (const post of TEST_BLOG_POSTS) {
    if (existingPostTitles.has(post.title)) {
      console.log(`  Blog post "${post.title}" already exists, skipping`)
      const existing = existingPosts?.content?.find((p) => p.title === post.title)
      if (existing) createdPostIds.push(existing.id)
      continue
    }

    const created = await apiPost<{ id: string }>(
      `${BLOG_API}/posts`,
      userToken,
      post,
    )
    if (created) {
      console.log(`  Blog post "${post.title}" created (id: ${created.id})`)
      createdPostIds.push(created.id)
    }
  }

  // Create comments on the first published post
  if (createdPostIds.length > 0) {
    const firstPostId = createdPostIds[0]

    const TEST_COMMENTS = [
      { postId: firstPostId, content: 'E2E Test Comment - Great article!' },
      { postId: firstPostId, content: 'E2E Test Comment - Very helpful, thanks for sharing.' },
    ]

    for (const comment of TEST_COMMENTS) {
      const created = await apiPost<{ id: string }>(
        `${BLOG_API}/comments`,
        userToken,
        comment,
      )
      if (created) {
        console.log(`  Blog comment created on post ${firstPostId} (id: ${created.id})`)

        // Create a reply to the first comment
        if (TEST_COMMENTS.indexOf(comment) === 0) {
          await apiPost(
            `${BLOG_API}/comments`,
            userToken,
            { postId: firstPostId, parentCommentId: created.id, content: 'E2E Test Reply - Thanks for the feedback!' },
          )
        }
      }
    }

    // Like the first post
    await apiPost(
      `${BLOG_API}/posts/${firstPostId}/like`,
      userToken,
      {},
    )
    console.log(`  Blog like toggled on post ${firstPostId}`)
  }

  // Create a series with posts
  if (createdPostIds.length >= 2) {
    const seriesResult = await apiPost<{ id: string }>(
      `${BLOG_API}/series`,
      userToken,
      {
        name: 'E2E Test Series',
        description: 'A test series for E2E testing with multiple posts',
      },
    )
    if (seriesResult) {
      console.log(`  Blog series "E2E Test Series" created (id: ${seriesResult.id})`)
      // Add published posts to series
      for (const postId of createdPostIds.slice(0, 3)) {
        await apiPost(
          `${BLOG_API}/series/${seriesResult.id}/posts/${postId}`,
          userToken,
          {},
        )
      }
      console.log(`  Added ${Math.min(3, createdPostIds.length)} posts to series`)
    }
  }

  console.log('Data seed: Blog seed completed')

  // ===============================
  // Prism Data Seed
  // ===============================
  const PRISM_API = `${BASE_URL}/api/v1/prism`

  console.log('Data seed: Starting prism data seeding...')

  // Check existing boards
  const existingBoards = await apiGet<{ id: number; name: string }[]>(
    `${PRISM_API}/boards`,
    userToken,
  )
  const existingBoardNames = new Set(
    (existingBoards ?? []).map((b) => b.name),
  )

  // Create a board
  let testBoardId: number | null = null
  if (existingBoardNames.has('E2E Test Board')) {
    const existing = (existingBoards ?? []).find((b) => b.name === 'E2E Test Board')
    testBoardId = existing?.id ?? null
    console.log(`  Prism board "E2E Test Board" already exists (id: ${testBoardId}), skipping`)
  } else {
    const board = await apiPost<{ id: number }>(
      `${PRISM_API}/boards`,
      userToken,
      { name: 'E2E Test Board', description: 'Board for E2E testing' },
    )
    if (board) {
      testBoardId = board.id
      console.log(`  Prism board "E2E Test Board" created (id: ${board.id})`)
    }
  }

  // Create a task in the board
  if (testBoardId) {
    const existingTasks = await apiGet<{ id: number; title: string }[]>(
      `${PRISM_API}/boards/${testBoardId}/tasks`,
      userToken,
    )
    const existingTaskTitles = new Set(
      (existingTasks ?? []).map((t) => t.title),
    )

    if (!existingTaskTitles.has('E2E Test Task')) {
      const task = await apiPost<{ id: number }>(
        `${PRISM_API}/boards/${testBoardId}/tasks`,
        userToken,
        { title: 'E2E Test Task', description: 'Task for E2E testing', priority: 'MEDIUM' },
      )
      if (task) {
        console.log(`  Prism task "E2E Test Task" created (id: ${task.id})`)
      }
    } else {
      console.log(`  Prism task "E2E Test Task" already exists, skipping`)
    }
  }

  console.log('Data seed: Prism seed completed')
  console.log('Data seed: Completed')
})
