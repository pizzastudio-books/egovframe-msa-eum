import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createApplication,
  fetchApplication,
  fetchMyApplications,
  fetchStats,
  searchApplications,
  transitionApplication,
} from '@/api/applications';

// 신청·심사 서버 상태 훅.

export function useMyApplications() {
  return useQuery({ queryKey: ['applications', 'my'], queryFn: fetchMyApplications });
}

export function useApplicationSearch(condition) {
  return useQuery({
    queryKey: ['applications', 'search', condition],
    queryFn: () => searchApplications(condition),
    placeholderData: (prev) => prev, // 페이지 전환 시 이전 데이터 유지(깜빡임 방지)
  });
}

export function useStats() {
  return useQuery({ queryKey: ['applications', 'stats'], queryFn: fetchStats });
}

export function useApplication(applicationId) {
  return useQuery({
    queryKey: ['applications', 'detail', applicationId],
    queryFn: () => fetchApplication(applicationId),
    enabled: !!applicationId,
  });
}

export function useCreateApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createApplication,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['applications', 'my'] }),
  });
}

// 상태 전이 성공 시 목록·상세·KPI를 한 번에 무효화 → 화면이 동시에 갱신된다(절정).
export function useTransition(applicationId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload) => transitionApplication(applicationId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['applications', 'detail', applicationId] });
      qc.invalidateQueries({ queryKey: ['applications', 'search'] });
      qc.invalidateQueries({ queryKey: ['applications', 'stats'] });
      qc.invalidateQueries({ queryKey: ['applications', 'my'] });
    },
  });
}
