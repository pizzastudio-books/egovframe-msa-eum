import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '@/api/auth';
import { useAuthStore } from '@/store/authStore';

// 로그인. 성공 시 토큰·사용자를 스토어에 저장하고 역할별 첫 화면으로 보낸다.
const HOME_BY_ROLE = { APPLICANT: '/programs', REVIEWER: '/review', MANAGER: '/review' };

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await login({ username, password });
      setAuth(data.accessToken, { userId: data.userId, name: data.name, role: data.role });
      navigate(HOME_BY_ROLE[data.role] ?? '/', { replace: true });
    } catch (err) {
      setError(err.message ?? '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 360, margin: '80px auto', padding: 24 }}>
      <h1 style={{ fontSize: 24, marginBottom: 4 }}>이음</h1>
      <p style={{ color: '#6b7280', marginTop: 0 }}>소상공인 지원금 신청·심사</p>
      <form onSubmit={onSubmit} noValidate>
        <label htmlFor="username" style={labelStyle}>아이디</label>
        <input
          id="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          style={inputStyle}
        />
        <label htmlFor="password" style={labelStyle}>비밀번호</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          style={inputStyle}
        />
        {error && (
          <p role="alert" style={{ color: '#dc2626', fontSize: 14 }}>{error}</p>
        )}
        <button type="submit" disabled={loading} style={buttonStyle}>
          {loading ? '로그인 중…' : '로그인'}
        </button>
      </form>
      <p style={{ fontSize: 12, color: '#9ca3af', marginTop: 16, lineHeight: 1.7 }}>
        테스트 계정: kim·lee(신청자) / park(심사자) / choi(관리자) — 비밀번호 Password1!
      </p>
    </div>
  );
}

const labelStyle = { display: 'block', fontSize: 14, fontWeight: 600, margin: '12px 0 4px' };
const inputStyle = {
  width: '100%', padding: '10px 12px', border: '1px solid #d1d5db', borderRadius: 8, fontSize: 15,
};
const buttonStyle = {
  width: '100%', marginTop: 20, padding: '12px', border: 0, borderRadius: 8,
  background: '#1d4ed8', color: '#fff', fontSize: 16, fontWeight: 600, cursor: 'pointer',
};
