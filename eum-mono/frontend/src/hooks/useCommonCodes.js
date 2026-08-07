import { useQuery } from '@tanstack/react-query';
import { fetchCodes } from '@/api/codes';

// 공통코드 조회 훅. 코드 그룹은 자주 바뀌지 않으므로 오래 캐시한다.
// 의존 셀렉트(시도→시군구)에서는 parentCode에 ''(빈 값)을 넘기면 부모를 고르기 전까지 비활성화된다.
// (최상위 코드 조회는 parentCode를 생략 → undefined → 활성화)
export function useCommonCodes(codeGroup, parentCode) {
  return useQuery({
    queryKey: ['codes', codeGroup, parentCode ?? null],
    queryFn: () => fetchCodes(codeGroup, parentCode || undefined),
    staleTime: 60 * 60 * 1000, // 1시간
    enabled: !!codeGroup && parentCode !== '',
  });
}
