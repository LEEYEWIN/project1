import { useState } from 'react';
import { uploadImage } from '../../api/communityApi.js';
import { errorMessage } from '../../api/client.js';

const MAX_MB = 5;

/**
 * 사진 1장 올리기 (후기 글쓰기·관리자 관광지 편집 공용)
 * value: 올린 사진 주소(없으면 null), onChange(주소 | null)
 * 서버 /api/community/images 에 저장 → 받은 주소를 그대로 쓴다.
 */
export default function ImagePicker({ value, onChange, label = '사진 고르기' }) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const pick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // 같은 파일을 다시 골라도 동작하게
    if (!file) return;
    if (file.size > MAX_MB * 1024 * 1024) {
      setError(`사진은 ${MAX_MB}MB 이하만 올릴 수 있습니다.`);
      return;
    }
    setUploading(true);
    setError('');
    try {
      const { imageUrl } = await uploadImage(file);
      onChange(imageUrl);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="image-picker">
      {value ? (
        <div className="image-picker-preview">
          <img src={value} alt="첨부한 사진 미리보기" />
          <button type="button" className="btn small ghost" onClick={() => onChange(null)}>
            사진 빼기
          </button>
        </div>
      ) : (
        <label className="image-picker-btn">
          <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" onChange={pick} className="sr-only" />
          {uploading ? '올리는 중…' : `📷 ${label}`}
        </label>
      )}
      <small className="muted">JPG·PNG·GIF·WEBP, {MAX_MB}MB 이하 1장</small>
      {error && (
        <p className="error-text" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}