// frontend/blog-frontend/src/api/files.ts

import apiClient from './index';

/**
 * 파일 업로드 응답 DTO
 * Spring의 FileUploadResponse와 매칭
 */
export interface FileUploadResponse {
  url: string;          // S3에 업로드된 파일의 전체 URL
  filename: string;     // 원본 파일명
  size: number;         // 파일 크기 (bytes)
  contentType: string;  // MIME 타입 (예: image/jpeg)
}

/**
 * 파일 삭제 요청 DTO
 * Spring의 FileDeleteRequest와 매칭
 */
export interface FileDeleteRequest {
  url: string;  // 삭제할 파일의 S3 URL
}

/**
 * S3에 파일 업로드
 *
 * @param file - 업로드할 파일 객체 (File 또는 Blob)
 * @returns 업로드된 파일 정보 (url, filename, size, contentType)
 *
 * 사용 예시:
 * const file = event.target.files[0];
 * const response = await uploadFile(file);
 * console.log('업로드 URL:', response.url);
 */
export async function uploadFile(file: File | Blob): Promise<FileUploadResponse> {
  // FormData 생성 (multipart/form-data 전송을 위해)
  const formData = new FormData();
  formData.append('file', file);

  // API 호출
  const response = await apiClient.post<FileUploadResponse>(
    '/api/blog/file/upload',  // Gateway를 통한 라우팅: /api/blog/** -> blog-service/**
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',  // 파일 업로드 시 필수
      },
    }
  );

  return response.data;
}

/**
 * S3에서 파일 삭제 (ADMIN 권한 필요)
 *
 * @param url - 삭제할 파일의 S3 URL
 *
 * 사용 예시:
 * await deleteFile('http://localhost:4566/blog-bucket/abc123_image.jpg');
 */
export async function deleteFile(url: string): Promise<void> {
  await apiClient.delete('/api/blog/file/delete', {
    data: { url },  // DELETE 요청의 body
  });
}