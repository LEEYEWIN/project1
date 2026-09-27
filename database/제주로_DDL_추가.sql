CREATE DATABASE jeju_travel
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE jeju_travel;

-- 제주 여행 서비스: 새 DB 생성용 DDL + 기초 코드 + AI 입력 VIEW
-- 기준: MySQL 8.0.16 이상 / InnoDB / utf8mb4
-- 8.0.16 미만에서는 CHECK가 강제되지 않으므로 이 파일의 검증 규칙을 충족하지 않습니다.
-- 사용: 빈 스키마를 생성·선택한 뒤 전체 실행하십시오. 예: USE 선택한_DB;
-- DB 이름을 임의 지정하지 않았으며 DROP 문, 기존 데이터 이관은 포함하지 않습니다.
-- 원본: 첨부 ERD를 우선하고, 첨부 문서의 코드 값 및 충돌하지 않는 기능 설명 반영.
-- 26개 TABLE + 1개 VIEW. AI 추천 요청/결과 및 POI_PREFERENCE는 만들지 않습니다.
-- 모든 FK는 자식 PK 밖에 두는 비식별 관계입니다. 기존 복합 자연키는 UNIQUE로 보존합니다.
-- 질문 그룹의 선택 범위를 저장합니다. 최소 응답과 선택 개수는 백엔드 트랜잭션에서 검증합니다.
SET NAMES utf8mb4;


-- ============================================================
-- 제주 여행 서비스 전체 DB 스키마 (jeju_schema_full.sql)
-- 원본 25개 테이블 + AI_TRAVEL_INPUT VIEW + 트리거 2개 + 프로시저 2개
-- + 커뮤니티 확장(조회수/공개비공개/첨부파일/좋아요싫어요) 통합본
-- MySQL 8.0 / InnoDB / utf8mb4
-- ============================================================

START TRANSACTION;

