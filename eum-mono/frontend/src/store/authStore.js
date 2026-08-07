import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// 전역 인증 상태. 토큰과 로그인 사용자를 들고, 새로고침에도 유지되도록 localStorage에 저장한다.
// axios 인터셉터가 이 스토어에서 토큰을 꺼내 요청에 붙인다.
export const useAuthStore = create(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null, // { userId, name, role }

      setAuth: (accessToken, user) => set({ accessToken, user }),
      clearAuth: () => set({ accessToken: null, user: null }),

      isAuthenticated: () => !!get().accessToken,
      hasRole: (...roles) => roles.includes(get().user?.role),
    }),
    { name: 'eum-auth' },
  ),
);
