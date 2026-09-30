-- 기존 DB의 TRAVEL 테이블에 source_post_id가 없을 때 한 번 실행한다.
-- 새 DB는 jeju_schema.sql에 같은 컬럼과 외래 키가 이미 포함되어 있다.
-- 실행할 스키마(예: jejuro)를 먼저 선택한다.

ALTER TABLE `TRAVEL`
    ADD COLUMN `source_post_id` BIGINT UNSIGNED NULL
        COMMENT '커뮤니티 글의 경로를 가져와 만든 여행이면 그 글 번호'
        AFTER `adopted_at`;

ALTER TABLE `TRAVEL`
    ADD CONSTRAINT `fk_travel_source_post`
        FOREIGN KEY (`source_post_id`) REFERENCES `COMMUNITY_POST` (`post_id`)
        ON UPDATE RESTRICT ON DELETE SET NULL;
