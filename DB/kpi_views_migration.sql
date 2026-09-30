-- 기존 DB에 빠진 KPI 뷰를 jeju_schema.sql의 정의로 복구한다.
-- 새 DB에는 이미 포함되어 있다. 기존 스키마를 먼저 선택한다.

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
