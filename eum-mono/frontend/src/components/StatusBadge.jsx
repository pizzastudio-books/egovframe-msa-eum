import { STATUS_COLORS } from '@/lib/format';

// 신청 상태 배지. 색과 함께 반드시 라벨 텍스트를 노출해, 색을 못 보는 사용자도 상태를 안다.
export default function StatusBadge({ status, label }) {
  const color = STATUS_COLORS[status] ?? '#4b5563';
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 12,
        fontSize: 13,
        fontWeight: 600,
        color: '#fff',
        backgroundColor: color,
      }}
    >
      {label ?? status}
    </span>
  );
}
