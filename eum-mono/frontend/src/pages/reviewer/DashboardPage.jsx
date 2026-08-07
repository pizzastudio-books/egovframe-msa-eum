import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { useStats } from '@/hooks/useApplications';
import { STATUS_COLORS } from '@/lib/format';

// 심사 현황. 상태별 KPI 카드 + 막대 차트. 심사 처리가 일어나면 invalidate로 자동 갱신된다.
// 차트는 색만으로 구분하지 않도록 같은 데이터를 표로도 제공한다(접근성).
export default function DashboardPage() {
  const { data: stats, isLoading } = useStats();

  if (isLoading) return <p>불러오는 중…</p>;
  const total = stats?.reduce((sum, s) => sum + s.count, 0) ?? 0;

  return (
    <section>
      <h1>심사 현황</h1>

      <div style={cardGrid}>
        {stats?.map((s) => (
          <div key={s.status} style={{ ...card, borderTopColor: STATUS_COLORS[s.status] ?? '#4b5563' }}>
            <div style={{ fontSize: 13, color: '#6b7280' }}>{s.statusLabel}</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{s.count.toLocaleString('ko-KR')}</div>
          </div>
        ))}
      </div>

      <h2 style={{ fontSize: 18, marginTop: 28 }}>상태별 분포 (총 {total.toLocaleString('ko-KR')}건)</h2>
      <div style={{ width: '100%', height: 280, background: '#fff', borderRadius: 10, padding: 16 }}>
        <ResponsiveContainer>
          <BarChart data={stats} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="statusLabel" fontSize={12} />
            <YAxis allowDecimals={false} fontSize={12} />
            <Tooltip formatter={(v) => [`${v}건`, '건수']} />
            <Bar dataKey="count" fill="#1d4ed8" radius={[4, 4, 0, 0]} isAnimationActive={false} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* 차트와 동일한 데이터의 표 대체(스크린리더·색 인지 어려운 사용자용) */}
      <table style={{ marginTop: 16, borderCollapse: 'collapse', fontSize: 14 }}>
        <caption className="sr-only">상태별 신청 건수</caption>
        <thead>
          <tr><th scope="col" style={th}>상태</th><th scope="col" style={th}>건수</th></tr>
        </thead>
        <tbody>
          {stats?.map((s) => (
            <tr key={s.status}>
              <td style={td}>{s.statusLabel}</td>
              <td style={{ ...td, textAlign: 'right' }}>{s.count.toLocaleString('ko-KR')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

const cardGrid = { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: 12, marginTop: 16 };
const card = { background: '#fff', borderRadius: 10, padding: 16, borderTop: '4px solid #4b5563' };
const th = { textAlign: 'left', padding: '6px 16px', borderBottom: '1px solid #e5e7eb' };
const td = { padding: '6px 16px', borderBottom: '1px solid #f0f0f0' };
