import { createBrowserRouter, Navigate } from 'react-router-dom';
import Layout from '@/components/layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute';
import { useAuthStore } from '@/store/authStore';
import LoginPage from '@/pages/LoginPage';
import ProgramListPage from '@/pages/applicant/ProgramListPage';
import ApplicationFormPage from '@/pages/applicant/ApplicationFormPage';
import MyApplicationsPage from '@/pages/applicant/MyApplicationsPage';
import ApplicationDetailPage from '@/pages/ApplicationDetailPage';
import ReviewListPage from '@/pages/reviewer/ReviewListPage';
import DashboardPage from '@/pages/reviewer/DashboardPage';
import NotFoundPage from '@/pages/NotFoundPage';

// 역할에 따라 첫 화면을 정한다.
function HomeRedirect() {
  const role = useAuthStore((s) => s.user?.role);
  if (role === 'APPLICANT') return <Navigate to="/programs" replace />;
  if (role === 'REVIEWER' || role === 'MANAGER') return <Navigate to="/review" replace />;
  return <Navigate to="/login" replace />;
}

// 라우팅. 공통 레이아웃 아래에 화면을 끼우고, 보호 라우트로 인증·권한을 가린다.
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <Layout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <HomeRedirect /> },
      { path: 'programs', element: <ProtectedRoute roles={['APPLICANT']}><ProgramListPage /></ProtectedRoute> },
      { path: 'apply', element: <ProtectedRoute roles={['APPLICANT']}><ApplicationFormPage /></ProtectedRoute> },
      { path: 'my', element: <ProtectedRoute roles={['APPLICANT']}><MyApplicationsPage /></ProtectedRoute> },
      { path: 'applications/:applicationId', element: <ApplicationDetailPage /> },
      { path: 'review', element: <ProtectedRoute roles={['REVIEWER', 'MANAGER']}><ReviewListPage /></ProtectedRoute> },
      { path: 'dashboard', element: <ProtectedRoute roles={['REVIEWER', 'MANAGER']}><DashboardPage /></ProtectedRoute> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);
