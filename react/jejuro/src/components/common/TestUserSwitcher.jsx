import { useEffect, useState } from 'react';
import { fetchTestUsers } from '../../api/testUserApi.js';
import { getTestUserId, setTestUserId } from '../../utils/testUser.js';

/**
 * 로그인 대신 쓰는 "테스트 회원" 선택 상자 (상단 메뉴 오른쪽).
 * 바꾸면 localStorage에 저장하고 내 여행 목록으로 새로 불러온다 → 이후 모든 요청이 그 회원으로 처리된다.
 */
export default function TestUserSwitcher() {
  const [users, setUsers] = useState([]);
  const current = getTestUserId();

  useEffect(() => {
    fetchTestUsers()
      .then(setUsers)
      .catch(() => setUsers([])); // 서버에서 테스트 로그인을 끈 경우: 상자를 숨김
  }, []);

  if (users.length === 0) return null;

  const change = (e) => {
    setTestUserId(e.target.value);
    window.location.href = '/travels'; // 화면 전체를 새 회원 기준으로 다시 불러오기
  };

  return (
    <label className="test-user">
      테스트 회원
      <select value={current} onChange={change}>
        {users.map((u) => (
          <option key={u.userId} value={String(u.userId)}>
            {u.userId}. {u.nickname}
          </option>
        ))}
      </select>
    </label>
  );
}