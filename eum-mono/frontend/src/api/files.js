import { client } from '@/api/client';
import { useAuthStore } from '@/store/authStore';

// 첨부파일 API.

// 증빙서류 업로드(진행률 콜백 지원)
export async function uploadFile(applicationId, docType, file, onProgress) {
  const form = new FormData();
  form.append('file', file);
  const body = await client.post(`/applications/${applicationId}/files`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    },
  });
  return { fileId: body.attachmentId, origName: body.originalName, size: body.fileSize };
}

export async function fetchFiles(applicationId) {
  const body = await client.get(`/applications/${applicationId}/files`);
  return body.map((f) => ({ fileId: f.attachmentId, origName: f.originalName, size: f.fileSize }));
}

// 다운로드: 인증 토큰이 필요하므로 fetch로 blob을 받아 저장한다.
export async function downloadFile(fileId, origName) {
  const token = useAuthStore.getState().accessToken;
  const base = import.meta.env.VITE_API_BASE_URL;
  const res = await fetch(`${base}/files/${fileId}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error('파일을 내려받지 못했습니다.');
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = origName ?? `file-${fileId}`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
