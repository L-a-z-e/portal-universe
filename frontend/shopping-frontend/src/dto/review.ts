export interface BlogReview {
  id: string
  title: string
  content: string
  authorId: string
  productId: string
}

export interface ProductWithReviews {
  id: number
  name: string
  description: string
  price: number
  stock: number
  reviews: BlogReview[]
}
