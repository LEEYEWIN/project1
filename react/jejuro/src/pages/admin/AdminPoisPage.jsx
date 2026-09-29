import { useCallback, useEffect, useState } from 'react';
import { fetchAdminPois, fetchPoiOptions } from '../../api/adminApi.js';
import { errorMessage } from '../../api/client.js';
import Loading from '../../components/common/Loading.jsx';
import { Forbidden, Pager } from '../../components/admin/AdminCommon.jsx';
import PoiEditor from '../../components/admin/PoiEditor.jsx';

/** 데이터 점검 카드 (누르면 그 문제만 보기) */
const CHECKS = [
  { key: 'noAiName', issue: 'NO_AI', label: 'AI 이름 없음', hint: 'AI 추천에 절대 안 나와요' },
  { key: 'noImage', issue: 'NO_IMAGE', label: '사진 없음', hint: '카드에 기본 그림' },
  { key: 'noDescription', issue: 'NO_DESC', label: '소개 없음', hint: '카드 설명이 비어요' },
  { key: 'outOfJeju', issue: 'OUT_OF_JEJU', label: '좌표 오류', hint: '지도·동선 계산이 틀려요' },
];

const VIS = [
  { value: 'ALL', label: '전체' },
  { value: 'VISIBLE', label: '보이는 곳' },
  { value: 'HIDDEN', label: '숨긴 곳' },
  { value: 'DELETED', label: '삭제한 곳' },
];

/**
 * 관리자: 관광지 데이터 관리 (/admin/pois)
 * - 데이터 점검(AI 이름 없음·사진 없음·소개 없음·좌표 오류) → 눌러서 해당 관광지만
 * - 검색·권역·분류·보임/숨김 필터, 20곳씩
 * - [수정]·[+ 새 관광지] → 오른쪽 편집 창 (운영 정보, AI 이름 연결, 숨기기)
 */
