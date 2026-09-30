-- 제주 여행 서비스: 새 DB 생성용 DDL + 기초 코드 + 트리거 + 삭제 프로시저 + AI 입력 VIEW
-- 기준: MySQL 8.0.16 이상 / InnoDB / utf8mb4
-- 8.0.16 미만에서는 CHECK가 강제되지 않으므로 이 파일의 검증 규칙을 충족하지 않습니다.
-- 사용: 빈 스키마를 생성·선택한 뒤 전체 실행하십시오. 예: USE 선택한_DB;
--       DELIMITER 구문이 있으므로 mysql CLI 또는 MySQL Workbench로 실행하십시오.
-- DB 이름을 임의 지정하지 않았으며 DROP 문, 기존 데이터 이관은 포함하지 않습니다.
-- 25개 TABLE + 1개 VIEW + 트리거 2개 + 프로시저 2개.
-- 모든 FK는 자식 PK 밖에 두는 비식별 관계입니다. 기존 복합 자연키는 UNIQUE로 보존합니다.
--
-- [이전 버전 대비 변경]
--  1. ADMIN_AREA 삭제
--  2. 회원가입은 이메일로만. 소셜은 가입 후 "로그인 연동"만 → password_hash NOT NULL,
--     SOCIAL_ACCOUNT에 (user_id, provider) UNIQUE 추가
--  3. 탈퇴: 30일 유예(WITHDRAWAL_PENDING) → 배치에서 sp_purge_user로 물리 삭제
--     커뮤니티 글·댓글은 남기고 user_id를 NULL로(익명화)
--  4. EMAIL_VERIFICATION.created_at NOT NULL, 목적에 PASSWORD_RESET 추가
--  5. 채택 경로: 소유권 트리거 삭제 → 복합 FK (travel_id, adopted_route_id)
--  6. TRAVEL_ROUTE.route(요약 문자열) 삭제 → route_name(선택 제목)
--  7. ROUTE_DAY.travel_date 삭제 (날짜 = TRAVEL.start_date + day_no - 1)
--  8. TRAVEL_FEEDBACK_REASON FK를 feedback_id로 변경
--  9. 설문 규칙 하드코딩 제거: 단일선택은 response_type 기반 생성열,
--     최대 개수는 PREFERENCE_GROUP.max_selections를 읽는 범용 트리거,
--     VIEW 완료 여부도 PREFERENCE_GROUP 기준으로 계산
-- 10. 여행 스타일 6개 문항(101~106)은 모델 입력에 맞춰 1~7점 척도 (왼쪽 1 / 오른쪽 7)
-- 11. 여행 동기(201)·테마(202)는 각각 정확히 3개 선택. TRAVEL_PREFERENCE.answer_rank에 고른 순서 저장,
--     1순위를 AI에 TRAVEL_MOTIVE_1 / TRAVEL_MISSION_PRIORITY_WEB로 전달
-- 12. 동반자는 COMPANION 행 단위로 relation/gender/age_group 보존 → AI 서버가 최대 18명 18-slot으로 변환
-- 13. REGION은 EAST/WEST/SOUTH/NORTH. ALL은 TRAVEL_REGION 0행, SELECTED는 1행 이상
-- 14. COMMUNITY_POST.travel_id: 후기에 첨부한 여행(게시판에 최종 경로 표시)
-- 15. 관광지 분류 코드 POI_CAT (VISIT_AREA_TYPE_CD → category_code)
-- 16. POI.detail_description: 세부 설명(한 줄 소개 description과 별도)
-- 17. ACCOMMODATION: 숙소 종류·전화·사진 + 위경도 색인, 코드 ACCOM_TYPE (루트 주변 숙소 FR-26)
-- 18. 커뮤니티: 게시글 사진 1장·조회수·삭제 표시, 대댓글(parent_comment_id), 좋아요(COMMUNITY_POST_LIKE)
--     1 NATURE / 2 HISTORY / 3 CULTURE / 4 COMMERCIAL / 5 LEISURE / 6 THEME / 7 TRAIL / 8 FESTIVAL / 9 EXPERIENCE(원본 13)
SET NAMES utf8mb4;


