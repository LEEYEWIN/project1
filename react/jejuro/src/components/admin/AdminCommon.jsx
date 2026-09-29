/** 관리자 화면 공통 조각 */

/** 403일 때 */
export function Forbidden() {
  return (
    <div className="adm-empty-page">
      <h1>관리자만 볼 수 있어요</h1>
      <p>왼쪽 메뉴 아래 "테스트 회원"을 관리자 계정(1번)으로 바꾼 뒤 다시 열어 주세요.</p>
    </div>
  );
}

/** 이전/다음 페이지 (page는 0부터) */
export function Pager({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <nav className="adm-pager" aria-label="페이지">
      <button type="button" className="adm-btn" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        ‹ 이전
      </button>
      <span className="num">
        {page + 1} / {totalPages}
      </span>
      <button type="button" className="adm-btn" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        다음 ›
      </button>
    </nav>
  );
}

/** "2026-09-29T14:03:00" → "2026.09.29 14:03" */
export function dt(iso) {
  if (!iso) return '-';
  const [d, t = ''] = String(iso).split('T');
  return `${d.replaceAll('-', '.')} ${t.slice(0, 5)}`.trim();
}
