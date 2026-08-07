import { Link } from 'react-router-dom';
import { useMyApplications } from '@/hooks/useApplications';
import StatusBadge from '@/components/StatusBadge';
import { formatMoney, formatDate } from '@/lib/format';

// 내 신청 내역. TanStack Query로 조회하고, 신청 제출 후 자동 갱신된다.
export default function MyApplicationsPage() {
  const { data, isLoading, isError } = useMyApplications();

  if (isLoading) return <p>불러오는 중…</p>;
  if (isError) return <p role="alert">목록을 불러오지 못했습니다.</p>;

  return (
    <section>
      <h1>내 신청 내역</h1>
      {data?.length === 0 ? (
        <p>아직 신청한 내역이 없습니다. <Link to="/programs">지원사업 보러 가기</Link></p>
      ) : (
        <table style={tableStyle}>
          <caption className="sr-only">내가 신청한 지원금 목록</caption>
          <thead>
            <tr>
              <th scope="col">지원사업</th>
              <th scope="col">상호명</th>
              <th scope="col" style={{ textAlign: 'right' }}>신청 금액</th>
              <th scope="col">상태</th>
              <th scope="col">신청일</th>
              <th scope="col"><span className="sr-only">상세</span></th>
            </tr>
          </thead>
          <tbody>
            {data?.map((a) => (
              <tr key={a.applicationId}>
                <td>{a.programName}</td>
                <td>{a.bizName}</td>
                <td style={{ textAlign: 'right' }}>{formatMoney(a.requestAmount)}</td>
                <td><StatusBadge status={a.status} label={a.statusLabel} /></td>
                <td>{formatDate(a.createdAt)}</td>
                <td><Link to={`/applications/${a.applicationId}`}>상세</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

const tableStyle = { width: '100%', borderCollapse: 'collapse', background: '#fff', fontSize: 14 };
