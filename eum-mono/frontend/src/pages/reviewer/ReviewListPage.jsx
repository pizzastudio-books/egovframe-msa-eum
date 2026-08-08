import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createColumnHelper, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';
import { useApplicationSearch } from '@/hooks/useApplications';
import { useCommonCodes } from '@/hooks/useCommonCodes';
import StatusBadge from '@/components/StatusBadge';
import { formatMoney, formatDate } from '@/lib/format';

// 접수 목록 그리드. 서버 페이징·정렬·필터. TanStack Table로 컬럼을 정의하고,
// 페이징·정렬은 서버에 맡긴다(manual). 검색 조건은 한 객체로 관리한다.
const col = createColumnHelper();

export default function ReviewListPage() {
  const navigate = useNavigate();
  const [condition, setCondition] = useState({
    status: '', programType: '', keyword: '', page: 0, size: 20, sort: 'createdAt', direction: 'desc',
  });
  const { data: statusCodes } = useCommonCodes('application-status');
  const { data: typeCodes } = useCommonCodes('support-category');
  const { data, isLoading } = useApplicationSearch(condition);

  const rows = data?.data ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  const columns = useMemo(() => [
    col.accessor('bizName', { header: '상호명' }),
    col.accessor('programName', { header: '지원사업' }),
    col.accessor('requestAmount', {
      header: '신청 금액',
      cell: (c) => <span style={{ textAlign: 'right', display: 'block' }}>{formatMoney(c.getValue())}</span>,
    }),
    col.accessor('status', {
      header: '상태',
      cell: (c) => <StatusBadge status={c.getValue()} label={c.row.original.statusLabel} />,
    }),
    col.accessor('createdAt', { header: '신청일', cell: (c) => formatDate(c.getValue()) }),
  ], []);

  const table = useReactTable({
    data: rows, columns, getCoreRowModel: getCoreRowModel(),
    manualPagination: true, manualSorting: true,
  });

  const setFilter = (patch) => setCondition((c) => ({ ...c, ...patch, page: 0 }));
  const toggleSort = (columnId) => {
    const map = { bizName: 'bizName', requestAmount: 'requestAmount', status: 'status', createdAt: 'createdAt' };
    const sort = map[columnId];
    if (!sort) return;
    setCondition((c) => ({
      ...c, sort,
      direction: c.sort === sort && c.direction === 'desc' ? 'asc' : 'desc',
    }));
  };

  return (
    <section>
      <h1>접수 목록</h1>

      <div style={filterBar}>
        <select value={condition.status} onChange={(e) => setFilter({ status: e.target.value })} aria-label="상태 필터">
          <option value="">전체 상태</option>
          {statusCodes?.map((s) => <option key={s.code} value={s.code}>{s.codeName}</option>)}
        </select>
        <select value={condition.programType} onChange={(e) => setFilter({ programType: e.target.value })} aria-label="사업유형 필터">
          <option value="">전체 사업유형</option>
          {typeCodes?.map((t) => <option key={t.code} value={t.code}>{t.codeName}</option>)}
        </select>
        <input
          value={condition.keyword}
          onChange={(e) => setFilter({ keyword: e.target.value })}
          placeholder="상호명·대표자 검색"
          aria-label="검색어"
          style={{ padding: '6px 10px', border: '1px solid #d1d5db', borderRadius: 6 }}
        />
      </div>

      <p style={{ fontSize: 14, color: '#6b7280' }}>총 {totalElements.toLocaleString('ko-KR')}건</p>

      <table style={tableStyle}>
        <thead>
          {table.getHeaderGroups().map((hg) => (
            <tr key={hg.id}>
              {hg.headers.map((header) => (
                <th
                  key={header.id}
                  scope="col"
                  onClick={() => toggleSort(header.column.id)}
                  style={{ ...thStyle, cursor: 'pointer' }}
                >
                  {flexRender(header.column.columnDef.header, header.getContext())}
                  {condition.sort === header.column.id && (condition.direction === 'desc' ? ' ▼' : ' ▲')}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {isLoading ? (
            <tr><td colSpan={columns.length} style={{ padding: 24, textAlign: 'center' }}>불러오는 중…</td></tr>
          ) : rows.length === 0 ? (
            <tr><td colSpan={columns.length} style={{ padding: 24, textAlign: 'center' }}>조건에 맞는 신청이 없습니다.</td></tr>
          ) : (
            table.getRowModel().rows.map((row) => (
              <tr
                key={row.id}
                onClick={() => navigate(`/applications/${row.original.applicationId}`)}
                style={{ cursor: 'pointer' }}
              >
                {row.getVisibleCells().map((cell) => (
                  <td key={cell.id} style={tdStyle}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 16 }}>
        <button type="button" disabled={condition.page <= 0} onClick={() => setCondition((c) => ({ ...c, page: c.page - 1 }))}>
          이전
        </button>
        <span style={{ fontSize: 14 }}>{condition.page + 1} / {Math.max(totalPages, 1)}</span>
        <button type="button" disabled={condition.page + 1 >= totalPages} onClick={() => setCondition((c) => ({ ...c, page: c.page + 1 }))}>
          다음
        </button>
      </div>
    </section>
  );
}

const filterBar = { display: 'flex', gap: 8, margin: '16px 0', flexWrap: 'wrap' };
const tableStyle = { width: '100%', borderCollapse: 'collapse', background: '#fff', fontSize: 14 };
const thStyle = { textAlign: 'left', padding: '10px 12px', borderBottom: '2px solid #e5e7eb', background: '#f9fafb' };
const tdStyle = { padding: '10px 12px', borderBottom: '1px solid #f0f0f0' };
