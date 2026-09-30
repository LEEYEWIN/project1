/**
 * 신고 코드 → 화면 이름 (서버 ReportPolicy와 같은 값). 회원 제재(정지·경고)는 없음
 */
export const REPORT_REASONS = [
  { code: 'SEXUAL', label: '음란·불법', hint: '음란물, 불법 촬영물, 불법 거래' },
  { code: 'PRIVACY', label: '개인정보 노출', hint: '전화번호·주소·얼굴 등 남의 개인정보' },
  { code: 'ABUSE', label: '욕설·비방', hint: '욕설, 특정인·지역·집단 비하' },
  { code: 'SPAM', label: '스팸·광고', hint: '홍보 글, 같은 내용 반복, 외부 링크 유도' },
];

/** 예전 기록(기타)도 이름이 보이게 */
export const REASON_LABEL = { ...Object.fromEntries(REPORT_REASONS.map((r) => [r.code, r.label])), OTHER: '기타(예전 기록)' };

/** 신고 처리: 반려(정상 표시) / 차단(사유 알림 후 목록으로). 삭제는 예전 기록 표시용 */
export const ACTION_LABEL = { KEEP: '반려', BLOCK: '차단', DELETE: '삭제' };