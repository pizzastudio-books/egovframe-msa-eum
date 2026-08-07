import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useApplication, useTransition } from '@/hooks/useApplications';
import { downloadFile } from '@/api/files';
import StatusBadge from '@/components/StatusBadge';
import FileUpload from '@/components/FileUpload';
import { useAuthStore } from '@/store/authStore';
import { formatMoney, formatDate } from '@/lib/format';

// 신청 상세. 신청자는 진행 타임라인을, 심사자는 가능한 처리 버튼을 본다.
// 처리 한 건이 목록·상세·KPI를 한 번에 갱신한다(useTransition의 invalidate).
const ACTION_LABEL = {
  START_REVIEW: '서류검토 시작',
  REQUEST_SUPPLEMENT: '보완요청',
  SUBMIT_SUPPLEMENT: '보완 제출',
  RESUME_REVIEW: '검토 재개',
  APPROVE: '승인',
  REJECT: '반려',
  GRANT: '교부',
};
const NEEDS_COMMENT = new Set(['REQUEST_SUPPLEMENT', 'REJECT']);

export default function ApplicationDetailPage() {
  const { applicationId } = useParams();
  const { data, isLoading, isError } = useApplication(Number(applicationId));
  const transition = useTransition(Number(applicationId));
  const role = useAuthStore((s) => s.user?.role);
  const [comment, setComment] = useState('');
  const [actionError, setActionError] = useState('');

  if (isLoading) return <p>불러오는 중…</p>;
  if (isError || !data) return <p role="alert">신청 정보를 불러오지 못했습니다.</p>;

  const runAction = async (action) => {
    setActionError('');
    if (NEEDS_COMMENT.has(action) && !comment.trim()) {
      setActionError('처리 사유(코멘트)를 입력해 주세요.');
      return;
    }
    try {
      await transition.mutateAsync({ action, comment: comment.trim() || null });
      setComment('');
    } catch (err) {
      setActionError(err.message);
    }
  };

  return (
    <section style={{ maxWidth: 760 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <h1 style={{ margin: 0 }}>{data.programName}</h1>
        <StatusBadge status={data.status} label={data.statusLabel} />
      </div>

      <dl style={infoGrid}>
        <Info label="상호명" value={data.bizName} />
        <Info label="대표자" value={data.ownerName} />
        <Info label="사업자번호" value={data.bizNo} />
        <Info label="신청 금액" value={formatMoney(data.requestAmount)} />
        <Info label="신청일" value={formatDate(data.createdAt)} />
        {data.rejectReason && <Info label="반려 사유" value={data.rejectReason} />}
      </dl>

      <h2 style={h2}>증빙서류</h2>
      {data.files.length === 0 ? (
        <p style={{ color: '#6b7280' }}>첨부된 서류가 없습니다.</p>
      ) : (
        <ul>
          {data.files.map((f) => (
            <li key={f.fileId}>
              {f.origName}{' '}
              <button type="button" onClick={() => downloadFile(f.fileId, f.origName)} style={linkBtn}>
                내려받기
              </button>
            </li>
          ))}
        </ul>
      )}
      {role === 'APPLICANT' && <FileUpload applicationId={Number(applicationId)} />}

      <h2 style={h2}>진행 상황</h2>
      <ol style={{ listStyle: 'none', padding: 0, borderLeft: '2px solid #e5e7eb', marginLeft: 8 }}>
        {data.histories.map((h) => (
          <li key={h.historyId} style={{ padding: '6px 0 6px 16px', position: 'relative' }}>
            <span aria-hidden style={dot} />
            <strong>{labelOfStatus(h.toStatus)}</strong>
            <span style={{ color: '#9ca3af', fontSize: 13, marginLeft: 8 }}>{formatDate(h.createdAt)}</span>
            {h.comment && <div style={{ color: '#374151', fontSize: 14 }}>{h.comment}</div>}
          </li>
        ))}
        {data.histories.length === 0 && <li style={{ paddingLeft: 16 }}>접수 완료. 심사를 기다리고 있습니다.</li>}
      </ol>

      {data.availableActions.length > 0 && (
        <div style={actionBox}>
          <h2 style={{ ...h2, marginTop: 0 }}>처리</h2>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="반려·보완요청 시 사유를 입력하세요."
            rows={2}
            style={{ width: '100%', padding: 8, borderRadius: 6, border: '1px solid #d1d5db' }}
            aria-label="처리 코멘트"
          />
          {actionError && <p role="alert" style={{ color: '#dc2626', fontSize: 13 }}>{actionError}</p>}
          <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
            {data.availableActions.map((action) => (
              <button
                key={action}
                type="button"
                disabled={transition.isPending}
                onClick={() => runAction(action)}
                style={actionBtn(action)}
              >
                {ACTION_LABEL[action] ?? action}
              </button>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function Info({ label, value }) {
  return (
    <div>
      <dt style={{ color: '#9ca3af', fontSize: 13 }}>{label}</dt>
      <dd style={{ margin: 0, fontSize: 15 }}>{value}</dd>
    </div>
  );
}

function labelOfStatus(status) {
  return {
    RECEIVED: '접수', UNDER_REVIEW: '서류검토', SUPPLEMENT_REQUESTED: '보완요청',
    SUPPLEMENTED: '보완제출', APPROVED: '승인', REJECTED: '반려', GRANTED: '교부완료',
  }[status] ?? status;
}

const infoGrid = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 12, margin: '16px 0', background: '#fff', padding: 16, borderRadius: 10 };
const h2 = { fontSize: 18, marginTop: 24 };
const dot = { position: 'absolute', left: -7, top: 12, width: 10, height: 10, borderRadius: 5, background: '#1d4ed8' };
const actionBox = { marginTop: 24, padding: 16, background: '#f9fafb', borderRadius: 10, border: '1px solid #e5e7eb' };
const linkBtn = { background: 'none', border: 0, color: '#1d4ed8', cursor: 'pointer', textDecoration: 'underline' };
function actionBtn(action) {
  const bg = action === 'REJECT' ? '#dc2626' : action === 'APPROVE' || action === 'GRANT' ? '#059669' : '#1d4ed8';
  return { padding: '8px 16px', border: 0, borderRadius: 8, background: bg, color: '#fff', fontWeight: 600, cursor: 'pointer' };
}
