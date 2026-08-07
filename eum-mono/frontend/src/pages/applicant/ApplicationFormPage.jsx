import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { fetchPrograms } from '@/api/programs';
import { useCommonCodes } from '@/hooks/useCommonCodes';
import { useCreateApplication } from '@/hooks/useApplications';

// 지원금 신청 폼. 형식 검증은 Zod로, 한도·중복 같은 업무 검증은 서버가 하고
// 그 오류를 폼 필드에 매핑한다.
const schema = z.object({
  programId: z.coerce.number({ invalid_type_error: '지원사업을 선택해 주세요.' }).int().positive('지원사업을 선택해 주세요.'),
  bizNo: z.string().regex(/^\d{3}-\d{2}-\d{5}$/, '사업자등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)'),
  bizName: z.string().min(1, '상호명을 입력해 주세요.'),
  ownerName: z.string().min(1, '대표자명을 입력해 주세요.'),
  industryCode: z.string().min(1, '업종을 선택해 주세요.'),
  regionCode: z.string().min(1, '사업장 소재지를 선택해 주세요.'),
  requestAmount: z.coerce.number({ invalid_type_error: '신청 금액을 입력해 주세요.' }).positive('신청 금액은 0보다 커야 합니다.'),
  accountNo: z.string().min(1, '입금 계좌를 입력해 주세요.'),
});

export default function ApplicationFormPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [sido, setSido] = useState('');
  const [serverError, setServerError] = useState('');

  const { data: programs } = useQuery({ queryKey: ['programs', ''], queryFn: () => fetchPrograms() });
  const { data: industries } = useCommonCodes('INDUSTRY');
  const { data: sidos } = useCommonCodes('REGION');
  const { data: sigungus } = useCommonCodes('REGION', sido);

  const {
    register, handleSubmit, setError, formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { programId: searchParams.get('programId') ?? '' },
  });

  const createMutation = useCreateApplication();

  const onSubmit = async (values) => {
    setServerError('');
    try {
      const { applicationId } = await createMutation.mutateAsync(values);
      navigate(`/applications/${applicationId}`);
    } catch (err) {
      // 서버 검증 오류(자격 미달·한도 초과·중복 수급)를 폼 필드에 매핑한다.
      if (err.fieldErrors?.length) {
        err.fieldErrors.forEach((fe) => setError(fe.field, { type: 'server', message: fe.message }));
      } else {
        setServerError(err.message);
      }
    }
  };

  return (
    <section style={{ maxWidth: 640 }}>
      <h1>지원금 신청</h1>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <Field label="지원사업" error={errors.programId}>
          <select {...register('programId')} aria-invalid={!!errors.programId}>
            <option value="">선택</option>
            {programs?.map((p) => (
              <option key={p.programId} value={p.programId}>{p.programName}</option>
            ))}
          </select>
        </Field>

        <Field label="사업자등록번호" error={errors.bizNo}>
          <input {...register('bizNo')} placeholder="123-45-67890" aria-invalid={!!errors.bizNo} />
        </Field>

        <Field label="상호명" error={errors.bizName}>
          <input {...register('bizName')} aria-invalid={!!errors.bizName} />
        </Field>

        <Field label="대표자명" error={errors.ownerName}>
          <input {...register('ownerName')} aria-invalid={!!errors.ownerName} />
        </Field>

        <Field label="업종" error={errors.industryCode}>
          <select {...register('industryCode')} aria-invalid={!!errors.industryCode}>
            <option value="">선택</option>
            {industries?.map((c) => (
              <option key={c.code} value={c.code}>{c.codeName}</option>
            ))}
          </select>
        </Field>

        <Field label="사업장 소재지" error={errors.regionCode}>
          <div style={{ display: 'flex', gap: 8 }}>
            <select value={sido} onChange={(e) => setSido(e.target.value)} aria-label="시도">
              <option value="">시·도</option>
              {sidos?.map((c) => (
                <option key={c.code} value={c.code}>{c.codeName}</option>
              ))}
            </select>
            <select {...register('regionCode')} aria-label="시군구" aria-invalid={!!errors.regionCode}>
              <option value="">시·군·구</option>
              {sigungus?.map((c) => (
                <option key={c.code} value={c.code}>{c.codeName}</option>
              ))}
            </select>
          </div>
        </Field>

        <Field label="신청 금액(원)" error={errors.requestAmount}>
          <input type="number" {...register('requestAmount')} aria-invalid={!!errors.requestAmount} />
        </Field>

        <Field label="입금 계좌" error={errors.accountNo}>
          <input {...register('accountNo')} placeholder="은행/계좌번호" aria-invalid={!!errors.accountNo} />
        </Field>

        {serverError && <p role="alert" style={{ color: '#dc2626' }}>{serverError}</p>}
        <button type="submit" disabled={isSubmitting} style={submitStyle}>
          {isSubmitting ? '신청 중…' : '신청서 제출'}
        </button>
      </form>
    </section>
  );
}

function Field({ label, error, children }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <label style={{ display: 'block', fontWeight: 600, marginBottom: 4 }}>{label}</label>
      {children}
      {error && (
        <p role="alert" style={{ color: '#dc2626', fontSize: 13, margin: '4px 0 0' }}>
          {error.message}
        </p>
      )}
    </div>
  );
}

const submitStyle = {
  marginTop: 8, padding: '12px 20px', border: 0, borderRadius: 8,
  background: '#1d4ed8', color: '#fff', fontSize: 16, fontWeight: 600, cursor: 'pointer',
};