export default function AdminPoisPage() {
  const [options, setOptions] = useState({ regions: [], categories: [] });
  const [filters, setFilters] = useState({ keyword: '', regionId: '', category: '', visibility: 'ALL', issue: '' });
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [editing, setEditing] = useState(null); // null | 'new' | poiId
  const [error, setError] = useState('');
  const [forbidden, setForbidden] = useState(false);

  const onFail = useCallback((e) => {
    if (e?.response?.status === 403) setForbidden(true);
    else setError(errorMessage(e));
  }, []);

  useEffect(() => {
    fetchPoiOptions().then(setOptions).catch(onFail);
  }, [onFail]);

  const load = useCallback(() => {
    setError('');
    return fetchAdminPois({ ...filters, page }).then(setData).catch(onFail);
  }, [filters, page, onFail]);

  useEffect(() => {
    load();
  }, [load]);

  if (forbidden) return <Forbidden />;

  const set = (patch) => {
    setFilters((f) => ({ ...f, ...patch }));
    setPage(0);
  };

  return (
    <div className="adm-page">
      <header className="adm-header">
        <div>
          <h1>관광지 데이터</h1>
          <p className="adm-muted">
            이름·사진·설명·분류·권역·위치를 고치고 새로 추가하거나 삭제합니다. 삭제한 곳은 여행 장소·경로·후기에 "확인 불가"로 표시돼요.
          </p>
        </div>
        <button type="button" className="adm-btn dark" onClick={() => setEditing('new')}>
          + 새 관광지
        </button>
      </header>

      {data && (
        <div className="adm-checks">
          <button type="button" className={!filters.issue ? 'on' : ''} onClick={() => set({ issue: '' })}>
            <span>보이는 관광지</span>
            <b className="num">{data.checks.total}</b>
            <small>
              숨긴 곳 {data.checks.hidden} · 삭제 {data.checks.deleted}
            </small>
          </button>
          {CHECKS.map((c) => (
            <button
              key={c.key}
              type="button"
              className={`${filters.issue === c.issue ? 'on' : ''} ${data.checks[c.key] > 0 ? 'bad' : ''}`}
              onClick={() => set({ issue: filters.issue === c.issue ? '' : c.issue })}
            >
              <span>{c.label}</span>
              <b className="num">{data.checks[c.key]}</b>
              <small>{c.hint}</small>
            </button>
          ))}
        </div>
      )}

      <form
        className="adm-filters"
        onSubmit={(e) => {
          e.preventDefault();
          set({ keyword });
        }}
      >
        <input
          className="adm-input"
          value={keyword}
          placeholder="이름·주소·번호"
          aria-label="관광지 검색"
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button type="submit">검색</button>
        <label>
          권역
          <select value={filters.regionId} onChange={(e) => set({ regionId: e.target.value })}>
            <option value="">전체</option>
            {options.regions.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          분류
          <select value={filters.category} onChange={(e) => set({ category: e.target.value })}>
            <option value="">전체</option>
            {options.categories.map((c) => (
              <option key={c.value} value={c.value}>
                {c.label}
              </option>
            ))}
          </select>
        </label>
        {VIS.map((v) => (
          <button
            key={v.value}
            type="button"
            className={filters.visibility === v.value ? 'on' : ''}
            onClick={() => set({ visibility: v.value })}
          >
            {v.label}
          </button>
        ))}
      </form>

      {error && <p className="adm-error">{error}</p>}

      <div className={editing ? 'adm-split wide' : ''}>
        <section className="adm-card">
          {!data ? (
            <Loading />
          ) : data.items.length === 0 ? (
            <p className="adm-empty">조건에 맞는 관광지가 없어요.</p>
          ) : (
            <>
              <p className="adm-muted">{data.totalCount}곳</p>
              <div className="adm-scroll">
                <table className="adm-tbl">
                  <thead>
                    <tr>
                      <th>관광지</th>
                      <th>분류 · 권역</th>
                      <th>점검</th>
                      <th className="r">담김</th>
                      <th className="r">추천</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {data.items.map((p) => (
                      <tr key={p.poiId} className={editing === p.poiId ? 'sel' : p.hidden || p.deleted ? 'dim' : ''}>
                        <td>
                          <div className="adm-poi-cell">
                            {p.imageUrl ? <img src={p.imageUrl} alt="" loading="lazy" /> : <span className="adm-noimg">사진 없음</span>}
                            <div>
                              <b>{p.poiName}</b> <span className="adm-muted">#{p.poiId}</span>
                              {p.deleted && <span className="adm-badge bad">삭제됨</span>}
                              {!p.deleted && p.hidden && <span className="adm-badge">숨김</span>}
                              <br />
                              <span className="adm-muted">{p.address}</span>
                            </div>
                          </div>
                        </td>
                        <td>
                          {p.categoryName}
                          <br />
                          <span className="adm-muted">{p.regionName}</span>
                        </td>
                        <td>
                          <div className="adm-badges">
                            {p.noAiName && <span className="adm-badge bad">AI 이름 없음</span>}
                            {p.outOfJeju && <span className="adm-badge bad">좌표 오류</span>}
                            {p.noImage && <span className="adm-badge warn">사진 없음</span>}
                            {p.noDescription && <span className="adm-badge warn">소개 없음</span>}
                            {!p.noAiName && !p.outOfJeju && !p.noImage && !p.noDescription && (
                              <span className="adm-badge ok">정상</span>
                            )}
                          </div>
                        </td>
                        <td className="r num">{p.bookmarkCount}</td>
                        <td className="r num">{p.recommendCount}</td>
                        <td className="r">
                          <button type="button" className="adm-btn small" onClick={() => setEditing(p.poiId)}>
                            수정
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
          {data && <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />}
        </section>

        {editing && (
          <PoiEditor
            key={editing}
            poiId={editing === 'new' ? null : editing}
            options={options}
            onClose={() => setEditing(null)}
            onSaved={(id) => {
              if (id) setEditing(id);
              load();
            }}
          />
        )}
      </div>
    </div>
  );
}