-- ============================================================
-- 4-01. USER — 회원
-- ============================================================
CREATE TABLE `USER` (
    `user_id`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `email`         VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname`      VARCHAR(50)  NOT NULL,
    `birth_date`    DATE NOT NULL,
    `gender_code`   TINYINT UNSIGNED NOT NULL,
    `status`        VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    `withdrawn_at`  DATETIME NULL,
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `uk_user_email` UNIQUE (`email`),
    CONSTRAINT `ck_user_gender_code` CHECK (`gender_code` IN (1, 2)),
    CONSTRAINT `ck_user_status` CHECK (
        (`status` = 'ACTIVE' AND `withdrawn_at` IS NULL)
        OR (`status` = 'WITHDRAWAL_PENDING' AND `withdrawn_at` IS NOT NULL)
    ),
    INDEX `ix_user_status_withdrawn` (`status`, `withdrawn_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-02. SOCIAL_ACCOUNT — 소셜 로그인 연동
-- ============================================================
CREATE TABLE `SOCIAL_ACCOUNT` (
    `social_account_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`            BIGINT UNSIGNED NOT NULL,
    `provider`           VARCHAR(20) NOT NULL,
    `provider_user_id`   VARCHAR(255) NOT NULL,
    `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`social_account_id`),
    CONSTRAINT `fk_social_account_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `uk_social_provider_user` UNIQUE (`provider`, `provider_user_id`),
    CONSTRAINT `uk_social_user_provider` UNIQUE (`user_id`, `provider`),
    CONSTRAINT `ck_social_provider` CHECK (`provider` IN ('KAKAO', 'NAVER', 'GOOGLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-03. EMAIL_VERIFICATION — 이메일 인증 요청
-- ============================================================
CREATE TABLE `EMAIL_VERIFICATION` (
    `verification_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `email`            VARCHAR(255) NOT NULL,
    `purpose`          VARCHAR(30) NOT NULL,
    `code_verifier`    VARCHAR(255) NOT NULL,
    `expires_at`       DATETIME NOT NULL,
    `failed_count`     INT UNSIGNED NOT NULL DEFAULT 0,
    `used_at`          DATETIME NULL,
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`verification_id`),
    CONSTRAINT `ck_email_verification_purpose`
        CHECK (`purpose` IN ('SIGNUP', 'EMAIL_CHANGE', 'PASSWORD_RESET')),
    INDEX `ix_email_verification_lookup` (`email`, `purpose`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-04. AUTH_SESSION — 로그인 세션
-- ============================================================
CREATE TABLE `AUTH_SESSION` (
    `session_id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT UNSIGNED NOT NULL,
    `refresh_token_hash`  VARCHAR(255) NOT NULL,
    `expires_at`          DATETIME NOT NULL,
    `revoked_at`          DATETIME NULL,
    `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`session_id`),
    CONSTRAINT `fk_auth_session_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `uk_auth_session_refresh_token` UNIQUE (`refresh_token_hash`),
    INDEX `ix_auth_session_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-05. CODE_GROUP — 공통 코드 그룹
-- ============================================================
CREATE TABLE `CODE_GROUP` (
    `group_code` VARCHAR(30) NOT NULL,
    `group_name` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-06. CODE — 공통 코드 값
-- ============================================================
CREATE TABLE `CODE` (
    `code_id`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `group_code` VARCHAR(30) NOT NULL,
    `code_value` VARCHAR(50) NOT NULL,
    `code_name`  VARCHAR(100) NOT NULL,
    PRIMARY KEY (`code_id`),
    CONSTRAINT `fk_code_group`
        FOREIGN KEY (`group_code`) REFERENCES `CODE_GROUP` (`group_code`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `uk_code_natural` UNIQUE (`group_code`, `code_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-07. REGION — 제주 여행 권역
-- ============================================================
CREATE TABLE `REGION` (
    `region_id`   TINYINT UNSIGNED NOT NULL,
    `region_code` VARCHAR(20) NOT NULL,
    `region_name` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`region_id`),
    CONSTRAINT `uk_region_code` UNIQUE (`region_code`),
    CONSTRAINT `uk_region_name` UNIQUE (`region_name`),
    CONSTRAINT `ck_region_code` CHECK (`region_code` IN ('EAST', 'WEST', 'SOUTH', 'NORTH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-08. PREFERENCE_GROUP — 설문 질문 그룹·선택 수 규칙
-- ============================================================
CREATE TABLE `PREFERENCE_GROUP` (
    `group_code`      VARCHAR(30) NOT NULL,
    `group_name`      VARCHAR(100) NOT NULL,
    `min_selections`  TINYINT UNSIGNED NOT NULL,
    `max_selections`  TINYINT UNSIGNED NULL,
    PRIMARY KEY (`group_code`),
    CONSTRAINT `ck_preference_group_count` CHECK (
        `min_selections` >= 1
        AND (`max_selections` IS NULL OR `max_selections` >= `min_selections`)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-09. PREFERENCE — 설문 질문
-- ============================================================
CREATE TABLE `PREFERENCE` (
    `preference_id`   BIGINT UNSIGNED NOT NULL,
    `preference_code` VARCHAR(50) NOT NULL,
    `preference_name` VARCHAR(100) NOT NULL,
    `response_type`   VARCHAR(30) NOT NULL,
    `group_code`      VARCHAR(30) NOT NULL,
    PRIMARY KEY (`preference_id`),
    CONSTRAINT `fk_preference_group`
        FOREIGN KEY (`group_code`) REFERENCES `PREFERENCE_GROUP` (`group_code`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_preference_code` UNIQUE (`preference_code`),
    CONSTRAINT `uk_preference_type` UNIQUE (`preference_id`, `response_type`),
    CONSTRAINT `ck_preference_response_type`
        CHECK (`response_type` IN ('SINGLE_SELECT', 'MULTI_SELECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-10. PREFERENCE_OPTION — 설문 선택지
-- ============================================================
CREATE TABLE `PREFERENCE_OPTION` (
    `option_id`     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `preference_id` BIGINT UNSIGNED NOT NULL,
    `option_value`  INT UNSIGNED NOT NULL,
    `option_name`   VARCHAR(100) NOT NULL,
    `description`   VARCHAR(500) NULL,
    PRIMARY KEY (`option_id`),
    CONSTRAINT `fk_preference_option_preference`
        FOREIGN KEY (`preference_id`) REFERENCES `PREFERENCE` (`preference_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `uk_preference_option_natural` UNIQUE (`preference_id`, `option_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-11. POI — 관광지 기본정보
-- ============================================================
CREATE TABLE `POI` (
    `poi_id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `poi_name`      VARCHAR(200) NOT NULL,
    `address`       VARCHAR(500) NOT NULL,
    `latitude`      DECIMAL(10,7) NOT NULL,
    `longitude`     DECIMAL(10,7) NOT NULL,
    `category_code` VARCHAR(50) NOT NULL,
    `region_id`     TINYINT UNSIGNED NOT NULL,
    `description`   TEXT NOT NULL,
    `image_url`     VARCHAR(2048) NOT NULL,
    PRIMARY KEY (`poi_id`),
    CONSTRAINT `fk_poi_region`
        FOREIGN KEY (`region_id`) REFERENCES `REGION` (`region_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `ck_poi_latitude` CHECK (`latitude` BETWEEN -90 AND 90),
    CONSTRAINT `ck_poi_longitude` CHECK (`longitude` BETWEEN -180 AND 180),
    INDEX `ix_poi_region_category` (`region_id`, `category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-12. TRAVEL — 여행 계획
-- adopted_route_id의 복합 FK는 TRAVEL_ROUTE 생성 뒤 ALTER TABLE로 추가한다
-- (TRAVEL ↔ TRAVEL_ROUTE 순환 참조 때문)
-- ============================================================
CREATE TABLE `TRAVEL` (
    `travel_id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT UNSIGNED NOT NULL,
    `travel_no`           INT UNSIGNED NOT NULL,
    `travel_name`         VARCHAR(100) NOT NULL,
    `start_date`          DATE NOT NULL,
    `end_date`            DATE NOT NULL,
    `region_mode`         VARCHAR(20) NOT NULL,
    `age_group_snapshot`  TINYINT UNSIGNED NOT NULL,
    `adopted_route_id`    BIGINT UNSIGNED NULL,
    `adopted_at`          DATETIME NULL,
    `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`travel_id`),
    CONSTRAINT `fk_travel_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `uk_travel_user_no` UNIQUE (`user_id`, `travel_no`),
    CONSTRAINT `uk_travel_id_route` UNIQUE (`travel_id`, `adopted_route_id`),
    CONSTRAINT `ck_travel_date` CHECK (`start_date` <= `end_date`),
    CONSTRAINT `ck_travel_region_mode` CHECK (`region_mode` IN ('ALL', 'SELECTED')),
    CONSTRAINT `ck_travel_age_group` CHECK (`age_group_snapshot` BETWEEN 1 AND 8),
    CONSTRAINT `ck_travel_adopted_pair` CHECK (
        (`adopted_route_id` IS NULL AND `adopted_at` IS NULL)
        OR (`adopted_route_id` IS NOT NULL AND `adopted_at` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-13. COMPANION — 여행 동반자
-- ============================================================
CREATE TABLE `COMPANION` (
    `companion_id`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id`       BIGINT UNSIGNED NOT NULL,
    `companion_seq`   SMALLINT UNSIGNED NOT NULL,
    `relation_code`   TINYINT UNSIGNED NOT NULL,
    `gender_code`     TINYINT UNSIGNED NOT NULL,
    `age_group_code`  TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (`companion_id`),
    CONSTRAINT `fk_companion_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `uk_companion_natural` UNIQUE (`travel_id`, `companion_seq`),
    CONSTRAINT `ck_companion_seq` CHECK (`companion_seq` >= 1),
    CONSTRAINT `ck_companion_relation` CHECK (`relation_code` BETWEEN 1 AND 11),
    CONSTRAINT `ck_companion_gender` CHECK (`gender_code` IN (1, 2)),
    CONSTRAINT `ck_companion_age_group` CHECK (`age_group_code` BETWEEN 1 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-14. TRAVEL_REGION — 여행에서 선택한 권역
-- ============================================================
CREATE TABLE `TRAVEL_REGION` (
    `travel_region_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id`         BIGINT UNSIGNED NOT NULL,
    `region_id`         TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (`travel_region_id`),
    CONSTRAINT `fk_travel_region_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_travel_region_region`
        FOREIGN KEY (`region_id`) REFERENCES `REGION` (`region_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_travel_region_natural` UNIQUE (`travel_id`, `region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-15. TRAVEL_PREFERENCE — 여행별 설문 답변
-- ============================================================
CREATE TABLE `TRAVEL_PREFERENCE` (
    `travel_preference_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id`             BIGINT UNSIGNED NOT NULL,
    `preference_id`         BIGINT UNSIGNED NOT NULL,
    `response_type`         VARCHAR(30) NOT NULL,
    `answer_value`          INT UNSIGNED NOT NULL,
    `single_preference_id`  BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN `response_type` = 'SINGLE_SELECT' THEN `preference_id` ELSE NULL END
        ) STORED,
    PRIMARY KEY (`travel_preference_id`),
    CONSTRAINT `fk_travel_preference_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_travel_preference_type`
        FOREIGN KEY (`preference_id`, `response_type`)
        REFERENCES `PREFERENCE` (`preference_id`, `response_type`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_travel_preference_option`
        FOREIGN KEY (`preference_id`, `answer_value`)
        REFERENCES `PREFERENCE_OPTION` (`preference_id`, `option_value`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_travel_preference_natural` UNIQUE (`travel_id`, `preference_id`, `answer_value`),
    CONSTRAINT `uk_travel_preference_single` UNIQUE (`travel_id`, `single_preference_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-16. POI_SOURCE_MAP — 외부 관광지 ID 연결
-- ============================================================
CREATE TABLE `POI_SOURCE_MAP` (
    `source_poi_id` VARCHAR(255) NOT NULL,
    `poi_id`        BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`source_poi_id`),
    CONSTRAINT `fk_poi_source_map_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX `ix_poi_source_map_poi` (`poi_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-17. TRAVEL_BOOKMARK — 여행별 관광지 찜
-- ============================================================
CREATE TABLE `TRAVEL_BOOKMARK` (
    `bookmark_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id`   BIGINT UNSIGNED NOT NULL,
    `poi_id`      BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`bookmark_id`),
    CONSTRAINT `fk_travel_bookmark_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_travel_bookmark_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_travel_bookmark_natural` UNIQUE (`travel_id`, `poi_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-18. TRAVEL_ROUTE — 여행 경로
-- ============================================================
CREATE TABLE `TRAVEL_ROUTE` (
    `route_id`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id`   BIGINT UNSIGNED NOT NULL,
    `route_name`  VARCHAR(100) NULL,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`route_id`),
    CONSTRAINT `fk_travel_route_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_travel_route_travel_route` UNIQUE (`travel_id`, `route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- TRAVEL.adopted_route_id 복합 FK를 이제 추가한다 (두 테이블 모두 생성된 뒤)
ALTER TABLE `TRAVEL`
    ADD CONSTRAINT `fk_travel_adopted_route`
        FOREIGN KEY (`travel_id`, `adopted_route_id`)
        REFERENCES `TRAVEL_ROUTE` (`travel_id`, `route_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

-- ============================================================
-- 4-19. ROUTE_DAY — 일차별 일정
-- ============================================================
CREATE TABLE `ROUTE_DAY` (
    `route_day_id`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `route_id`           BIGINT UNSIGNED NOT NULL,
    `day_no`             SMALLINT UNSIGNED NOT NULL,
    `primary_region_id`  TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (`route_day_id`),
    CONSTRAINT `fk_route_day_route`
        FOREIGN KEY (`route_id`) REFERENCES `TRAVEL_ROUTE` (`route_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_route_day_region`
        FOREIGN KEY (`primary_region_id`) REFERENCES `REGION` (`region_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_route_day_no` UNIQUE (`route_id`, `day_no`),
    CONSTRAINT `ck_route_day_no` CHECK (`day_no` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-20. ROUTE_SPOT — 일정 안의 방문 관광지
-- ============================================================
CREATE TABLE `ROUTE_SPOT` (
    `route_spot_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `route_day_id`  BIGINT UNSIGNED NOT NULL,
    `poi_id`        BIGINT UNSIGNED NOT NULL,
    `visit_order`   SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (`route_spot_id`),
    CONSTRAINT `fk_route_spot_day`
        FOREIGN KEY (`route_day_id`) REFERENCES `ROUTE_DAY` (`route_day_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_route_spot_poi`
        FOREIGN KEY (`poi_id`) REFERENCES `POI` (`poi_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_route_spot_order` UNIQUE (`route_day_id`, `visit_order`),
    CONSTRAINT `uk_route_spot_poi` UNIQUE (`route_day_id`, `poi_id`),
    CONSTRAINT `ck_route_spot_order` CHECK (`visit_order` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-21. TRAVEL_FEEDBACK — 여행 수행 결과
-- ============================================================
CREATE TABLE `TRAVEL_FEEDBACK` (
    `feedback_id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `travel_id`           BIGINT UNSIGNED NOT NULL,
    `route_id`            BIGINT UNSIGNED NOT NULL,
    `execution_status`    VARCHAR(20) NOT NULL,
    `satisfaction_score`  TINYINT UNSIGNED NULL,
    `answered_at`         DATETIME NOT NULL,
    PRIMARY KEY (`feedback_id`),
    CONSTRAINT `fk_travel_feedback_travel`
        FOREIGN KEY (`travel_id`) REFERENCES `TRAVEL` (`travel_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_travel_feedback_route`
        FOREIGN KEY (`travel_id`, `route_id`)
        REFERENCES `TRAVEL_ROUTE` (`travel_id`, `route_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `uk_travel_feedback_natural` UNIQUE (`travel_id`),
    CONSTRAINT `ck_travel_feedback_status`
        CHECK (`execution_status` IN ('COMPLETED', 'PARTIAL', 'NOT_TAKEN')),
    CONSTRAINT `ck_travel_feedback_satisfaction` CHECK (
        (`execution_status` = 'NOT_TAKEN' AND `satisfaction_score` IS NULL)
        OR (`execution_status` <> 'NOT_TAKEN' AND `satisfaction_score` BETWEEN 1 AND 5)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-22. TRAVEL_FEEDBACK_REASON — 미수행·부분 수행 사유
-- ============================================================
CREATE TABLE `TRAVEL_FEEDBACK_REASON` (
    `feedback_reason_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `feedback_id`         BIGINT UNSIGNED NOT NULL,
    `reason_code`         VARCHAR(30) NOT NULL,
    `reason_text`         VARCHAR(50) NULL,
    PRIMARY KEY (`feedback_reason_id`),
    CONSTRAINT `fk_travel_feedback_reason_feedback`
        FOREIGN KEY (`feedback_id`) REFERENCES `TRAVEL_FEEDBACK` (`feedback_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `uk_travel_feedback_reason_natural` UNIQUE (`feedback_id`, `reason_code`),
    CONSTRAINT `ck_travel_feedback_reason_code` CHECK (
        `reason_code` IN ('TIME_SHORTAGE', 'CHANGE_OF_MIND', 'PERSONAL_REASON',
                           'WEATHER', 'POI_ISSUE', 'OTHER')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-23. ACCOMMODATION — 숙소 조회 목록 (다른 테이블과 FK 없음)
-- ============================================================
CREATE TABLE `ACCOMMODATION` (
    `accommodation_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `source_id`         VARCHAR(255) NOT NULL,
    `name`              VARCHAR(200) NOT NULL,
    `address`           VARCHAR(500) NOT NULL,
    `latitude`          DECIMAL(10,7) NOT NULL,
    `longitude`         DECIMAL(10,7) NOT NULL,
    PRIMARY KEY (`accommodation_id`),
    CONSTRAINT `uk_accommodation_source` UNIQUE (`source_id`),
    CONSTRAINT `ck_accommodation_latitude` CHECK (`latitude` BETWEEN -90 AND 90),
    CONSTRAINT `ck_accommodation_longitude` CHECK (`longitude` BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-24. COMMUNITY_POST — 커뮤니티 게시글
-- [커뮤니티 확장] view_count, visibility 컬럼 포함
-- [커뮤니티 확장] post_type에 'INFO'(정보) 추가 — 화면 목업/프론트 요구사항 반영
-- ============================================================
CREATE TABLE `COMMUNITY_POST` (
    `post_id`     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT UNSIGNED NULL,
    `post_type`   VARCHAR(20) NOT NULL,
    `title`       VARCHAR(200) NOT NULL,
    `content`     TEXT NOT NULL,
    `view_count`  INT UNSIGNED NOT NULL DEFAULT 0,
    `visibility`  VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NULL,
    PRIMARY KEY (`post_id`),
    CONSTRAINT `fk_community_post_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT `ck_community_post_type` CHECK (`post_type` IN ('QUESTION', 'REVIEW', 'INFO')),
    CONSTRAINT `ck_community_post_visibility` CHECK (`visibility` IN ('PUBLIC', 'PRIVATE')),
    INDEX `ix_community_post_user_created` (`user_id`, `created_at`),
    INDEX `ix_community_post_type_created` (`post_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- [커뮤니티 확장 신규] COMMUNITY_POST_ATTACHMENT — 게시글 첨부파일
-- ------------------------------------------------------------
CREATE TABLE `COMMUNITY_POST_ATTACHMENT` (
    `attachment_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id`       BIGINT UNSIGNED NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `stored_path`   VARCHAR(2048) NOT NULL,
    `file_size`     BIGINT UNSIGNED NOT NULL,
    `content_type`  VARCHAR(100) NOT NULL,
    `uploaded_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`attachment_id`),
    CONSTRAINT `fk_community_post_attachment_post`
        FOREIGN KEY (`post_id`) REFERENCES `COMMUNITY_POST` (`post_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    INDEX `ix_community_post_attachment_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------------------------
-- [커뮤니티 확장 신규] COMMUNITY_POST_REACTION — 좋아요/싫어요
-- ------------------------------------------------------------
CREATE TABLE `COMMUNITY_POST_REACTION` (
    `reaction_id`   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id`       BIGINT UNSIGNED NOT NULL,
    `user_id`       BIGINT UNSIGNED NULL,
    `reaction_type` VARCHAR(10) NOT NULL,
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`reaction_id`),
    CONSTRAINT `fk_community_post_reaction_post`
        FOREIGN KEY (`post_id`) REFERENCES `COMMUNITY_POST` (`post_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_community_post_reaction_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT `uk_community_post_reaction_natural` UNIQUE (`post_id`, `user_id`),
    CONSTRAINT `ck_community_post_reaction_type` CHECK (`reaction_type` IN ('LIKE', 'DISLIKE')),
    INDEX `ix_community_post_reaction_post_type` (`post_id`, `reaction_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4-25. COMMUNITY_COMMENT — 게시글 댓글
-- ============================================================
CREATE TABLE `COMMUNITY_COMMENT` (
    `comment_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `post_id`    BIGINT UNSIGNED NOT NULL,
    `user_id`    BIGINT UNSIGNED NULL,
    `content`    TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL,
    PRIMARY KEY (`comment_id`),
    CONSTRAINT `fk_community_comment_post`
        FOREIGN KEY (`post_id`) REFERENCES `COMMUNITY_POST` (`post_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_community_comment_user`
        FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
        ON DELETE SET NULL ON UPDATE RESTRICT,
    INDEX `ix_community_comment_post_created` (`post_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

COMMIT;

-- ============================================================
-- 6-3. AI_TRAVEL_INPUT — AI 제공 정보 VIEW
-- ============================================================
CREATE SQL SECURITY INVOKER VIEW `AI_TRAVEL_INPUT` AS
SELECT
    t.`travel_id`,
    t.`user_id`,
    u.`gender_code`,
    t.`age_group_snapshot` AS `age_group_code`,
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
    COALESCE((SELECT JSON_ARRAYAGG(p.`answer_value`)
       FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 201), JSON_ARRAY()) AS `travel_motive`,
    COALESCE((SELECT JSON_ARRAYAGG(p.`answer_value`)
       FROM `TRAVEL_PREFERENCE` p
      WHERE p.`travel_id` = t.`travel_id` AND p.`preference_id` = 202), JSON_ARRAY()) AS `user_mission`,
    (SELECT COUNT(*) FROM `COMPANION` c
      WHERE c.`travel_id` = t.`travel_id`) AS `companion_count`,
    CASE WHEN NOT EXISTS (
        SELECT 1
          FROM `PREFERENCE` q
          JOIN `PREFERENCE_GROUP` g ON g.`group_code` = q.`group_code`
         WHERE (SELECT COUNT(*) FROM `TRAVEL_PREFERENCE` p
                 WHERE p.`travel_id` = t.`travel_id`
                   AND p.`preference_id` = q.`preference_id`)
               NOT BETWEEN g.`min_selections` AND COALESCE(g.`max_selections`, 65535)
    ) THEN 1 ELSE 0 END AS `is_survey_complete`
FROM `TRAVEL` t
JOIN `USER` u ON u.`user_id` = t.`user_id`;

-- ============================================================
-- 4-26. 트리거와 프로시저
-- ============================================================
DELIMITER $$

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

-- ============================================================
-- 5. 초기 코드 데이터
-- ============================================================
START TRANSACTION;

-- 5-1 / 5-2 / 5-3 / 5-4. 공통 코드
INSERT INTO `CODE_GROUP` (`group_code`, `group_name`) VALUES
    ('GEN', '성별'),
    ('AGE', '연령대'),
    ('TCR', '동행자 관계');

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
    ('TCR', '11', '기타');

-- 5-5. 제주 권역
INSERT INTO `REGION` (`region_id`, `region_code`, `region_name`) VALUES
    (1, 'EAST', '동부'),
    (2, 'WEST', '서부'),
    (3, 'SOUTH', '남부'),
    (4, 'NORTH', '북부');

-- 5-6. 질문 그룹과 선택 수 규칙
INSERT INTO `PREFERENCE_GROUP` (`group_code`, `group_name`, `min_selections`, `max_selections`) VALUES
    ('STYLE', '여행 스타일', 1, 1),
    ('MOTIVE', '여행 동기', 1, 3),
    ('THEME', '테마 선호도', 1, NULL),
    ('INCOME', '월평균 소득 구간', 1, 1);

-- 5-7. 질문
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

-- 5-8. 양자택일 6개 질문의 선택지
INSERT INTO `PREFERENCE_OPTION` (`preference_id`, `option_value`, `option_name`) VALUES
    (101, 1, '자연 중심'), (101, 2, '도시 중심'),
    (102, 1, '새로운 장소'), (102, 2, '익숙한 장소'),
    (103, 1, '숨은 명소'), (103, 2, '유명 인기 명소'),
    (104, 1, '휴식·힐링 중심'), (104, 2, '체험·액티비티 중심'),
    (105, 1, '중요함'), (105, 2, '중요하지 않음'),
    (106, 1, '정해진 계획대로'), (106, 2, '상황에 따라 자유롭게');

-- 5-9. 여행 동기(201)
INSERT INTO `PREFERENCE_OPTION` (`preference_id`, `option_value`, `option_name`) VALUES
    (201, 1, '일상 탈출·기분 전환'),
    (201, 2, '휴식·피로 회복'),
    (201, 3, '친목·유대감'),
    (201, 4, '자아 탐색·성찰'),
    (201, 5, 'SNS 공유·자랑'),
    (201, 6, '운동·건강'),
    (201, 7, '새로운 경험'),
    (201, 8, '역사·문화·배움'),
    (201, 9, '기념·특별한 목적');

-- 5-10. 테마 선호도(202, 유저 미션)
INSERT INTO `PREFERENCE_OPTION` (`preference_id`, `option_value`, `option_name`) VALUES
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
    (202, 21, '등반여행');

-- 5-11. 월평균 소득(203)
INSERT INTO `PREFERENCE_OPTION` (`preference_id`, `option_value`, `option_name`) VALUES
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
