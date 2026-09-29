/**
 * 신고·제재 코드 → 화면 이름 (서버 ReportPolicy·AdminUserService와 같은 값)
 */
export const REPORT_REASONS = [
  { code: 'SEXUAL', label: '음란·불법', hint: '음란물, 불법 촬영물, 불법 거래' },
  { code: 'PRIVACY', label: '개인정보 노출', hint: '전화번호·주소·얼굴 등 남의 개인정보' },
  { code: 'ABUSE', label: '욕설·비방', hint: '욕설, 특정인·지역·집단 비하' },
  { code: 'SPAM', label: '스팸·광고', hint: '홍보 글, 같은 내용 반복, 외부 링크 유도' },
  { code: 'OTHER', label: '기타', hint: '자세한 내용을 적어 주세요' },
];

export const REASON_LABEL = Object.fromEntries(REPORT_REASONS.map((r) => [r.code, r.label]));

/** 신고 처리: 유지(다시 보이기) / 차단(사유 표시, 작성자 포함 모두 못 봄) / 삭제 */
export const ACTION_LABEL = { KEEP: '유지', BLOCK: '차단', DELETE: '삭제' };

export const SANCTION_LABEL = {
  NONE: '제재 없음',
  WARNING: '경고',
  SUSPEND_7D: '7일 정지',
  SUSPEND_30D: '30일 정지',
  BAN: '영구 정지',
  RELEASE: '정지 해제',
};

/** 정지 기한 → "영구 정지" / "2026.10.07 14:00까지" */
export function suspendText(until) {
  if (!until) return '';
  if (String(until).startsWith('9999')) return '영구 정지';
  const [d, t = ''] = String(until).split('T');
  return `${d.replaceAll('-', '.')} ${t.slice(0, 5)}까지 정지`;
}