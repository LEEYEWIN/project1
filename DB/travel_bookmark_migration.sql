-- 기존 DB의 TRAVEL_BOOKMARK에 source, created_at이 없을 때 한 번 실행한다.
-- 새 DB는 jeju_schema.sql에 같은 컬럼과 제약이 이미 포함되어 있다.

ALTER TABLE `TRAVEL_BOOKMARK`
    ADD COLUMN `source` VARCHAR(20) NOT NULL DEFAULT 'SEARCH'
        COMMENT 'RECOMMEND AI 추천 / SEARCH 관광지 검색 / IMPORT 경로 가져오기'
        AFTER `poi_id`,
    ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        AFTER `source`,
    ADD CONSTRAINT `ck_travel_bookmark_source`
        CHECK (`source` IN ('RECOMMEND', 'SEARCH', 'IMPORT'));
