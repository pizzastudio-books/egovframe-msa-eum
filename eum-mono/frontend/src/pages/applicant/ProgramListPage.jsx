import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchPrograms } from '@/api/programs';
import { useCommonCodes } from '@/hooks/useCommonCodes';
import { formatMoney, formatDate } from '@/lib/format';

// 지원사업 안내. 사업유형 필터 + 카드 목록. 카드에서 신청 화면으로 이동한다.
export default function ProgramListPage() {
  const [type, setType] = useState('');
  const { data: types } = useCommonCodes('support-category');
  const { data: programs, isLoading } = useQuery({
    queryKey: ['programs', type],
    queryFn: () => fetchPrograms(type || undefined),
  });

  return (
    <section>
      <h1>지원사업 안내</h1>
      <div style={{ margin: '16px 0' }}>
        <label htmlFor="ptype" style={{ marginRight: 8, fontWeight: 600 }}>사업유형</label>
        <select id="ptype" value={type} onChange={(e) => setType(e.target.value)}>
          <option value="">전체</option>
          {types?.map((t) => (
            <option key={t.code} value={t.code}>{t.codeName}</option>
          ))}
        </select>
      </div>

      {isLoading && <p>불러오는 중…</p>}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 16 }}>
        {programs?.map((p) => (
          <article key={p.programId} style={cardStyle}>
            <h2 style={{ fontSize: 18, margin: '0 0 8px' }}>{p.programName}</h2>
            <dl style={{ margin: 0, fontSize: 14, color: '#374151', lineHeight: 1.9 }}>
              <div><dt style={dt}>지원 한도</dt><dd style={dd}>{formatMoney(p.supportLimit)}</dd></div>
              <div><dt style={dt}>접수 기간</dt><dd style={dd}>{formatDate(p.applyStart)} ~ {formatDate(p.applyEnd)}</dd></div>
            </dl>
            <p style={{ fontSize: 14, color: '#6b7280' }}>{p.description}</p>
            <Link to={`/apply?programId=${p.programId}`} style={applyBtn}>이 사업에 신청하기</Link>
          </article>
        ))}
      </div>
    </section>
  );
}

const cardStyle = { border: '1px solid #e5e7eb', borderRadius: 12, padding: 18, background: '#fff' };
const dt = { display: 'inline-block', width: 72, color: '#9ca3af' };
const dd = { display: 'inline', margin: 0 };
const applyBtn = {
  display: 'inline-block', marginTop: 8, padding: '8px 14px', background: '#1d4ed8',
  color: '#fff', borderRadius: 8, textDecoration: 'none', fontSize: 14, fontWeight: 600,
};
