import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import styles from './Layout.module.css';

// 공통 레이아웃. 헤더 + 좌측 메뉴 + 본문. 메뉴는 역할에 따라 달라진다.
const MENU = {
  APPLICANT: [
    { to: '/programs', label: '지원사업 안내' },
    { to: '/apply', label: '지원금 신청' },
    { to: '/my', label: '내 신청 내역' },
  ],
  REVIEWER: [
    { to: '/review', label: '접수 목록' },
    { to: '/dashboard', label: '심사 현황' },
  ],
  MANAGER: [
    { to: '/review', label: '접수 목록' },
    { to: '/dashboard', label: '심사 현황' },
  ],
};

export default function Layout() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const menu = MENU[user?.role] ?? [];

  const logout = () => {
    clearAuth();
    navigate('/login', { replace: true });
  };

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <Link to="/" className={styles.brand}>
          이음 <span className={styles.brandSub}>소상공인 지원금 신청·심사</span>
        </Link>
        <div className={styles.user}>
          {user && (
            <>
              <span>
                {user.name} 님 ({roleLabel(user.role)})
              </span>
              <button type="button" className={styles.logout} onClick={logout}>
                로그아웃
              </button>
            </>
          )}
        </div>
      </header>
      <div className={styles.body}>
        <nav className={styles.sidebar} aria-label="주요 메뉴">
          {menu.map((m) => (
            <NavLink
              key={m.to}
              to={m.to}
              className={({ isActive }) => (isActive ? `${styles.navItem} ${styles.active}` : styles.navItem)}
            >
              {m.label}
            </NavLink>
          ))}
        </nav>
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function roleLabel(role) {
  return { APPLICANT: '신청자', REVIEWER: '심사자', MANAGER: '관리자' }[role] ?? role;
}
