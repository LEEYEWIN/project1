-- 닉네임 중복 방지 (이미 DB를 만든 PC에서 한 번만 실행)
-- 새로 만드는 DB는 jejuro_user.sql 에 같은 제약이 들어 있으므로 실행하지 않는다.
-- 콜레이션 utf8mb4_0900_ai_ci 는 대소문자를 구분하지 않으므로 'Jeju' 와 'jeju' 도 같은 닉네임으로 막힌다.

-- 1) 먼저 겹치는 닉네임이 있는지 확인한다. 결과가 나오면 해당 회원의 닉네임을 바꾼 뒤 2)를 실행한다.
SELECT nickname, COUNT(*) AS cnt, GROUP_CONCAT(user_id) AS user_ids
  FROM `user`
 GROUP BY nickname
HAVING COUNT(*) > 1;

-- 2) UNIQUE 제약 추가
ALTER TABLE `user` ADD UNIQUE KEY `uk_user_nickname` (`nickname`);
