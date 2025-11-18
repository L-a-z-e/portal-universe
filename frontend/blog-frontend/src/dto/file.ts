// frontend/blog-frontend/src/dto/file.ts

/**
 * 파일 업로드 응답 DTO
 */
export interface FileUploadResponse {
  url: string;
  filename: string;
  size: number;
  contentType: string;
}

/**
 * 파일 삭제 요청 DTO
 */
export interface FileDeleteRequest {
  url: string;
}