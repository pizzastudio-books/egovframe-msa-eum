import { QueryClient } from "@tanstack/react-query";

// TanStack Query 클라이언트. 서버 상태의 캐시·재요청 정책을 한곳에서 정한다.
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000, // 30초간은 최신이라고 보고 재요청하지 않는다
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
