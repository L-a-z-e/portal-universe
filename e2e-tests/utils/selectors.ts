import { Page, Locator } from '@playwright/test'

/**
 * Selector 헬퍼 - 여러 CSS 클래스 옵션을 지원
 */
export function multiSelector(page: Page, ...selectors: string[]): Locator {
  return page.locator(selectors.join(', '))
}

/**
 * 공통 UI 요소 셀렉터
 */
export const commonSelectors = {
  // 로딩
  loading: (page: Page) => page.locator('.loading, .spinner, [data-loading]'),
  skeleton: (page: Page) => page.locator('.skeleton, [data-skeleton]'),

  // 버튼
  submitButton: (page: Page) =>
    page.getByRole('button', { name: /등록|submit|저장|save/i })
      .or(page.locator('button[type="submit"]')),
  cancelButton: (page: Page) =>
    page.getByRole('button', { name: /취소|cancel/i }),

  // 모달
  modal: (page: Page) => page.locator('[role="dialog"], .modal'),
  modalClose: (page: Page) =>
    page.getByRole('button', { name: /닫기|close/i })
      .or(page.locator('.modal-close')),

  // 에러
  errorMessage: (page: Page) =>
    page.locator('.error, .error-message, [role="alert"]'),

  // 폼
  formError: (page: Page) =>
    page.locator('.form-error, .field-error, .invalid-feedback'),

  // 토스트/알림
  toast: (page: Page) =>
    page.locator('.toast, .notification, [role="status"]'),
}

/**
 * Blog 전용 셀렉터
 */
export const blogSelectors = {
  postCard: (page: Page) => page.locator('.post-card, .post-item, article'),
  postTitle: (page: Page) => page.locator('.post-title, h1, h2'),
  postContent: (page: Page) => page.locator('.post-content, .content'),

  commentSection: (page: Page) => page.locator('.comment-section, .comments'),
  commentItem: (page: Page) => page.locator('.comment-item, .comment'),
  commentInput: (page: Page) => page.locator('textarea').first(),

  likeButton: (page: Page) => page.locator('.like-button, .like-btn'),
  followButton: (page: Page) =>
    page.getByRole('button', { name: /팔로우|follow/i }),

  tagList: (page: Page) => page.locator('.tag-list, .tags'),
  tagItem: (page: Page) => page.locator('.tag-item, .tag'),

  seriesList: (page: Page) => page.locator('.series-list, .my-series'),
  seriesItem: (page: Page) => page.locator('.series-item, .series-card'),
}

/**
 * Shopping 전용 셀렉터
 */
export const shoppingSelectors = {
  productCard: (page: Page) => page.locator('.product-card, .product-item'),
  productPrice: (page: Page) => page.locator('.price, .product-price'),
  addToCartButton: (page: Page) =>
    page.getByRole('button', { name: /장바구니|cart|담기/i }),

  cartItem: (page: Page) => page.locator('.cart-item'),
  cartTotal: (page: Page) => page.locator('.cart-total, .total'),
  checkoutButton: (page: Page) =>
    page.getByRole('button', { name: /주문|checkout|결제/i }),

  wishlistButton: (page: Page) =>
    page.getByRole('button', { name: /찜|wish|좋아요/i }),
}

/**
 * Prism 전용 셀렉터
 */
export const prismSelectors = {
  chatContainer: (page: Page) => page.locator('.chat-container, .chat-interface'),
  chatInput: (page: Page) =>
    page.getByRole('textbox').or(page.locator('textarea, input[type="text"]').first()),
  chatSendButton: (page: Page) =>
    page.getByRole('button', { name: /전송|send/i }),
  chatMessage: (page: Page) => page.locator('.message, .chat-message'),
  aiMessage: (page: Page) => page.locator('.ai-message, .assistant-message, .bot-message'),
  userMessage: (page: Page) => page.locator('.user-message, .human-message'),
}
