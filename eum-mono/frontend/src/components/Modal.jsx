import { useEffect, useRef } from 'react';

// 접근성 있는 모달(공통 UI 키트). 다음을 지킨다.
// - role="dialog" + aria-modal로 보조기기에 모달임을 알린다.
// - 열릴 때 포커스를 모달 안으로 옮기고, Esc로 닫는다.
// - 제목을 aria-labelledby로 연결한다.
export default function Modal({ open, title, onClose, children }) {
  const dialogRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const previouslyFocused = document.activeElement;
    dialogRef.current?.focus();
    const onKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      if (previouslyFocused instanceof HTMLElement) previouslyFocused.focus();
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div style={overlay} onClick={onClose}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        tabIndex={-1}
        style={panel}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 id="modal-title" style={{ margin: 0, fontSize: 18 }}>{title}</h2>
          <button type="button" onClick={onClose} aria-label="닫기" style={closeBtn}>×</button>
        </div>
        <div style={{ marginTop: 12 }}>{children}</div>
      </div>
    </div>
  );
}

const overlay = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
};
const panel = {
  background: '#fff', borderRadius: 12, padding: 20, minWidth: 320, maxWidth: 480, width: '90%',
};
const closeBtn = { border: 0, background: 'none', fontSize: 24, lineHeight: 1, cursor: 'pointer' };
