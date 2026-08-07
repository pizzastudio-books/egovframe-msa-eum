import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div style={{ textAlign: 'center', padding: '80px 20px' }}>
      <h1 style={{ fontSize: 48, margin: 0 }}>404</h1>
      <p>요청한 화면을 찾을 수 없습니다.</p>
      <Link to="/">처음으로</Link>
    </div>
  );
}
