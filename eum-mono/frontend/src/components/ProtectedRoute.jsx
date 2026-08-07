import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

// 보호 라우트. 로그인 여부(인증)와 역할(권한)을 모두 검사한다.
// 권한 미달이면 로그인으로 보내고, 백엔드도 같은 권한을 다시 검사한다(최종 방어선은 서버).
export default function ProtectedRoute({ roles, children }) {
  const location = useLocation();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());
  const role = useAuthStore((s) => s.user?.role);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(role)) {
    return <Navigate to="/" replace />;
  }
  return children;
}
