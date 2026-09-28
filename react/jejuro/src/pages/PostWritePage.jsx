import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { createPost, fetchPost, updatePost, uploadImage } from '../api/communityApi.js';
import { fetchMyTravels } from '../api/travelApi.js';
import { errorMessage } from '../api/client.js';
import Loading from '../components/common/Loading.jsx';
import ErrorBox from '../components/common/ErrorBox.jsx';
import '../styles/community.css';

const MAX_IMAGE_MB = 5;

/**
 * 커뮤니티 글쓰기 / 수정 (같은 화면)
 * - /community/posts/new?type=REVIEW   새 글
 * - /community/posts/:postId/edit      수정 (글 종류·첨부 여행은 바꿀 수 없음)
 * - 사진 1장: 고르면 바로 서버에 올리고 받은 주소를 글과 함께 저장
 * - 여행 후기는 "최종 경로 채택 + 여행 후기 작성"을 마친 내 여행을 첨부할 수 있음
 * - 저장에 실패하면 입력 내용은 그대로 두고 오류만 보여 준다
 */
export default function PostWritePage() {
  const { postId } = useParams();
  const editing = Boolean(postId);
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    postType: params.get('type') === 'QUESTION' ? 'QUESTION' : 'REVIEW',
    title: '',
    content: '',
    travelId: '',
    imageUrl: null,
  });
  const [travels, setTravels] = useState([]);
  const [loading, setLoading] = useState(editing);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  // 수정: 기존 글 불러오기 / 새 글: 첨부할 수 있는 내 여행
  useEffect(() => {
    if (editing) {
      fetchPost(postId)
        .then((p) => {
          if (!p.mine) {
            setError('본인이 쓴 글만 수정할 수 있습니다.');
            return;
          }
          setForm({ postType: p.postType, title: p.title, content: p.content, travelId: p.travelId ?? '', imageUrl: p.imageUrl });
        })
        .catch((e) => setError(errorMessage(e)))
        .finally(() => setLoading(false));
    } else {
      fetchMyTravels()
        .then((list) => setTravels(list.filter((t) => t.adoptedRouteId && t.hasFeedback)))
        .catch(() => setTravels([]));
    }
  }, [editing, postId]);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const pickImage = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // 같은 파일을 다시 골라도 동작하게
    if (!file) return;
    if (file.size > MAX_IMAGE_MB * 1024 * 1024) {
      setError(`사진은 ${MAX_IMAGE_MB}MB 이하만 올릴 수 있습니다.`);
      return;
    }
    setUploading(true);
    setError('');
    try {
      const { imageUrl } = await uploadImage(file);
      setForm((f) => ({ ...f, imageUrl }));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!form.title.trim() || !form.content.trim()) {
      setError('제목과 내용을 입력해 주세요.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      if (editing) {
        await updatePost(postId, { title: form.title.trim(), content: form.content.trim(), imageUrl: form.imageUrl });
        navigate(`/community/posts/${postId}`, { replace: true });
      } else {
        const { postId: newId } = await createPost({
          postType: form.postType,
          title: form.title.trim(),
          content: form.content.trim(),
          travelId: form.postType === 'REVIEW' && form.travelId ? Number(form.travelId) : null,
          imageUrl: form.imageUrl,
        });
        navigate(`/community/posts/${newId}`, { replace: true });
      }
    } catch (err) {
      setError(errorMessage(err)); // 입력 내용은 그대로 유지
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <main className="page cm"><Loading /></main>;

  return (
    <main className="page cm">
      <p className="cm-eyebrow">COMMUNITY</p>
      <h1 className="cm-title">{editing ? '글 수정' : '글쓰기'}</h1>

      <form className="cm-form" onSubmit={submit}>
        <fieldset className="cm-field" disabled={editing}>
          <legend>글 종류</legend>
          <div className="cm-tabs">
            {[
              ['REVIEW', '여행 후기'],
              ['QUESTION', '질문'],
            ].map(([value, label]) => (
              <label key={value} className={form.postType === value ? 'cm-pill on' : 'cm-pill'}>
                <input
                  type="radio"
                  name="postType"
                  value={value}
                  checked={form.postType === value}
                  onChange={set('postType')}
                  className="sr-only"
                />
                {label}
              </label>
            ))}
          </div>
        </fieldset>

        {!editing && form.postType === 'REVIEW' && (
          <label className="cm-field">
            첨부할 여행 (선택)
            <select value={form.travelId} onChange={set('travelId')}>
              <option value="">첨부하지 않음</option>
              {travels.map((t) => (
                <option key={t.travelId} value={t.travelId}>
                  {t.travelName} ({t.startDate} ~ {t.endDate})
                </option>
              ))}
            </select>
            <small className="cm-muted">최종 경로를 채택하고 여행 후기를 남긴 여행만 첨부할 수 있어요. 첨부하면 최종 경로가 함께 보여요.</small>
          </label>
        )}

        <label className="cm-field">
          제목
          <input value={form.title} maxLength={200} onChange={set('title')} placeholder="제목을 입력해 주세요" />
        </label>

        <label className="cm-field">
          내용
          <textarea value={form.content} rows={10} maxLength={10000} onChange={set('content')} placeholder="내용을 입력해 주세요" />
        </label>

        <div className="cm-field">
          <span>사진 1장 (선택, JPG·PNG·GIF·WEBP, {MAX_IMAGE_MB}MB 이하)</span>
          {form.imageUrl ? (
            <div className="cm-image-preview">
              <img src={form.imageUrl} alt="첨부한 사진 미리보기" />
              <button type="button" className="cm-btn" onClick={() => setForm((f) => ({ ...f, imageUrl: null }))}>
                사진 빼기
              </button>
            </div>
          ) : (
            <label className="cm-upload">
              <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" onChange={pickImage} className="sr-only" />
              {uploading ? '올리는 중…' : '사진 고르기'}
            </label>
          )}
        </div>

        <ErrorBox message={error} />

        <div className="cm-form-actions">
          <Link className="cm-btn" to={editing ? `/community/posts/${postId}` : '/community'}>
            취소
          </Link>
          <button type="submit" className="cm-btn dark" disabled={saving || uploading}>
            {saving ? '저장 중…' : editing ? '수정하기' : '등록하기'}
          </button>
        </div>
      </form>
    </main>
  );
}