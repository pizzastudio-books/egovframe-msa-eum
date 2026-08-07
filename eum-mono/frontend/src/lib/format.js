// 표시용 포맷 도우미.

export function formatMoney(amount) {
  if (amount == null) return '-';
  return `${Number(amount).toLocaleString('ko-KR')}원`;
}

export function formatDate(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// 신청 상태별 색(배지·타임라인). 색만으로 구분하지 않도록 항상 라벨 텍스트를 함께 쓴다(접근성).
export const STATUS_COLORS = {
  RECEIVED: '#4b5563',
  UNDER_REVIEW: '#2563eb',
  SUPPLEMENT_REQUESTED: '#d97706',
  SUPPLEMENTED: '#7c3aed',
  APPROVED: '#059669',
  REJECTED: '#dc2626',
  GRANTED: '#0f766e',
};
