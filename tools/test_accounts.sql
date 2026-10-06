-- 캡처·테스트용 계정 만들기 (메일 인증 없이 DB에 직접 추가)
-- 비밀번호: 아래 "내 이메일" 계정과 같은 비밀번호로 로그인됩니다. (그 계정의 암호화된 값을 그대로 복사)
-- 실행 전 '내이메일@gmail.com' 한 곳만 내 실제 가입 이메일로 바꾸세요. 이 파일은 바꾼 채로 커밋하지 마세요.

SET @pw = (SELECT password_hash FROM `user` WHERE email = '내이메일@gmail.com');
SELECT IF(@pw IS NULL, '내 이메일 계정을 찾지 못했습니다. 이메일을 확인하세요.', '비밀번호 복사 준비 완료') AS 확인;

INSERT INTO `user` (email, password_hash, nickname, birth_date, gender_code, status, role)
VALUES
  ('test1@example.com', @pw, '테스트여행자', '1998-05-01', 2, 'ACTIVE', 'USER'),
  ('test2@example.com', @pw, '제주탐험가',   '1995-09-15', 1, 'ACTIVE', 'USER'),
  ('admin@example.com', @pw, '제주로관리자', '1990-01-01', 1, 'ACTIVE', 'ADMIN');

SELECT user_id, email, nickname, role, status FROM `user` WHERE email LIKE '%@example.com';

-- 지울 때 (여행 등 데이터가 없을 때만 바로 지워집니다)
-- DELETE FROM `user` WHERE email IN ('test1@example.com', 'test2@example.com', 'admin@example.com');