-- 01. USER
-- 회원가입은 이메일 인증 + 비밀번호로만 한다. 소셜 계정은 가입 후 연동만 한다.
-- 탈퇴 요청 시 status=WITHDRAWAL_PENDING, withdrawn_at=요청 시각. 유예 30일 후 sp_purge_user.
CREATE TABLE `USER` (
    `user_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `birth_date` DATE NOT NULL,
    `gender_code` TINYINT UNSIGNED NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER 일반 / ADMIN 관리자',
    `withdrawn_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_user` PRIMARY KEY (`user_id`),
    CONSTRAINT `uk_user_email` UNIQUE (`email`),
    CONSTRAINT `ck_user_gender_code` CHECK (`gender_code` IN (1, 2)),
    CONSTRAINT `ck_user_role` CHECK (`role` IN ('USER', 'ADMIN')),
    CONSTRAINT `ck_user_status` CHECK (
        (`status` = 'ACTIVE' AND `withdrawn_at` IS NULL)
        OR
        (`status` = 'WITHDRAWAL_PENDING' AND `withdrawn_at` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='서비스 회원. 이메일 가입 전용';

CREATE INDEX `ix_user_status_withdrawn`
    ON `USER` (`status`, `withdrawn_at`);

-- 02. SOCIAL_ACCOUNT
-- 로그인한 회원이 설정 화면에서 연동한다. 소셜 로그인은 (provider, provider_user_id)로 회원을 찾는다.
-- 제공자에게서 이메일을 받지 않으므로 카카오 이메일 동의 항목이 필요 없다.
CREATE TABLE `SOCIAL_ACCOUNT` (
    `social_account_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `provider` VARCHAR(20) NOT NULL,
    `provider_user_id` VARCHAR(255) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_social_account` PRIMARY KEY (`social_account_id`),
    CONSTRAINT `uk_social_provider_user` UNIQUE (`provider`, `provider_user_id`),
    CONSTRAINT `uk_social_user_provider` UNIQUE (`user_id`, `provider`),
    CONSTRAINT `fk_social_account_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `ck_social_provider` CHECK (
        `provider` IN ('KAKAO', 'NAVER', 'GOOGLE')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='기존 회원에 연동한 소셜 로그인 계정';

-- 03. EMAIL_VERIFICATION
CREATE TABLE `EMAIL_VERIFICATION` (
    `verification_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `purpose` VARCHAR(30) NOT NULL,
    `code_verifier` VARCHAR(255) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `failed_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `used_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_email_verification` PRIMARY KEY (`verification_id`),
    CONSTRAINT `ck_email_verification_purpose` CHECK (
        `purpose` IN ('SIGNUP', 'EMAIL_CHANGE', 'PASSWORD_RESET')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='이메일 인증 요청';

CREATE INDEX `ix_email_verification_lookup`
    ON `EMAIL_VERIFICATION` (`email`, `purpose`, `created_at`);

-- 04. AUTH_SESSION
CREATE TABLE `AUTH_SESSION` (
    `session_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `refresh_token_hash` VARCHAR(255) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `revoked_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_auth_session` PRIMARY KEY (`session_id`),
    CONSTRAINT `uk_auth_session_refresh_token` UNIQUE (`refresh_token_hash`),
    CONSTRAINT `fk_auth_session_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Refresh Token 및 로그인 세션';

CREATE INDEX `ix_auth_session_user` ON `AUTH_SESSION` (`user_id`);

-- 05. CODE_GROUP
CREATE TABLE `CODE_GROUP` (
    `group_code` VARCHAR(30) NOT NULL,
    `group_name` VARCHAR(100) NOT NULL,
    CONSTRAINT `pk_code_group` PRIMARY KEY (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='공통 코드 그룹';

-- 06. CODE
CREATE TABLE `CODE` (
    `code_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `group_code` VARCHAR(30) NOT NULL,
    `code_value` VARCHAR(50) NOT NULL,
    `code_name` VARCHAR(100) NOT NULL,
    CONSTRAINT `pk_code` PRIMARY KEY (`code_id`),
    CONSTRAINT `uk_code_natural` UNIQUE (`group_code`, `code_value`),
    CONSTRAINT `fk_code_group`
        FOREIGN KEY (`group_code`) REFERENCES `CODE_GROUP` (`group_code`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='공통 코드 값';

-- 07. REGION
CREATE TABLE `REGION` (
    `region_id` TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `region_code` VARCHAR(20) NOT NULL,
    `region_name` VARCHAR(20) NOT NULL,
    CONSTRAINT `pk_region` PRIMARY KEY (`region_id`),
    CONSTRAINT `uk_region_code` UNIQUE (`region_code`),
    CONSTRAINT `uk_region_name` UNIQUE (`region_name`),
    CONSTRAINT `ck_region_code` CHECK (
        `region_code` IN ('EAST', 'WEST', 'SOUTH', 'NORTH')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='서비스에서 정의한 제주 4개 권역';

-- 08. PREFERENCE_GROUP: 설문 응답 수 규칙(그룹에 속한 질문 각각에 적용)
-- 설문 개수 규칙의 유일한 기준. 트리거와 VIEW가 이 값을 읽는다.
CREATE TABLE `PREFERENCE_GROUP` (
    `group_code` VARCHAR(30) NOT NULL,
    `group_name` VARCHAR(100) NOT NULL,
    `min_selections` TINYINT UNSIGNED NOT NULL,
    `max_selections` TINYINT UNSIGNED NULL,
    CONSTRAINT `pk_preference_group` PRIMARY KEY (`group_code`),
    CONSTRAINT `ck_preference_group_count` CHECK (
        `min_selections` >= 1 AND
        (`max_selections` IS NULL OR `max_selections` >= `min_selections`)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='질문 그룹과 질문별 최소/최대 선택 수. NULL 최대값은 상한 없음';

-- 09. PREFERENCE
-- SINGLE_SELECT 질문은 max_selections = 1인 그룹에 넣는다(서비스·운영 규칙).
CREATE TABLE `PREFERENCE` (
    `preference_id` BIGINT UNSIGNED NOT NULL,
    `preference_code` VARCHAR(50) NOT NULL,
    `preference_name` VARCHAR(100) NOT NULL,
    `response_type` VARCHAR(30) NOT NULL,
    `group_code` VARCHAR(30) NOT NULL,
    CONSTRAINT `pk_preference` PRIMARY KEY (`preference_id`),
    CONSTRAINT `uk_preference_code` UNIQUE (`preference_code`),
    CONSTRAINT `uk_preference_type` UNIQUE (`preference_id`, `response_type`),
    CONSTRAINT `fk_preference_group`
        FOREIGN KEY (`group_code`) REFERENCES `PREFERENCE_GROUP` (`group_code`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `ck_preference_response_type`
        CHECK (`response_type` IN ('SINGLE_SELECT', 'MULTI_SELECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행 설문 질문';

-- 10. PREFERENCE_OPTION
CREATE TABLE `PREFERENCE_OPTION` (
    `option_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `preference_id` BIGINT UNSIGNED NOT NULL,
    `option_value` INT UNSIGNED NOT NULL,
    `option_name` VARCHAR(100) NOT NULL,
    CONSTRAINT `pk_preference_option` PRIMARY KEY (`option_id`),
    CONSTRAINT `uk_preference_option_natural` UNIQUE (`preference_id`, `option_value`),
    CONSTRAINT `fk_preference_option_preference`
        FOREIGN KEY (`preference_id`) REFERENCES `PREFERENCE` (`preference_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='설문 질문의 선택지';

-- 11. POI
CREATE TABLE `POI` (
    `poi_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `poi_name` VARCHAR(200) NOT NULL,
    `address` VARCHAR(500) NOT NULL,
    `latitude` DECIMAL(10, 7) NOT NULL,
    `longitude` DECIMAL(10, 7) NOT NULL,
    `category_code` VARCHAR(50) NOT NULL,
    `region_id` TINYINT UNSIGNED NOT NULL,
    `description` TEXT NOT NULL COMMENT '한 줄 소개(카드용)',
    `detail_description` TEXT NULL COMMENT '세부 설명(관광공사·비짓제주 소개글 전체)',
    `image_url` VARCHAR(2048) NOT NULL,
    `hidden_at` DATETIME NULL DEFAULT NULL COMMENT '관리자가 숨긴 시각(검색·AI 추천 제외)',
    `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '관리자가 삭제한 시각(화면에는 확인 불가로 표시)',
    `ai_recommend` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'AI 추천 대상(AI가 학습한 275곳)=1 / 직접 선택만=0',
    CONSTRAINT `pk_poi` PRIMARY KEY (`poi_id`),
    CONSTRAINT `fk_poi_region`
        FOREIGN KEY (`region_id`) REFERENCES `REGION` (`region_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `ck_poi_latitude` CHECK (`latitude` BETWEEN -90 AND 90),
    CONSTRAINT `ck_poi_longitude` CHECK (`longitude` BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='제주 관광지 마스터';

CREATE INDEX `ix_poi_region_category`
    ON `POI` (`region_id`, `category_code`);

-- 12. TRAVEL
CREATE TABLE `TRAVEL` (
    `travel_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `travel_no` INT UNSIGNED NOT NULL,
    `travel_name` VARCHAR(100) NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `region_mode` VARCHAR(20) NOT NULL,
    `age_group_snapshot` TINYINT UNSIGNED NOT NULL,
    `adopted_route_id` BIGINT UNSIGNED NULL,
    `adopted_at` DATETIME NULL,
    `source_post_id` BIGINT UNSIGNED NULL COMMENT '커뮤니티 글의 경로를 가져와 만든 여행이면 그 글 번호',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_travel` PRIMARY KEY (`travel_id`),
    CONSTRAINT `uk_travel_user_no` UNIQUE (`user_id`, `travel_no`),
    CONSTRAINT `fk_travel_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `ck_travel_date` CHECK (`start_date` <= `end_date`),
    CONSTRAINT `ck_travel_region_mode` CHECK (
        `region_mode` IN ('ALL', 'SELECTED')
    ),
    CONSTRAINT `ck_travel_age_group` CHECK (`age_group_snapshot` BETWEEN 1 AND 8),
    CONSTRAINT `ck_travel_adopted_pair` CHECK (
        (`adopted_route_id` IS NULL AND `adopted_at` IS NULL)
        OR
        (`adopted_route_id` IS NOT NULL AND `adopted_at` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='사용자가 계획한 제주 여행';

-- 13. COMPANION
CREATE TABLE `COMPANION` (
    `companion_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id` BIGINT UNSIGNED NOT NULL,
    `companion_seq` SMALLINT UNSIGNED NOT NULL,
    `relation_code` TINYINT UNSIGNED NOT NULL,
    `gender_code` TINYINT UNSIGNED NOT NULL,
    `age_group_code` TINYINT UNSIGNED NOT NULL,
    CONSTRAINT `pk_companion` PRIMARY KEY (`companion_id`),
    CONSTRAINT `uk_companion_natural` UNIQUE (`travel_id`, `companion_seq`),
    CONSTRAINT `fk_companion_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `ck_companion_seq` CHECK (`companion_seq` >= 1),
    CONSTRAINT `ck_companion_relation` CHECK (`relation_code` BETWEEN 1 AND 11),
    CONSTRAINT `ck_companion_gender` CHECK (`gender_code` IN (1, 2)),
    CONSTRAINT `ck_companion_age_group` CHECK (`age_group_code` BETWEEN 1 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행별 동행자';

-- 14. TRAVEL_REGION
CREATE TABLE `TRAVEL_REGION` (
    `travel_region_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id` BIGINT UNSIGNED NOT NULL,
    `region_id` TINYINT UNSIGNED NOT NULL,
    CONSTRAINT `pk_travel_region` PRIMARY KEY (`travel_region_id`),
    CONSTRAINT `uk_travel_region_natural` UNIQUE (`travel_id`, `region_id`),
    CONSTRAINT `fk_travel_region_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_travel_region_region`
        FOREIGN KEY (`region_id`) REFERENCES `REGION` (`region_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행에서 선택한 제주 권역';

-- 15. TRAVEL_PREFERENCE
-- response_type은 질문의 응답 방식을 복사해 두는 칼럼이며, 복합 FK가 원본과 같은지 검사한다.
-- single_preference_id: 단일 선택 질문이면 preference_id, 아니면 NULL → UNIQUE로 2번째 답변 차단.
CREATE TABLE `TRAVEL_PREFERENCE` (
    `travel_preference_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id` BIGINT UNSIGNED NOT NULL,
    `preference_id` BIGINT UNSIGNED NOT NULL,
    `response_type` VARCHAR(30) NOT NULL,
    `answer_value` INT UNSIGNED NOT NULL,
    `answer_rank` TINYINT UNSIGNED NOT NULL DEFAULT 1
        COMMENT '다중 선택 순위(사용자가 고른 순서). 1 = 1순위. 단일 선택은 1',
    `single_preference_id` BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN `response_type` = 'SINGLE_SELECT'
             THEN `preference_id` ELSE NULL END
    ) STORED,
    CONSTRAINT `pk_travel_preference` PRIMARY KEY (`travel_preference_id`),
    CONSTRAINT `uk_travel_preference_natural`
        UNIQUE (`travel_id`, `preference_id`, `answer_value`),
    CONSTRAINT `uk_travel_preference_single`
        UNIQUE (`travel_id`, `single_preference_id`),
    CONSTRAINT `uk_travel_preference_rank`
        UNIQUE (`travel_id`, `preference_id`, `answer_rank`),
    CONSTRAINT `ck_travel_preference_rank` CHECK (`answer_rank` >= 1),
    CONSTRAINT `fk_travel_preference_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_travel_preference_type`
        FOREIGN KEY (`preference_id`, `response_type`)
        REFERENCES `PREFERENCE` (`preference_id`, `response_type`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_travel_preference_option`
        FOREIGN KEY (`preference_id`, `answer_value`)
        REFERENCES `PREFERENCE_OPTION` (`preference_id`, `option_value`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행별 설문 응답';

-- 16. POI_SOURCE_MAP: AI 추천 결과 이름(place_name) → 관광지 번호. AI가 학습한 275곳만 연결 (poi_ai_map.sql)
CREATE TABLE `POI_SOURCE_MAP` (
    `source_poi_id` VARCHAR(255) NOT NULL,
    `poi_id` BIGINT UNSIGNED NOT NULL,
    CONSTRAINT `pk_poi_source_map`
        PRIMARY KEY (`source_poi_id`),
    CONSTRAINT `fk_poi_source_map_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='외부 관광지 ID와 서비스 POI 매핑';

CREATE INDEX `ix_poi_source_map_poi` ON `POI_SOURCE_MAP` (`poi_id`);

-- 17. TRAVEL_BOOKMARK
CREATE TABLE `TRAVEL_BOOKMARK` (
    `bookmark_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id` BIGINT UNSIGNED NOT NULL,
    `poi_id` BIGINT UNSIGNED NOT NULL,
    `source` VARCHAR(20) NOT NULL DEFAULT 'SEARCH' COMMENT 'RECOMMEND AI 추천 / SEARCH 관광지 검색 / IMPORT 경로 가져오기',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_travel_bookmark` PRIMARY KEY (`bookmark_id`),
    CONSTRAINT `ck_travel_bookmark_source` CHECK (`source` IN ('RECOMMEND', 'SEARCH', 'IMPORT')),
    CONSTRAINT `uk_travel_bookmark_natural` UNIQUE (`travel_id`, `poi_id`),
    CONSTRAINT `fk_travel_bookmark_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_travel_bookmark_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행 장소: 여행 일정(루트)에 넣으려고 사용자가 추가한 관광지';

-- 18. TRAVEL_ROUTE
-- 실제 일정은 ROUTE_DAY / ROUTE_SPOT이 기준. 요약 문자열은 저장하지 않고 조회로 만든다.
-- uk_travel_route_travel_route는 복합 FK(채택 경로, 피드백)의 참조 대상이다.
CREATE TABLE `TRAVEL_ROUTE` (
    `route_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id` BIGINT UNSIGNED NOT NULL,
    `route_name` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_travel_route` PRIMARY KEY (`route_id`),
    CONSTRAINT `uk_travel_route_travel_route` UNIQUE (`travel_id`, `route_id`),
    CONSTRAINT `uk_travel_route_one` UNIQUE (`travel_id`),              -- 여행당 경로 1개
    CONSTRAINT `fk_travel_route_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행 장소(TRAVEL_BOOKMARK)를 일차별로 배치한 여행 루트. 여행당 1개';

-- 19. ROUTE_DAY
-- 해당 날짜 = TRAVEL.start_date + (day_no - 1). day_no는 여행 일수 이하여야 한다(서비스 검사).
CREATE TABLE `ROUTE_DAY` (
    `route_day_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `route_id` BIGINT UNSIGNED NOT NULL,
    `day_no` SMALLINT UNSIGNED NOT NULL,
    `primary_region_id` TINYINT UNSIGNED NOT NULL,
    CONSTRAINT `pk_route_day` PRIMARY KEY (`route_day_id`),
    CONSTRAINT `uk_route_day_no` UNIQUE (`route_id`, `day_no`),
    CONSTRAINT `fk_route_day_route`
        FOREIGN KEY (`route_id`) REFERENCES `TRAVEL_ROUTE` (`route_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_route_day_region`
        FOREIGN KEY (`primary_region_id`) REFERENCES `REGION` (`region_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `ck_route_day_no` CHECK (`day_no` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행 루트의 일차별 일정';

-- 20. ROUTE_SPOT
-- 순서 변경은 그 날의 행을 모두 DELETE 후 다시 INSERT (UNIQUE 순서 충돌 방지).
CREATE TABLE `ROUTE_SPOT` (
    `route_spot_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `route_day_id` BIGINT UNSIGNED NOT NULL,
    `poi_id` BIGINT UNSIGNED NOT NULL,
    `visit_order` SMALLINT UNSIGNED NOT NULL,
    CONSTRAINT `pk_route_spot` PRIMARY KEY (`route_spot_id`),
    CONSTRAINT `uk_route_spot_order` UNIQUE (`route_day_id`, `visit_order`),
    CONSTRAINT `uk_route_spot_poi` UNIQUE (`route_day_id`, `poi_id`),
    CONSTRAINT `fk_route_spot_day`
        FOREIGN KEY (`route_day_id`) REFERENCES `ROUTE_DAY` (`route_day_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_route_spot_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `ck_route_spot_order` CHECK (`visit_order` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='일차별 루트의 관광지와 방문 순서';

-- 21. TRAVEL_FEEDBACK
CREATE TABLE `TRAVEL_FEEDBACK` (
    `feedback_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id` BIGINT UNSIGNED NOT NULL,
    `route_id` BIGINT UNSIGNED NOT NULL,
    `execution_status` VARCHAR(20) NOT NULL,
    `satisfaction_score` TINYINT UNSIGNED NULL,
    `answered_at` DATETIME NOT NULL,
    CONSTRAINT `pk_travel_feedback` PRIMARY KEY (`feedback_id`),
    CONSTRAINT `uk_travel_feedback_natural` UNIQUE (`travel_id`),
    CONSTRAINT `fk_travel_feedback_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_travel_feedback_route`
        FOREIGN KEY (`travel_id`, `route_id`)
        REFERENCES `TRAVEL_ROUTE` (`travel_id`, `route_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `ck_travel_feedback_status` CHECK (
        `execution_status` IN ('COMPLETED', 'PARTIAL', 'NOT_TAKEN')
    ),
    CONSTRAINT `ck_travel_feedback_satisfaction` CHECK (
        (`execution_status` = 'NOT_TAKEN' AND `satisfaction_score` IS NULL)
        OR
        (`execution_status` IN ('COMPLETED', 'PARTIAL')
         AND `satisfaction_score` IS NOT NULL
         AND `satisfaction_score` BETWEEN 1 AND 5)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='채택한 여행 루트의 실제 수행 결과';

-- 22. TRAVEL_FEEDBACK_REASON
CREATE TABLE `TRAVEL_FEEDBACK_REASON` (
    `feedback_reason_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `feedback_id` BIGINT UNSIGNED NOT NULL,
    `reason_code` VARCHAR(30) NOT NULL,
    `reason_text` VARCHAR(50) NULL,
    CONSTRAINT `pk_travel_feedback_reason` PRIMARY KEY (`feedback_reason_id`),
    CONSTRAINT `uk_travel_feedback_reason_natural` UNIQUE (`feedback_id`, `reason_code`),
    CONSTRAINT `fk_travel_feedback_reason_feedback`
        FOREIGN KEY (`feedback_id`) REFERENCES `TRAVEL_FEEDBACK` (`feedback_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `ck_travel_feedback_reason_code` CHECK (
        `reason_code` IN (
            'TIME_SHORTAGE',
            'CHANGE_OF_MIND',
            'PERSONAL_REASON',
            'WEATHER',
            'POI_ISSUE',
            'OTHER'
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행 루트를 일부 수행했거나 여행하지 못한 이유';

-- AI 추천 기록 (관리자 KPI·재학습)
CREATE TABLE `RECOMMEND_REQUEST` (
    `request_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id` BIGINT UNSIGNED NOT NULL,
    `model_version` VARCHAR(20) NOT NULL,
    `status` VARCHAR(10) NOT NULL,
    `response_ms` INT UNSIGNED NULL,
    `result_count` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    `unmapped_count` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    `error_message` VARCHAR(200) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_recommend_request` PRIMARY KEY (`request_id`),
    CONSTRAINT `fk_recommend_request_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `ck_recommend_request_status` CHECK (`status` IN ('SUCCESS', 'FAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='AI 추천 요청 1건 (관리자 KPI·재학습 데이터)';

CREATE INDEX `ix_recommend_request_created` ON `RECOMMEND_REQUEST` (`created_at`);
CREATE INDEX `ix_recommend_request_travel` ON `RECOMMEND_REQUEST` (`travel_id`);

CREATE TABLE `RECOMMEND_ITEM` (
    `item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `request_id` BIGINT UNSIGNED NOT NULL,
    `rank_no` SMALLINT UNSIGNED NOT NULL,
    `place_name` VARCHAR(200) NOT NULL,
    `poi_id` BIGINT UNSIGNED NULL COMMENT '우리 관광지와 연결 안 되면 NULL (매핑 실패)',
    `shown` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '화면에 보여 준 10곳이면 1',
    CONSTRAINT `pk_recommend_item` PRIMARY KEY (`item_id`),
    CONSTRAINT `uk_recommend_item_rank` UNIQUE (`request_id`, `rank_no`),
    CONSTRAINT `fk_recommend_item_request`
        FOREIGN KEY (`request_id`) REFERENCES `RECOMMEND_REQUEST` (`request_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_recommend_item_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='AI가 추천한 관광지 (순위 순)';

CREATE INDEX `ix_recommend_item_poi` ON `RECOMMEND_ITEM` (`poi_id`);

-- 후기의 관광지별 결과
CREATE TABLE `TRAVEL_FEEDBACK_SPOT` (
    `feedback_spot_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `feedback_id` BIGINT UNSIGNED NOT NULL,
    `poi_id` BIGINT UNSIGNED NOT NULL,
    `visited` TINYINT(1) NOT NULL,
    `reaction` VARCHAR(10) NULL COMMENT 'LIKE 좋았어요 / DISLIKE 아쉬워요 / NULL 선택 안 함',
    CONSTRAINT `pk_travel_feedback_spot` PRIMARY KEY (`feedback_spot_id`),
    CONSTRAINT `uk_travel_feedback_spot` UNIQUE (`feedback_id`, `poi_id`),
    CONSTRAINT `fk_travel_feedback_spot_feedback`
        FOREIGN KEY (`feedback_id`) REFERENCES `TRAVEL_FEEDBACK` (`feedback_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_travel_feedback_spot_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `ck_travel_feedback_spot_reaction` CHECK (
        `reaction` IS NULL OR (`visited` = 1 AND `reaction` IN ('LIKE', 'DISLIKE'))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='후기: 확정 일정의 관광지별 방문 여부와 반응';

-- 23. ACCOMMODATION: 루트 주변 숙소 안내(FR-26)용. 예약·결제 정보 없음, 조회만
--     source_id = 출처 접두어 + 원본 ID (TOUR:123 한국관광공사 / VJ:CNTS_… 비짓제주 / KAKAO:123 카카오)
CREATE TABLE `ACCOMMODATION` (
    `accommodation_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `source_id` VARCHAR(255) NOT NULL,
    `name` VARCHAR(200) NOT NULL,
    `accommodation_type` VARCHAR(20) NOT NULL DEFAULT 'ETC'
        COMMENT 'CODE ACCOM_TYPE: HOTEL/RESORT/PENSION/GUESTHOUSE/MOTEL/CAMPING/ETC',
    `address` VARCHAR(500) NOT NULL,
    `latitude` DECIMAL(10, 7) NOT NULL,
    `longitude` DECIMAL(10, 7) NOT NULL,
    `phone` VARCHAR(50) NULL,
    `image_url` VARCHAR(2048) NULL,
    CONSTRAINT `pk_accommodation` PRIMARY KEY (`accommodation_id`),
    CONSTRAINT `uk_accommodation_source` UNIQUE (`source_id`),
    CONSTRAINT `ck_accommodation_latitude` CHECK (`latitude` BETWEEN -90 AND 90),
    CONSTRAINT `ck_accommodation_longitude` CHECK (`longitude` BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='루트 주변 숙소 안내용 숙소(조회 전용)';

-- 반경 검색: 위도·경도 범위로 먼저 좁힌다
CREATE INDEX `ix_accommodation_lat_lng` ON `ACCOMMODATION` (`latitude`, `longitude`);

-- 24. COMMUNITY_POST
-- 작성자가 탈퇴(물리 삭제)되면 user_id가 NULL이 되고 글은 남는다. 화면에는 '탈퇴한 회원'으로 표시.
-- travel_id: 후기(REVIEW)에 첨부한 여행. 게시판에서 그 여행의 최종 경로를 보여준다. 여행이 지워지면 NULL.
-- image_url: 첨부 사진 1장(서버에 저장한 파일 주소). deleted_at: 삭제한 글(목록·상세에서 제외)
CREATE TABLE `COMMUNITY_POST` (
    `post_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NULL,
    `travel_id` BIGINT UNSIGNED NULL COMMENT '후기에 첨부한 여행(채택 경로 표시용). 여행 삭제 시 NULL',
    `post_type` VARCHAR(20) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `image_url` VARCHAR(500) NULL COMMENT '첨부 사진 1장',
    `view_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT NULL,
    `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '삭제 시각(삭제된 글은 조회 제외)',
    `hidden_at` DATETIME NULL DEFAULT NULL COMMENT '신고된 시각("신고된 게시글입니다", 관리자만 내용 확인). 반려하면 NULL',
    `block_reason` VARCHAR(20) NULL DEFAULT NULL COMMENT '관리자 차단 사유(신고 사유 코드). NULL이면 차단 아님',
    CONSTRAINT `pk_community_post` PRIMARY KEY (`post_id`),
    CONSTRAINT `fk_community_post_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT `fk_community_post_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT `ck_community_post_block_reason`
        CHECK (`block_reason` IS NULL OR `block_reason` IN ('SEXUAL', 'PRIVACY', 'ABUSE', 'SPAM')),
    CONSTRAINT `ck_community_post_type` CHECK (
        `post_type` IN ('QUESTION', 'REVIEW')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='커뮤니티 질문 및 여행 후기. 탈퇴 회원 글은 user_id NULL';

CREATE INDEX `ix_community_post_user_created`
    ON `COMMUNITY_POST` (`user_id`, `created_at`);

CREATE INDEX `ix_community_post_type_created`
    ON `COMMUNITY_POST` (`post_type`, `deleted_at`, `created_at`);

-- 25. COMMUNITY_COMMENT
-- parent_comment_id: 대댓글이면 원댓글 ID(한 단계만). 원댓글이 지워져도 대댓글이 있으면 "삭제된 댓글"로 남긴다.
CREATE TABLE `COMMUNITY_COMMENT` (
    `comment_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT UNSIGNED NOT NULL,
    `parent_comment_id` BIGINT UNSIGNED NULL COMMENT '대댓글의 원댓글',
    `user_id` BIGINT UNSIGNED NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT NULL,
    `deleted_at` DATETIME NULL DEFAULT NULL,
    `hidden_at` DATETIME NULL DEFAULT NULL COMMENT '신고된 시각("신고된 댓글입니다"). 반려하면 NULL',
    `block_reason` VARCHAR(20) NULL DEFAULT NULL COMMENT '관리자 차단 사유(신고 사유 코드)',
    CONSTRAINT `pk_community_comment` PRIMARY KEY (`comment_id`),
    CONSTRAINT `fk_community_comment_post`
        FOREIGN KEY (`post_id`) REFERENCES `COMMUNITY_POST` (`post_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_community_comment_parent`
        FOREIGN KEY (`parent_comment_id`) REFERENCES `COMMUNITY_COMMENT` (`comment_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_community_comment_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT `ck_community_comment_block_reason`
        CHECK (`block_reason` IS NULL OR `block_reason` IN ('SEXUAL', 'PRIVACY', 'ABUSE', 'SPAM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='커뮤니티 게시글 댓글·대댓글. 탈퇴 회원 댓글은 user_id NULL';

CREATE INDEX `ix_community_comment_post_created`
    ON `COMMUNITY_COMMENT` (`post_id`, `created_at`);

-- 26. COMMUNITY_POST_LIKE: 회원 1명이 글 1개에 좋아요 1번
CREATE TABLE `COMMUNITY_POST_LIKE` (
    `post_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_community_post_like` PRIMARY KEY (`post_id`, `user_id`),
    CONSTRAINT `fk_community_post_like_post`
        FOREIGN KEY (`post_id`) REFERENCES `COMMUNITY_POST` (`post_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_community_post_like_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='게시글 좋아요';

-- 27. REPORT: 게시글·댓글 신고. 같은 대상은 한 회원이 1번만. 처리 기준은 docs/17 참고
CREATE TABLE `REPORT` (
    `report_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `target_type` VARCHAR(10) NOT NULL COMMENT 'POST 글 / COMMENT 댓글',
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT 'post_id 또는 comment_id',
    `target_user_id` BIGINT UNSIGNED NULL COMMENT '신고 당시 작성자',
    `reporter_id` BIGINT UNSIGNED NOT NULL,
    `reason_code` VARCHAR(20) NOT NULL,
    `detail` VARCHAR(200) NULL COMMENT '신고한 회원이 적은 자세한 내용(선택)',
    `status` VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 대기 / ACCEPTED 조치 / REJECTED 문제 없음',
    `action` VARCHAR(10) NULL COMMENT '처리 결과 KEEP 유지 / BLOCK 차단 / DELETE 삭제',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handled_at` DATETIME NULL,
    `handled_by` BIGINT UNSIGNED NULL,
    CONSTRAINT `pk_report` PRIMARY KEY (`report_id`),
    CONSTRAINT `uk_report_once` UNIQUE (`target_type`, `target_id`, `reporter_id`),
    CONSTRAINT `fk_report_reporter`
        FOREIGN KEY (`reporter_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_report_target_user`
        FOREIGN KEY (`target_user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT `fk_report_handled_by`
        FOREIGN KEY (`handled_by`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    CONSTRAINT `ck_report_target_type` CHECK (`target_type` IN ('POST', 'COMMENT')),
    CONSTRAINT `ck_report_reason` CHECK (`reason_code` IN ('SEXUAL', 'PRIVACY', 'ABUSE', 'SPAM')),
    CONSTRAINT `ck_report_status` CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT `ck_report_action` CHECK (`action` IS NULL OR `action` IN ('KEEP', 'BLOCK', 'DELETE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='게시글·댓글 신고';

CREATE INDEX `ix_report_status_target` ON `REPORT` (`status`, `target_type`, `target_id`);
CREATE INDEX `ix_report_target_user` ON `REPORT` (`target_user_id`, `status`);

-- 28. (USER_SANCTION 회원 제재 이력은 migration_14에서 삭제 — 회원 제재 없이 글 차단만)

-- 29. USER_POI_DISLIKE: 회원이 관심없음으로 표시한 관광지 (다음 AI 추천에서 제외, 모든 여행 공통)
CREATE TABLE `USER_POI_DISLIKE` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `poi_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `pk_user_poi_dislike` PRIMARY KEY (`user_id`, `poi_id`),
    CONSTRAINT `fk_user_poi_dislike_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fk_user_poi_dislike_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='회원이 관심없음으로 표시한 관광지 (AI 추천에서 제외)';

CREATE INDEX `ix_community_post_like_user` ON `COMMUNITY_POST_LIKE` (`user_id`);

-- 채택 경로: 복합 FK로 "존재 + 같은 여행 소속"을 함께 보장한다(소유권 트리거 불필요).
-- adopted_route_id가 NULL이면 FK 검사를 하지 않으므로 미채택 상태가 허용된다.
-- TRAVEL ↔ TRAVEL_ROUTE 순환 참조이므로 테이블 생성 뒤 ALTER로 추가한다.
ALTER TABLE `TRAVEL`
    ADD CONSTRAINT `fk_travel_adopted_route`
        FOREIGN KEY (`travel_id`, `adopted_route_id`)
        REFERENCES `TRAVEL_ROUTE` (`travel_id`, `route_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT;

-- 커뮤니티에서 가져온 여행: 글이 지워져도 여행은 남는다. (TRAVEL ↔ COMMUNITY_POST 순환 참조라 ALTER로 추가)
ALTER TABLE `TRAVEL`
    ADD CONSTRAINT `fk_travel_source_post`
        FOREIGN KEY (`source_post_id`) REFERENCES `COMMUNITY_POST` (`post_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL;

-- 기초 데이터: 질문 9개, 선택지 84개(스타일 6문항 × 7점 + 동기 9 + 테마 21 + 소득 12). 임의 관광지·예시 회원 데이터 없음.
START TRANSACTION;

INSERT INTO `CODE_GROUP` (`group_code`, `group_name`) VALUES
    ('GEN', '성별'),
    ('AGE', '연령대'),
    ('TCR', '동행자 관계'),
    ('POI_CAT', '관광지 분류'),   -- POI.category_code. AI 학습 VISIT_AREA_TYPE_CD 1~8·9(원본 13) 기준
    ('ACCOM_TYPE', '숙소 종류');  -- ACCOMMODATION.accommodation_type

INSERT INTO `CODE` (`group_code`, `code_value`, `code_name`) VALUES
    ('GEN', '1', '남자'),
    ('GEN', '2', '여자'),
    ('AGE', '1', '9세 이하'),
    ('AGE', '2', '10대'),
    ('AGE', '3', '20대'),
    ('AGE', '4', '30대'),
    ('AGE', '5', '40대'),
    ('AGE', '6', '50대'),
    ('AGE', '7', '60대'),
    ('AGE', '8', '70세 이상'),
    ('TCR', '1', '배우자'),
    ('TCR', '2', '자녀'),
    ('TCR', '3', '부모'),
    ('TCR', '4', '조부모'),
    ('TCR', '5', '형제·자매'),
    ('TCR', '6', '친인척'),
    ('TCR', '7', '친구'),
    ('TCR', '8', '연인'),
    ('TCR', '9', '동료'),
    ('TCR', '10', '친목 단체·모임'),
    ('TCR', '11', '기타'),
    ('POI_CAT', 'NATURE', '자연관광지'),
    ('POI_CAT', 'HISTORY', '역사·유적·종교 시설'),
    ('POI_CAT', 'CULTURE', '문화시설'),
    ('POI_CAT', 'COMMERCIAL', '상업지구'),
    ('POI_CAT', 'LEISURE', '레저·스포츠 관련 시설'),
    ('POI_CAT', 'THEME', '테마시설'),
    ('POI_CAT', 'TRAIL', '산책로·둘레길'),
    ('POI_CAT', 'FESTIVAL', '지역 축제·행사'),
    ('POI_CAT', 'EXPERIENCE', '체험 활동 관광지'),
    ('ACCOM_TYPE', 'HOTEL', '호텔'),
    ('ACCOM_TYPE', 'RESORT', '리조트·콘도'),
    ('ACCOM_TYPE', 'PENSION', '펜션·풀빌라'),
    ('ACCOM_TYPE', 'GUESTHOUSE', '게스트하우스·민박'),
    ('ACCOM_TYPE', 'MOTEL', '모텔'),
    ('ACCOM_TYPE', 'CAMPING', '캠핑·글램핑'),
    ('ACCOM_TYPE', 'ETC', '기타 숙소');

INSERT INTO `REGION` (`region_id`, `region_code`, `region_name`) VALUES
    (1, 'EAST', '동부'),
    (2, 'WEST', '서부'),
    (3, 'SOUTH', '남부'),
    (4, 'NORTH', '북부');

INSERT INTO `PREFERENCE_GROUP` (`group_code`, `group_name`, `min_selections`, `max_selections`) VALUES
    ('STYLE', '여행 스타일', 1, 1),
    ('MOTIVE', '여행 동기', 3, 3),
    ('THEME', '테마 선호도', 3, 3),
    ('INCOME', '월평균 소득 구간', 1, 1);

INSERT INTO `PREFERENCE` (`preference_id`, `preference_code`, `preference_name`, `response_type`, `group_code`) VALUES
    (101, 'STYLE_NATURE_CITY', '자연 중심 ↔ 도시 중심', 'SINGLE_SELECT', 'STYLE'),
    (102, 'STYLE_NEW_FAMILIAR', '새로운 장소 ↔ 익숙한 장소', 'SINGLE_SELECT', 'STYLE'),
    (103, 'STYLE_HIDDEN_FAMOUS', '숨은 명소 ↔ 유명 인기 명소', 'SINGLE_SELECT', 'STYLE'),
    (104, 'STYLE_RELAX_ACTIVITY', '휴식·힐링 ↔ 체험·액티비티', 'SINGLE_SELECT', 'STYLE'),
    (105, 'PHOTO_IMPORTANCE', '사진 촬영 중요도', 'SINGLE_SELECT', 'STYLE'),
    (106, 'STYLE_PLAN_FREE', '계획대로 ↔ 상황에 따라 자유롭게', 'SINGLE_SELECT', 'STYLE'),
    (201, 'TRAVEL_MOTIVE', '여행 동기', 'MULTI_SELECT', 'MOTIVE'),
    (202, 'USER_MISSION', '테마 선호도', 'MULTI_SELECT', 'THEME'),
    (203, 'INCOME_CODE', '월평균 소득 구간', 'SINGLE_SELECT', 'INCOME');

INSERT INTO `PREFERENCE_OPTION` (`preference_id`, `option_value`, `option_name`) VALUES
    -- 101. TRAVEL_STYL_1: 자연 ↔ 도시
    (101, 1, '자연 매우 선호'), (101, 2, '자연 중간 선호'), (101, 3, '자연 약간 선호'), (101, 4, '중립'),
    (101, 5, '도시 약간 선호'), (101, 6, '도시 중간 선호'), (101, 7, '도시 매우 선호'),
    -- 102. TRAVEL_STYL_3: 새로운 지역 ↔ 익숙한 지역
    (102, 1, '새로운 지역 매우 선호'), (102, 2, '새로운 지역 중간 선호'), (102, 3, '새로운 지역 약간 선호'), (102, 4, '중립'),
    (102, 5, '익숙한 지역 약간 선호'), (102, 6, '익숙한 지역 중간 선호'), (102, 7, '익숙한 지역 매우 선호'),
    -- 103. TRAVEL_STYL_6: 숨은 명소 ↔ 유명 명소
    (103, 1, '숨은 명소 매우 선호'), (103, 2, '숨은 명소 중간 선호'), (103, 3, '숨은 명소 약간 선호'), (103, 4, '중립'),
    (103, 5, '유명 명소 약간 선호'), (103, 6, '유명 명소 중간 선호'), (103, 7, '유명 명소 매우 선호'),
    -- 104. TRAVEL_STYL_5: 휴양·휴식 ↔ 체험·활동
    (104, 1, '휴양·휴식 매우 선호'), (104, 2, '휴양·휴식 중간 선호'), (104, 3, '휴양·휴식 약간 선호'), (104, 4, '중립'),
    (104, 5, '체험·활동 약간 선호'), (104, 6, '체험·활동 중간 선호'), (104, 7, '체험·활동 매우 선호'),
    -- 105. TRAVEL_STYL_8: 사진 촬영 중요하지 않음 ↔ 중요함
    (105, 1, '사진 촬영 전혀 중요하지 않음'), (105, 2, '사진 촬영 중요하지 않은 편'), (105, 3, '사진 촬영 약간 중요하지 않음'), (105, 4, '중립'),
    (105, 5, '사진 촬영 약간 중요함'), (105, 6, '사진 촬영 중요한 편'), (105, 7, '사진 촬영 매우 중요함'),
    -- 106. TRAVEL_STYL_7: 계획에 따른 여행 ↔ 상황에 따른 여행
    (106, 1, '계획에 따른 여행 매우 선호'), (106, 2, '계획에 따른 여행 중간 선호'), (106, 3, '계획에 따른 여행 약간 선호'), (106, 4, '중립'),
    (106, 5, '상황에 따른 여행 약간 선호'), (106, 6, '상황에 따른 여행 중간 선호'), (106, 7, '상황에 따른 여행 매우 선호'),
    (201, 1, '일상 탈출·기분 전환'),
    (201, 2, '휴식·피로 회복'),
    (201, 3, '친목·유대감'),
    (201, 4, '자아 탐색·성찰'),
    (201, 5, 'SNS 공유·자랑'),
    (201, 6, '운동·건강'),
    (201, 7, '새로운 경험'),
    (201, 8, '역사·문화·배움'),
    (201, 9, '기념·특별한 목적'),
    (202, 1, '쇼핑'),
    (202, 2, '테마파크, 놀이시설, 동·식물원 방문'),
    (202, 3, '역사 유적지 방문'),
    (202, 4, '시티투어'),
    (202, 5, '야외 스포츠·레포츠 활동'),
    (202, 6, '지역 문화예술·공연·전시시설 관람'),
    (202, 7, '유흥·오락(나이트라이프)'),
    (202, 8, '캠핑'),
    (202, 9, '지역 축제·이벤트 참가'),
    (202, 10, '온천·스파'),
    (202, 11, '교육·체험 프로그램 참가'),
    (202, 12, '드라마 촬영지 방문'),
    (202, 13, '종교·성지 순례'),
    (202, 14, '웰니스 여행'),
    (202, 15, 'SNS 인생샷 여행'),
    (202, 16, '호캉스 여행'),
    (202, 17, '신규 여행지 발굴'),
    (202, 18, '반려동물 동반 여행'),
    (202, 19, '인플루언서 따라 하기 여행'),
    (202, 20, '친환경 여행'),
    (202, 21, '등반여행'),
    (203, 1, '소득 없음'),
    (203, 2, '월평균 100만 원 미만'),
    (203, 3, '월평균 100만 원 이상 ~ 200만 원 미만'),
    (203, 4, '월평균 200만 원 이상 ~ 300만 원 미만'),
    (203, 5, '월평균 300만 원 이상 ~ 400만 원 미만'),
    (203, 6, '월평균 400만 원 이상 ~ 500만 원 미만'),
    (203, 7, '월평균 500만 원 이상 ~ 600만 원 미만'),
    (203, 8, '월평균 600만 원 이상 ~ 700만 원 미만'),
    (203, 9, '월평균 700만 원 이상 ~ 800만 원 미만'),
    (203, 10, '월평균 800만 원 이상 ~ 900만 원 미만'),
    (203, 11, '월평균 900만 원 이상 ~ 1,000만 원 미만'),
    (203, 12, '월평균 1,000만 원 이상');

COMMIT;

DELIMITER $$

-- 설문 최대 선택 수: PREFERENCE_GROUP.max_selections를 읽어 초과 답변을 차단한다.
-- (단일 선택의 2번째 답변은 uk_travel_preference_single이 먼저 막는다.)
-- 동시 요청은 서비스가 TRAVEL 행을 SELECT ... FOR UPDATE로 잠근 상태에서 답변을 변경한다.
CREATE TRIGGER `tr_travel_preference_max_insert`
BEFORE INSERT ON `TRAVEL_PREFERENCE` FOR EACH ROW
BEGIN
    DECLARE v_max INT DEFAULT NULL;
    DECLARE v_count INT DEFAULT 0;

    SELECT g.`max_selections` INTO v_max
      FROM `PREFERENCE` q
      JOIN `PREFERENCE_GROUP` g ON g.`group_code` = q.`group_code`
     WHERE q.`preference_id` = NEW.`preference_id`;

    IF v_max IS NOT NULL THEN
        SELECT COUNT(*) INTO v_count FROM `TRAVEL_PREFERENCE`
         WHERE `travel_id` = NEW.`travel_id`
           AND `preference_id` = NEW.`preference_id`;
        IF v_count >= v_max THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Too many answers for this preference';
        END IF;
    END IF;
END$$

CREATE TRIGGER `tr_travel_preference_max_update`
BEFORE UPDATE ON `TRAVEL_PREFERENCE` FOR EACH ROW
BEGIN
    DECLARE v_max INT DEFAULT NULL;
    DECLARE v_count INT DEFAULT 0;

    IF NOT (OLD.`travel_id` = NEW.`travel_id`
            AND OLD.`preference_id` = NEW.`preference_id`) THEN
        SELECT g.`max_selections` INTO v_max
          FROM `PREFERENCE` q
          JOIN `PREFERENCE_GROUP` g ON g.`group_code` = q.`group_code`
         WHERE q.`preference_id` = NEW.`preference_id`;

        IF v_max IS NOT NULL THEN
            SELECT COUNT(*) INTO v_count FROM `TRAVEL_PREFERENCE`
             WHERE `travel_id` = NEW.`travel_id`
               AND `preference_id` = NEW.`preference_id`;
            IF v_count >= v_max THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Too many answers for this preference';
            END IF;
        END IF;
    END IF;
END$$

-- 여행 1건 물리 삭제. 트랜잭션은 호출하는 쪽(백엔드 @Transactional 등)에서 연다.
-- 순서: 채택 해제 → 피드백(사유 CASCADE) → 경로(일차·방문지 CASCADE) → 여행(설문·동반자·권역·찜 CASCADE)
CREATE PROCEDURE `sp_delete_travel`(IN p_travel_id BIGINT UNSIGNED)
BEGIN
    UPDATE `TRAVEL`
       SET `adopted_route_id` = NULL, `adopted_at` = NULL
     WHERE `travel_id` = p_travel_id;

    DELETE FROM `TRAVEL_FEEDBACK` WHERE `travel_id` = p_travel_id;
    DELETE FROM `TRAVEL_ROUTE`    WHERE `travel_id` = p_travel_id;
    DELETE FROM `TRAVEL`          WHERE `travel_id` = p_travel_id;
END$$

-- 탈퇴 유예가 끝난 회원 1명 물리 삭제. 배치가 대상자를 골라 회원마다 트랜잭션으로 호출한다.
-- 대상 조회 예: status='WITHDRAWAL_PENDING' AND withdrawn_at <= NOW() - INTERVAL 30 DAY
-- 결과: 여행 데이터·소셜 연동·세션·이메일 인증 기록 삭제, 커뮤니티 글·댓글은 user_id NULL로 보존.
CREATE PROCEDURE `sp_purge_user`(IN p_user_id BIGINT UNSIGNED)
BEGIN
    DECLARE v_status VARCHAR(30) DEFAULT NULL;
    DECLARE v_email VARCHAR(255) DEFAULT NULL;

    SELECT `status`, `email` INTO v_status, v_email
      FROM `USER` WHERE `user_id` = p_user_id
       FOR UPDATE;

    IF v_status IS NULL OR v_status <> 'WITHDRAWAL_PENDING' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Only WITHDRAWAL_PENDING users can be purged';
    END IF;

    UPDATE `TRAVEL`
       SET `adopted_route_id` = NULL, `adopted_at` = NULL
     WHERE `user_id` = p_user_id;

    DELETE f FROM `TRAVEL_FEEDBACK` f
      JOIN `TRAVEL` t ON t.`travel_id` = f.`travel_id`
     WHERE t.`user_id` = p_user_id;

    DELETE r FROM `TRAVEL_ROUTE` r
      JOIN `TRAVEL` t ON t.`travel_id` = r.`travel_id`
     WHERE t.`user_id` = p_user_id;

    DELETE FROM `EMAIL_VERIFICATION` WHERE `email` = v_email;

    -- TRAVEL·SOCIAL_ACCOUNT·AUTH_SESSION은 CASCADE, 커뮤니티 글·댓글은 SET NULL
    DELETE FROM `USER` WHERE `user_id` = p_user_id;
END$$

DELIMITER ;

-- AI 입력 조회 전용 VIEW: 테이블에 별도 복사하지 않으므로 답변 수정이 즉시 반영된다.
-- 여행당 1행. 성별은 현재 USER 값, 연령대는 여행 생성 시 저장한 스냅샷(1~8 그대로. 모델 학습 범위 밖 처리는 AI 서버 담당).
-- 여행 스타일 101~106은 1~7점. 여행 동기(201)·테마(202)는 3개씩이며 answer_rank 1이 1순위(사용자가 처음 고른 것).
--   travel_motive_1  → 모델 TRAVEL_MOTIVE_1
--   user_mission_1   → 모델 TRAVEL_MISSION_PRIORITY_WEB
-- region_mode / selected_regions / companions를 함께 제공하여 FastAPI 요청값을 구성할 수 있게 한다.
-- JSON 배열의 원소 순서는 보장하지 않는다(순위가 필요하면 *_1 ~ *_3 칼럼 사용).
-- user_id는 서비스의 소유권 검사 용도. AI에는 요청한 특성만 명시적으로 전달한다.
CREATE SQL SECURITY INVOKER VIEW `AI_TRAVEL_INPUT` AS
SELECT
    t.`travel_id`,
    t.`user_id`,
    u.`gender_code`,
    t.`age_group_snapshot` AS `age_group_code`,
    t.`region_mode`,

    COALESCE((
        SELECT JSON_ARRAYAGG(r.`region_code`)
          FROM `TRAVEL_REGION` tr
          JOIN `REGION` r ON r.`region_id` = tr.`region_id`
         WHERE tr.`travel_id` = t.`travel_id`
    ), JSON_ARRAY()) AS `selected_regions`,

    (SELECT MAX(p.`answer_value`) FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 101) AS `style_nature_city`,
    (SELECT MAX(p.`answer_value`) FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 102) AS `style_new_familiar`,
    (SELECT MAX(p.`answer_value`) FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 103) AS `style_hidden_famous`,
    (SELECT MAX(p.`answer_value`) FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 104) AS `style_relax_activity`,
    (SELECT MAX(p.`answer_value`) FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 105) AS `photo_importance`,
    (SELECT MAX(p.`answer_value`) FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 106) AS `style_plan_free`,
    (SELECT MAX(p.`answer_value`) FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 203) AS `income_code`,

    -- 여행 동기 1~3순위 (1순위 = TRAVEL_MOTIVE_1)
    (SELECT p.`answer_value` FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 201 AND p.`answer_rank` = 1) AS `travel_motive_1`,
    (SELECT p.`answer_value` FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 201 AND p.`answer_rank` = 2) AS `travel_motive_2`,
    (SELECT p.`answer_value` FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 201 AND p.`answer_rank` = 3) AS `travel_motive_3`,

    -- 테마 1~3순위 (1순위 = TRAVEL_MISSION_PRIORITY_WEB)
    (SELECT p.`answer_value` FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 202 AND p.`answer_rank` = 1) AS `user_mission_1`,
    (SELECT p.`answer_value` FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 202 AND p.`answer_rank` = 2) AS `user_mission_2`,
    (SELECT p.`answer_value` FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 202 AND p.`answer_rank` = 3) AS `user_mission_3`,

    COALESCE((
        SELECT JSON_ARRAYAGG(p.`answer_value`)
          FROM `TRAVEL_PREFERENCE` p
         WHERE p.`travel_id` = t.`travel_id`
           AND p.`preference_id` = 201
    ), JSON_ARRAY()) AS `travel_motive`,

    COALESCE((
        SELECT JSON_ARRAYAGG(p.`answer_value`)
          FROM `TRAVEL_PREFERENCE` p
         WHERE p.`travel_id` = t.`travel_id`
           AND p.`preference_id` = 202
    ), JSON_ARRAY()) AS `user_mission`,

    (SELECT COUNT(*)
       FROM `COMPANION` c
      WHERE c.`travel_id` = t.`travel_id`) AS `companion_count`,

    COALESCE((
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'companion_seq', c.`companion_seq`,
                'relation_code', c.`relation_code`,
                'gender_code', c.`gender_code`,
                'age_group_code', c.`age_group_code`
            )
        )
          FROM `COMPANION` c
         WHERE c.`travel_id` = t.`travel_id`
    ), JSON_ARRAY()) AS `companions`,

    CASE WHEN NOT EXISTS (
        SELECT 1
          FROM `PREFERENCE` q
          JOIN `PREFERENCE_GROUP` g ON g.`group_code` = q.`group_code`
         WHERE (SELECT COUNT(*) FROM `TRAVEL_PREFERENCE` p
                 WHERE p.`travel_id` = t.`travel_id`
                   AND p.`preference_id` = q.`preference_id`)
               NOT BETWEEN g.`min_selections`
                       AND COALESCE(g.`max_selections`, 65535)
    ) THEN 1 ELSE 0 END AS `is_survey_complete`

FROM `TRAVEL` t
JOIN `USER` u ON u.`user_id` = t.`user_id`;

-- 재학습 데이터셋: 후기까지 끝난 여행의 (여행, AI 추천 관광지) 1줄
--  - 대상 여행: 일정 확정 + 후기 작성(모두 다녀옴 / 일부만 다녀옴). 가지 않음·경로 가져온 여행은 제외
--  - 대상 관광지: AI가 추천한 관광지 중 우리 관광지와 연결된 것(poi_id 있음). 화면에 안 보인 후보도 shown = 0으로 포함
--    같은 여행에서 여러 번 추천됐으면 화면에 보인 것 우선, 같으면 가장 최근 추천 1줄
--  - label: 3 방문 + 좋았어요 / 2 실제 방문 / 1 일정 확정(못 갔어요) / 0 추천만 (일정에 넣지 않음)
CREATE OR REPLACE SQL SECURITY INVOKER VIEW `AI_TRAINING_DATASET` AS
WITH done AS (
    SELECT t.`travel_id`, t.`user_id`, t.`adopted_route_id`, f.`feedback_id`, f.`execution_status`
      FROM `TRAVEL` t
      JOIN `TRAVEL_FEEDBACK` f ON f.`travel_id` = t.`travel_id`
     WHERE t.`adopted_route_id` IS NOT NULL
       AND t.`source_post_id` IS NULL
       AND f.`execution_status` IN ('COMPLETED', 'PARTIAL')
),
cand AS (
    SELECT q.`travel_id`, i.`poi_id`, i.`place_name`, i.`rank_no`, i.`shown`, q.`model_version`,
           ROW_NUMBER() OVER (PARTITION BY q.`travel_id`, i.`poi_id`
                              ORDER BY i.`shown` DESC, q.`request_id` DESC) AS rn
      FROM `RECOMMEND_REQUEST` q
      JOIN `RECOMMEND_ITEM` i ON i.`request_id` = q.`request_id`
     WHERE q.`status` = 'SUCCESS' AND i.`poi_id` IS NOT NULL
),
comp AS (
    SELECT c.`travel_id`, c.`relation_code`, c.`gender_code`, c.`age_group_code`,
           ROW_NUMBER() OVER (PARTITION BY c.`travel_id` ORDER BY c.`companion_seq`) AS slot
      FROM `COMPANION` c
),
comp_slots AS (
    SELECT `travel_id`,
           MAX(CASE WHEN slot = 1 THEN relation_code END)  AS `COMPANION_1_REL`,
           MAX(CASE WHEN slot = 1 THEN gender_code END)    AS `COMPANION_1_GENDER`,
           MAX(CASE WHEN slot = 1 THEN age_group_code END) AS `COMPANION_1_AGE`,
           MAX(CASE WHEN slot = 2 THEN relation_code END)  AS `COMPANION_2_REL`,
           MAX(CASE WHEN slot = 2 THEN gender_code END)    AS `COMPANION_2_GENDER`,
           MAX(CASE WHEN slot = 2 THEN age_group_code END) AS `COMPANION_2_AGE`,
           MAX(CASE WHEN slot = 3 THEN relation_code END)  AS `COMPANION_3_REL`,
           MAX(CASE WHEN slot = 3 THEN gender_code END)    AS `COMPANION_3_GENDER`,
           MAX(CASE WHEN slot = 3 THEN age_group_code END) AS `COMPANION_3_AGE`,
           MAX(CASE WHEN slot = 4 THEN relation_code END)  AS `COMPANION_4_REL`,
           MAX(CASE WHEN slot = 4 THEN gender_code END)    AS `COMPANION_4_GENDER`,
           MAX(CASE WHEN slot = 4 THEN age_group_code END) AS `COMPANION_4_AGE`,
           MAX(CASE WHEN slot = 5 THEN relation_code END)  AS `COMPANION_5_REL`,
           MAX(CASE WHEN slot = 5 THEN gender_code END)    AS `COMPANION_5_GENDER`,
           MAX(CASE WHEN slot = 5 THEN age_group_code END) AS `COMPANION_5_AGE`,
           MAX(CASE WHEN slot = 6 THEN relation_code END)  AS `COMPANION_6_REL`,
           MAX(CASE WHEN slot = 6 THEN gender_code END)    AS `COMPANION_6_GENDER`,
           MAX(CASE WHEN slot = 6 THEN age_group_code END) AS `COMPANION_6_AGE`,
           MAX(CASE WHEN slot = 7 THEN relation_code END)  AS `COMPANION_7_REL`,
           MAX(CASE WHEN slot = 7 THEN gender_code END)    AS `COMPANION_7_GENDER`,
           MAX(CASE WHEN slot = 7 THEN age_group_code END) AS `COMPANION_7_AGE`,
           MAX(CASE WHEN slot = 8 THEN relation_code END)  AS `COMPANION_8_REL`,
           MAX(CASE WHEN slot = 8 THEN gender_code END)    AS `COMPANION_8_GENDER`,
           MAX(CASE WHEN slot = 8 THEN age_group_code END) AS `COMPANION_8_AGE`,
           MAX(CASE WHEN slot = 9 THEN relation_code END)  AS `COMPANION_9_REL`,
           MAX(CASE WHEN slot = 9 THEN gender_code END)    AS `COMPANION_9_GENDER`,
           MAX(CASE WHEN slot = 9 THEN age_group_code END) AS `COMPANION_9_AGE`,
           MAX(CASE WHEN slot = 10 THEN relation_code END)  AS `COMPANION_10_REL`,
           MAX(CASE WHEN slot = 10 THEN gender_code END)    AS `COMPANION_10_GENDER`,
           MAX(CASE WHEN slot = 10 THEN age_group_code END) AS `COMPANION_10_AGE`,
           MAX(CASE WHEN slot = 11 THEN relation_code END)  AS `COMPANION_11_REL`,
           MAX(CASE WHEN slot = 11 THEN gender_code END)    AS `COMPANION_11_GENDER`,
           MAX(CASE WHEN slot = 11 THEN age_group_code END) AS `COMPANION_11_AGE`,
           MAX(CASE WHEN slot = 12 THEN relation_code END)  AS `COMPANION_12_REL`,
           MAX(CASE WHEN slot = 12 THEN gender_code END)    AS `COMPANION_12_GENDER`,
           MAX(CASE WHEN slot = 12 THEN age_group_code END) AS `COMPANION_12_AGE`,
           MAX(CASE WHEN slot = 13 THEN relation_code END)  AS `COMPANION_13_REL`,
           MAX(CASE WHEN slot = 13 THEN gender_code END)    AS `COMPANION_13_GENDER`,
           MAX(CASE WHEN slot = 13 THEN age_group_code END) AS `COMPANION_13_AGE`,
           MAX(CASE WHEN slot = 14 THEN relation_code END)  AS `COMPANION_14_REL`,
           MAX(CASE WHEN slot = 14 THEN gender_code END)    AS `COMPANION_14_GENDER`,
           MAX(CASE WHEN slot = 14 THEN age_group_code END) AS `COMPANION_14_AGE`,
           MAX(CASE WHEN slot = 15 THEN relation_code END)  AS `COMPANION_15_REL`,
           MAX(CASE WHEN slot = 15 THEN gender_code END)    AS `COMPANION_15_GENDER`,
           MAX(CASE WHEN slot = 15 THEN age_group_code END) AS `COMPANION_15_AGE`,
           MAX(CASE WHEN slot = 16 THEN relation_code END)  AS `COMPANION_16_REL`,
           MAX(CASE WHEN slot = 16 THEN gender_code END)    AS `COMPANION_16_GENDER`,
           MAX(CASE WHEN slot = 16 THEN age_group_code END) AS `COMPANION_16_AGE`,
           MAX(CASE WHEN slot = 17 THEN relation_code END)  AS `COMPANION_17_REL`,
           MAX(CASE WHEN slot = 17 THEN gender_code END)    AS `COMPANION_17_GENDER`,
           MAX(CASE WHEN slot = 17 THEN age_group_code END) AS `COMPANION_17_AGE`,
           MAX(CASE WHEN slot = 18 THEN relation_code END)  AS `COMPANION_18_REL`,
           MAX(CASE WHEN slot = 18 THEN gender_code END)    AS `COMPANION_18_GENDER`,
           MAX(CASE WHEN slot = 18 THEN age_group_code END) AS `COMPANION_18_AGE`
      FROM comp
     GROUP BY `travel_id`
)
SELECT
    d.`travel_id`,
    d.`user_id`,
    v.`gender_code`          AS `GENDER`,
    v.`age_group_code`       AS `AGE_GRP`,
    v.`income_code`          AS `INCOME`,
    v.`companion_count`      AS `TRAVEL_COMPANIONS_NUM`,
    v.`style_nature_city`    AS `TRAVEL_STYL_1`,
    v.`style_new_familiar`   AS `TRAVEL_STYL_3`,
    v.`style_relax_activity` AS `TRAVEL_STYL_5`,
    v.`style_hidden_famous`  AS `TRAVEL_STYL_6`,
    v.`style_plan_free`      AS `TRAVEL_STYL_7`,
    v.`photo_importance`     AS `TRAVEL_STYL_8`,
    v.`travel_motive_1`      AS `TRAVEL_MOTIVE_1`,
    v.`user_mission_1`       AS `TRAVEL_MISSION_PRIORITY_WEB`,
    cs.`COMPANION_1_REL`, cs.`COMPANION_1_GENDER`, cs.`COMPANION_1_AGE`,
    cs.`COMPANION_2_REL`, cs.`COMPANION_2_GENDER`, cs.`COMPANION_2_AGE`,
    cs.`COMPANION_3_REL`, cs.`COMPANION_3_GENDER`, cs.`COMPANION_3_AGE`,
    cs.`COMPANION_4_REL`, cs.`COMPANION_4_GENDER`, cs.`COMPANION_4_AGE`,
    cs.`COMPANION_5_REL`, cs.`COMPANION_5_GENDER`, cs.`COMPANION_5_AGE`,
    cs.`COMPANION_6_REL`, cs.`COMPANION_6_GENDER`, cs.`COMPANION_6_AGE`,
    cs.`COMPANION_7_REL`, cs.`COMPANION_7_GENDER`, cs.`COMPANION_7_AGE`,
    cs.`COMPANION_8_REL`, cs.`COMPANION_8_GENDER`, cs.`COMPANION_8_AGE`,
    cs.`COMPANION_9_REL`, cs.`COMPANION_9_GENDER`, cs.`COMPANION_9_AGE`,
    cs.`COMPANION_10_REL`, cs.`COMPANION_10_GENDER`, cs.`COMPANION_10_AGE`,
    cs.`COMPANION_11_REL`, cs.`COMPANION_11_GENDER`, cs.`COMPANION_11_AGE`,
    cs.`COMPANION_12_REL`, cs.`COMPANION_12_GENDER`, cs.`COMPANION_12_AGE`,
    cs.`COMPANION_13_REL`, cs.`COMPANION_13_GENDER`, cs.`COMPANION_13_AGE`,
    cs.`COMPANION_14_REL`, cs.`COMPANION_14_GENDER`, cs.`COMPANION_14_AGE`,
    cs.`COMPANION_15_REL`, cs.`COMPANION_15_GENDER`, cs.`COMPANION_15_AGE`,
    cs.`COMPANION_16_REL`, cs.`COMPANION_16_GENDER`, cs.`COMPANION_16_AGE`,
    cs.`COMPANION_17_REL`, cs.`COMPANION_17_GENDER`, cs.`COMPANION_17_AGE`,
    cs.`COMPANION_18_REL`, cs.`COMPANION_18_GENDER`, cs.`COMPANION_18_AGE`,
    c.`poi_id`,
    c.`place_name`           AS `VISIT_AREA_NM`,
    p.`address`,
    CASE p.`category_code`
        WHEN 'NATURE' THEN 1 WHEN 'HISTORY' THEN 2 WHEN 'CULTURE' THEN 3 WHEN 'COMMERCIAL' THEN 4
        WHEN 'LEISURE' THEN 5 WHEN 'THEME' THEN 6 WHEN 'TRAIL' THEN 7 WHEN 'FESTIVAL' THEN 8
        WHEN 'EXPERIENCE' THEN 13 END AS `VISIT_AREA_TYPE_CD`,
    p.`latitude`,
    p.`longitude`,
    r.`region_code`,
    c.`rank_no`              AS `recommend_rank`,
    c.`model_version`,
    c.`shown`,
    CASE
        WHEN fs.`visited` = 1 AND fs.`reaction` = 'LIKE' THEN 3
        WHEN fs.`visited` = 1 THEN 2
        WHEN fs.`visited` = 0 THEN 1
        -- 관광지별 결과가 없는 예전 후기: 확정 일정에 있으면 "모두 다녀옴"은 방문(2), 그 외는 일정 확정(1)
        WHEN EXISTS (SELECT 1 FROM `ROUTE_DAY` rd
                       JOIN `ROUTE_SPOT` s ON s.`route_day_id` = rd.`route_day_id`
                      WHERE rd.`route_id` = d.`adopted_route_id` AND s.`poi_id` = c.`poi_id`)
            THEN CASE WHEN d.`execution_status` = 'COMPLETED' THEN 2 ELSE 1 END
        ELSE 0
    END AS `label`
FROM done d
JOIN cand c ON c.`travel_id` = d.`travel_id` AND c.rn = 1
JOIN `AI_TRAVEL_INPUT` v ON v.`travel_id` = d.`travel_id`
JOIN `POI` p ON p.`poi_id` = c.`poi_id`
JOIN `REGION` r ON r.`region_id` = p.`region_id`
LEFT JOIN comp_slots cs ON cs.`travel_id` = d.`travel_id`
LEFT JOIN `TRAVEL_FEEDBACK_SPOT` fs ON fs.`feedback_id` = d.`feedback_id` AND fs.`poi_id` = c.`poi_id`;

-- 사용자 여정 퍼널: 여행 1개 = 1줄. 단계별 도달 여부(0/1) + 어디까지 갔나 + 어디서 이탈했나
--  단계: CREATED 여행 생성 → SURVEYED 설문 → RECOMMENDED 추천 받음 → PLACED_ANY 장소 담음
--        → PLACED_ALL 모두 배치 → ADOPTED 일정 확정 → REVIEWED 후기 작성 (SHARED 커뮤니티 공유는 선택 단계)
--  funnel_status: DONE 후기까지 완료 / IN_PROGRESS 아직 진행 중 / DROPPED 이탈 확정
--    이탈 확정 = 여행 종료일이 지났는데 일정 확정 전에 멈춤, 또는 확정했는데 종료일 + 14일까지 후기 없음
--  drop_step: 이탈 확정일 때 넘어가지 못한 단계 (그 외 NULL)
--  커뮤니티 경로를 가져와 만든 여행은 설문·추천이 없어 제외
CREATE OR REPLACE SQL SECURITY INVOKER VIEW `TRAVEL_FUNNEL` AS
WITH base AS (
    SELECT t.`travel_id`, t.`user_id`, t.`created_at`, t.`start_date`, t.`end_date`,
           EXISTS (SELECT 1 FROM `TRAVEL_PREFERENCE` tp WHERE tp.`travel_id` = t.`travel_id`) AS s_surveyed,
           EXISTS (SELECT 1 FROM `RECOMMEND_REQUEST` q
                    WHERE q.`travel_id` = t.`travel_id` AND q.`status` = 'SUCCESS') AS s_recommended,
           (SELECT COUNT(*) FROM `TRAVEL_BOOKMARK` b WHERE b.`travel_id` = t.`travel_id`) AS place_count,
           (SELECT COUNT(*) FROM `TRAVEL_BOOKMARK` b
             WHERE b.`travel_id` = t.`travel_id`
               AND EXISTS (SELECT 1 FROM `TRAVEL_ROUTE` rt
                             JOIN `ROUTE_DAY` rd ON rd.`route_id` = rt.`route_id`
                             JOIN `ROUTE_SPOT` s ON s.`route_day_id` = rd.`route_day_id`
                            WHERE rt.`travel_id` = t.`travel_id` AND s.`poi_id` = b.`poi_id`)) AS placed_count,
           t.`adopted_route_id` IS NOT NULL AS s_adopted,
           EXISTS (SELECT 1 FROM `TRAVEL_FEEDBACK` f WHERE f.`travel_id` = t.`travel_id`) AS s_reviewed,
           EXISTS (SELECT 1 FROM `COMMUNITY_POST` cp
                    WHERE cp.`travel_id` = t.`travel_id` AND cp.`deleted_at` IS NULL) AS s_shared
      FROM `TRAVEL` t
     WHERE t.`source_post_id` IS NULL
),
steps AS (
    SELECT b.*,
           b.place_count > 0 AS s_placed_any,
           b.place_count > 0 AND b.placed_count = b.place_count AS s_placed_all,
           CASE
               WHEN b.s_reviewed THEN 7
               WHEN b.s_adopted THEN 6
               WHEN b.place_count > 0 AND b.placed_count = b.place_count THEN 5
               WHEN b.place_count > 0 THEN 4
               WHEN b.s_recommended THEN 3
               WHEN b.s_surveyed THEN 2
               ELSE 1
           END AS reached_no
      FROM base b
)
SELECT
    `travel_id`, `user_id`, `created_at`, `start_date`, `end_date`,
    1 AS `step_created`,
    s_surveyed    AS `step_surveyed`,
    s_recommended AS `step_recommended`,
    s_placed_any  AS `step_placed_any`,
    s_placed_all  AS `step_placed_all`,
    s_adopted     AS `step_adopted`,
    s_reviewed    AS `step_reviewed`,
    s_shared      AS `step_shared`,
    place_count, placed_count,
    ELT(reached_no, 'CREATED', 'SURVEYED', 'RECOMMENDED', 'PLACED_ANY', 'PLACED_ALL', 'ADOPTED', 'REVIEWED') AS `reached_step`,
    CASE
        WHEN reached_no = 7 THEN 'DONE'
        WHEN reached_no < 6 AND `end_date` < CURRENT_DATE THEN 'DROPPED'
        WHEN reached_no = 6 AND `end_date` < CURRENT_DATE - INTERVAL 14 DAY THEN 'DROPPED'
        ELSE 'IN_PROGRESS'
    END AS `funnel_status`,
    CASE
        WHEN (reached_no < 6 AND `end_date` < CURRENT_DATE)
          OR (reached_no = 6 AND `end_date` < CURRENT_DATE - INTERVAL 14 DAY)
        THEN ELT(reached_no, 'SURVEYED', 'RECOMMENDED', 'PLACED_ANY', 'PLACED_ALL', 'ADOPTED', 'REVIEWED')
    END AS `drop_step`
FROM steps;

-- 서비스 처리 규칙
-- 1. 회원가입은 이메일 인증 + 비밀번호로만. 소셜은 로그인 상태에서 연동(SOCIAL_ACCOUNT INSERT).
--    소셜 로그인 시 (provider, provider_user_id)로 회원을 찾고, 없으면 "연동된 계정 없음" 안내.
-- 2. 탈퇴 요청: USER.status='WITHDRAWAL_PENDING', withdrawn_at=NOW(), 해당 회원 AUTH_SESSION 모두 revoked_at 설정.
--    유예 중 로그인 시 탈퇴 철회 화면만 제공. 철회: status='ACTIVE', withdrawn_at=NULL.
--    유예 30일 경과: 배치가 sp_purge_user 호출. 유예 중에는 같은 이메일로 재가입할 수 없다.
-- 3. TRAVEL과 필수 설문 9개 항목을 동일 트랜잭션에서 저장한다.
--    TRAVEL_PREFERENCE INSERT 시 response_type은 PREFERENCE의 값을 그대로 넣는다(복합 FK가 검사).
--    수정 시 TRAVEL 행을 SELECT ... FOR UPDATE로 잠근 뒤 답변 교체·검증·커밋한다.
-- 4. 최소 선택 수는 DB가 강제하지 않는다(서비스가 검사). 저장 후 AI_TRAVEL_INPUT.is_survey_complete로 확인 가능.
--    여행 동기·테마는 정확히 3개, answer_rank는 1·2·3 (사용자가 고른 순서). 단일 선택 답변은 answer_rank = 1.
-- 5. SINGLE_SELECT 질문은 max_selections = 1인 그룹에만 둔다.
-- 6. ALL 지역은 TRAVEL_REGION 0행, SELECTED는 1행 이상.
-- 7. 경로는 같은 여행의 찜 목록으로 생성한다. day_no는 1 ~ (end_date - start_date + 1).
--    방문 순서 변경은 해당 일차의 ROUTE_SPOT을 DELETE 후 다시 INSERT.
-- 8. 피드백 route_id는 현재 채택된 경로와 같아야 한다. DB FK는 같은 여행 소속까지만 강제한다.
-- 9. NOT_TAKEN은 만족도 NULL + 사유 1개 이상. PARTIAL은 만족도 1~5 + 사유 1개 이상.
--    COMPLETED는 만족도 1~5, 사유 없음. 상태 수정 시 피드백/사유를 한 트랜잭션으로 수정한다.
-- 10. 동반자 수는 본인을 제외한 COMPANION 행 수. 혼자라면 0.
-- 11. 여행 삭제는 sp_delete_travel, 회원 최종 삭제는 sp_purge_user를 사용한다.
