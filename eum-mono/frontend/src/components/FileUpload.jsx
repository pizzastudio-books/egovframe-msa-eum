import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { uploadFile } from '@/api/files';

// 증빙서류 업로드. 여러 파일을 차례로 올리며 진행률을 보여 준다.
// 업로드가 끝나면 상세 쿼리를 무효화해 첨부 목록이 자동으로 갱신된다.
const DOC_TYPES = [
  { code: 'BIZ_LICENSE', name: '사업자등록증' },
  { code: 'LEASE', name: '임대차계약서' },
  { code: 'SALES', name: '매출 증빙' },
];

export default function FileUpload({ applicationId }) {
  const qc = useQueryClient();
  const inputRef = useRef(null);
  const [docType, setDocType] = useState('BIZ_LICENSE');
  const [progress, setProgress] = useState(null);
  const [error, setError] = useState('');

  const onSelect = async (e) => {
    const files = Array.from(e.target.files ?? []);
    if (files.length === 0) return;
    setError('');
    try {
      for (const file of files) {
        setProgress({ name: file.name, percent: 0 });
        await uploadFile(applicationId, docType, file, (percent) =>
          setProgress({ name: file.name, percent }),
        );
      }
      await qc.invalidateQueries({ queryKey: ['applications', 'detail', applicationId] });
    } catch (err) {
      setError(err.message ?? '업로드에 실패했습니다.');
    } finally {
      setProgress(null);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <label htmlFor="docType" className="sr-only">서류 종류</label>
        <select id="docType" value={docType} onChange={(e) => setDocType(e.target.value)}>
          {DOC_TYPES.map((d) => (
            <option key={d.code} value={d.code}>{d.name}</option>
          ))}
        </select>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept=".pdf,.jpg,.jpeg,.png,.hwp,.hwpx"
          capture="environment"
          onChange={onSelect}
          aria-label="증빙서류 파일 선택"
        />
      </div>
      {progress && (
        <div style={{ marginTop: 8 }} aria-live="polite">
          <span style={{ fontSize: 13 }}>{progress.name} 업로드 {progress.percent}%</span>
          <div style={{ height: 6, background: '#e5e7eb', borderRadius: 3, marginTop: 4 }}>
            <div style={{ width: `${progress.percent}%`, height: '100%', background: '#1d4ed8', borderRadius: 3 }} />
          </div>
        </div>
      )}
      {error && <p role="alert" style={{ color: '#dc2626', fontSize: 13 }}>{error}</p>}
    </div>
  );
}
