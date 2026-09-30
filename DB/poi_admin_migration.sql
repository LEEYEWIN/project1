-- 기존 jeju_schema.sql로 만든 DB에 관리자 관광지 기능을 적용할 때 한 번 실행한다.
-- 새 DB는 jeju_schema.sql에 같은 컬럼이 포함되어 있으므로 이 파일을 실행하지 않는다.
ALTER TABLE `POI`
    ADD COLUMN `hidden_at` DATETIME NULL,
    ADD COLUMN `deleted_at` DATETIME NULL,
    ADD COLUMN `ai_recommend` BOOLEAN NOT NULL DEFAULT FALSE;
