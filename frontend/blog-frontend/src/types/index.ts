// blog-frontend/src/types/index.ts

export * from './common';

// Post DTOs
export type {
  PostResponse,
  PostCreateRequest,
  PostUpdateRequest,
  PostSummaryResponse,
  PostStatusChangeRequest,
  PostSearchRequest,
  PostStatus,
  AuthorStats,
  BlogStats,
  CategoryStats,
} from '../dto/post';

// Comment DTOs
export type {
  CommentResponse,
  CommentCreateRequest,
  CommentUpdateRequest,
} from '../dto/comment';

// Series DTOs
export type {
  SeriesResponse,
  SeriesCreateRequest,
  SeriesUpdateRequest,
  SeriesListResponse,
  SeriesPostOrderRequest,
} from '../dto/series';

// Tag DTOs
export type {
  TagResponse,
  TagCreateRequest,
  TagStatsResponse,
} from '../dto/tag';

// File DTOs
export type {
  FileUploadResponse,
  FileDeleteRequest,
} from '../dto/file';

// Like DTOs
export type {
  LikeToggleResponse,
  LikeStatusResponse,
  LikerResponse,
} from '../dto/like';

// Navigation DTOs
export type {
  PostNavigationResponse,
  PostNavigationItem,
} from '../dto/navigation';

// User DTOs
export type {
  UserProfileResponse,
  UserProfileUpdateRequest,
  UsernameSetRequest,
  UsernameCheckResponse,
} from '../dto/user';

