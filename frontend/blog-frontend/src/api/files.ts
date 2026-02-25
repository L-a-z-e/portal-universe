import apiClient from './index';

export interface FileUploadResponse {
  url: string;
  filename: string;
  size: number;
  contentType: string;
}

interface PresignedUrlResponse {
  uploadUrl: string;
  objectUrl: string;
  key: string;
  expiresInSeconds: number;
}

/**
 * Presigned URL로 S3에 직접 업로드
 *
 * 1. 서버에서 Presigned URL 발급 (파일 메타데이터만 전송)
 * 2. 발급받은 URL로 S3에 직접 PUT (서버 메모리 사용 없음)
 */
export async function uploadFile(file: File | Blob): Promise<FileUploadResponse> {
  const filename = file instanceof File ? file.name : 'image.png';
  const contentType = file.type || 'image/png';

  const presignResponse = await apiClient.post<{ data: PresignedUrlResponse }>(
    '/api/v1/blog/file/presign',
    { filename, contentType, contentLength: file.size },
  );
  const { uploadUrl, objectUrl } = presignResponse.data.data;

  const uploadResponse = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': contentType },
    body: file,
  });

  if (!uploadResponse.ok) {
    throw new Error(`S3 upload failed: ${uploadResponse.status}`);
  }

  return {
    url: objectUrl,
    filename,
    size: file.size,
    contentType,
  };
}

/**
 * S3에서 파일 삭제 (ADMIN 권한 필요)
 */
export async function deleteFile(url: string): Promise<void> {
  await apiClient.delete('/api/v1/blog/file/delete', {
    data: { url },
  });
}
