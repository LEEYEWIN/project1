import { useCallback, useEffect, useState } from 'react';
import {
  addPoiMapping,
  createPoi,
  deletePoi,
  fetchAdminPoi,
  removePoiMapping,
  restorePoi,
  setPoiHidden,
  updatePoi,
} from '../../api/adminApi.js';
import ImagePicker from '../common/ImagePicker.jsx';
import MapPicker from './MapPicker.jsx';
import { errorMessage } from '../../api/client.js';
import Loading from '../common/Loading.jsx';
import { dt } from './AdminCommon.jsx';

const EMPTY = {
  poiName: '',
  address: '',
  latitude: '',
  longitude: '',
  categoryCode: '',
  regionId: '',
  description: '',
  detailDescription: '',
  imageUrl: '',
};

/**
 * 관리자 관광지 편집 창 (오른쪽)
 * poiId = null 이면 새 관광지(= 직접 선택만, AI 추천 대상 아님). 저장하면 편집 모드로 바뀐다.
 */
export default function PoiEditor({ poiId, options, onClose, onSaved }) {
  const [form, setForm] = useState(poiId ? null : EMPTY);
  const [detail, setDetail] = useState(null);
  const [mapName, setMapName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState('');

  const load = useCallback(async () => {
    if (!poiId) return;
    const d = await fetchAdminPoi(poiId);
    setDetail(d);
    const next = {};
    Object.keys(EMPTY).forEach((k) => {
      next[k] = d[k] == null ? '' : String(d[k]);
    });
    setForm(next);
  }, [poiId]);

  useEffect(() => {
    load().catch((e) => setError(errorMessage(e)));
  }, [load]);

  const run = async (fn, doneMsg) => {
    setBusy(true);
    setError('');
    setSaved('');
    try {
      await fn();
      if (doneMsg) setSaved(doneMsg);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (!form) {
    return (
      <aside className="adm-card adm-panel">
        {error ? <p className="adm-error">{error}</p> : <Loading />}
      </aside>
    );
  }

  const change = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const save = (e) => {
    e.preventDefault();
    const payload = {
      ...form,
      latitude: form.latitude.trim(),
      longitude: form.longitude.trim(),
      regionId: form.regionId ? Number(form.regionId) : null,
    };
    run(async () => {
      if (poiId) {
        await updatePoi(poiId, payload);
        await load();
        onSaved();
      } else {
        const { poiId: newId } = await createPoi(payload);
        onSaved(newId); // 편집 모드로 바꿈
      }
    }, poiId ? '저장했어요.' : '');
  };

  const toggleHidden = () => {
    const hide = !detail.hiddenAt;
    const msg = hide
      ? `숨기면 관광지 검색·AI 추천·새로 담기에서 빠져요.\n이미 담긴 여행 ${detail.usage.bookmarks}건과 확정 일정은 그대로 둡니다. 숨길까요?`
      : '다시 보이게 할까요?';
    if (!window.confirm(msg)) return;
    run(async () => {
      await setPoiHidden(poiId, hide);
      await load();
      onSaved();
    }, hide ? '숨겼어요.' : '다시 보이게 했어요.');
  };

  /** 삭제: 행은 남기고 여행 장소·경로·후기에는 "확인 불가"로 표시. 검색·추천·담기에서 빠짐 */
  const toggleDeleted = () => {
    const del = !detail.deletedAt;
    const msg = del
      ? `이 관광지를 삭제할까요?\n· 관광지 검색·AI 추천·새로 담기에서 빠져요\n· 이미 담긴 여행 ${detail.usage.bookmarks}건·경로 ${detail.usage.routeSpots}건·후기 ${detail.usage.feedbackSpots}건에는 "확인 불가"로 표시돼요\n· 삭제 취소로 되돌릴 수 있어요`
      : '삭제를 취소하고 다시 쓸 수 있게 할까요?';
    if (!window.confirm(msg)) return;
    run(async () => {
      if (del) await deletePoi(poiId);
      else await restorePoi(poiId);
      await load();
      onSaved();
    }, del ? '삭제했어요.' : '복구했어요.');
  };

  const addMap = (e) => {
    e.preventDefault();
    if (!mapName.trim()) return;
    run(async () => {
      await addPoiMapping(poiId, mapName.trim());
      setMapName('');
      await load();
      onSaved();
    });
  };

  const removeMap = (name) => {
    if (!window.confirm(`"${name}" 연결을 뺄까요? AI가 이 이름으로 추천하면 더 이상 이 관광지로 연결되지 않아요.`)) return;
    run(async () => {
      await removePoiMapping(poiId, name);
      await load();
      onSaved();
    });
  };



  const aiNames = detail?.mappings.filter((m) => m.ai) ?? [];
  const sourceIds = detail?.mappings.filter((m) => !m.ai) ?? [];

  return (
    <aside className="adm-card adm-panel" aria-label="관광지 편집">
      <div className="adm-card-head">
        <h2>{poiId ? `#${poiId} 수정` : '새 관광지'}</h2>
        <button type="button" className="adm-link" onClick={onClose}>
          닫기 ✕
        </button>
      </div>

      {detail && (
        <p className="adm-muted">
          여행 장소 {detail.usage.bookmarks} · 경로 {detail.usage.routeSpots} · AI 추천 노출 {detail.usage.recommended} · 후기{' '}
          {detail.usage.feedbackSpots}
          {detail.aiRecommend ? (
            <span className="adm-badge ok">AI 추천 대상</span>
          ) : (
            <span className="adm-badge">직접 선택만 (AI 추천 제외)</span>
          )}
          {detail.hiddenAt && <span className="adm-badge">숨김 {dt(detail.hiddenAt)}</span>}
          {detail.deletedAt && <span className="adm-badge bad">삭제됨 {dt(detail.deletedAt)}</span>}
        </p>
      )}

      <form className="adm-form" onSubmit={save}>
        <label>
          이름 *
          <input name="poiName" value={form.poiName} maxLength={200} required onChange={change} />
        </label>
        <label>
          주소 *
          <input name="address" value={form.address} maxLength={500} required onChange={change} />
        </label>
        <div className="adm-form-row">
          <label>
            위도 *
            <input name="latitude" value={form.latitude} inputMode="decimal" placeholder="33.4589" required onChange={change} />
          </label>
          <label>
            경도 *
            <input name="longitude" value={form.longitude} inputMode="decimal" placeholder="126.9425" required onChange={change} />
          </label>
        </div>
        <MapPicker
          lat={form.latitude}
          lng={form.longitude}
          onPick={(latitude, longitude) => setForm((f) => ({ ...f, latitude, longitude }))}
        />
        <div className="adm-form-row">
          <label>
            분류 *
            <select name="categoryCode" value={form.categoryCode} required onChange={change}>
              <option value="">선택</option>
              {options.categories.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            권역 *
            <select name="regionId" value={form.regionId} required onChange={change}>
              <option value="">선택</option>
              {options.regions.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <label>
          한 줄 소개 * <small className="adm-muted">카드에 보여요</small>
          <input name="description" value={form.description} maxLength={1000} required onChange={change} />
        </label>
        <label>
          세부 설명 <small className="adm-muted">상세 화면</small>
          <textarea name="detailDescription" rows={4} value={form.detailDescription} maxLength={10000} onChange={change} />
        </label>
        <div className="adm-form-block">
          <span>사진</span>
          <ImagePicker value={form.imageUrl || null} onChange={(url) => setForm((f) => ({ ...f, imageUrl: url ?? '' }))} label="사진 올리기" />
          <label>
            <small className="adm-muted">또는 사진 주소 직접 입력</small>
            <input name="imageUrl" value={form.imageUrl} maxLength={2048} placeholder="https://" onChange={change} />
          </label>
        </div>
        {error && <p className="adm-error">{error}</p>}
        {saved && (
          <p className="adm-ok" role="status">
            {saved}
          </p>
        )}
        <div className="adm-actions">
          <button type="submit" className="adm-btn dark" disabled={busy}>
            {poiId ? '저장' : '추가'}
          </button>
          {detail && !detail.deletedAt && (
            <button type="button" className="adm-btn" disabled={busy} onClick={toggleHidden}>
              {detail.hiddenAt ? '다시 보이기' : '숨기기'}
            </button>
          )}
          {detail && (
            <button type="button" className={detail.deletedAt ? 'adm-btn' : 'adm-btn danger'} disabled={busy} onClick={toggleDeleted}>
              {detail.deletedAt ? '삭제 취소(복구)' : '삭제'}
            </button>
          )}
        </div>
      </form>

      {detail && (
        <>
          <h3 className="adm-panel-h">AI 추천</h3>
          {!detail.aiRecommend ? (
            <p className="adm-muted">
              AI가 학습하지 않은 관광지라 <b>AI 추천에는 나오지 않고</b>, 회원이 관광지 목록·검색에서 직접 골라 담을 수만 있어요.
              (새로 추가한 관광지도 여기에 해당)
            </p>
          ) : (
            <>
              <p className="adm-muted">
                AI가 학습한 관광지(275곳 중 하나)예요. AI가 추천 결과로 주는 장소 이름(VISIT_AREA_NM)이 아래 이름으로 이 관광지와
                연결됩니다. 연결이 모두 빠지면 AI 추천에 나오지 않아요.
              </p>
              {aiNames.length === 0 ? (
                <p className="adm-badge bad">연결된 AI 이름 없음</p>
              ) : (
                <ul className="adm-chips">
                  {aiNames.map((m) => (
                    <li key={m.sourcePoiId}>
                      {m.sourcePoiId}
                      <button type="button" aria-label={`${m.sourcePoiId} 연결 빼기`} disabled={busy} onClick={() => removeMap(m.sourcePoiId)}>
                        ✕
                      </button>
                    </li>
                  ))}
                </ul>
              )}
              <form className="adm-sanction" onSubmit={addMap}>
                <input
                  className="adm-input"
                  value={mapName}
                  maxLength={255}
                  placeholder="예) 성산일출봉"
                  aria-label="연결할 AI 장소 이름"
                  onChange={(e) => setMapName(e.target.value)}
                />
                <button type="submit" className="adm-btn" disabled={busy}>
                  연결
                </button>
              </form>
            </>
          )}
          {sourceIds.length > 0 && (
            <p className="adm-muted">원본 데이터 ID: {sourceIds.map((m) => m.sourcePoiId).join(', ')}</p>
          )}
        </>
      )}
    </aside>
  );
}
