-- 로그인 기능 대신 쓰는 테스트 회원 3명
-- jeju_schema.sql 실행 후, 같은 스키마에서 한 번만 실행
-- 화면 오른쪽 위 "테스트 회원" 선택 상자로 바꿔 가며 테스트한다.
INSERT INTO `USER` (`user_id`, `email`, `password_hash`, `nickname`, `birth_date`, `gender_code`) VALUES
    (1, 'test1@example.com', 'not-a-real-hash', '테스트1 (20대 여자)', '2000-05-14', 2),
    (2, 'test2@example.com', 'not-a-real-hash', '테스트2 (40대 남자)', '1984-03-02', 1),
    (3, 'test3@example.com', 'not-a-real-hash', '테스트3 (60대 여자)', '1962-11-20', 2);