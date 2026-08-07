import axios from 'axios';
import { useAuthStore } from '@/store/authStore';

// 이음 axios 클라이언트. 인터셉터 한곳에서 토큰을 붙이고 오류를 정규화한다.
//
// 이 책에서 화면 개발은 다루지 않는다. 이 폴더를 둔 것은 백엔드를 넷으로 갈라도 화면은
// 그대로라는 것을 보이기 위해서다. 화면이 아는 주소는 /api 하나뿐이고, 그 뒤가 한
// 덩어리인지 네 서비스인지 화면은 알지 못한다.
export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
});

// 요청 인터셉터: 인증 토큰을 Authorization 헤더에 자동으로 붙인다.
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터: 이음 백엔드는 봉투 없이 본문을 그대로 준다.
// 실패면 서버가 준 code·message·필드오류를 정규화한 에러로 바꿔 던진다.
client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const res = error.response;
    // 401: 인증 만료·미인증 → 로그아웃 처리. 화면 전환은 라우터 가드가 맡는다.
    if (res?.status === 401) {
      useAuthStore.getState().clearAuth();
    }
    const body = res?.data ?? {};
    return Promise.reject({
      code: body.code ?? -1,
      message: body.message ?? '요청 처리 중 오류가 발생했습니다.',
      // 서버는 [{ field, value, reason }] 로 준다. 화면이 쓰는 모양으로 바꾼다.
      fieldErrors: (body.errors ?? []).map((e) => ({ field: e.field, message: e.reason })),
      status: res?.status,
    });
  },
);
