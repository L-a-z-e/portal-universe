/**
 * Test data fixtures for E2E tests
 */

export const mockUser = {
  id: 'test-user-1',
  username: 'testuser',
  email: 'test@example.com',
  nickname: 'Test User',
}

export const mockPost = {
  id: 1,
  title: 'Test Post Title',
  content: 'This is test post content',
  author: mockUser,
  tags: ['test', 'e2e'],
  likes: 5,
  createdAt: new Date().toISOString(),
}

export const mockSeries = {
  id: 1,
  name: 'Test Series',
  description: 'Test series description',
  thumbnailUrl: null,
  postCount: 3,
  posts: [
    { id: 1, title: 'Post 1', order: 1 },
    { id: 2, title: 'Post 2', order: 2 },
    { id: 3, title: 'Post 3', order: 3 },
  ],
}

export const mockTag = {
  name: 'test-tag',
  postCount: 10,
  description: 'Test tag description',
}

export const mockComment = {
  id: 1,
  content: 'Test comment',
  author: mockUser,
  createdAt: new Date().toISOString(),
  replies: [],
}
