export const testPosts = {
  existing: {
    id: '1',
    title: '테스트 게시글',
    slug: 'test-post',
  },
  draft: {
    title: '임시 저장 게시글',
    content: '임시 저장 내용입니다.',
  },
}

export const testProducts = {
  existing: {
    id: '1',
    name: '테스트 상품',
    price: 10000,
  },
}

export const testComments = {
  new: {
    content: 'E2E 테스트 댓글입니다.',
  },
  reply: {
    content: 'E2E 테스트 답글입니다.',
  },
}

export const routes = {
  portal: {
    home: '/',
    login: '/login',
    signup: '/signup',
    dashboard: '/dashboard',
    settings: '/settings',
    status: '/status',
    profile: '/profile',
  },
  blog: {
    home: '/blog',
    feed: '/blog/feed',
    post: (id: string) => `/blog/posts/${id}`,
    write: '/blog/write',
    edit: (postId: string) => `/blog/edit/${postId}`,
    myPage: '/blog/my',
    user: (username: string) => `/blog/@${username}`,
    tag: (tag: string) => `/blog/tags/${tag}`,
    tags: '/blog/tags',
    series: (id: string) => `/blog/series/${id}`,
    trending: '/blog/trending',
    categories: '/blog/categories',
    advancedSearch: '/blog/search/advanced',
    stats: '/blog/stats',
  },
  shopping: {
    home: '/shopping',
    products: '/shopping/products',
    product: (id: string) => `/shopping/products/${id}`,
    cart: '/shopping/cart',
    orders: '/shopping/orders',
    wishlist: '/shopping/wishlist',
    admin: {
      queue: '/shopping/admin/queue',
      products: '/shopping/admin/products',
      coupons: '/shopping/admin/coupons',
      timeDeals: '/shopping/admin/time-deals',
      orders: '/shopping/admin/orders',
      deliveries: '/shopping/admin/deliveries',
      stockMovements: '/shopping/admin/stock-movements',
    },
  },
  prism: {
    home: '/prism',
    chat: '/prism/chat',
    settings: '/prism/settings',
    boards: '/prism/boards',
    agents: '/prism/agents',
    providers: '/prism/providers',
  },
}
